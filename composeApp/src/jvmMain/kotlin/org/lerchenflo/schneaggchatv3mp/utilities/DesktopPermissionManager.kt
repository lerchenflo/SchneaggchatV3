package org.lerchenflo.schneaggchatv3mp.utilities

import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Line
import javax.sound.sampled.TargetDataLine

class DesktopPermissionManager : PermissionManager {

    // Java has no microphone permission API. macOS asks the user itself on first use (it needs
    // NSMicrophoneUsageDescription in the app bundle, see composeApp/build.gradle.kts), so this
    // only checks that the computer has a microphone at all.
    override suspend fun checkMicrophonePermission(): PermissionState {
        return if (AudioSystem.isLineSupported(Line.Info(TargetDataLine::class.java))) {
            PermissionState.GRANTED
        } else {
            PermissionState.DENIED
        }
    }

    override suspend fun requestMicrophonePermission(): PermissionState {
        return checkMicrophonePermission()
    }

    // No GPS on Desktop — always report as not available
    override suspend fun checkLocationPermission(): PermissionState {
        return PermissionState.NOT_DETERMINED
    }

    override suspend fun requestLocationPermission(): PermissionState {
        return PermissionState.NOT_DETERMINED
    }

    override suspend fun checkNotificationPermission(): PermissionState {
        return PermissionState.GRANTED
    }

    override suspend fun requestNotificationPermission(openSettings: Boolean): PermissionState {
        if (openSettings) {
            openAppSettings()
            return checkNotificationPermission()
        }
        return PermissionState.GRANTED
    }

    // Desktop has no lock screen full screen intents - the wake feature is Android only anyway.
    override suspend fun checkFullScreenIntentPermission(): PermissionState {
        return PermissionState.GRANTED
    }

    override suspend fun requestFullScreenIntentPermission(): PermissionState {
        return PermissionState.GRANTED
    }

    override fun openAppSettings() {
        // Desktop has no standard app settings screen
    }
}