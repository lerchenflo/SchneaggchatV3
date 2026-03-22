package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.AudioSystem

actual class AudioManager {
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
        TODO("Not yet implemented")
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

    private fun saveBytesToFile(data: ByteArray, filename: String): String {
        val file = File(getPath(filename))
        file.parentFile?.mkdirs()
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }
}