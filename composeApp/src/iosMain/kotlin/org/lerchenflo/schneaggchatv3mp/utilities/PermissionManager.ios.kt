package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
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

    actual suspend fun checkLocationPermission(): PermissionState =
        clStatusToState(CLLocationManager.authorizationStatus())

    actual suspend fun requestLocationPermission(): PermissionState {
        val current = checkLocationPermission()
        if (current == PermissionState.GRANTED) return current

        return suspendCancellableCoroutine { continuation ->
            val manager = CLLocationManager()

            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                    val state = clStatusToState(manager.authorizationStatus())
                    if (state != PermissionState.NOT_DETERMINED) {
                        continuation.resume(state)
                    }
                }
            }

            manager.delegate = delegate
            manager.requestWhenInUseAuthorization()
        }
    }

    actual suspend fun checkNotificationPermission(): PermissionState =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    val state = when (settings?.authorizationStatus) {
                        UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional -> PermissionState.GRANTED
                        UNAuthorizationStatusDenied -> PermissionState.DENIED
                        else -> PermissionState.NOT_DETERMINED
                    }
                    continuation.resume(state)
                }
        }

    actual suspend fun requestNotificationPermission(): PermissionState {
        val current = checkNotificationPermission()
        if (current == PermissionState.GRANTED) return current

        return suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
            ) { granted, _ ->
                continuation.resume(if (granted) PermissionState.GRANTED else PermissionState.DENIED)
            }
        }
    }

    private fun clStatusToState(status: CLAuthorizationStatus): PermissionState = when (status) {
        kCLAuthorizationStatusAuthorizedWhenInUse,
        kCLAuthorizationStatusAuthorizedAlways -> PermissionState.GRANTED
        kCLAuthorizationStatusDenied,
        kCLAuthorizationStatusRestricted -> PermissionState.DENIED
        kCLAuthorizationStatusNotDetermined -> PermissionState.NOT_DETERMINED
        else -> PermissionState.NOT_DETERMINED
    }
}