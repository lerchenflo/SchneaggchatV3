package org.lerchenflo.schneaggchatv3mp.utilities

import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.utilities.notifications.NotificationCredentialsMirror
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

actual class LanguageManager(
    private val preferenceManager: Preferencemanager
) {
    companion object {
        // Capture original system locale at class initialization
        private val ORIGINAL_SYSTEM_LOCALE: String =
            (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"
    }

    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual suspend fun applyLanguage(language: LanguageSetting) {
        val effectiveIso: String
        if (language == LanguageSetting.SYSTEM) {
            // Restore to original system locale captured at startup
            effectiveIso = ORIGINAL_SYSTEM_LOCALE
            userDefaults.setObject(
                listOf(ORIGINAL_SYSTEM_LOCALE),
                forKey = "AppleLanguages"
            )
            userDefaults.synchronize()
            println("Reset to original system locale: $ORIGINAL_SYSTEM_LOCALE")
        } else {
            // iOS-specific: Set AppleLanguages preference
            val isoCode = language.getIsoCode()
            val languageCode = if (isoCode.contains("-")) {
                val parts = isoCode.split("-")
                "${parts[0]}-${parts[1].uppercase()}"
            } else {
                isoCode
            }

            effectiveIso = languageCode
            userDefaults.setObject(
                listOf(languageCode),
                forKey = "AppleLanguages"
            )
            userDefaults.synchronize()
            println("Applying language: $language, Code: $languageCode")
        }

        // Mirror to App Group so the Notification Service Extension can pick the
        // same locale when localizing pushes received while the app is closed.
        NotificationCredentialsMirror.setLanguageIso(effectiveIso)

        // Save to DataStore
        preferenceManager.saveLanguageSetting(language)
    }

    actual fun getSystemLanguage(): String {
        // Get the preferred languages from the system
        return (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"
    }
}