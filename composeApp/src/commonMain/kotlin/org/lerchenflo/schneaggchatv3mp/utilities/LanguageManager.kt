package org.lerchenflo.schneaggchatv3mp.utilities

expect class LanguageManager {
    suspend fun applyLanguage(language: LanguageSetting)

    fun getSystemLanguage(): String

}