package org.lerchenflo.schneaggchatv3mp.utilities

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

object ActivityHolder {

    private var currentActivity: ComponentActivity? = null
    private var micPermissionLauncher: ActivityResultLauncher<String>? = null
    private var permissionDeferred: CompletableDeferred<PermissionState>? = null

    fun set(activity: ComponentActivity) {
        currentActivity = activity

        micPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                permissionDeferred?.complete(
                    if (granted) PermissionState.GRANTED else PermissionState.DENIED
                )
            }
    }

    fun getActivity(): ComponentActivity? = currentActivity

    fun requestMicPermission(): CompletableDeferred<PermissionState> {
        permissionDeferred = CompletableDeferred()
        micPermissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
        return permissionDeferred!!
    }

    fun clear() {
        currentActivity = null
        micPermissionLauncher = null
        permissionDeferred = null
    }
}
