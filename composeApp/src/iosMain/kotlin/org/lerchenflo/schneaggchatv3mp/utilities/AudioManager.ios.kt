@file:OptIn(ExperimentalForeignApi::class)
package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
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
    actual suspend fun deleteAudio(filename: String): Boolean =
        withContext(Dispatchers.Default) {
            val fileManager = NSFileManager.defaultManager
            val filePath = "$basePath/$filename"
            return@withContext fileManager.removeItemAtPath(filePath, null)
        }

    actual fun checkAudioExists(filePath: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        return fileManager.fileExistsAtPath(filePath)
    }

    actual suspend fun getMediaDuration(path: String): Long {
        return withContext(Dispatchers.Default) {
            try {
                // Normalize the path - if it doesn't start with /, prepend the base path
                val fullPath = if (path.startsWith("/")) {
                    // For absolute paths, extract filename and resolve to current basePath
                    // This handles stale container UUIDs from old paths
                    val filename = path.substringAfterLast('/')
                    "$basePath/$filename"
                } else {
                    "$basePath/$path"
                }

                // Check if file exists
                val fileManager = NSFileManager.defaultManager
                if (!fileManager.fileExistsAtPath(fullPath)) {
                    return@withContext 0L
                }

                val url = NSURL.fileURLWithPath(fullPath)

                // Use AVAudioPlayer which loads duration synchronously
                val audioPlayer = AVAudioPlayer(contentsOfURL = url, error = null)
                if (audioPlayer == null) {
                    return@withContext 0L
                }

                val seconds = audioPlayer.duration
                val durationMs = (seconds * 1000).toLong()
                return@withContext durationMs
            } catch (e: Exception) {
                0L
            }
        }
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
