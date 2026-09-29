package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import kotlin.math.min

// Magic mushrooms grow on the ground, mostly in the forest. Galloping over one (hooves on the
// ground) eats it, and for a few seconds one of these happens at random:
//  - GIANT: the horse grows and tramples every fence and runner without a crash
//  - TINY: the horse shrinks and jumps much higher
//  - SLOW_MOTION: the whole world runs at half speed
// Jumping over a mushroom leaves it be.

private const val MUSHROOM_EFFECT_SECONDS = 6f
/** Seconds the horse takes to grow or shrink, at the start and at the end of the effect. */
private const val MUSHROOM_MORPH_SECONDS = 0.4f
private const val GIANT_SCALE = 1.6f
private const val TINY_SCALE = 0.6f
private const val TINY_JUMP_BOOST = 1.35f
private const val SLOW_MOTION_FACTOR = 0.5f
internal const val MUSHROOM_POINTS = 10
/** Width of a mushroom on the ground, for eating it. */
internal const val MUSHROOM_HALF_WIDTH = 1.5f

internal enum class MushroomEffect { GIANT, TINY, SLOW_MOTION }

/** The effect of the last mushroom eaten, while it lasts. */
internal class RodeoMushroomTrip {
    var effect: MushroomEffect? = null
        private set
    /** Real seconds left. */
    private var left = 0f

    fun reset() {
        effect = null
        left = 0f
    }

    /** A mushroom was eaten: a random effect starts (or replaces the current one). */
    fun start() {
        effect = MushroomEffect.entries.random()
        left = MUSHROOM_EFFECT_SECONDS
    }

    fun tick(realDt: Float) {
        if (effect == null) return
        left -= realDt
        if (left <= 0f) reset()
    }

    /** 0..1 how far the horse has grown or shrunk: eases in at the start, out at the end. */
    private val morph: Float
        get() = if (effect == null) 0f else min(1f, min(MUSHROOM_EFFECT_SECONDS - left, left) / MUSHROOM_MORPH_SECONDS)

    /** Size of the horse (and rider) drawn. */
    val horseScale: Float
        get() = when (effect) {
            MushroomEffect.GIANT -> lerp(1f, GIANT_SCALE, morph)
            MushroomEffect.TINY -> lerp(1f, TINY_SCALE, morph)
            else -> 1f
        }

    /** Tramples fences and runners instead of crashing into them. */
    val tramples: Boolean get() = effect == MushroomEffect.GIANT

    val jumpBoost: Float get() = if (effect == MushroomEffect.TINY) TINY_JUMP_BOOST else 1f

    /** Share of real time the world moves on by. */
    val timeFactor: Float get() = if (effect == MushroomEffect.SLOW_MOTION) SLOW_MOTION_FACTOR else 1f
}
