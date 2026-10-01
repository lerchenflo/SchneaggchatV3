package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import kotlin.math.min
import kotlin.random.Random

// Every knob that makes a run harder over time, in one place - tune the game here.
//
// 1. Warm-up: the pace ramps up slowly like the Chrome dino (about two minutes until MAX_SPEED)
//    while the fences grow from low to high over FENCE_GROWTH_SECONDS.
// 2. Pressure: once the top pace is reached the ride keeps getting harder for a long time.
//    Over PRESSURE_RAMP_SECONDS the pace rises further, fences come tighter, higher and wider,
//    runners come more often, the pack stops falling back on its own and every mistake costs more.
//    The pack never closes in on a clean ride though - only mistakes bring it nearer.
//
// Every "PRESSURE_" value is what its plain counterpart turns into at full pressure.
//
// On top of that: EASE_MULTIPLIER (set by hand, for testing) and the level the player picks
// (low / medium / high, see RodeoLevel below).

/**
 * One knob for a friendlier game, set by hand: above 1 the vehicles come more often, the premium
 * ones join sooner (see RodeoVehicleRotation) and the warm-up at the start lasts longer and stays
 * easier (slower pace gain, fences growing later, pressure starting later). 1 = as designed.
 */
internal const val EASE_MULTIPLIER = 1f

internal const val START_SPEED = 75f       // u/s
private const val MAX_SPEED = 180f         // u/s, before pressure
private const val PRESSURE_MAX_SPEED = 215f
internal const val ACCELERATION = 0.9f     // u/s per second of riding

private const val PRESSURE_START_SECONDS = 120f
private const val PRESSURE_RAMP_SECONDS = 480f

// Fence height range at the start of a run; both ends grow linearly to their final values
private const val START_MAX_FENCE_CM = 90
private const val FINAL_MIN_FENCE_CM = 100
/** Under pressure the low fences die out: the lowest one grows on up to this. */
private const val PRESSURE_MIN_FENCE_CM = 130
private const val FENCE_GROWTH_SECONDS = 150f

// Wide jumps (oxers) only show up once the pace is high enough
private const val OXER_MIN_SPEED = 110f
private const val OXER_CHANCE = 0.3f
private const val PRESSURE_OXER_CHANCE = 0.6f

// Gap between two fences, in riding time: scales with the pace so the reaction time stays constant
private const val FENCE_GAP_MIN_SECONDS = 0.9f   // never tighter: a full jump needs ~0.85 s
private const val FENCE_GAP_RANDOM_SECONDS = 0.9f
private const val PRESSURE_FENCE_GAP_RANDOM_SECONDS = 0.25f

// Runners (sprinting snails) only join once the rider had some time to warm up
internal const val RUNNER_START_SECONDS = 30f
private const val RUNNER_INTERVAL_MIN = 5f
private const val RUNNER_INTERVAL_RANDOM = 7f
private const val PRESSURE_RUNNER_INTERVAL_MIN = 2.5f
private const val PRESSURE_RUNNER_INTERVAL_RANDOM = 3f

// u/s the pack falls back while riding clean; under pressure it just holds its distance
private const val CHASE_GAP_REGAIN = 1.5f
private const val PRESSURE_CHASE_GAP_REGAIN = 0f

// What a mistake (knocked fence, mud, trip on foot) lets the pack gain, times this
private const val PRESSURE_PENALTY_FACTOR = 2f

/**
 * What the picked level changes on top of the time-based difficulty: the top pace, the highest
 * fence and how much higher every fence is, what a mistake costs, how fast a clean ride pushes the pack back and the points per
 * distance ridden (harder pays more). The start screen offers MEDIUM ("easier") and HIGH ("harder").
 */
internal enum class RodeoLevel(
    val speedFactor: Float,
    val maxFenceCm: Int,
    val extraFenceCm: Int,
    val penaltyFactor: Float,
    val regainFactor: Float,
    val pointsFactor: Float,
) {
    LOW(speedFactor = 0.85f, maxFenceCm = 140, extraFenceCm = 0, penaltyFactor = 0.7f, regainFactor = 1.5f, pointsFactor = 0.75f),
    MEDIUM(speedFactor = 1f, maxFenceCm = MAX_FENCE_CM, extraFenceCm = 0, penaltyFactor = 1f, regainFactor = 1f, pointsFactor = 1f),
    HIGH(speedFactor = 1.03f, maxFenceCm = HIGHEST_FENCE_CM, extraFenceCm = 20, penaltyFactor = 1.8f, regainFactor = 0.3f, pointsFactor = 1.5f);

    val difficulty: GameDifficulty
        get() = when (this) {
            LOW -> GameDifficulty.LOW
            MEDIUM -> GameDifficulty.MEDIUM
            HIGH -> GameDifficulty.HIGH
        }

    companion object {
        fun of(difficulty: GameDifficulty): RodeoLevel = when (difficulty) {
            GameDifficulty.LOW -> LOW
            GameDifficulty.MEDIUM -> MEDIUM
            GameDifficulty.HIGH -> HIGH
        }
    }
}

/** Answers "how hard is it right now?" from the game seconds ridden and the current pace. */
internal object RodeoDifficulty {

    /** The level of the current run; set by the engine when a run starts or is restored. */
    var level = RodeoLevel.MEDIUM

    /** u/s per second the horse gains; slower while [EASE_MULTIPLIER] stretches the warm-up. */
    val acceleration: Float get() = ACCELERATION / EASE_MULTIPLIER

    /** 0 until [PRESSURE_START_SECONDS] of riding, then rises to 1 over [PRESSURE_RAMP_SECONDS]. */
    fun pressure(elapsed: Float): Float =
        ((elapsed - PRESSURE_START_SECONDS * EASE_MULTIPLIER) / PRESSURE_RAMP_SECONDS).coerceIn(0f, 1f)

    /** The pace the horse accelerates towards. */
    fun topSpeed(elapsed: Float): Float = lerp(MAX_SPEED, PRESSURE_MAX_SPEED, pressure(elapsed)) * level.speedFactor

    /** Height of the next fence, in whole 10 cm steps; the level adds to it and caps it. */
    fun randomFenceHeightCm(elapsed: Float): Int {
        val progress = min(1f, elapsed / (FENCE_GROWTH_SECONDS * EASE_MULTIPLIER))
        val maxCm = min(
            level.maxFenceCm,
            START_MAX_FENCE_CM + ((MAX_FENCE_CM - START_MAX_FENCE_CM) * progress).toInt() + level.extraFenceCm,
        )
        val minCm = min(
            maxCm,
            MIN_FENCE_CM + ((FINAL_MIN_FENCE_CM - MIN_FENCE_CM) * progress).toInt() +
                    ((PRESSURE_MIN_FENCE_CM - FINAL_MIN_FENCE_CM) * pressure(elapsed)).toInt() + level.extraFenceCm,
        )
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

    /** u/s the pack falls back while riding clean; never negative, a clean ride never loses ground. */
    fun chaseGapRegain(elapsed: Float): Float =
        lerp(CHASE_GAP_REGAIN, PRESSURE_CHASE_GAP_REGAIN, pressure(elapsed)) * level.regainFactor

    /** Mistakes cost more the longer the run (and on a higher level): what the pack gains from one is multiplied by this. */
    fun penaltyFactor(elapsed: Float): Float = lerp(1f, PRESSURE_PENALTY_FACTOR, pressure(elapsed)) * level.penaltyFactor
}
