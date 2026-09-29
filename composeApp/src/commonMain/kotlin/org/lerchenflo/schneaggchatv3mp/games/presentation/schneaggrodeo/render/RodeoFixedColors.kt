package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.graphics.Color

// The only fixed (non-theme) colors of the game besides the vehicle-specific ones in their drawing
// files - each one was explicitly requested. Everything else comes from MaterialTheme.colorScheme.

/** Same rainbow palette as the TowerStack game (explicitly requested for the poles). */
internal val POLE_COLORS = listOf(
    Color(0xFFFF0000), // Red
    Color(0xFFFF7F00), // Orange
    Color(0xFF00FF00), // Green
    Color(0xFF0000FF)  // Blue
)

/** Cowboy hat, explicitly requested as a fixed orange. */
internal val HAT_COLOR = Color(0xFFFF8C00)

// Speedometer, explicitly requested in the same traffic sign look as the one on the Schneaggmap
internal val SPEEDOMETER_FILL = Color.White
internal val SPEEDOMETER_RING = Color.Red
internal val SPEEDOMETER_TEXT = Color.Black

// Space (rocket ride), explicitly requested as a fixed dark navy with white stars
internal val SPACE_COLOR = Color(0xFF0B1026)
internal val STAR_COLOR = Color.White

// Coats of the horses other than the theme-colored one the run starts on (wild horses, friends'
// horses, new horses), explicitly requested as fixed colors
internal val HORSE_COATS = listOf(
    Color(0xFF8B5A2B), // Bay
    Color(0xFFA0522D), // Chestnut
    Color(0xFF5C3A1E), // Dark bay
    Color(0xFFD2A45A), // Palomino
    Color(0xFF9E9E9E), // Grey
)

// A horse's strength: gold from the hooves up for its level, hearts on its side (explicitly requested)
internal val LEVEL_GOLD_COLOR = Color(0xFFFFD700)
internal val HEART_COLOR = Color(0xFFE53935)

// Mud puddles and carrots (explicitly requested)
internal val MUD_COLOR = Color(0xFF6D4C2F)
internal val MUD_DARK_COLOR = Color(0xFF4E3521)
internal val CARROT_COLOR = Color(0xFFF57C00)
internal val CARROT_LEAF_COLOR = Color(0xFF43A047)

// Mountain with snow cap, the cable car's cable and stations (explicitly requested)
internal val MOUNTAIN_ROCK = Color(0xFF78909C)
internal val MOUNTAIN_ROCK_DARK = Color(0xFF546E7A)
internal val MOUNTAIN_SNOW = Color(0xFFF5F7FA)
internal val CABLE_COLOR = Color(0xFF37474F)
internal val STATION_WALL = Color(0xFFD7CCC8)

// Forest and magic mushrooms (explicitly requested)
internal val TREE_TRUNK = Color(0xFF5D4037)
internal val TREE_LEAVES = Color(0xFF2E7D32)
internal val TREE_LEAVES_DARK = Color(0xFF1B5E20)
internal val MUSHROOM_RED = Color(0xFFE53935)
internal val MUSHROOM_DOTS = Color.White
internal val MUSHROOM_STEM = Color(0xFFFFF3E0)

// The cave (see RodeoUnderground): crystals, torch light and the wooden mine shaft frame
// (explicitly allowed; everything else down there is the theme turned inside out, see RodeoPaint)
internal val CRYSTAL_COLORS = listOf(Color(0xFF4FC3F7), Color(0xFFBA68C8))
internal val TORCH_COLOR = Color(0xFFFFB300)
