package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import platform.Foundation.NSUserDefaults

private const val APP_GROUP = "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtention"
private const val KEY_ENCRYPTION_KEY = "noti_encryption_key"

actual fun saveEncryptionKeyForExtension(key: String) {
    NSUserDefaults(suiteName = APP_GROUP)?.setObject(key, forKey = KEY_ENCRYPTION_KEY)
}

actual fun clearEncryptionKeyForExtension() {
    NSUserDefaults(suiteName = APP_GROUP)?.removeObjectForKey(KEY_ENCRYPTION_KEY)
}
