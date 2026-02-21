@file:OptIn(ExperimentalForeignApi::class)

package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

@OptIn(BetaInteropApi::class)
actual class PictureManager {

    private val basePath: String
        get() = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String

    actual suspend fun savePictureToStorage(
        base64Pic: String,
        filename: String
    ): String = withContext(Dispatchers.Default) {
        val cleanBase64 = base64Pic.substringAfterLast(",")
        val nsData = NSData.create(
            base64EncodedString = cleanBase64,
            options = NSDataBase64DecodingIgnoreUnknownCharacters
        ) ?: throw IllegalArgumentException("Invalid Base64 string")

        return@withContext saveData(nsData, filename)
    }

    actual suspend fun savePictureToStorage(
        data: ByteArray,
        filename: String
    ): String = withContext(Dispatchers.Default) {
        val nsData = data.toNSData()
        return@withContext saveData(nsData, filename)
    }

    private fun saveData(data: NSData, filename: String): String {
        val filePath = "$basePath/$filename"
        val success = data.writeToFile(
            path = filePath,
            atomically = true
        )
        if (!success) {
            throw RuntimeException("Failed to save image to: $filePath")
        }
        return filePath
    }

    actual suspend fun loadPictureFromStorage(filename: String): ImageBitmap? =
        withContext(Dispatchers.Default) {
            val filePath = "$basePath/$filename"
            val data = NSData.dataWithContentsOfFile(filePath) ?: return@withContext null
            val uiImage = UIImage.imageWithData(data) ?: return@withContext null
            return@withContext uiImage.toImageBitmap()
        }

    actual suspend fun deletePicture(filename: String): Boolean =
        withContext(Dispatchers.Default) {
            val fileManager = NSFileManager.defaultManager
            val filePath = "$basePath/$filename"
            return@withContext fileManager.removeItemAtPath(filePath, null)
        }

    private fun ByteArray.toNSData(): NSData = memScoped {
        NSData.create(
            bytes = allocArrayOf(this@toNSData),
            length = this@toNSData.size.toULong()
        )
    }

    private fun UIImage.toImageBitmap(): ImageBitmap {
        // Convert UIImage to JPEG data first (you could also use PNG)
        val imageData = UIImageJPEGRepresentation(this, 1.0)
            ?: throw RuntimeException("Failed to convert UIImage to data")

        // Convert NSData to ByteArray
        val byteArray = imageData.toByteArray()

        // Create Skia Image and convert to ImageBitmap
        val skiaImage = Image.makeFromEncoded(byteArray)
        return skiaImage.toComposeImageBitmap()
    }

    private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), bytes, length)
        }
    }

    actual fun getProfilePicFilePath(id: String, gruppe: Boolean): String {
        val filename = id + if(gruppe) GROUPPROFILEPICTURE_FILE_NAME else USERPROFILEPICTURE_FILE_NAME

        return "$basePath/$filename"
    }

    actual fun checkImageExists(filePath: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        return fileManager.fileExistsAtPath(filePath)
    }

    actual suspend fun downscaleImage(
        imageBytes: ByteArray,
        targetSizeBytes: Int
    ): ByteArray = withContext(Dispatchers.Default) {
        // If original is already small enough, return it
        if (imageBytes.size <= targetSizeBytes) {
            println("iOS: Image already under target (${imageBytes.size} <= $targetSizeBytes), returning original")
            return@withContext imageBytes
        }

        val nsData = imageBytes.toNSData()
        var uiImage = UIImage.imageWithData(nsData)
            ?: throw IllegalArgumentException("Invalid image data")

        // Calculate compression ratio needed: target / current
        val compressionRatio = targetSizeBytes.toFloat() / imageBytes.size

        // Estimate starting quality based on compression ratio (0.0-1.0 scale for iOS)
        var quality = when {
            compressionRatio >= 0.85f -> 0.85  // Need minimal compression
            compressionRatio >= 0.7f -> 0.7    // Moderate compression
            compressionRatio >= 0.5f -> 0.5    // Significant compression
            else -> 0.3                         // Heavy compression needed
        }

        // Estimate if we need resizing based on compression ratio
        var estimatedScale = if (compressionRatio < 0.5f) {
            kotlin.math.sqrt(compressionRatio.toDouble())
        } else {
            1.0
        }

        println("iOS: Compression ratio: $compressionRatio, starting quality: $quality, estimated scale: $estimatedScale")

        // If we estimate resizing is needed, do it first
        if (estimatedScale < 0.95) {
            memScoped {
                val originalSize = uiImage.size
                val newWidth = (originalSize.useContents { width } * estimatedScale).coerceAtLeast(100.0)
                val newHeight = (originalSize.useContents { height } * estimatedScale).coerceAtLeast(100.0)

                val newSize = CGSizeMake(newWidth, newHeight)
                UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
                uiImage.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
                val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()

                if (resizedImage != null) {
                    uiImage = resizedImage
                    println("iOS: Pre-scaled to ${newWidth}x${newHeight}")
                }
            }
        }

        var resultData: NSData?
        var attempts = 0
        val maxAttempts = 15

        // Iteratively adjust quality and size
        do {
            resultData = UIImageJPEGRepresentation(uiImage, quality)
            attempts++

            if (resultData == null || resultData.length.toInt() <= targetSizeBytes) {
                // We hit the target!
                break
            }

            // Calculate how much we overshot and adjust
            val overshootRatio = resultData.length.toFloat() / targetSizeBytes

            if (overshootRatio > 1.5 && uiImage.size.useContents { width } > 200.0) {
                // We're way over, need to resize more aggressively
                memScoped {
                    val scaleReduction = 0.85
                    val currentSize = uiImage.size
                    val newWidth = currentSize.useContents { width } * scaleReduction
                    val newHeight = currentSize.useContents { height } * scaleReduction

                    val newSize = CGSizeMake(newWidth, newHeight)
                    UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
                    uiImage.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
                    val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
                    UIGraphicsEndImageContext()

                    if (resizedImage != null) {
                        uiImage = resizedImage
                        quality = 0.8 // Reset quality after resize
                        println("iOS: Resized to ${newWidth}x${newHeight} (overshoot: ${overshootRatio}x)")
                    }
                }
            } else {
                // Fine-tune with quality adjustment
                quality -= 0.05
                if (quality < 0.1) {
                    break
                }
            }

        } while (attempts < maxAttempts)

        val finalData = resultData ?: throw RuntimeException("Failed to downscale image")
        val finalBytes = finalData.toByteArray()

        return@withContext finalBytes
    }
}