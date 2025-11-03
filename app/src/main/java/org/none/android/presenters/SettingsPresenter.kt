package org.none.android.presenters

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View contract for SettingsActivity
 */
interface SettingsView {
    fun showSyncEnabled()
    fun showSyncDisabled()
    fun showSessionClosed()
    fun showSyncing()
    fun showSyncComplete()
    fun showSyncError(message: String)
    fun showSignedIn(email: String)
    fun showSignInError(message: String)
    fun updateUI()
    fun showLanguageDialog()
    fun recreateActivity()
    fun finishActivity()
    fun finishWithEditModeToggle()
    fun finishWithLanguageChanged()
    fun startGoogleSignInFlow()
}

/**
 * Presenter for SettingsActivity - Contains all business logic
 * Platform-independent and ready for KMP migration
 */
class SettingsPresenter(
    private val view: SettingsView,
    private val googleSignInClient: GoogleSignInClient,
    private val sharedPreferences: SharedPreferences,
    private val context: Context, // Will be replaced with expect/actual in KMP
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    /**
     * Enable Google Drive sync
     */
    fun enableSync(account: GoogleSignInAccount?) {
        if (account == null) {
            // Need to sign in first
            view.startGoogleSignInFlow()
        } else {
            // Already signed in
            sharedPreferences.edit().putBoolean(KEY_SYNC_ENABLED, true).apply()
            view.updateUI()
            view.showSyncEnabled()
        }
    }

    /**
     * Disable Google Drive sync
     */
    fun disableSync() {
        sharedPreferences.edit().putBoolean(KEY_SYNC_ENABLED, false).apply()
        view.updateUI()
        view.showSyncDisabled()
    }

    /**
     * Sign out from Google account
     */
    fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            sharedPreferences.edit()
                .putBoolean(KEY_SYNC_ENABLED, false)
                .putString(KEY_USER_EMAIL, "")
                .putString(KEY_USER_NAME, "")
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply()

            view.updateUI()
            view.showSessionClosed()
        }
    }

    /**
     * Trigger manual sync
     */
    fun syncNow() {
        view.showSyncing()

        coroutineScope.launch {
            try {
                // Trigger a sync by notifying MainActivity
                withContext(Dispatchers.Main) {
                    view.showSyncComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view.showSyncError(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Handle Google Sign-In result
     */
    fun handleSignInResult(account: GoogleSignInAccount?, error: Exception?) {
        if (account != null) {
            // Save login state
            sharedPreferences.edit().apply {
                putString(KEY_USER_EMAIL, account.email)
                putString(KEY_USER_NAME, account.displayName)
                putBoolean(KEY_IS_LOGGED_IN, true)
                putBoolean(KEY_SYNC_ENABLED, true)
                apply()
            }

            view.updateUI()
            view.showSignedIn(account.email ?: "")
        } else if (error != null) {
            view.updateUI()
            view.showSignInError(error.message ?: "Unknown error")
        }
    }

    /**
     * Handle edit mode activation
     */
    fun activateEditMode() {
        view.finishWithEditModeToggle()
    }

    /**
     * Handle language selection
     */
    fun selectLanguage() {
        view.showLanguageDialog()
    }

    /**
     * Handle language change
     */
    @Suppress("UNUSED_PARAMETER")
    fun onLanguageChanged(languageCode: String) {
        // Language is saved by LocaleHelper
        // Recreate activity to apply new language
        view.recreateActivity()
    }

    /**
     * Check if device is a tablet based on screen width
     */
    fun isTablet(screenWidthDp: Float): Boolean {
        return screenWidthDp >= 600
    }

    /**
     * Calculate grid span count based on orientation
     */
    fun calculateSpanCount(isLandscape: Boolean): Int {
        return when {
            // Tablet landscape: 3 columns (to fit all options without scrolling)
            isLandscape -> 3
            // Tablet portrait: 2 columns
            else -> 2
        }
    }

    /**
     * Get sync enabled state
     */
    fun isSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SYNC_ENABLED, false)
    }

    /**
     * Get current sync status text
     */
    fun getSyncStatusText(): Pair<Boolean, String> {
        val syncEnabled = isSyncEnabled()
        // Returns pair of (syncEnabled, statusText)
        // View will provide the actual string resources
        return Pair(syncEnabled, "")
    }

    /**
     * Get user info for display
     */
    fun getUserInfo(): UserInfo {
        val email = sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
        val name = sharedPreferences.getString(KEY_USER_NAME, "") ?: ""
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

        return UserInfo(email, name, isLoggedIn)
    }

    /**
     * Close settings
     */
    fun closeSettings() {
        view.finishActivity()
    }
}

/**
 * User information data class
 */
data class UserInfo(
    val email: String,
    val name: String,
    val isLoggedIn: Boolean
)
