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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
            item(key = "chips") { RouteDayChips(state = state, onAction = onAction, showPlacesChip = false) }
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

private fun listRows(points: List<RoutePointUi>): List<RouteListRow> {
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
        if (point.kind == RoutePointKind.WAYPOINT) {
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
            listRows(points).forEach { row ->
                when (row) {
                    is RouteListRow.Single -> RoutePointRow(
                        point = row.point,
                        onClick = { onAction(RouteAction.OnPointEditClick(row.point.itemId)) },
                    )
                    is RouteListRow.Via -> {
                        val key = row.points.first().itemId
                        if (key in unfoldedKeys) {
                            row.points.forEach { point ->
                                RoutePointRow(point = point, onClick = { onAction(RouteAction.OnPointEditClick(point.itemId)) })
                            }
                        } else {
                            ViaRow(points = row.points, onClick = { unfoldedKeys = unfoldedKeys + key })
                        }
                    }
                }
            }
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
