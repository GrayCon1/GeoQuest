package com.prog7314.geoquest.data.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prog7314.geoquest.data.data.UserData
import com.prog7314.geoquest.data.preferences.UserPreferences
import com.prog7314.geoquest.data.repo.UserRepo
import com.prog7314.geoquest.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepo = UserRepo(application.applicationContext)

    // UI State
    private val _currentUser = MutableStateFlow<UserData?>(null)
    val currentUser: StateFlow<UserData?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    // Connectivity + prompt state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _showGuestUpgradePrompt = MutableStateFlow(false)
    val showGuestUpgradePrompt: StateFlow<Boolean> = _showGuestUpgradePrompt.asStateFlow()

    private var guestPromptDismissed = false

    init {
        // Check if user is in guest mode from preferences
        val isGuest = UserPreferences.isGuestMode(getApplication())
        _isGuestMode.value = isGuest
        // Don't show prompt immediately on init, only after connectivity changes
        _showGuestUpgradePrompt.value = false
        observeConnectivity()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            NetworkMonitor.connectivityFlow(getApplication())
                .debounce(1500)
                .distinctUntilChanged()
                .collect { connected ->
                    val wasConnected = _isConnected.value
                    _isConnected.value = connected
                    if (connected && _isGuestMode.value && !guestPromptDismissed) {
                        // Coming online as guest -> show prompt
                        _showGuestUpgradePrompt.value = true
                    } else if (!connected) {
                        // Reset prompt visibility when offline again (but keep dismissal flag)
                        _showGuestUpgradePrompt.value = false
                    }
                    // If we were disconnected then reconnected and user had dismissed earlier, allow showing again
                    if (!wasConnected && connected && guestPromptDismissed) {
                        // A full offline -> online cycle resets dismissal for session
                        guestPromptDismissed = false
                        if (_isGuestMode.value) _showGuestUpgradePrompt.value = true
                    }
                }
        }
    }

    fun dismissGuestUpgradePrompt(permanent: Boolean = false) {
        _showGuestUpgradePrompt.value = false
        guestPromptDismissed = true
        if (permanent) {
            // Could persist preference (future enhancement)
        }
    }

    /**
     * Auto sign-in on app start
     */
    fun autoSignIn() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = userRepo.autoSignIn()
                result.onSuccess { user ->
                    _currentUser.value = user
                    _loginSuccess.value = true
                    _isGuestMode.value = UserPreferences.isGuestMode(getApplication())
                }.onFailure {
                    // Auto sign-in failed, stay on login screen
                    _loginSuccess.value = false
                }
            } catch (e: Exception) {
                // Silently fail auto sign-in
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = userRepo.signInWithGoogle(idToken)
                result.onSuccess { user ->
                    _currentUser.value = user
                    _loginSuccess.value = true
                    _errorMessage.value = null
                }.onFailure { exception ->
                    _errorMessage.value = "Google Sign-In failed: ${exception.message}"
                    _loginSuccess.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                _loginSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerUser(userData: UserData, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            userRepo.registerUser(userData, password)
                .onSuccess { registeredUser ->
                    _currentUser.value = registeredUser
                    _loginSuccess.value = true
                    _errorMessage.value = null
                    _isGuestMode.value = false // ensure leaving guest mode
                }
                .onFailure { exception ->
                    _errorMessage.value = exception.message
                }
            _isLoading.value = false
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            userRepo.loginUser(email, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _loginSuccess.value = true
                    _errorMessage.value = null
                    _isGuestMode.value = false
                    _showGuestUpgradePrompt.value = false
                }
                .onFailure { exception ->
                    _errorMessage.value = exception.message
                    _loginSuccess.value = false
                }
            _isLoading.value = false
        }
    }

    /**
     * Login as guest (offline mode)
     */
    fun loginAsGuest() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = userRepo.loginAsGuest()
                result.onSuccess { guestUser ->
                    _currentUser.value = guestUser
                    _loginSuccess.value = true
                    _isGuestMode.value = true
                    _errorMessage.value = null
                    // Guest login resets prompt state
                    guestPromptDismissed = false
                    if (_isConnected.value) {
                        _showGuestUpgradePrompt.value = true
                    }
                }.onFailure { exception ->
                    _errorMessage.value = "Failed to login as guest: ${exception.message}"
                    _loginSuccess.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                _loginSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUser(userData: UserData, currentPassword: String?, newPassword: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Update password if provided
                if (!currentPassword.isNullOrBlank() && !newPassword.isNullOrBlank()) {
                    val passwordResult = userRepo.updatePassword(currentPassword, newPassword)
                    if (passwordResult.isFailure) {
                        throw passwordResult.exceptionOrNull() ?: Exception("Failed to update password")
                    }
                }

                // Update profile
                val result = userRepo.updateUserProfile(userData)
                if (result.isSuccess) {
                    _currentUser.value = result.getOrNull()
                    _errorMessage.value = "Profile updated successfully"
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to update profile")
                }
            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("password", ignoreCase = true) == true -> "Incorrect current password"
                    e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check your connection"
                    else -> "Error: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logoutUser() {
        userRepo.logoutUser()
        _currentUser.value = null
        _loginSuccess.value = false
        _isGuestMode.value = false
        _showGuestUpgradePrompt.value = false
        guestPromptDismissed = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
