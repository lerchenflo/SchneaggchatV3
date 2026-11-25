package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.LocationPickOverlay
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.MapAttributionBar
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.MapLocateButton
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.fitCameraTo
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.needsFit
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.toPosition
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.location.LocationAccuracy
import org.maplibre.compose.location.LocationRequest
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.route_point_move_title

/** Same start camera as the Schneaggmap: Vorarlberg and around. */
private val DEFAULT_CAMERA = CameraPosition(center = Position(longitude = 9.92, latitude = 47.32), zoom = 7.0)
private const val SINGLE_POINT_ZOOM = 13.0
private const val OWN_LOCATION_ZOOM = 12.0

/**
 * The route map with its fixed chrome: locate button, attribution strip and, while a point is
 * moved, the crosshair overlay. [content] (chips, cards) lies on top of the map and is hidden
 * while moving. The camera follows [RouteState.cameraRequest]; an empty route starts on the own
 * location once it is known.
 */
@Composable
fun RouteMap(
    state: RouteState,
    onAction: (RouteAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val styleUrl = state.mapStyleUrl
    if (styleUrl == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // The provider reads the permission when it is created, so a new grant needs a new provider
    val locationProvider = key(state.locationPermissionGranted) { rememberDefaultLocationProvider() }
    val locationState = rememberLocationState(
        enabled = state.locationPermissionGranted,
        provider = locationProvider,
        request = LocationRequest(from = LocationRequest.Standard) {
            accuracy = LocationAccuracy.BestForNavigation
        }
    )
    val ownPosition = locationState.lastLocation?.position

    // `content` is only invoked once the map composes it, after mapState is assigned
    lateinit var mapState: MapState
    mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = DEFAULT_CAMERA,
    ) {
        RouteMapLayers(state = state, ownPosition = ownPosition, onAction = onAction)
    }
    val scope = rememberCoroutineScope()

    RouteCameraEffects(state = state, mapState = mapState, ownPosition = ownPosition, onAction = onAction)

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            overlay = {},
            interactions = MapInteractions {
                callbacks {
                    longClick {
                        onEvent { event ->
                            event.position?.let { position ->
                                onAction(RouteAction.OnMapLongPress(LatLong(lat = position.latitude, long = position.longitude)))
                            }
                            ClickResult.Consume
                        }
                    }
                }
            }
        )

        if (state.isMovingPoint) {
            val center = mapState.cameraPosition.center
            LocationPickOverlay(
                cameraTarget = LatLong(lat = center.latitude, long = center.longitude),
                onConfirm = { onAction(RouteAction.OnMovePointConfirm(it)) },
                onCancel = { onAction(RouteAction.OnMovePointCancel) },
                title = stringResource(Res.string.route_point_move_title),
            )
        } else {
            content()

            MapLocateButton(
                mapState = mapState,
                isLocationAvailable = true,
                isFollowingLocation = false,
                onLocateClick = {
                    if (ownPosition != null) {
                        scope.launch { mapState.animateCamera(CameraUpdate(center = ownPosition, zoom = OWN_LOCATION_ZOOM)) }
                    } else {
                        onAction(RouteAction.OnLocateClick)
                    }
                },
                onResetRotationClick = {
                    scope.launch { mapState.animateCamera(CameraUpdate(bearing = 0.0, pitch = 0.0)) }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )
        }

        // Legally required OSM/OpenFreeMap copyright notice, always visible
        MapAttributionBar(mapState = mapState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun RouteCameraEffects(
    state: RouteState,
    mapState: MapState,
    ownPosition: Position?,
    onAction: (RouteAction) -> Unit,
) {
    LaunchedEffect(state.cameraRequest?.requestId) {
        when (val request = state.cameraRequest ?: return@LaunchedEffect) {
            is RouteCameraRequest.Fit -> {
                if (needsFit(request.positions)) {
                    mapState.fitCameraTo(request.positions, animate = true)
                } else {
                    request.positions.firstOrNull()?.let {
                        mapState.animateCamera(CameraUpdate(center = it.toPosition(), zoom = SINGLE_POINT_ZOOM))
                    }
                }
            }
            is RouteCameraRequest.Center -> mapState.animateCamera(CameraUpdate(center = request.position.toPosition()))
        }
        onAction(RouteAction.OnCameraRequestHandled)
    }

    // An empty route starts on the own location (user, 2026-11-24), once
    var hasCenteredOnOwnLocation by remember { mutableStateOf(false) }
    val routeIsEmpty = !state.isLoading && state.points.isEmpty()
    LaunchedEffect(routeIsEmpty, ownPosition) {
        if (!hasCenteredOnOwnLocation && routeIsEmpty && ownPosition != null) {
            hasCenteredOnOwnLocation = true
            mapState.animateCamera(CameraUpdate(center = ownPosition, zoom = OWN_LOCATION_ZOOM))
        }
    }
}
