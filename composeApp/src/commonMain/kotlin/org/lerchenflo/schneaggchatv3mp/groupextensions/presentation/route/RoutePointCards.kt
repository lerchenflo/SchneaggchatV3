package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.route_leg
import schneaggchatv3mp.composeapp.generated.resources.route_open_place

/**
 * Swipeable cards of the shown points at the bottom of the map, like the friends bar on the
 * Schneaggmap. Swiping selects the point (the camera flies there); tapping a marker scrolls to
 * its card.
 */
@Composable
fun RoutePointCards(
    points: List<RoutePointUi>,
    selectedPointId: String?,
    onAction: (RouteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val selectedIndex = points.indexOfFirst { it.itemId == selectedPointId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedIndex) { points.size }

    // A marker tap selects a point: show its card
    LaunchedEffect(selectedPointId) {
        val index = points.indexOfFirst { it.itemId == selectedPointId }
        if (index >= 0 && index != pagerState.currentPage) pagerState.animateScrollToPage(index)
    }
    // A swipe selects the point of the card it settles on
    val currentSelectedId by rememberUpdatedState(selectedPointId)
    LaunchedEffect(pagerState, points) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val point = points.getOrNull(page) ?: return@collect
            // Nothing selected yet and still on the first card: the user hasn't swiped
            val untouched = currentSelectedId == null && page == 0
            if (point.itemId != currentSelectedId && !untouched) {
                onAction(RouteAction.OnPointSelect(point.itemId))
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 40.dp),
        pageSpacing = 10.dp,
    ) { page ->
        RoutePointCard(
            point = points[page],
            onEditClick = { onAction(RouteAction.OnPointEditClick(points[page].itemId)) },
            onOpenPlaceClick = { mapEntryId -> onAction(RouteAction.OnOpenMapEntryClick(mapEntryId)) },
        )
    }
}

@Composable
private fun RoutePointCard(
    point: RoutePointUi,
    onEditClick: () -> Unit,
    onOpenPlaceClick: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        onClick = onEditClick,
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoutePointBadge(kind = point.kind, stopNumber = point.stopNumber)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = point.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(
                            stringResource(point.kind.labelRes()),
                            point.plannedAt?.let { formatPlannedDateTime(it) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
                }
            }

            val details = listOf(point.description, point.authorName).filter { it.isNotBlank() }.joinToString(" · ")
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp, end = 10.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
                point.nextLeg?.let { leg ->
                    Icon(
                        imageVector = leg.mode.icon(),
                        contentDescription = stringResource(leg.mode.labelRes()),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = point.nextLeg?.let { leg ->
                        stringResource(Res.string.route_leg, formatDistanceAndTime(leg.meters, leg.minutes), leg.toName)
                    }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (point.isMapEntryAvailable && point.mapEntryId != null) {
                    TextButton(onClick = { onOpenPlaceClick(point.mapEntryId) }) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.route_open_place))
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}
