package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import kotlin.math.min
import kotlin.random.Random

// Every knob that makes a run harder over time, in one place - tune the game here.
//
// 1. Warm-up: the pace ramps up slowly like the Chrome dino (about two minutes until MAX_SPEED)
//    while the fences grow from low to high over FENCE_GROWTH_SECONDS.
// 2. Pressure: once the top pace is reached the ride keeps getting harder, so no run lasts forever.
//    Over PRESSURE_RAMP_SECONDS the pace rises further, fences come tighter and wider, runners come
//    more often and the pack stops falling back - in the end it closes in even on a clean ride.
//
// Every "PRESSURE_" value is what its plain counterpart turns into at full pressure.

internal const val START_SPEED = 75f       // u/s
private const val MAX_SPEED = 180f         // u/s, before pressure
private const val PRESSURE_MAX_SPEED = 215f
internal const val ACCELERATION = 0.9f     // u/s per second of riding

private const val PRESSURE_START_SECONDS = 120f
private const val PRESSURE_RAMP_SECONDS = 180f

// Fence height range at the start of a run; both ends grow linearly to their final values
private const val START_MAX_FENCE_CM = 90
private const val FINAL_MIN_FENCE_CM = 100
private const val FENCE_GROWTH_SECONDS = 150f

// Wide jumps (oxers) only show up once the pace is high enough
private const val OXER_MIN_SPEED = 110f
private const val OXER_CHANCE = 0.3f
private const val PRESSURE_OXER_CHANCE = 0.5f

// Gap between two fences, in riding time: scales with the pace so the reaction time stays constant
private const val FENCE_GAP_MIN_SECONDS = 0.9f   // never tighter: a full jump needs ~0.85 s
private const val FENCE_GAP_RANDOM_SECONDS = 0.9f
private const val PRESSURE_FENCE_GAP_RANDOM_SECONDS = 0.35f

// Runners (sprinting snails) only join once the rider had some time to warm up
internal const val RUNNER_START_SECONDS = 30f
private const val RUNNER_INTERVAL_MIN = 5f
private const val RUNNER_INTERVAL_RANDOM = 7f
private const val PRESSURE_RUNNER_INTERVAL_MIN = 2.5f
private const val PRESSURE_RUNNER_INTERVAL_RANDOM = 3f

// u/s the pack falls back while riding clean; negative = it creeps in
private const val CHASE_GAP_REGAIN = 1.5f
private const val PRESSURE_CHASE_GAP_REGAIN = -1.5f

/** Answers "how hard is it right now?" from the game seconds ridden and the current pace. */
internal object RodeoDifficulty {

    /** 0 until [PRESSURE_START_SECONDS] of riding, then rises to 1 over [PRESSURE_RAMP_SECONDS]. */
    fun pressure(elapsed: Float): Float =
        ((elapsed - PRESSURE_START_SECONDS) / PRESSURE_RAMP_SECONDS).coerceIn(0f, 1f)

    /** The pace the horse accelerates towards. */
    fun topSpeed(elapsed: Float): Float = lerp(MAX_SPEED, PRESSURE_MAX_SPEED, pressure(elapsed))

    /** Height of the next fence, in whole 10 cm steps. */
    fun randomFenceHeightCm(elapsed: Float): Int {
        val progress = min(1f, elapsed / FENCE_GROWTH_SECONDS)
        val minCm = MIN_FENCE_CM + ((FINAL_MIN_FENCE_CM - MIN_FENCE_CM) * progress).toInt()
        val maxCm = START_MAX_FENCE_CM + ((MAX_FENCE_CM - START_MAX_FENCE_CM) * progress).toInt()
        return Random.nextInt(minCm / 10, maxCm / 10 + 1) * 10
    }

    fun rollOxer(speed: Float, elapsed: Float): Boolean =
        speed >= OXER_MIN_SPEED && Random.nextFloat() < lerp(OXER_CHANCE, PRESSURE_OXER_CHANCE, pressure(elapsed))

    /** Free space before the next fence; under pressure it is closer to the tightest gap more often. */
    fun randomFenceGap(speed: Float, elapsed: Float): Float = speed * (
        FENCE_GAP_MIN_SECONDS +
            Random.nextFloat() * lerp(FENCE_GAP_RANDOM_SECONDS, PRESSURE_FENCE_GAP_RANDOM_SECONDS, pressure(elapsed))
        )

    /** Seconds until the next runner. */
    fun randomRunnerInterval(elapsed: Float): Float {
        val pressure = pressure(elapsed)
        return lerp(RUNNER_INTERVAL_MIN, PRESSURE_RUNNER_INTERVAL_MIN, pressure) +
                Random.nextFloat() * lerp(RUNNER_INTERVAL_RANDOM, PRESSURE_RUNNER_INTERVAL_RANDOM, pressure)
    }

    /** u/s the pack falls back (or creeps in, when negative) while riding. */
    fun chaseGapRegain(elapsed: Float): Float = lerp(CHASE_GAP_REGAIN, PRESSURE_CHASE_GAP_REGAIN, pressure(elapsed))
}
