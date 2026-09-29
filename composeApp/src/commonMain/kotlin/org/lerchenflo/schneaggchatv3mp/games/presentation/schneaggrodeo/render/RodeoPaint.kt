package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Everything the track is painted with in one map: its colors and the text laid out in them. The
 * surface uses the theme; the cave (see RodeoUnderground) uses it turned inside out, so it is dark
 * in a light theme and the other way round, and everything on it keeps its contrast.
 */
internal class RodeoPaint(
    val colors: ColorScheme,
    val horseColors: RodeoHorseColors,
    /** Fence height labels by height in cm. */
    val heightLabels: Map<Int, TextLayoutResult>,
    val bodyLabelStyle: TextStyle,
    /** Caught-snail numbers painted on the horse, laid out on first use. */
    val bodyLabels: MutableMap<Int, TextLayoutResult>,
    val markerLabelStyle: TextStyle,
    val markerLabels: MutableMap<Triple<String, Long, Boolean>, TextLayoutResult>,
    val friendNameStyle: TextStyle,
    val friendNames: MutableMap<String, TextLayoutResult>,
    val snailOutline: ColorFilter,
)

/** The theme turned inside out for the cave. */
internal fun ColorScheme.cave(): ColorScheme = copy(
    surface = inverseSurface,
    surfaceContainer = inverseSurface,
    surfaceBright = inverseSurface,
    onSurface = inverseOnSurface,
    onSurfaceVariant = inverseOnSurface.copy(alpha = 0.75f),
    outline = inverseOnSurface.copy(alpha = 0.5f),
    outlineVariant = inverseOnSurface.copy(alpha = 0.3f),
    primary = inversePrimary,
)

@Composable
internal fun rememberRodeoPaint(colors: ColorScheme, textMeasurer: TextMeasurer, heightLabelTexts: Map<Int, String>): RodeoPaint {
    val typography = MaterialTheme.typography
    return remember(colors, textMeasurer, heightLabelTexts, typography) {
        val heightLabelStyle = typography.labelSmall.copy(color = colors.onSurfaceVariant)
        RodeoPaint(
            colors = colors,
            horseColors = RodeoHorseColors(
                body = colors.onSurface,
                shirt = colors.primary,
                glow = colors.primary,
                canopy = colors.secondary,
                friendShirt = colors.tertiary,
                headRing = colors.surfaceContainer,
            ),
            heightLabels = heightLabelTexts.mapValues { (_, label) -> textMeasurer.measure(label, heightLabelStyle) },
            bodyLabelStyle = typography.labelMedium.copy(color = colors.surface, fontWeight = FontWeight.Bold),
            bodyLabels = mutableMapOf(),
            markerLabelStyle = typography.labelSmall,
            markerLabels = mutableMapOf(),
            friendNameStyle = typography.labelSmall.copy(color = colors.tertiary, fontWeight = FontWeight.Bold),
            friendNames = mutableMapOf(),
            snailOutline = ColorFilter.tint(colors.surfaceContainer),
        )
    }
}
