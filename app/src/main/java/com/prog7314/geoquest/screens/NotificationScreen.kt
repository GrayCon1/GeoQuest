package com.prog7314.geoquest.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.prog7314.geoquest.data.data.NotificationData
import com.prog7314.geoquest.data.data.NotificationType
import com.prog7314.geoquest.data.model.NotificationViewModel
import com.prog7314.geoquest.data.model.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@Preview(showBackground = true)
@Composable
fun NotificationScreenPreview() {
    NotificationScreen(rememberNavController())
}

@Composable
fun NotificationScreen(
    navController: NavController,
    userViewModel: UserViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val errorMessage by notificationViewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    // Start listening to notifications when user is available
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            notificationViewModel.startListening(user.id)
        }
    }

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            notificationViewModel.clearError()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with title and unread count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    if (unreadCount > 0) {
                        Badge(
                            containerColor = Color(0xFFE53935)
                        ) {
                            Text(
                                text = unreadCount.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mark All as Read Button
                    if (unreadCount > 0) {
                        Button(
                            onClick = {
                                currentUser?.let {
                                    notificationViewModel.markAllAsRead(it.id)
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF64B5F6)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all read",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark All Read",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Clear All Button
                    Button(
                        onClick = {
                            currentUser?.let {
                                notificationViewModel.deleteAllNotifications(it.id)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE0E0E0)),
                        enabled = !isLoading && notifications.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear all",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF757575)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear All",
                            color = Color(0xFF757575),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Loading Indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF64B5F6),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Notifications List
                if (notifications.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "No notifications",
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFFE0E0E0)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notifications yet",
                                color = Color(0xFF757575),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "You'll see updates here",
                                color = Color(0xFFBDBDBD),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = notifications,
                            key = { it.id }
                        ) { notification ->
                            NotificationCard(
                                notification = notification,
                                onMarkAsRead = {
                                    notificationViewModel.markAsRead(notification.id)
                                },
                                onDelete = {
                                    notificationViewModel.deleteNotification(notification.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun NotificationCard(
    notification: NotificationData,
    onMarkAsRead: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val icon = getIconForNotificationType(notification.type)
    val iconColor = getColorForNotificationType(notification.type)
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(notification.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!notification.isRead) {
                    onMarkAsRead()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFF5F9FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2C3E50)
                    )

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF64B5F6), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = Color(0xFFBDBDBD)
                    )

                    // Delete icon
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get icon for notification type
 */
private fun getIconForNotificationType(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.LOCATION_ADDED -> Icons.Default.LocationOn
        NotificationType.LOCATION_NEARBY -> Icons.Default.NearMe
        NotificationType.ACHIEVEMENT -> Icons.Default.EmojiEvents
        NotificationType.POINTS_EARNED -> Icons.Default.Star
        NotificationType.FRIEND_ACTIVITY -> Icons.Default.People
        NotificationType.SYSTEM -> Icons.Default.Settings
        NotificationType.GENERAL -> Icons.Default.Notifications
    }
}

/**
 * Get color for notification type
 */
private fun getColorForNotificationType(type: NotificationType): Color {
    return when (type) {
        NotificationType.LOCATION_ADDED -> Color(0xFF64B5F6)
        NotificationType.LOCATION_NEARBY -> Color(0xFF26C6DA)
        NotificationType.ACHIEVEMENT -> Color(0xFFFFD54F)
        NotificationType.POINTS_EARNED -> Color(0xFFFFB74D)
        NotificationType.FRIEND_ACTIVITY -> Color(0xFF9575CD)
        NotificationType.SYSTEM -> Color(0xFF78909C)
        NotificationType.GENERAL -> Color(0xFF64B5F6)
    }
}

