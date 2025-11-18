package com.prog7314.geoquest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    // Insert or update location
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    // Get all locations for a user (with DISTINCT to prevent duplicates)
    @Query("SELECT DISTINCT * FROM locations WHERE userId = :userId AND isDeleted = 0 ORDER BY dateAdded DESC")
    fun getUserLocations(userId: String): Flow<List<LocationEntity>>

    @Query("SELECT DISTINCT * FROM locations WHERE userId = :userId AND isDeleted = 0 ORDER BY dateAdded DESC")
    suspend fun getUserLocationsOnce(userId: String): List<LocationEntity>

    // Get all public locations (with DISTINCT to prevent duplicates)
    @Query("SELECT DISTINCT * FROM locations WHERE visibility = 'public' AND isDeleted = 0 ORDER BY dateAdded DESC")
    fun getAllPublicLocations(): Flow<List<LocationEntity>>

    @Query("SELECT DISTINCT * FROM locations WHERE visibility = 'public' AND isDeleted = 0 ORDER BY dateAdded DESC")
    suspend fun getAllPublicLocationsOnce(): List<LocationEntity>

    // Get unsynced locations (for sync)
    @Query("SELECT * FROM locations WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedLocations(): List<LocationEntity>

    // Get deleted but unsynced locations
    @Query("SELECT * FROM locations WHERE isDeleted = 1 AND isSynced = 0")
    suspend fun getDeletedUnsyncedLocations(): List<LocationEntity>

    // Get filtered locations (with DISTINCT to prevent duplicates)
    @Query("""
        SELECT DISTINCT * FROM locations 
        WHERE userId = :userId 
        AND isDeleted = 0
        AND (:visibility IS NULL OR visibility = :visibility)
        AND (:startDate IS NULL OR dateAdded >= :startDate)
        AND (:endDate IS NULL OR dateAdded <= :endDate)
        ORDER BY dateAdded DESC
    """)
    suspend fun getFilteredLocations(
        userId: String,
        visibility: String?,
        startDate: Long?,
        endDate: Long?
    ): List<LocationEntity>

    // Get location by ID (only non-deleted)
    @Query("SELECT * FROM locations WHERE id = :locationId AND isDeleted = 0")
    suspend fun getLocationById(locationId: String): LocationEntity?

    // Get location by ID (including deleted, for conflict checking)
    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationByIdIncludingDeleted(locationId: String): LocationEntity?

    // Get all existing location IDs (for duplicate checking)
    @Query("SELECT id FROM locations WHERE isDeleted = 0")
    suspend fun getAllLocationIds(): List<String>

    // Check if location is marked as deleted
    @Query("SELECT COUNT(*) > 0 FROM locations WHERE id = :locationId AND isDeleted = 1")
    suspend fun isLocationDeleted(locationId: String): Boolean

    // Mark location as synced
    @Query("UPDATE locations SET isSynced = 1 WHERE id = :locationId")
    suspend fun markAsSynced(locationId: String)

    // Soft delete location
    @Query("UPDATE locations SET isDeleted = 1, isSynced = 0, lastModified = :timestamp WHERE id = :locationId")
    suspend fun softDeleteLocation(locationId: String, timestamp: Long = System.currentTimeMillis())

    // Hard delete location (after sync)
    @Query("DELETE FROM locations WHERE id = :locationId")
    suspend fun deleteLocation(locationId: String)

    // Delete all synced and deleted locations
    @Query("DELETE FROM locations WHERE isDeleted = 1 AND isSynced = 1")
    suspend fun cleanupSyncedDeletedLocations()

    // Get sync status
    @Query("SELECT COUNT(*) FROM locations WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    // Clear all locations (for logout/testing)
    @Query("DELETE FROM locations")
    suspend fun clearAllLocations()
}

