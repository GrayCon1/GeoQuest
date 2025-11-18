package com.prog7314.geoquest.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.prog7314.geoquest.utils.LocaleHelper

/**
 * Manager for language/locale preferences
 */
object LanguagePreferences {
    
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE_CODE = "language_code"
    private const val DEFAULT_LANGUAGE = "en" // English
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Save selected language code
     */
    fun setLanguage(context: Context, languageCode: String) {
        getPreferences(context).edit().apply {
            putString(KEY_LANGUAGE_CODE, languageCode)
            apply()
        }
    }
    
    /**
     * Get saved language code, or default if not set
     */
    fun getLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_LANGUAGE_CODE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    /**
     * Get current language display name
     */
    fun getCurrentLanguageName(context: Context): String {
        val languageCode = getLanguage(context)
        return LocaleHelper.getLanguageDisplayName(languageCode)
    }
    
    /**
     * Clear language preference (reset to default)
     */
    fun clearLanguage(context: Context) {
        getPreferences(context).edit().apply {
            remove(KEY_LANGUAGE_CODE)
            apply()
        }
    }
}

