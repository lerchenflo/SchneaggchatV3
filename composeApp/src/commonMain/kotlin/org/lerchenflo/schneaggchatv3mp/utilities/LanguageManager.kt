package org.lerchenflo.schneaggchatv3mp.utilities

import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting

interface LanguageManager {
    suspend fun applyLanguage(language: LanguageSetting)

    fun getSystemLanguage(): String

}