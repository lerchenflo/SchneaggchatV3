package org.lerchenflo.schneaggchatv3mp.datasource.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.graphics.vector.ImageVector
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.english_language
import schneaggchatv3mp.composeapp.generated.resources.german_language
import schneaggchatv3mp.composeapp.generated.resources.italian_language
import schneaggchatv3mp.composeapp.generated.resources.system_language
import schneaggchatv3mp.composeapp.generated.resources.vori_language

enum class LanguageSetting {
    SYSTEM,     // Follow system setting
    GERMAN,     // German language
    ENGLISH,    // English language
    VORI,       // Vori language
    ITALIAN;    // Italian language

    fun toUiText(): UiText = when (this) {
        SYSTEM -> UiText.StringResourceText(Res.string.system_language)
        GERMAN -> UiText.StringResourceText(Res.string.german_language)
        ENGLISH -> UiText.StringResourceText(Res.string.english_language)
        VORI -> UiText.StringResourceText(Res.string.vori_language)
        ITALIAN -> UiText.StringResourceText(Res.string.italian_language)
    }

    fun getIcon(): ImageVector = when (this) {
        SYSTEM -> Icons.Default.Contrast
        GERMAN -> Icons.Default.Language
        ENGLISH -> Icons.Default.Language
        VORI -> Icons.Default.Language
        ITALIAN -> Icons.Default.Language
    }

    fun getIsoCode(): String = when (this) {
        SYSTEM -> "" // Use system default
        GERMAN -> "de"
        ENGLISH -> "en"
        VORI -> "de-at"
        ITALIAN -> "it"
    }

    companion object {
        /**
         * Converts a language code string (e.g. "DE", "en", "de-AT") into a LanguageSetting.
         * Falls back to SYSTEM if the code doesn't match any known language.
         *
         * Note: VORI's code ("de-at") is checked before plain "de" so that
         * region-specific German (Austria) resolves to VORI rather than GERMAN.
         */
        fun fromIsoCode(code: String): LanguageSetting {
            val normalized = code.trim().lowercase()

            return when {
                normalized.isEmpty() -> SYSTEM
                normalized == VORI.getIsoCode() || normalized.startsWith("de-at") -> VORI
                normalized == GERMAN.getIsoCode() || normalized.startsWith("de") -> GERMAN
                normalized == ENGLISH.getIsoCode() || normalized.startsWith("en") -> ENGLISH
                normalized == ITALIAN.getIsoCode() || normalized.startsWith("it") -> ITALIAN
                else -> SYSTEM
            }
        }
    }
}