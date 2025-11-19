package com.prog7314.geoquest.data.repo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.prog7314.geoquest.MainActivity
import com.prog7314.geoquest.R
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
        private const val CHANNEL_ID = "geoquest_notifications"
        private const val CHANNEL_NAME = "GeoQuest Notifications"
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
    suspend fun addNotification(notification: NotificationData, context: Context? = null): Result<NotificationData> {
        return try {
            val docRef = notificationsCollection.document()
            val notificationWithId = notification.copy(id = docRef.id)
            docRef.set(notificationWithId).await()
            Log.d(TAG, "Notification added: ${docRef.id}")
            
            // Show local notification if context is provided
            context?.let {
                showLocalNotification(it, notificationWithId)
            }
            
            Result.success(notificationWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding notification", e)
            Result.failure(e)
        }
    }
    
    /**
     * Show a local system notification
     */
    private fun showLocalNotification(context: Context, notification: NotificationData) {
        try {
            createNotificationChannel(context)
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("notification_id", notification.id)
                putExtra("notification_type", notification.type.name)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            
            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.notify(notification.id.hashCode(), notificationBuilder.build())
            Log.d(TAG, "Local notification shown: ${notification.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing local notification", e)
        }
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for GeoQuest app"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
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
        locationName: String,
        context: Context? = null
    ): Result<Unit> {
        return try {
            val notification = NotificationData(
                userId = userId,
                type = NotificationType.LOCATION_ADDED,
                title = "New Location Added",
                message = "Location '$locationName' has been added successfully",
                locationId = locationId,
                locationName = locationName
            )
            addNotification(notification, context)
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

