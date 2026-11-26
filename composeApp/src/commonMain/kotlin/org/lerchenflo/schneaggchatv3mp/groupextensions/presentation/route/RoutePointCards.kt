package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.edit
import schneaggchatv3mp.composeapp.generated.resources.route_leg
import schneaggchatv3mp.composeapp.generated.resources.route_open_place
import kotlin.math.abs

private val CARD_PEEK = 40.dp
private val ITEM_SPACING = 10.dp
// About five dots take the room of one card
private val DOT_SIZE = 44.dp
// Per second; slower releases rest on the nearest card or dot
private val MIN_FLING_VELOCITY = 400.dp

/**
 * The shown points at the bottom of the map, like the friends bar on the Schneaggmap: a card for
 * each point with a name or description, a small dot for each drive-through point without. A swipe
 * goes on to the next card (the dots in between are skipped), a slow drag can rest on the dots. The
 * card it settles on is selected (the camera flies there); tapping a marker scrolls to its card or
 * dot; tapping a dot selects it (the camera flies there).
 */
@Composable
fun RoutePointCards(
    points: List<RoutePointUi>,
    selectedPointId: String?,
    onAction: (RouteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = maxWidth - CARD_PEEK * 2
        val density = LocalDensity.current
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = points.indexOfFirst { it.itemId == selectedPointId }.coerceAtLeast(0)
        )
        val snapping = remember(listState, points, cardWidth, density) {
            with(density) {
                RoutePointRowSnapping(
                    listState = listState,
                    isCard = points.map { it.hasDetails },
                    cardSizePx = cardWidth.roundToPx(),
                    dotSizePx = DOT_SIZE.roundToPx(),
                    spacingPx = ITEM_SPACING.roundToPx(),
                    minFlingVelocityPx = MIN_FLING_VELOCITY.toPx(),
                )
            }
        }

        // A marker tap selects a point: show its card or dot
        LaunchedEffect(selectedPointId) {
            val index = points.indexOfFirst { it.itemId == selectedPointId }
            if (index >= 0 && !listState.isFullyVisible(index)) listState.animateScrollToItem(index)
        }
        // A swipe selects the card it settles on
        val currentSelectedId by rememberUpdatedState(selectedPointId)
        LaunchedEffect(listState, points) {
            snapshotFlow { listState.isScrollInProgress }.filter { !it }.collect {
                val index = listState.alignedItemIndex() ?: return@collect
                val point = points.getOrNull(index) ?: return@collect
                // Nothing selected yet and still on the first card: the user hasn't swiped
                val untouched = currentSelectedId == null && index == 0
                // A marker tap scrolled to the selected point (a dot, or the end of the row)
                val selectedStillShown = points.indexOfFirst { it.itemId == currentSelectedId }
                    .let { it >= 0 && listState.isFullyVisible(it) }
                if (point.hasDetails && point.itemId != currentSelectedId && !untouched && !selectedStillShown) {
                    onAction(RouteAction.OnPointSelect(point.itemId))
                }
            }
        }

        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(snapping),
            contentPadding = PaddingValues(horizontal = CARD_PEEK),
            horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(points, key = { it.itemId }) { point ->
                if (point.hasDetails) {
                    RoutePointCard(
                        point = point,
                        onEditClick = { onAction(RouteAction.OnPointEditClick(point.itemId)) },
                        onOpenPlaceClick = { mapEntryId -> onAction(RouteAction.OnOpenMapEntryClick(mapEntryId)) },
                        modifier = Modifier.width(cardWidth),
                    )
                } else {
                    RouteDriveThroughDot(
                        point = point,
                        isSelected = point.itemId == selectedPointId,
                        // Only flies there; a drive-through point is edited in the list (user, 2026-11-26)
                        onClick = { onAction(RouteAction.OnPointSelect(point.itemId)) },
                    )
                }
            }
        }
    }
}

/** The item whose start lies at the row's snap position (start padding). */
private fun LazyListState.alignedItemIndex(): Int? =
    layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset) }?.index

private fun LazyListState.isFullyVisible(index: Int): Boolean {
    val info = layoutInfo
    val item = info.visibleItemsInfo.find { it.index == index } ?: return false
    return item.offset >= info.viewportStartOffset && item.offset + item.size <= info.viewportEndOffset
}

/** A drive-through point without name and description: a small dot instead of a card. */
@Composable
private fun RouteDriveThroughDot(
    point: RoutePointUi,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(point.kind.labelRes())
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .size(DOT_SIZE)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            RoutePointBadge(kind = point.kind, stopNumber = point.stopNumber)
        }
    }
}

@Composable
private fun RoutePointCard(
    point: RoutePointUi,
    onEditClick: () -> Unit,
    onOpenPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
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
                            // A drive-through point without a name already shows its type as the name
                            stringResource(point.kind.labelRes()).takeIf { point.hasName },
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
                        val distance = formatDistanceAndTime(leg.meters, leg.minutes)
                        leg.toName?.let { stringResource(Res.string.route_leg, distance, it) } ?: distance
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
