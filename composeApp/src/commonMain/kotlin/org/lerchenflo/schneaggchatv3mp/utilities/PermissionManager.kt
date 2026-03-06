package org.lerchenflo.schneaggchatv3mp.utilities

// commonMain
expect class PermissionManager {
    suspend fun checkMicrophonePermission(): PermissionState
    suspend fun requestMicrophonePermission(): PermissionState
}

// commonMain
enum class PermissionState {
    GRANTED,
    DENIED,
    NOT_DETERMINED
}