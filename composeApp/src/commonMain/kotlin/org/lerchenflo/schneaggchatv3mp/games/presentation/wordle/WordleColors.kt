package org.lerchenflo.schneaggchatv3mp.games.presentation.wordle

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleLetterState

/**
 * Wordle's feedback colors belong to the game's rules rather than to the app
 * theme: "right spot" has to read as green and "wrong spot" as orange in every
 * theme variant. The theme roles cannot express that — `tertiary` is near-black
 * in the light scheme, so PRESENT and ABSENT were indistinguishable — so these
 * three are an explicitly approved non-theme exception.
 *
 * Each has a light and a dark variant, picked from the surface luminance instead
 * of the system dark mode, because the custom theme variants (Neon Pulse, Flo,
 * Anna, …) are light or dark independently of the system setting.
 */
private val CorrectLight = Color(0xFF2E7D32)
private val CorrectDark = Color(0xFF3F8C43)
private val PresentLight = Color(0xFFBF6A00)
private val PresentDark = Color(0xFFC97A1E)
private val AbsentLight = Color(0xFF6E6E73)
private val AbsentDark = Color(0xFF45454A)

/** All three feedback colors are dark enough to carry white letters. */
private val OnFeedback = Color(0xFFFFFFFF)

internal data class WordleTileColors(
    val background: Color,
    val content: Color,
)

/** True for a dark color scheme, whichever theme variant is active. */
@Composable
private fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/**
 * Colors of one board tile or keyboard key. null is a tile that holds no
 * feedback yet — an empty cell or the word currently being typed — and [UNUSED]
 * is a keyboard key whose letter has not been guessed; both stay on theme.
 */
@Composable
internal fun WordleLetterState?.tileColors(): WordleTileColors {
    val dark = isDarkScheme()
    return when (this) {
        WordleLetterState.CORRECT -> WordleTileColors(
            background = if (dark) CorrectDark else CorrectLight,
            content = OnFeedback,
        )

        WordleLetterState.PRESENT -> WordleTileColors(
            background = if (dark) PresentDark else PresentLight,
            content = OnFeedback,
        )

        WordleLetterState.ABSENT -> WordleTileColors(
            background = if (dark) AbsentDark else AbsentLight,
            content = OnFeedback,
        )

        WordleLetterState.UNUSED -> WordleTileColors(
            background = MaterialTheme.colorScheme.surfaceContainerHighest,
            content = MaterialTheme.colorScheme.onSurface,
        )

        null -> WordleTileColors(
            background = Color.Transparent,
            content = MaterialTheme.colorScheme.onSurface,
        )
    }
}
