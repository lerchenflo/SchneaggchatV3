package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

actual class LanguageManager(
    private val context: Context,
    private val preferenceManager: Preferencemanager
) {
    companion object {
        // Capture original system locale at class initialization
        private val ORIGINAL_SYSTEM_LOCALES: LocaleList = LocaleList.getAdjustedDefault()
    }
    actual suspend fun applyLanguage(language: LanguageSetting) {
        // Save to DataStore first
        preferenceManager.saveLanguageSetting(language)
        
        if (language == LanguageSetting.SYSTEM) {
            // Restore to original system locale captured at startup
            Locale.setDefault(ORIGINAL_SYSTEM_LOCALES[0])
            
            val config = context.resources.configuration
            config.setLocales(ORIGINAL_SYSTEM_LOCALES)
            context.createConfigurationContext(config)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            
            println("Reset to original system locale: ${ORIGINAL_SYSTEM_LOCALES[0].language}-${ORIGINAL_SYSTEM_LOCALES[0].country}")
        } else {
            // Apply specific locale
            val isoCode = language.getIsoCode()
            val locale = if (isoCode.contains("-")) {
                val parts = isoCode.split("-")
                Locale(parts[0], parts[1]) // language, country
            } else {
                Locale(isoCode) // language only
            }
            
            // Debug logging
            println("Applying language: $language, ISO: ${language.getIsoCode()}, Locale: ${locale.language}-${locale.country}")
            
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocales(LocaleList(locale))
            context.createConfigurationContext(config)
            
            // Update context to apply changes immediately
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            
            // Verify the change
            val newLocale = context.resources.configuration.locales[0]
            println("New system locale: ${newLocale.language}-${newLocale.country}")
        }
    }

    actual fun getSystemLanguage(): String {
        val locale = context.resources.configuration.locales[0]
        return "${locale.language}-${locale.country}"
    }
}
