package org.lerchenflo.schneaggchatv3mp.settings.data

import android.annotation.SuppressLint
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

    // ANDROID_ID is scoped to the app signing key and user since API 26, the Android analogue of
    // iOS identifierForVendor, and only labels the session in the device list
    @SuppressLint("HardwareIds")
    actual fun getDeviceName(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        return "${userDeviceName() ?: buildDeviceName()} - androidId: $androidId"
    }

    // Settings.Secure "bluetooth_name" throws SecurityException for targetSdk > 31, "device_name" stays readable
    private fun userDeviceName(): String? =
        runCatching { Settings.Global.getString(context.contentResolver, "device_name") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    private fun buildDeviceName(): String {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }
}