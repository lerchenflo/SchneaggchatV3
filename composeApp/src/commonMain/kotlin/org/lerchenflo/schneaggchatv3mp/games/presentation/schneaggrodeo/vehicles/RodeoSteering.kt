package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import kotlin.math.min

/**
 * Up / down steering of anything that flies (plane, rocket): holding jump climbs, holding dive
 * dives, without input it slowly sinks. Accelerations in u/s², speed limits in u/s.
 */
internal class RodeoSteering(
    private val climbAccel: Float,
    private val diveAccel: Float,
    private val sinkAccel: Float,
    val maxClimb: Float,
    private val maxSink: Float,
) {
    var climb = false
    var dive = false
    /** Vertical speed, positive = up. */
    var vy = 0f
        private set

    /** Lets go of both inputs. */
    fun release() {
        climb = false
        dive = false
    }

    fun stop() {
        vy = 0f
    }

    /** Moves [y] on by [dt] and returns the new height, never above [ceiling]. */
    fun fly(y: Float, dt: Float, ceiling: Float): Float {
        val accel = when {
            climb && !dive -> climbAccel
            dive && !climb -> -diveAccel
            else -> -sinkAccel
        }
        vy = (vy + accel * dt).coerceIn(-maxSink, maxClimb)
        val newY = y + vy * dt
        if (newY < ceiling) return newY
        vy = min(0f, vy)
        return ceiling
    }
}
