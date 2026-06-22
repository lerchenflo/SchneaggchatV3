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
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
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
