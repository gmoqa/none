package org.none.android.models

import kotlinx.serialization.Serializable

@Serializable
data class ButtonConfig(
    val buttonId: Int = 0,
    val label: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val imagePath: String = "", // Path local del archivo
    val audioPath: String = "", // Path local del archivo
    val driveImageId: String = "",
    val driveAudioId: String = "",
    val backgroundColor: String = "#6B4CE6"
) {
    fun hasImage(): Boolean = imageUrl.isNotEmpty() || imagePath.isNotEmpty()
    fun hasAudio(): Boolean = audioUrl.isNotEmpty() || audioPath.isNotEmpty()
}
