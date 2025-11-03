package org.none.android.storage

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.posix.memcpy

/**
 * iOS implementation of PlatformFile using NSURL
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformFile(val url: NSURL) {
    actual val path: String get() = url.path ?: ""
    actual val name: String get() = url.lastPathComponent ?: ""

    actual fun exists(): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }

    actual fun length(): Long {
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
        return (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
    }

    actual fun delete(): Boolean {
        return NSFileManager.defaultManager.removeItemAtURL(url, null)
    }

    actual suspend fun readBytes(): ByteArray = withContext(Dispatchers.Default) {
        val data = NSData.dataWithContentsOfURL(url) ?: return@withContext byteArrayOf()
        ByteArray(data.length.toInt()).apply {
            data.bytes?.let { bytes ->
                usePinned { pinned ->
                    memcpy(pinned.addressOf(0), bytes, data.length)
                }
            }
        }
    }

    actual suspend fun writeBytes(bytes: ByteArray): Unit = withContext(Dispatchers.Default) {
        bytes.usePinned { pinned ->
            val data = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            data.writeToURL(url, atomically = true)
        }
        Unit
    }
}

/**
 * iOS implementation of FileProvider using NSFileManager
 */
@OptIn(ExperimentalForeignApi::class)
actual class FileProvider {

    actual fun getFilesDir(): PlatformFile {
        val documentsURL = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull() as? NSURL
            ?: throw IllegalStateException("Cannot access documents directory")
        return PlatformFile(documentsURL)
    }

    actual fun getFile(subdirectory: String, filename: String): PlatformFile {
        val documentsURL = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull() as? NSURL
            ?: throw IllegalStateException("Cannot access documents directory")

        var fileURL = documentsURL

        if (subdirectory.isNotEmpty()) {
            fileURL = fileURL.URLByAppendingPathComponent(subdirectory)
                ?: throw IllegalStateException("Cannot create subdirectory path")
        }

        fileURL = fileURL.URLByAppendingPathComponent(filename)
            ?: throw IllegalStateException("Cannot create file path")

        return PlatformFile(fileURL)
    }

    actual fun createDirectory(subdirectory: String): PlatformFile {
        val documentsURL = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull() as? NSURL
            ?: throw IllegalStateException("Cannot access documents directory")

        val dirURL = documentsURL.URLByAppendingPathComponent(subdirectory)
            ?: throw IllegalStateException("Cannot create directory path")

        val dirPath = dirURL.path ?: throw IllegalStateException("Invalid directory path")

        if (!NSFileManager.defaultManager.fileExistsAtPath(dirPath)) {
            NSFileManager.defaultManager.createDirectoryAtURL(
                dirURL,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        return PlatformFile(dirURL)
    }

    actual fun listFiles(directory: PlatformFile): List<PlatformFile> {
        val contents = NSFileManager.defaultManager.contentsOfDirectoryAtURL(
            directory.url,
            includingPropertiesForKeys = null,
            options = 0u,
            error = null
        ) ?: return emptyList()

        return contents.mapNotNull { item ->
            (item as? NSURL)?.let { PlatformFile(it) }
        }
    }

    actual suspend fun copyFile(source: PlatformFile, destination: PlatformFile): Unit =
        withContext(Dispatchers.Default) {
            val data = source.readBytes()
            destination.writeBytes(data)
            Unit
        }
}
