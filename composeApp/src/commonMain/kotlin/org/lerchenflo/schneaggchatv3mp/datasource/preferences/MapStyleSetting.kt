package org.lerchenflo.schneaggchatv3mp.datasource.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.map_style_bright
import schneaggchatv3mp.composeapp.generated.resources.map_style_dark
import schneaggchatv3mp.composeapp.generated.resources.map_style_fiord
import schneaggchatv3mp.composeapp.generated.resources.map_style_liberty
import schneaggchatv3mp.composeapp.generated.resources.map_style_positron

enum class MapStyleSetting {
    LIBERTY,
    BRIGHT,
    POSITRON,
    DARK,
    FIORD;

    val tileUrl: String get() = when (this) {
        LIBERTY -> "https://tiles.openfreemap.org/styles/liberty"
        BRIGHT -> "https://tiles.openfreemap.org/styles/bright"
        POSITRON -> "https://tiles.openfreemap.org/styles/positron"
        DARK -> "https://tiles.openfreemap.org/styles/dark"
        FIORD -> "https://tiles.openfreemap.org/styles/fiord"
    }

    fun toUiText(): UiText = when (this) {
        LIBERTY -> UiText.StringResourceText(Res.string.map_style_liberty)
        BRIGHT -> UiText.StringResourceText(Res.string.map_style_bright)
        POSITRON -> UiText.StringResourceText(Res.string.map_style_positron)
        DARK -> UiText.StringResourceText(Res.string.map_style_dark)
        FIORD -> UiText.StringResourceText(Res.string.map_style_fiord)
    }

    fun getIcon(): ImageVector = Icons.Default.Map
}
