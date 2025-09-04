package org.lerchenflo.schneaggchatv3mp.settings.data

import android.content.Context

actual class AppVersion(
    private val context: Context
) {
    actual fun getversionName(): String {
        return context.packageManager
            .getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }

    actual fun getversionCode(): String {
        return context.packageManager
            .getPackageInfo(context.packageName, 0).versionCode.toString()
    }
}