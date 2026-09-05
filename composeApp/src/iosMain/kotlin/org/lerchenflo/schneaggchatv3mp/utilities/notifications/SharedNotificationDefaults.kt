package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * NSUserDefaults backed by the shared App Group container.
 * Used by the main app to publish credentials/preferences that the
 * Notification Service Extension needs to localize incoming pushes.
 *
 * The same App Group identifier is already used by the Share Extension; the
 * notification extension's entitlements must list it too.
 */
object SharedNotificationDefaults {

    const val APP_GROUP =
        "group.org.lerchenflo.schneaggchatv3mp.SchneaggchatV3mp.SchneaggchatShareExtension"

    const val KEY_LANGUAGE_ISO = "shared_language_iso"
    // Pre-E2E push decryption key. No longer written; kept only so old installs can purge it.
    private const val LEGACY_KEY_ENCRYPTION_KEY = "shared_encryption_key"

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

    /** Removes the shared push key that versions before the E2E switch published for the extension. */
    fun purgeLegacyEncryptionKey() {
        defaults?.let {
            it.removeObjectForKey(LEGACY_KEY_ENCRYPTION_KEY)
            it.synchronize()
        }
    }

    fun getLanguageIso(): String? = defaults?.stringForKey(KEY_LANGUAGE_ISO)

    /** Reads and clears the queue the NSE has written, so each queued push is applied exactly once. */
    fun takePendingPushMessages(): List<Map<String, String>> {
        val prefs = defaults ?: return emptyList()
        val raw = prefs.stringForKey(KEY_PENDING_PUSH_MESSAGES) ?: return emptyList()
        prefs.removeObjectForKey(KEY_PENDING_PUSH_MESSAGES)
        prefs.synchronize()
        return runCatching { Json.decodeFromString<List<Map<String, String>>>(raw) }.getOrDefault(emptyList())
    }
}
