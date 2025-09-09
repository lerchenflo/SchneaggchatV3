package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import okio.ByteString.Companion.decodeBase64

actual class PictureManager {

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

    private fun saveBytesToFile(data: ByteArray, filename: String): String {
        val file = File(getPath(filename))
        file.parentFile?.mkdirs()
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }

    actual suspend fun savePictureToStorage(base64Pic: String, filename: String): String {
        // Remove data URL prefix if present :cite[7]
        val pureBase64 = base64Pic.substringAfterLast(",", base64Pic)
        val bytes = pureBase64.decodeBase64()?.toByteArray() ?: throw IllegalArgumentException("Invalid Base64 string")
        return saveBytesToFile(bytes, filename)
    }

    actual suspend fun savePictureToStorage(data: ByteArray, filename: String): String {
        return saveBytesToFile(data, filename)
    }

    actual suspend fun loadPictureFromStorage(filename: String): ImageBitmap? {
        val file = File(getPath(filename))
        if (!file.exists()) return null

        return try {
            // Read the file as bytes and use Compose's native image decoding :cite[5]:cite[8]
            val bytes = file.readBytes()

            // For Compose Desktop 1.0+ use ImageBitmap.makeFromEncoded
            // Note: This is the preferred method as it uses Skia internally
            try {
                // Try to use the modern API first
                val method = ImageBitmap::class.java.getMethod("makeFromEncoded", ByteArray::class.java)
                method.invoke(null, bytes) as ImageBitmap
            } catch (e: NoSuchMethodException) {
                // Fallback to BufferedImage approach for older versions
                val bufferedImage: BufferedImage = ImageIO.read(file)
                bufferedImage.toComposeImageBitmap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual suspend fun deletePicture(filename: String): Boolean {
        return File(getPath(filename)).delete()
    }
}