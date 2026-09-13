package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.onboarding.tapTarget
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserLocation
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.CoordinateView
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.FriendLocationsPreview
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapCreateChoiceDialog
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapEntryInfoCard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapSearchBar
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapStyleDropdown
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapZoomSlider
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.SchneaggmapLayers
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.ShownLocationsDropdown
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.UserInfoCard
import org.lerchenflo.schneaggchatv3mp.utilities.battery.BatteryService
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.location.LocationAccuracy
import org.maplibre.compose.location.LocationRequest
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.material3.AttributionButtonDefaults
import org.maplibre.compose.material3.DisappearingCompassButton
import org.maplibre.compose.material3.DisappearingScaleBar
import org.maplibre.compose.material3.ExpandingAttributionButton
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

private const val OWN_LOCATION_START_ZOOM = 14.0
private const val OWN_LOCATION_CLICK_ZOOM = 16.0
private const val ENTRY_FOCUS_ZOOM = 16.0

private const val ZOOM_SNAP_RADIUS_DP = 64.0

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
        request = LocationRequest(accuracy = LocationAccuracy.BestForNavigation),
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
            target = Position(9.92, 47.32),
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
            mapState.animateCameraPosition(CameraPosition(target = position, zoom = OWN_LOCATION_START_ZOOM))
        }
    }

    //Opened with a specific entry (deep link): fly to it once. Marks the own-location
    //auto-center as done so it can't yank the camera away from the entry afterwards.
    LaunchedEffect(state.focusEntryTarget) {
        state.focusEntryTarget?.let { target ->
            hasAutoCentered = true
            mapState.animateCameraPosition(
                CameraPosition(
                    target = Position(longitude = target.long, latitude = target.lat),
                    zoom = ENTRY_FOCUS_ZOOM
                )
            )
            onAction(SchneaggmapAction.OnFocusEntryHandled)
        }
    }

    //"Follow me" mode, toggled on by the locate button. Stops as soon as the user manually
    //pans/zooms the map - any GESTURE-driven camera move is treated as "I don't want to follow".
    var isFollowingLocation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { mapState.cameraMoveReason }
            .collect { reason ->
                if (reason == CameraMoveReason.GESTURE) {
                    isFollowingLocation = false
                }
            }
    }
    LaunchedEffect(isFollowingLocation) {
        if (!isFollowingLocation) return@LaunchedEffect

        //Zoom in once when following starts, then keep re-centering on the latest location
        //at whatever zoom the user leaves it at.
        ownLocation?.position?.let { position ->
            mapState.animateCameraPosition(CameraPosition(target = position, zoom = OWN_LOCATION_CLICK_ZOOM))
        }

        snapshotFlow { locationState.lastLocation }
            .collect { location ->
                location?.position?.let { position ->
                    mapState.animateCameraPosition(mapState.cameraPosition.copy(target = position))
                }
            }
    }

    //Compass + attribution now render from inside MaplibreMap's own overlay scope (the new
    //library API reads the map state implicitly instead of taking cameraState/styleState params),
    //so this whole bottom chrome column is passed down separately instead of being
    //drawn as a sibling on top of it. Positioning (BottomCenter of the full map area) is unchanged.
    val bottomMapChrome: @Composable MapOverlayScope.() -> Unit = {
        //Captured once so the MapOverlayScope extensions below (ExpandingAttributionButton,
        //DisappearingCompassButton) can be called with an explicit receiver from inside the nested
        //Row/Box further down - Compose's DSL scope markers block resolving them implicitly once a
        //RowScope/BoxScope becomes the nearer receiver.
        val overlayScope = this
        if (!state.pickLocationMode) {
            //Bottom column
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {

                        //Center to own location button
                        ownLocation?.let {
                            SmallFloatingActionButton(
                                onClick = { isFollowingLocation = true },
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                if (state.usersWithLocation.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    FriendLocationsPreview(
                        friends = state.usersWithLocation,
                        onlineFriendIds = state.onlineFriendIds,
                        onUserClick = { user ->
                            val loc = user.location ?: return@FriendLocationsPreview
                            isFollowingLocation = false
                            scope.launch {
                                mapState.animateCameraPosition(
                                    CameraPosition(
                                        target = Position(longitude = loc.long, latitude = loc.lat),
                                        zoom = OWN_LOCATION_CLICK_ZOOM
                                    )
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))


                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Left: Scale bar + attribution
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        // Legally required OSM/OpenFreeMap copyright notice.
                        overlayScope.ExpandingAttributionButton(
                            contentAlignment = Alignment.BottomStart,
                            //The library starts this expanded (shows the full popup) rather than
                            //just the collapsed icon - collapse it right away so it only opens
                            //when tapped, same as the collapse that already happens on any map
                            //gesture.
                            toggleButton = { onClick ->
                                LaunchedEffect(Unit) { onClick() }
                                AttributionButtonDefaults.button(onClick)
                            },
                        )

                        DisappearingScaleBar(
                            metersPerDp = mapState.metersPerDpAtTarget,
                            color = MaterialTheme.colorScheme.background,
                            zoom = mapState.cameraPosition.zoom
                        )

                    }

                    //compass
                    overlayScope.DisappearingCompassButton(
                        size = 32.dp
                    )

                    //Round speed indicator
                    ownLocation?.distancePerSecond?.let { speed ->
                        if (speed.inMeters > 3) {
                            val speedKmh = (speed.inMeters * 3.6).roundToInt()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
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

                    // Right: Snail trails toggle
                    Card(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .tapTarget("schneaggmap_snailtrail_switch")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Polyline,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = state.showSnailTrails,
                                onCheckedChange = { onAction(SchneaggmapAction.ToggleSnailTrails) },
                            )
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            //The root Scaffold (App.kt) already insets its content for the system bars/IME and
            //pads this screen accordingly - without this override, the overlay defaults to
            //safe-drawing insets of its own on top of that, doubling the gap under the bottom
            //chrome (FABs, snail-trail toggle, etc.).
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            //The bottom chrome (FABs, friend preview, scale bar, attribution, compass, snail-trail
            //toggle) is composed here since it needs this MapOverlayScope receiver to draw the
            //compass and attribution buttons. The map's sources/layers are declared on `mapState`
            //itself (see its `content` above), not here.
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
                    lat = mapState.cameraPosition.target.latitude,
                    long = mapState.cameraPosition.target.longitude
                ),
                onConfirm = { onAction(SchneaggmapAction.OnConfirmLocationPick(it)) },
                onCancel = { onAction(SchneaggmapAction.OnCancelLocationPick) },
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(4.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {


                    //Settingsbutton
                    IconButton(
                        onClick = { onAction(SchneaggmapAction.OnSettingsClick) },
                        colors = IconButtonDefaults.iconButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.tapTarget("schneaggmap_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    //Map Search bar with user info button
                    MapSearchBar(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    ShownLocationsDropdown(
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.tapTarget("schneaggmap_location_dropdown")
                    )
                }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        //Map style dropdown menu
                        MapStyleDropdown(
                            state = state,
                            onAction = onAction,
                        )


                        //Own user detail button
                        SmallFloatingActionButton(
                            onClick = { onAction(SchneaggmapAction.OnOwnUserClick) },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null
                            )
                        }
                    }


                }

            }
        }





        // Right edge: vertical zoom scrollbar, centered between the top and bottom rows so it
        // never collides with the filter dropdown above or the snail-trail toggle below.
        MapZoomSlider(
            zoom = mapState.cameraPosition.zoom,
            onZoomChange = { newZoom ->
                val currentTarget = mapState.cameraPosition.target
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
                        target = snapTarget ?: currentTarget,
                        zoom = newZoom
                    )
                )
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        if (!state.pickLocationMode) {
            //Popup cards from the bottom
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
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

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
