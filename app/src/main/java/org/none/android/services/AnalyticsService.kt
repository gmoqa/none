package org.none.android.services

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

/**
 * Analytics service for TeaBoard app
 * Tracks user interactions and app usage patterns
 */
class AnalyticsService(context: Context) {

    private val analytics: FirebaseAnalytics = Firebase.analytics

    // Enable/disable analytics collection
    fun setAnalyticsEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    // ============ Language Events ============

    fun logLanguageSelected(languageCode: String) {
        analytics.logEvent("language_selected") {
            param("language", languageCode)
        }
    }

    // ============ Button Configuration Events ============

    fun logButtonConfigured(buttonId: Int, hasImage: Boolean, hasAudio: Boolean) {
        analytics.logEvent("button_configured") {
            param("button_id", buttonId.toLong())
            param("has_image", if (hasImage) "yes" else "no")
            param("has_audio", if (hasAudio) "yes" else "no")
        }
    }

    fun logImageCaptured(buttonId: Int, source: String) {
        analytics.logEvent("image_captured") {
            param("button_id", buttonId.toLong())
            param("source", source) // "camera" or "gallery"
        }
    }

    fun logAudioRecorded(buttonId: Int, durationSeconds: Int) {
        analytics.logEvent("audio_recorded") {
            param("button_id", buttonId.toLong())
            param("duration_seconds", durationSeconds.toLong())
        }
    }

    // ============ Button Usage Events ============

    fun logButtonPressed(buttonId: Int) {
        analytics.logEvent("button_pressed") {
            param("button_id", buttonId.toLong())
        }
    }

    fun logModeToggled(newMode: String) {
        analytics.logEvent("mode_toggled") {
            param("mode", newMode) // "edit" or "use"
        }
    }

    // ============ Google Drive Sync Events ============

    fun logDriveSyncEnabled() {
        analytics.logEvent("drive_sync_enabled") {
            param("feature", "google_drive")
        }
    }

    fun logDriveSyncDisabled() {
        analytics.logEvent("drive_sync_disabled") {
            param("feature", "google_drive")
        }
    }

    fun logDriveSyncSuccess(itemType: String) {
        analytics.logEvent("drive_sync_success") {
            param("item_type", itemType) // "config", "image", "audio"
        }
    }

    fun logDriveSyncError(errorType: String) {
        analytics.logEvent("drive_sync_error") {
            param("error_type", errorType)
        }
    }

    // ============ Settings Events ============

    fun logSettingsOpened() {
        analytics.logEvent("settings_opened") {
            param("screen", "settings")
        }
    }

    fun logDataCleared() {
        analytics.logEvent("data_cleared") {
            param("action", "clear_all_data")
        }
    }

    // ============ App Lifecycle Events ============

    fun logAppOpened() {
        analytics.logEvent("app_opened") {
            param("screen", "main")
        }
    }

    fun logSessionDuration(durationMinutes: Long) {
        analytics.logEvent("session_duration") {
            param("duration_minutes", durationMinutes)
        }
    }

    // ============ Screen Tracking ============

    fun logScreenView(screenName: String, screenClass: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }

    // ============ Error Tracking ============

    fun logError(errorMessage: String, errorContext: String) {
        analytics.logEvent("app_error") {
            param("error_message", errorMessage)
            param("error_context", errorContext)
        }
    }

    // ============ User Properties ============

    fun setUserProperty(propertyName: String, propertyValue: String) {
        analytics.setUserProperty(propertyName, propertyValue)
    }

    fun setButtonsConfiguredCount(count: Int) {
        setUserProperty("buttons_configured", count.toString())
    }

    fun setUsesGoogleDrive(usesGoogleDrive: Boolean) {
        setUserProperty("uses_google_drive", if (usesGoogleDrive) "yes" else "no")
    }

    companion object {
        // Singleton instance
        @Volatile
        private var instance: AnalyticsService? = null

        fun getInstance(context: Context): AnalyticsService {
            return instance ?: synchronized(this) {
                instance ?: AnalyticsService(context.applicationContext).also { instance = it }
            }
        }
    }
}
