package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.route_empty_hint
import schneaggchatv3mp.composeapp.generated.resources.route_path_show_on_map
import schneaggchatv3mp.composeapp.generated.resources.route_reorder
import schneaggchatv3mp.composeapp.generated.resources.route_straight_line
import schneaggchatv3mp.composeapp.generated.resources.route_via

/** The list tab: totals, then one card per day (or one for a route without dates). */
@Composable
fun RouteDayList(
    state: RouteState,
    onAction: (RouteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shownDays = state.selectedDayIndex?.let { listOfNotNull(state.days.getOrNull(it)) } ?: state.days

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.showDayChips) {
            item(key = "chips") { RouteDayChips(state = state, onAction = onAction) }
        }
        item(key = "summary") { RouteListSummary(state = state, onAction = onAction) }
        if (state.points.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(Res.string.route_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
        items(shownDays, key = { "day-${it.index}" }) { day ->
            RouteDayCard(
                day = day,
                points = state.points.filter { it.dayIndex == day.index },
                paths = state.paths.filter { it.dayIndex == day.index },
                onAction = onAction,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun RouteListSummary(state: RouteState, onAction: (RouteAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.Route, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatDistanceAndTime(state.shownMeters, state.shownMinutes),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(Res.string.route_straight_line),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (state.points.size >= 2) {
            TextButton(onClick = { onAction(RouteAction.OnReorderClick) }) {
                Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(Res.string.route_reorder))
            }
        }
    }
}

/** Consecutive drive-through points fold into one "via …" row; a tap unfolds several. */
private sealed interface RouteListRow {
    data class Single(val point: RoutePointUi) : RouteListRow
    data class Via(val points: List<RoutePointUi>) : RouteListRow
}

/** [keepSingle]: drive-through points that must keep their own row, e.g. where a path starts. */
private fun listRows(points: List<RoutePointUi>, keepSingle: (RoutePointUi) -> Boolean = { false }): List<RouteListRow> {
    val rows = mutableListOf<RouteListRow>()
    var waypoints = mutableListOf<RoutePointUi>()
    fun flushWaypoints() {
        when (waypoints.size) {
            0 -> Unit
            1 -> rows += RouteListRow.Single(waypoints.single())
            else -> rows += RouteListRow.Via(waypoints)
        }
        waypoints = mutableListOf()
    }
    points.forEach { point ->
        if (point.kind == RoutePointKind.WAYPOINT && !keepSingle(point)) {
            waypoints += point
        } else {
            flushWaypoints()
            rows += RouteListRow.Single(point)
        }
    }
    flushWaypoints()
    return rows
}

@Composable
private fun RouteDayCard(
    day: RouteDayUi,
    points: List<RoutePointUi>,
    paths: List<RoutePathUi>,
    onAction: (RouteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var unfoldedKeys by rememberSaveable { mutableStateOf(listOf<String>()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayTitle(day),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatDistanceAndTime(day.meters, day.minutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.padding(bottom = 6.dp)) {
            val pathsByStart = paths.groupBy { it.fromItemId }
            PointRows(
                points = points,
                unfoldedKeys = unfoldedKeys,
                onUnfold = { unfoldedKeys = unfoldedKeys + it },
                onAction = onAction,
                keepSingle = { it.itemId in pathsByStart },
                after = { point ->
                    pathsByStart[point.itemId]?.forEach { path ->
                        RoutePathBranch(
                            path = path,
                            pathsByStart = pathsByStart,
                            unfoldedKeys = unfoldedKeys,
                            onUnfold = { unfoldedKeys = unfoldedKeys + it },
                            onAction = onAction,
                        )
                    }
                },
            )
            // A path whose start was deleted shows at the end, so it can be fixed or deleted
            val shownIds = (points + paths.flatMap { it.points }).map { it.itemId }.toSet()
            paths.filter { it.fromItemId !in shownIds }.forEach { path ->
                RoutePathBranch(
                    path = path,
                    pathsByStart = pathsByStart,
                    unfoldedKeys = unfoldedKeys,
                    onUnfold = { unfoldedKeys = unfoldedKeys + it },
                    onAction = onAction,
                )
            }
        }
    }
}

/** Point rows with folded drive-through runs; [after] adds content below a single point's row. */
@Composable
private fun PointRows(
    points: List<RoutePointUi>,
    unfoldedKeys: List<String>,
    onUnfold: (String) -> Unit,
    onAction: (RouteAction) -> Unit,
    keepSingle: (RoutePointUi) -> Boolean = { false },
    after: @Composable (RoutePointUi) -> Unit = {},
) {
    listRows(points, keepSingle).forEach { row ->
        when (row) {
            is RouteListRow.Single -> {
                RoutePointRow(point = row.point, onClick = { onAction(RouteAction.OnPointEditClick(row.point.itemId)) })
                after(row.point)
            }
            is RouteListRow.Via -> {
                val key = row.points.first().itemId
                if (key in unfoldedKeys) {
                    row.points.forEach { point ->
                        RoutePointRow(point = point, onClick = { onAction(RouteAction.OnPointEditClick(point.itemId)) })
                    }
                } else {
                    ViaRow(points = row.points, onClick = { onUnfold(key) })
                }
            }
        }
    }
}

/**
 * An alternative path under the point it leaves from: name, ends, distance and time, its points,
 * and the paths branching off them (nested, from [pathsByStart]; [outerPathIds] stops paths that
 * branch off each other from repeating).
 */
@Composable
private fun RoutePathBranch(
    path: RoutePathUi,
    pathsByStart: Map<String, List<RoutePathUi>>,
    unfoldedKeys: List<String>,
    onUnfold: (String) -> Unit,
    onAction: (RouteAction) -> Unit,
    outerPathIds: Set<String> = emptySet(),
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 12.dp, top = 2.dp, bottom = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(RouteAction.OnPathEditClick(path.itemId)) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = path.mode.icon(),
                    contentDescription = stringResource(path.mode.labelRes()),
                    tint = path.mode.colors().container,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = path.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = routePathEndsText(path),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (path.isBroken) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!path.isBroken) {
                    Text(
                        text = formatDistanceAndTime(path.meters, path.minutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // On the map, where long presses then add points to it
                    IconButton(onClick = { onAction(RouteAction.OnPathSelect(path.itemId)) }) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = stringResource(Res.string.route_path_show_on_map))
                    }
                }
            }
            if (path.description.isNotBlank()) {
                Text(
                    text = path.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 42.dp, end = 12.dp, bottom = 6.dp)
                )
            }
            PointRows(
                points = path.points,
                unfoldedKeys = unfoldedKeys,
                onUnfold = onUnfold,
                onAction = onAction,
                keepSingle = { it.itemId in pathsByStart },
                after = { point ->
                    val shownAbove = outerPathIds + path.itemId
                    pathsByStart[point.itemId]?.filter { it.itemId !in shownAbove }?.forEach { nested ->
                        RoutePathBranch(
                            path = nested,
                            pathsByStart = pathsByStart,
                            unfoldedKeys = unfoldedKeys,
                            onUnfold = onUnfold,
                            onAction = onAction,
                            outerPathIds = shownAbove,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun RoutePointRow(point: RoutePointUi, onClick: () -> Unit) {
    val isWaypoint = point.kind == RoutePointKind.WAYPOINT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (isWaypoint) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoutePointBadge(kind = point.kind, stopNumber = point.stopNumber)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = point.name,
                    style = if (isWaypoint) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = if (isWaypoint) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (point.mapEntryId != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (point.description.isNotBlank()) {
                Text(
                    text = point.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        point.plannedAt?.let {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatPlannedTime(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // How the group gets here
        point.arrivalMode?.let { mode ->
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = mode.icon(),
                contentDescription = stringResource(mode.labelRes()),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ViaRow(points: List<RoutePointUi>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoutePointBadge(kind = RoutePointKind.WAYPOINT, stopNumber = null)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = stringResource(Res.string.route_via, points.joinToString(", ") { it.name }),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
