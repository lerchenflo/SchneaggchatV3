package org.lerchenflo.schneaggchatv3mp.utilities

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

object ActivityHolder {

    private var currentActivity: ComponentActivity? = null

    // Microphone
    private var micPermissionLauncher: ActivityResultLauncher<String>? = null
    private var micPermissionDeferred: CompletableDeferred<PermissionState>? = null

    // Location
    private var locationPermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var locationPermissionDeferred: CompletableDeferred<PermissionState>? = null

    // Notification
    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null
    private var notificationPermissionDeferred: CompletableDeferred<PermissionState>? = null
    private var notificationRationaleBeforeRequest = false

    @SuppressLint("InlinedApi")
    fun set(activity: ComponentActivity) {
        currentActivity = activity

        micPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                micPermissionDeferred?.complete(
                    if (granted) PermissionState.GRANTED else PermissionState.DENIED
                )
            }

        locationPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
                        || results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                locationPermissionDeferred?.complete(
                    if (granted) PermissionState.GRANTED else PermissionState.DENIED
                )
            }

        notificationPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                val rationaleAfterRequest = activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
                )

                val result = when {
                    granted -> PermissionState.GRANTED
                    rationaleAfterRequest -> PermissionState.DENIED
                    notificationRationaleBeforeRequest -> PermissionState.PERMANENTLY_DENIED
                    else -> PermissionState.DENIED // very first-ever ask
                }

                notificationPermissionDeferred?.complete(result)
            }
    }

    fun getActivity(): ComponentActivity? = currentActivity

    fun requestMicPermission(): CompletableDeferred<PermissionState> {
        micPermissionDeferred = CompletableDeferred()
        micPermissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
        return micPermissionDeferred!!
    }

    fun requestLocationPermission(): CompletableDeferred<PermissionState> {
        locationPermissionDeferred = CompletableDeferred()
        locationPermissionLauncher?.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
        return locationPermissionDeferred!!
    }

    fun requestNotificationPermission(): CompletableDeferred<PermissionState> {
        val deferred = CompletableDeferred<PermissionState>()
        notificationPermissionDeferred = deferred

        val launcher = notificationPermissionLauncher
        if (launcher == null) {
            deferred.completeExceptionally(
                IllegalStateException("Notification permission launcher not registered")
            )
            return deferred
        }

        notificationRationaleBeforeRequest = currentActivity?.shouldShowRequestPermissionRationale(
            Manifest.permission.POST_NOTIFICATIONS
        ) ?: false

        try {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
        }

        return deferred
    }

    fun clear() {
        currentActivity = null
        micPermissionLauncher = null
        micPermissionDeferred = null
        locationPermissionLauncher = null
        locationPermissionDeferred = null
        notificationPermissionLauncher = null
        notificationPermissionDeferred = null
        notificationRationaleBeforeRequest = false
    }
}