package com.example.teaboard.services

import android.content.Context
import com.example.teaboard.storage.DriveStorageImpl
import com.example.teaboard.storage.FileProvider
import com.example.teaboard.storage.LocalStorageImpl
import com.example.teaboard.storage.PreferencesProvider
import com.example.teaboard.storage.StorageService

/**
 * Factory to create StorageService with Android implementations
 */
object StorageServiceFactory {

    fun create(context: Context): StorageService {
        // Create platform-specific providers
        val fileProvider = FileProvider(context)
        val preferencesProvider = PreferencesProvider(context)

        // Create local storage implementation
        val localStorage = LocalStorageImpl(context, fileProvider)

        // Create drive storage implementation (optional)
        val driveStorage = DriveStorageImpl(context, preferencesProvider)

        // Create and return the shared StorageService
        return StorageService(
            localStorage = localStorage,
            driveStorage = driveStorage,
            preferences = preferencesProvider
        )
    }
}
