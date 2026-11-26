package org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route

import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.RoutePath
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.RoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionItem

// Alternative paths (user decisions 2026-11-25 and 2026-11-26, plans/ROUTE_PLANNER_PLAN.md "Phase 2").
// A path leaves and rejoins one part: the main route, or another path (a nested path).

/** An alternative way with its own points in order. */
data class PlannedRoutePath(
    val itemId: String,
    val path: RoutePath,
    val createdBy: String,
    val points: List<PlannedRoutePoint>,
) {
    /** Comes back to the point it left from, e.g. a motorbike loop from the parked van. */
    val isSideTrip: Boolean get() = path.fromItemId == path.toItemId
}

/** The stored route: the main line and the paths that leave it. */
data class PlannedRoute(
    val main: List<PlannedRoutePoint> = emptyList(),
    val paths: List<PlannedRoutePath> = emptyList(),
) {
    val allPoints: List<PlannedRoutePoint> get() = main + paths.flatMap { it.points }

    fun point(itemId: String?): PlannedRoutePoint? = itemId?.let { id -> allPoints.find { it.itemId == id } }

    fun path(itemId: String?): PlannedRoutePath? = itemId?.let { id -> paths.find { it.itemId == id } }

    /** The points of the main route ([pathItemId] null) or of that path, in order. */
    fun pointsOf(pathItemId: String?): List<PlannedRoutePoint> = path(pathItemId)?.points ?: main

    /** The part a point lies on: the item id of its path, null for the main route. */
    fun partOf(point: PlannedRoutePoint): String? = path(point.point.pathId)?.itemId

    /** The points of the part [pointItemId] lies on (main route or a path); empty when it is gone. */
    fun partPointsOf(pointItemId: String): List<PlannedRoutePoint> =
        point(pointItemId)?.let { pointsOf(partOf(it)) }.orEmpty()

    /** Paths that leave or rejoin at [pointItemId] (on the main route or on another path). */
    fun pathsAt(pointItemId: String): List<PlannedRoutePath> =
        paths.filter { it.path.fromItemId == pointItemId || it.path.toItemId == pointItemId }

    /**
     * The way of a path as points: where it leaves its part, its own points (without
     * [withoutItemId]), where it rejoins. Null while the start or the rejoin point is missing, when
     * they lie on different parts, or when the path would leave from itself.
     */
    fun lineOf(path: PlannedRoutePath, withoutItemId: String? = null): List<RoutePoint>? {
        val from = point(path.path.fromItemId) ?: return null
        val to = point(path.path.toItemId) ?: return null
        val part = partOf(from)
        if (part != partOf(to) || part == path.itemId) return null
        return listOf(from.point) + path.points.filter { it.itemId != withoutItemId }.map { it.point } + to.point
    }

    /** The paths that branch off [path]'s own points, and theirs, without repeats. */
    fun nestedPathsOf(path: PlannedRoutePath): List<PlannedRoutePath> {
        val found = linkedSetOf<PlannedRoutePath>()
        fun collect(parent: PlannedRoutePath) {
            parent.points.flatMap { pathsAt(it.itemId) }
                .filter { it.itemId != path.itemId && found.add(it) }
                .forEach(::collect)
        }
        collect(path)
        return found.toList()
    }
}

/** The route of [items]; a point whose path is gone (e.g. a delete stopped half way) counts to the main route. */
fun plannedRouteOf(items: List<GroupExtensionItem>): PlannedRoute {
    val paths = items.mapNotNull { item -> (item.data as? RoutePath)?.let { item to it } }.sortedBy { (item, _) -> item.createdAt }
    val pathIds = paths.map { (item, _) -> item.id }.toSet()
    val (pathPoints, main) = routePointsOf(items).partition { it.point.pathId?.let { id -> id in pathIds } == true }
    return PlannedRoute(
        main = main,
        paths = paths.map { (item, path) ->
            PlannedRoutePath(
                itemId = item.id,
                path = path,
                createdBy = item.createdBy,
                points = pathPoints.filter { it.point.pathId == item.id },
            )
        },
    )
}

/** The path's own mode, else the route's [routeDefault]. */
fun RoutePath.mode(routeDefault: RouteTransportMode): RouteTransportMode =
    transportMode?.takeIf { it.isNotBlank() }?.let(RouteTransportMode::fromStored) ?: routeDefault

/**
 * The mode of each leg of a path's line ([PlannedRoute.lineOf]): the mode of the path point it
 * leads to, else the path's; the leg back to the main route is the path's.
 */
fun PlannedRoutePath.legModes(routeDefault: RouteTransportMode): List<RouteTransportMode> {
    val pathMode = path.mode(routeDefault)
    return points.map { it.point.arrivalMode(pathMode) } + pathMode
}

/** Where a new point goes: into the main route ([pathItemId] null) or a path, at [position] of its points. */
data class RouteInsert(val pathItemId: String?, val position: Int)

/**
 * Where a point goes into the part [pathItemId] of [route], leaving out [withoutItemId] (the point
 * itself when it is edited): see [insertPosition].
 */
fun insertInto(
    route: PlannedRoute,
    pathItemId: String?,
    plannedAt: Long?,
    withoutItemId: String? = null,
): RouteInsert {
    val path = route.path(pathItemId)
    val points = route.pointsOf(path?.itemId).filter { it.itemId != withoutItemId }.map { it.point }
    return RouteInsert(pathItemId = path?.itemId, position = insertPosition(points, plannedAt))
}
