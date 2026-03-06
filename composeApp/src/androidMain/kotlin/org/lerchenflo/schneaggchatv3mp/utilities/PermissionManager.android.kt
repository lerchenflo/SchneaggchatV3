// androidMain
package org.lerchenflo.schneaggchatv3mp.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

actual class PermissionManager(private val context: Context) {

    // Bridge between callback and suspend
    private var permissionDeferred: CompletableDeferred<PermissionState>? = null

    // Launcher tied to a specific activity instance
    private var launcher: ActivityResultLauncher<String>? = null

    // Track which activity we registered on
    private var registeredActivityRef: WeakReference<ComponentActivity>? = null

    actual suspend fun checkMicrophonePermission(): PermissionState {
        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        return if (status == PackageManager.PERMISSION_GRANTED) {
            PermissionState.GRANTED
        } else {
            PermissionState.NOT_DETERMINED
        }
    }

    actual suspend fun requestMicrophonePermission(): PermissionState {

        if (checkMicrophonePermission() == PermissionState.GRANTED)
            return PermissionState.GRANTED

        val deferred = ActivityHolder.requestMicPermission()

        return deferred.await()
    }

    private fun Context.findActivity(): android.app.Activity? {
        var ctx: Context = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}