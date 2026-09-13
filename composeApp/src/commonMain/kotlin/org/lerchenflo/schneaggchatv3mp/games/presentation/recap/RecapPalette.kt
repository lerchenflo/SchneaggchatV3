package org.lerchenflo.schneaggchatv3mp.games.presentation.recap

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The recap is a deliberately branded, theme-independent story (think Spotify Wrapped): every page
 * paints its own saturated full-bleed background and reads identically in light and dark mode, so
 * it does not use MaterialTheme colors. Every color the recap uses lives here so the palette stays
 * consistent and is changed in exactly one place.
 */
object RecapPalette {
    val Ink = Color(0xFF121212)
    val Paper = Color(0xFFFFFFFF)
    val Green = Color(0xFF1DB954)
    val Pink = Color(0xFFFF007F)
    val Lemon = Color(0xFFFFDF00)
    val Amber = Color(0xFFFFB300)
    val Violet = Color(0xFFD63CFF)
    val Blue = Color(0xFF0A1ED3)
    val Aqua = Color(0xFF00E5C7)
    val Orange = Color(0xFFFF9E00)
    val Coral = Color(0xFFFF5C5C)
    val Gold = Color(0xFFFFD700)
    val Sky = Color(0xFF35C4FF)
    val Lilac = Color(0xFFFFB6F9)
}

/** Colors of one story page: background gradient, two accents, text color and the decor shapes tint. */
@Immutable
data class RecapPageTheme(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val accent: Color,
    val secondary: Color,
    val onBackground: Color,
    val decor: Color,
)

object RecapPageThemes {
    val Intro = RecapPageTheme(RecapPalette.Blue, Color(0xFF040A4F), RecapPalette.Green, RecapPalette.Violet, RecapPalette.Paper, RecapPalette.Violet)
    val Sent = RecapPageTheme(Color(0xFF0C1033), Color(0xFF2B0A4E), RecapPalette.Pink, RecapPalette.Green, RecapPalette.Paper, RecapPalette.Pink)
    val Typing = RecapPageTheme(Color(0xFF00343A), Color(0xFF001B1F), RecapPalette.Aqua, RecapPalette.Lemon, RecapPalette.Paper, RecapPalette.Aqua)
    val Rhythm = RecapPageTheme(Color(0xFF3A0057), Color(0xFF1B0029), RecapPalette.Orange, RecapPalette.Lemon, RecapPalette.Paper, RecapPalette.Orange)
    val Received = RecapPageTheme(Color(0xFF6B0F1A), Color(0xFF2E060B), RecapPalette.Lemon, RecapPalette.Coral, RecapPalette.Paper, RecapPalette.Coral)
    val TopContacts = RecapPageTheme(RecapPalette.Lemon, RecapPalette.Amber, RecapPalette.Ink, RecapPalette.Blue, RecapPalette.Ink, RecapPalette.Paper)
    val Reactions = RecapPageTheme(Color(0xFF7A0BC0), Color(0xFF3B0764), RecapPalette.Lilac, RecapPalette.Lemon, RecapPalette.Paper, RecapPalette.Lilac)
    val Social = RecapPageTheme(Color(0xFF005F2E), Color(0xFF00230F), RecapPalette.Green, RecapPalette.Lemon, RecapPalette.Paper, RecapPalette.Green)
    val Groups = RecapPageTheme(Color(0xFF00456B), Color(0xFF001D2E), RecapPalette.Sky, RecapPalette.Lemon, RecapPalette.Paper, RecapPalette.Sky)
    val Leaderboard = RecapPageTheme(Color(0xFF1A1A1A), Color(0xFF000000), RecapPalette.Gold, RecapPalette.Paper, RecapPalette.Paper, RecapPalette.Gold)
    val Map = RecapPageTheme(Color(0xFF4A3200), Color(0xFF1F1500), RecapPalette.Orange, RecapPalette.Lemon, RecapPalette.Paper, RecapPalette.Orange)
    val Games = RecapPageTheme(Color(0xFF10002B), Color(0xFF240090), RecapPalette.Aqua, RecapPalette.Gold, RecapPalette.Paper, RecapPalette.Aqua)
    val BetaTester = RecapPageTheme(Color(0xFF4A0000), Color(0xFF1A0000), RecapPalette.Coral, RecapPalette.Paper, RecapPalette.Paper, RecapPalette.Coral)
    val Password = RecapPageTheme(Color(0xFF4A3B00), Color(0xFF1F1800), RecapPalette.Lemon, RecapPalette.Paper, RecapPalette.Paper, RecapPalette.Lemon)
    val Outro = RecapPageTheme(RecapPalette.Blue, RecapPalette.Violet, RecapPalette.Paper, RecapPalette.Lemon, RecapPalette.Paper, RecapPalette.Paper)
}
