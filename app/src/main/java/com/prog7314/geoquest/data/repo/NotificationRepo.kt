package com.prog7314.geoquest.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.prog7314.geoquest.data.data.NotificationData
import com.prog7314.geoquest.data.data.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing notifications
 */
class NotificationRepo {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationsCollection = firestore.collection("notifications")

    companion object {
        private const val TAG = "NotificationRepo"
    }

    /**
     * Get real-time notifications for the current user
     */
    fun getUserNotificationsFlow(userId: String): Flow<List<NotificationData>> = callbackFlow {
        val listenerRegistration = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Limit to last 50 notifications
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(NotificationData::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification", e)
                        null
                    }
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Add a new notification
     */
    suspend fun addNotification(notification: NotificationData): Result<NotificationData> {
        return try {
            val docRef = notificationsCollection.document()
            val notificationWithId = notification.copy(id = docRef.id)
            docRef.set(notificationWithId).await()
            Log.d(TAG, "Notification added: ${docRef.id}")
            Result.success(notificationWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding notification", e)
            Result.failure(e)
        }
    }

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true)
                .await()
            Log.d(TAG, "Notification marked as read: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val unreadNotifications = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            unreadNotifications.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()

            Log.d(TAG, "All notifications marked as read for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Log.d(TAG, "Notification deleted: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
            Result.failure(e)
        }
    }

    /**
     * Delete all notifications for a user
     */
    suspend fun deleteAllNotifications(userId: String): Result<Unit> {
        return try {
            val notifications = notificationsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val batch = firestore.batch()
            notifications.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Log.d(TAG, "All notifications deleted for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all notifications", e)
            Result.failure(e)
        }
    }

    /**
     * Get unread notification count
     */
    suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            Result.success(snapshot.size())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
            Result.failure(e)
        }
    }

    /**
     * Create notification when location is added
     */
    suspend fun notifyLocationAdded(
        userId: String,
        locationId: String,
        locationName: String
    ): Result<Unit> {
        return try {
            val notification = NotificationData(
                userId = userId,
                type = NotificationType.LOCATION_ADDED,
                title = "Location Added",
                message = "You successfully added '$locationName'",
                locationId = locationId,
                locationName = locationName
            )
            addNotification(notification)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create notification for points earned
     */
    suspend fun notifyPointsEarned(
        userId: String,
        points: Int,
        reason: String
    ): Result<Unit> {
        return try {
            val notification = NotificationData(
                userId = userId,
                type = NotificationType.POINTS_EARNED,
                title = "Points Earned",
                message = "You earned +$points points for $reason",
                data = mapOf("points" to points.toString(), "reason" to reason)
            )
            addNotification(notification)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

