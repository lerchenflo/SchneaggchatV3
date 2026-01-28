package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import okio.ByteString.Companion.decodeBase64
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import java.awt.Image
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter

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

    actual fun getProfilePicFilePath(id: Long, gruppe: Boolean): String {
        val filename = id.toString() + if (gruppe) GROUPPROFILEPICTURE_FILE_NAME else USERPROFILEPICTURE_FILE_NAME

        return getPath(filename)
    }

    actual fun checkImageExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    actual suspend fun downscaleImage(
        imageBytes: ByteArray,
        targetSizeBytes: Int
    ): ByteArray {
        // If original is already small enough, return it
        if (imageBytes.size <= targetSizeBytes) {
            println("Desktop: Image already under target (${imageBytes.size} <= $targetSizeBytes), returning original")
            return imageBytes
        }

        var originalImage = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: throw IllegalArgumentException("Invalid image data")

        // Convert to RGB if needed (JPEG doesn't support alpha channel)
        var bufferedImage = if (originalImage.type != BufferedImage.TYPE_INT_RGB) {
            val rgbImage = BufferedImage(originalImage.width, originalImage.height, BufferedImage.TYPE_INT_RGB)
            val g = rgbImage.createGraphics()
            g.drawImage(originalImage, 0, 0, null)
            g.dispose()
            rgbImage
        } else {
            originalImage
        }

        val writer: ImageWriter = ImageIO.getImageWritersByFormatName("jpeg").next()
        val writeParam = writer.defaultWriteParam

        // Enable compression
        writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT

        var quality = 0.9f
        var resultBytes: ByteArray

        // First try with original size at reduced quality
        do {
            val outputStream = ByteArrayOutputStream()
            val ios = ImageIO.createImageOutputStream(outputStream)
            writer.output = ios
            writeParam.compressionQuality = quality

            writer.write(null, IIOImage(bufferedImage, null, null), writeParam)
            ios.close()

            resultBytes = outputStream.toByteArray()

            if (resultBytes.size <= targetSizeBytes) {
                break
            }
            quality -= 0.1f
        } while (quality > 0.1f)

        // If still too large, resize the image
        if (resultBytes.size > targetSizeBytes) {
            var scale = 0.9
            while (scale > 0.1) {
                val newWidth = (bufferedImage.width * scale).toInt()
                val newHeight = (bufferedImage.height * scale).toInt()

                // Create scaled image
                val scaledImage = bufferedImage.getScaledInstance(
                    newWidth,
                    newHeight,
                    Image.SCALE_SMOOTH
                )

                // Convert to BufferedImage (RGB for JPEG)
                val resizedBufferedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
                val g = resizedBufferedImage.createGraphics()
                g.drawImage(scaledImage, 0, 0, null)
                g.dispose()

                bufferedImage = resizedBufferedImage
                quality = 0.9f

                // Try compression again with resized image
                do {
                    val outputStream = ByteArrayOutputStream()
                    val ios = ImageIO.createImageOutputStream(outputStream)
                    writer.output = ios
                    writeParam.compressionQuality = quality

                    writer.write(null, IIOImage(bufferedImage, null, null), writeParam)
                    ios.close()

                    resultBytes = outputStream.toByteArray()

                    if (resultBytes.size <= targetSizeBytes) {
                        break
                    }
                    quality -= 0.1f
                } while (quality > 0.1f)

                if (resultBytes.size <= targetSizeBytes) {
                    break
                }

                scale -= 0.1
            }
        }

        writer.dispose()

        println("Desktop downscale: ${imageBytes.size} bytes -> ${resultBytes.size} bytes")
        return resultBytes
    }
}