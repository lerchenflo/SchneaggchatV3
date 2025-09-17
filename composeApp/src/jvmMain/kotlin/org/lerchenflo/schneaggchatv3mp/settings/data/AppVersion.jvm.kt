package org.lerchenflo.schneaggchatv3mp.settings.data

actual class AppVersion {
    actual fun getVersionName(): String {
        return "Desktop"
    }

    actual fun getVersionCode(): String {
        return "???"
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