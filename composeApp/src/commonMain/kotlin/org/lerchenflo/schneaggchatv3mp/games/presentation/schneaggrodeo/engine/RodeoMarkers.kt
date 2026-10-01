package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGhostUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMarkerUi
import kotlin.math.min

/** Highscore markers stay visible this far past either edge, so their labels slide in and out. */
private const val MARKER_VISIBLE_MARGIN = 40f
/**
 * Bonus points move the markers closer without any distance ridden. They catch up at this speed
 * (u/s) instead of jumping, so a marker slides in from the edge rather than popping up mid-screen.
 */
private const val MARKER_CATCH_UP_SPEED = 300f

/**
 * The all-time highscores as posts along the track: each one reaches the horse exactly when the
 * run's score reaches the entry's score.
 */
internal class RodeoMarkers {
    /** Bonus points already applied to the markers, in units; trails the real bonus points. */
    private var appliedBonus = 0f

    fun reset(bonusPoints: Int = 0) {
        appliedBonus = bonusPoints * UNITS_PER_POINT
    }

    fun catchUp(dt: Float, bonusPoints: Int) {
        appliedBonus = min(bonusPoints * UNITS_PER_POINT, appliedBonus + MARKER_CATCH_UP_SPEED * dt)
    }

    /**
     * [ghosts] sorted by score; only the ones near the visible stretch become markers. Each unit
     * ridden is worth [pointsFactor] times the normal points (see RodeoLevel).
     */
    fun ui(ghosts: List<RodeoGhostUi>, distance: Float, worldWidth: Float, pointsFactor: Float): List<RodeoMarkerUi> {
        // A marker's post stands where the horse's nose will be once the score reaches the entry.
        // Bonus points add score without distance, so markers move closer by the bonus (smoothly).
        return ghosts.mapIndexedNotNull { index, ghost ->
            val x = HORSE_X + HITBOX_RIGHT + (ghost.score * UNITS_PER_POINT - appliedBonus) / pointsFactor - distance
            if (x > -MARKER_VISIBLE_MARGIN && x < worldWidth + MARKER_VISIBLE_MARGIN) {
                RodeoMarkerUi(
                    x = x,
                    username = ghost.username,
                    score = ghost.score,
                    isOwn = ghost.isOwn,
                    // Neighbouring highscores alternate between a tall and a short post
                    staggered = index % 2 == 1,
                )
            } else {
                null
            }
        }
    }
}
