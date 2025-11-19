package com.prog7314.geoquest.data.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prog7314.geoquest.data.data.LocationData
import com.prog7314.geoquest.data.local.GeoQuestDatabase
import com.prog7314.geoquest.data.local.toLocationData
import com.prog7314.geoquest.data.local.toLocationEntity
import com.prog7314.geoquest.data.repo.LocationRepo
import com.prog7314.geoquest.data.repo.NotificationRepo
import com.prog7314.geoquest.data.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val locationRepo = LocationRepo()
    private val notificationRepo = NotificationRepo()
    private val database = GeoQuestDatabase.getDatabase(application)
    private val locationDao = database.locationDao()
    private val syncManager = SyncManager(application)

    private val _locations = MutableStateFlow<List<LocationData>>(emptyList())
    val locations: StateFlow<List<LocationData>> = _locations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    // Track recently deleted location IDs to prevent them from being re-inserted
    private val _deletedLocationIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedLocationIds: StateFlow<Set<String>> = _deletedLocationIds.asStateFlow()

    init {
        checkConnectivity()
        updateUnsyncedCount()
    }

    private fun checkConnectivity() {
        _isOnline.value = syncManager.isOnline()
    }

    private fun updateUnsyncedCount() {
        viewModelScope.launch {
            // Count both unsynced locations and unsynced deletions
            val unsyncedLocations = locationDao.getUnsyncedLocations().size
            val unsyncedDeletions = locationDao.getDeletedUnsyncedLocations().size
            _unsyncedCount.value = unsyncedLocations + unsyncedDeletions
        }
    }

    fun addLocation(locationData: LocationData) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Generate ID if not present
                val location = if (locationData.id.isEmpty()) {
                    locationData.copy(id = UUID.randomUUID().toString())
                } else {
                    locationData
                }

                checkConnectivity()

                if (_isOnline.value) {
                    // Online: Save to Firebase first, then local
                    val result = locationRepo.addLocation(location)
                    if (result.isSuccess) {
                        // Save to local DB as synced
                        locationDao.insertLocation(location.toLocationEntity(isSynced = true))

                        // Send notification
                        notificationRepo.notifyLocationAdded(
                            userId = location.userId,
                            locationId = location.id,
                            locationName = location.name,
                            context = getApplication()
                        )
                        _syncStatus.value = "Location saved online"
                    } else {
                        throw result.exceptionOrNull() ?: Exception("Failed to save location")
                    }
                } else {
                    // Offline: Save to local DB as unsynced
                    locationDao.insertLocation(location.toLocationEntity(isSynced = false))
                    
                    // Try to create notification (will be queued if Firestore offline persistence is enabled)
                    notificationRepo.notifyLocationAdded(
                        userId = location.userId,
                        locationId = location.id,
                        locationName = location.name,
                        context = getApplication()
                    ).onFailure { e ->
                        // Notification creation failed offline - this is expected if Firestore offline persistence is not enabled
                        // The notification will be created when syncing online
                    }
                    
                    _syncStatus.value = "Location saved offline (will sync when online)"
                }

                updateUnsyncedCount()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllLocations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                checkConnectivity()

                if (_isOnline.value) {
                    // Try to download from Firebase
                    try {
                        val result = locationRepo.getAllLocations()
                        if (result.isSuccess) {
                            val firebaseLocations = result.getOrNull() ?: emptyList()
                            // Filter out locations that are marked as deleted locally or in deleted set
                            val deletedIds = _deletedLocationIds.value
                            val locationsToInsert = firebaseLocations.filter { location ->
                                !locationDao.isLocationDeleted(location.id) && location.id !in deletedIds
                            }
                            
                            // Get all existing location IDs to prevent duplicates
                            val existingIds = locationDao.getAllLocationIds().toSet()
                            
                            // Only update/insert locations from Firebase - preserve existing local unsynced locations
                            val locationsToUpdate = locationsToInsert.filter { firebaseLocation ->
                                if (firebaseLocation.id in existingIds) {
                                    val existingLocation = locationDao.getLocationByIdIncludingDeleted(firebaseLocation.id)
                                    existingLocation?.isSynced == true || existingLocation == null
                                } else {
                                    // New location - can insert (but not if it's in deleted set)
                                    firebaseLocation.id !in deletedIds
                                }
                            }
                            
                            // Remove duplicates by ID before inserting
                            val uniqueLocationsToUpdate = locationsToUpdate.distinctBy { it.id }
                            
                            if (uniqueLocationsToUpdate.isNotEmpty()) {
                                locationDao.insertLocations(uniqueLocationsToUpdate.map { it.toLocationEntity(isSynced = true) })
                            }
                        }
                    } catch (e: Exception) {
                        // If Firebase fails, we'll use local data
                    }
                }

                // Load from local DB
                val entities = locationDao.getAllPublicLocationsOnce()
                // Filter out deleted location IDs and ensure no duplicates
                val deletedIds = _deletedLocationIds.value
                _locations.value = entities
                    .map { it.toLocationData() }
                    .filter { it.id !in deletedIds }
                    .distinctBy { it.id }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUserLocations(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                checkConnectivity()

                if (_isOnline.value) {
                    // Try to download from Firebase
                    try {
                        val result = locationRepo.getUserLocations(userId)
                        if (result.isSuccess) {
                            val firebaseLocations = result.getOrNull() ?: emptyList()
                            // Filter out locations that are marked as deleted locally or in deleted set
                            val deletedIds = _deletedLocationIds.value
                            val locationsToInsert = firebaseLocations.filter { location ->
                                !locationDao.isLocationDeleted(location.id) && location.id !in deletedIds
                            }
                            
                            // Get all existing location IDs to prevent duplicates
                            val existingIds = locationDao.getAllLocationIds().toSet()
                            
                            // Only update/insert locations from Firebase - preserve existing local unsynced locations
                            val locationsToUpdate = locationsToInsert.filter { firebaseLocation ->
                                if (firebaseLocation.id in existingIds) {
                                    val existingLocation = locationDao.getLocationByIdIncludingDeleted(firebaseLocation.id)
                                    existingLocation?.isSynced == true || existingLocation == null
                                } else {
                                    // New location - can insert (but not if it's in deleted set)
                                    firebaseLocation.id !in deletedIds
                                }
                            }
                            
                            // Remove duplicates by ID before inserting
                            val uniqueLocationsToUpdate = locationsToUpdate.distinctBy { it.id }
                            
                            if (uniqueLocationsToUpdate.isNotEmpty()) {
                                locationDao.insertLocations(uniqueLocationsToUpdate.map { it.toLocationEntity(isSynced = true) })
                            }
                        }
                    } catch (e: Exception) {
                        // If Firebase fails, we'll use local data
                    }
                }

                // Load from local DB
                val entities = locationDao.getUserLocationsOnce(userId)
                // Filter out deleted location IDs and ensure no duplicates
                val deletedIds = _deletedLocationIds.value
                _locations.value = entities
                    .map { it.toLocationData() }
                    .filter { it.id !in deletedIds }
                    .distinctBy { it.id }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUserLocationsByDateRange(userId: String, startDate: Long, endDate: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                checkConnectivity()

                if (_isOnline.value) {
                    try {
                        val result = locationRepo.getUserLocationsByDateRange(userId, startDate, endDate)
                        if (result.isSuccess) {
                            val firebaseLocations = result.getOrNull() ?: emptyList()
                            // Filter out locations that are marked as deleted locally or in deleted set
                            val deletedIds = _deletedLocationIds.value
                            val locationsToInsert = firebaseLocations.filter { location ->
                                !locationDao.isLocationDeleted(location.id) && location.id !in deletedIds
                            }
                            
                            // Get all existing location IDs to prevent duplicates
                            val existingIds = locationDao.getAllLocationIds().toSet()
                            
                            // Only update/insert locations from Firebase - preserve existing local unsynced locations
                            val locationsToUpdate = locationsToInsert.filter { firebaseLocation ->
                                if (firebaseLocation.id in existingIds) {
                                    val existingLocation = locationDao.getLocationByIdIncludingDeleted(firebaseLocation.id)
                                    existingLocation?.isSynced == true || existingLocation == null
                                } else {
                                    // New location - can insert (but not if it's in deleted set)
                                    firebaseLocation.id !in deletedIds
                                }
                            }
                            
                            // Remove duplicates by ID before inserting
                            val uniqueLocationsToUpdate = locationsToUpdate.distinctBy { it.id }
                            
                            if (uniqueLocationsToUpdate.isNotEmpty()) {
                                locationDao.insertLocations(uniqueLocationsToUpdate.map { it.toLocationEntity(isSynced = true) })
                            }
                        }
                    } catch (e: Exception) {
                        // Use local data
                    }
                }

                val entities = locationDao.getFilteredLocations(userId, null, startDate, endDate)
                // Filter out deleted location IDs and ensure no duplicates
                val deletedIds = _deletedLocationIds.value
                _locations.value = entities
                    .map { it.toLocationData() }
                    .filter { it.id !in deletedIds }
                    .distinctBy { it.id }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFilteredUserLocations(userId: String, startDate: Long?, endDate: Long?, visibility: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                checkConnectivity()

                if (_isOnline.value) {
                    try {
                        val result = locationRepo.getFilteredUserLocations(userId, startDate, endDate, visibility)
                        if (result.isSuccess) {
                            val firebaseLocations = result.getOrNull() ?: emptyList()
                            // Filter out locations that are marked as deleted locally or in deleted set
                            val deletedIds = _deletedLocationIds.value
                            val locationsToInsert = firebaseLocations.filter { location ->
                                !locationDao.isLocationDeleted(location.id) && location.id !in deletedIds
                            }
                            
                            // Get all existing location IDs to prevent duplicates
                            val existingIds = locationDao.getAllLocationIds().toSet()
                            
                            // Only update/insert locations from Firebase - preserve existing local unsynced locations
                            val locationsToUpdate = locationsToInsert.filter { firebaseLocation ->
                                if (firebaseLocation.id in existingIds) {
                                    // Location exists - check if it's synced (can update) or unsynced (preserve)
                                    val existingLocation = locationDao.getLocationByIdIncludingDeleted(firebaseLocation.id)
                                    existingLocation?.isSynced == true || existingLocation == null
                                } else {
                                    // New location - can insert (but not if it's in deleted set)
                                    firebaseLocation.id !in deletedIds
                                }
                            }
                            
                            // Remove duplicates by ID before inserting
                            val uniqueLocationsToUpdate = locationsToUpdate.distinctBy { it.id }
                            
                            if (uniqueLocationsToUpdate.isNotEmpty()) {
                                locationDao.insertLocations(uniqueLocationsToUpdate.map { it.toLocationEntity(isSynced = true) })
                            }
                        }
                    } catch (e: Exception) {
                        // Use local data
                    }
                }

                // Query local database with the filter - this ensures only matching locations are returned
                // This will include both synced and unsynced locations
                val entities = locationDao.getFilteredLocations(userId, visibility, startDate, endDate)
                // Filter out deleted location IDs and ensure no duplicates
                val deletedIds = _deletedLocationIds.value
                _locations.value = entities
                    .map { it.toLocationData() }
                    .filter { it.id !in deletedIds }
                    .distinctBy { it.id }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                // Clear locations on error to avoid showing stale data
                _locations.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isLoading.value = true
            _syncStatus.value = "Syncing..."
            try {
                checkConnectivity()

                if (!_isOnline.value) {
                    _syncStatus.value = "Cannot sync: No internet connection"
                    return@launch
                }

                val result = syncManager.syncAll()
                if (result.isSuccess) {
                    val syncResult = result.getOrNull()
                    val uploaded = syncResult?.uploadedCount ?: 0
                    val deleted = syncResult?.deletedCount ?: 0
                    val failed = syncResult?.failedCount ?: 0
                    val errorMsg = syncResult?.errorMessage
                    
                    if (uploaded == 0 && deleted == 0 && failed == 0) {
                        _syncStatus.value = "Nothing to sync - all items are up to date"
                    } else {
                        val statusParts = mutableListOf<String>()
                        if (uploaded > 0) statusParts.add("$uploaded uploaded")
                        if (deleted > 0) statusParts.add("$deleted deleted")
                        if (failed > 0) statusParts.add("$failed failed")
                        var status = "Sync complete: ${statusParts.joinToString(", ")}"
                        if (errorMsg != null && failed > 0) {
                            status += " ($errorMsg)"
                        }
                        _syncStatus.value = status
                    }
                    updateUnsyncedCount()
                } else {
                    val error = result.exceptionOrNull()
                    val errorMsg = error?.message ?: "Unknown error"
                    _syncStatus.value = "Sync failed: $errorMsg"
                    _errorMessage.value = errorMsg
                    Log.e("LocationViewModel", "Sync failed", error)
                }
            } catch (e: Exception) {
                _syncStatus.value = "Sync failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            try {
                checkConnectivity()

                // Add to deleted IDs set immediately to prevent re-insertion
                _deletedLocationIds.value = _deletedLocationIds.value + locationId

                if (_isOnline.value) {
                    // Online: Try to delete from Firebase first
                    val result = locationRepo.deleteLocation(locationId)
                    if (result.isSuccess) {
                        // Successfully deleted from Firebase - hard delete from local DB
                        locationDao.deleteLocation(locationId)
                        _syncStatus.value = "Location deleted"
                    } else {
                        // Firebase deletion failed - soft delete locally for retry
                        locationDao.softDeleteLocation(locationId)
                        _syncStatus.value = "Location marked for deletion (will retry sync)"
                        // Don't throw - allow the deletion to proceed locally
                    }
                } else {
                    // Offline: Mark as deleted in local DB (soft delete for later sync)
                    locationDao.softDeleteLocation(locationId)
                    _syncStatus.value = "Location deleted locally (will sync when online)"
                }

                // Remove from current list
                _locations.value = _locations.value.filter { it.id != locationId }
                updateUnsyncedCount()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete location: ${e.message}"
                // Keep the location ID in deleted set even if deletion failed
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearLocations() {
        _locations.value = emptyList()
    }

    /**
     * Permanently delete a location from local database (hard delete)
     * Use this to remove cached locations that shouldn't exist
     */
    fun forceDeleteLocation(locationId: String) {
        viewModelScope.launch {
            try {
                // Hard delete from local database
                locationDao.deleteLocation(locationId)
                // Also try to delete from Firebase if online
                if (_isOnline.value) {
                    try {
                        locationRepo.deleteLocation(locationId)
                    } catch (e: Exception) {
                        // Ignore Firebase errors for force delete
                    }
                }
                // Remove from current list
                _locations.value = _locations.value.filter { it.id != locationId }
                _syncStatus.value = "Location permanently deleted"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to force delete location: ${e.message}"
            }
        }
    }

    /**
     * Clean up all deleted locations that have been synced
     */
    fun cleanupDeletedLocations() {
        viewModelScope.launch {
            try {
                locationDao.cleanupSyncedDeletedLocations()
                _syncStatus.value = "Cleaned up deleted locations"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to cleanup deleted locations: ${e.message}"
            }
        }
    }

    /**
     * Get all deleted locations (for debugging/cleanup)
     */
    suspend fun getDeletedLocations(): List<LocationData> {
        return try {
            val deletedEntities = locationDao.getDeletedUnsyncedLocations()
            deletedEntities.map { it.toLocationData() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
