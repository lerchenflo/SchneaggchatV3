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
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetoothA2DP
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
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
            val session = AVAudioSession.sharedInstance()

            val sessionErrorVar = alloc<ObjCObjectVar<NSError?>>()
            // Playback-only category so the session follows the user's selected
            // output route (Bluetooth A2DP, AirPlay, wired, CarPlay) instead of
            // forcing the built-in speaker.
            session.setCategory(
                AVAudioSessionCategoryPlayback,
                AVAudioSessionModeDefault,
                AVAudioSessionCategoryOptionAllowBluetoothA2DP,
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
        val current = player?.takeIf { !it.isPlaying() } ?: return

        memScoped {
            val session = AVAudioSession.sharedInstance()
            val sessionActiveErrorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setActive(true, error = sessionActiveErrorVar.ptr)
            // Ignore activation errors here - play() below will surface a real
            // failure if the session truly can't be used.
        }

        current.play()
    }

    actual fun seekTo(positionMs: Long) {
        player?.currentTime = positionMs / 1_000.0
    }

    actual fun stop() {
        player?.stop()
        player = null

        memScoped {
            val session = AVAudioSession.sharedInstance()
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setActive(
                false,
                withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                error = errorVar.ptr
            )
            // Ignore errors - session might not be active
        }
    }
}
