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
}

// commonMain
enum class PermissionState {
    GRANTED,
    DENIED,
    NOT_DETERMINED
}