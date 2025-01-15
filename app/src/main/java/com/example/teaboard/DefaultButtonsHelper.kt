package com.example.teaboard

import android.content.Context
import android.util.Log
import com.example.teaboard.models.ButtonConfig
import com.example.teaboard.storage.StorageService
import java.io.File
import java.io.FileOutputStream

object DefaultButtonsHelper {

    private const val TAG = "DefaultButtonsHelper"

    /**
     * Check if this is the first launch (no buttons configured)
     */
    fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences("TeaBoardPrefs", Context.MODE_PRIVATE)
        return !prefs.getBoolean("default_buttons_created", false)
    }

    /**
     * Mark default buttons as created
     */
    fun markDefaultButtonsCreated(context: Context) {
        val prefs = context.getSharedPreferences("TeaBoardPrefs", Context.MODE_PRIVATE)
        val currentLanguage = LocaleHelper.getSavedLanguage(context)
        prefs.edit()
            .putBoolean("default_buttons_created", true)
            .putString("default_buttons_language", currentLanguage)
            .apply()
    }

    /**
     * Check if default buttons need to be recreated due to language change
     */
    fun shouldRecreateDefaultButtons(context: Context): Boolean {
        val prefs = context.getSharedPreferences("TeaBoardPrefs", Context.MODE_PRIVATE)
        val buttonsCreated = prefs.getBoolean("default_buttons_created", false)

        if (!buttonsCreated) {
            return true // First launch
        }

        // Check if language changed
        val savedButtonsLanguage = prefs.getString("default_buttons_language", "")
        val currentLanguage = LocaleHelper.getSavedLanguage(context)

        return savedButtonsLanguage != currentLanguage
    }

    /**
     * Create default button configurations
     */
    suspend fun createDefaultButtons(context: Context, storageService: StorageService) {
        // Detect current language and determine voice folder
        val currentLanguage = LocaleHelper.getSavedLanguage(context)
        val systemLanguage = if (currentLanguage.isEmpty()) {
            context.resources.configuration.locales[0].language
        } else {
            currentLanguage
        }

        // Map language code to voice folder (only for languages with audio)
        val voiceLanguage = when (systemLanguage) {
            "es" -> "ES"
            "en" -> "EN"
            "fr" -> "FR"
            "pt" -> "PT"
            "de" -> "DE"
            else -> null // No default audio for other languages
        }

        val defaultButtons = listOf(
            ButtonData(
                buttonId = 1,
                labelResId = R.string.default_button_juice,
                imageAssetFileName = "juice.png",
                audioAssetFileName = "juice.mp3",
                color = "#4A90E2" // Soft Blue
            ),
            ButtonData(
                buttonId = 2,
                labelResId = R.string.default_button_food,
                imageAssetFileName = "fruits.png",
                audioAssetFileName = "puree.mp3",
                color = "#72C604" // Sage Green
            ),
            ButtonData(
                buttonId = 3,
                labelResId = R.string.default_button_diaper,
                imageAssetFileName = "diaper.png",
                audioAssetFileName = "diaper.mp3",
                color = "#B4A7D6" // Muted Lavender
            ),
            ButtonData(
                buttonId = 4,
                labelResId = R.string.default_button_walk,
                imageAssetFileName = "park.png",
                audioAssetFileName = "park.mp3",
                color = "#BCD19E" // Mint Green
            ),
            ButtonData(
                buttonId = 5,
                labelResId = R.string.default_button_snack,
                imageAssetFileName = "cookie.png",
                audioAssetFileName = "snack.mp3",
                color = "#F4A582" // Soft Coral
            ),
            ButtonData(
                buttonId = 6,
                labelResId = R.string.default_button_school,
                imageAssetFileName = "school.png",
                audioAssetFileName = "school.mp3",
                color = "#8AB4D6" // Light Blue
            )
        )

        for (buttonData in defaultButtons) {
            val label = context.getString(buttonData.labelResId)
            val imageFile = copyImageAssetToInternalStorage(context, buttonData.buttonId, buttonData.imageAssetFileName)

            // Copy audio file if available for current language
            val audioFile = if (voiceLanguage != null) {
                copyAudioAssetToInternalStorage(
                    context,
                    buttonData.buttonId,
                    buttonData.audioAssetFileName,
                    voiceLanguage
                )
            } else {
                null
            }

            val config = ButtonConfig(
                buttonId = buttonData.buttonId,
                label = label,
                imageUrl = "", // Legacy field
                audioUrl = "", // Legacy field
                imagePath = imageFile?.absolutePath ?: "",
                audioPath = audioFile?.absolutePath ?: "",
                driveImageId = "",
                driveAudioId = "",
                backgroundColor = buttonData.color
            )

            storageService.saveButtonConfig(config)
        }

        markDefaultButtonsCreated(context)
    }

    /**
     * Copy image from assets folder to internal storage
     */
    private fun copyImageAssetToInternalStorage(context: Context, buttonId: Int, assetFileName: String): File? {
        try {
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val imageFile = File(imagesDir, "button_${buttonId}_default.png")

            // Copy from assets/images/cards/ to internal storage
            context.assets.open("images/cards/$assetFileName").use { inputStream ->
                FileOutputStream(imageFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            return imageFile
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error copying image asset: $assetFileName", e)
            }
            return null
        }
    }

    /**
     * Copy audio from assets folder to internal storage
     * @param languageCode The language folder (ES, EN, FR, etc.)
     */
    private fun copyAudioAssetToInternalStorage(
        context: Context,
        buttonId: Int,
        assetFileName: String,
        languageCode: String
    ): File? {
        try {
            val audioDir = File(context.filesDir, "audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val audioFile = File(audioDir, "button_${buttonId}_default.mp3")

            // Copy from assets/voices/{LANGUAGE}/ to internal storage
            context.assets.open("voices/$languageCode/$assetFileName").use { inputStream ->
                FileOutputStream(audioFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            return audioFile
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error copying audio asset: $assetFileName for language: $languageCode", e)
            }
            return null
        }
    }

    data class ButtonData(
        val buttonId: Int,
        val labelResId: Int,
        val imageAssetFileName: String,
        val audioAssetFileName: String,
        val color: String
    )
}
