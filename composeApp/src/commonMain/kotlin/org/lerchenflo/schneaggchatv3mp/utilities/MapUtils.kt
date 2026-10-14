package org.lerchenflo.schneaggchatv3mp.utilities

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Mean Earth radius in metres, used for great-circle calculations. */
private const val EARTH_RADIUS_METERS = 6_371_000.0

private fun Double.toRadians(): Double = this * PI / 180.0

private fun Double.toDegrees(): Double = this * 180.0 / PI

/**
 * Great-circle distance between two coordinates in metres, using the haversine formula.
 * Returns full-precision metres (use [formatDistance] for display).
 */
fun distanceMeters(from: LatLong, to: LatLong): Double {
    val lat1 = from.lat.toRadians()
    val lat2 = to.lat.toRadians()
    val deltaLat = (to.lat - from.lat).toRadians()
    val deltaLong = (to.long - from.long).toRadians()

    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLong / 2) * sin(deltaLong / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return EARTH_RADIUS_METERS * c
}

/**
 * Formats a distance in metres into a short human-readable string:
 * below 1 km as whole metres ("850 m"), otherwise kilometres with one decimal ("1.2 km").
 */
fun formatDistance(meters: Double): String {
    if (meters < 1000.0) {
        return "${meters.roundToInt()} m"
    }
    val km = meters / 1000.0
    val rounded = (km * 10).roundToInt() / 10.0
    return "$rounded km"
}

/**
 * Initial bearing (heading) from one coordinate to another, in degrees normalized to 0..360.
 * 0 = north, 90 = east, 180 = south, 270 = west.
 */
fun bearing(from: LatLong, to: LatLong): Double {
    val lat1 = from.lat.toRadians()
    val lat2 = to.lat.toRadians()
    val deltaLong = (to.long - from.long).toRadians()

    val y = sin(deltaLong) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLong)

    return (atan2(y, x).toDegrees() + 360.0) % 360.0
}

/** Returns true if [a] and [b] are within [radiusMeters] of each other. */
fun isWithinRadius(a: LatLong, b: LatLong, radiusMeters: Double): Boolean =
    distanceMeters(a, b) <= radiusMeters

/** Great-circle midpoint between two coordinates. */
fun midpoint(a: LatLong, b: LatLong): LatLong {
    val lat1 = a.lat.toRadians()
    val lat2 = b.lat.toRadians()
    val long1 = a.long.toRadians()
    val deltaLong = (b.long - a.long).toRadians()

    val bx = cos(lat2) * cos(deltaLong)
    val by = cos(lat2) * sin(deltaLong)

    val midLat = atan2(
        sin(lat1) + sin(lat2),
        sqrt((cos(lat1) + bx) * (cos(lat1) + bx) + by * by)
    )
    val midLong = long1 + atan2(by, cos(lat1) + bx)

    return LatLong(lat = midLat.toDegrees(), long = midLong.toDegrees())
}
