package com.example.teaboard.storage

/**
 * Platform-specific file handle
 * Android: wraps java.io.File
 * iOS: wraps URL
 */
expect class PlatformFile {
    val path: String
    val name: String
    fun exists(): Boolean
    fun length(): Long
    fun delete(): Boolean
    suspend fun readBytes(): ByteArray
    suspend fun writeBytes(bytes: ByteArray)
}

/**
 * Platform-agnostic file provider
 * Abstracts filesystem access for multiplatform code
 */
expect class FileProvider {
    /**
     * Get the application's files directory
     * Android: context.filesDir
     * iOS: FileManager.default.urls(for: .documentDirectory)
     */
    fun getFilesDir(): PlatformFile

    /**
     * Get a file in a subdirectory of filesDir
     */
    fun getFile(subdirectory: String, filename: String): PlatformFile

    /**
     * Create a directory if it doesn't exist
     */
    fun createDirectory(subdirectory: String): PlatformFile

    /**
     * List files in a directory
     */
    fun listFiles(directory: PlatformFile): List<PlatformFile>

    /**
     * Copy file from source to destination
     */
    suspend fun copyFile(source: PlatformFile, destination: PlatformFile)
}
