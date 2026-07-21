// androidMain
package org.lerchenflo.schneaggchatv3mp.utilities

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
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

    actual suspend fun checkLocationPermission(): PermissionState {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            PermissionState.GRANTED
        } else {
            PermissionState.NOT_DETERMINED
        }
    }

    actual suspend fun requestLocationPermission(): PermissionState {
        if (checkLocationPermission() == PermissionState.GRANTED)
            return PermissionState.GRANTED

        val deferred = ActivityHolder.requestLocationPermission()
        return deferred.await()
    }

    actual suspend fun checkNotificationPermission(): PermissionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return PermissionState.GRANTED
        }
        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        return if (status == PackageManager.PERMISSION_GRANTED) {
            PermissionState.GRANTED
        } else {
            PermissionState.NOT_DETERMINED
        }
    }

    actual suspend fun requestNotificationPermission(): PermissionState {
        if (checkNotificationPermission() == PermissionState.GRANTED)
            return PermissionState.GRANTED

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            return PermissionState.GRANTED

        val deferred = ActivityHolder.requestNotificationPermission()
        return deferred.await()
    }

    actual suspend fun checkFullScreenIntentPermission(): PermissionState {
        //Unrestricted before API 34 - any app could use full screen intents.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            return PermissionState.GRANTED

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        return if (notificationManager?.canUseFullScreenIntent() == true) {
            PermissionState.GRANTED
        } else {
            PermissionState.NOT_DETERMINED
        }
    }

    actual suspend fun requestFullScreenIntentPermission(): PermissionState {
        val current = checkFullScreenIntentPermission()
        if (current == PermissionState.GRANTED) return current

        println("Opening fullscreen settings")
        //There is no ActivityResultContract for this one - it can only be granted in Settings,
        //so all we can do is take the user there. The caller must re-check afterwards.
        runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        return current
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