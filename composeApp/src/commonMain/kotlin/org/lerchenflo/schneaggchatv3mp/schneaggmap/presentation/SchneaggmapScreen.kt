package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserLocation
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.CoordinateView
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.FriendLocationsPreview
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapCreateChoiceDialog
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapAttributionBar
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapEntryInfoCard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapFilterChipsRow
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapLocateButton
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapSearchBar
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapZoomSlider
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.SchneaggmapLayers
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.UserInfoCard
import org.lerchenflo.schneaggchatv3mp.utilities.battery.BatteryService
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.location.LocationAccuracy
import org.maplibre.compose.location.LocationRequest
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.material3.DisappearingScaleBar
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlayScope
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.inDegrees
import org.maplibre.spatialk.units.extensions.inMeters
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.cancel
import schneaggchatv3mp.composeapp.generated.resources.event_pick_location_hint
import schneaggchatv3mp.composeapp.generated.resources.event_pick_location_title
import schneaggchatv3mp.composeapp.generated.resources.event_use_this_location
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private const val OWN_LOCATION_START_ZOOM = 14.0
private const val OWN_LOCATION_CLICK_ZOOM = 16.0
private const val ENTRY_FOCUS_ZOOM = 16.0

private const val ZOOM_SNAP_RADIUS_DP = 64.0

//Speed badge hysteresis: shows above the first speed, only hides again below the second, so GPS
//noise around a single threshold can't make it flicker.
private const val SPEED_BADGE_SHOW_MPS = 3.0
private const val SPEED_BADGE_HIDE_MPS = 2.0

//Follow mode skips re-centering for moves smaller than this (GPS jitter while standing still).
private const val FOLLOW_MIN_MOVE_METERS = 2.0
private val FOLLOW_ANIMATION_DURATION = 500.milliseconds

