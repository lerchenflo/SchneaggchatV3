package org.lerchenflo.schneaggchatv3mp.utilities

import android.Manifest
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

    fun clear() {
        currentActivity = null
        micPermissionLauncher = null
        micPermissionDeferred = null
        locationPermissionLauncher = null
        locationPermissionDeferred = null
    }
}

