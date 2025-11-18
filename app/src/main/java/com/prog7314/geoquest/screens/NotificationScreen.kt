package com.prog7314.geoquest.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

data class Notification(
    val id: Int,
    val type: NotificationType,
    val title: String,
    val message: String,
    val icon: ImageVector
)

enum class NotificationType {
    LOCATION, POINTS
}

@Preview(showBackground = true)
@Composable
fun NotificationScreenPreview() {
    NotificationScreen(rememberNavController())
}

@Composable
fun NotificationScreen(navController: NavController) {
    var notifications by remember {
        mutableStateOf(
            listOf(
                Notification(
                    1,
                    NotificationType.LOCATION,
                    "Location:",
                    "Location 1 has been added!",
                    Icons.Default.LocationOn
                ),
                Notification(
                    2,
                    NotificationType.POINTS,
                    "Points:",
                    "You have earned +5 points",
                    Icons.Default.Star
                ),
                Notification(
                    3,
                    NotificationType.LOCATION,
                    "Location:",
                    "Location 2 has been added!",
                    Icons.Default.LocationOn
                ),
                Notification(
                    4,
                    NotificationType.POINTS,
                    "Points:",
                    "You have earned +5 points",
                    Icons.Default.Star
                )
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clickable { /* Prevent clicks from passing through to the background */ },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Clear Notification Button
            Button(
                onClick = { notifications = emptyList() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4A90E2))
            ) {
                Text(
                    text = "Clear All Notifications",
                    color = Color(0xFF4A90E2),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications List
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notifications",
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(notification = notification)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = notification.icon,
                contentDescription = notification.title,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF2C3E50)
            )
        }
    }
}
