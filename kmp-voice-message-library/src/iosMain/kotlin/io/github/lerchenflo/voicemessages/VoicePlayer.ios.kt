package io.github.lerchenflo.voicemessages

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
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
        val avPlayer = AVAudioPlayer(contentsOfURL = url, error = null)
        avPlayer.prepareToPlay()
        avPlayer.play()
        player = avPlayer
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
