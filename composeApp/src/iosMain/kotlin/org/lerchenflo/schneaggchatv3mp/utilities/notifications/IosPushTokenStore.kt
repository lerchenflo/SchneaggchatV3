package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import platform.Foundation.NSUserDefaults

private const val KEY_APNS_TOKEN = "apns_device_token"

object IosPushTokenStore {
    fun saveToken(hexToken: String) {
        NSUserDefaults.standardUserDefaults.setObject(hexToken, KEY_APNS_TOKEN)
    }

    fun getToken(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_APNS_TOKEN)
    }

    fun clearToken() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_APNS_TOKEN)
    }
}
