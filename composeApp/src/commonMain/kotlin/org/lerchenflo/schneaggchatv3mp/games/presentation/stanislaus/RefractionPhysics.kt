package org.lerchenflo.schneaggchatv3mp.games.presentation.stanislaus

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Refractive index of water relative to air. */
const val WATER_REFRACTIVE_INDEX = 1.33

/** A plain 2D point/vector — kept Compose-free so this file has no UI dependencies. */
data class Vec2(val x: Float, val y: Float)

/**
 * Where the light ray from a fish actually crosses the water surface on its way to an eye
 * above the water, and where the fish therefore *appears* to be from that eye's point of view.
 *
 * World space convention used throughout: y = 0 is the water surface, y > 0 is underwater
 * depth, y < 0 is height in the air.
 */
data class Sighting(
    /** x where the light ray crosses the surface. */
    val surfaceX: Float,
    /** Angle of the ray in water, from the surface normal (radians). */
    val angleInWater: Float,
    /** Angle of the ray in air, from the surface normal (radians). */
    val angleInAir: Float,
    /** x coordinate where the fish appears to be from the eye. */
    val apparentX: Float,
    /** Apparent depth (>= 0) — always shallower than the real depth. */
    val apparentDepth: Float,
    /** 0f..1f — fraction of light actually transmitted to the eye; ~0 near grazing incidence. */
    val visibility: Float,
)

/**
 * Solves Snell's law for the point where the ray connecting a submerged fish at
 * ([fishX], [fishDepth]) reaches an eye in the air at ([eyeX], [eyeHeight]) crosses the
 * surface, then derives where the fish appears to be and how much light reaches the eye.
 *
 * [fishDepth] and [eyeHeight] are plain distances (> 0): fishDepth measured downward from
 * the surface, eyeHeight measured upward from the surface. Assumes fishX < eyeX (the fish is
 * out in the water, the eye is on the shore to its right), which holds for every caller here.
 */
fun sight(fishX: Float, fishDepth: Float, eyeX: Float, eyeHeight: Float): Sighting {
    val n = WATER_REFRACTIVE_INDEX

    fun angleInWaterAt(s: Double) = atan2(s - fishX, fishDepth.toDouble())
    fun angleInAirAt(s: Double): Double {
        val sinAir = (n * sin(angleInWaterAt(s))).coerceIn(-1.0, 1.0)
        return asin(sinAir)
    }
    fun angleRequiredAt(s: Double) = atan2(eyeX - s, eyeHeight.toDouble())
    // f(s) = angleInAir(s) - angleRequired(s): strictly non-decreasing in s, negative at
    // s = fishX and positive at s = eyeX, so a bisection root always exists in between.
    fun f(s: Double) = angleInAirAt(s) - angleRequiredAt(s)

    var lo = fishX.toDouble()
    var hi = eyeX.toDouble()
    repeat(40) {
        val mid = (lo + hi) / 2.0
        if (f(mid) < 0.0) lo = mid else hi = mid
    }
    val surfaceX = (lo + hi) / 2.0

    val angleWater = angleInWaterAt(surfaceX)
    val angleAir = angleInAirAt(surfaceX)
    val cosWater = cos(angleWater)
    val cosAir = cos(angleAir)

    val apparentDepth = if (cosWater > 0.001) {
        fishDepth * (cosAir.pow(3) / (n * cosWater.pow(3)))
    } else {
        0.0
    }
    val apparentX = surfaceX - apparentDepth * tan(angleAir)

    // Unpolarised Fresnel transmittance, water -> air.
    val rs = ((n * cosWater - cosAir) / (n * cosWater + cosAir)).pow(2)
    val rp = ((n * cosAir - cosWater) / (n * cosAir + cosWater)).pow(2)
    val transmittance = (1.0 - (rs + rp) / 2.0).coerceIn(0.0, 1.0)

    return Sighting(
        surfaceX = surfaceX.toFloat(),
        angleInWater = angleWater.toFloat(),
        angleInAir = angleAir.toFloat(),
        apparentX = apparentX.toFloat(),
        apparentDepth = apparentDepth.coerceIn(0.0, fishDepth * 2.0).toFloat(),
        visibility = transmittance.toFloat(),
    )
}

/** Shortest distance from [point] to the line segment [a]-[b]. */
fun pointToSegmentDistance(point: Vec2, a: Vec2, b: Vec2): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lengthSquared = abx * abx + aby * aby
    if (lengthSquared == 0f) return distance(point, a)
    val t = (((point.x - a.x) * abx + (point.y - a.y) * aby) / lengthSquared).coerceIn(0f, 1f)
    val closest = Vec2(a.x + abx * t, a.y + aby * t)
    return distance(point, closest)
}

private fun distance(p1: Vec2, p2: Vec2): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    return sqrt(dx * dx + dy * dy)
}
