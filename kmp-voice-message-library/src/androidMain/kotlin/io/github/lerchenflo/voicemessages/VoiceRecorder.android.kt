package io.github.lerchenflo.voicemessages

import android.media.MediaRecorder

actual class VoiceRecorder actual constructor() {

    private var mediaRecorder: MediaRecorder? = null
    private var recording: Boolean = false

    actual val isRecording: Boolean
        get() = recording

    actual suspend fun start(filePath: String) {
        @Suppress("DEPRECATION") // No-arg constructor works on all supported API levels.
        val recorder = MediaRecorder()
        // VOICE_COMMUNICATION enables the device's built-in echo cancellation/noise
        // suppression/AGC, intended for voice calls rather than general audio capture.
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        // Tuned for voice rather than music: mono, 16kHz (well above speech bandwidth),
        // 64kbps (the standard "sweet spot" for mono voice at this sample rate).
        recorder.setAudioEncodingBitRate(64_000)
        recorder.setAudioSamplingRate(16_000)
        recorder.setAudioChannels(1)
        recorder.setOutputFile(filePath)
        recorder.prepare()
        recorder.start()
        mediaRecorder = recorder
        recording = true
    }

    actual fun stop() {
        val recorder = mediaRecorder ?: return
        runCatching { recorder.stop() }
        recorder.release()
        mediaRecorder = null
        recording = false
    }
}
