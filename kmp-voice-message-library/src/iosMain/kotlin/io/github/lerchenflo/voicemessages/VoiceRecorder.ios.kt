package io.github.lerchenflo.voicemessages

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFAudio.AVAudioQualityHigh
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVEncoderAudioQualityKey
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
        val settings: Map<Any?, *> = mapOf(
            AVFormatIDKey to kAudioFormatMPEG4AAC,
            AVSampleRateKey to 44_100.0,
            AVNumberOfChannelsKey to 1,
            AVEncoderAudioQualityKey to AVAudioQualityHigh,
        )

        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            val avRecorder = AVAudioRecorder(uRL = url, settings = settings, error = errorVar.ptr)
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