@Composable
fun SchneaggmapScreenRoot(
    initialEntryId: String? = null,
    currentlyEditedEvent: Event? = null
) {
    val viewModel = koinViewModel<SchneaggmapViewModel> { parametersOf(initialEntryId, currentlyEditedEvent) }
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

    //Own position, resolved once here (not in SchneaggmapLayers) so we don't open a second,
    //redundant GPS subscription just to also show the speed readout below.
    //The provider reads the platform permission when it is created and afterwards only on activity
    //resume, so it is recreated whenever our own permission state flips to pick the grant up.
    val locationProvider = key(state.locationPermissionGranted) { rememberDefaultLocationProvider() }
    val locationState = rememberLocationState(
        enabled = state.locationPermissionGranted,
        provider = locationProvider,
        request = LocationRequest(
            from = LocationRequest.Standard
        ) {
                accuracy = LocationAccuracy.BestForNavigation
        }
    )
    val ownLocation = locationState.lastLocation

    //The map's sources/layers now live on the MapState itself (declared through `content`) instead
    //of as a trailing lambda on MaplibreMap - the current library attaches map content to the
    //logical map, not to whatever surface happens to render it. `mapState` is captured by this
    //closure (not invoked until the map actually composes it), so the forward reference is safe.
    //`content` isn't invoked until the map actually composes it, by which point `mapState` below is
    //assigned - `lateinit` (rather than a forward `val` reference, which the compiler can't resolve
    //through the nested rememberMapState(...) { ... } call) makes that safe.
    lateinit var mapState: MapState
    mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(state.mapStyleUrl),
        initialCameraPosition = CameraPosition(
            center = Position(9.92, 47.32),
            zoom = 7.0,
        )
    ) {
        SchneaggmapLayers(state = state, mapState = mapState, ownLocation = ownLocation, onAction = onAction)
    }
    val scope = rememberCoroutineScope()



    //Candidates for zoom-snapping: every friend with a known location plus our own live fix.
    //Own location comes from GPS directly (not state.ownUser.location, which is never synced back).
    val zoomSnapCandidates = remember(state.usersWithLocation, ownLocation) {
        buildList {
            state.usersWithLocation.forEach { user ->
                user.location?.let { loc -> add(Position(longitude = loc.long, latitude = loc.lat)) }
            }
            ownLocation?.position?.let { add(it) }
        }
    }

    //Center on our own location once, on start, but only if we actually share it - otherwise
    //the user has no reason to expect the map to jump there.
    var hasAutoCentered by remember { mutableStateOf(false) }
    LaunchedEffect(ownLocation, state.ownLocationShared) {
        val position = ownLocation?.position
        if (!hasAutoCentered && state.ownLocationShared && position != null) {
            hasAutoCentered = true
            mapState.animateCamera(CameraUpdate(center = position, zoom = OWN_LOCATION_START_ZOOM))
        }
    }

    //Opened with a specific entry (deep link): fly to it once. Marks the own-location
    //auto-center as done so it can't yank the camera away from the entry afterwards.
    LaunchedEffect(state.focusEntryTarget) {
        state.focusEntryTarget?.let { target ->
            hasAutoCentered = true
            mapState.animateCamera(
                CameraUpdate(
                    center = Position(longitude = target.long, latitude = target.lat),
                    zoom = ENTRY_FOCUS_ZOOM
                )
            )
            onAction(SchneaggmapAction.OnFocusEntryHandled)
        }
    }

    //"Follow me" mode, toggled on by the locate button. Stops as soon as the user manually
    //pans/zooms the map - any GESTURE-driven camera move is treated as "I don't want to follow".
    var isFollowingLocation by remember { mutableStateOf(false) }
    //Bumped on every locate tap, so a tap while already following still restarts the effect below
    //(re-zooms onto the own location) instead of being a no-op true -> true.
    var followRequest by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        snapshotFlow { mapState.cameraMoveReason }
            .collect { reason ->
                if (reason == CameraMoveReason.Gesture) {
                    isFollowingLocation = false
                }
            }
    }
    LaunchedEffect(isFollowingLocation, followRequest) {
        if (!isFollowingLocation) return@LaunchedEffect

        //Every newer camera command (zoom slider, compass tap, ...) cancels a running camera
        //animation with a CancellationException. That must only abort this one animation, not the
        //whole follow loop - ensureActive() still rethrows if this effect itself got cancelled.
        suspend fun animateSafely(position: CameraPosition, animation: CameraAnimation) {
            try {
                mapState.animateCamera(position.toCameraUpdate(), animation)
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }

        //Zoom in once when following starts (waiting for the first fix if there is none yet), then
        //keep re-centering on the latest location at whatever zoom/bearing the user leaves it at.
        val start = snapshotFlow { locationState.lastLocation?.position }.filterNotNull().first()
        animateSafely(CameraPosition(center = start, zoom = OWN_LOCATION_CLICK_ZOOM), CameraAnimation.Fly {})

        snapshotFlow { locationState.lastLocation?.position }
            .filterNotNull()
            .collect { position ->
                //Let another command (e.g. the compass turning back north) finish first - starting
                //a re-center from its half-done camera would cancel it and freeze it midway.
                snapshotFlow { mapState.isCameraMoving }.first { !it }

                val current = mapState.cameraPosition
                val movedMeters = approximateDistanceMeters(
                    current.center.latitude, current.center.longitude,
                    position.latitude, position.longitude
                )
                if (movedMeters < FOLLOW_MIN_MOVE_METERS) return@collect

                animateSafely(current.copy(center = position), CameraAnimation.Ease() {
                    duration = FOLLOW_ANIMATION_DURATION
                })
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

    val bottomMapChrome: @Composable MapOverlayScope.() -> Unit = {

        // Right edge: vertical zoom scrollbar. Its touch target is invisible but always mounted,
        // so it is placed first - every button drawn after it (here and in the top bar) sits above
        // it and keeps its taps where they overlap, e.g. on short/landscape screens.
        MapZoomSlider(
            zoom = mapState.cameraPosition.zoom,
            onZoomChange = { newZoom ->
                val currentTarget = mapState.cameraPosition.center
                val snapRadiusMeters = ZOOM_SNAP_RADIUS_DP * mapState.metersPerDpAtTarget

                //If a user is sitting near the current screen center, zoom onto them (like Snap Map);
                //otherwise just zoom in/out around the current map center.
                val nearestCandidate = zoomSnapCandidates.minByOrNull { position ->
                    approximateDistanceMeters(
                        currentTarget.latitude, currentTarget.longitude,
                        position.latitude, position.longitude
                    )
                }
                val snapTarget = nearestCandidate?.takeIf { position ->
                    approximateDistanceMeters(
                        currentTarget.latitude, currentTarget.longitude,
                        position.latitude, position.longitude
                    ) <= snapRadiusMeters
                }

                mapState.setCameraPosition(
                    mapState.cameraPosition.copy(
                        center = snapTarget ?: currentTarget,
                        zoom = newZoom
                    )
                )
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        if (!state.pickLocationMode) {
            //Bottom column: map info row, friends bar, attribution line
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    // Left: scale bar, only visible while zooming
                    DisappearingScaleBar(
                        metersPerDp = { mapState.metersPerDpAtTarget },
                        color = MaterialTheme.colorScheme.background,
                        zoom = { mapState.cameraPosition.zoom },
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

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

                    // Right: compass + center to own location button. Shown as soon as location is
                    // permitted (a tap before the first fix starts following and zooms in once the
                    // fix arrives) or while the map is rotated.
                    MapLocateButton(
                        mapState = mapState,
                        isLocationAvailable = state.locationPermissionGranted || ownLocation != null,
                        isFollowingLocation = isFollowingLocation,
                        onLocateClick = {
                            isFollowingLocation = true
                            followRequest++
                        },
                        onResetRotationClick = {
                            scope.launch {
                                mapState.animateCamera(CameraUpdate(bearing = 0.0, pitch = 0.0))
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }

                if (state.usersWithLocation.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        FriendLocationsPreview(
                            friends = state.usersWithLocation,
                            onlineFriendIds = state.onlineFriendIds,
                            onUserClick = { user ->
                                val loc = user.location ?: return@FriendLocationsPreview
                                isFollowingLocation = false
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

                Spacer(modifier = Modifier.height(4.dp))

                // Legally required OSM/OpenFreeMap copyright notice, always visible
                MapAttributionBar(mapState = mapState)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            overlay = bottomMapChrome,
            interactions = MapInteractions {
                callbacks {
                    click {
                        onUnhandled { event ->
                            event.position?.let { position ->
                                onAction(SchneaggmapAction.OnMapClick(LatLong(position.latitude, position.longitude), longClick = false))
                            }

                            ClickResult.Pass
                        }
                    }
                    longClick {
                        onEvent { event ->
                            event.position?.let { position ->
                                onAction(SchneaggmapAction.OnMapClick(LatLong(position.latitude, position.longitude), longClick = true))
                            }

                            ClickResult.Consume
                        }
                    }
                }
            }
        )


        if (state.pickLocationMode) {
            LocationPickOverlay(
                cameraTarget = LatLong(
                    lat = mapState.cameraPosition.center.latitude,
                    long = mapState.cameraPosition.center.longitude
                ),
                onConfirm = { onAction(SchneaggmapAction.OnConfirmLocationPick(it)) },
                onCancel = { onAction(SchneaggmapAction.OnCancelLocationPick) },
            )
        } else {
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
                    //Spacer(modifier = Modifier.height(4.dp))

                    MapFilterChipsRow(
                        state = state,
                        onAction = onAction,
                    )
                }
            }
        }





        if (!state.pickLocationMode) {
            state.selectedEntry?.let { entry ->
                MapEntryInfoCard(
                    entry = entry,
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
                    isOnline = isOwnUser || user.id in state.onlineFriendIds,
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
}

/**
 * Focused "pick a coordinate" chrome shown instead of the normal top bar/dropdowns while
 * [SchneaggmapState.pickLocationMode] is true. The pin stays fixed at the screen center; the
 * user places it by panning the map underneath (like a typical address picker), so there's no
 * tap/drag handling here - [cameraTarget] is just the live camera center converted to [LatLong].
 */
@Composable
private fun LocationPickOverlay(
    cameraTarget: LatLong,
    onConfirm: (LatLong) -> Unit,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onCancel,
                colors = IconButtonDefaults.iconButtonColors().copy(
                    containerColor = MaterialTheme.colorScheme.background
                )
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(Res.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Text(
                    text = stringResource(Res.string.event_pick_location_title),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .offset(y = (-24).dp),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.event_pick_location_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                CoordinateView(coordinates = cameraTarget)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onConfirm(cameraTarget) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.event_use_this_location))
                }
            }
        }
    }
}
