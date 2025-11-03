package org.none.android.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import org.none.android.storage.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * Android implementation of AudioRecorder using MediaRecorder.
 *
 * Features:
 * - Recording to M4A format (MPEG4/AAC)
 * - Android version-aware MediaRecorder initialization
 * - Silence trimming using MediaCodec for audio analysis
 * - Proper error handling and resource cleanup
 *
 * @property context Application context for accessing file system
 */
class AndroidAudioRecorder(private val context: Context) : AudioRecorder {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecordingFlag = false

    companion object {
        private const val TAG = "AndroidAudioRecorder"
        private const val DEBUG = false // Set to true for debug logging
    }

    override fun startRecording(buttonId: Int): PlatformFile? {
        try {
            // Create directory if it doesn't exist
            val audioDir = File(context.filesDir, "audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            // Create audio file
            audioFile = File(audioDir, "button_${buttonId}_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile?.absolutePath)

                try {
                    prepare()
                    start()
                    isRecordingFlag = true
                    if (DEBUG) {
                        Log.d(TAG, "Recording started: ${audioFile?.absolutePath}")
                    }
                } catch (e: IOException) {
                    if (DEBUG) {
                        Log.e(TAG, "prepare() failed: ${e.message}")
                    }
                    release()
                    mediaRecorder = null
                    return null
                }
            }

            return audioFile?.let { PlatformFile(it) }
        } catch (e: Exception) {
            if (DEBUG) {
                Log.e(TAG, "Error starting recording: ${e.message}")
            }
            return null
        }
    }

    override fun stopRecording(): PlatformFile? {
        if (!isRecordingFlag) {
            if (DEBUG) {
                Log.w(TAG, "stopRecording called but not recording")
            }
            return null
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            isRecordingFlag = false

            // Verify that the file was created correctly
            audioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    if (DEBUG) {
                        Log.d(TAG, "Recording stopped successfully: ${file.absolutePath} (${file.length()} bytes)")
                    }
                    return PlatformFile(file)
                } else {
                    if (DEBUG) {
                        Log.e(TAG, "Recording file is invalid - exists: ${file.exists()}, size: ${file.length()}")
                    }
                    file.delete()
                    return null
                }
            }

            if (DEBUG) {
                Log.e(TAG, "Audio file is null after recording")
            }
            return null
        } catch (e: Exception) {
            if (DEBUG) {
                Log.e(TAG, "Error stopping recording: ${e.message}", e)
            }
            // Try to clean up the file if there's an error
            try {
                mediaRecorder?.release()
                audioFile?.delete()
            } catch (cleanupError: Exception) {
                if (DEBUG) {
                    Log.e(TAG, "Error during cleanup: ${cleanupError.message}")
                }
            }
            return null
        } finally {
            mediaRecorder = null
        }
    }

    override fun isRecording(): Boolean = isRecordingFlag

    override fun cancelRecording() {
        if (isRecordingFlag) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                audioFile?.delete()
            } catch (e: Exception) {
                if (DEBUG) {
                    Log.e(TAG, "Error cancelling recording: ${e.message}")
                }
            } finally {
                mediaRecorder = null
                audioFile = null
                isRecordingFlag = false
            }
        }
    }

    /**
     * Trim silence from the beginning and end of an audio file.
     * This is a suspend function that runs on IO dispatcher.
     *
     * @param inputFile The audio file to trim
     * @param silenceThreshold The amplitude threshold to consider as silence (0-32768, default 1000)
     * @param marginSamples Number of samples to keep before/after audio for smooth attack (default 1)
     * @return The trimmed audio file, or null if trimming failed
     */
    override suspend fun trimSilence(
        inputFile: PlatformFile,
        silenceThreshold: Int,
        marginSamples: Int
    ): PlatformFile? = withContext(Dispatchers.IO) {
        val file = inputFile.file

        if (!file.exists() || file.length() == 0L) {
            if (DEBUG) {
                Log.e(TAG, "Input file does not exist or is empty")
            }
            return@withContext null
        }

        val outputFile = File(
            file.parent,
            "trimmed_${System.currentTimeMillis()}.m4a"
        )

        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            // Setup extractor
            extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)

            // Find audio track
            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || inputFormat == null) {
                if (DEBUG) {
                    Log.e(TAG, "No audio track found")
                }
                return@withContext null
            }

            extractor.selectTrack(audioTrackIndex)

            // Decode all audio to analyze amplitude
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: ""
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val decodedSamples = mutableListOf<ByteArray>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isDecodingDone = false

            while (!isDecodingDone) {
                // Feed input
                val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()

                    val ext = extractor // Local reference for smart cast
                    val sampleSize = if (inputBuffer != null && ext != null) {
                        ext.readSampleData(inputBuffer, 0)
                    } else {
                        -1
                    }
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        val presentationTime = ext!!.sampleTime
                        decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTime, 0)
                        ext.advance()
                    }
                }

                // Get output
                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)

                    if (bufferInfo.size > 0 && outputBuffer != null) {
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.get(chunk, 0, bufferInfo.size)
                        decodedSamples.add(chunk)
                    }

                    decoder.releaseOutputBuffer(outputBufferIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isDecodingDone = true
                    }
                }
            }

            decoder.stop()
            decoder.release()
            decoder = null

            // Analyze samples to find start and end of actual audio
            var startSample = 0
            var endSample = decodedSamples.size - 1

            // Find start - require 3 consecutive loud samples to avoid false positives from noise
            val consecutiveThreshold = 3
            var consecutiveLoudCount = 0
            for (i in decodedSamples.indices) {
                if (isLoudEnough(decodedSamples[i], silenceThreshold)) {
                    consecutiveLoudCount++
                    if (consecutiveLoudCount >= consecutiveThreshold) {
                        // Found sustained sound, back up to the first of the consecutive loud samples
                        startSample = maxOf(0, i - consecutiveLoudCount + 1 - marginSamples)
                        break
                    }
                } else {
                    consecutiveLoudCount = 0
                }
            }

            // Find end - require 3 consecutive loud samples from the end
            consecutiveLoudCount = 0
            for (i in decodedSamples.indices.reversed()) {
                if (isLoudEnough(decodedSamples[i], silenceThreshold)) {
                    consecutiveLoudCount++
                    if (consecutiveLoudCount >= consecutiveThreshold) {
                        // Found sustained sound, move forward to the last of the consecutive loud samples
                        endSample = minOf(decodedSamples.size - 1, i + consecutiveLoudCount - 1 + marginSamples)
                        break
                    }
                } else {
                    consecutiveLoudCount = 0
                }
            }

            if (DEBUG) {
                Log.d(TAG, "Audio analysis: total samples=${decodedSamples.size}, start=$startSample, end=$endSample, threshold=$silenceThreshold")
            }

            // If entire audio is silence or trimming would remove everything, return original
            if (startSample >= endSample || startSample >= decodedSamples.size - 1) {
                if (DEBUG) {
                    Log.w(TAG, "Audio is entirely silence or trimming would remove all audio, keeping original")
                }
                return@withContext inputFile
            }

            // Reset extractor to re-read the file for muxing
            extractor.release()
            val newExtractor = MediaExtractor()
            newExtractor.setDataSource(file.absolutePath)
            newExtractor.selectTrack(audioTrackIndex)
            extractor = newExtractor

            // Calculate time range to keep
            val sampleDuration = if (decodedSamples.isNotEmpty()) {
                // Estimate duration per sample
                inputFormat.getLong(MediaFormat.KEY_DURATION) / decodedSamples.size
            } else {
                0L
            }

            val startTimeUs = startSample * sampleDuration
            val endTimeUs = (endSample + 1) * sampleDuration

            if (DEBUG) {
                Log.d(TAG, "Trimming from ${startTimeUs}us to ${endTimeUs}us")
            }

            // Seek to start position
            newExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            // Setup muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackIndex = muxer.addTrack(inputFormat)
            muxer.start()

            // Copy samples in the time range
            val buffer = ByteBuffer.allocate(256 * 1024)
            val muxerBufferInfo = MediaCodec.BufferInfo()

            while (true) {
                buffer.clear()
                val sampleSize = newExtractor.readSampleData(buffer, 0)

                if (sampleSize < 0) break

                val sampleTime = newExtractor.sampleTime
                if (sampleTime > endTimeUs) break

                if (sampleTime >= startTimeUs) {
                    muxerBufferInfo.offset = 0
                    muxerBufferInfo.size = sampleSize
                    muxerBufferInfo.presentationTimeUs = sampleTime - startTimeUs // Adjust timestamp
                    @Suppress("WrongConstant")
                    muxerBufferInfo.flags = newExtractor.sampleFlags

                    muxer.writeSampleData(trackIndex, buffer, muxerBufferInfo)
                }

                newExtractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            newExtractor.release()
            extractor = null

            // Delete original and rename trimmed file
            val originalPath = file.absolutePath
            if (file.delete()) {
                val finalFile = File(originalPath)
                if (outputFile.renameTo(finalFile)) {
                    if (DEBUG) {
                        Log.d(TAG, "Successfully trimmed audio: ${finalFile.absolutePath}")
                    }
                    return@withContext PlatformFile(finalFile)
                } else {
                    if (DEBUG) {
                        Log.w(TAG, "Could not rename trimmed file, returning as-is")
                    }
                    return@withContext PlatformFile(outputFile)
                }
            } else {
                if (DEBUG) {
                    Log.w(TAG, "Could not delete original file, returning trimmed file")
                }
                return@withContext PlatformFile(outputFile)
            }

        } catch (e: Exception) {
            if (DEBUG) {
                Log.e(TAG, "Error trimming silence: ${e.message}", e)
            }
            outputFile.delete()
            return@withContext inputFile // Return original file if trimming failed
        } finally {
            try {
                decoder?.stop()
                decoder?.release()
                encoder?.stop()
                encoder?.release()
                muxer?.stop()
                muxer?.release()
                extractor?.release()
            } catch (e: Exception) {
                if (DEBUG) {
                    Log.e(TAG, "Error cleaning up resources: ${e.message}")
                }
            }
        }
    }

    /**
     * Check if a sample buffer is loud enough (not silence).
     * Uses peak amplitude for instant detection.
     */
    private fun isLoudEnough(sample: ByteArray, threshold: Int): Boolean {
        if (sample.size < 2) return false

        // Calculate peak amplitude for instant detection
        var peakAmplitude = 0

        for (i in 0 until sample.size - 1 step 2) {
            // Read 16-bit signed little-endian sample
            val low = sample[i].toInt() and 0xFF
            val high = sample[i + 1].toInt()
            val sampleValue = (high shl 8) or low

            // Convert to signed 16-bit range
            val signedSample = if (sampleValue > 32767) sampleValue - 65536 else sampleValue
            val amplitude = abs(signedSample)

            if (amplitude > peakAmplitude) {
                peakAmplitude = amplitude
            }
        }

        return peakAmplitude > threshold
    }
}
