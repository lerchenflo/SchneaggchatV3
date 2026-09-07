package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import org.maplibre.compose.camera.CameraState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

//Shared between the phone chrome (SchneaggmapScreen: zoom-slider snapping) and the map layers
//(SchneaggmapLayers: friend clustering), which live in different packages - kept internal (not
//private) so both can use it without duplicating the formula.

private const val EARTH_RADIUS_METERS = 6371000.0

//The camera only knows its scale once the map has reported a viewport; until then everything that
//converts dp to meters falls back to zero, which reads as "no snapping, no clustering yet".
internal val CameraState.metersPerDpAtTarget: Double
    get() = viewport?.metersPerDpAtTarget ?: 0.0

//Flat-earth approximation - accurate enough for grouping markers that are at most a few
//kilometers apart, which is all clustering cares about, without the cost of full Haversine.
internal fun approximateDistanceMeters(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
    val avgLatRad = (lat1 + lat2) / 2.0 * PI / 180.0
    val dx = (long2 - long1) * cos(avgLatRad) * EARTH_RADIUS_METERS * PI / 180.0
    val dy = (lat2 - lat1) * EARTH_RADIUS_METERS * PI / 180.0
    return sqrt(dx * dx + dy * dy)
}
