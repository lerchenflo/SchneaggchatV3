@file:OptIn(ExperimentalForeignApi::class)
package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

@OptIn(BetaInteropApi::class)
actual class AudioManager {
    actual fun initializeAudio() {
    }

    private val basePath: String
        get() = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String

    actual fun getRecordingPath(filename: String): String {
        //val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val cachesDirectory = basePath
        return "$cachesDirectory/$filename"
    }

    actual suspend fun saveAudioToStorage(audioBytes: ByteArray, filename: String): String =
        withContext(Dispatchers.Default) {
            val nsData = audioBytes.toNSData()
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

    private fun ByteArray.toNSData(): NSData = memScoped {
        NSData.create(
            bytes = allocArrayOf(this@toNSData),
            length = this@toNSData.size.toULong()
        )
    }
}
