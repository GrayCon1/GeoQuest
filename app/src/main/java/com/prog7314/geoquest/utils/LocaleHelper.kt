package com.prog7314.geoquest.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * Helper utility to manage app locale/language changes
 */
object LocaleHelper {
    
    private const val LANGUAGE_ENGLISH = "en"
    private const val LANGUAGE_AFRIKAANS = "af"
    
    /**
     * Set the app locale based on language code
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode.lowercase()) {
            LANGUAGE_AFRIKAANS -> Locale.Builder().setLanguage("af").setRegion("ZA").build() // Afrikaans (South Africa)
            LANGUAGE_ENGLISH -> Locale.Builder().setLanguage("en").setRegion("US").build() // English (US)
            else -> Locale.getDefault()
        }
        
        return updateResources(context, locale)
    }
    
    /**
     * Get the current language code from context
     */
    fun getLanguageCode(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        return locale.language
    }
    
    /**
     * Update resources with new locale
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
    
    /**
     * Get display name for language code
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode.lowercase()) {
            LANGUAGE_ENGLISH -> "English"
            LANGUAGE_AFRIKAANS -> "Afrikaans"
            else -> "English"
        }
    }
    
    /**
     * Get all supported language codes
     */
    fun getSupportedLanguages(): List<String> {
        return listOf(LANGUAGE_ENGLISH, LANGUAGE_AFRIKAANS)
    }
    
    /**
     * Get all supported language display names
     */
    fun getSupportedLanguageNames(): List<String> {
        return listOf("English", "Afrikaans")
    }
}

