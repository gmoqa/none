package com.example.teaboard.storage

import com.example.teaboard.models.ButtonConfig

/**
 * iOS implementation of Google Drive storage
 *
 * TODO: Implement Google Drive API integration for iOS
 * This requires setting up Google Sign-In for iOS and Google Drive iOS SDK
 */
class IOSDriveStorageImpl(
    private val preferences: PreferencesProvider
) : IDriveStorage {

    override suspend fun initialize() {
        // TODO: Initialize Google Drive API for iOS
        println("Warning: Google Drive not yet implemented for iOS")
    }

    override suspend fun uploadImage(buttonId: Int, imageFile: PlatformFile): String {
        // TODO: Implement Drive upload for iOS
        return ""
    }

    override suspend fun uploadAudio(buttonId: Int, audioFile: PlatformFile): String {
        // TODO: Implement Drive upload for iOS
        return ""
    }

    override suspend fun saveButtonConfig(config: ButtonConfig) {
        // TODO: Implement Drive save for iOS
    }

    override suspend fun getButtonConfig(buttonId: Int): ButtonConfig? {
        // TODO: Implement Drive get for iOS
        return null
    }

    override suspend fun getAllButtonConfigs(): List<ButtonConfig> {
        // TODO: Implement Drive getAll for iOS
        return emptyList()
    }

    override suspend fun downloadImage(fileId: String, buttonId: Int, localDir: PlatformFile): PlatformFile {
        // TODO: Implement Drive download for iOS
        throw NotImplementedError("Google Drive download not yet implemented for iOS")
    }

    override suspend fun downloadAudio(fileId: String, buttonId: Int, localDir: PlatformFile): PlatformFile {
        // TODO: Implement Drive download for iOS
        throw NotImplementedError("Google Drive download not yet implemented for iOS")
    }

    override suspend fun deleteButtonConfig(buttonId: Int) {
        // TODO: Implement Drive delete for iOS
    }
}
