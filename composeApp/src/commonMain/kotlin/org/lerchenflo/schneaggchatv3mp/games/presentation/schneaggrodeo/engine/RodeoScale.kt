package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

// The shared measuring system of the whole game: engine, vehicles and drawing all use it.
//
// All positions and speeds are in "units" (u). One unit is the canvas height / WORLD_HEIGHT_UNITS,
// so the game plays identically on every screen size - only the visible width (how far ahead you
// see) changes. x runs from the left canvas edge to the right, y (or "height") up from the ground.

internal const val WORLD_HEIGHT_UNITS = 75f
/** Ground line distance from the canvas bottom. */
internal const val GROUND_OFFSET_UNITS = 10f

/** One unit is also 10 cm in "horse scale", which is what the fence height labels show. */
internal const val CM_PER_UNIT = 10
/** One unit is 10 cm, so u/s * 0.36 = km/h (75 u/s start pace = 27 km/h, 180 u/s top pace = 65 km/h). */
internal const val KMH_PER_UNIT_PER_SECOND = 0.36f
/** Distance ridden per score point. */
internal const val UNITS_PER_POINT = 10f

// The horse never moves horizontally while riding - the world scrolls by under it.
/** Left edge of the horse. */
internal const val HORSE_X = 12f
/** The rider's rein hand (where the lasso starts), relative to the horse's left edge / hooves. */
internal const val HAND_X = 18f
internal const val HAND_Y = 18f
/** The saddle, relative to the horse's left edge / hooves. */
internal const val COWBOY_SEAT_X = 12f
internal const val COWBOY_SEAT_Y = 16f
/** Feet to torso of the standing cowboy. */
internal const val COWBOY_LEG_LENGTH = 6f

// Hitbox of the horse relative to its left edge / hooves - a bit smaller than the drawing so near
// misses feel fair.
internal const val HITBOX_LEFT = 8f
internal const val HITBOX_RIGHT = 24f
internal const val HITBOX_BOTTOM = 1f
