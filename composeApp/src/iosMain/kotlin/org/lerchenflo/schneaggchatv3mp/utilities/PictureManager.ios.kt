@file:OptIn(ExperimentalForeignApi::class)

package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIImage

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
            file = filePath,
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
            val data = dataWithContentsOfFile(filePath) ?: return@withContext null
            val uiImage = UIImage.imageWithData(data) ?: return@withContext null
            return@withContext uiImage.toImageBitmap()
        }

    actual suspend fun deletePicture(filename: String): Boolean =
        withContext(Dispatchers.Default) {
            val fileManager = NSFileManager.defaultManager
            val filePath = "$basePath/$filename"
            return@withContext fileManager.removeItemAtPath(filePath, null)
        }

    private fun ByteArray.toNSData(): NSData {
        val string = NSString.create(this, NSUTF8StringEncoding)
        return string.dataUsingEncoding(NSUTF8StringEncoding)!!
    }
}