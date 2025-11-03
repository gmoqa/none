package org.none.android.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSError
import platform.Foundation.NSURL

/**
 * iOS implementation of AudioPlayer using AVAudioPlayer.
 *
 * Features:
 * - Audio playback using AVFoundation
 * - Pre-loading for instant playback
 * - Memory-efficient caching
 *
 * TODO: Implement completion callback using AVAudioPlayerDelegate
 */
@OptIn(ExperimentalForeignApi::class)
class IOSAudioPlayer : AudioPlayer {

    private var currentPlayer: AVAudioPlayer? = null
    private val audioCache = mutableMapOf<String, AVAudioPlayer>()

    override fun preloadAudio(audioPath: String) {
        try {
            val url = NSURL.fileURLWithPath(audioPath)
            var error: NSError? = null
            val player = AVAudioPlayer(contentsOfURL = url, error = null)
            player?.prepareToPlay()
            if (player != null) {
                audioCache[audioPath] = player
            }
        } catch (e: Exception) {
            println("Error preloading audio: ${e.message}")
        }
    }

    override fun playAudio(
        audioPath: String,
        onComplete: (() -> Unit)?,
        onError: ((String) -> Unit)?
    ) {
        try {
            // Check cache first
            val player = audioCache[audioPath] ?: run {
                val url = NSURL.fileURLWithPath(audioPath)
                val newPlayer = AVAudioPlayer(contentsOfURL = url, error = null)
                newPlayer?.prepareToPlay()
                newPlayer
            }

            if (player == null) {
                onError?.invoke("Failed to create audio player")
                return
            }

            currentPlayer = player
            val success = player.play()

            if (!success) {
                onError?.invoke("Failed to play audio")
            } else {
                // TODO: Implement proper completion callback
                // Need to use AVAudioPlayerDelegate
            }
        } catch (e: Exception) {
            onError?.invoke("Error playing audio: ${e.message}")
        }
    }

    override fun stopAudio() {
        currentPlayer?.stop()
        currentPlayer = null
    }

    override fun releaseCache() {
        audioCache.values.forEach { it.stop() }
        audioCache.clear()
    }

    override fun isPlaying(): Boolean {
        return currentPlayer?.playing ?: false
    }
}
