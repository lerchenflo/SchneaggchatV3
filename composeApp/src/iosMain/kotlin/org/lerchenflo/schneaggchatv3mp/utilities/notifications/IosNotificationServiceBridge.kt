package org.lerchenflo.schneaggchatv3mp.utilities.notifications

import kotlinx.coroutines.runBlocking
import platform.Foundation.NSUserDefaults

/**
 * Entry point for the iOS Notification Service Extension.
 *
 * The extension is its own process with no Koin graph and no DataStore access,
 * so we read everything we need (language ISO, encryption key) from the App
 * Group `NSUserDefaults` populated by [SharedNotificationDefaults] in the main
 * app, then call into the shared resolver to produce localized title/body.
 */
class IosNotificationServiceBridge {

    data class Result(val title: String?, val body: String?)

    fun resolveLocalized(
        payload: Map<String, String>,
        completion: (Result) -> Unit,
    ) {
        // Apply the user's chosen language for this extension process so
        // Compose Resources `getString` resolves into the right locale.
        SharedNotificationDefaults.getLanguageIso()?.let { iso ->
            NSUserDefaults.standardUserDefaults.setObject(
                listOf(iso),
                forKey = "AppleLanguages",
            )
            NSUserDefaults.standardUserDefaults.synchronize()
        }

        val decoded = PayloadDecoder.decode(payload)
        if (decoded == null) {
            completion(Result(null, null))
            return
        }

        val key = SharedNotificationDefaults.getEncryptionKey()
        val content = runBlocking { resolveLocalizedContent(decoded, key) }
        completion(Result(content?.title, content?.body))
    }
}
