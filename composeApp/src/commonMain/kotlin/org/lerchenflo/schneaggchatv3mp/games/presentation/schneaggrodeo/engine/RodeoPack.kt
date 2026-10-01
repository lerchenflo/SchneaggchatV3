package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoRavenUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// The chasing ravens: crashes let them close in, riding clean (and catching snails) lets the horse
// pull away again. Once they reach the horse they swoop down from the sky onto the cowboy, peck
// something off him (see RodeoCowboyPart) and fly back up, falling far behind again. Once he has
// nothing left to lose they feast on him where he lies (see RodeoDeath).
internal const val CHASE_GAP_MAX = 45f     // distance of the ravens behind the horse; starts here (off screen above ~26)
private const val PACK_SIZE = 3
private const val PACK_SPACING = 8f
/** Half a raven's length, beak to tail; its beak reaching the horse catches it. */
private const val RAVEN_HALF_LENGTH = 5f
/** How high each raven flies above the ground. */
private val RAVEN_HEIGHTS = floatArrayOf(22f, 28f, 17f)
// The attack: swooping up from behind the horse and dropping onto the cowboy from above, pecking,
// flying back up and away
private const val DIVE_SECONDS = 0.8f
private const val PECK_SECONDS = 0.6f
private const val RISE_SECONDS = 0.9f
private const val ATTACK_SECONDS = DIVE_SECONDS + PECK_SECONDS + RISE_SECONDS
/** After an attack the ravens are this far behind again, off screen. */
private const val RETREAT_GAP = 30f
/** Where each raven lands round the cowboy's head while pecking: x and y offsets. */
private val PECK_X = floatArrayOf(-3.5f, 1f, 4f)
private val PECK_Y = floatArrayOf(1.5f, 4f, 0f)
/** How high above the line from behind the horse to the cowboy's head the swoop goes. */
private const val SWOOP_HEIGHT = 22f
/** Carrying the cowboy off: where each raven holds him, round the middle of the flock. */
private val HOLD_X = floatArrayOf(-3f, 0.5f, 3.5f)
private val HOLD_Y = floatArrayOf(0f, 1.6f, -0.4f)
/** How far up and back they fly off. */
private const val RISE_HEIGHT = 40f
private const val RISE_BACK = 30f
/** Advance of [RodeoPack.clock] per second. */
private const val CLOCK_SPEED = 10f

internal class RodeoPack {
    /** Distance between the horse and the first raven. */
    var gap = CHASE_GAP_MAX
        private set
    /**
     * Animation clock of the flapping ravens; also drives the wobble of the snails on the track and
     * the bobbing horseshoes, so everything on the track moves in the same rhythm.
     */
    var clock = 0f
        private set

    /** Seconds into the current attack, negative while there is none. */
    private var attackTime = -1f

    val isAttacking: Boolean get() = attackTime >= 0f

    fun reset() {
        gap = CHASE_GAP_MAX
        clock = 0f
        attackTime = -1f
    }

