package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

// Super jump: one charge per SNAILS_PER_SUPER_JUMP caught snails. A huge leap over the next
// SUPER_JUMP_FENCES fences at full pace, preceded by a short wind-up in which the horse rears up on
// its growing hind legs. The view pans up to follow the horse into the sky.
internal const val SNAILS_PER_SUPER_JUMP = 5
private const val SUPER_JUMP_FENCES = 5
private const val SUPER_JUMP_HEIGHT = 93f           // peak height; the camera follows above ~30
internal const val SUPER_JUMP_WINDUP_SECONDS = 0.35f
/** The wind-up is cut short when a fence gets this close (in riding time), so the horse never runs through it. */
private const val SUPER_JUMP_EARLY_LAUNCH_SECONDS = 0.08f
private const val SUPER_JUMP_ROW_GAP_SECONDS = 0.4f // gap between the pre-placed fences, in riding time
private const val SUPER_JUMP_LANDING_MARGIN = 6f
/** Exponent < 1 turns the sine arc into a steep takeoff, long float and steep landing. */
private const val SUPER_JUMP_ARC_SHAPE = 0.6f

internal enum class SuperJumpPhase { NONE, WINDUP, FLIGHT }

/** The super jump's state machine: NONE -> (queued while in the air) -> WINDUP -> FLIGHT -> NONE. */
internal class RodeoSuperJump {
    var phase = SuperJumpPhase.NONE
        private set
    /** Fired in the air: starts as soon as the horse lands. */
    var queued = false
        private set
    /** Real seconds into the wind-up. */
    var windup = 0f
        private set
    private var flightDistance = 0f
    private var travelled = 0f
    private var lastFence: Fence? = null

    val isActive: Boolean get() = phase != SuperJumpPhase.NONE
    /** Active or about to start - nothing else (vehicles, the rocket, falling off) may start meanwhile. */
    val isBusy: Boolean get() = isActive || queued

    /** 0..1 through the wind-up. */
    val windupProgress: Float get() = progressOf(windup, SUPER_JUMP_WINDUP_SECONDS)
    /** 0..1 through the flight. */
    val flightProgress: Float get() = progressOf(travelled, flightDistance)

    fun reset() {
        phase = SuperJumpPhase.NONE
        queued = false
        lastFence = null
    }

    fun queue() {
        queued = true
    }

    fun cancelQueue() {
        queued = false
    }

    /** Rears up; the fences for the leap are laid out on [course] right away. */
    fun start(course: RodeoCourse, worldWidth: Float, speed: Float, elapsed: Float) {
        queued = false
        phase = SuperJumpPhase.WINDUP
        windup = 0f
        lastFence = course.prepareFenceRow(SUPER_JUMP_FENCES, SUPER_JUMP_ROW_GAP_SECONDS, worldWidth, speed, elapsed)
    }

    /**
     * Moves the super jump on: [dt] of wind-up, or [ridden] distance of flight, which sets the
     * horse's height. Returns true in the frame the horse lands.
     */
    fun step(dt: Float, ridden: Float, horse: RodeoHorse, fences: List<Fence>): Boolean {
        when (phase) {
            SuperJumpPhase.NONE -> Unit
            SuperJumpPhase.WINDUP -> {
                windup += dt
                // Takes off early rather than trotting through a fence that is already close
                val hitRight = HORSE_X + HITBOX_RIGHT
                val fenceClose = fences.any {
                    !it.knocked && it.x + it.width > hitRight && it.x - hitRight < horse.speed * SUPER_JUMP_EARLY_LAUNCH_SECONDS
                }
                if (windup >= SUPER_JUMP_WINDUP_SECONDS || fenceClose) launch(horse.speed)
            }
            SuperJumpPhase.FLIGHT -> {
                // Height follows the planned arc over the distance, so the landing is always right
                // behind the last fence of the row - no matter the speed.
                travelled += ridden
                val progress = flightProgress
                horse.height = SUPER_JUMP_HEIGHT * sin(PI.toFloat() * progress).coerceAtLeast(0f).pow(SUPER_JUMP_ARC_SHAPE)
                if (progress >= 1f) {
                    horse.height = 0f
                    phase = SuperJumpPhase.NONE
                    lastFence = null
                    return true
                }
            }
        }
        return false
    }

    private fun launch(speed: Float) {
        val hitLeft = HORSE_X + HITBOX_LEFT
        val landingX = lastFence?.let { it.x + it.width } ?: (hitLeft + speed)
        flightDistance = max(speed, landingX - hitLeft + SUPER_JUMP_LANDING_MARGIN)
        travelled = 0f
        phase = SuperJumpPhase.FLIGHT
    }
}
