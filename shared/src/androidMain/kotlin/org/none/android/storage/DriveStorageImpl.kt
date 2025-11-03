package org.none.android.storage

import android.content.Context
import android.util.Log
import org.none.android.models.ButtonConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Android implementation of Google Drive storage
 */
class DriveStorageImpl(
    private val context: Context,
    private val preferences: PreferencesProvider
) : IDriveStorage {

    private var driveService: Drive? = null
    private var teaBoardFolderId: String? = null

    companion object {
        private const val TAG = "DriveStorageImpl"
        private const val FOLDER_NAME = "TeaBoard"
        private const val CONFIG_FILE_NAME = "button_configs.json"
        private const val PREF_FOLDER_ID = "teaboard_folder_id"
        private const val PREF_CONFIG_FILE_ID = "config_file_id"
        private val json = Json { prettyPrint = true }
    }

    override suspend fun initialize(): Unit = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: throw IllegalStateException("Usuario no autenticado")

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("TeaBoard")
                .build()

            // Get or create TeaBoard folder
            teaBoardFolderId = getOrCreateFolder()
            Log.d(TAG, "Drive service initialized with folder: $teaBoardFolderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Drive service: ${e.message}")
            throw e
        }
    }

    private fun getOrCreateFolder(): String {
        // Check if we have cached folder ID
        val cachedFolderId = preferences.getString(PREF_FOLDER_ID, "")
        if (cachedFolderId.isNotEmpty()) {
            try {
                // Verify folder still exists
                driveService?.files()?.get(cachedFolderId)?.execute()
                return cachedFolderId
            } catch (e: Exception) {
                Log.w(TAG, "Cached folder not found, creating new one")
            }
        }

        // Search for existing folder
        val query = "name='$FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val result = driveService?.files()?.list()
            ?.setQ(query)
            ?.setSpaces("drive")
            ?.setFields("files(id, name)")
            ?.execute()

        val folderId = if (result?.files?.isNotEmpty() == true) {
            result.files[0].id
        } else {
            // Create new folder
            val folderMetadata = DriveFile().apply {
                name = FOLDER_NAME
                mimeType = "application/vnd.google-apps.folder"
            }
            val folder = driveService?.files()?.create(folderMetadata)
                ?.setFields("id")
                ?.execute()
            folder?.id ?: throw Exception("Failed to create folder")
        }

        // Cache folder ID
        preferences.putString(PREF_FOLDER_ID, folderId)
        return folderId
    }

    override suspend fun uploadImage(buttonId: Int, imageFile: PlatformFile): String = withContext(Dispatchers.IO) {
        ensureInitialized()

        try {
            val fileName = "button_${buttonId}_${System.currentTimeMillis()}.jpg"

            val fileMetadata = DriveFile().apply {
                name = fileName
                parents = listOf(teaBoardFolderId)
            }

            // Convert PlatformFile to java.io.File
            val javaFile = (imageFile as? PlatformFile)?.file ?: File(imageFile.path)
            val mediaContent = FileContent("image/jpeg", javaFile)

            val uploadedFile = driveService?.files()?.create(fileMetadata, mediaContent)
                ?.setFields("id, webViewLink")
                ?.execute()

            val fileId = uploadedFile?.id ?: throw Exception("Failed to upload image")
            Log.d(TAG, "Image uploaded successfully: $fileId")
            fileId
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image: ${e.message}")
            throw e
        }
    }

    override suspend fun uploadAudio(buttonId: Int, audioFile: PlatformFile): String = withContext(Dispatchers.IO) {
        ensureInitialized()

        try {
            val fileName = "button_${buttonId}_${System.currentTimeMillis()}.m4a"

            val fileMetadata = DriveFile().apply {
                name = fileName
                parents = listOf(teaBoardFolderId)
            }

            // Convert PlatformFile to java.io.File
            val javaFile = (audioFile as? PlatformFile)?.file ?: File(audioFile.path)
            val mediaContent = FileContent("audio/m4a", javaFile)

            val uploadedFile = driveService?.files()?.create(fileMetadata, mediaContent)
                ?.setFields("id, webViewLink")
                ?.execute()

            val fileId = uploadedFile?.id ?: throw Exception("Failed to upload audio")
            Log.d(TAG, "Audio uploaded successfully: $fileId")
            fileId
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading audio: ${e.message}")
            throw e
        }
    }

    override suspend fun saveButtonConfig(config: ButtonConfig): Unit = withContext(Dispatchers.IO) {
        ensureInitialized()

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

            // Save to temporary file
            val tempFile = File(context.cacheDir, "temp_config.json")
            tempFile.writeText(jsonString)

            // Upload or update config file in Drive
            val configFileId = preferences.getString(PREF_CONFIG_FILE_ID, "")

            if (configFileId.isNotEmpty()) {
                // Update existing file
                val mediaContent = FileContent("application/json", tempFile)
                driveService?.files()?.update(configFileId, null, mediaContent)?.execute()
            } else {
                // Create new file
                val fileMetadata = DriveFile().apply {
                    name = CONFIG_FILE_NAME
                    parents = listOf(teaBoardFolderId)
                }
                val mediaContent = FileContent("application/json", tempFile)
                val uploadedFile = driveService?.files()?.create(fileMetadata, mediaContent)
                    ?.setFields("id")
                    ?.execute()

                uploadedFile?.id?.let { id ->
                    preferences.putString(PREF_CONFIG_FILE_ID, id)
                }
            }

            tempFile.delete()
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

    private fun loadAllConfigs(): List<ButtonConfig> {
        ensureInitialized()

        val configFileId = preferences.getString(PREF_CONFIG_FILE_ID, "")
        if (configFileId.isEmpty()) {
            return emptyList()
        }

        return try {
            val outputStream = ByteArrayOutputStream()
            driveService?.files()?.get(configFileId)?.executeMediaAndDownloadTo(outputStream)

            val jsonString = outputStream.toString("UTF-8")
            json.decodeFromString<List<ButtonConfig>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading configs: ${e.message}")
            emptyList()
        }
    }

    override suspend fun downloadImage(fileId: String, buttonId: Int, localDir: PlatformFile): PlatformFile = withContext(Dispatchers.IO) {
        ensureInitialized()

        try {
            // Convert PlatformFile to java.io.File
            val localDirFile = (localDir as? PlatformFile)?.file ?: File(localDir.path)
            val localFile = File(localDirFile, "button_${buttonId}_image.jpg")

            val outputStream = FileOutputStream(localFile)
            driveService?.files()?.get(fileId)?.executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            Log.d(TAG, "Image downloaded: ${localFile.absolutePath}")
            PlatformFile(localFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: ${e.message}")
            throw e
        }
    }

    override suspend fun downloadAudio(fileId: String, buttonId: Int, localDir: PlatformFile): PlatformFile = withContext(Dispatchers.IO) {
        ensureInitialized()

        try {
            // Convert PlatformFile to java.io.File
            val localDirFile = (localDir as? PlatformFile)?.file ?: File(localDir.path)
            val localFile = File(localDirFile, "button_${buttonId}_audio.m4a")

            val outputStream = FileOutputStream(localFile)
            driveService?.files()?.get(fileId)?.executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            Log.d(TAG, "Audio downloaded: ${localFile.absolutePath}")
            PlatformFile(localFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading audio: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteButtonConfig(buttonId: Int): Unit = withContext(Dispatchers.IO) {
        ensureInitialized()

        try {
            // Get current config to find Drive file IDs
            val config = getButtonConfig(buttonId)

            // Delete image file from Drive if exists
            if (config?.driveImageId?.isNotEmpty() == true) {
                try {
                    driveService!!.files().delete(config.driveImageId).execute()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete image from Drive: ${e.message}")
                }
            }

            // Delete audio file from Drive if exists
            if (config?.driveAudioId?.isNotEmpty() == true) {
                try {
                    driveService!!.files().delete(config.driveAudioId).execute()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete audio from Drive: ${e.message}")
                }
            }

            // Update config file (remove this button)
            val allConfigs = getAllButtonConfigs().toMutableList()
            allConfigs.removeAll { it.buttonId == buttonId }

            // Save updated config list to Drive using kotlinx.serialization
            val jsonContent = json.encodeToString(allConfigs)

            // Update the config file on Drive
            val configFileId = preferences.getString(PREF_CONFIG_FILE_ID, "")
            if (configFileId.isNotEmpty()) {
                val mediaContent = ByteArrayContent("application/json", jsonContent.toByteArray())
                driveService!!.files().update(configFileId, null, mediaContent).execute()
            }

            Log.d(TAG, "Button deleted from Drive: $buttonId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting button from Drive: ${e.message}")
            throw e
        }
    }

    private fun ensureInitialized() {
        if (driveService == null || teaBoardFolderId == null) {
            throw IllegalStateException("StorageService not initialized. Call initialize() first.")
        }
    }
}
