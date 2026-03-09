package org.lerchenflo.schneaggchatv3mp.utilities

import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting

expect class LanguageManager {
    suspend fun applyLanguage(language: LanguageSetting)

    fun getSystemLanguage(): String

}