package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lerchenflo.taptarget.tapTarget
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserLocation
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.FriendLocationsPreview
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapCreateChoiceDialog
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapEntryInfoCard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapSearchBar
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.SnailTrailLayers
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.UserInfoCard
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.BaseMap
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.MapPickRequest
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.fitCameraTo
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapContentLayers
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapFilterChip
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapFilterChipsRow
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapLayerClicks
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapLayersAction
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.rememberBaseMapState
import org.lerchenflo.schneaggchatv3mp.utilities.battery.BatteryService
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.inDegrees
import org.maplibre.spatialk.units.extensions.inMeters
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_pick_location_title
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_filter_snail_trails
import kotlin.math.roundToInt
import kotlin.time.Clock

private const val OWN_LOCATION_CLICK_ZOOM = 16.0
private const val ENTRY_FOCUS_ZOOM = 16.0

//Speed badge hysteresis: shows above the first speed, only hides again below the second, so GPS
//noise around a single threshold can't make it flicker.
private const val SPEED_BADGE_SHOW_MPS = 3.0
private const val SPEED_BADGE_HIDE_MPS = 2.0

@Composable
fun SchneaggmapScreenRoot(
    initialEntryId: String? = null,
    currentlyEditedEvent: Event? = null,
    focusArea: List<LatLong> = emptyList(),
    focusUserId: String? = null,
) {
    val viewModel = koinViewModel<SchneaggmapViewModel> { parametersOf(initialEntryId, currentlyEditedEvent, focusArea, focusUserId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    SchneaggmapScreen(
        state = state,
        onAction = viewModel::onAction,
    )


}

@Composable
fun SchneaggmapScreen(
    state: SchneaggmapState = SchneaggmapState(),
    onAction: (SchneaggmapAction) -> Unit = {},
) {
    val onLayersAction: (MapLayersAction) -> Unit = remember(onAction) {
        { action -> onAction(SchneaggmapAction.OnLayersAction(action)) }
    }
    val layerClicks = remember(onAction) {
        MapLayerClicks(
            onEntryClick = { onAction(SchneaggmapAction.OnEntryClick(it)) },
            onUserClick = { onAction(SchneaggmapAction.OnUserClick(it)) },
            onEventClick = { onAction(SchneaggmapAction.OnEventPinClick(it)) },
            onOwnUserClick = { onAction(SchneaggmapAction.OnOwnUserClick) },
        )
    }

    val baseMapState = rememberBaseMapState(
        styleUrl = state.mapStyleUrl,
        locationPermissionGranted = state.locationPermissionGranted,
    ) { mapState, ownLocation ->
        MapContentLayers(
            data = state.layers,
            display = state.layerDisplay,
            mapState = mapState,
            ownLocation = ownLocation,
            clicks = layerClicks,
            underPeople = {
                if (state.showSnailTrails) {
                    SnailTrailLayers(
                        usersWithLocation = state.layers.usersWithLocation,
                        snailTrails = state.snailTrails,
                        ownLocation = ownLocation,
                    )
                }
            },
        )
    }
    val mapState = baseMapState.mapState
    val ownLocation = baseMapState.ownLocation
    val scope = rememberCoroutineScope()

    //Zoom-snapping onto every friend with a known location (BaseMap adds our own live fix).
    //Own location comes from GPS directly (not ownUser.location, which is never synced back).
    val friendPositions = remember(state.layers.usersWithLocation) {
        state.layers.usersWithLocation.mapNotNull { user ->
            user.location?.let { loc -> Position(longitude = loc.long, latitude = loc.lat) }
        }
    }

    //Opened with a specific entry (deep link): fly to it once. Marks the own-location
    //auto-center as done so it can't yank the camera away from the entry afterwards.
    LaunchedEffect(state.focusEntryTarget) {
        state.focusEntryTarget?.let { target ->
            baseMapState.skipAutoCenter()
            mapState.animateCamera(
                CameraUpdate(
                    center = Position(longitude = target.long, latitude = target.lat),
                    zoom = ENTRY_FOCUS_ZOOM
                )
            )
            onAction(SchneaggmapAction.OnFocusEntryHandled)
        }
    }

    //Opened on several positions (a group's map preview): fit them all into view once
    LaunchedEffect(state.focusAreaTarget) {
        state.focusAreaTarget?.let { target ->
            baseMapState.skipAutoCenter()
            mapState.fitCameraTo(target, animate = true)
            onAction(SchneaggmapAction.OnFocusEntryHandled)
        }
    }

    //Hides the filter chips below the search bar while the full-screen search is open
    var isSearchExpanded by remember { mutableStateOf(false) }

    //Speed badge visibility with hysteresis (see SPEED_BADGE_SHOW_MPS / SPEED_BADGE_HIDE_MPS)
    var showSpeedBadge by remember { mutableStateOf(false) }
    val speedMps = ownLocation?.distancePerSecond?.inMeters
    LaunchedEffect(speedMps) {
        showSpeedBadge = when {
            speedMps == null -> false
            speedMps > SPEED_BADGE_SHOW_MPS -> true
            speedMps < SPEED_BADGE_HIDE_MPS -> false
            else -> showSpeedBadge
        }
    }

    val pickTitle = stringResource(Res.string.event_pick_location_title)
    val pickRequest = remember(state.pickLocationMode, pickTitle, onAction) {
        if (state.pickLocationMode) {
            MapPickRequest(
                title = pickTitle,
                onConfirm = { onAction(SchneaggmapAction.OnConfirmLocationPick(it)) },
                onCancel = { onAction(SchneaggmapAction.OnCancelLocationPick) },
            )
        } else null
    }

    BaseMap(
        state = baseMapState,
        onMapClick = { onAction(SchneaggmapAction.OnMapClick(it, longClick = false)) },
        onMapLongClick = { onAction(SchneaggmapAction.OnMapClick(it, longClick = true)) },
        zoomSnapPositions = friendPositions,
        //Only if we actually share our location - otherwise the user has no reason to expect the
        //map to jump there
        centerOnOwnLocationOnce = state.ownLocationShared,
        pickLocation = pickRequest,
        bottomCenter = {
            //Round speed indicator
            speedMps?.let { speed ->
                if (showSpeedBadge) {
                    val speedKmh = (speed * 3.6).roundToInt()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(56.dp)
                            .background(Color.White, CircleShape)
                            .border(width = 4.dp, color = Color.Red, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$speedKmh",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        },
        bottomContent = {
            if (state.layers.usersWithLocation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                    FriendLocationsPreview(
                        friends = state.layers.usersWithLocation,
                        onlineFriendIds = state.layers.onlineFriendIds,
                        pinnedFriendIds = state.pinnedFriendIds,
                        onUserClick = { user ->
                            val loc = user.location ?: return@FriendLocationsPreview
                            baseMapState.stopFollowing()
                            scope.launch {
                                mapState.animateCamera(
                                    CameraUpdate(
                                        center = Position(longitude = loc.long, latitude = loc.lat),
                                        zoom = OWN_LOCATION_CLICK_ZOOM
                                    )
                                )
                            }
                        }
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {

            //Map search bar with search and settings buttons inside
            MapSearchBar(
                state = state,
                onAction = onAction,
                onExpandedChange = { isSearchExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp, end = 4.dp)
            )

            //Quick filter chips, 8 dp below the search bar (4 dp row padding + 4 dp spacer)
            if (!isSearchExpanded) {
                MapFilterChipsRow(
                    display = state.layerDisplay,
                    onLayersAction = onLayersAction,
                    placesChipModifier = Modifier.tapTarget("schneaggmap_location_dropdown"),
                    isPlacesLoading = state.isLoading,
                    extraChips = {
                        MapFilterChip(
                            label = stringResource(Res.string.schneaggmap_filter_snail_trails),
                            icon = Icons.Outlined.Route,
                            selected = state.showSnailTrails,
                            onClick = { onAction(SchneaggmapAction.ToggleSnailTrails) },
                        )
                    },
                )
            }
        }

        //Only shown without pickLocationMode (BaseMap swaps the content for the pick overlay)
        state.selectedEntry?.let { entry ->
            MapEntryInfoCard(
                entry = entry,
                editorName = state.selectedEntryEditorName,
                onDismiss = {
                    onAction(SchneaggmapAction.OnPopupDismiss)
                },
                onSave = { changedEntry ->
                    onAction(SchneaggmapAction.OnEntryPopupSave(
                        entry = changedEntry,
                    ))
                },
                onDelete = {
                    onAction(SchneaggmapAction.OnEntryPopupDelete(it))
                },
                onQuickSave = { changedEntry ->
                    onAction(SchneaggmapAction.OnEntryPopupSave(
                        entry = changedEntry,
                        keepPopupOpen = true
                    ))
                }
            )
        }

        //Popup card from the bottom
        state.selectedUser?.let { user ->
            val isOwnUser = user.id == SessionCache.requireLoggedIn()?.userId
            val batteryService = koinInject<BatteryService>()

            //Own user has no synced `location` in the local DB (only ever pushed to the server,
            //never written back) - build it from the same live GPS fix the map puck already uses.
            val displayUser = if (isOwnUser) {
                user.copy(
                    location = ownLocation?.let { location ->
                        UserLocation(
                            lat = location.position.latitude,
                            long = location.position.longitude,
                            date = Clock.System.now().toEpochMilliseconds(),
                            speed = location.distancePerSecond?.inMeters,
                            heading = location.course?.let { bearing -> (bearing - Bearing.North).inDegrees },
                            altitude = location.position.altitude,
                            batteryLevel = batteryService.getBatteryLevel(),
                        )
                    }
                )
            } else {
                user
            }

            UserInfoCard(
                user = displayUser,
                isOnline = isOwnUser || user.id in state.layers.onlineFriendIds,
                ownLocation = ownLocation?.position?.let { position ->
                    LatLong(lat = position.latitude, long = position.longitude)
                },
                onDismiss = {
                    onAction(SchneaggmapAction.OnPopupDismiss)
                },
                onOpenChat = { clickedUser ->
                    onAction(SchneaggmapAction.OnOpenChatClick(clickedUser))
                },
                onOpenCompass = { clickedUser ->
                    onAction(SchneaggmapAction.OnOpenCompassClick(clickedUser))
                },
                isOwnUser = isOwnUser,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        state.currentlySelectedLocation?.let { location ->
            MapCreateChoiceDialog(
                onDismiss = { onAction(SchneaggmapAction.OnCreateChoiceDismiss) },
                onCreateMapEntry = { onAction(SchneaggmapAction.OnCreateMapEntryChoice(it)) },
                onCreateEvent = { onAction(SchneaggmapAction.OnCreateEventChoice(it)) },
                location = location
            )
        }
    }
}
