package io.github.lerchenflo.voicemessages

import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

internal actual class VoicePlayer actual constructor() {

    private var clip: Clip? = null

    actual val positionMs: Long
        get() = clip?.let { it.microsecondPosition / 1_000 } ?: 0L

    actual val durationMs: Long
        get() = clip?.let { it.microsecondLength / 1_000 } ?: 0L

    actual val isPlaying: Boolean
        get() = clip?.isRunning ?: false

    actual fun play(filePath: String) {
        stop()
        val audioStream = AudioSystem.getAudioInputStream(File(filePath))
        val newClip = AudioSystem.getClip()
        newClip.open(audioStream)
        newClip.start()
        clip = newClip
    }

    actual fun pause() {
        clip?.takeIf { it.isRunning }?.stop()
    }

    actual fun stop() {
        val currentClip = clip ?: return
        currentClip.stop()
        currentClip.close()
        clip = null
    }
}
