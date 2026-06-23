package io.github.lerchenflo.voicemessages

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
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
            // Activate the audio session before playback too — without it, playback
            // can fail or stay silent even for a valid file. Re-using PlayAndRecord
            // keeps this compatible with a recorder that's already configured it.
            val session = AVAudioSession.sharedInstance()
            val sessionErrorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = sessionErrorVar.ptr)

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

            avPlayer.prepareToPlay()
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
    }
}
