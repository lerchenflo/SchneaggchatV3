package org.lerchenflo.schneaggchatv3mp.utilities

import org.lerchenflo.schneaggchatv3mp.utilities.preferences.LanguageSetting

expect class LanguageManager {
    suspend fun applyLanguage(language: LanguageSetting)

    fun getSystemLanguage(): String

}