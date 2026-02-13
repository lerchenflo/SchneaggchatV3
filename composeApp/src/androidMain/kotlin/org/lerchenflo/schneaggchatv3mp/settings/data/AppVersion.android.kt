package org.lerchenflo.schneaggchatv3mp.settings.data

import android.content.Context

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
}