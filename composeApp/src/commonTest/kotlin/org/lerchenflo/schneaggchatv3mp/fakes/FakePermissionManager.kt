package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.utilities.PermissionManager
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState

/** [PermissionManager] whose checks and requests answer with the state set for each permission. */
class FakePermissionManager(
    var microphone: PermissionState = PermissionState.GRANTED,
    var location: PermissionState = PermissionState.GRANTED,
    var notification: PermissionState = PermissionState.GRANTED,
    var fullScreenIntent: PermissionState = PermissionState.GRANTED,
) : PermissionManager {

    /** `openSettings` of every [requestNotificationPermission] call. */
    val notificationRequests = CallLog<Boolean>()

    override suspend fun checkMicrophonePermission(): PermissionState = microphone
    override suspend fun requestMicrophonePermission(): PermissionState = microphone
    override suspend fun checkLocationPermission(): PermissionState = location
    override suspend fun requestLocationPermission(): PermissionState = location
    override suspend fun checkNotificationPermission(): PermissionState = notification

    override suspend fun requestNotificationPermission(openSettings: Boolean): PermissionState {
        notificationRequests.record(openSettings)
        return notification
    }

    override suspend fun checkFullScreenIntentPermission(): PermissionState = fullScreenIntent
    override suspend fun requestFullScreenIntentPermission(): PermissionState = fullScreenIntent
    override fun openAppSettings() = Unit
}
