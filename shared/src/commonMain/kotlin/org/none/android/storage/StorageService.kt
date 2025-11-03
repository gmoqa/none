package org.none.android.storage

import org.none.android.models.ButtonConfig

/**
 * Unified storage service for multiplatform code
 * Delegates to either local or cloud storage based on sync settings
 */
class StorageService(
    private val localStorage: ILocalStorage,
    private val driveStorage: IDriveStorage?,
    private val preferences: PreferencesProvider
) {
    companion object {
        private const val KEY_SYNC_ENABLED = "sync_enabled"
    }

    private fun isSyncEnabled(): Boolean {
        return preferences.getBoolean(KEY_SYNC_ENABLED, false) && driveStorage != null
    }

    /**
     * Initialize cloud storage if sync is enabled
     */
    suspend fun initialize() {
        if (isSyncEnabled()) {
            driveStorage?.initialize()
        }
    }

    /**
     * Upload image - uses cloud if sync enabled, otherwise saves locally
     */
    suspend fun uploadImage(buttonId: Int, imageFile: PlatformFile): String {
        return if (isSyncEnabled()) {
            try {
                driveStorage!!.uploadImage(buttonId, imageFile)
            } catch (e: Exception) {
                // Fallback to local
                localStorage.saveImage(buttonId, imageFile)
            }
        } else {
            localStorage.saveImage(buttonId, imageFile)
        }
    }

    /**
     * Upload audio - uses cloud if sync enabled, otherwise saves locally
     */
    suspend fun uploadAudio(buttonId: Int, audioFile: PlatformFile): String {
        return if (isSyncEnabled()) {
            try {
                driveStorage!!.uploadAudio(buttonId, audioFile)
            } catch (e: Exception) {
                // Fallback to local
                localStorage.saveAudio(buttonId, audioFile)
            }
        } else {
            localStorage.saveAudio(buttonId, audioFile)
        }
    }

    /**
     * Save button configuration
     */
    suspend fun saveButtonConfig(config: ButtonConfig) {
        // Always save locally first
        localStorage.saveButtonConfig(config)

        // Also save to cloud if sync is enabled
        if (isSyncEnabled()) {
            try {
                driveStorage!!.saveButtonConfig(config)
            } catch (e: Exception) {
                // Ignore cloud errors - local save succeeded
            }
        }
    }

    /**
     * Get button configuration
     */
    suspend fun getButtonConfig(buttonId: Int): ButtonConfig? {
        // Try cloud first if sync is enabled
        if (isSyncEnabled()) {
            try {
                val config = driveStorage!!.getButtonConfig(buttonId)
                if (config != null) {
                    // Cache it locally
                    localStorage.saveButtonConfig(config)
                    return config
                }
            } catch (e: Exception) {
                // Fall through to local
            }
        }

        // Fall back to local
        return localStorage.getButtonConfig(buttonId)
    }

    /**
     * Get all button configurations
     */
    suspend fun getAllButtonConfigs(): List<ButtonConfig> {
        // Try cloud first if sync is enabled
        if (isSyncEnabled()) {
            try {
                val configs = driveStorage!!.getAllButtonConfigs()
                if (configs.isNotEmpty()) {
                    // Cache them locally
                    configs.forEach { localStorage.saveButtonConfig(it) }
                    return configs
                }
            } catch (e: Exception) {
                // Fall through to local
            }
        }

        // Fall back to local
        return localStorage.getAllButtonConfigs()
    }

    /**
     * Download audio file (for cloud mode, downloads from cloud to local cache)
     */
    suspend fun downloadAudio(fileId: String, buttonId: Int, localDir: PlatformFile): PlatformFile {
        return if (isSyncEnabled() && fileId.isNotEmpty()) {
            try {
                driveStorage!!.downloadAudio(fileId, buttonId, localDir)
            } catch (e: Exception) {
                localStorage.getAudioFile(buttonId)
            }
        } else {
            localStorage.getAudioFile(buttonId)
        }
    }

    /**
     * Download image file (for cloud mode, downloads from cloud to local cache)
     */
    suspend fun downloadImage(fileId: String, buttonId: Int, localDir: PlatformFile): PlatformFile {
        return if (isSyncEnabled() && fileId.isNotEmpty()) {
            try {
                driveStorage!!.downloadImage(fileId, buttonId, localDir)
            } catch (e: Exception) {
                localStorage.getImageFile(buttonId)
            }
        } else {
            localStorage.getImageFile(buttonId)
        }
    }

    /**
     * Delete button configuration and associated media
     */
    suspend fun deleteButtonConfig(buttonId: Int) {
        // Always delete locally
        localStorage.deleteButtonConfig(buttonId)

        // Also delete from cloud if sync is enabled
        if (isSyncEnabled()) {
            try {
                driveStorage!!.deleteButtonConfig(buttonId)
            } catch (e: Exception) {
                // Ignore cloud errors - local deletion succeeded
            }
        }
    }
}
