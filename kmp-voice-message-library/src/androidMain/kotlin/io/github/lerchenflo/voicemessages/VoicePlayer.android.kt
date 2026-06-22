package io.github.lerchenflo.voicemessages

import android.media.MediaPlayer

actual class VoicePlayer actual constructor() {

    private var mediaPlayer: MediaPlayer? = null

    actual val positionMs: Long
        get() = mediaPlayer?.let { runCatching { it.currentPosition.toLong() }.getOrDefault(0L) } ?: 0L

    actual val durationMs: Long
        get() = mediaPlayer?.let { runCatching { it.duration.toLong() }.getOrDefault(0L) } ?: 0L

    actual val isPlaying: Boolean
        get() = mediaPlayer?.let { runCatching { it.isPlaying }.getOrDefault(false) } ?: false

    actual fun play(filePath: String) {
        stop()
        val player = MediaPlayer()
        player.setDataSource(filePath)
        player.prepare()
        player.start()
        mediaPlayer = player
    }

    actual fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
    }

    actual fun resume() {
        mediaPlayer?.takeIf { !it.isPlaying }?.start()
    }

    actual fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
    }

    actual fun stop() {
        val player = mediaPlayer ?: return
        runCatching { player.stop() }
        player.release()
        mediaPlayer = null
    }
}
