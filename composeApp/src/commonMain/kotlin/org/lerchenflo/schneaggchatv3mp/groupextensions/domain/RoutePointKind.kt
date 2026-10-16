package org.lerchenflo.schneaggchatv3mp.groupextensions.domain

import kotlinx.serialization.Serializable

/** What a route planner point is. The names are wire and stored values shared with the server, so never rename an entry. */
@Serializable
enum class RoutePointKind {
    STOP,
    WAYPOINT,
    ACCOMMODATION,
    POI
}
