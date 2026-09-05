package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

actual class AudioManager {

    private var beepClip: Clip? = null
    private var beepClipGain: FloatControl? = null
    private var beepClipFrequencyHz = 0

    private fun getPath(filename: String): String {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val appDataDir = when {
            os.contains("win") -> File(System.getenv("APPDATA"), "Schneaggchat")
            os.contains("mac") -> File(userHome, "Library/Application Support/Schneaggchat")
            else -> File(userHome, ".local/share/Schneaggchat")
        }

        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
        return File(appDataDir, filename).absolutePath
    }
    actual fun initializeAudio() {
    }

    actual fun getRecordingPath(filename: String): String {
        return getPath(filename)
    }

    actual suspend fun saveAudioToStorage(audioBytes: ByteArray, filename: String): String {
        return saveBytesToFile(audioBytes, filename)
    }

    actual suspend fun deleteAudio(filename: String): Boolean {
        return File(getPath(filename)).delete()
    }

    actual fun checkAudioExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    actual suspend fun getMediaDuration(path: String): Long = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext 0L

            val audioInputStream = AudioSystem.getAudioInputStream(file)
            val format = audioInputStream.format
            val frames = audioInputStream.frameLength
            val frameRate = format.frameRate

            // Duration in milliseconds: (frames / frameRate) * 1000
            val durationMs = (frames / frameRate * 1000).toLong()

            audioInputStream.close()
            durationMs
        } catch (e: Exception) {
            println("Desktop: Failed to get duration (likely unsupported format): ${e.message}")

            // Fallback for MP3/MPEG if using SPI libraries (see below)
            try {
                val fileFormat = AudioSystem.getAudioFileFormat(File(path))
                val properties = fileFormat.properties()
                val microseconds = properties["duration"] as? Long ?: 0L
                microseconds / 1000
            } catch (ex: Exception) {
                0L
            }
        }
    }

    actual fun prepareBeep(frequencyHz: Int) {
        if (beepClip != null && beepClipFrequencyHz == frequencyHz) return
        releaseBeep()

        try {
            val pcm = generateBeepPcm16(frequencyHz)
            val format = AudioFormat(BEEP_SAMPLE_RATE_HZ.toFloat(), 16, 1, true, false)
            val clip = AudioSystem.getClip()
            clip.open(format, pcm, 0, pcm.size)
            beepClip = clip
            beepClipFrequencyHz = frequencyHz

            // Where the mixer offers a gain control the tone loops silently for as long as it stays
            // prepared, so starting it is only a gain change instead of starting playback.
            beepClipGain = clip.takeIf { it.isControlSupported(FloatControl.Type.MASTER_GAIN) }
                ?.let { it.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl }
            beepClipGain?.let {
                it.value = it.minimum
                clip.loop(Clip.LOOP_CONTINUOUSLY)
            }
        } catch (e: Exception) {
            println("Desktop: Failed to prepare beep: ${e.message}")
        }
    }

    actual fun startBeep(frequencyHz: Int) {
        prepareBeep(frequencyHz)
        val clip = beepClip ?: return
        val gain = beepClipGain
        if (gain == null) {
            clip.framePosition = 0
            clip.loop(Clip.LOOP_CONTINUOUSLY)
        } else {
            gain.value = 0f.coerceIn(gain.minimum, gain.maximum)
        }
    }

    actual fun stopBeep() {
        val gain = beepClipGain
        if (gain == null) {
            beepClip?.stop()
        } else {
            gain.value = gain.minimum
        }
    }

    actual fun releaseBeep() {
        beepClip?.let {
            it.stop()
            it.close()
        }
        beepClip = null
        beepClipGain = null
        beepClipFrequencyHz = 0
    }

    private fun saveBytesToFile(data: ByteArray, filename: String): String {
        val file = File(getPath(filename))
        file.parentFile?.mkdirs()
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }
}