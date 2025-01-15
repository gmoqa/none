package com.example.teaboard

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "TeaBoardPrefs"
    private const val KEY_LANGUAGE = "app_language"

    /**
     * Available language codes (only officially supported languages with voice recordings)
     */
    object Languages {
        const val SYSTEM_DEFAULT = ""
        const val ENGLISH = "en"
        const val SPANISH = "es"
        const val FRENCH = "fr"
        const val PORTUGUESE = "pt"
        const val GERMAN = "de"
    }

    /**
     * Apply the saved language preference
     */
    fun applyLanguage(context: Context): Context {
        val languageCode = getSavedLanguage(context)
        return if (languageCode.isEmpty()) {
            // Use system default
            context
        } else {
            updateResources(context, languageCode)
        }
    }

    /**
     * Save language preference
     */
    fun saveLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    /**
     * Get saved language
     */
    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, Languages.SYSTEM_DEFAULT) ?: Languages.SYSTEM_DEFAULT
    }

    /**
     * Get language display name for current locale
     */
    fun getLanguageDisplayName(context: Context, languageCode: String): String {
        return when (languageCode) {
            Languages.SYSTEM_DEFAULT -> context.getString(R.string.lang_system)
            Languages.ENGLISH -> context.getString(R.string.lang_en)
            Languages.SPANISH -> context.getString(R.string.lang_es)
            Languages.FRENCH -> context.getString(R.string.lang_fr)
            Languages.PORTUGUESE -> context.getString(R.string.lang_pt)
            Languages.GERMAN -> context.getString(R.string.lang_de)
            else -> context.getString(R.string.lang_system)
        }
    }

    /**
     * Get language display name with flag emoji
     */
    fun getLanguageDisplayNameWithFlag(context: Context, languageCode: String): String {
        return when (languageCode) {
            Languages.SYSTEM_DEFAULT -> context.getString(R.string.lang_system)
            Languages.ENGLISH -> "${context.getString(R.string.lang_en)} 🇺🇸"
            Languages.SPANISH -> "${context.getString(R.string.lang_es)} 🇪🇸"
            Languages.FRENCH -> "${context.getString(R.string.lang_fr)} 🇫🇷"
            Languages.PORTUGUESE -> "${context.getString(R.string.lang_pt)} 🇵🇹"
            Languages.GERMAN -> "${context.getString(R.string.lang_de)} 🇩🇪"
            else -> context.getString(R.string.lang_system)
        }
    }

    /**
     * Get all available languages (only officially supported with voice recordings)
     */
    fun getAvailableLanguages(): List<Pair<String, Int>> {
        return listOf(
            Languages.SYSTEM_DEFAULT to R.string.lang_system,
            Languages.ENGLISH to R.string.lang_en,
            Languages.SPANISH to R.string.lang_es,
            Languages.FRENCH to R.string.lang_fr,
            Languages.PORTUGUESE to R.string.lang_pt,
            Languages.GERMAN to R.string.lang_de
        )
    }

    /**
     * Get flag asset filename for language code
     * Returns null for system default or unsupported languages
     */
    fun getFlagAsset(languageCode: String): String? {
        return when (languageCode) {
            Languages.ENGLISH -> "flags/EN.png"
            Languages.SPANISH -> "flags/ES.png"
            Languages.FRENCH -> "flags/FR.png"
            Languages.PORTUGUESE -> "flags/PT.png"
            Languages.GERMAN -> "flags/DE.png"
            else -> null // No flag for system default
        }
    }

    /**
     * Update app resources with new locale
     */
    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}