    /** The cowboy is done for: no more attacks, they take him away (see [flockUi]). */
    fun takeAway() {
        attackTime = -1f
        gap = 0f
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

    /**
     * Starts an attack once the ravens reached the horse and moves it on by [dt]; true in the frame
     * they peck the cowboy. After it they are far behind again.
     */
    fun stepAttack(dt: Float): Boolean {
        if (!isAttacking) {
            if (gap > 0f) return false
            attackTime = 0f
        }
        val before = attackTime
        attackTime += dt
        if (attackTime >= ATTACK_SECONDS) {
            attackTime = -1f
            gap = max(gap, RETREAT_GAP)
            return false
        }
        return before < DIVE_SECONDS && attackTime >= DIVE_SECONDS
    }

    /** Riding on: the pack falls back by [rate] u/s (or creeps in, when negative). */
    fun ride(dt: Float, rate: Float) {
        gap = min(CHASE_GAP_MAX, gap + rate * dt)
        if (gap <= 0f) gap = 0f
    }

    /**
     * The ravens flapping along behind the horse, rising over the fences (off screen while the gap
     * is large). In an attack they swoop onto the cowboy's head at [headX] / [headY].
     */
    fun ui(fences: List<Fence>, headX: Float, headY: Float): List<RodeoRavenUi> =
        if (isAttacking) attackUi(headX, headY) else chaseUi(fences)

    private fun attackUi(headX: Float, headY: Float): List<RodeoRavenUi> = (0 until PACK_SIZE).map { index ->
        val peckX = headX + PECK_X[index]
        val peckY = headY + PECK_Y[index]
        // A little behind one another, so they don't arrive all at once
        val time = attackTime - index * 0.08f
        when {
            time < DIVE_SECONDS -> {
                // From where they caught up behind the horse, up in an arc and down onto him
                val progress = smoothstep(progressOf(max(0f, time), DIVE_SECONDS))
                val arc = sin(PI.toFloat() * progress)
                RodeoRavenUi(
                    x = lerp(chaseX(index, gap = 0f), peckX, progress),
                    height = lerp(RAVEN_HEIGHTS[index], peckY, progress) + SWOOP_HEIGHT * arc,
                    // Beating hard on the way up, wings folded back for the drop
                    flap = if (progress < 0.5f) sin(clock * 2.6f + index * 2.1f) else 0.9f,
                    tiltDeg = -35f * cos(PI.toFloat() * progress),
                )
            }
            time < DIVE_SECONDS + PECK_SECONDS -> {
                // Flapping in place and jabbing down with the beak
                val jab = sin(clock * 3f + index * 1.7f)
                RodeoRavenUi(
                    x = peckX + 0.6f * jab,
                    height = peckY + 0.5f * sin(clock * 2f + index),
                    flap = sin(clock * 2.6f + index * 2.1f),
                    tiltDeg = 20f + 15f * jab,
                )
            }
            else -> {
                val progress = smoothstep(progressOf(time - DIVE_SECONDS - PECK_SECONDS, RISE_SECONDS))
                RodeoRavenUi(
                    x = peckX - RISE_BACK * progress,
                    height = peckY + RISE_HEIGHT * progress,
                    flap = sin(clock * 2f + index * 2.1f),
                    // Nose up, climbing away
                    tiltDeg = -25f * progress,
                    facingLeft = true,
                )
            }
        }
    }

    /**
     * The ravens taking the cowboy away, round [flockX] / [flockY]: flapping hard and close together
     * while they [hold] him, hovering a little more loosely while they don't.
     */
    fun flockUi(flockX: Float, flockY: Float, hold: Boolean): List<RodeoRavenUi> = (0 until PACK_SIZE).map { index ->
        val spread = if (hold) 1f else 1.6f
        RodeoRavenUi(
            x = flockX + HOLD_X[index] * spread + (if (hold) 0.3f else 1.2f) * sin(clock * 0.9f + index * 2f),
            height = flockY + HOLD_Y[index] * spread + 0.6f * sin(clock * 1.3f + index),
            flap = sin(clock * (if (hold) 3.4f else 2.4f) + index * 2.1f),
            tiltDeg = if (hold) -15f + 8f * sin(clock * 1.7f + index) else 10f * sin(clock * 0.5f + index),
            facingLeft = index == PACK_SIZE - 1,
        )
    }

    private fun chaseUi(fences: List<Fence>): List<RodeoRavenUi> = (0 until PACK_SIZE).mapNotNull { index ->
        val x = chaseX(index, gap)
        if (x > -2f * RAVEN_HALF_LENGTH) {
            val bob = sin(clock * 0.5f + index * 1.3f) * 1.5f
            val height = max(RAVEN_HEIGHTS[index] + bob, hopOverFences(x, fences, reach = 8f, clearance = 4f))
            RodeoRavenUi(
                x = x,
                height = height,
                flap = sin(clock * 1.6f + index * 2.1f),
                tiltDeg = sin(clock * 0.4f + index) * 5f,
            )
        } else {
            null
        }
    }

    /** Where raven [index] flies while chasing [gap] behind the horse. */
    private fun chaseX(index: Int, gap: Float) = HORSE_X + HITBOX_LEFT - RAVEN_HALF_LENGTH - gap - index * PACK_SPACING
}
