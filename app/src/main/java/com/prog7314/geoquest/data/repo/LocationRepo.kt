package com.prog7314.geoquest.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.prog7314.geoquest.data.data.LocationData
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class LocationRepo {

    private val firestore = FirebaseFirestore.getInstance()
    private val locationsCollection = firestore.collection("locations")

    suspend fun addLocation(locationData: LocationData): Result<String> {
        return try {
            val docRef = locationsCollection.document()
            val locationWithId = locationData.copy(id = docRef.id)
            docRef.set(locationWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLocation(locationId: String): Result<Unit> {
        return try {
            locationsCollection.document(locationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllLocations(): Result<List<LocationData>> {
        return try {
            val snapshot = locationsCollection
                .orderBy("dateAdded", Query.Direction.DESCENDING)
                .get()
                .await()
            val locations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(LocationData::class.java)?.copy(id = doc.id)
            }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserLocations(userId: String): Result<List<LocationData>> {
        return try {
            val snapshot = locationsCollection
                .whereEqualTo("userId", userId)
                .orderBy("dateAdded", Query.Direction.DESCENDING)
                .get()
                .await()
            val locations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(LocationData::class.java)?.copy(id = doc.id)
            }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocationById(locationId: String): Result<LocationData?> {
        return try {
            val doc = locationsCollection.document(locationId).get().await()
            val location = doc.toObject(LocationData::class.java)?.copy(id = doc.id)
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get locations by user ID and date range
    suspend fun getUserLocationsByDateRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Result<List<LocationData>> {
        return try {
            val snapshot = locationsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("dateAdded", startDate)
                .whereLessThanOrEqualTo("dateAdded", endDate)
                .orderBy("dateAdded", Query.Direction.DESCENDING)
                .get()
                .await()
            val locations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(LocationData::class.java)?.copy(id = doc.id)
            }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get locations by user ID, date range, and visibility
    suspend fun getFilteredUserLocations(
        userId: String,
        startDate: Long?,
        endDate: Long?,
        visibility: String?
    ): Result<List<LocationData>> {
        return try {
            var query: Query = locationsCollection
                .whereEqualTo("userId", userId)

            if (visibility != null) {
                query = query.whereEqualTo("visibility", visibility)
            }

            if (startDate != null) {
                query = query.whereGreaterThanOrEqualTo("dateAdded", startDate)
            }

            if (endDate != null) {
                query = query.whereLessThanOrEqualTo("dateAdded", endDate)
            }

            val snapshot = query
                .orderBy("dateAdded", Query.Direction.DESCENDING)
                .get()
                .await()

            val locations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(LocationData::class.java)?.copy(id = doc.id)
            }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get public locations by date range (for discovering other users' locations)
    suspend fun getPublicLocationsByDateRange(
        startDate: Long,
        endDate: Long
    ): Result<List<LocationData>> {
        return try {
            val snapshot = locationsCollection
                .whereEqualTo("visibility", "public")
                .whereGreaterThanOrEqualTo("dateAdded", startDate)
                .whereLessThanOrEqualTo("dateAdded", endDate)
                .orderBy("dateAdded", Query.Direction.DESCENDING)
                .get()
                .await()
            val locations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(LocationData::class.java)?.copy(id = doc.id)
            }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get locations by specific date (single day)
    suspend fun getUserLocationsByDate(userId: String, date: String): Result<List<LocationData>> {
        return try {
            // Convert date string to start and end of day timestamps
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startOfDay = dateFormat.parse(date)?.time ?: 0L
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1 // End of day

            val snapshot = locationsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("dateAdded", startOfDay)
                .whereLessThanOrEqualTo("dateAdded", endOfDay)
                .orderBy("dateAdded", Query.Direction.DESCENDING)
                .get()
                .await()
            val locations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(LocationData::class.java)?.copy(id = doc.id)
            }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
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
