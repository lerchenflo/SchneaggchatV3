package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoBridgeUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoTerrainUi
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// The ground's height along the track: flat stretches take turns with stretches of gentle rolling
// hills, which get a little steeper over the run. Everything on the track keeps its height above
// the ground right under it - fences, snails, jumps and collisions work exactly as on the flat - and
// only the drawing lifts it onto the hills (see RodeoDrawContext.p). The camera follows the ground
// under the horse, so the horse stays at the same height on screen.
//
// The ways to the other maps shape the ground too (see [TerrainFeature]): the mine or cave shaft
// lies flat at the foot of a hill, the beach runs down into the sea, and ramps lead back up.
//
// Now and then a hilly stretch starts with a gorge instead, a river at its bottom and a plank
// bridge on ropes across it, like in Hill Climb Racing (see [TerrainBridge]). Its sag is part of
// the ground's height, so everything crossing it rides along; the planks bend a little more under
// the horse and bounce back. No fences, mud or stops come on it.
//
// The hills are kept gentle enough for every vehicle to drive over them, tilting as one piece (see
// RodeoFootprint); the fast and the long ones (lawn tractor, plane, train, traffic jam) only come on
// the flat, and the ground stays flat while they are around.

/** Units between two control points of the ground; the ground eases smoothly from one to the next. */
private const val STEP = 70f
/** Height change between two control points of a hill, at the start of a run and at its steepest. */
private const val HILL_RISE_START = 6f
private const val HILL_RISE_MAX = 13f
/** Game seconds until the hills are at their steepest. */
private const val HILL_RAMP_SECONDS = 300f
/** Length of the flat and the hilly stretches (units). */
private const val FLAT_MIN = 500f
private const val FLAT_RANDOM = 400f
private const val HILLS_MIN = 600f
private const val HILLS_RANDOM = 500f
/** The ground wanders between these heights, so it never drifts off. */
private const val LOWEST = -20f
private const val HIGHEST = 30f

/** Units between two control points; features are placed at least this far beyond the picture. */
internal const val TERRAIN_STEP = STEP

/** Share of hilly stretches that start with a gorge and its bridge. */
private const val BRIDGE_CHANCE = 0.2f
/** Length of the bridge, and the flat bank before it (both units). */
internal const val BRIDGE_LENGTH = 120f
private const val BRIDGE_BANK = 1.5f * STEP
/** Flat ground around the gorge: the bank, the bridge and some ground after it. */
private val BRIDGE_SHAPE = List(6) { 0f }
/** How far the bridge sags in the middle on its own, and how much more under the horse (units). */
private const val BRIDGE_SAG = 2.5f
internal const val BRIDGE_LOAD_DIP = 3.5f
/** The planks spring back under the horse: stiffness (1/s²) and damping (1/s). */
private const val BRIDGE_SPRING = 140f
private const val BRIDGE_DAMPING = 7f

/**
 * How far a bridge starting at [from] sags at [x]: a gentle rope curve, plus [dip] under a load at
 * [loadAt] (0..1 along it) that fades out towards the ends, like a rope bridge under a weight.
 */
internal fun bridgeSagAt(x: Float, from: Float, dip: Float, loadAt: Float): Float {
    val t = (x - from) / BRIDGE_LENGTH
    if (t <= 0f || t >= 1f) return 0f
    val load = if (t < loadAt) t / loadAt else (1f - t) / (1f - loadAt)
    return BRIDGE_SAG * sin(PI.toFloat() * t) + dip * 4f * loadAt * (1f - loadAt) * load
}

/** A plank bridge over a gorge, from [x] to x + BRIDGE_LENGTH; [dip] bounces under the horse. */
internal class TerrainBridge(override var x: Float) : OnTrack {
    var dip = 0f
    private var dipSpeed = 0f
    /** Where the load stands, 0..1 along the bridge. */
    var loadAt = 0.5f
        private set

    val end: Float get() = x + BRIDGE_LENGTH

