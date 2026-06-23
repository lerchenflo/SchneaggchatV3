package io.github.lerchenflo.voicemessages

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFAudio.AVAudioQualityHigh
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSError
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
actual class VoiceRecorder actual constructor() {

    private var recorder: AVAudioRecorder? = null
    private var recording: Boolean = false

    actual val isRecording: Boolean
        get() = recording

    actual suspend fun start(filePath: String) {
        val url = NSURL.fileURLWithPath(filePath)
        // Tuned for voice rather than music: mono, 16kHz (well above speech bandwidth),
        // 64kbps (the standard "sweet spot" for mono voice at this sample rate).
        val settings: Map<Any?, *> = mapOf(
            AVFormatIDKey to kAudioFormatMPEG4AAC,
            AVSampleRateKey to 16_000.0,
            AVNumberOfChannelsKey to 1,
            AVEncoderAudioQualityKey to AVAudioQualityHigh,
            AVEncoderBitRateKey to 64_000,
        )

        memScoped {
            // The audio session must be configured and activated *before* recording,
            // otherwise AVAudioRecorder.record() silently no-ops and produces an
            // empty/invalid file (which then fails to open during playback with
            // "AudioFileObject.cpp:105 OpenFromDataSource failed").
            val session = AVAudioSession.sharedInstance()
            val sessionErrorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = sessionErrorVar.ptr)
            val sessionCategoryError = sessionErrorVar.value
            if (sessionCategoryError != null) {
                throw IllegalStateException(
                    "Failed to set AVAudioSession category: ${sessionCategoryError.localizedDescription}"
                )
            }

            val sessionActiveErrorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setActive(true, error = sessionActiveErrorVar.ptr)
            val sessionActiveError = sessionActiveErrorVar.value
            if (sessionActiveError != null) {
                throw IllegalStateException(
                    "Failed to activate AVAudioSession: ${sessionActiveError.localizedDescription}"
                )
            }

            val recorderErrorVar = alloc<ObjCObjectVar<NSError?>>()
            val avRecorder = AVAudioRecorder(uRL = url, settings = settings, error = recorderErrorVar.ptr)
            val recorderError = recorderErrorVar.value
            if (recorderError != null) {
                throw IllegalStateException(
                    "Failed to create AVAudioRecorder: ${recorderError.localizedDescription}"
                )
            }

            val started = avRecorder.record()
            if (!started) {
                throw IllegalStateException("AVAudioRecorder.record() failed to start recording")
            }

            recorder = avRecorder
            recording = true
        }
    }

    actual fun stop() {
        recorder?.stop()
        recorder = null
        recording = false
    }
}
