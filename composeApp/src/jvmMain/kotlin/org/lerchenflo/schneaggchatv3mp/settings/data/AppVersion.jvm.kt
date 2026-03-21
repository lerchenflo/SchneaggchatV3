package org.lerchenflo.schneaggchatv3mp.settings.data

actual class AppVersion {
    actual fun getVersionName(): String {
        return this::class.java.`package`.implementationVersion ?: "Desktop"
    }

    actual fun getVersionCode(): String {
        return "1"
    }

    actual fun isMobile(): Boolean {
        return false
    }

    actual fun isDesktop(): Boolean {
        return true
    }

    actual fun isAndroid(): Boolean {
        return false
    }

    actual fun isIOS(): Boolean {
        return false
    }


}