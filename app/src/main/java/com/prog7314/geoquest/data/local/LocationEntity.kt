package com.prog7314.geoquest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prog7314.geoquest.data.data.LocationData

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val imageUri: String?,
    val visibility: String,
    val dateAdded: Long,
    val isSynced: Boolean = false, // Track if synced to Firebase
    val isDeleted: Boolean = false, // For soft delete before sync
    val lastModified: Long = System.currentTimeMillis()
)

// Extension functions to convert between Entity and Data
fun LocationEntity.toLocationData(): LocationData {
    return LocationData(
        id = id,
        userId = userId,
        name = name,
        description = description,
        latitude = latitude,
        longitude = longitude,
        imageUri = imageUri,
        visibility = visibility,
        dateAdded = dateAdded
    )
}

fun LocationData.toLocationEntity(isSynced: Boolean = false): LocationEntity {
    return LocationEntity(
        id = id,
        userId = userId,
        name = name,
        description = description,
        latitude = latitude,
        longitude = longitude,
        imageUri = imageUri,
        visibility = visibility,
        dateAdded = dateAdded,
        isSynced = isSynced,
        isDeleted = false,
        lastModified = System.currentTimeMillis()
    )
}

