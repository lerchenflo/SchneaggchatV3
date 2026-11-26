package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.navigation.ObserveAsEvents
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteTransportMode
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.CountSegmentedSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.TitleMenuItem
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_extension_not_found
import schneaggchatv3mp.composeapp.generated.resources.route_open_navigation
import schneaggchatv3mp.composeapp.generated.resources.route_tab_list
import schneaggchatv3mp.composeapp.generated.resources.route_tab_map

@Composable
fun RouteScreenRoot(
    extensionId: String,
    topBarMenuItems: List<TitleMenuItem> = emptyList(),
) {
    val viewModel = koinViewModel<RouteViewModel> {
        parametersOf(extensionId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            // No app or browser for the link: nothing to open, nothing to report
            is RouteEvent.OpenUrl -> runCatching { uriHandler.openUri(event.url) }
        }
    }

    RouteScreen(
        state = state,
        onAction = viewModel::onAction,
        topBarMenuItems = topBarMenuItems,
    )
}

/**
 * The route planner (layout "M3", user 2026-11-24): a map with day chips and swipeable point
 * cards, or the points as one card per day, switched like the tabs of Money split.
 */
@Composable
fun RouteScreen(
    state: RouteState,
    onAction: (RouteAction) -> Unit,
    topBarMenuItems: List<TitleMenuItem> = emptyList(),
) {
    val isReady = !state.isLoading && !state.extensionMissing

    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = state.title,
            onBackClick = { onAction(RouteAction.OnBackClick) },
            actions = {
                if (isReady && state.points.isNotEmpty()) {
                    IconButton(onClick = { onAction(RouteAction.OnNavigateClick) }) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = stringResource(Res.string.route_open_navigation)
                        )
                    }
                }
            },
            menuItems = if (isReady) topBarMenuItems else emptyList()
        )

        if (isReady && !state.isMovingPoint) {
            CountSegmentedSwitch(
                options = RouteTab.entries,
                selected = state.selectedTab,
                label = { tab ->
                    when (tab) {
                        RouteTab.MAP -> stringResource(Res.string.route_tab_map)
                        RouteTab.LIST -> stringResource(Res.string.route_tab_list)
                    }
                },
                count = { tab -> if (tab == RouteTab.LIST) state.points.size else null },
                onSelect = { onAction(RouteAction.OnTabSelect(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        HorizontalDivider()

        when {
            state.isLoading -> CenteredBox { CircularProgressIndicator() }
            state.extensionMissing -> CenteredBox { Text(stringResource(Res.string.group_extension_not_found)) }
            // The map stays composed under the list, so its camera survives a tab switch
            else -> Box(modifier = Modifier.fillMaxSize()) {
                RouteMapTab(state = state, onAction = onAction)
                // Moving a point needs the map, also when its sheet was opened from the list
                if (state.selectedTab == RouteTab.LIST && !state.isMovingPoint) {
                    RouteDayList(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            // Keeps taps and long presses on the list away from the map below
                            .pointerInput(Unit) {}
                    )
                }
            }
        }
    }

    if (isReady) {
        state.pointDraft?.takeIf { !state.isMovingPoint }?.let { draft ->
            RoutePointSheet(draft = draft, onAction = onAction)
        }
        if (state.isReorderVisible) {
            RouteReorderSheet(
                points = state.points,
                onSave = { onAction(RouteAction.OnReorderSave(it)) },
                onDismiss = { onAction(RouteAction.OnReorderDismiss) },
            )
        }
    }
}

@Composable
private fun RouteMapTab(
    state: RouteState,
    onAction: (RouteAction) -> Unit,
) {
    RouteMap(state = state, onAction = onAction) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp)
        ) {
            RouteDayChips(state = state, onAction = onAction, showPlacesChip = true)
            if (state.points.isNotEmpty()) {
                RouteSummaryPill(
                    meters = state.shownMeters,
                    minutes = state.shownMinutes,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp)
                )
            }
        }

        // Above the attribution strip, which must stay visible
        val bottomModifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 28.dp)
        val selectedPlace = state.selectedPlace
        when {
            selectedPlace != null -> RoutePlaceCard(
                place = selectedPlace,
                insertHint = state.selectedPlaceInsertHint,
                onAddClick = { onAction(RouteAction.OnPlaceAddClick) },
                onDismiss = { onAction(RouteAction.OnPlaceDismiss) },
                modifier = bottomModifier.padding(horizontal = 12.dp)
            )
            state.points.isEmpty() -> RouteEmptyMapHint(modifier = bottomModifier.padding(horizontal = 24.dp))
            // A new day shows its own cards from the first one
            else -> key(state.selectedDayIndex) {
                RoutePointCards(
                    points = state.shownPoints,
                    selectedPointId = state.selectedPointId,
                    onAction = onAction,
                    modifier = bottomModifier
                )
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun RouteScreenListPreview() {
    fun point(id: String, kind: RoutePointKind, number: Int?, name: String, description: String, day: Int) = RoutePointUi(
        itemId = id,
        kind = kind,
        stopNumber = number,
        name = name,
        description = description,
        coordinates = LatLong(47.15, 9.82),
        plannedAt = null,
        dayIndex = day,
        authorName = "Joni",
        arrivalMode = if (id == "1") null else RouteTransportMode.MOTORBIKE,
        nextLeg = null,
        mapEntryId = null,
        isMapEntryAvailable = false,
        canDelete = true,
    )
    SchneaggchatTheme {
        RouteScreen(
            state = RouteState(
                title = "Silvretta-Runde",
                isLoading = false,
                selectedTab = RouteTab.LIST,
                points = listOf(
                    point("1", RoutePointKind.STOP, 1, "Bludenz Bahnhof", "Treffpunkt, volltanken", 0),
                    point("2", RoutePointKind.WAYPOINT, null, "St. Gallenkirch", "", 0),
                    point("3", RoutePointKind.WAYPOINT, null, "Partenen", "", 0),
                    point("4", RoutePointKind.POI, null, "Bielerhöhe", "Fotostopp Silvretta-Stausee", 0),
                    point("5", RoutePointKind.ACCOMMODATION, null, "Galtür", "Alpenhotel, 3 Zimmer", 0),
                    point("6", RoutePointKind.STOP, 2, "Lech", "Mittagessen", 1),
                ),
                days = listOf(
                    RouteDayUi(index = 0, date = LocalDate(2027, 6, 12), meters = 43_000.0, minutes = 65),
                    RouteDayUi(index = 1, date = LocalDate(2027, 6, 13), meters = 27_000.0, minutes = 40),
                ),
                totalMeters = 70_000.0,
                totalMinutes = 110,
            ),
            onAction = {}
        )
    }
}
