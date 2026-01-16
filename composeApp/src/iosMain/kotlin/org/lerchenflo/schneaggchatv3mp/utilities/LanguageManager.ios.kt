package org.lerchenflo.schneaggchatv3mp.utilities

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSLocale
import platform.Foundation.arrayOfObjects
import platform.Foundation.setObject
import platform.Foundation.synchronize
import platform.Foundation.removeObjectForKey

actual class LanguageManager(
    private val preferenceManager: Preferencemanager
) {
    companion object {
        // Capture original system locale at class initialization
        private val ORIGINAL_SYSTEM_LOCALE: String = NSLocale.preferredLanguages.firstOrNull() ?: "en"
    }
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    actual suspend fun applyLanguage(language: LanguageSetting) {
        if (language == LanguageSetting.SYSTEM) {
            // Restore to original system locale captured at startup
            userDefaults.setObject(arrayOfObjects(ORIGINAL_SYSTEM_LOCALE), "AppleLanguages")
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
            
            userDefaults.setObject(arrayOfObjects(languageCode), "AppleLanguages")
            userDefaults.synchronize()
            println("Applying language: $language, Code: $languageCode")
        }
        
        // Save to DataStore
        preferenceManager.saveLanguageSetting(language)
    }
}
