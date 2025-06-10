package com.example.teaboard.storage

import com.example.teaboard.models.ButtonConfig

/**
 * Interface for local storage operations
 * Platform-agnostic contract for storing button configs and media files locally
 */
interface ILocalStorage {
    /**
     * Save image file for a button
     * @return Local file identifier (path or ID)
     */
    suspend fun saveImage(buttonId: Int, imageFile: PlatformFile): String

    /**
     * Save audio file for a button
     * @return Local file identifier (path or ID)
     */
    suspend fun saveAudio(buttonId: Int, audioFile: PlatformFile): String

    /**
     * Save button configuration
     */
    suspend fun saveButtonConfig(config: ButtonConfig)

    /**
     * Get button configuration by ID
     */
    suspend fun getButtonConfig(buttonId: Int): ButtonConfig?

    /**
     * Get all button configurations
     */
    suspend fun getAllButtonConfigs(): List<ButtonConfig>

    /**
     * Get image file for a button
     */
    suspend fun getImageFile(buttonId: Int): PlatformFile

    /**
     * Get audio file for a button
     */
    suspend fun getAudioFile(buttonId: Int): PlatformFile

    /**
     * Delete button configuration and associated media
     */
    suspend fun deleteButtonConfig(buttonId: Int)
}
