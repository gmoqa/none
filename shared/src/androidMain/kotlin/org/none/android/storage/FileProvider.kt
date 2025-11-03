package org.none.android.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of PlatformFile
 */
actual class PlatformFile(val file: File) {
    actual val path: String get() = file.absolutePath
    actual val name: String get() = file.name

    actual fun exists(): Boolean = file.exists()
    actual fun length(): Long = file.length()
    actual fun delete(): Boolean = file.delete()

    actual suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        file.readBytes()
    }

    actual suspend fun writeBytes(bytes: ByteArray) = withContext(Dispatchers.IO) {
        file.writeBytes(bytes)
    }
}

/**
 * Android implementation of FileProvider
 */
actual class FileProvider(private val context: Context) {
    actual fun getFilesDir(): PlatformFile {
        return PlatformFile(context.filesDir)
    }

    actual fun getFile(subdirectory: String, filename: String): PlatformFile {
        val dir = File(context.filesDir, subdirectory)
        return PlatformFile(File(dir, filename))
    }

    actual fun createDirectory(subdirectory: String): PlatformFile {
        val dir = File(context.filesDir, subdirectory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return PlatformFile(dir)
    }

    actual fun listFiles(directory: PlatformFile): List<PlatformFile> {
        return directory.file.listFiles()?.map { PlatformFile(it) } ?: emptyList()
    }

    actual suspend fun copyFile(source: PlatformFile, destination: PlatformFile) {
        withContext(Dispatchers.IO) {
            source.file.copyTo(destination.file, overwrite = true)
        }
    }
}
