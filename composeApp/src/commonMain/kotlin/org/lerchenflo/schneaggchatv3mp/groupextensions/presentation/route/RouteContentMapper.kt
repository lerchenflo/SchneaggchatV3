package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.RoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.PlannedRoute
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.PlannedRoutePath
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.PlannedRoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteTransportMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.arrivalMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.groupRouteByDay
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.legModes
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.legsTravelMinutes
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.mode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.roundTravelMinutes
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.straightLineMeters
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.geo.distanceMeters

/** What the route screen shows, built from the stored points and paths. */
data class RouteContent(
    val points: List<RoutePointUi>,
    val paths: List<RoutePathUi>,
    val days: List<RouteDayUi>,
    val totalMeters: Double,
    val totalMinutes: Int,
)

/** Builds [RouteContent]; [canDelete] gets the creator of a point or path. */
fun buildRouteContent(
    route: PlannedRoute,
    defaultMode: RouteTransportMode,
    authorName: (userId: String) -> String,
    canDelete: (createdBy: String) -> Boolean,
    availableMapEntryIds: Set<String>,
    /** "Drive-through" in the app's language, see [hasOwnName]. */
    unnamedWaypointLabel: String,
): RouteContent {
    val days = groupRouteByDay(route.main)
    val dayIndexOf = days.flatMapIndexed { index, day -> day.points.map { it.itemId to index } }.toMap()

    fun pointUi(planned: PlannedRoutePoint, stopNumber: Int?, dayIndex: Int, arrivalMode: RouteTransportMode?) =
        RoutePointUi(
            itemId = planned.itemId,
            kind = planned.point.kind,
            stopNumber = stopNumber,
            name = planned.point.displayName(unnamedWaypointLabel),
            hasName = planned.point.hasOwnName(unnamedWaypointLabel),
            description = planned.point.description,
            coordinates = planned.point.coordinates,
            plannedAt = planned.point.plannedAt,
            dayIndex = dayIndex,
            authorName = authorName(planned.createdBy),
            arrivalMode = arrivalMode,
            nextLeg = null,
            mapEntryId = planned.point.mapEntryId,
            isMapEntryAvailable = planned.point.mapEntryId != null && planned.point.mapEntryId in availableMapEntryIds,
            canDelete = canDelete(planned.createdBy),
            pathId = planned.point.pathId?.takeIf { route.path(it) != null },
        )

    var stopNumber = 0
    val mainUis = route.main.mapIndexed { position, planned ->
        pointUi(
            planned = planned,
            stopNumber = if (planned.point.kind == RoutePointKind.STOP) ++stopNumber else null,
            dayIndex = dayIndexOf[planned.itemId] ?: 0,
            // The first point has no way leading to it
            arrivalMode = if (position == 0) null else planned.point.arrivalMode(defaultMode),
        )
    }
    val mainLine = PartLine(
        points = route.main.map { it.point },
        modes = route.main.drop(1).map { it.point.arrivalMode(defaultMode) },
        stops = mainUis,
    )
    val points = mainUis.mapIndexed { index, point -> point.copy(nextLeg = mainLine.wayOn(index)) }

    // Every path's points first, so a nested path finds the path point it leaves from
    val pathDays = route.paths.associate { it.itemId to pathDayIndex(it, route, dayIndexOf) }
    val pathPointUis = route.paths.associate { path ->
        val pathMode = path.path.mode(defaultMode)
        val dayIndex = pathDays.getValue(path.itemId)
        path.itemId to path.points.map { pointUi(it, null, dayIndex, it.point.arrivalMode(pathMode)) }
    }
    val uiById = (mainUis + pathPointUis.values.flatten()).associateBy { it.itemId }
    val paths = withNestedSideTrips(
        route.paths.map { path ->
            pathUi(
                path = path,
                route = route,
                uiById = uiById,
                ownUis = pathPointUis.getValue(path.itemId),
                dayIndex = pathDays.getValue(path.itemId),
                defaultMode = defaultMode,
                canDelete = canDelete,
            )
        }
    )

    // A day's way starts at the last point of the day before (e.g. the accommodation)
    var previousDayEnd: RoutePoint? = null
    val dayUis = days.mapIndexed { index, day ->
        val dayPoints = listOfNotNull(previousDayEnd) + day.points.map { it.point }
        previousDayEnd = day.points.lastOrNull()?.point
        val sideTrips = paths.filter { it.isSideTrip && it.parentPathId == null && it.dayIndex == index }
        RouteDayUi(
            index = index,
            date = day.date,
            meters = straightLineMeters(dayPoints.map { it.coordinates }) + sideTrips.sumOf { it.meters },
            minutes = roundTravelMinutes(legsTravelMinutes(dayPoints, defaultMode) + sideTrips.sumOf { it.rawMinutes }),
        )
    }

    // A path that rejoins later replaces part of the main route; a side trip adds to it (user, 2026-11-26)
    val sideTrips = paths.filter { it.isSideTrip && it.parentPathId == null }
    val mainPoints = route.main.map { it.point }
    return RouteContent(
        points = points,
        paths = paths,
        days = dayUis,
        totalMeters = straightLineMeters(mainPoints.map { it.coordinates }) + sideTrips.sumOf { it.meters },
        totalMinutes = roundTravelMinutes(legsTravelMinutes(mainPoints, defaultMode) + sideTrips.sumOf { it.rawMinutes }),
    )
}

