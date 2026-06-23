@file:OptIn(BetaInteropApi::class)

package io.github.lerchenflo.voicemessages

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioQualityHigh
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
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
            // Pure Record category (matching kmp-audio-recorder-player). Under
            // PlayAndRecord, AVAudioRecorder.prepareToRecord() returns false here.
            val session = AVAudioSession.sharedInstance()
            val categoryErrorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setCategory(AVAudioSessionCategoryRecord, error = categoryErrorVar.ptr)
            val categoryError = categoryErrorVar.value
            if (categoryError != null) {
                throw IllegalStateException(
                    "Failed to set AVAudioSession category: ${categoryError.localizedDescription}"
                )
            }

            val activeErrorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setActive(true, error = activeErrorVar.ptr)
            val activeError = activeErrorVar.value
            if (activeError != null) {
                throw IllegalStateException(
                    "Failed to activate AVAudioSession: ${activeError.localizedDescription}"
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

            if (!avRecorder.prepareToRecord()) {
                throw IllegalStateException("AVAudioRecorder.prepareToRecord() failed")
            }

            avRecorder.record()
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
