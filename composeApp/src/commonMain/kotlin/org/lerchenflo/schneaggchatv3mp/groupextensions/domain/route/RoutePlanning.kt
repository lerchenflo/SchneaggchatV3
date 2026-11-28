package org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.RoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionItem
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.geo.distanceMeters
import kotlin.math.roundToInt
import kotlin.time.Instant

// Route planner rules (user decisions 2026-10-29 and 2026-11-24, see plans/ROUTE_PLANNER_PLAN.md)

/** Room between two neighbouring sortIndex values, so a point can go in between with one write. */
const val SORT_INDEX_STEP = 1024

/** Roads are longer than the straight line; the rough travel time adds this much. */
const val ROUTE_DETOUR_FACTOR = 1.3

private const val TRAVEL_TIME_ROUNDING_MINUTES = 5

/** One point of a route together with the item it is stored in. */
data class PlannedRoutePoint(
    val itemId: String,
    val point: RoutePoint,
    val createdBy: String,
    val createdAt: Long,
)

/** The route points of [items] in route order: [RoutePoint.sortIndex], then creation time. */
fun routePointsOf(items: List<GroupExtensionItem>): List<PlannedRoutePoint> =
    items.mapNotNull { item ->
        (item.data as? RoutePoint)?.let { point ->
            PlannedRoutePoint(itemId = item.id, point = point, createdBy = item.createdBy, createdAt = item.createdAt)
        }
    }.sortedWith(compareBy({ it.point.sortIndex }, { it.createdAt }))

/**
 * Where a new point goes into [points] (which must not contain it): with a planned time in time
 * order, else at the end, so a route is drawn in driving order, also back out on the road it came
 * in on (user, 2026-11-26; before, it went into the nearest segment).
 */
fun insertPosition(points: List<RoutePoint>, plannedAt: Long?): Int =
    if (plannedAt != null) timeOrderPosition(points, plannedAt) else points.size

/**
 * A timed point goes right before the first point with a later time, else to the end. Points
 * without time keep their place between the timed ones.
 */
fun timeOrderPosition(route: List<RoutePoint>, plannedAt: Long): Int {
    val firstLater = route.indexOfFirst { point -> point.plannedAt?.let { it > plannedAt } == true }
    return if (firstLater == -1) route.size else firstLater
}

/**
 * The sortIndex values to write so the items follow [order] (item ids in route order). Ids missing
 * in [current] are new. Keeps the longest run of values that are already in order and puts every
 * other item into the middle of the gap around it, so a single insert or move is one write. When a
 * gap is too small, all items are renumbered. Returns only new or changed values.
 */
fun sortIndexUpdates(order: List<String>, current: Map<String, Int>): Map<String, Int> {
    val values = order.map { current[it]?.takeIf { value -> value >= 0 } }
    val assigned = arrayOfNulls<Int>(order.size)
    longestOrderedRun(values).forEach { position -> assigned[position] = values[position] }

    var previousValue = -1
    for (position in order.indices) {
        assigned[position]?.let {
            previousValue = it
            continue
        }
        val nextValue = (position + 1 until order.size).firstNotNullOfOrNull { assigned[it] }
        val upperBound = nextValue ?: (previousValue + 2 * SORT_INDEX_STEP)
        if (upperBound - previousValue < 2) return renumbered(order, current)
        val value = previousValue + (upperBound - previousValue) / 2
        assigned[position] = value
        previousValue = value
    }

    return order.indices
        .filter { current[order[it]] != assigned[it] }
        .associate { order[it] to assigned[it]!! }
}

private fun renumbered(order: List<String>, current: Map<String, Int>): Map<String, Int> =
    order.mapIndexed { position, id -> id to (position + 1) * SORT_INDEX_STEP }
        .filter { (id, value) -> current[id] != value }
        .toMap()

