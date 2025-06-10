package com.example.teaboard.storage

import android.content.Context
import android.util.Log
import com.example.teaboard.models.ButtonConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android implementation of local storage
 */
class LocalStorageImpl(
    private val context: Context,
    private val fileProvider: FileProvider
) : ILocalStorage {

    private val imagesDir: PlatformFile = fileProvider.createDirectory("images")
    private val audioDir: PlatformFile = fileProvider.createDirectory("audio")
    private val configFile: PlatformFile = fileProvider.getFile("", "button_configs.json")

    companion object {
        private const val TAG = "LocalStorageImpl"
        private val json = Json { prettyPrint = true }
    }

    override suspend fun saveImage(buttonId: Int, imageFile: PlatformFile): String = withContext(Dispatchers.IO) {
        try {
            val destFile = fileProvider.getFile("images", "button_${buttonId}.jpg")
            fileProvider.copyFile(imageFile, destFile)
            Log.d(TAG, "Image saved locally for button $buttonId")
            destFile.path
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image: ${e.message}")
            throw e
        }
    }

    override suspend fun saveAudio(buttonId: Int, audioFile: PlatformFile): String = withContext(Dispatchers.IO) {
        try {
            val destFile = fileProvider.getFile("audio", "button_${buttonId}.m4a")
            fileProvider.copyFile(audioFile, destFile)
            Log.d(TAG, "Audio saved locally for button $buttonId")
            destFile.path
        } catch (e: Exception) {
            Log.e(TAG, "Error saving audio: ${e.message}")
            throw e
        }
    }

    override suspend fun saveButtonConfig(config: ButtonConfig): Unit = withContext(Dispatchers.IO) {
        try {
            // Load existing configs
            val configs = loadAllConfigs().toMutableList()

            // Update or add this config
            val existingIndex = configs.indexOfFirst { it.buttonId == config.buttonId }
            if (existingIndex >= 0) {
                configs[existingIndex] = config
            } else {
                configs.add(config)
            }

            // Convert to JSON using kotlinx.serialization
            val jsonString = json.encodeToString(configs)

            // Save to file
            configFile.writeBytes(jsonString.toByteArray())
            Log.d(TAG, "Button config saved: ${config.buttonId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving button config: ${e.message}")
            throw e
        }
    }

    override suspend fun getButtonConfig(buttonId: Int): ButtonConfig? = withContext(Dispatchers.IO) {
        try {
            val configs = loadAllConfigs()
            configs.firstOrNull { it.buttonId == buttonId }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting button config: ${e.message}")
            null
        }
    }

    override suspend fun getAllButtonConfigs(): List<ButtonConfig> = withContext(Dispatchers.IO) {
        try {
            loadAllConfigs()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all button configs: ${e.message}")
            emptyList()
        }
    }

    private suspend fun loadAllConfigs(): List<ButtonConfig> {
        if (!configFile.exists()) {
            return emptyList()
        }

        return try {
            val jsonString = String(configFile.readBytes())
            json.decodeFromString<List<ButtonConfig>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading configs: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getImageFile(buttonId: Int): PlatformFile {
        return fileProvider.getFile("images", "button_${buttonId}.jpg")
    }

    override suspend fun getAudioFile(buttonId: Int): PlatformFile {
        return fileProvider.getFile("audio", "button_${buttonId}.m4a")
    }

    override suspend fun deleteButtonConfig(buttonId: Int): Unit = withContext(Dispatchers.IO) {
        try {
            // Load all configs
            val configs = loadAllConfigs().toMutableList()

            // Remove the config for this button
            configs.removeAll { it.buttonId == buttonId }

            // Save updated config list using kotlinx.serialization
            val jsonString = json.encodeToString(configs)
            configFile.writeBytes(jsonString.toByteArray())

            // Delete associated media files
            val imageFile = getImageFile(buttonId)
            if (imageFile.exists()) {
                imageFile.delete()
            }

            val audioFile = getAudioFile(buttonId)
            if (audioFile.exists()) {
                audioFile.delete()
            }

            Log.d(TAG, "Button deleted: $buttonId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting button config: ${e.message}")
            throw e
        }
    }
}