    /** Lets the planks spring towards bearing a load at world x [load] (null: nothing on them). */
    fun bear(dt: Float, load: Float?) {
        val on = load?.takeIf { it > x && it < end }
        if (on != null) loadAt = ((on - x) / BRIDGE_LENGTH).coerceIn(0.02f, 0.98f)
        val target = if (on != null) BRIDGE_LOAD_DIP else 0f
        dipSpeed += (BRIDGE_SPRING * (target - dip) - BRIDGE_DAMPING * dipSpeed) * dt
        dip += dipSpeed * dt
    }

    fun sagAt(x: Float): Float = bridgeSagAt(x, this.x, dip, loadAt)
}

/**
 * The shapes the ways between the maps give the ground: height offsets of the control points, one
 * step apart, starting two steps before the feature's x (see [RodeoTerrain.addFeature]).
 */
internal enum class TerrainFeature(val shape: List<Float>) {
    /** Flat around the shaft at x, then a hill going up: jump the shaft to stay on the main track. */
    SHAFT(listOf(0f, 0f, 0f, 0f, 9f, 18f, 24f)),
    /** Down a beach into the sea, which begins at x. */
    BEACH(listOf(0f, -6f, -12f, -14f, -14f, -14f)),
    /** A ramp up that ends at x: out of the mine, the cave or the sea. */
    RAMP_UP(listOf(0f, 9f, 18f, 18f, 18f)),
}

/** Height of the ground at [x] on a profile of control points ([xs] ascending) eased with a cosine. */
internal fun terrainHeightAt(xs: List<Float>, hs: List<Float>, x: Float): Float {
    if (xs.isEmpty()) return 0f
    if (x <= xs.first()) return hs.first()
    for (i in 1 until xs.size) {
        if (x <= xs[i]) {
            val t = (x - xs[i - 1]) / (xs[i] - xs[i - 1])
            val eased = (1f - cos(PI.toFloat() * t)) / 2f
            return hs[i - 1] + (hs[i] - hs[i - 1]) * eased
        }
    }
    return hs.last()
}

/** Rise per unit at [x] on the same profile as [terrainHeightAt]: positive uphill. */
internal fun terrainSlopeAt(xs: List<Float>, hs: List<Float>, x: Float): Float =
    (terrainHeightAt(xs, hs, x + 1f) - terrainHeightAt(xs, hs, x - 1f)) / 2f

internal class RodeoTerrain {
    private val xs = mutableListOf<Float>()
    private val hs = mutableListOf<Float>()
    private var hilly = false
    /** Units until the current stretch ends. */
    private var stretchLeft = FLAT_MIN
    /** Next hill goes up (or down). */
    private var up = true
    /** A shape still ahead: control points from [shapeStart] on follow [shape] (height offsets per point). */
    private var shape: List<Float> = emptyList()
    private var shapeStart = 0f
    private var shapeBase = 0f
    val bridges = mutableListOf<TerrainBridge>()

    fun reset() {
        xs.clear()
        hs.clear()
        hilly = false
        stretchLeft = FLAT_MIN
        up = true
        shape = emptyList()
        bridges.clear()
    }

    /** A gorge is coming or in the picture: other shapes of the ground wait. */
    val hasBridge: Boolean get() = bridges.isNotEmpty()

    /** Some bridge spans part of [from]..[to]. */
    fun bridgeWithin(from: Float, to: Float): Boolean = bridges.any { it.x < to && it.end > from }

    /**
     * Shapes the ground ahead for [feature], just beyond the ground laid out so far (out of the
     * picture), and returns where it is (see [TerrainFeature]). The ground before it is not touched.
     */
    fun addFeature(feature: TerrainFeature): Float {
        val x = (xs.lastOrNull() ?: 0f) + 2f * STEP
        shape = feature.shape
        shapeStart = x - 2f * STEP
        shapeBase = hs.lastOrNull() ?: 0f
        return x
    }

    /** Height of the ground at [x], down a sagging bridge's planks. */
    fun heightAt(x: Float): Float {
        var height = baseHeightAt(x)
        for (bridge in bridges) height -= bridge.sagAt(x)
        return height
    }

    /** Height at [x] without the bridges' sag: the gorge's banks. */
    fun baseHeightAt(x: Float): Float = terrainHeightAt(xs, hs, x)

