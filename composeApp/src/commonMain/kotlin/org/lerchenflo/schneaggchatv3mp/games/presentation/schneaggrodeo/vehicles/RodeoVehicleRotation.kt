package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import kotlin.random.Random

/** Game seconds until the first vehicle of a run, and between the end of one and the next. */
private const val VEHICLE_FIRST_SECONDS = 35f
private const val VEHICLE_INTERVAL_MIN = 28f
private const val VEHICLE_INTERVAL_RANDOM = 20f

/**
 * Decides which vehicle comes next and when. Each one is picked at random, so a vehicle can come by
 * many times in a run - just never twice in a row. [RodeoVehicle.special] ones only join once every
 * regular one came by, so they stay a treat for long runs.
 */
internal class RodeoVehicleRotation(private val vehicles: List<RodeoVehicle>) {
    private val seen = mutableSetOf<RodeoVehicleKind>()
    private var last: RodeoVehicle? = null
    private var nextIn = VEHICLE_FIRST_SECONDS

    fun reset() {
        seen.clear()
        last = null
        nextIn = VEHICLE_FIRST_SECONDS
    }

    /** Counts down while no vehicle is around; returns the one to send in once it is time. */
    fun tick(dt: Float): RodeoVehicle? {
        nextIn -= dt
        if (nextIn > 0f) return null
        nextIn = VEHICLE_INTERVAL_MIN + Random.nextFloat() * VEHICLE_INTERVAL_RANDOM
        val regular = vehicles.filter { !it.special }
        val pool = (if (regular.all { it.kind in seen }) vehicles else regular)
            .filter { it != last }
            .ifEmpty { vehicles }
        return pool.random().also {
            seen += it.kind
            last = it
        }
    }
}
