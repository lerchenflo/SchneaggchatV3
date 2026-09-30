package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoDeepThingUi
import kotlin.random.Random

// Life in the deep water of the open sea, below the surface the horse swims on: schools of fish,
// jellyfish, sharks, now and then a whale, and old shipwrecks on the seabed. Only to look at - the
// U-boat dives down among them (see vehicles/uboat). The view pans down in the sea so it shows.

/** How far the view is panned down in the sea (see RodeoVehicle.camera), so the deep water shows. */
internal const val SEA_CAMERA_DOWN = -20f

/** Units of scrolled ground between two things coming along, at least and at random on top. */
private const val GAP_MIN = 25f
private const val GAP_RANDOM = 35f

enum class RodeoDeepKind { FISH, JELLYFISH, SHARK, WHALE, WRECK }

/** Something in the deep; [x] moves with the ground plus its own [swim] (u/s to the left). */
private class DeepThing(var x: Float, val y: Float, val kind: RodeoDeepKind, val swim: Float, val seed: Int)

internal class RodeoDeepSea {
    private val things = mutableListOf<DeepThing>()
    private var nextIn = 0f
    private var time = 0f

    fun reset() {
        things.clear()
        nextIn = 0f
    }

    /** Moves everything by [scroll] and brings new things in at the right edge while [active] (in the sea). */
    fun step(dt: Float, scroll: Float, worldWidth: Float, active: Boolean) {
        time += dt
        things.forEach { it.x -= scroll + it.swim * dt }
        things.removeAll { it.x < -60f || it.x > worldWidth + 120f }
        if (!active) {
            things.clear()
            return
        }
        nextIn -= scroll
        if (nextIn > 0f) return
        nextIn = GAP_MIN + Random.nextFloat() * GAP_RANDOM
        val roll = Random.nextFloat()
        val kind = when {
            roll < 0.38f -> RodeoDeepKind.FISH
            roll < 0.62f -> RodeoDeepKind.JELLYFISH
            roll < 0.8f -> RodeoDeepKind.SHARK
            roll < 0.9f && things.none { it.kind == RodeoDeepKind.WHALE } -> RodeoDeepKind.WHALE
            else -> RodeoDeepKind.WRECK
        }
        val (y, swim) = when (kind) {
            RodeoDeepKind.FISH -> -6f - Random.nextFloat() * 18f to 6f + Random.nextFloat() * 6f
            RodeoDeepKind.JELLYFISH -> -5f - Random.nextFloat() * 16f to -1f
            RodeoDeepKind.SHARK -> -10f - Random.nextFloat() * 14f to 8f
            RodeoDeepKind.WHALE -> -19f to -4f
            RodeoDeepKind.WRECK -> SEABED_Y to 0f
        }
        things += DeepThing(x = worldWidth + 40f, y = y, kind = kind, swim = swim, seed = Random.nextInt(1000))
    }

    fun ui(): List<RodeoDeepThingUi> = things.map { RodeoDeepThingUi(it.x, it.y, it.kind, it.seed, time) }
}
