package org.lerchenflo.schneaggchatv3mp.utilities

import java.io.File

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
    private fun saveBytesToFile(data: ByteArray, filename: String): String {
        val file = File(getPath(filename))
        file.parentFile?.mkdirs()
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }
}