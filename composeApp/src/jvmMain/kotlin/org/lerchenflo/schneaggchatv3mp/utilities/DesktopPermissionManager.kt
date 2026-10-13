package org.lerchenflo.schneaggchatv3mp.utilities

class DesktopPermissionManager : PermissionManager {

    //todo real implementation for Desktop permissions
    override suspend fun checkMicrophonePermission(): PermissionState {
        return PermissionState.NOT_DETERMINED
    }

    override suspend fun requestMicrophonePermission(): PermissionState {
        return PermissionState.NOT_DETERMINED
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