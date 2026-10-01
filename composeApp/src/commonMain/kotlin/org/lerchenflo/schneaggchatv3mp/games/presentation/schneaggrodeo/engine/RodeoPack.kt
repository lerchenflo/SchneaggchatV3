package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSnailUi
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// The chasing pack of schneaggs: the run ends once it reaches the horse. Crashes let it close in,
// riding clean (and catching snails) lets the horse pull away again.
internal const val CHASE_GAP_MAX = 45f     // pack distance behind the horse; starts here (off screen above ~26)
private const val PACK_SIZE = 3
private const val PACK_SPACING = 7f
/** Advance of [RodeoPack.clock] per second. */
private const val CLOCK_SPEED = 10f

internal class RodeoPack {
    /** Distance between the horse and the first snail of the pack. */
    var gap = CHASE_GAP_MAX
        private set
    /**
     * Animation clock of the hopping pack; also drives the wobble of the snails on the track and
     * the bobbing horseshoes, so everything on the track moves in the same rhythm.
     */
    var clock = 0f
        private set

    val hasCaughtUp: Boolean get() = gap <= 0f

    fun reset() {
        gap = CHASE_GAP_MAX
        clock = 0f
    }

    fun restore(gap: Float) {
        this.gap = gap
    }

    fun tick(dt: Float) {
        clock += dt * CLOCK_SPEED
    }

    /** A crash: the pack gains [units]. */
    fun closeIn(units: Float) {
        gap -= units
    }

    fun fallBack(units: Float) {
        gap = min(CHASE_GAP_MAX, gap + units)
    }

    /** Falls back to at least [minGap]. */
    fun keepAway(minGap: Float) {
        gap = max(gap, minGap)
    }

    /** Far behind again, as far as it gets. */
    fun escape() {
        gap = CHASE_GAP_MAX
    }

    /** Riding on: the pack falls back by [rate] u/s (or creeps in, when negative). */
    fun ride(dt: Float, rate: Float) {
        gap = min(CHASE_GAP_MAX, gap + rate * dt)
        if (gap <= 0f) gap = 0f
    }

    /** The pack hopping along behind the horse, hopping the fences too (off screen while the gap is large). */
    fun ui(fences: List<Fence>): List<RodeoSnailUi> = (0 until PACK_SIZE).mapNotNull { index ->
        val x = HORSE_X + HITBOX_LEFT - SNAIL_HALF_WIDTH - gap - index * PACK_SPACING
        if (x > -SNAIL_SIZE) {
            val hop = abs(sin(clock + index * 1.3f)) * 1.5f
            val height = max(hop, hopOverFences(x, fences, reach = 8f, clearance = 2f))
            RodeoSnailUi(x, height, facingLeft = false, tiltDeg = sin(clock + index) * 6f)
        } else {
            null
        }
    }
}
