package org.lerchenflo.schneaggchatv3mp.utilities.notifications

actual object NotificationCredentialsMirror {
    actual fun setEncryptionKey(key: String?) = SharedNotificationDefaults.setEncryptionKey(key)
    actual fun setLanguageIso(iso: String) = SharedNotificationDefaults.setLanguageIso(iso)
}
