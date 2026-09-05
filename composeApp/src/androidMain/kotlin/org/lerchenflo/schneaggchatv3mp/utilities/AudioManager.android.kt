package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaMetadataRetriever
import android.media.AudioManager as SystemAudioManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class AudioManager(private val context: Context){

    private var beepTrack: AudioTrack? = null
    private var beepTrackFrequencyHz = 0

    private val outputSampleRateHz: Int by lazy {
        val systemAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager
        systemAudioManager.getProperty(SystemAudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
            ?: BEEP_SAMPLE_RATE_HZ
    }

    actual fun initializeAudio() {
        // No global init needed for MediaRecorder/MediaPlayer (matches iOS/JVM no-op actuals).
    }

    actual fun getRecordingPath(filename: String): String {
        // Use filesDir (the same persistent directory as saveAudioToStorage/deleteAudio) so that
        // recording, saving, existence checks and playback all resolve to the same location.
        // cacheDir can be evicted by the OS and diverged from where downloaded audio is stored.
        return File(context.filesDir, filename).absolutePath
    }

    private fun saveAudioBytes(data: ByteArray, filename: String): String {
        val file = File(context.filesDir, filename)
        file.parentFile?.mkdirs()
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }

    actual suspend fun saveAudioToStorage(audioBytes: ByteArray, filename: String): String =
        withContext(Dispatchers.IO) {
            saveAudioBytes(audioBytes, filename)
        }

    actual suspend fun deleteAudio(filename: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (!file.exists()) return@withContext false
            file.delete()
        }

    actual fun checkAudioExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    actual suspend fun getMediaDuration(path: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    actual fun prepareBeep(frequencyHz: Int) {
        if (beepTrack != null && beepTrackFrequencyHz == frequencyHz) return
        releaseBeep()

        val pcm = generateBeepPcm16(frequencyHz, outputSampleRateHz)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(outputSampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        try {
            track.write(pcm, 0, pcm.size)
            track.setLoopPoints(0, pcm.size / 2, -1)
            // The tone loops silently for as long as it stays prepared: starting it is then only a
            // volume change on an already running mixer, which is far faster than starting playback.
            track.setVolume(0f)
            track.play()
            beepTrack = track
            beepTrackFrequencyHz = frequencyHz
        } catch (e: Exception) {
            track.release()
            println("Failed to prepare beep: ${e.message}")
        }
    }

    actual fun startBeep(frequencyHz: Int) {
        prepareBeep(frequencyHz)
        beepTrack?.setVolume(1f)
    }

    actual fun stopBeep() {
        beepTrack?.setVolume(0f)
    }

    actual fun releaseBeep() {
        val track = beepTrack ?: return
        beepTrack = null
        beepTrackFrequencyHz = 0
        try {
            track.pause()
        } catch (e: IllegalStateException) {
            println("Failed to stop beep: ${e.message}")
        }
        track.release()
    }

    /*
    actual fun checkFilePermissions(yourPathString: String){
        println("DEBUG: Checking file permissions for path: $yourPathString")
        val file = java.io.File(yourPathString)
        if (file.exists()) {
            println("DEBUG: File exists! Size: ${file.length()} bytes")
            println("DEBUG: Readable: ${file.canRead()}")
        } else {
            println("DEBUG: File DOES NOT exist at this path.")
        }
    }

     */

}
