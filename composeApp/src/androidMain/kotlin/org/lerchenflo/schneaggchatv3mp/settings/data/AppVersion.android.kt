package org.lerchenflo.schneaggchatv3mp.settings.data

import android.content.Context
import android.os.Build
import android.provider.Settings

actual class AppVersion(
    private val context: Context
) {
    actual fun getVersionName(): String {
        return context.packageManager
            .getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }

    actual fun getVersionCode(): String {
        return context.packageManager
            .getPackageInfo(context.packageName, 0).longVersionCode.toString()
    }

    actual fun isMobile(): Boolean {
        return true
    }

    actual fun isDesktop(): Boolean {
        return false
    }

    actual fun isAndroid(): Boolean {
        return true
    }

    actual fun isIOS(): Boolean {
        return false
    }

    actual fun getDeviceType(): DEVICETYPE {
        return DEVICETYPE.ANDROID
    }

    actual fun getDeviceName(): String {
        val bluetoothName = Settings.Secure.getString(context.contentResolver, "bluetooth_name")
        val name = if (!bluetoothName.isNullOrBlank()) {
            bluetoothName
        } else {
            val model = Build.MODEL
            val manufacturer = Build.MANUFACTURER
            if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        }

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        return "$name - androidId: $androidId"
    }
}