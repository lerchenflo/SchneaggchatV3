package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.flow.Flow

class LanguageService(
    private val preferenceManager: Preferencemanager,
    private val languageManager: LanguageManager
) {
    
    /**
     * Get the current language setting as a Flow
     */
    fun getCurrentLanguageFlow(): Flow<LanguageSetting> {
        return preferenceManager.getLanguageFlow()
    }
    
    /**
     * Get the current language setting (one-time read)
     */
    suspend fun getCurrentLanguage(): LanguageSetting {
        return preferenceManager.getLanguageSetting()
    }

    fun getSystemLanguage(): String {
        return languageManager.getSystemLanguage()
    }
    
    /**
     * Apply a new language setting
     * This will save the preference and apply the platform-specific language changes
     */
    suspend fun applyLanguage(language: LanguageSetting) {
        languageManager.applyLanguage(language)
    }

}
