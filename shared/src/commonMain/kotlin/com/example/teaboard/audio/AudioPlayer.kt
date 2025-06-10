package com.example.teaboard.audio

/**
 * Platform-agnostic interface for audio playback.
 *
 * This interface defines the contract for playing audio files. Platform-specific
 * implementations should handle the actual audio playback using native APIs:
 * - Android: MediaPlayer
 * - iOS: AVAudioPlayer
 *
 * Features:
 * - Pre-loading audio files for instant playback
 * - Playback with completion and error callbacks
 * - Playback control (stop, check status)
 * - Resource cleanup
 */
interface AudioPlayer {

    /**
     * Pre-loads an audio file into memory for instant playback.
     *
     * @param audioPath Absolute path to the audio file to preload
     */
    fun preloadAudio(audioPath: String)

    /**
     * Plays an audio file with optional callbacks for completion and errors.
     *
     * @param audioPath Absolute path to the audio file to play
     * @param onComplete Optional callback invoked when playback completes successfully
     * @param onError Optional callback invoked if playback fails, receives error message
     */
    fun playAudio(
        audioPath: String,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    )

    /**
     * Stops the currently playing audio, if any.
     */
    fun stopAudio()

    /**
     * Releases all cached audio resources to free up memory.
     */
    fun releaseCache()

    /**
     * Checks if audio is currently playing.
     *
     * @return true if audio is playing, false otherwise
     */
    fun isPlaying(): Boolean
}
