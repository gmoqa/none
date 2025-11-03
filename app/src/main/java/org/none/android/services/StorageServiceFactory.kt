package org.none.android.services

import android.content.Context
import org.none.android.storage.DriveStorageImpl
import org.none.android.storage.FileProvider
import org.none.android.storage.LocalStorageImpl
import org.none.android.storage.PreferencesProvider
import org.none.android.storage.StorageService

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
