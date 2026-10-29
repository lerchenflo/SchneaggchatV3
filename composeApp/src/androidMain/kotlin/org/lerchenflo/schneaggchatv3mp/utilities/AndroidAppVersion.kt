package org.lerchenflo.schneaggchatv3mp.utilities

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

class AndroidAppVersion(
    private val context: Context
) : AppVersion {
    override fun getVersionName(): String {
        return context.packageManager
            .getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }

    override fun getVersionCode(): String {
        return context.packageManager
            .getPackageInfo(context.packageName, 0).longVersionCode.toString()
    }

    override fun isMobile(): Boolean {
        return true
    }

    override fun isDesktop(): Boolean {
        return false
    }

    override fun isAndroid(): Boolean {
        return true
    }

    override fun isIOS(): Boolean {
        return false
    }

    override fun getDeviceType(): DEVICETYPE {
        return DEVICETYPE.ANDROID
    }

    // ANDROID_ID is scoped to the app signing key and user since API 26, the Android analogue of
    // iOS identifierForVendor, and only labels the session in the device list
    @SuppressLint("HardwareIds")
    override fun getDeviceName(): String {
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