package com.prog7314.geoquest.data.repo

import android.content.Context
import android.util.Log
import com.prog7314.geoquest.data.api.ApiRepository
import com.prog7314.geoquest.data.data.LocationData
import java.text.SimpleDateFormat
import java.util.*

class LocationRepo(private val context: Context? = null) {

    private val apiRepository = context?.let { ApiRepository(it) }
    
    companion object {
        private const val TAG = "LocationRepo"
    }

    suspend fun addLocation(locationData: LocationData): Result<String> {
        return if (apiRepository != null) {
            try {
                val result = apiRepository.createLocation(locationData)
                result.map { it.id }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding location via API", e)
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Context not available for API calls"))
        }
    }

    suspend fun deleteLocation(locationId: String): Result<Unit> {
        return if (apiRepository != null) {
            try {
                apiRepository.deleteLocation(locationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting location via API", e)
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Context not available for API calls"))
        }
    }

    suspend fun getAllLocations(): Result<List<LocationData>> {
        return if (apiRepository != null) {
            try {
                apiRepository.getAllLocations()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting all locations via API", e)
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Context not available for API calls"))
        }
    }

    suspend fun getUserLocations(userId: String): Result<List<LocationData>> {
        return if (apiRepository != null) {
            try {
                apiRepository.getUserLocations()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user locations via API", e)
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Context not available for API calls"))
        }
    }

    suspend fun getLocationById(locationId: String): Result<LocationData?> {
        return if (apiRepository != null) {
            try {
                apiRepository.getLocationById(locationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location by ID via API", e)
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Context not available for API calls"))
        }
    }

    // Get locations by user ID and date range
    suspend fun getUserLocationsByDateRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Result<List<LocationData>> {
        return if (apiRepository != null) {
            try {
                apiRepository.getFilteredLocations(
                    visibility = null,
                    startDate = startDate,
                    endDate = endDate
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting locations by date range via API", e)
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Context not available for API calls"))
        }
    }

    // Get locations by user ID, date range, and visibility
    suspend fun getFilteredUserLocations(
        userId: String,
        startDate: Long?,
        endDate: Long?,
        visibility: String?
    ): Result<List<LocationData>> {
        return if (apiRepository != null) {
            try {
                apiRepository.getFilteredLocations(
                    visibility = visibility,
                    startDate = startDate,
                    endDate = endDate
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting filtered locations via API", e)
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Context not available for API calls"))
        }
    }

    // Get public locations by date range (for discovering other users' locations)
    suspend fun getPublicLocationsByDateRange(
        startDate: Long,
        endDate: Long
    ): Result<List<LocationData>> {
        return if (apiRepository != null) {
            try {
                // Get all locations and filter for public ones
                val allLocations = apiRepository.getAllLocations().getOrElse { emptyList() }
                val filtered = allLocations.filter { location ->
                    location.visibility == "public" &&
                    location.dateAdded >= startDate &&
                    location.dateAdded <= endDate
                }
                Result.success(filtered.sortedByDescending { it.dateAdded })
            } catch (e: Exception) {
                Log.e(TAG, "Error getting public locations by date range via API", e)
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Context not available for API calls"))
        }
    }

    // Get locations by specific date (single day)
    suspend fun getUserLocationsByDate(userId: String, date: String): Result<List<LocationData>> {
        return if (apiRepository != null) {
            try {
                // Convert date string to start and end of day timestamps
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startOfDay = dateFormat.parse(date)?.time ?: 0L
                val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1 // End of day

                apiRepository.getFilteredLocations(
                    visibility = null,
                    startDate = startOfDay,
                    endDate = endOfDay
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting locations by date via API", e)
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Context not available for API calls"))
        }
    }

    // Helper function to convert date string to timestamp
    fun dateStringToTimestamp(dateString: String): Long {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFormat.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    // Helper function to get start of day timestamp
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Helper function to get end of day timestamp
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}
