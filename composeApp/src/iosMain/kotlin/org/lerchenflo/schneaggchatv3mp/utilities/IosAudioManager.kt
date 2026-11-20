@file:OptIn(ExperimentalForeignApi::class)
package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(BetaInteropApi::class)
class IosAudioManager : AudioManager {

    private var beepPlayer: AVAudioPlayer? = null
    private var beepPlayerFrequencyHz = 0

    override fun prepareBeep(frequencyHz: Int) {
        if (beepPlayer != null && beepPlayerFrequencyHz == frequencyHz) return
        releaseBeep()

        val player = AVAudioPlayer(data = beepWavBytes(frequencyHz).toNSData(), error = null)
        player.numberOfLoops = -1
        player.prepareToPlay()
        beepPlayer = player
        beepPlayerFrequencyHz = frequencyHz
    }

    override fun startBeep(frequencyHz: Int) {
        prepareBeep(frequencyHz)
        val player = beepPlayer ?: return
        player.currentTime = 0.0
        player.play()
    }

    override fun stopBeep() {
        val player = beepPlayer ?: return
        player.pause()
        player.currentTime = 0.0
    }

    override fun releaseBeep() {
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

    private fun ByteArray.toNSData(): NSData = memScoped {
        NSData.create(
            bytes = allocArrayOf(this@toNSData),
            length = this@toNSData.size.toULong()
        )
    }
}

private const val WAV_HEADER_SIZE = 44
private const val WAV_FORMAT_PCM = 1
