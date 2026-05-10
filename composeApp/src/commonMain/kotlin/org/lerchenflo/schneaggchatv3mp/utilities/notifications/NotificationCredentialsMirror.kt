package org.lerchenflo.schneaggchatv3mp.utilities.notifications

/**
 * Mirrors credentials needed by out-of-process notification handlers (e.g. the
 * iOS Notification Service Extension) into a process-shared location.
 * No-op on platforms that handle notifications inside the main process.
 */
expect object NotificationCredentialsMirror {
    fun setEncryptionKey(key: String?)
    fun setLanguageIso(iso: String)
}
