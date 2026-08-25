package org.lerchenflo.schneaggchatv3mp.games.presentation.stanislaus

import kotlin.math.sin

// World-space tuning constants. y = 0 is the water surface, y > 0 is depth underwater,
// y < 0 is height in the air. x grows from the far shore (0) towards Stanislaus.
const val WORLD_WIDTH = 200f
const val AIR_HEIGHT = 40f
const val WATER_DEPTH = 70f
const val POND_LEFT = 6f
const val POND_RIGHT = 150f

/** Stanislaus stands at this x; his eye/hand are above the water at these offsets. */
const val STANI_X = 178f
const val EYE_HEIGHT = 26f
const val HAND_X = 168f
const val HAND_Y = -14f

const val CATCH_RADIUS = 6f
const val REVEAL_DURATION_MILLIS = 1200L
const val MISS_TIME_PENALTY_MILLIS = 2000L
const val AIR_SPEED = 220f
const val WATER_SPEED = 70f
const val BASE_CATCH_SCORE = 100

enum class ThrowResult { HIT, MISS }

data class FishState(
    val x: Float,
    val baseDepth: Float,
    val directionX: Float,
    val speed: Float,
    val bobPhase: Float,
    /** Per-difficulty: 0f keeps the fish on a straight line, >0f gives it a gentle up/down bob. */
    val bobAmplitude: Float,
) {
    /** Actual current depth including the up/down bob (if any), clamped inside the pond. */
    val depth: Float
        get() = (baseDepth + sin(bobPhase) * bobAmplitude).coerceIn(2f, WATER_DEPTH - 2f)
}

data class Spear(
    val x: Float,
    val y: Float,
    val directionX: Float,
    val directionY: Float,
)

data class ThrowReveal(
    val sighting: Sighting,
    val fishX: Float,
    val fishDepth: Float,
    val result: ThrowResult,
)

data class StanislausState(
    val isPlaying: Boolean = false,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val score: Int = 0,
    val streak: Int = 0,
    val timeRemainingMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    val fish: FishState? = null,
    val spear: Spear? = null,
    val reveal: ThrowReveal? = null,
    /** Live sighting of the current fish, recomputed every tick; drives the ghost fish's position/alpha. */
    val currentSighting: Sighting? = null,
    /** LOW difficulty keeps the real ray/fish faintly visible at all times, not just after a throw. */
    val liveHint: Boolean = false,
) {
    /** Aiming is only allowed while nothing is in flight and no reveal is being shown. */
    val canAim: Boolean get() = isPlaying && !isPaused && spear == null && reveal == null
}

sealed interface StanislausAction {
    data object StartGame : StanislausAction
    data object StopGame : StanislausAction
    data object RestartGame : StanislausAction
    data object TogglePause : StanislausAction
    data class Throw(val directionX: Float, val directionY: Float) : StanislausAction
}
