package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * NSUserDefaults backed by the shared App Group container.
 * Used by the main app to publish credentials/preferences that the
 * Notification Service Extension needs to localize and decrypt incoming pushes.
 *
 * The same App Group identifier is already used by the Share Extension; the
 * notification extension's entitlements must list it too.
 */
object SharedNotificationDefaults {

    const val APP_GROUP =
        "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtension"

    const val KEY_LANGUAGE_ISO = "shared_language_iso"
    const val KEY_ENCRYPTION_KEY = "shared_encryption_key"

    // Written by the Swift Notification Service Extension (a separate process that never
    // touches Kotlin/Room) so a background push can still queue a message for the main app to
    // upsert once it next activates. Must match `pendingPushMessagesKey` in NotificationService.swift.
    const val KEY_PENDING_PUSH_MESSAGES = "pending_push_messages"

    private val defaults: NSUserDefaults?
        get() = NSUserDefaults(suiteName = APP_GROUP)

    fun setLanguageIso(iso: String) {
        defaults?.let {
            it.setObject(iso, KEY_LANGUAGE_ISO)
            it.synchronize()
        }
    }

    fun setEncryptionKey(key: String?) {
        defaults?.let {
            if (key.isNullOrEmpty()) {
                it.removeObjectForKey(KEY_ENCRYPTION_KEY)
            } else {
                it.setObject(key, KEY_ENCRYPTION_KEY)
            }
            it.synchronize()
        }
    }

    fun getLanguageIso(): String? = defaults?.stringForKey(KEY_LANGUAGE_ISO)

    fun getEncryptionKey(): String? = defaults?.stringForKey(KEY_ENCRYPTION_KEY)

    /** Reads and clears the queue the NSE has written, so each queued push is applied exactly once. */
    fun takePendingPushMessages(): List<Map<String, String>> {
        val prefs = defaults ?: return emptyList()
        val raw = prefs.stringForKey(KEY_PENDING_PUSH_MESSAGES) ?: return emptyList()
        prefs.removeObjectForKey(KEY_PENDING_PUSH_MESSAGES)
        prefs.synchronize()
        return runCatching { Json.decodeFromString<List<Map<String, String>>>(raw) }.getOrDefault(emptyList())
    }
}
