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

        memScoped {
            // Configure audio session for recording
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

            // Standard AAC settings
            val settings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to 44100.0,
                AVNumberOfChannelsKey to 2,
                AVEncoderAudioQualityKey to AVAudioQualityHigh
            )

            val recorderErrorVar = alloc<ObjCObjectVar<NSError?>>()
            val avRecorder = AVAudioRecorder(uRL = url, settings = settings, error = recorderErrorVar.ptr)
            val recorderError = recorderErrorVar.value
            if (recorderError != null) {
                throw IllegalStateException(
                    "Failed to create AVAudioRecorder: ${recorderError.localizedDescription}"
                )
            }

            // Set delegate to null and try to prepare
            avRecorder.setDelegate(null)

            if (!avRecorder.prepareToRecord()) {
                throw IllegalStateException(
                    "AVAudioRecorder.prepareToRecord() failed for path: ${url.path}"
                )
            }

            avRecorder.record()
            recorder = avRecorder
            recording = true
        }
    }

    actual fun stop() {
        runCatching { recorder?.stop() }
        recorder = null
        recording = false

        // Deactivate audio session to allow playback to take over
        memScoped {
            val session = AVAudioSession.sharedInstance()
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            session.setActive(false, error = errorVar.ptr)
            // Ignore errors - session might not be active
        }
    }
}
