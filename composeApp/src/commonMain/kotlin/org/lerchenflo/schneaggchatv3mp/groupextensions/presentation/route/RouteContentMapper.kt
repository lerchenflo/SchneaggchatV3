package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.PlannedRoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.RoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteTransportMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.arrivalMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.groupRouteByDay
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.legsTravelMinutes
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.roughTravelMinutes
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.roundTravelMinutes
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.straightLineMeters
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters

/** What the route screen shows, built from the stored points. */
data class RouteContent(
    val points: List<RoutePointUi>,
    val days: List<RouteDayUi>,
    val totalMeters: Double,
    val totalMinutes: Int,
)

fun buildRouteContent(
    route: List<PlannedRoutePoint>,
    defaultMode: RouteTransportMode,
    authorName: (userId: String) -> String,
    canDelete: (PlannedRoutePoint) -> Boolean,
    availableMapEntryIds: Set<String>,
): RouteContent {
    val days = groupRouteByDay(route)
    val dayIndexOf = days.flatMapIndexed { index, day -> day.points.map { it.itemId to index } }.toMap()

    var stopNumber = 0
    val points = route.mapIndexed { position, planned ->
        val point = planned.point
        val next = route.getOrNull(position + 1)
        RoutePointUi(
            itemId = planned.itemId,
            kind = point.kind,
            stopNumber = if (point.kind == RoutePointKind.STOP) ++stopNumber else null,
            name = point.name,
            description = point.description,
            coordinates = point.coordinates,
            plannedAt = point.plannedAt,
            dayIndex = dayIndexOf[planned.itemId] ?: 0,
            authorName = authorName(planned.createdBy),
            // The first point has no way leading to it
            arrivalMode = if (position == 0) null else point.arrivalMode(defaultMode),
            nextLeg = next?.let {
                val meters = distanceMeters(point.coordinates, it.point.coordinates)
                val mode = it.point.arrivalMode(defaultMode)
                RouteLegUi(meters = meters, minutes = roughTravelMinutes(meters, mode), mode = mode, toName = it.point.name)
            },
            mapEntryId = point.mapEntryId,
            isMapEntryAvailable = point.mapEntryId != null && point.mapEntryId in availableMapEntryIds,
            canDelete = canDelete(planned),
        )
    }

    // A day's way starts at the last point of the day before (e.g. the accommodation)
    var previousDayEnd: RoutePoint? = null
    val dayUis = days.mapIndexed { index, day ->
        val dayPoints = listOfNotNull(previousDayEnd) + day.points.map { it.point }
        previousDayEnd = day.points.lastOrNull()?.point
        RouteDayUi(
            index = index,
            date = day.date,
            meters = straightLineMeters(dayPoints.map { it.coordinates }),
            minutes = roundTravelMinutes(legsTravelMinutes(dayPoints, defaultMode)),
        )
    }

    val allPoints = route.map { it.point }
    return RouteContent(
        points = points,
        days = dayUis,
        totalMeters = straightLineMeters(allPoints.map { it.coordinates }),
        totalMinutes = roundTravelMinutes(legsTravelMinutes(allPoints, defaultMode)),
    )
}

/** Names the neighbours of [position] in [route] and how much longer the way gets there. */
fun insertHintFor(route: List<PlannedRoutePoint>, position: Int, coordinates: LatLong): RouteInsertHint {
    val after = route.getOrNull(position - 1)?.point
    val before = route.getOrNull(position)?.point
    val extraMeters = when {
        after != null && before != null ->
            distanceMeters(after.coordinates, coordinates) + distanceMeters(coordinates, before.coordinates) -
                distanceMeters(after.coordinates, before.coordinates)
        after != null -> distanceMeters(after.coordinates, coordinates)
        before != null -> distanceMeters(coordinates, before.coordinates)
        else -> 0.0
    }
    return RouteInsertHint(afterName = after?.name, beforeName = before?.name, extraMeters = extraMeters)
}
