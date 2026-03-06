package org.lerchenflo.schneaggchatv3mp.utilities

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// todo flo bitte testa
actual class PermissionManager {

    actual suspend fun checkMicrophonePermission(): PermissionState {
        val status = AVAudioSession.sharedInstance().recordPermission()
        return when (status) {
            AVAudioSessionRecordPermissionGranted -> PermissionState.GRANTED
            AVAudioSessionRecordPermissionDenied -> PermissionState.DENIED
            else -> PermissionState.NOT_DETERMINED
        }
    }

    actual suspend fun requestMicrophonePermission(): PermissionState =
        suspendCancellableCoroutine { continuation ->
            AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                if (granted) {
                    continuation.resume(PermissionState.GRANTED)
                } else {
                    continuation.resume(PermissionState.DENIED)
                }
            }
        }
}