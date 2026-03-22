package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.Context
import io.github.hyochan.audio.initializeAudioRecorderPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.media.MediaMetadataRetriever

actual class AudioManager(private val context: Context){
    actual fun initializeAudio() {
        initializeAudioRecorderPlayer(context)
    }

    actual fun getRecordingPath(filename: String): String {
        return File(context.cacheDir, filename).absolutePath
    }

    private fun saveAudioBytes(data: ByteArray, filename: String): String {
        val file = File(context.filesDir, filename)
        file.parentFile?.mkdirs()
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }

    actual suspend fun saveAudioToStorage(audioBytes: ByteArray, filename: String): String =
        withContext(Dispatchers.IO) {
            saveAudioBytes(audioBytes, filename)
        }

    actual suspend fun deleteAudio(filename: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (!file.exists()) return@withContext false
            file.delete()
        }

    actual fun checkAudioExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    actual suspend fun getMediaDuration(path: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    /*
    actual fun checkFilePermissions(yourPathString: String){
        println("DEBUG: Checking file permissions for path: $yourPathString")
        val file = java.io.File(yourPathString)
        if (file.exists()) {
            println("DEBUG: File exists! Size: ${file.length()} bytes")
            println("DEBUG: Readable: ${file.canRead()}")
        } else {
            println("DEBUG: File DOES NOT exist at this path.")
        }
    }

     */

}
