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

    /**
     * Whether this platform may show a full screen UI over the lock screen (used by the wake
     * alarm). Platforms without the concept report [PermissionState.GRANTED].
     */
    suspend fun checkFullScreenIntentPermission(): PermissionState

    /**
     * Sends the user to the OS screen where full screen intents can be granted. This is not a
     * runtime permission dialog, so the returned state is simply the state as it was before
     * leaving the app - re-check it once the user comes back.
     */
    suspend fun requestFullScreenIntentPermission(): PermissionState
}

// commonMain
enum class PermissionState {
    GRANTED,
    DENIED,
    NOT_DETERMINED
}