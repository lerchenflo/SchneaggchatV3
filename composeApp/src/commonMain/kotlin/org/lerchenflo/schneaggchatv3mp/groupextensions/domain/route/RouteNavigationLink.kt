package org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

/** Google Maps directions links take an origin, a destination and at most 9 waypoints. */
const val MAX_NAVIGATION_POINTS = 11

/** A directions link; [isCut] when the route had more points than one link can carry. */
data class RouteNavigationLink(
    val url: String,
    val isCut: Boolean,
)

/**
 * Directions link for the whole route from its first point, opened by the Google Maps app when
 * installed, else in the browser. Apple Maps links can't carry waypoints, so every platform uses
 * this one (user, 2026-11-24). A single point only sets the destination (route from the own
 * location). Null for an empty route.
 */
fun googleMapsRouteLink(points: List<LatLong>, mode: RouteTransportMode): RouteNavigationLink? {
    if (points.isEmpty()) return null
    val shown = points.take(MAX_NAVIGATION_POINTS)
    val parameters = buildList {
        add("api=1")
        if (shown.size > 1) add("origin=${shown.first().toUrlValue()}")
        add("destination=${shown.last().toUrlValue()}")
        val waypoints = shown.drop(1).dropLast(1)
        if (waypoints.isNotEmpty()) add("waypoints=${waypoints.joinToString("%7C") { it.toUrlValue() }}")
        add("travelmode=${mode.googleTravelMode()}")
    }
    return RouteNavigationLink(
        url = "https://www.google.com/maps/dir/?" + parameters.joinToString("&"),
        isCut = points.size > MAX_NAVIGATION_POINTS,
    )
}

private fun LatLong.toUrlValue(): String = "$lat,$long"

// Google's "two-wheeler" mode exists only in a few countries, so a motorbike uses "driving"
private fun RouteTransportMode.googleTravelMode(): String = when (this) {
    RouteTransportMode.FOOT -> "walking"
    RouteTransportMode.BICYCLE -> "bicycling"
    RouteTransportMode.MOTORBIKE, RouteTransportMode.CAR -> "driving"
}
