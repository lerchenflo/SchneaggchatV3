package org.lerchenflo.schneaggchatv3mp.utilities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

actual class PermissionManager(
    private val context: Context,
    ) {
    actual suspend fun checkMicrophonePermission(): PermissionState {
        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        return if (status == PackageManager.PERMISSION_GRANTED) {
            PermissionState.GRANTED
        } else {
            PermissionState.NOT_DETERMINED
        }
    }

    actual suspend fun requestMicrophonePermission(): PermissionState {
        if (checkMicrophonePermission() == PermissionState.GRANTED) return PermissionState.GRANTED
        val activity = context.findActivity()?: return PermissionState.NOT_DETERMINED // Or handle error
        // This is a simplified version. In a real app, you'd use ActivityResultLauncher
        // or a library like MOKO Permissions to handle the callback seamlessly.
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            101
        )

        // Note: To make this truly 'suspend' and return a value,
        // you would need to listen to the activity result.
        return checkMicrophonePermission()
    }

    fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
}