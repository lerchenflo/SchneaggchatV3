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

    actual fun getProfilePicFilePath(id: Long, gruppe: Boolean): String {
        val filename = id.toString() + if(gruppe) GROUPPROFILEPICTURE_FILE_NAME else USERPROFILEPICTURE_FILE_NAME

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
        val nsData = imageBytes.toNSData()
        var uiImage = UIImage.imageWithData(nsData)
            ?: throw IllegalArgumentException("Invalid image data")

        // Helper function to get compressed data size
        fun getCompressedSize(image: UIImage, quality: Double): NSData? {
            return UIImageJPEGRepresentation(image, quality)
        }

        // Helper function to resize image
        fun resizeImage(image: UIImage, scale: Double): UIImage? {
            return memScoped {
                val originalSize = image.size
                val newWidth = originalSize.useContents { width } * scale
                val newHeight = originalSize.useContents { height } * scale
                val newSize = CGSizeMake(newWidth, newHeight)

                UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
                image.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
                val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()

                resizedImage
            }
        }

        var currentImage = uiImage
        var currentQuality = 0.9
        var resultData: NSData? = null

        // Strategy 1: Try reducing quality first (maintains resolution)
        while (currentQuality >= 0.1) {
            resultData = getCompressedSize(currentImage, currentQuality)

            if (resultData != null && resultData.length.toInt() <= targetSizeBytes) {
                // Success! We found a quality that works
                return@withContext resultData.toByteArray()
            }

            currentQuality -= 0.1
        }

        // Strategy 2: If quality reduction wasn't enough, start reducing size
        var currentScale = 0.9

        while (currentScale >= 0.1) {
            val resizedImage = resizeImage(uiImage, currentScale)

            if (resizedImage == null) {
                currentScale -= 0.1
                continue
            }

            currentImage = resizedImage
            currentQuality = 0.9

            // Try different quality levels with this new size
            while (currentQuality >= 0.1) {
                resultData = getCompressedSize(currentImage, currentQuality)

                if (resultData != null && resultData.length.toInt() <= targetSizeBytes) {
                    // Success! We found a size/quality combo that works
                    return@withContext resultData.toByteArray()
                }

                currentQuality -= 0.1
            }

            // This size didn't work even at lowest quality, try smaller
            currentScale -= 0.1
        }

        // If we get here, even the smallest size at lowest quality is too big
        // Return the best we could do
        val finalData = resultData
            ?: UIImageJPEGRepresentation(currentImage, 0.1)
            ?: throw RuntimeException("Failed to compress image")

        return@withContext finalData.toByteArray()
    }
}