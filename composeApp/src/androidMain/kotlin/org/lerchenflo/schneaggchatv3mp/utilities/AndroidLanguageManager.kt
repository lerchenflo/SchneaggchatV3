package org.lerchenflo.schneaggchatv3mp.utilities

import android.content.Context
import android.os.LocaleList
import org.lerchenflo.schneaggchatv3mp.settings.domain.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import java.util.Locale

class AndroidLanguageManager(
    private val context: Context,
    private val preferenceManager: Preferencemanager
) : LanguageManager {
    companion object {
        // Capture original system locale at class initialization
        private val ORIGINAL_SYSTEM_LOCALES: LocaleList = LocaleList.getAdjustedDefault()
    }
    override suspend fun applyLanguage(language: LanguageSetting) {
        // Save to DataStore first
        preferenceManager.saveLanguageSetting(language)
        
        if (language == LanguageSetting.SYSTEM) {
            // Restore to original system locale captured at startup
            Locale.setDefault(ORIGINAL_SYSTEM_LOCALES[0])
            
            val config = context.resources.configuration
            config.setLocales(ORIGINAL_SYSTEM_LOCALES)
            //context.createConfigurationContext(config)
            context.resources.configuration.setLocales(ORIGINAL_SYSTEM_LOCALES)
            //context.resources.updateConfiguration(config, context.resources.displayMetrics)
            
            //println("Reset to original system locale: ${ORIGINAL_SYSTEM_LOCALES[0].language}-${ORIGINAL_SYSTEM_LOCALES[0].country}")
        } else {
            // Apply specific locale
            val isoCode = language.getIsoCode()
            val locale = Locale.forLanguageTag(isoCode.replace("_", "-"))
            
            // Debug logging
            //println("Applying language: $language, ISO: ${language.getIsoCode()}, Locale: ${locale.language}-${locale.country}")
            
            Locale.setDefault(locale)
            //val config = context.resources.configuration
            //config.setLocales(LocaleList(locale))
            //context.createConfigurationContext(config)
            context.resources.configuration.setLocale(locale)

            // Update context to apply changes immediately
            //context.resources.updateConfiguration(config, context.resources.displayMetrics)
            
            // Verify the change
            val newLocale = context.resources.configuration.locales[0]
            //println("New system locale: ${newLocale.language}-${newLocale.country}")
        }
    }

    override fun getSystemLanguage(): String {
        val locale = context.resources.configuration.locales[0]
        return "${locale.language}-${locale.country}"
    }
}
