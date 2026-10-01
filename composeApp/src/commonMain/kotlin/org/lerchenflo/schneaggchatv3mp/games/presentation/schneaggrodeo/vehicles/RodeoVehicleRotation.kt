package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.EASE_MULTIPLIER
import kotlin.math.roundToInt
import kotlin.random.Random

/** Game seconds until the first vehicle of a run, and between the end of one and the next. */
private const val VEHICLE_FIRST_SECONDS = 35f
private const val VEHICLE_INTERVAL_MIN = 28f
private const val VEHICLE_INTERVAL_RANDOM = 20f
/**
 * Special vehicles join once this many different regular ones came by (or all, on maps with fewer);
 * divided by EASE_MULTIPLIER, as are all the waiting times here.
 */
private const val SPECIAL_AFTER_SEEN = 8

/**
 * Decides which vehicle comes next and when. Each one is picked at random, so a vehicle can come by
 * many times in a run - just never twice in a row. [RodeoVehicle.special] ones only join once
 * [SPECIAL_AFTER_SEEN] different regular ones came by, so they stay a treat for long runs.
 */
internal class RodeoVehicleRotation(
    private val vehicles: List<RodeoVehicle>,
    private val firstSeconds: Float = VEHICLE_FIRST_SECONDS,
    private val intervalMin: Float = VEHICLE_INTERVAL_MIN,
    private val intervalRandom: Float = VEHICLE_INTERVAL_RANDOM,
) {
    private val seen = mutableSetOf<RodeoVehicleKind>()
    private var last: RodeoVehicle? = null
    private var nextIn = firstSeconds / EASE_MULTIPLIER

    fun reset() {
        seen.clear()
        last = null
        nextIn = firstSeconds / EASE_MULTIPLIER
    }

    fun has(kind: RodeoVehicleKind): Boolean = vehicles.any { it.kind == kind }

    /** Back on this rotation's map: the first vehicle comes after [firstSeconds] again. */
    fun restartTimer() {
        nextIn = firstSeconds / EASE_MULTIPLIER
    }

    /**
     * Counts down while no vehicle is around; returns the one to send in once it is time. Vehicles
     * that need a flat track only come while the track ahead is [flatTrack].
     */
    fun tick(dt: Float, flatTrack: Boolean): RodeoVehicle? {
        nextIn -= dt
        if (nextIn > 0f) return null
        val regular = vehicles.filter { !it.special }
        val seenRegular = regular.count { it.kind in seen }
        val specialAfter = (SPECIAL_AFTER_SEEN / EASE_MULTIPLIER).roundToInt().coerceAtLeast(1).coerceAtMost(regular.size)
        val pool = (if (seenRegular >= specialAfter) vehicles else regular)
            .filter { it != last }
            .ifEmpty { vehicles }
            .filter { flatTrack || !it.needsFlatTrack }
        if (pool.isEmpty()) {
            // Only fast ones left: try again once the track ahead is flat
            nextIn = 1f
            return null
        }
        nextIn = (intervalMin + Random.nextFloat() * intervalRandom) / EASE_MULTIPLIER
        return pool.random().also {
            seen += it.kind
            last = it
        }
    }
}
