package io.github.lerchenflo.voicemessages.internal

import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

internal actual fun probeDurationMs(filePath: String): Long {
    return try {
        val format = AudioSystem.getAudioFileFormat(File(filePath))
        val frameLength = format.frameLength
        val frameRate = format.format.frameRate
        if (frameLength != AudioSystem.NOT_SPECIFIED && frameRate > 0f) {
            ((frameLength / frameRate) * 1_000).toLong()
        } else {
            0L
        }
    } catch (e: UnsupportedAudioFileException) {
        0L
    } catch (e: IOException) {
        0L
    }
}
