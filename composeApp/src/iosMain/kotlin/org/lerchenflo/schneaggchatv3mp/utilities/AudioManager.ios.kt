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

    private var beepPlayer: AVAudioPlayer? = null
    private var beepPlayerFrequencyHz = 0

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

                val seconds = audioPlayer.duration
                val durationMs = (seconds * 1000).toLong()
                return@withContext durationMs
            } catch (e: Exception) {
                0L
            }
        }
    }

    actual fun prepareBeep(frequencyHz: Int) {
        if (beepPlayer != null && beepPlayerFrequencyHz == frequencyHz) return
        releaseBeep()

        val player = AVAudioPlayer(data = beepWavBytes(frequencyHz).toNSData(), error = null)
        player.numberOfLoops = -1
        player.prepareToPlay()
        beepPlayer = player
        beepPlayerFrequencyHz = frequencyHz
    }

    actual fun startBeep(frequencyHz: Int) {
        prepareBeep(frequencyHz)
        val player = beepPlayer ?: return
        player.currentTime = 0.0
        player.play()
    }

    actual fun stopBeep() {
        val player = beepPlayer ?: return
        player.pause()
        player.currentTime = 0.0
    }

    actual fun releaseBeep() {
        beepPlayer?.stop()
        beepPlayer = null
        beepPlayerFrequencyHz = 0
    }

    private fun beepWavBytes(frequencyHz: Int): ByteArray {
        val pcm = generateBeepPcm16(frequencyHz)
        return wavHeader(pcm.size) + pcm
    }

    private fun wavHeader(pcmSize: Int): ByteArray {
        val header = ByteArray(WAV_HEADER_SIZE)

        fun putAscii(offset: Int, text: String) {
            text.forEachIndexed { index, char -> header[offset + index] = char.code.toByte() }
        }

        fun putLittleEndian(offset: Int, value: Int, byteCount: Int) {
            for (index in 0 until byteCount) header[offset + index] = (value shr (8 * index)).toByte()
        }

        putAscii(0, "RIFF")
        putLittleEndian(4, WAV_HEADER_SIZE - 8 + pcmSize, 4)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putLittleEndian(16, 16, 4)
        putLittleEndian(20, WAV_FORMAT_PCM, 2)
        putLittleEndian(22, 1, 2)
        putLittleEndian(24, BEEP_SAMPLE_RATE_HZ, 4)
        putLittleEndian(28, BEEP_SAMPLE_RATE_HZ * 2, 4)
        putLittleEndian(32, 2, 2)
        putLittleEndian(34, 16, 2)
        putAscii(36, "data")
        putLittleEndian(40, pcmSize, 4)
        return header
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

private const val WAV_HEADER_SIZE = 44
private const val WAV_FORMAT_PCM = 1