private fun pathUi(
    path: PlannedRoutePath,
    route: PlannedRoute,
    uiById: Map<String, RoutePointUi>,
    ownUis: List<RoutePointUi>,
    dayIndex: Int,
    defaultMode: RouteTransportMode,
    canDelete: (createdBy: String) -> Boolean,
): RoutePathUi {
    val from = uiById[path.path.fromItemId]
    val to = uiById[path.path.toItemId]
    val line = route.lineOf(path)
    val modes = path.legModes(defaultMode)
    val points = if (line != null && from != null && to != null) {
        val pathLine = PartLine(points = line, modes = modes, stops = listOf(from) + ownUis + to)
        // Line index 0 is the start point
        ownUis.mapIndexed { index, point -> point.copy(nextLeg = pathLine.wayOn(index + 1)) }
    } else ownUis
    val rawMinutes = line?.let { legsTravelMinutes(it, modes) } ?: 0.0

    return RoutePathUi(
        itemId = path.itemId,
        name = path.path.name,
        description = path.path.description,
        mode = path.path.mode(defaultMode),
        fromItemId = path.path.fromItemId,
        toItemId = path.path.toItemId,
        parentPathId = route.point(path.path.fromItemId)?.let(route::partOf),
        fromName = from?.name,
        toName = to?.name,
        isSideTrip = path.isSideTrip,
        points = points,
        line = line?.map { it.coordinates }.orEmpty(),
        legModes = if (line != null) modes else emptyList(),
        legModeItemIds = path.points.map { it.itemId } + path.itemId,
        meters = line?.let { straightLineMeters(it.map { point -> point.coordinates }) } ?: 0.0,
        rawMinutes = rawMinutes,
        minutes = roundTravelMinutes(rawMinutes),
        dayIndex = dayIndex,
        canDelete = canDelete(path.createdBy),
    )
}

/** The day of the point a path leaves from; a nested path takes its parent path's day. */
private fun pathDayIndex(path: PlannedRoutePath, route: PlannedRoute, dayIndexOf: Map<String, Int>, depth: Int = 0): Int {
    val from = route.point(path.path.fromItemId) ?: return dayIndexOf.values.maxOrNull() ?: 0
    val parent = route.path(from.point.pathId)
    // Paths leaving from each other: stop after one round
    return if (parent == null || depth > route.paths.size) {
        dayIndexOf[from.itemId] ?: dayIndexOf.values.maxOrNull() ?: 0
    } else {
        pathDayIndex(parent, route, dayIndexOf, depth + 1)
    }
}

/** A side trip adds its way to the part it leaves from, so a path also counts its nested side trips. */
private fun withNestedSideTrips(paths: List<RoutePathUi>): List<RoutePathUi> {
    val totals = mutableMapOf<String, Pair<Double, Double>>()
    fun total(path: RoutePathUi, depth: Int): Pair<Double, Double> {
        totals[path.itemId]?.let { return it }
        val own = path.meters to path.rawMinutes
        if (depth > paths.size) return own
        val result = paths
            .filter { it.isSideTrip && it.parentPathId == path.itemId && it.itemId != path.itemId }
            .fold(own) { (meters, minutes), child ->
                val (childMeters, childMinutes) = total(child, depth + 1)
                (meters + childMeters) to (minutes + childMinutes)
            }
        totals[path.itemId] = result
        return result
    }
    return paths.map { path ->
        val (meters, minutes) = total(path, depth = 0)
        path.copy(meters = meters, rawMinutes = minutes, minutes = roundTravelMinutes(minutes))
    }
}

/** A part of the route as a line: its points, the mode of each leg, and how each point shows. */
private class PartLine(
    val points: List<RoutePoint>,
    val modes: List<RouteTransportMode>,
    val stops: List<RoutePointUi>,
) {
    /**
     * The way from line index [from] to the next point with details (or the line's end): the
     * drive-through points in between only show as dots, so their legs add up on the card before them.
     */
    fun wayOn(from: Int): RouteLegUi? {
        if (from >= points.lastIndex) return null
        val target = (from + 1..points.lastIndex).firstOrNull { stops[it].hasDetails } ?: points.lastIndex
        val legPoints = points.subList(from, target + 1)
        return RouteLegUi(
            meters = straightLineMeters(legPoints.map { it.coordinates }),
            minutes = roundTravelMinutes(legsTravelMinutes(legPoints, modes.subList(from, target))),
            mode = modes[from],
            toName = stops[target].takeIf { it.hasName }?.name,
        )
    }
}

/**
 * False for a drive-through point left without a name: stored as null, or (before the server took
 * null names, 2026-11-26) as the type's label in the creator's language; only the label in this
 * phone's language is recognised.
 */
fun RoutePoint.hasOwnName(unnamedWaypointLabel: String): Boolean =
    !name.isNullOrBlank() && !(kind == RoutePointKind.WAYPOINT && name == unnamedWaypointLabel)

/** The name to show: an unnamed drive-through point shows the type's label. */
fun RoutePoint.displayName(unnamedWaypointLabel: String): String =
    name?.takeIf { it.isNotBlank() } ?: unnamedWaypointLabel

/**
 * Names the neighbours of [position] in [line] (the points the new one goes between) and how much
 * longer the way gets there.
 */
fun insertHintFor(line: List<RoutePoint>, position: Int, coordinates: LatLong, unnamedWaypointLabel: String): RouteInsertHint {
    val after = line.getOrNull(position - 1)
    val before = line.getOrNull(position)
    val extraMeters = when {
        after != null && before != null ->
            distanceMeters(after.coordinates, coordinates) + distanceMeters(coordinates, before.coordinates) -
                distanceMeters(after.coordinates, before.coordinates)
        after != null -> distanceMeters(after.coordinates, coordinates)
        before != null -> distanceMeters(coordinates, before.coordinates)
        else -> 0.0
    }
    return RouteInsertHint(
        afterName = after?.displayName(unnamedWaypointLabel),
        beforeName = before?.displayName(unnamedWaypointLabel),
        extraMeters = extraMeters,
    )
}
