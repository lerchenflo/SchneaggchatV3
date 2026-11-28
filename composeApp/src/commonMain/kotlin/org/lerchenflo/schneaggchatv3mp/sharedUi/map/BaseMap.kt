package org.lerchenflo.schneaggchatv3mp.sharedUi.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.location.LocationAccuracy
import org.maplibre.compose.location.LocationMeasurement
import org.maplibre.compose.location.LocationRequest
import org.maplibre.compose.location.LocationState
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.material3.DisappearingScaleBar
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Duration.Companion.milliseconds
import org.lerchenflo.schneaggchatv3mp.utilities.approximateDistanceMeters

/** Start camera of every map: Vorarlberg and around. */
val DEFAULT_MAP_CAMERA = CameraPosition(center = Position(longitude = 9.92, latitude = 47.32), zoom = 7.0)

private const val OWN_LOCATION_START_ZOOM = 14.0
private const val OWN_LOCATION_CLICK_ZOOM = 16.0

private const val ZOOM_SNAP_RADIUS_DP = 64.0

//Follow mode skips re-centering for moves smaller than this (GPS jitter while standing still).
private const val FOLLOW_MIN_MOVE_METERS = 2.0
private val FOLLOW_ANIMATION_DURATION = 500.milliseconds

/**
 * The map behind [BaseMap]: the maplibre [mapState], the own position and the "follow me" mode.
 * Made by [rememberBaseMapState].
 */
@Stable
class BaseMapState internal constructor(val mapState: MapState) {

    internal var locationState: LocationState? by mutableStateOf(null)
    internal var locationPermissionGranted by mutableStateOf(false)

    /** Own position from GPS; null without permission or before the first fix. */
    val ownLocation: LocationMeasurement? get() = locationState?.lastLocation

    //"Follow me" mode, toggled on by the locate button. Stops as soon as the user manually
    //pans/zooms the map - any GESTURE-driven camera move is treated as "I don't want to follow".
    var isFollowingLocation by mutableStateOf(false)
        private set

    //Bumped on every locate tap, so a tap while already following still restarts the follow loop
    //(re-zooms onto the own location) instead of being a no-op true -> true.
    internal var followRequest by mutableIntStateOf(0)
        private set

    internal var hasAutoCentered = false

    fun startFollowing() {
        isFollowingLocation = true
        followRequest++
    }

    fun stopFollowing() {
        isFollowingLocation = false
    }

    /** The screen moved the camera on purpose (deep link, focus): skip the start-up jump to the own location. */
    fun skipAutoCenter() {
        hasAutoCentered = true
    }
}

/**
 * The map of a [BaseMap] with its [layers] (the declared style content, e.g.
 * [org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapContentLayers] plus a screen's own) and
 * the own position, resolved once here so a screen does not open a second GPS subscription.
 */
@Composable
fun rememberBaseMapState(
    styleUrl: String,
    locationPermissionGranted: Boolean,
    initialCamera: CameraPosition = DEFAULT_MAP_CAMERA,
    layers: @Composable @MaplibreComposable (mapState: MapState, ownLocation: LocationMeasurement?) -> Unit,
): BaseMapState {
    //The provider reads the platform permission when it is created and afterwards only on activity
    //resume, so it is recreated whenever our own permission state flips to pick the grant up.
    val locationProvider = key(locationPermissionGranted) { rememberDefaultLocationProvider() }
    val locationState = rememberLocationState(
        enabled = locationPermissionGranted,
        provider = locationProvider,
        request = LocationRequest(
            from = LocationRequest.Standard
        ) {
            accuracy = LocationAccuracy.BestForNavigation
        }
    )

    //The map's sources/layers live on the MapState itself (declared through `content`) - the
    //library attaches map content to the logical map, not to whatever surface renders it.
    //`content` isn't invoked until the map actually composes it, by which point `mapState` below is
    //assigned - `lateinit` (rather than a forward `val` reference, which the compiler can't resolve
    //through the nested rememberMapState(...) { ... } call) makes that safe.
    lateinit var mapState: MapState
    mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = initialCamera,
    ) {
        layers(mapState, locationState.lastLocation)
    }

    val state = remember(mapState) { BaseMapState(mapState) }
    SideEffect {
        state.locationState = locationState
        state.locationPermissionGranted = locationPermissionGranted
    }
    return state
}

/**
 * Shows the crosshair "pick a coordinate" overlay instead of the map's chrome and content.
 * Plain class (lambdas make a data class's equality meaningless): `remember` it on the screen.
 */
class MapPickRequest(
    val title: String,
    val onConfirm: (LatLong) -> Unit,
    val onCancel: () -> Unit,
)

/**
 * The map every map screen shares: zoom slider on the right (snapping onto the own position and
 * [zoomSnapPositions]), scale bar, compass / locate button with follow mode, and the always visible
 * attribution strip. A screen adds [content] on top (search, chips, cards), [bottomCenter] between
 * the scale bar and the locate button and [bottomContent] above the attribution strip (friends
 * bar). [pickLocation] swaps all of that for the crosshair overlay. With
 * [centerOnOwnLocationOnce] the camera flies to the own position once, when it is first known.
 * With [onLocateWithoutPermission] the locate button shows without the location permission too,
 * and a tap then calls it instead of following (e.g. to ask for the permission).
 */
