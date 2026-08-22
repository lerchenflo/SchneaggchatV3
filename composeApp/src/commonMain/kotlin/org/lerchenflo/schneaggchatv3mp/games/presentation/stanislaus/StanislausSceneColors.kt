package org.lerchenflo.schneaggchatv3mp.games.presentation.stanislaus

import androidx.compose.ui.graphics.Color

/**
 * Small fixed palette for the pond scene (sky, water, fish, light ray). These need to read as
 * sky/water regardless of the active app theme, so they intentionally don't come from
 * MaterialTheme.colorScheme — everything else in this game (HUD, overlays, buttons, labels)
 * still uses the theme as usual.
 */
object StanislausSceneColors {
    val sky = Color(0xFF8FD3F4)
    val skyHorizon = Color(0xFFC7ECFF)
    val water = Color(0xFF1E88A8)
    val waterDeep = Color(0xFF0B3D55)
    val mirror = Color(0xFFEAF7FF)
    val sand = Color(0xFFD9C08A)
    val fish = Color(0xFFFF8A3D)
    val fishFin = Color(0xFFE8630A)
    val ghost = Color(0xFFFFE0BE)
    val spear = Color(0xFF6D4C2B)
    val ray = Color(0xFFFFF176)
    val hit = Color(0xFF66BB6A)
    val miss = Color(0xFFEF5350)
}
