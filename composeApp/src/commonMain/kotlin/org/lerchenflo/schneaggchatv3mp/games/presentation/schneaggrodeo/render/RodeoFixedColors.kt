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
// Ravens: black in every theme, small red eyes, a dark grey beak (explicitly requested)
internal val RAVEN_BLACK = Color(0xFF16161A)
internal val RAVEN_BEAK = Color(0xFF3A3A42)
internal val RAVEN_EYE = Color(0xFFE53935)
internal val RAVEN_OUTLINE = Color(0xFFFFFFFF)
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

// The cave (see RodeoMapSwitch): crystals, torch light and the wooden mine shaft frame
// (explicitly allowed; everything else down there is the theme turned inside out, see RodeoPaint)
internal val CRYSTAL_COLORS = listOf(Color(0xFF4FC3F7), Color(0xFFBA68C8))
internal val TORCH_COLOR = Color(0xFFFFB300)

// The pizza oven by the roadside and its pizza
internal val OVEN_BRICK = Color(0xFFB5532E)
internal val OVEN_BRICK_DARK = Color(0xFF7A3317)
internal val OVEN_STONE = Color(0xFF9E9E9E)
internal val FIRE_COLOR = Color(0xFFFF9800)
internal val FIRE_CORE_COLOR = Color(0xFFFFEB3B)
internal val PIZZA_CRUST = Color(0xFFE0A860)
internal val PIZZA_SAUCE = Color(0xFFD84315)
internal val PIZZA_CHEESE = Color(0xFFFFE082)
internal val PIZZA_SALAMI = Color(0xFFB71C1C)
internal val PIZZA_BASIL = Color(0xFF388E3C)

// The sea: water, buoys, sharks, swim rings, oil and pearls
internal val SEA_WATER = Color(0xFF0D47A1)
internal val SEA_WATER_LIGHT = Color(0xFF4FC3F7)
internal val SEAWEED = Color(0xFF2E7D32)
internal val BUOY_RED = Color(0xFFE53935)
internal val BUOY_WHITE = Color(0xFFF5F5F5)
internal val SHARK_GRAY = Color(0xFF78909C)
internal val SHARK_BELLY = Color(0xFFECEFF1)
internal val SWIM_RING = Color(0xFFFF80AB)
internal val OIL_BLACK = Color(0xFF1B1B1B)
internal val OIL_SHEEN = listOf(Color(0xFF7E57C2), Color(0xFF26A69A), Color(0xFFFFCA28))
internal val PEARL_WHITE = Color(0xFFF8F4EC)
internal val CLAM_SHELL = Color(0xFFBCAAA4)
internal val TREASURE_WOOD = Color(0xFF8D6E63)
internal val TREASURE_GOLD = Color(0xFFFFC107)

// Earth, timber, rock and gold: the dirt mounds in the cave and the drill
internal val EARTH_DARK = Color(0xFF4E342E)
internal val EARTH_LIGHT = Color(0xFF6D4C41)
internal val MINE_TIMBER = Color(0xFF8D6E63)
internal val MINE_ROCK = Color(0xFF757575)
internal val GOLD_NUGGET = Color(0xFFFFC107)
internal val GOLD_SHINE = Color(0xFFFFF59D)
internal val SHOVEL_STEEL = Color(0xFF9E9E9E)

// Weather and time of day
internal val RAIN_COLOR = Color(0xFF90CAF9)
internal val SNOW_COLOR = Color(0xFFFFFFFF)
internal val DUSK_TINT = Color(0xFFFF7043)
internal val NIGHT_SHADE = Color(0xFF0A0F24)

// The Käsknöpfle kiosk
internal val KIOSK_AWNING = Color(0xFFE53935)
internal val KIOSK_WOOD = Color(0xFFA1887F)
internal val KNOEPFLE_YELLOW = Color(0xFFFFE082)
internal val ONION_BROWN = Color(0xFF8D6E63)
internal val BOWL_BROWN = Color(0xFF6D4C41)

// Rainbow after the rain (outer to inner band), fireflies and the ghost horse's glow
internal val RAINBOW_BANDS = listOf(
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
)
internal val FIREFLY_GLOW = Color(0xFFD4FF5C)
internal val GHOST_GLOW = Color(0xFF80DEEA)

// The beach and the harbour pier
internal val SAND = Color(0xFFE6C98F)
internal val PIER_WOOD = Color(0xFF8D6E63)
internal val PIER_WOOD_DARK = Color(0xFF5D4037)

// The rope bridge over a gorge (planks and posts use the pier's wood)
internal val BRIDGE_ROPE = Color(0xFFC8A165)

// The deep sea: darkest water, fish, jellyfish, the whale and old wrecks
internal val SEA_DEEP = Color(0xFF0D2B45)
internal val FISH_COLORS = listOf(Color(0xFFFFB74D), Color(0xFF4DD0E1), Color(0xFFF06292))
internal val JELLY_PINK = Color(0xFFF8BBD0)
internal val WHALE_BLUE = Color(0xFF3F6E9E)
internal val WHALE_BELLY = Color(0xFFB0C4DE)
internal val WRECK_WOOD = Color(0xFF5D4037)
internal val WRECK_WOOD_DARK = Color(0xFF3E2723)
