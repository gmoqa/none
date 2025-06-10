package com.example.teaboard.audio

import com.example.teaboard.storage.PlatformFile
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.*
import platform.Foundation.*

/**
 * iOS implementation of AudioRecorder using AVAudioRecorder.
 *
 * Features:
 * - Recording to M4A format
 * - Basic recording controls
 *
 * TODO: Implement full functionality including silence trimming
 */
@OptIn(ExperimentalForeignApi::class)
class IOSAudioRecorder : AudioRecorder {

    private var audioRecorder: AVAudioRecorder? = null
    private var currentRecordingURL: NSURL? = null
    private var isRecordingFlag = false

    override fun startRecording(buttonId: Int): PlatformFile? {
        try {
            // Setup audio session
            val audioSession = AVAudioSession.sharedInstance()
            var error: NSError? = null
            audioSession.setCategory(AVAudioSessionCategoryRecord, error = null)
            audioSession.setActive(true, error = null)

            // Create file URL in documents directory
            val documentsURL = NSFileManager.defaultManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask
            ).firstOrNull() as? NSURL

            if (documentsURL == null) {
                return null
            }

            val audioDir = documentsURL.URLByAppendingPathComponent("audio")
            val audioDirPath = audioDir?.path ?: return null

            if (!NSFileManager.defaultManager.fileExistsAtPath(audioDirPath)) {
                NSFileManager.defaultManager.createDirectoryAtURL(
                    audioDir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }

            val timestamp = NSDate().timeIntervalSince1970.toLong()
            val filename = "button_${buttonId}_${timestamp}.m4a"
            val fileURL = audioDir?.URLByAppendingPathComponent(filename)

            if (fileURL == null) {
                return null
            }

            // Setup recording settings
            val settings = mapOf<Any?, Any?>(
                AVFormatIDKey to 1633772320, // kAudioFormatMPEG4AAC
                AVSampleRateKey to 44100.0,
                AVNumberOfChannelsKey to 1,
                AVEncoderBitRateKey to 128000
            )

            val recorder = AVAudioRecorder(fileURL, settings as Map<Any?, *>, null)
            recorder?.prepareToRecord()

            val success = recorder?.record() ?: false
            if (success) {
                audioRecorder = recorder
                currentRecordingURL = fileURL
                isRecordingFlag = true
                return PlatformFile(fileURL)
            }

            return null
        } catch (e: Exception) {
            println("Error starting recording: ${e.message}")
            return null
        }
    }

    override fun stopRecording(): PlatformFile? {
        if (!isRecordingFlag) {
            return null
        }

        audioRecorder?.stop()
        isRecordingFlag = false

        val url = currentRecordingURL
        audioRecorder = null

        return url?.let { PlatformFile(it) }
    }

    override fun isRecording(): Boolean = isRecordingFlag

    override fun cancelRecording() {
        if (isRecordingFlag) {
            audioRecorder?.stop()
            currentRecordingURL?.let { url ->
                NSFileManager.defaultManager.removeItemAtURL(url, null)
            }
            audioRecorder = null
            currentRecordingURL = null
            isRecordingFlag = false
        }
    }

    override suspend fun trimSilence(
        inputFile: PlatformFile,
        silenceThreshold: Int,
        marginSamples: Int
    ): PlatformFile? {
        // TODO: Implement silence trimming for iOS
        // For now, just return the original file
        println("Warning: Silence trimming not yet implemented for iOS")
        return inputFile
    }
}