@Composable
fun BaseMap(
    state: BaseMapState,
    modifier: Modifier = Modifier,
    onMapClick: (LatLong) -> Unit = {},
    onMapLongClick: (LatLong) -> Unit = {},
    zoomSnapPositions: List<Position> = emptyList(),
    centerOnOwnLocationOnce: Boolean = false,
    onLocateWithoutPermission: (() -> Unit)? = null,
    pickLocation: MapPickRequest? = null,
    bottomCenter: @Composable BoxScope.() -> Unit = {},
    bottomContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
) {
    val mapState = state.mapState
    val ownLocation = state.ownLocation
    val scope = rememberCoroutineScope()

    AutoCenterEffect(state = state, enabled = centerOnOwnLocationOnce)
    FollowLocationEffect(state = state)

    //Candidates for zoom-snapping, e.g. every friend with a known location, plus our own live fix
    val zoomSnapCandidates = remember(zoomSnapPositions, ownLocation) {
        zoomSnapPositions + listOfNotNull(ownLocation?.position)
    }

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            overlay = {
                // Right edge: vertical zoom scrollbar. Its touch target is invisible but always mounted,
                // so it is placed first - every button drawn after it (here and in the top bar) sits above
                // it and keeps its taps where they overlap, e.g. on short/landscape screens.
                MapZoomSlider(
                    zoom = mapState.cameraPosition.zoom,
                    onZoomChange = { newZoom -> mapState.zoomTo(newZoom, zoomSnapCandidates) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )

                if (pickLocation == null) {
                    //Bottom column: map info row, screen content (friends bar), attribution line
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

                            bottomCenter()

                            // Right: compass + center to own location button. Shown as soon as location is
                            // permitted (a tap before the first fix starts following and zooms in once the
                            // fix arrives), while the map is rotated, or always when the screen can ask for
                            // the permission.
                            val hasLocation = state.locationPermissionGranted || ownLocation != null
                            MapLocateButton(
                                mapState = mapState,
                                isLocationAvailable = hasLocation || onLocateWithoutPermission != null,
                                isFollowingLocation = state.isFollowingLocation,
                                onLocateClick = {
                                    if (!hasLocation && onLocateWithoutPermission != null) {
                                        onLocateWithoutPermission()
                                    } else {
                                        state.startFollowing()
                                    }
                                },
                                onResetRotationClick = {
                                    scope.launch {
                                        mapState.animateCamera(CameraUpdate(bearing = 0.0, pitch = 0.0))
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }

                        bottomContent()

                        Spacer(modifier = Modifier.height(4.dp))

                        // Legally required OSM/OpenFreeMap copyright notice, always visible
                        MapAttributionBar(mapState = mapState)
                    }
                }
            },
            interactions = MapInteractions {
                callbacks {
                    click {
                        onUnhandled { event ->
                            event.position?.let { position ->
                                onMapClick(LatLong(position.latitude, position.longitude))
                            }

                            ClickResult.Pass
                        }
                    }
                    longClick {
                        onEvent { event ->
                            event.position?.let { position ->
                                onMapLongClick(LatLong(position.latitude, position.longitude))
                            }

                            ClickResult.Consume
                        }
                    }
                }
            }
        )

        if (pickLocation != null) {
            LocationPickOverlay(
                cameraTarget = LatLong(
                    lat = mapState.cameraPosition.center.latitude,
                    long = mapState.cameraPosition.center.longitude
                ),
                onConfirm = pickLocation.onConfirm,
                onCancel = pickLocation.onCancel,
                title = pickLocation.title,
            )
        } else {
            content()
        }
    }
}

//If a candidate (a friend, the own position) is sitting near the current screen center, zoom onto
//them (like Snap Map); otherwise just zoom in/out around the current map center.
private fun MapState.zoomTo(newZoom: Double, snapCandidates: List<Position>) {
    val currentTarget = cameraPosition.center
    val snapRadiusMeters = ZOOM_SNAP_RADIUS_DP * metersPerDpAtTarget

    val nearestCandidate = snapCandidates.minByOrNull { position ->
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

    setCameraPosition(
        cameraPosition.copy(
            center = snapTarget ?: currentTarget,
            zoom = newZoom
        )
    )
}

//Center on our own location once, on start - only where the screen asks for it (the Schneaggmap
//only while we share it, otherwise the user has no reason to expect the map to jump there).
@Composable
private fun AutoCenterEffect(state: BaseMapState, enabled: Boolean) {
    val ownLocation = state.ownLocation
    LaunchedEffect(ownLocation, enabled) {
        val position = ownLocation?.position
        if (!state.hasAutoCentered && enabled && position != null) {
            state.hasAutoCentered = true
            state.mapState.animateCamera(CameraUpdate(center = position, zoom = OWN_LOCATION_START_ZOOM))
        }
    }
}

@Composable
private fun FollowLocationEffect(state: BaseMapState) {
    val mapState = state.mapState
    LaunchedEffect(Unit) {
        snapshotFlow { mapState.cameraMoveReason }
            .collect { reason ->
                if (reason == CameraMoveReason.Gesture) {
                    state.stopFollowing()
                }
            }
    }
    LaunchedEffect(state.isFollowingLocation, state.followRequest) {
        if (!state.isFollowingLocation) return@LaunchedEffect

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
        val start = snapshotFlow { state.ownLocation?.position }.filterNotNull().first()
        animateSafely(CameraPosition(center = start, zoom = OWN_LOCATION_CLICK_ZOOM), CameraAnimation.Fly {})

        snapshotFlow { state.ownLocation?.position }
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
}
