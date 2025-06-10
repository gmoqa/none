package com.example.teaboard.audio

import com.example.teaboard.storage.PlatformFile

/**
 * Platform-agnostic interface for audio recording.
 *
 * This interface defines the contract for recording audio files. Platform-specific
 * implementations should handle the actual audio recording using native APIs:
 * - Android: MediaRecorder/MediaCodec
 * - iOS: AVAudioRecorder
 *
 * Features:
 * - Start/stop recording to files
 * - Check recording status
 * - Cancel recording
 * - Post-processing (silence trimming)
 */
interface AudioRecorder {

    /**
     * Starts recording audio to a file.
     *
     * @param buttonId Identifier for the button being configured
     * @return The file where audio is being recorded, or null if recording couldn't start
     */
    fun startRecording(buttonId: Int): PlatformFile?

    /**
     * Stops the current recording and finalizes the audio file.
     *
     * @return The recorded audio file, or null if no recording was in progress
     */
    fun stopRecording(): PlatformFile?

    /**
     * Checks if recording is currently in progress.
     *
     * @return true if recording, false otherwise
     */
    fun isRecording(): Boolean

    /**
     * Cancels the current recording and discards the audio file.
     */
    fun cancelRecording()

    /**
     * Trims silence from the beginning and end of an audio file.
     *
     * This is a suspend function that performs audio processing.
     * Platform implementations should handle this appropriately
     * (e.g., using Dispatchers.IO on Android).
     *
     * @param inputFile The audio file to process
     * @param silenceThreshold Amplitude threshold for considering audio as silence
     * @param marginSamples Number of samples to keep before/after non-silent audio
     * @return The trimmed audio file, or null if processing failed
     */
    suspend fun trimSilence(
        inputFile: PlatformFile,
        silenceThreshold: Int = 1000,
        marginSamples: Int = 1
    ): PlatformFile?
}
