package com.prog7314.geoquest.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.prog7314.geoquest.data.local.GeoQuestDatabase
import com.prog7314.geoquest.data.local.toLocationData
import com.prog7314.geoquest.data.local.toLocationEntity
import com.prog7314.geoquest.data.repo.LocationRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(private val context: Context) {

    private val database = GeoQuestDatabase.getDatabase(context)
    private val locationDao = database.locationDao()
    private val locationRepo = LocationRepo()

    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Check if device has internet connectivity
     */
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Sync all unsynced data with Firebase
     */
    suspend fun syncAll(): Result<SyncResult> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            val syncResult = SyncResult()

            // 1. Upload unsynced locations
            val unsyncedLocations = locationDao.getUnsyncedLocations()
            Log.d(TAG, "Found ${unsyncedLocations.size} unsynced locations")

            unsyncedLocations.forEach { entity ->
                val result = locationRepo.addLocation(entity.toLocationData())
                if (result.isSuccess) {
                    locationDao.markAsSynced(entity.id)
                    syncResult.uploadedCount++
                    Log.d(TAG, "Successfully synced location: ${entity.name}")
                } else {
                    syncResult.failedCount++
                    Log.e(TAG, "Failed to sync location: ${entity.name}", result.exceptionOrNull())
                }
            }

            // 2. Process deleted locations
            val deletedLocations = locationDao.getDeletedUnsyncedLocations()
            Log.d(TAG, "Found ${deletedLocations.size} deleted locations to sync")

            deletedLocations.forEach { entity ->
                val result = locationRepo.deleteLocation(entity.id)
                if (result.isSuccess) {
                    locationDao.deleteLocation(entity.id) // Hard delete after sync
                    syncResult.deletedCount++
                    Log.d(TAG, "Successfully deleted location from Firebase: ${entity.name}")
                } else {
                    syncResult.failedCount++
                    Log.e(TAG, "Failed to delete location from Firebase: ${entity.name}", result.exceptionOrNull())
                }
            }

            // 3. Cleanup synced deleted locations
            locationDao.cleanupSyncedDeletedLocations()

            Log.d(TAG, "Sync completed: $syncResult")
            Result.success(syncResult)

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Download locations from Firebase and update local database
     */
    suspend fun downloadLocations(userId: String): Result<Int> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            // Download user's locations
            val result = locationRepo.getUserLocations(userId)

            if (result.isSuccess) {
                val locations = result.getOrNull() ?: emptyList()

                // Convert to entities and mark as synced
                val entities = locations.map { it.toLocationEntity(isSynced = true) }

                // Insert into local database
                locationDao.insertLocations(entities)

                Log.d(TAG, "Downloaded and saved ${entities.size} locations")
                Result.success(entities.size)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to download locations"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        }
    }

    /**
     * Download all public locations for discovery
     */
    suspend fun downloadAllPublicLocations(): Result<Int> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        try {
            val result = locationRepo.getAllLocations()

            if (result.isSuccess) {
                val locations = result.getOrNull() ?: emptyList()
                val publicLocations = locations.filter { it.visibility == "public" }

                // Convert to entities and mark as synced
                val entities = publicLocations.map { it.toLocationEntity(isSynced = true) }

                // Insert into local database
                locationDao.insertLocations(entities)

                Log.d(TAG, "Downloaded and saved ${entities.size} public locations")
                Result.success(entities.size)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to download locations"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get count of unsynced items
     */
    suspend fun getUnsyncedCount(): Int {
        return locationDao.getUnsyncedCount()
    }
}

data class SyncResult(
    var uploadedCount: Int = 0,
    var deletedCount: Int = 0,
    var failedCount: Int = 0
) {
    override fun toString(): String {
        return "Uploaded: $uploadedCount, Deleted: $deletedCount, Failed: $failedCount"
    }
}

