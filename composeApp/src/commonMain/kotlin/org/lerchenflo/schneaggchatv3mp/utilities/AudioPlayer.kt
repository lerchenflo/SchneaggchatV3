package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lerchenflo.voicemessages.VoicePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.jetbrains.compose.resources.getString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.audio_playback_on_desktop_not_supported_yet

class AudioPlayer(
    val isDesktop: Boolean = false
) {
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
        if(isDesktop){
            SnackbarManager.showMessage(getString(Res.string.audio_playback_on_desktop_not_supported_yet))
        }else {
            try {
                // 1. If we are already playing this specific audio, just resume it
                if (currentlyPlayingId == messageId && !isPlaying) {
                    voicePlayer.resume()
                    isPlaying = true
                    _playbackProgress.value = _playbackProgress.value.copy(isPlaying = true)
                    startPolling(messageId)
                    println("this audio is already playing")
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
                //loggingRepository.logWarning("Failed to play audio: ${e.message}")
                println("Failed to play audio: ${e.message}")
                stopAudio()
            }
        }
    }

    private fun startPolling(messageId: String) {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                if (!voicePlayer.isPlaying) {
                    // Finished or stopped externally
                    resetPlaybackState()
                    _playbackProgress.value = PlaybackProgress()
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
            isPlaying = false
            _playbackProgress.value = _playbackProgress.value.copy(isPlaying = false)
        } catch (e: Exception) {
            //loggingRepository.logWarning("Failed to pause audio: ${e.message}")
            println("Failed to pause audio: ${e.message}")
        }
    }

    suspend fun seekTo(position: Long) {
        try {
            voicePlayer.seekTo(position)
        }catch (e: Exception) {
            println("Failed to seeking audio: ${e.message}")
        }
    }

    suspend fun stopAudio() {
        try {
            pollingJob?.cancel()
            voicePlayer.stop()
            resetPlaybackState()
        } catch (e: Exception) {
            //loggingRepository.logWarning("Failed to stop audio: ${e.message}")
            println("Failed to stop audio: ${e.message}")
        }
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
