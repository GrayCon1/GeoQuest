package com.prog7314.geoquest.data.model

import android.app.Application
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

    init {
        checkConnectivity()
        updateUnsyncedCount()
    }

    private fun checkConnectivity() {
        _isOnline.value = syncManager.isOnline()
    }

    private fun updateUnsyncedCount() {
        viewModelScope.launch {
            _unsyncedCount.value = syncManager.getUnsyncedCount()
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
                            locationName = location.name
                        )
                        _syncStatus.value = "Location saved online"
                    } else {
                        throw result.exceptionOrNull() ?: Exception("Failed to save location")
                    }
                } else {
                    // Offline: Save to local DB as unsynced
                    locationDao.insertLocation(location.toLocationEntity(isSynced = false))
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
                            // Save to local DB
                            locationDao.insertLocations(firebaseLocations.map { it.toLocationEntity(isSynced = true) })
                        }
                    } catch (e: Exception) {
                        // If Firebase fails, we'll use local data
                    }
                }

                // Load from local DB
                val entities = locationDao.getAllPublicLocationsOnce()
                _locations.value = entities.map { it.toLocationData() }
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
                            // Save to local DB
                            locationDao.insertLocations(firebaseLocations.map { it.toLocationEntity(isSynced = true) })
                        }
                    } catch (e: Exception) {
                        // If Firebase fails, we'll use local data
                    }
                }

                // Load from local DB
                val entities = locationDao.getUserLocationsOnce(userId)
                _locations.value = entities.map { it.toLocationData() }
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
                            locationDao.insertLocations(firebaseLocations.map { it.toLocationEntity(isSynced = true) })
                        }
                    } catch (e: Exception) {
                        // Use local data
                    }
                }

                val entities = locationDao.getFilteredLocations(userId, null, startDate, endDate)
                _locations.value = entities.map { it.toLocationData() }
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
                            locationDao.insertLocations(firebaseLocations.map { it.toLocationEntity(isSynced = true) })
                        }
                    } catch (e: Exception) {
                        // Use local data
                    }
                }

                val entities = locationDao.getFilteredLocations(userId, visibility, startDate, endDate)
                _locations.value = entities.map { it.toLocationData() }
            } catch (e: Exception) {
                _errorMessage.value = e.message
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
                    _syncStatus.value = "Sync complete: ${syncResult?.uploadedCount ?: 0} uploaded, ${syncResult?.deletedCount ?: 0} deleted"
                    updateUnsyncedCount()
                } else {
                    _syncStatus.value = "Sync failed: ${result.exceptionOrNull()?.message}"
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

                if (_isOnline.value) {
                    // Online: Delete from Firebase first
                    val result = locationRepo.deleteLocation(locationId)
                    if (result.isSuccess) {
                        // Delete from local DB
                        locationDao.deleteLocation(locationId)
                        _syncStatus.value = "Location deleted"
                    } else {
                        throw result.exceptionOrNull() ?: Exception("Failed to delete location")
                    }
                } else {
                    // Offline: Mark as deleted in local DB (soft delete for later sync)
                    locationDao.deleteLocation(locationId)
                    _syncStatus.value = "Location deleted locally (will sync when online)"
                }

                // Remove from current list
                _locations.value = _locations.value.filter { it.id != locationId }
                updateUnsyncedCount()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete location: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearLocations() {
        _locations.value = emptyList()
    }
}
