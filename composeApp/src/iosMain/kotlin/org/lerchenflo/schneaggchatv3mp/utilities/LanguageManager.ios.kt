package org.lerchenflo.schneaggchatv3mp.utilities

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSLocale
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
        if (language == LanguageSetting.SYSTEM) {
            // Restore to original system locale captured at startup
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

            userDefaults.setObject(
                listOf(languageCode),
                forKey = "AppleLanguages"
            )
            userDefaults.synchronize()
            println("Applying language: $language, Code: $languageCode")
        }

        // Save to DataStore
        preferenceManager.saveLanguageSetting(language)
    }

    actual fun getSystemLanguage(): String {
        // Get the preferred languages from the system
        val preferredLanguages = NSLocale.preferredLanguages
        val firstLanguage = preferredLanguages.firstOrNull() as? String
        
        return if (!firstLanguage.isNullOrEmpty()) {
            // Extract language code (before any dash or underscore)
            firstLanguage.split("-", "_").firstOrNull()?.takeIf { it.isNotEmpty() } ?: "en"
        } else {
            // Fallback to current locale or default to English
            NSLocale.currentLocale.languageCode?.takeIf { it.isNotEmpty() } ?: "en"
        }
    }
}