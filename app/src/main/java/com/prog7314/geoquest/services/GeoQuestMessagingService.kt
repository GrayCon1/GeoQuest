package com.prog7314.geoquest.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.prog7314.geoquest.MainActivity
import com.prog7314.geoquest.R
import com.prog7314.geoquest.data.data.NotificationData
import com.prog7314.geoquest.data.data.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firebase Cloud Messaging Service for handling push notifications
 */
class GeoQuestMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        private const val CHANNEL_ID = "geoquest_notifications"
        private const val CHANNEL_NAME = "GeoQuest Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Save token to Firestore for the current user
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            saveFCMToken(userId, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // Handle data payload
        message.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${message.data}")
            handleDataMessage(message.data)
        }

        // Handle notification payload
        message.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(
                title = it.title ?: "GeoQuest",
                message = it.body ?: "",
                data = message.data
            )
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Save notification to Firestore
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = NotificationData(
                    userId = userId,
                    type = parseNotificationType(data["type"]),
                    title = data["title"] ?: "Notification",
                    message = data["message"] ?: "",
                    locationId = data["locationId"],
                    locationName = data["locationName"],
                    data = data
                )

                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("notifications")
                    .add(notification)
                    .await()

                Log.d(TAG, "Notification saved to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification to Firestore", e)
            }
        }

        // Show local notification
        showNotification(
            title = data["title"] ?: "GeoQuest",
            message = data["message"] ?: "",
            data = data
        )
    }

    private fun showNotification(
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_data", HashMap(data))
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // You'll need to add this
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
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

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveFCMToken(userId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .await()
                Log.d(TAG, "FCM token saved for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving FCM token", e)
            }
        }
    }

    private fun parseNotificationType(type: String?): NotificationType {
        return when (type?.uppercase()) {
            "LOCATION_ADDED" -> NotificationType.LOCATION_ADDED
            "LOCATION_NEARBY" -> NotificationType.LOCATION_NEARBY
            "ACHIEVEMENT" -> NotificationType.ACHIEVEMENT
            "POINTS_EARNED" -> NotificationType.POINTS_EARNED
            "FRIEND_ACTIVITY" -> NotificationType.FRIEND_ACTIVITY
            "SYSTEM" -> NotificationType.SYSTEM
            else -> NotificationType.GENERAL
        }
    }
}

