package org.lerchenflo.schneaggchatv3mp.settings.data

import platform.Foundation.NSBundle

actual class AppVersion {
    actual fun getversionName(): String {
        return NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
    }

    actual fun getversionCode(): String {
        return NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "unknown"
    }
}