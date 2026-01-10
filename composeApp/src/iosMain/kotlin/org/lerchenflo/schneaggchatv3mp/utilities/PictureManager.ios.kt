@file:OptIn(ExperimentalForeignApi::class)

package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
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
}