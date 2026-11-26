package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.route_path_add_hint
import schneaggchatv3mp.composeapp.generated.resources.route_path_close
import schneaggchatv3mp.composeapp.generated.resources.route_add_to_route
import schneaggchatv3mp.composeapp.generated.resources.route_all_days
import schneaggchatv3mp.composeapp.generated.resources.route_day_number
import schneaggchatv3mp.composeapp.generated.resources.route_empty_hint
import schneaggchatv3mp.composeapp.generated.resources.route_open_place
import schneaggchatv3mp.composeapp.generated.resources.route_place_subtitle

/** "All | Day 1 | Day 2 …" over the map, only for a route over several days. */
@Composable
fun RouteDayChips(
    state: RouteState,
    onAction: (RouteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.showDayChips) return
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            FilterChip(
                selected = state.selectedDayIndex == null,
                onClick = { onAction(RouteAction.OnDaySelect(null)) },
                label = { Text(stringResource(Res.string.route_all_days)) },
                colors = chipColors,
            )
        }
        items(state.days, key = { "day-${it.index}" }) { day ->
            FilterChip(
                selected = state.selectedDayIndex == day.index,
                onClick = { onAction(RouteAction.OnDaySelect(day.index)) },
                label = { Text(stringResource(Res.string.route_day_number, day.index + 1)) },
                colors = chipColors,
            )
        }
    }
}

/** Distance and rough time (each leg at its own speed) of the shown part of the route. */
@Composable
fun RouteSummaryPill(
    meters: Double,
    minutes: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Route, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = formatDistanceAndTime(meters, minutes), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** "Long press the map …" while the route has no point yet. */
@Composable
fun RouteEmptyMapHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
    ) {
        Text(
            text = stringResource(Res.string.route_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/** A Schneaggmap place tapped on the route map: where it would go, "Open place" and "Add to route". */
@Composable
fun RoutePlaceCard(
    place: RouteMapPlaceUi,
    insertHint: RouteInsertHint?,
    onAddClick: () -> Unit,
    onOpenClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Place, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = place.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(Res.string.route_place_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(Res.string.cancel))
                }
            }
            insertHint?.let {
                Text(
                    text = insertHintText(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onOpenClick) {
                    Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.route_open_place))
                }
                Button(onClick = onAddClick, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.route_add_to_route))
                }
            }
        }
    }
}

/**
 * Above the cards of a selected alternative path: its name, ends, distance and time, edit, back to
 * the main route. While it is shown, long presses on the map add points to it.
 */
@Composable
fun RoutePathHeader(
    path: RoutePathUi,
    onEditClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        onClick = onEditClick,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = path.mode.icon(),
                contentDescription = stringResource(path.mode.labelRes()),
                tint = path.mode.colors().container,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = path.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOf(routePathEndsText(path), formatDistanceAndTime(path.meters, path.minutes))
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (path.points.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.route_path_add_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onEditClick) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
            }
            IconButton(onClick = onCloseClick) {
                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(Res.string.route_path_close))
            }
        }
    }
}
