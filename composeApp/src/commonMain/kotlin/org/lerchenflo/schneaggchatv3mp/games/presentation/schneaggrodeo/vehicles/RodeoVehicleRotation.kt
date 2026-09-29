package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import kotlin.random.Random

/** Game seconds until the first vehicle of a run, and between the end of one and the next. */
private const val VEHICLE_FIRST_SECONDS = 35f
private const val VEHICLE_INTERVAL_MIN = 28f
private const val VEHICLE_INTERVAL_RANDOM = 20f

/**
 * Decides which vehicle comes next and when. Vehicles come in shuffled rounds; [RodeoVehicle.special]
 * ones only join once every regular one came by, so they stay a treat for long runs.
 */
internal class RodeoVehicleRotation(private val vehicles: List<RodeoVehicle>) {
    private val seen = mutableSetOf<RodeoVehicleKind>()
    private val round = mutableListOf<RodeoVehicle>()
    private var last: RodeoVehicle? = null
    private var nextIn = VEHICLE_FIRST_SECONDS

    fun reset() {
        seen.clear()
        round.clear()
        last = null
        nextIn = VEHICLE_FIRST_SECONDS
    }

    /** Counts down while no vehicle is around; returns the one to send in once it is time. */
    fun tick(dt: Float): RodeoVehicle? {
        nextIn -= dt
        if (nextIn > 0f) return null
        nextIn = VEHICLE_INTERVAL_MIN + Random.nextFloat() * VEHICLE_INTERVAL_RANDOM
        if (round.isEmpty()) refill()
        return round.removeAt(0).also {
            seen += it.kind
            last = it
        }
    }

    private fun refill() {
        val regular = vehicles.filter { !it.special }
        val pool = if (regular.all { it.kind in seen }) vehicles else regular
        round += pool.shuffled()
        // Never the same one twice in a row across two rounds
        if (round.size > 1 && round.first() == last) round.add(round.removeAt(0))
    }
}