    /** Lets the bridges' planks bend under a load at world x [load] (null: nothing on them). */
    fun bearBridges(dt: Float, load: Float?) = bridges.forEach { it.bear(dt, load) }

    /** Rise per unit at [x], sag left out: positive uphill. */
    fun slopeAt(x: Float): Float = terrainSlopeAt(xs, hs, x)

    /** The ground from [from] to [to] is flat (no gorge either). */
    fun isFlat(from: Float, to: Float): Boolean {
        if (bridgeWithin(from - STEP, to + STEP)) return false
        val base = baseHeightAt(from)
        for (i in xs.indices) {
            if (xs[i] >= from - STEP && xs[i] <= to + STEP && abs(hs[i] - base) > 0.01f) return false
        }
        return true
    }

    /**
     * Moves the ground by [scroll] and adds new control points on the right up to [worldWidth]. New
     * ground is flat while [keepFlat]; hills grow with [elapsed] game seconds.
     */
    fun scroll(scroll: Float, worldWidth: Float, elapsed: Float, keepFlat: Boolean) {
        if (xs.isEmpty()) {
            xs += -STEP
            hs += 0f
        }
        for (i in xs.indices) xs[i] -= scroll
        // Keep one point left of the picture
        while (xs.size > 2 && xs[1] < 0f) {
            xs.removeAt(0)
            hs.removeAt(0)
        }
        if (shape.isNotEmpty()) shapeStart -= scroll
        bridges.scrollAlong(scroll) { it.end < 0f }
        while (xs.last() < worldWidth + STEP) {
            val x = xs.last() + STEP
            xs += x
            hs += shapedHeight(x) ?: nextHeight(x, elapsed, keepFlat)
        }
    }

    /** Height of a new control point at [x] inside the shape ahead, or null outside of it. */
    private fun shapedHeight(x: Float): Float? {
        if (shape.isEmpty() || x < shapeStart) return null
        val index = ((x - shapeStart) / STEP).roundToInt()
        if (index >= shape.size) {
            // Past the shape: the ground carries on from its last height, flat for a while - past a
            // gorge the hills it opened come
            val last = shapeBase + shape.last()
            hilly = shape === BRIDGE_SHAPE
            stretchLeft = if (hilly) HILLS_MIN else FLAT_MIN
            shape = emptyList()
            return last
        }
        return shapeBase + shape[index]
    }

    private fun nextHeight(x: Float, elapsed: Float, keepFlat: Boolean): Float {
        val last = hs.last()
        stretchLeft -= STEP
        if (stretchLeft <= 0f) {
            hilly = !hilly
            stretchLeft = if (hilly) HILLS_MIN + Random.nextFloat() * HILLS_RANDOM else FLAT_MIN + Random.nextFloat() * FLAT_RANDOM
            // Now and then the hills start with a gorge: flat banks around a bridge
            if (hilly && !keepFlat && bridges.isEmpty() && Random.nextFloat() < BRIDGE_CHANCE) {
                shape = BRIDGE_SHAPE
                shapeStart = x - STEP
                shapeBase = last
                bridges += TerrainBridge(x = shapeStart + BRIDGE_BANK)
                return last
            }
        }
        if (keepFlat || !hilly) return last
        val maxRise = HILL_RISE_START + (HILL_RISE_MAX - HILL_RISE_START) * min(1f, elapsed / HILL_RAMP_SECONDS)
        val rise = maxRise * (0.5f + 0.5f * Random.nextFloat())
        // Up and down in turns, turning back before it wanders off
        if (last + rise > HIGHEST) up = false
        if (last - rise < LOWEST) up = true
        val next = if (up) last + rise else last - rise
        if (Random.nextFloat() < 0.7f) up = !up
        return next
    }

    fun ui(): RodeoTerrainUi = RodeoTerrainUi(
        xs = xs.toList(),
        hs = hs.toList(),
        bridges = bridges.map { RodeoBridgeUi(x = it.x, dip = it.dip, loadAt = it.loadAt) },
    )
}
