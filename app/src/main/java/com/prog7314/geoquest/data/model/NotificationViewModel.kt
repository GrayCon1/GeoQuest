package com.prog7314.geoquest.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog7314.geoquest.data.data.NotificationData
import com.prog7314.geoquest.data.repo.NotificationRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing notifications
 */
class NotificationViewModel : ViewModel() {

    private val notificationRepo = NotificationRepo()

    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Start listening to real-time notifications for a user
     */
    fun startListening(userId: String) {
        viewModelScope.launch {
            notificationRepo.getUserNotificationsFlow(userId).collect { notificationList ->
                _notifications.value = notificationList
                _unreadCount.value = notificationList.count { !it.isRead }
            }
        }
    }

    /**
     * Mark a notification as read
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            notificationRepo.markAsRead(notificationId)
                .onFailure { exception ->
                    _errorMessage.value = "Error marking notification as read: ${exception.message}"
                }
            _isLoading.value = false
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            notificationRepo.markAllAsRead(userId)
                .onSuccess {
                    // Notifications will update via Flow
                }
                .onFailure { exception ->
                    _errorMessage.value = "Error marking notifications as read: ${exception.message}"
                }
            _isLoading.value = false
        }
    }

    /**
     * Delete a notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepo.deleteNotification(notificationId)
                .onFailure { exception ->
                    _errorMessage.value = "Error deleting notification: ${exception.message}"
                }
        }
    }

    /**
     * Delete all notifications
     */
    fun deleteAllNotifications(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            notificationRepo.deleteAllNotifications(userId)
                .onSuccess {
                    // Notifications will update via Flow
                }
                .onFailure { exception ->
                    _errorMessage.value = "Error deleting notifications: ${exception.message}"
                }
            _isLoading.value = false
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Stop listening to notifications
     */
    override fun onCleared() {
        super.onCleared()
        // Flow will automatically stop when coroutine scope is cancelled
    }
}

