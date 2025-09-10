package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.decodeBase64
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import java.io.File

actual class PictureManager(private val context: Context) {

    actual suspend fun savePictureToStorage(base64Pic: String, filename: String): String =
        withContext(Dispatchers.IO) {
            Log.d("ANDROID-Picturesave", "Saving: $filename")

            // ✅ Correct Base64 decoding
            val bytes = base64Pic.decodeBase64()?.toByteArray()
                ?: throw IllegalArgumentException("Invalid Base64 string")

            savePictureBytes(bytes, filename)
        }

    actual suspend fun savePictureToStorage(data: ByteArray, filename: String): String =
        withContext(Dispatchers.IO) {
            savePictureBytes(data, filename)
        }

    private fun savePictureBytes(data: ByteArray, filename: String): String {
        val file = File(context.filesDir, filename)
        file.parentFile?.mkdirs()
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }

    actual suspend fun loadPictureFromStorage(filename: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (!file.exists()) {
                Log.d("Android file load", "File not found: $filename")
                return@withContext null
            }

            try {
                val bytes = file.readBytes()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Log.d("Android file load", "Loaded picture: $filename (${bytes.size} bytes)")
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                Log.e("Android file load", "Error loading picture: $filename", e)
                null
            }
        }

    actual suspend fun deletePicture(filename: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (!file.exists()) return@withContext false
            file.delete()
        }

    actual fun getProfilePicFilePath(id: Long, gruppe: Boolean) : String {

        val filename = id.toString() + if (gruppe) GROUPPROFILEPICTURE_FILE_NAME else USERPROFILEPICTURE_FILE_NAME

        return File(context.filesDir, filename).absolutePath
    }
}
