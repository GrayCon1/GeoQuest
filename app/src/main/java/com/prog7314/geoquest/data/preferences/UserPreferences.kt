package com.prog7314.geoquest.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.prog7314.geoquest.data.data.UserData

/**
 * Manages user preferences and cached credentials for offline login
 */
object UserPreferences {

    private const val PREFS_NAME = "user_prefs"
    private const val ENCRYPTED_PREFS_NAME = "encrypted_user_prefs"

    // Keys
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_USERNAME = "user_username"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_CACHED_PASSWORD = "cached_password"
    private const val KEY_IS_GUEST = "is_guest"
    private const val KEY_AUTO_SIGN_IN = "auto_sign_in"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Save user data for offline access
     */
    fun saveUserData(context: Context, userData: UserData, password: String? = null) {
        getPreferences(context).edit().apply {
            putString(KEY_USER_ID, userData.id)
            putString(KEY_USER_NAME, userData.name)
            putString(KEY_USER_USERNAME, userData.username)
            putString(KEY_USER_EMAIL, userData.email)
            putBoolean(KEY_IS_GUEST, false)
            apply()
        }

        // Save password in encrypted prefs if provided
        if (password != null) {
            getEncryptedPreferences(context).edit().apply {
                putString(KEY_CACHED_PASSWORD, password)
                apply()
            }
        }
    }

    /**
     * Get cached user data
     */
    fun getCachedUserData(context: Context): UserData? {
        val prefs = getPreferences(context)
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, null) ?: return null
        val username = prefs.getString(KEY_USER_USERNAME, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null

        return UserData(
            id = id,
            name = name,
            username = username,
            email = email
        )
    }

    /**
     * Get cached password (encrypted)
     */
    fun getCachedPassword(context: Context): String? {
        return try {
            getEncryptedPreferences(context).getString(KEY_CACHED_PASSWORD, null)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if user credentials are cached
     */
    fun hasCredentialsCached(context: Context): Boolean {
        val userData = getCachedUserData(context)
        val password = getCachedPassword(context)
        return userData != null && password != null
    }

    /**
     * Set guest mode
     */
    fun setGuestMode(context: Context, isGuest: Boolean) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_IS_GUEST, isGuest)
            if (isGuest) {
                putString(KEY_USER_ID, "guest_${System.currentTimeMillis()}")
                putString(KEY_USER_NAME, "Guest User")
                putString(KEY_USER_USERNAME, "guest")
                putString(KEY_USER_EMAIL, "guest@geoquest.local")
            }
            apply()
        }
    }

    /**
     * Check if user is in guest mode
     */
    fun isGuestMode(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_GUEST, false)
    }

    /**
     * Enable/disable auto sign-in
     */
    fun setAutoSignIn(context: Context, enabled: Boolean) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_AUTO_SIGN_IN, enabled)
            apply()
        }
    }

    /**
     * Check if auto sign-in is enabled
     */
    fun isAutoSignInEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_AUTO_SIGN_IN, true)
    }

    /**
     * Clear all user data and credentials
     */
    fun clearAll(context: Context) {
        getPreferences(context).edit().clear().apply()
        try {
            getEncryptedPreferences(context).edit().clear().apply()
        } catch (e: Exception) {
            // Ignore if encrypted prefs don't exist
        }
    }

    /**
     * Clear only password (keep user data)
     */
    fun clearPassword(context: Context) {
        try {
            getEncryptedPreferences(context).edit().apply {
                remove(KEY_CACHED_PASSWORD)
                apply()
            }
        } catch (e: Exception) {
            // Ignore if encrypted prefs don't exist
        }
    }
}

