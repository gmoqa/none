package com.example.teaboard.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ButtonConfigTest {

    @Test
    fun `hasImage returns true when imageUrl is not empty`() {
        val config = ButtonConfig(
            buttonId = 1,
            imageUrl = "https://example.com/image.jpg"
        )

        assertThat(config.hasImage()).isTrue()
    }

    @Test
    fun `hasImage returns true when imagePath is not empty`() {
        val config = ButtonConfig(
            buttonId = 1,
            imagePath = "/data/user/0/com.example.teaboard/files/images/button_1.jpg"
        )

        assertThat(config.hasImage()).isTrue()
    }

    @Test
    fun `hasImage returns false when both imageUrl and imagePath are empty`() {
        val config = ButtonConfig(
            buttonId = 1,
            imageUrl = "",
            imagePath = ""
        )

        assertThat(config.hasImage()).isFalse()
    }

    @Test
    fun `hasAudio returns true when audioUrl is not empty`() {
        val config = ButtonConfig(
            buttonId = 1,
            audioUrl = "https://example.com/audio.m4a"
        )

        assertThat(config.hasAudio()).isTrue()
    }

    @Test
    fun `hasAudio returns true when audioPath is not empty`() {
        val config = ButtonConfig(
            buttonId = 1,
            audioPath = "/data/user/0/com.example.teaboard/files/audio/button_1.m4a"
        )

        assertThat(config.hasAudio()).isTrue()
    }

    @Test
    fun `hasAudio returns false when both audioUrl and audioPath are empty`() {
        val config = ButtonConfig(
            buttonId = 1,
            audioUrl = "",
            audioPath = ""
        )

        assertThat(config.hasAudio()).isFalse()
    }

    @Test
    fun `default values are applied correctly`() {
        val config = ButtonConfig()

        assertThat(config.buttonId).isEqualTo(0)
        assertThat(config.label).isEmpty()
        assertThat(config.imageUrl).isEmpty()
        assertThat(config.audioUrl).isEmpty()
        assertThat(config.imagePath).isEmpty()
        assertThat(config.audioPath).isEmpty()
        assertThat(config.driveImageId).isEmpty()
        assertThat(config.driveAudioId).isEmpty()
        assertThat(config.backgroundColor).isEqualTo("#6B4CE6")
    }

    @Test
    fun `data class copy works correctly`() {
        val original = ButtonConfig(
            buttonId = 1,
            label = "Water",
            imagePath = "/path/to/image.jpg",
            audioPath = "/path/to/audio.m4a"
        )

        val copy = original.copy(label = "Juice")

        assertThat(copy.buttonId).isEqualTo(original.buttonId)
        assertThat(copy.label).isEqualTo("Juice")
        assertThat(copy.imagePath).isEqualTo(original.imagePath)
        assertThat(copy.audioPath).isEqualTo(original.audioPath)
    }

    @Test
    fun `data class equality works correctly`() {
        val config1 = ButtonConfig(
            buttonId = 1,
            label = "Water",
            imagePath = "/path/to/image.jpg"
        )

        val config2 = ButtonConfig(
            buttonId = 1,
            label = "Water",
            imagePath = "/path/to/image.jpg"
        )

        assertThat(config1).isEqualTo(config2)
    }

    @Test
    fun `data class hashCode works correctly`() {
        val config1 = ButtonConfig(
            buttonId = 1,
            label = "Water",
            imagePath = "/path/to/image.jpg"
        )

        val config2 = ButtonConfig(
            buttonId = 1,
            label = "Water",
            imagePath = "/path/to/image.jpg"
        )

        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())
    }

    @Test
    fun `ButtonConfig with all fields populated`() {
        val config = ButtonConfig(
            buttonId = 3,
            label = "Cookie",
            imageUrl = "https://example.com/cookie.jpg",
            audioUrl = "https://example.com/cookie.m4a",
            imagePath = "/data/user/0/com.example.teaboard/files/images/button_3_1234567890.jpg",
            audioPath = "/data/user/0/com.example.teaboard/files/audio/button_3_1234567890.m4a",
            driveImageId = "drive_image_123",
            driveAudioId = "drive_audio_456",
            backgroundColor = "#FF5722"
        )

        assertThat(config.buttonId).isEqualTo(3)
        assertThat(config.label).isEqualTo("Cookie")
        assertThat(config.imageUrl).isEqualTo("https://example.com/cookie.jpg")
        assertThat(config.audioUrl).isEqualTo("https://example.com/cookie.m4a")
        assertThat(config.imagePath).contains("button_3_1234567890.jpg")
        assertThat(config.audioPath).contains("button_3_1234567890.m4a")
        assertThat(config.driveImageId).isEqualTo("drive_image_123")
        assertThat(config.driveAudioId).isEqualTo("drive_audio_456")
        assertThat(config.backgroundColor).isEqualTo("#FF5722")
        assertThat(config.hasImage()).isTrue()
        assertThat(config.hasAudio()).isTrue()
    }
}
