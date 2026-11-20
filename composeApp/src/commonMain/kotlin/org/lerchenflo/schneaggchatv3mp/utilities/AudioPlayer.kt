package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lerchenflo.voicemessages.VoicePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

class AudioPlayer {
    private val voicePlayer = VoicePlayer()
    private var pollingJob: Job? = null

    // Track which message is currently being played
    var currentlyPlayingId by mutableStateOf<String?>(null)
        private set

    // Track playback progress
    var currentPlaybackPosition by mutableStateOf(0L)
        private set

    var totalPlaybackDuration by mutableStateOf(0L)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()

    suspend fun playAudio(messageId: String, path: String) {
        try {
            // 1. If this audio is paused (by the user or by another app taking the audio), resume it
            if (currentlyPlayingId == messageId && !isPlaying) {
                resumeAudio(messageId)
                return
            }

            // 2. If a different audio was playing, stop it first
            if (currentlyPlayingId != null) {
                stopAudio()
                println("stopped other audio playing")
            }


            currentlyPlayingId = messageId
            isPlaying = true

            // 4. Start playback
            voicePlayer.play(path)
            println("started playing ...")

            // 5. Poll for progress updates (replaces the old push-based listener)
            startPolling(messageId)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            //loggingRepository.logWarning("Failed to play audio: ${e.message}")
            println("Failed to play audio: ${e.message}")
            stopAudio()
        }
    }

    private suspend fun resumeAudio(messageId: String) {
        try {
            voicePlayer.resume()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            // Audio denied (e.g. during a phone call): the message stays paused at its position
            println("Failed to resume audio: ${e.message}")
            return
        }
        isPlaying = true
        _playbackProgress.value = _playbackProgress.value.copy(isPlaying = true)
        startPolling(messageId)
    }

    private fun startPolling(messageId: String) {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                if (!voicePlayer.isPlaying) {
                    if (voicePlayer.isPaused) {
                        // Another app took the audio (call, video, music): keep the position
                        markPaused()
                    } else {
                        // Finished or stopped externally
                        resetPlaybackState()
                        _playbackProgress.value = PlaybackProgress()
                    }
                    break
                }
                _playbackProgress.value = PlaybackProgress(
                    currentPosition = voicePlayer.positionMs,
                    duration = voicePlayer.durationMs,
                    isPlaying = true,
                    messageId = messageId
                )
                delay(200)
            }
        }
    }

    suspend fun pauseAudio() {
        try {
            pollingJob?.cancel()
            voicePlayer.pause()
            markPaused()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            //loggingRepository.logWarning("Failed to pause audio: ${e.message}")
            println("Failed to pause audio: ${e.message}")
        }
    }

    suspend fun seekTo(position: Long) {
        try {
            voicePlayer.seekTo(position)
        }catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            println("Failed to seeking audio: ${e.message}")
        }
    }

    suspend fun stopAudio() {
        try {
            pollingJob?.cancel()
            voicePlayer.stop()
            resetPlaybackState()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            //loggingRepository.logWarning("Failed to stop audio: ${e.message}")
            println("Failed to stop audio: ${e.message}")
        }
    }

    /** Paused with the message still loaded, so the next play of it resumes at the same position. */
    private fun markPaused() {
        isPlaying = false
        _playbackProgress.value = _playbackProgress.value.copy(isPlaying = false)
    }

    private fun resetPlaybackState() {
        currentlyPlayingId = null
        isPlaying = false
        currentPlaybackPosition = 0L
        totalPlaybackDuration = 0L
    }
}

data class PlaybackProgress(
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isPlaying: Boolean = false,
    val messageId: String? = null
)

fun getAudioBytes(filePath: String): ByteArray {
    try {
        if (filePath.isEmpty()) return byteArrayOf()

        val path = Path(filePath)
        val fileSource = SystemFileSystem.source(path).buffered()

        // Read the entire file into a ByteArray
        return fileSource.readByteArray()
    } catch (e: Exception) {
        println("Failed to read audio file: ${e.message}")
        return byteArrayOf()
    }
}
