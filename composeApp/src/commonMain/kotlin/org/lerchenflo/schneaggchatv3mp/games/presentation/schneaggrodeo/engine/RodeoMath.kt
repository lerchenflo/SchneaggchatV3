package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// Small easing helpers used all over the engine and the vehicles.

internal fun lerp(start: Float, end: Float, fraction: Float) = start + (end - start) * fraction

/** Eases 0..1 in and out (slow start, slow end); the standard for anything sliding into place. */
internal fun smoothstep(progress: Float) = progress * progress * (3f - 2f * progress)

/** Overshoots slightly past 1 before settling - gives a springy pop. */
internal fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val t = x - 1f
    return 1f + (c1 + 1f) * t * t * t + c1 * t * t
}

/**
 * Height of a hop from [from] to [to] at [progress] (0..1): eases between the two and adds an arc
 * of [hop] units on top. Used for the horse hopping onto and off vehicles.
 */
internal fun hopArc(from: Float, to: Float, progress: Float, hop: Float) =
    lerp(from, to, smoothstep(progress)) + hop * sin(PI.toFloat() * progress)

/** Fraction 0..1 of [seconds] that [time] has reached. */
internal fun progressOf(time: Float, seconds: Float) = min(1f, time / seconds)

/** A timer that counts down to zero and stays there. */
internal fun countDown(value: Float, dt: Float) = max(0f, value - dt)
