package com.prog7314.geoquest.data.data

import com.google.firebase.Timestamp

/**
 * Data class representing a notification
 */
data class NotificationData(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val title: String = "",
    val message: String = "",
    val locationId: String? = null,
    val locationName: String? = null,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val data: Map<String, String> = emptyMap()
)

/**
 * Types of notifications
 */
enum class NotificationType {
    GENERAL,           // General app notifications
    LOCATION_ADDED,    // New location added
    LOCATION_NEARBY,   // User is near a saved location
    ACHIEVEMENT,       // Achievement unlocked
    POINTS_EARNED,     // Points earned
    FRIEND_ACTIVITY,   // Friend added a location
    SYSTEM            // System notifications
}

