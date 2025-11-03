package org.none.android.utils

import org.none.android.storage.PlatformFile
import java.io.File

/**
 * Extension functions to convert between java.io.File and PlatformFile
 */

fun File.toPlatformFile(): PlatformFile {
    return PlatformFile(this)
}

fun PlatformFile.toFile(): File {
    return this.file
}
