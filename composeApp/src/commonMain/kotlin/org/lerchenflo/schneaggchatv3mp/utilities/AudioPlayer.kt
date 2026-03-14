package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.hyochan.audio.AudioRecorderPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioPlayer(

) {
    var audioRecorderPlayer: AudioRecorderPlayer? = null
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
            if (audioRecorderPlayer == null) {
            println("audioRecorderPlayer is not initialized")
            return
            }

            // 1. If we are already playing this specific audio, just resume it
            if (currentlyPlayingId == messageId && !isPlaying) {
                audioRecorderPlayer?.resumePlaying()
                isPlaying = true
                _playbackProgress.value = _playbackProgress.value.copy(isPlaying = true)
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
            audioRecorderPlayer?.startPlaying(path)
            println("started playing ...")

            // 5. Listen for progress updates
            // Update Flow inside the listener
            audioRecorderPlayer?.addPlaybackListener { progress ->
                _playbackProgress.value = PlaybackProgress(
                    currentPosition = progress.currentPosition,
                    duration = progress.duration,
                    isPlaying = true,
                    messageId = messageId
                )

                // Reset state when finished
                if (progress.currentPosition >= progress.duration && progress.duration > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        stopAudio()
                        // Reset flow state
                        _playbackProgress.value = PlaybackProgress()
                    }
                }
            }
        } catch (e: Exception) {
            //loggingRepository.logWarning("Failed to play audio: ${e.message}")
            println("Failed to play audio: ${e.message}")
            stopAudio()
        }
    }

    suspend fun pauseAudio() {
        try {
            audioRecorderPlayer?.pausePlaying()
            isPlaying = false
        } catch (e: Exception) {
            //loggingRepository.logWarning("Failed to pause audio: ${e.message}")
            println("Failed to pause audio: ${e.message}")
        }
    }

    suspend fun stopAudio() {
        try {
            audioRecorderPlayer?.stopPlaying()
            audioRecorderPlayer?.removeListeners() // Clean up listeners
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