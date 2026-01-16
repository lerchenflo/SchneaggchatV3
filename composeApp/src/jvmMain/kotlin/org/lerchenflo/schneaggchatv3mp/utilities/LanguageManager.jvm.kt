package org.lerchenflo.schneaggchatv3mp.utilities

import java.util.Locale

actual class LanguageManager(
    private val preferenceManager: Preferencemanager
) {
    companion object {
        // Capture original system locale at class initialization
        private val ORIGINAL_SYSTEM_LOCALE: Locale = Locale.getDefault()
    }
    actual suspend fun applyLanguage(language: LanguageSetting) {
        // For Desktop/JVM, we set the default locale for the JVM
        // and save to DataStore for consistency
        preferenceManager.saveLanguageSetting(language)
        
        if (language == LanguageSetting.SYSTEM) {
            // Restore to original system locale captured at startup
            Locale.setDefault(ORIGINAL_SYSTEM_LOCALE)
            
            // Also restore system properties if needed
            System.setProperty("user.language", ORIGINAL_SYSTEM_LOCALE.language)
            if (ORIGINAL_SYSTEM_LOCALE.country.isNotEmpty()) {
                System.setProperty("user.country", ORIGINAL_SYSTEM_LOCALE.country)
            }
            
            println("Reset to original system locale: ${ORIGINAL_SYSTEM_LOCALE.language}-${ORIGINAL_SYSTEM_LOCALE.country}")
        } else {
            // Set specific locale
            val isoCode = language.getIsoCode()
            val locale = if (isoCode.contains("-")) {
                val parts = isoCode.split("-")
                Locale(parts[0], parts[1]) // language, country
            } else {
                Locale(isoCode) // language only
            }
            
            Locale.setDefault(locale)
            
            // Also set system properties for some Java libraries
            val parts = isoCode.split("-")
            System.setProperty("user.language", parts[0])
            if (parts.size > 1) {
                System.setProperty("user.country", parts[1])
            }
            
            println("Applying language: $language, Locale: ${locale.language}-${locale.country}")
        }
    }
}