/** Positions of the longest strictly increasing run of the non-null [values]. */
private fun longestOrderedRun(values: List<Int?>): Set<Int> {
    val runLength = IntArray(values.size)
    val previous = IntArray(values.size) { -1 }
    var bestEnd = -1
    values.forEachIndexed { position, value ->
        if (value == null) return@forEachIndexed
        runLength[position] = 1
        for (earlier in 0 until position) {
            val earlierValue = values[earlier] ?: continue
            if (earlierValue < value && runLength[earlier] + 1 > runLength[position]) {
                runLength[position] = runLength[earlier] + 1
                previous[position] = earlier
            }
        }
        if (bestEnd == -1 || runLength[position] > runLength[bestEnd]) bestEnd = position
    }
    return generateSequence(bestEnd.takeIf { it != -1 }) { previous[it].takeIf { p -> p != -1 } }.toSet()
}

/**
 * One day of the route. [date] is null when no point has a planned time (the route is one block).
 * A point without time belongs to the day of the point before it; points before the first timed
 * point belong to that point's day.
 */
data class RouteDay(
    val date: LocalDate?,
    val points: List<PlannedRoutePoint>,
)

fun groupRouteByDay(
    route: List<PlannedRoutePoint>,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): List<RouteDay> {
    if (route.isEmpty()) return emptyList()
    fun dateOf(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date

    val days = mutableListOf<RouteDay>()
    var currentDate = route.firstNotNullOfOrNull { it.point.plannedAt }?.let(::dateOf)
    var currentPoints = mutableListOf<PlannedRoutePoint>()
    route.forEach { planned ->
        val date = planned.point.plannedAt?.let(::dateOf) ?: currentDate
        if (date != currentDate && currentPoints.isNotEmpty()) {
            days += RouteDay(currentDate, currentPoints)
            currentPoints = mutableListOf()
        }
        currentDate = date
        currentPoints += planned
    }
    days += RouteDay(currentDate, currentPoints)
    return days
}

/** Length of the straight lines from point to point. */
fun straightLineMeters(points: List<LatLong>): Double =
    points.zipWithNext { from, to -> distanceMeters(from, to) }.sum()

/** How the group gets to this point: its own mode, else the route's [routeDefault]. */
fun RoutePoint.arrivalMode(routeDefault: RouteTransportMode): RouteTransportMode =
    transportMode?.takeIf { it.isNotBlank() }?.let(RouteTransportMode::fromStored) ?: routeDefault

/** How the group leaves this point: a TRANSFER's "continue by", else the way it came. */
fun RoutePoint.departureMode(routeDefault: RouteTransportMode): RouteTransportMode =
    continueTransportMode?.takeIf { kind == RoutePointKind.TRANSFER && it.isNotBlank() }
        ?.let(RouteTransportMode::fromStored)
        ?: arrivalMode(routeDefault)

/** Travel time of a straight-line distance by [mode]: × [ROUTE_DETOUR_FACTOR] ÷ the mode's average speed. */
fun travelMinutes(straightLineMeters: Double, mode: RouteTransportMode): Double =
    straightLineMeters / 1000.0 * ROUTE_DETOUR_FACTOR / mode.averageSpeedKmh * 60.0

/** Travel time of the legs between [points], each leg at the speed of the point it leads to. */
fun legsTravelMinutes(points: List<RoutePoint>, routeDefault: RouteTransportMode): Double =
    points.zipWithNext { from, to ->
        travelMinutes(distanceMeters(from.coordinates, to.coordinates), to.arrivalMode(routeDefault))
    }.sum()

/** Travel time of the legs of [line], leg i at [modes] i. */
fun legsTravelMinutes(line: List<RoutePoint>, modes: List<RouteTransportMode>): Double =
    line.zipWithNext().mapIndexed { index, (from, to) ->
        travelMinutes(distanceMeters(from.coordinates, to.coordinates), modes[index])
    }.sum()

/** [minutes] rounded to 5 minutes for display, at least 5 for any travel. */
fun roundTravelMinutes(minutes: Double): Int {
    if (minutes <= 0.0) return 0
    val rounded = (minutes / TRAVEL_TIME_ROUNDING_MINUTES).roundToInt() * TRAVEL_TIME_ROUNDING_MINUTES
    return rounded.coerceAtLeast(TRAVEL_TIME_ROUNDING_MINUTES)
}

/** The mode most legs use (ties: the earlier leg), else [routeDefault] for a route without legs. */
fun mostUsedArrivalMode(points: List<RoutePoint>, routeDefault: RouteTransportMode): RouteTransportMode =
    points.drop(1)
        .map { it.arrivalMode(routeDefault) }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: routeDefault
