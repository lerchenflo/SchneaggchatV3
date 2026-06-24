package io.github.lerchenflo.voicemessages

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class VoicePlayer actual constructor() {

    private var player: AVAudioPlayer? = null

    actual val positionMs: Long
        get() = player?.let { (it.currentTime * 1_000).toLong() } ?: 0L

    actual val durationMs: Long
        get() = player?.let { (it.duration * 1_000).toLong() } ?: 0L

    actual val isPlaying: Boolean
        get() = player?.isPlaying() ?: false

    actual fun play(filePath: String) {
        stop()
        val url = NSURL.fileURLWithPath(filePath)

        memScoped {
            // Use PlayAndRecord category to allow seamless transitions between
            // recording and playback without audio session conflicts.
            val session = AVAudioSession.sharedInstance()

            val sessionErrorVar = alloc<ObjCObjectVar<NSError?>>()
            // Use DefaultToSpeaker option to route playback to speaker instead of earpiece
            session.setCategory(
                AVAudioSessionCategoryPlayAndRecord,
                AVAudioSessionModeDefault,
                AVAudioSessionCategoryOptionDefaultToSpeaker,
                sessionErrorVar.ptr
            )
            val sessionError = sessionErrorVar.value
            if (sessionError != null) {
                throw IllegalStateException(
                    "Failed to set AVAudioSession category for playback: ${sessionError.localizedDescription}"
                )
            }

            val sessionActiveErrorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setActive(true, error = sessionActiveErrorVar.ptr)
            val sessionActiveError = sessionActiveErrorVar.value
            if (sessionActiveError != null) {
                throw IllegalStateException(
                    "Failed to activate AVAudioSession for playback: ${sessionActiveError.localizedDescription}"
                )
            }

            val playerErrorVar = alloc<ObjCObjectVar<NSError?>>()
            val avPlayer = AVAudioPlayer(contentsOfURL = url, error = playerErrorVar.ptr)
            val playerError = playerErrorVar.value
            if (playerError != null) {
                throw IllegalStateException(
                    "Failed to create AVAudioPlayer for $filePath: ${playerError.localizedDescription}"
                )
            }

            if (!avPlayer.prepareToPlay()) {
                throw IllegalStateException("AVAudioPlayer.prepareToPlay() failed")
            }

            avPlayer.play()
            player = avPlayer
        }
    }

    actual fun pause() {
        player?.takeIf { it.isPlaying() }?.pause()
    }

    actual fun resume() {
        player?.takeIf { !it.isPlaying() }?.play()
    }

    actual fun seekTo(positionMs: Long) {
        player?.currentTime = positionMs / 1_000.0
    }

    actual fun stop() {
        player?.stop()
        player = null

        // Deactivate the audio session to allow recording to take over
        memScoped {
            val session = AVAudioSession.sharedInstance()
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setActive(false, error = errorVar.ptr)
            // Ignore errors - session might not be active
        }
    }
}
