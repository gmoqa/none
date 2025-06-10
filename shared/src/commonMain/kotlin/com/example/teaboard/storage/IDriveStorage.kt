package com.example.teaboard.storage

import com.example.teaboard.models.ButtonConfig

/**
 * Interface for cloud storage operations (Google Drive, iCloud, etc.)
 * Platform-agnostic contract for cloud synchronization
 */
interface IDriveStorage {
    /**
     * Initialize cloud storage service (authenticate, etc.)
     */
    suspend fun initialize()

    /**
     * Upload image file to cloud
     * @return Cloud file identifier (Drive file ID, iCloud URL, etc.)
     */
    suspend fun uploadImage(buttonId: Int, imageFile: PlatformFile): String

    /**
     * Upload audio file to cloud
     * @return Cloud file identifier
     */
    suspend fun uploadAudio(buttonId: Int, audioFile: PlatformFile): String

    /**
     * Save button configuration to cloud
     */
    suspend fun saveButtonConfig(config: ButtonConfig)

    /**
     * Get button configuration from cloud
     */
    suspend fun getButtonConfig(buttonId: Int): ButtonConfig?

    /**
     * Get all button configurations from cloud
     */
    suspend fun getAllButtonConfigs(): List<ButtonConfig>

    /**
     * Download image from cloud to local storage
     */
    suspend fun downloadImage(fileId: String, buttonId: Int, localDir: PlatformFile): PlatformFile

    /**
     * Download audio from cloud to local storage
     */
    suspend fun downloadAudio(fileId: String, buttonId: Int, localDir: PlatformFile): PlatformFile

    /**
     * Delete button configuration from cloud
     */
    suspend fun deleteButtonConfig(buttonId: Int)
}
