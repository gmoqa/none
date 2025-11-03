package org.none.android.presenters

import android.content.Context
import org.none.android.models.ButtonConfig
import org.none.android.storage.StorageService
import org.none.android.audio.AudioPlayer
import org.none.android.DefaultButtonsHelper
import org.none.android.utils.toFile
import org.none.android.utils.toPlatformFile
import org.none.android.constants.ButtonConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * View contract for MainActivity
 */
interface MainView {
    fun showEditModeActivated()
    fun showUseModeActivated()
    fun updateEditModeUI(isEditMode: Boolean)
    fun updateButtonConfigs(configs: List<ButtonConfig>)
    fun showConfigureSavedMessage()
    fun showLoadingError(message: String)
    fun showConfigureFirstMessage()
    fun showMaxButtonsReached()
    fun showButtonDeleted()
    fun showDeleteError(message: String)
    fun showAudioErrorDialog(buttonId: Int)
    fun openConfigureDialog(buttonId: Int)
    fun preloadAudio(audioPath: String)
    fun playAudioFile(audioPath: String)
}

/**
 * Presenter for MainActivity - Contains all business logic
 * Platform-independent and ready for KMP migration
 */
class MainPresenter(
    private val view: MainView,
    private val storageService: StorageService,
    private val audioPlayer: AudioPlayer,
    private val context: Context, // Will be replaced with expect/actual in KMP
    private val coroutineScope: CoroutineScope
) {
    companion object {
        const val MAX_BUTTONS = 12 // Note: Differs from ButtonConstants.MAX_BUTTONS (6) due to pagination support
        const val SETTINGS_TAP_COUNT_REQUIRED = 3
        const val SETTINGS_TAP_TIMEOUT_MS = 3000L

        // Extended color palette for pagination (12 colors)
        // First 6 colors align with ButtonConstants.Colors for consistency
        private val BUTTON_COLORS = listOf(
            "#FFCCCC", "#CCE5FF", "#FFF3CC", "#D9FFD9", "#F2D9FF", "#FFE6CC", // Matches ButtonConstants
            "#A8D8EA", "#FFD3B6", "#D4A5A5", "#9FD8CB", "#C5A3E0", "#F4C87A"  // Extended colors
        )
    }

    private var isEditMode = false
    private var buttonConfigs = listOf<ButtonConfig>()
    private var settingsIconTapCount = 0
    private var lastTapTime = 0L

    /**
     * Initialize presenter - load button configurations
     */
    fun initialize() {
        loadButtonConfigurations()
    }

    /**
     * Toggle between edit and use modes
     */
    fun toggleEditMode() {
        isEditMode = !isEditMode

        if (isEditMode) {
            view.showEditModeActivated()
        } else {
            view.showUseModeActivated()
        }

        view.updateEditModeUI(isEditMode)
    }

    /**
     * Load all button configurations from storage
     */
    fun loadButtonConfigurations() {
        coroutineScope.launch {
            try {
                // Check if default buttons need to be created or recreated (language change)
                if (DefaultButtonsHelper.shouldRecreateDefaultButtons(context)) {
                    DefaultButtonsHelper.createDefaultButtons(context, storageService)
                }

                // Load all button configurations from storage
                val configs = storageService.getAllButtonConfigs()
                buttonConfigs = configs

                withContext(Dispatchers.Main) {
                    view.updateButtonConfigs(configs)

                    // Pre-load audio for instant playback
                    configs.forEach { config ->
                        if (config.audioPath.isNotEmpty() && File(config.audioPath).exists()) {
                            view.preloadAudio(config.audioPath)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view.showLoadingError(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Handle button click - either play audio or open configure dialog
     */
    fun onButtonClick(buttonId: Int) {
        if (isEditMode) {
            view.openConfigureDialog(buttonId)
        } else {
            playButtonSound(buttonId)
        }
    }

    /**
     * Play audio for a button
     */
    private fun playButtonSound(buttonId: Int) {
        val config = buttonConfigs.firstOrNull { it.buttonId == buttonId }

        if (config == null) {
            view.showConfigureFirstMessage()
            return
        }

        coroutineScope.launch {
            var attempt = 0
            val maxRetries = 3
            var audioFile: File? = null

            while (attempt < maxRetries && audioFile == null) {
                try {
                    attempt++

                    audioFile = if (config.audioPath.isNotEmpty() && File(config.audioPath).exists()) {
                        File(config.audioPath)
                    } else if (config.audioUrl.isNotEmpty()) {
                        // Download audio if doesn't exist locally (with retries)
                        val audioDir = File(context.filesDir, "audio")
                        if (!audioDir.exists()) audioDir.mkdirs()

                        // Exponential backoff: 500ms, 1000ms, 2000ms
                        if (attempt > 1) {
                            kotlinx.coroutines.delay(500L * (1 shl (attempt - 1)))
                        }

                        storageService.downloadAudio(config.audioUrl, buttonId, audioDir.toPlatformFile()).toFile()
                    } else {
                        withContext(Dispatchers.Main) {
                            view.showConfigureFirstMessage()
                        }
                        null
                    }
                } catch (e: Exception) {
                    if (attempt >= maxRetries) {
                        // All retries failed - show retry dialog
                        withContext(Dispatchers.Main) {
                            view.showAudioErrorDialog(buttonId)
                        }
                    }
                }
            }

            audioFile?.let { file ->
                withContext(Dispatchers.Main) {
                    view.playAudioFile(file.absolutePath)
                }
            }
        }
    }

    /**
     * Handle settings icon tap
     * Returns true if settings should be opened
     */
    fun onSettingsIconTap(): Boolean {
        val currentTime = System.currentTimeMillis()

        // Reset counter if timeout has passed since last tap
        if (currentTime - lastTapTime > SETTINGS_TAP_TIMEOUT_MS) {
            settingsIconTapCount = 0
        }

        lastTapTime = currentTime
        settingsIconTapCount++

        if (settingsIconTapCount >= SETTINGS_TAP_COUNT_REQUIRED) {
            settingsIconTapCount = 0
            return true
        }

        return false
    }

    /**
     * Get remaining taps needed for settings access
     */
    fun getRemainingTapsForSettings(): Int {
        return SETTINGS_TAP_COUNT_REQUIRED - settingsIconTapCount
    }

    /**
     * Handle add button click
     */
    fun onAddButtonClick() {
        // Check if max buttons reached
        if (buttonConfigs.size >= MAX_BUTTONS) {
            view.showMaxButtonsReached()
            return
        }

        // Get next available button ID
        val nextId = getNextButtonId()
        if (nextId == -1) {
            view.showMaxButtonsReached()
            return
        }

        // Open configure dialog for the new button
        view.openConfigureDialog(nextId)
    }

    /**
     * Handle delete button click
     */
    fun onDeleteButtonClick(buttonId: Int) {
        coroutineScope.launch {
            try {
                // Delete from storage
                storageService.deleteButtonConfig(buttonId)

                // Reload configurations to update UI
                loadButtonConfigurations()

                withContext(Dispatchers.Main) {
                    view.showButtonDeleted()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view.showDeleteError(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Get the next available button ID
     */
    private fun getNextButtonId(): Int {
        val usedIds = buttonConfigs.map { it.buttonId }.toSet()
        for (id in 1..MAX_BUTTONS) {
            if (id !in usedIds) {
                return id
            }
        }
        return -1 // No available ID
    }

    /**
     * Get color for a button based on its ID
     */
    fun getButtonColor(buttonId: Int): String {
        val colorIndex = (buttonId - 1) % BUTTON_COLORS.size
        return BUTTON_COLORS[colorIndex]
    }

    /**
     * Called when configuration is saved
     */
    fun onConfigurationSaved() {
        loadButtonConfigurations()
        view.showConfigureSavedMessage()
    }

    /**
     * Get current edit mode state
     */
    fun isInEditMode(): Boolean = isEditMode

    /**
     * Get current button configs
     */
    fun getButtonConfigs(): List<ButtonConfig> = buttonConfigs

    /**
     * Clean up resources
     */
    fun onDestroy() {
        audioPlayer.stopAudio()
        audioPlayer.releaseCache()
    }
}
