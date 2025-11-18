package com.prog7314.geoquest.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Manager for biometric authentication preferences
 */
object BiometricPreferences {

    private const val PREFS_NAME = "biometric_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_USER_EMAIL = "user_email"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Enable biometric authentication for a user
     */
    fun enableBiometric(context: Context, userEmail: String) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_BIOMETRIC_ENABLED, true)
            putString(KEY_USER_EMAIL, userEmail)
            apply()
        }
    }

    /**
     * Disable biometric authentication
     */
    fun disableBiometric(context: Context) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_BIOMETRIC_ENABLED, false)
            remove(KEY_USER_EMAIL)
            apply()
        }
    }

    /**
     * Check if biometric authentication is enabled
     */
    fun isBiometricEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    /**
     * Get the saved user email for biometric authentication
     */
    fun getSavedUserEmail(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_EMAIL, null)
    }

    /**
     * Clear all biometric preferences
     */
    fun clearAll(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}

