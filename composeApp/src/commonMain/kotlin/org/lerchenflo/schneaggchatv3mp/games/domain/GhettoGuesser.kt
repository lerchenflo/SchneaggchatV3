package org.lerchenflo.schneaggchatv3mp.games.domain

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters
import kotlin.random.Random

const val GHETTO_GUESSER_ROUNDS = 5
const val GHETTO_GUESSER_MAX_ROUND_POINTS = 5000

/** An area to guess in. The polygon is closed implicitly: the last point connects back to the first. */
data class GuessMap(
    val id: String,
    val name: String,
    val polygon: List<LatLong>,
)

/** Hardcoded maps until the server provides them. */
object DemoGuessMaps {
    // Outline taken over from the SchneaggchatV2 schneaggguess game
    val DORNBIRN = GuessMap(
        id = "demo-dornbirn",
        name = "Dornbirn",
        polygon = listOf(
            LatLong(47.436519, 9.723824),
            LatLong(47.434658, 9.761692),
            LatLong(47.416658, 9.756837),
            LatLong(47.388889, 9.753560),
            LatLong(47.384698, 9.718966),
            LatLong(47.408608, 9.703420),
        ),
    )

    val all = listOf(DORNBIRN)
}

/** A geotagged Wikimedia Commons photo plus everything its license requires us to show. */
data class CommonsPhoto(
    val pageId: Long,
    val imageUrl: String,
    val location: LatLong,
    val author: String?,
    val licenseName: String,
    val licenseUrl: String?,
    val filePageUrl: String,
    val title: String,
    val description: String?,
)

data class GuessRoundResult(
    val photo: CommonsPhoto,
    val guess: LatLong,
    val distanceMeters: Double,
    val points: Int,
    val timeMillis: Long,
)

/** Largest distance from [location] to any polygon point; the round's zero-point distance. */
fun GuessMap.maxDistanceFrom(location: LatLong): Double =
    polygon.maxOf { distanceMeters(location, it) }

/**
 * Point calculation of the SchneaggchatV2 schneaggguess game: full points up to 0.5 % of
 * [maxDistance], zero from [maxDistance] on, piecewise linear in between through
 * 6/8, 2.5/8 and 0.5/8 of the points at a quarter, half and three quarters of the range.
 */
fun calculateGuessPoints(guessDistance: Double, maxDistance: Double): Int {
    val maxPoints = GHETTO_GUESSER_MAX_ROUND_POINTS.toDouble()
    val correctMeters = (maxDistance * 0.005).toInt().toDouble()

    if (guessDistance <= correctMeters) return GHETTO_GUESSER_MAX_ROUND_POINTS
    if (guessDistance >= maxDistance) return 0

    val range = maxDistance - correctMeters
    val distances = listOf(
        correctMeters,
        correctMeters + range / 4.0,
        correctMeters + range / 2.0,
        correctMeters + 3 * range / 4.0,
        maxDistance,
    )
    val points = listOf(maxPoints, maxPoints * 6.0 / 8.0, maxPoints * 2.5 / 8.0, maxPoints * 0.5 / 8.0, 0.0)

    val segment = (1 until distances.size).first { guessDistance <= distances[it] }
    val x1 = distances[segment - 1]
    val x2 = distances[segment]
    val y1 = points[segment - 1]
    val y2 = points[segment]
    val interpolated = y1 + (y2 - y1) * (guessDistance - x1) / (x2 - x1)
    return interpolated.toInt().coerceAtLeast(0)
}

/** Ray casting on lat/long; precise enough for town-sized polygons. */
fun GuessMap.contains(location: LatLong): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        val crosses = (a.lat > location.lat) != (b.lat > location.lat) &&
                location.long < (b.long - a.long) * (location.lat - a.lat) / (b.lat - a.lat) + a.long
        if (crosses) inside = !inside
        j = i
    }
    return inside
}

/** Uniform random point inside the polygon by sampling its bounding box. */
fun GuessMap.randomPointInside(random: Random = Random): LatLong {
    val minLat = polygon.minOf { it.lat }
    val maxLat = polygon.maxOf { it.lat }
    val minLong = polygon.minOf { it.long }
    val maxLong = polygon.maxOf { it.long }
    while (true) {
        val candidate = LatLong(
            lat = random.nextDouble(minLat, maxLat),
            long = random.nextDouble(minLong, maxLong),
        )
        if (contains(candidate)) return candidate
    }
}
