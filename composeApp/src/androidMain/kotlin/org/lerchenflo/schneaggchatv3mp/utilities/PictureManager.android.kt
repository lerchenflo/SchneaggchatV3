package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.decodeBase64
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.core.graphics.scale

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

    actual fun getProfilePicFilePath(id: String, gruppe: Boolean) : String {

        val filename = id + if (gruppe) GROUPPROFILEPICTURE_FILE_NAME else USERPROFILEPICTURE_FILE_NAME

        return File(context.filesDir, filename).absolutePath
    }

    actual fun checkImageExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    actual suspend fun downscaleImage(
        imageBytes: ByteArray,
        targetSizeBytes: Int,
    ): ByteArray = withContext(Dispatchers.IO) {
        // If original is already small enough, return it
        if (imageBytes.size <= targetSizeBytes) {
            Log.d("Android downscale", "Image already under target (${imageBytes.size} <= $targetSizeBytes), returning original")
            return@withContext imageBytes
        }

        var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalArgumentException("Invalid image data")

        var quality = 90
        var resultBytes: ByteArray

        // First try with original size at reduced quality
        do {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            resultBytes = outputStream.toByteArray()

            if (resultBytes.size <= targetSizeBytes) {
                break
            }
            quality -= 10
        } while (quality > 10)

        // If still too large, resize the image
        if (resultBytes.size > targetSizeBytes) {
            var scale = 0.9f
            while (scale > 0.1f) {
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()

                val resizedBitmap = bitmap.scale(newWidth, newHeight)

                // Reset quality for resized bitmap
                quality = 90

                // Try compression again with resized image
                do {
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    resultBytes = outputStream.toByteArray()

                    if (resultBytes.size <= targetSizeBytes) {
                        if (resizedBitmap != bitmap) {
                            bitmap.recycle()
                        }
                        bitmap = resizedBitmap
                        break
                    }
                    quality -= 10
                } while (quality > 10)

                if (resultBytes.size <= targetSizeBytes) {
                    break
                }

                if (resizedBitmap != bitmap) {
                    resizedBitmap.recycle()
                }
                scale -= 0.1f
            }
        }

        // Clean up bitmap
        bitmap.recycle()

        Log.d("Android downscale", "Downscaled image from ${imageBytes.size} to ${resultBytes.size} bytes")
        return@withContext resultBytes
    }
}
