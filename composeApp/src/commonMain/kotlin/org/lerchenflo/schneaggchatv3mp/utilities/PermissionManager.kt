package org.lerchenflo.schneaggchatv3mp.utilities

// commonMain
/**
 * for checking and Requesting Permissions (todo JVM)
 */
expect class PermissionManager {
    /**
     * returns the permissions state of the microphone permission
     */
    suspend fun checkMicrophonePermission(): PermissionState
    /**
     * requests the microphone permission
     */
    suspend fun requestMicrophonePermission(): PermissionState

    /**
     * Returns the current location permission state.
     */
    suspend fun checkLocationPermission(): PermissionState

    /**
     * Requests location permission if not yet granted.
     * Returns the resulting [PermissionState].
     */
    suspend fun requestLocationPermission(): PermissionState

    /**
     * Returns the current notification permission state.
     */
    suspend fun checkNotificationPermission(): PermissionState

    /**
     * Requests notification permission if not yet granted.
     * Returns the resulting [PermissionState].
     */
    suspend fun requestNotificationPermission(): PermissionState
}

// commonMain
enum class PermissionState {
    GRANTED,
    DENIED,
    NOT_DETERMINED
}