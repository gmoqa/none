package org.none.android.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.lang.ref.WeakReference

/**
 * Android implementation of AudioPlayer using MediaPlayer.
 *
 * Features:
 * - Pre-loading audio files for instant playback
 * - Memory-efficient caching using WeakReferences
 * - Automatic volume boost for better audibility
 * - Proper cleanup to prevent memory leaks
 *
 * @property context Application context for accessing system services
 */
class AndroidAudioPlayer(private val context: Context) : AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Cache of pre-loaded MediaPlayers using WeakReference to prevent memory leaks
    private val audioCache = mutableMapOf<String, WeakReference<MediaPlayer>>()

    companion object {
        private const val TAG = "AndroidAudioPlayer"
        private const val DEBUG = false // Set to true for debug logging
    }

    override fun preloadAudio(audioPath: String) {
        try {
            val file = File(audioPath)
            if (!file.exists() || file.length() == 0L) {
                if (DEBUG) {
                    Log.w(TAG, "Cannot preload audio")
                }
                return
            }

            // Check if already cached and still valid
            audioCache[audioPath]?.get()?.let { player ->
                if (DEBUG) {
                    Log.d(TAG, "Audio already cached")
                }
                return
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val cachedPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(audioPath)
                setVolume(1.0f, 1.0f)
                setOnCompletionListener {
                    // Reset to beginning for next play
                    seekTo(0)
                }
                prepare() // Synchronous prepare during preload is okay
            }

            audioCache[audioPath] = WeakReference(cachedPlayer)
            if (DEBUG) {
                Log.d(TAG, "Audio preloaded (${file.length()} bytes)")
            }
        } catch (e: Exception) {
            if (DEBUG) {
                Log.e(TAG, "Error preloading audio", e)
            }
        }
    }

    override fun playAudio(audioPath: String, onComplete: (() -> Unit)?, onError: ((String) -> Unit)?) {
        try {
            val file = File(audioPath)
            if (!file.exists()) {
                val error = "Audio file does not exist"
                if (DEBUG) {
                    Log.e(TAG, error)
                }
                onError?.invoke(error)
                return
            }

            if (file.length() == 0L) {
                val error = "Audio file is empty"
                if (DEBUG) {
                    Log.e(TAG, error)
                }
                onError?.invoke(error)
                return
            }

            // Check if audio is cached and still valid
            val cachedPlayer = audioCache[audioPath]?.get()
            if (cachedPlayer != null) {
                // Use cached player for instant playback
                stopAudio()
                mediaPlayer = cachedPlayer

                if (cachedPlayer.isPlaying) {
                    cachedPlayer.seekTo(0)
                } else {
                    cachedPlayer.start()
                }

                if (DEBUG) {
                    Log.d(TAG, "Playing cached audio")
                }
                onComplete?.let { callback ->
                    cachedPlayer.setOnCompletionListener {
                        cachedPlayer.seekTo(0)
                        callback()
                    }
                }
            } else {
                // Fallback: load and play asynchronously
                // If cached player was garbage collected, remove from cache
                if (audioCache.containsKey(audioPath)) {
                    audioCache.remove(audioPath)
                }

                stopAudio()

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(audioPath)
                    setVolume(1.0f, 1.0f)
                    setOnErrorListener { _, what, extra ->
                        if (DEBUG) {
                            Log.e(TAG, "MediaPlayer error - what: $what, extra: $extra")
                        }
                        onError?.invoke("Error de reproducción: código $what")
                        true
                    }
                    setOnCompletionListener {
                        onComplete?.invoke()
                    }
                    prepareAsync() // Use async to avoid blocking
                    setOnPreparedListener {
                        start()
                        if (DEBUG) {
                            Log.d(TAG, "Playing audio (async) (${file.length()} bytes)")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val error = "Error playing audio: ${e.message}"
            if (DEBUG) {
                Log.e(TAG, error, e)
            }
            onError?.invoke(e.message ?: "Error desconocido")
        }
    }

    override fun stopAudio() {
        // Don't release cached players, just stop current playback
        mediaPlayer?.apply {
            val isCached = audioCache.values.any { it.get() == this }
            if (isPlaying && !isCached) {
                // Only stop and release if it's not a cached player
                stop()
                release()
            } else if (isPlaying) {
                // If it's a cached player, just pause and reset
                pause()
                seekTo(0)
            }
        }
        mediaPlayer = null
    }

    override fun releaseCache() {
        audioCache.values.forEach { weakRef ->
            try {
                weakRef.get()?.let { player ->
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.release()
                }
            } catch (e: Exception) {
                if (DEBUG) {
                    Log.e(TAG, "Error releasing cached player", e)
                }
            }
        }
        audioCache.clear()
        if (DEBUG) {
            Log.d(TAG, "Audio cache released")
        }
    }

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
}
