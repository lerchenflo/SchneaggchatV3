package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFireflyUi
import kotlin.math.sin
import kotlin.random.Random

// Wonders that come with the run's flavor (see RodeoRunFlavor):
//  - a rainbow once the rain stops: ride under it and every point of the ride counts double for a while
//  - fireflies on night runs: they drift over the track, the horse catches them passing through
//  - the ghost horse (see RodeoWildHorses), only on night runs: lassoed, it turns horse and rider
//    ghostly and nothing on the track can hurt them for a while

/** Half the width of the rainbow's arch at its feet; its top is this high too. */
internal const val RAINBOW_HALF_WIDTH = 34f
private const val DOUBLE_POINTS_SECONDS = 10f
private const val GHOST_SECONDS = 10f
private const val FIREFLY_FIRST_GAP = 10f
private const val FIREFLY_MIN_GAP = 14f
private const val FIREFLY_GAP_RANDOM = 26f
/** Heights they drift at, all within reach of horse and rider. */
private const val FIREFLY_MIN_HEIGHT = 14f
private const val FIREFLY_HEIGHT_RANGE = 14f
private const val FIREFLY_BOB = 2f

/** A firefly drifting over the track; [x] moves with the ground. */
internal class Firefly(var x: Float, private val baseHeight: Float, var phase: Float) {
    val height: Float get() = baseHeight + FIREFLY_BOB * sin(phase)
}

internal class RodeoSkyWonders {
    /** Middle of the rainbow's feet, or null while there is none. */
    var rainbowX: Float? = null
        private set
    private var rainbowRidden = false
    /** Seconds the points still count double. */
    var doublePointsSeconds = 0f
        private set
    /** Seconds horse and rider are still ghostly. */
    var ghostSeconds = 0f
        private set
    val fireflies = mutableListOf<Firefly>()
    private var nextFireflyIn = FIREFLY_FIRST_GAP
    /** Points of the ride not yet doubled, below one whole point. */
    private var doubleFraction = 0f

    val isGhost: Boolean get() = ghostSeconds > 0f

    fun reset() {
        rainbowX = null
        rainbowRidden = false
        doublePointsSeconds = 0f
        ghostSeconds = 0f
        fireflies.clear()
        nextFireflyIn = FIREFLY_FIRST_GAP
        doubleFraction = 0f
    }

    /** The rain stopped: a rainbow comes in from the right. */
    fun sendRainbow(worldWidth: Float) {
        rainbowX = worldWidth + RAINBOW_HALF_WIDTH
        rainbowRidden = false
    }

    /** The ghost horse was lassoed. */
    fun startGhost() {
        ghostSeconds = GHOST_SECONDS
    }

    /** Clears everything tied to the map, switching to another one. */
    fun clearForNewMap() {
        rainbowX = null
        fireflies.clear()
    }

    /**
     * Moves everything with the ground by [scroll] and counts the timers down; fireflies come along
     * while [firefliesAllowed]. Returns the extra points of [ridden] while they count double.
     */
    fun step(dt: Float, scroll: Float, ridden: Float, worldWidth: Float, firefliesAllowed: Boolean): Int {
        ghostSeconds = countDown(ghostSeconds, dt)

        rainbowX = rainbowX?.minus(scroll)?.takeIf { it + RAINBOW_HALF_WIDTH > 0f }
        val rainbow = rainbowX
        if (rainbow != null && !rainbowRidden && rainbow < HORSE_X + 15f) {
            rainbowRidden = true
            doublePointsSeconds = DOUBLE_POINTS_SECONDS
        }

        var extra = 0
        if (doublePointsSeconds > 0f) {
            doublePointsSeconds = countDown(doublePointsSeconds, dt)
            doubleFraction += ridden / UNITS_PER_POINT
            extra = doubleFraction.toInt()
            doubleFraction -= extra
        }

        fireflies.forEach {
            it.x -= scroll
            it.phase += dt * 3f
        }
        fireflies.removeAll { it.x < -2f }
        if (firefliesAllowed) {
            nextFireflyIn -= scroll
            if (nextFireflyIn <= 0f) {
                nextFireflyIn = FIREFLY_MIN_GAP + Random.nextFloat() * FIREFLY_GAP_RANDOM
                fireflies += Firefly(
                    x = worldWidth + 2f,
                    baseHeight = FIREFLY_MIN_HEIGHT + Random.nextFloat() * FIREFLY_HEIGHT_RANGE,
                    phase = Random.nextFloat() * 6f,
                )
            }
        }
        return extra
    }

    /** Fireflies between [left] and [right] and between [bottom] and [top] are caught; each one goes to [onCaught]. */
    fun catchFireflies(left: Float, right: Float, bottom: Float, top: Float, onCaught: (Firefly) -> Unit) {
        fireflies.removeAll { firefly ->
            val caught = firefly.x in left..right && firefly.height in bottom..top
            if (caught) onCaught(firefly)
            caught
        }
    }

    fun fireflyUis(): List<RodeoFireflyUi> = fireflies.map {
        RodeoFireflyUi(x = it.x, y = it.height, glow = 0.6f + 0.4f * sin(it.phase * 2.3f))
    }
}
