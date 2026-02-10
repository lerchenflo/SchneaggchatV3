package org.lerchenflo.schneaggchatv3mp.utilities.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.graphics.vector.ImageVector
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.dark_theme
import schneaggchatv3mp.composeapp.generated.resources.light_theme
import schneaggchatv3mp.composeapp.generated.resources.system_theme

enum class ThemeSetting {
    SYSTEM,     // Follow system setting
    LIGHT,      // Always light
    DARK;       // Always dark
    //PHTHEME;    // Anderes Theme ganz sicher ned klaut
    fun toUiText(): UiText = when (this) {
        SYSTEM -> UiText.StringResourceText(Res.string.system_theme)
        LIGHT -> UiText.StringResourceText(Res.string.light_theme)
        DARK   -> UiText.StringResourceText(Res.string.dark_theme)
        //PHTHEME -> UiText.StringResourceText(Res.string.ph_theme)
    }
    fun getIcon(): ImageVector = when (this) {
        SYSTEM -> Icons.Default.Contrast
        LIGHT -> Icons.Default.LightMode
        DARK   -> Icons.Default.DarkMode
        //PHTHEME -> Icons.Default.Male
    }
}