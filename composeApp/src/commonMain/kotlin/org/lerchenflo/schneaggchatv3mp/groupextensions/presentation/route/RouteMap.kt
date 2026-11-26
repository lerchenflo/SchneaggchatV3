package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.BaseMap
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.BaseMapState
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.MapPickRequest
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.fitCameraTo
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapContentLayers
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapLayerClicks
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.friendPositions
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.metersPerDpAtTarget
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.needsFit
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.rememberBaseMapState
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.toPosition
import org.maplibre.compose.camera.CameraUpdate
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.route_point_move_title

private const val SINGLE_POINT_ZOOM = 13.0
// A long press this close to a point counts as on it
private const val POINT_HIT_RADIUS_DP = 24.0

/**
 * The route map: the shared [BaseMap] with places, friends and events ([MapContentLayers]) and the
 * route on top of them ([RouteMapLayers]). [topContent] (chips) and [bottomContent] (cards, above
 * the locate button row) are hidden while a point is moved with the crosshair. The camera follows
 * [RouteState.cameraRequest]; an empty route starts on the own location once it is known.
 */
@Composable
fun RouteMap(
    state: RouteState,
    onAction: (RouteAction) -> Unit,
    modifier: Modifier = Modifier,
    bottomContent: @Composable ColumnScope.() -> Unit = {},
    topContent: @Composable BoxScope.() -> Unit = {},
) {
    val styleUrl = state.mapStyleUrl
    if (styleUrl == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Own position: no tap (user decision 2026-11-25)
    val layerClicks = remember(onAction) {
        MapLayerClicks(
            onEntryClick = { onAction(RouteAction.OnPlaceClick(it)) },
            onUserClick = { onAction(RouteAction.OnFriendClick(it)) },
            onEventClick = { onAction(RouteAction.OnEventClick(it)) },
        )
    }
    val baseMapState = rememberBaseMapState(
        styleUrl = styleUrl,
        locationPermissionGranted = state.locationPermissionGranted,
    ) { mapState, ownLocation ->
        MapContentLayers(
            data = state.layers,
            display = state.layerDisplay,
            mapState = mapState,
            ownLocation = ownLocation,
            clicks = layerClicks,
        )
        // Composed last, so the route lies on top of places, friends and events
        RouteMapLayers(state = state, onAction = onAction)
    }

    RouteCameraEffects(state = state, baseMapState = baseMapState, onAction = onAction)

    val friendPositions = remember(state.layers.usersWithLocation) { state.layers.friendPositions() }
    val moveTitle = stringResource(Res.string.route_point_move_title)
    val moveRequest = remember(state.isMovingPoint, moveTitle, onAction) {
        if (state.isMovingPoint) {
            MapPickRequest(
                title = moveTitle,
                onConfirm = { onAction(RouteAction.OnMovePointConfirm(it)) },
                onCancel = { onAction(RouteAction.OnMovePointCancel) },
            )
        } else null
    }

    BaseMap(
        state = baseMapState,
        modifier = modifier,
        onMapClick = { onAction(RouteAction.OnMapClick) },
        onMapLongClick = {
            onAction(RouteAction.OnMapLongPress(it, POINT_HIT_RADIUS_DP * baseMapState.mapState.metersPerDpAtTarget))
        },
        zoomSnapPositions = friendPositions,
        // An empty route starts on the own location (user, 2026-11-24)
        centerOnOwnLocationOnce = !state.isLoading && state.points.isEmpty(),
        // Without the permission the locate button asks for it
        onLocateWithoutPermission = { onAction(RouteAction.OnLocateClick) },
        pickLocation = moveRequest,
        bottomContent = bottomContent,
        content = topContent,
    )
}

@Composable
private fun RouteCameraEffects(
    state: RouteState,
    baseMapState: BaseMapState,
    onAction: (RouteAction) -> Unit,
) {
    val mapState = baseMapState.mapState
    LaunchedEffect(state.cameraRequest?.requestId) {
        val request = state.cameraRequest ?: return@LaunchedEffect
        // The route moved the camera on purpose: no later jump to the own location
        baseMapState.skipAutoCenter()
        when (request) {
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
}
