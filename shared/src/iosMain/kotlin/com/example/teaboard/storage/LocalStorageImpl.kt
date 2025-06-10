package com.example.teaboard.storage

import com.example.teaboard.models.ButtonConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.*

/**
 * iOS implementation of local storage using NSFileManager
 *
 * TODO: Complete implementation matching Android version
 */
@OptIn(ExperimentalForeignApi::class)
class IOSLocalStorageImpl(
    private val fileProvider: FileProvider
) : ILocalStorage {

    private val json = Json { prettyPrint = true }

    override suspend fun saveImage(buttonId: Int, imageFile: PlatformFile): String {
        val destFile = fileProvider.getFile("images", "button_${buttonId}.jpg")
        fileProvider.copyFile(imageFile, destFile)
        return destFile.path
    }

    override suspend fun saveAudio(buttonId: Int, audioFile: PlatformFile): String {
        val destFile = fileProvider.getFile("audio", "button_${buttonId}.m4a")
        fileProvider.copyFile(audioFile, destFile)
        return destFile.path
    }

    override suspend fun saveButtonConfig(config: ButtonConfig) {
        val configs = getAllButtonConfigs().toMutableList()
        val existingIndex = configs.indexOfFirst { it.buttonId == config.buttonId }

        if (existingIndex >= 0) {
            configs[existingIndex] = config
        } else {
            configs.add(config)
        }

        val jsonString = json.encodeToString(configs)
        val configFile = fileProvider.getFile("", "button_configs.json")
        configFile.writeBytes(jsonString.encodeToByteArray())
    }

    override suspend fun getButtonConfig(buttonId: Int): ButtonConfig? {
        return getAllButtonConfigs().firstOrNull { it.buttonId == buttonId }
    }

    override suspend fun getAllButtonConfigs(): List<ButtonConfig> {
        return try {
            val configFile = fileProvider.getFile("", "button_configs.json")
            if (!configFile.exists()) {
                return emptyList()
            }

            val jsonString = configFile.readBytes().decodeToString()
            json.decodeFromString<List<ButtonConfig>>(jsonString)
        } catch (e: Exception) {
            println("Error loading configs: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getImageFile(buttonId: Int): PlatformFile {
        return fileProvider.getFile("images", "button_${buttonId}.jpg")
    }

    override suspend fun getAudioFile(buttonId: Int): PlatformFile {
        return fileProvider.getFile("audio", "button_${buttonId}.m4a")
    }

    override suspend fun deleteButtonConfig(buttonId: Int) {
        val configs = getAllButtonConfigs().toMutableList()
        configs.removeAll { it.buttonId == buttonId }

        val jsonString = json.encodeToString(configs)
        val configFile = fileProvider.getFile("", "button_configs.json")
        configFile.writeBytes(jsonString.encodeToByteArray())

        // Delete associated media files
        val imageFile = getImageFile(buttonId)
        if (imageFile.exists()) {
            imageFile.delete()
        }

        val audioFile = getAudioFile(buttonId)
        if (audioFile.exists()) {
            audioFile.delete()
        }
    }
}
