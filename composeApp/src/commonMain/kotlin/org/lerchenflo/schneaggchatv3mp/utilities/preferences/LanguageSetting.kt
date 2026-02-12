package org.lerchenflo.schneaggchatv3mp.utilities.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.graphics.vector.ImageVector
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.english_language
import schneaggchatv3mp.composeapp.generated.resources.german_language
import schneaggchatv3mp.composeapp.generated.resources.system_language
import schneaggchatv3mp.composeapp.generated.resources.vori_language

enum class LanguageSetting {
    SYSTEM,     // Follow system setting
    GERMAN,     // German language
    ENGLISH,    // English language
    VORI;       // Vori language

    fun toUiText(): UiText = when (this) {
        SYSTEM -> UiText.StringResourceText(Res.string.system_language)
        GERMAN -> UiText.StringResourceText(Res.string.german_language)
        ENGLISH -> UiText.StringResourceText(Res.string.english_language)
        VORI -> UiText.StringResourceText(Res.string.vori_language)
    }

    fun getIcon(): ImageVector = when (this) {
        SYSTEM -> Icons.Default.Contrast
        GERMAN -> Icons.Default.Language
        ENGLISH -> Icons.Default.Language
        VORI -> Icons.Default.Language
    }

    fun getIsoCode(): String = when (this) {
        SYSTEM -> "" // Use system default
        GERMAN -> "de"
        ENGLISH -> "en"
        VORI -> "de-at"
    }
}