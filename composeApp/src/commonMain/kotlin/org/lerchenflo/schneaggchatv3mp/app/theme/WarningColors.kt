package org.lerchenflo.schneaggchatv3mp.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Warning colors, which Material 3's color scheme has no role for (e.g. "users can add unlimited
 * answers"). Read them with [MaterialTheme.warningColors], which picks the light or dark set to
 * match the current color scheme's background.
 */
@Immutable
data class WarningColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val lightWarningColors = WarningColors(
    warning = warningLight,
    onWarning = onWarningLight,
    warningContainer = warningContainerLight,
    onWarningContainer = onWarningContainerLight,
)

internal val darkWarningColors = WarningColors(
    warning = warningDark,
    onWarning = onWarningDark,
    warningContainer = warningContainerDark,
    onWarningContainer = onWarningContainerDark,
)

val MaterialTheme.warningColors: WarningColors
    @Composable
    @ReadOnlyComposable
    get() = if (colorScheme.background.luminance() > 0.5f) lightWarningColors else darkWarningColors
