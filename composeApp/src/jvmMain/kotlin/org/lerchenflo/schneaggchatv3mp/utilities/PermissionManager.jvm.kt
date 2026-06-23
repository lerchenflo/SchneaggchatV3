package org.lerchenflo.schneaggchatv3mp.utilities

actual class PermissionManager {

    //todo real implementation for Desktop permissions
    actual suspend fun checkMicrophonePermission(): PermissionState {
        return PermissionState.NOT_DETERMINED
    }

    actual suspend fun requestMicrophonePermission(): PermissionState {
        return PermissionState.NOT_DETERMINED
    }

    // No GPS on Desktop — always report as not available
    actual suspend fun checkLocationPermission(): PermissionState {
        return PermissionState.NOT_DETERMINED
    }

    actual suspend fun requestLocationPermission(): PermissionState {
        return PermissionState.NOT_DETERMINED
    }
}