package org.lerchenflo.schneaggchatv3mp.datasource.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.ui.graphics.vector.ImageVector
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.dark_theme
import schneaggchatv3mp.composeapp.generated.resources.light_theme
import schneaggchatv3mp.composeapp.generated.resources.neon_pulse_theme
import schneaggchatv3mp.composeapp.generated.resources.system_theme
import schneaggchatv3mp.composeapp.generated.resources.variant1
import schneaggchatv3mp.composeapp.generated.resources.variant2

enum class ThemeSetting {
    SYSTEM,     // Follow system setting
    LIGHT,      // Always light
    DARK,       // Always dark
    VARIANT1,   // dark Variant 1
    VARIANT2,   // dark Variant 1
    NEONPULSE;  // dark Neon Colors
    //PHTHEME;    // Anderes Theme ganz sicher ned klaut
    fun toUiText(): UiText = when (this) {
        SYSTEM -> UiText.StringResourceText(Res.string.system_theme)
        LIGHT -> UiText.StringResourceText(Res.string.light_theme)
        DARK   -> UiText.StringResourceText(Res.string.dark_theme)
        VARIANT1   -> UiText.StringResourceText(Res.string.variant1)
        VARIANT2   -> UiText.StringResourceText(Res.string.variant2)
        NEONPULSE   -> UiText.StringResourceText(Res.string.neon_pulse_theme)
        //PHTHEME -> UiText.StringResourceText(Res.string.ph_theme)
    }
    fun getIcon(): ImageVector = when (this) {
        SYSTEM -> Icons.Default.Contrast
        LIGHT -> Icons.Default.LightMode
        DARK   -> Icons.Default.DarkMode
        VARIANT1 -> Icons.Default.ShieldMoon
        VARIANT2 -> Icons.Default.Lightbulb
        NEONPULSE -> Icons.Default.ElectricBolt
        //PHTHEME -> Icons.Default.Male
    }
}