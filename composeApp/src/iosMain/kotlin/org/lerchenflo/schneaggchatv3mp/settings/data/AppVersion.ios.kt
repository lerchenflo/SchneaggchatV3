package org.lerchenflo.schneaggchatv3mp.settings.data

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

actual class AppVersion {
    actual fun getVersionName(): String {
        return NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
    }

    actual fun getVersionCode(): String {
        return NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "unknown"
    }

    actual fun isMobile(): Boolean {
        return true
    }

    actual fun isDesktop(): Boolean {
        return false
    }

    actual fun isAndroid(): Boolean {
        return false
    }

    actual fun isIOS(): Boolean {
        return true
    }

    actual fun getDeviceType(): DEVICETYPE {
        return DEVICETYPE.IOS
    }

    actual fun getDeviceName(): String {
        return UIDevice.currentDevice.name
    }
}