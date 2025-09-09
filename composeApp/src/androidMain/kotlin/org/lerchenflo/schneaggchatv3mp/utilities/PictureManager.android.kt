package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.bytestring.decode
import org.lerchenflo.schneaggchatv3mp.utilities.Base64.decodeFromBase64
import java.io.File
import kotlin.io.encoding.ExperimentalEncodingApi

actual class PictureManager(private val context: Context) {

    @OptIn(ExperimentalEncodingApi::class)
    actual suspend fun savePictureToStorage(base64Pic: String, filename: String): String =
        withContext(Dispatchers.IO) {
            val bytes = base64Pic.encodeToByteArray()
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

    actual suspend fun loadPictureFromStorage(filename: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (!file.exists()) return@withContext null
            return@withContext file.readBytes()
        }

    actual suspend fun deletePicture(filename: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (!file.exists()) return@withContext false
            return@withContext file.delete()
        }
}