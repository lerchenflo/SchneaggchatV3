package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.*
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.material3.DisappearingCompassButton
import org.maplibre.compose.material3.DisappearingScaleBar
import org.maplibre.compose.material3.ExpandingAttributionButton
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.StyleState
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.Coordinate
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.MapLocation
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle

private val USER_MARKER_COLOR = Color(0xFF607D8B)
private val CLUSTER_COLOR = Color(0xFF424242)

@Composable
fun SchneaggmapScreenRoot(
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<SchneaggmapViewModel>()
    SchneaggmapScreen(
        modifier = modifier,
        state = viewModel.state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun SchneaggmapScreen(
    modifier: Modifier = Modifier,
    state: SchneaggmapState = SchneaggmapState(),
    onAction: (SchneaggmapAction) -> Unit = {},
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(16.3738, 48.2082),
            zoom = 12.0,
        )
    )
    val styleState = rememberStyleState()

    Column(modifier = modifier.fillMaxSize()) {
        ActivityTitle(
            title = "Schneaggmap",
            onBackClick = { onAction(SchneaggmapAction.OnBackClicked) },
        )
        Box(modifier = Modifier.fillMaxSize()) {
            SchneaggmapMapContent(
                state = state,
                cameraState = cameraState,
                styleState = styleState,
                onAction = onAction,
            )
            state.selectedLocation?.let { location ->
                MapMarkerOverlay(coordinate = location.coordinate, cameraState = cameraState) {
                    LocationInfoCard(
                        location = location,
                        onDismiss = { onAction(SchneaggmapAction.SelectLocation(null)) },
                        modifier = Modifier.offset(y = (-80).dp),
                    )
                }
            }
            DisappearingCompassButton(
                cameraState = cameraState,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            )
            DisappearingScaleBar(
                metersPerDp = cameraState.metersPerDpAtTarget,
                zoom = cameraState.position.zoom,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp),
            )
            ExpandingAttributionButton(
                cameraState = cameraState,
                styleState = styleState,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
            )
            FilterPanel(
                state = state,
                onAction = onAction,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
        }
    }
}

@Composable
private fun SchneaggmapMapContent(
    state: SchneaggmapState,
    cameraState: CameraState,
    styleState: StyleState,
    onAction: (SchneaggmapAction) -> Unit,
) {
    val placeFeatures = state.placeLocations.mapIndexed { index, loc ->
        Feature(
            geometry = Point(Position(loc.coordinate.lon, loc.coordinate.lat)),
            properties = buildJsonObject {
                put("locationType", loc.type.name)
            },
            id = JsonPrimitive(index),
        )
    }
    val userFeatures = state.userLocations.mapIndexed { index, loc ->
        Feature(
            geometry = Point(Position(loc.coordinate.lon, loc.coordinate.lat)),
            properties = buildJsonObject { put("username", loc.username) },
            id = JsonPrimitive("user_$index"),
        )
    }

    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        cameraState = cameraState,
        styleState = styleState,
    ) {
        val placeSource = rememberGeoJsonSource(
            data = GeoJsonData.Features(FeatureCollection(placeFeatures)),
            options = GeoJsonOptions(cluster = true, clusterRadius = 50, clusterMaxZoom = 14),
        )
        val userSource = rememberGeoJsonSource(
            data = GeoJsonData.Features(FeatureCollection(userFeatures)),
        )

        // Individual place markers — one layer per type for independent toggle visibility
        LocationType.entries.forEach { type ->
            CircleLayer(
                id = "place_${type.name}",
                source = placeSource,
                visible = type in state.enabledTypes,
                filter = !feature.has("cluster") and
                    (feature["locationType"].asString() eq const(type.name)),
                color = const(type.markerColor),
                radius = const(10.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp),
                onClick = { features ->
                    val index = features.firstOrNull()?.id?.content?.toIntOrNull()
                    val location = index?.let { state.placeLocations.getOrNull(it) }
                    if (location != null) onAction(SchneaggmapAction.SelectLocation(location))
                    ClickResult.Consume
                },
            )
        }

        // Cluster bubble
        CircleLayer(
            id = "place_clusters",
            source = placeSource,
            filter = feature.has("cluster"),
            color = const(CLUSTER_COLOR),
            radius = const(18.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp),
            onClick = { ClickResult.Consume },
        )

        // Cluster count label
        SymbolLayer(
            id = "place_cluster_count",
            source = placeSource,
            filter = feature.has("cluster"),
            textField = format(span(feature["point_count_abbreviated"].asString())),
            textColor = const(Color.White),
            textSize = const(12.sp),
        )

        // User location markers
        CircleLayer(
            id = "layer_users",
            source = userSource,
            color = const(USER_MARKER_COLOR),
            radius = const(12.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(3.dp),
            onClick = { features ->
                val index = features.firstOrNull()?.id?.content?.removePrefix("user_")?.toIntOrNull()
                val location = index?.let { state.userLocations.getOrNull(it) }
                if (location != null) onAction(SchneaggmapAction.SelectLocation(location))
                ClickResult.Consume
            },
        )
    }
}

/**
 * Overlays [content] at the given map [coordinate], tracking the camera.
 * Remove from composition to hide.
 */
@Composable
fun BoxScope.MapMarkerOverlay(
    coordinate: Coordinate,
    cameraState: CameraState,
    content: @Composable () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE")
    val cameraPosition = cameraState.position
    val projection = cameraState.projection ?: return
    val screenOffset = projection.screenLocationFromPosition(
        Position(coordinate.lon, coordinate.lat)
    )
    Box(Modifier.offset(x = screenOffset.x, y = screenOffset.y)) {
        content()
    }
}

@Composable
private fun LocationInfoCard(
    location: MapLocation,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.width(220.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (location) {
                        is MapLocation.SimplePlaceLocation -> location.title
                        is MapLocation.UserLocation -> location.username
                    },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            when (location) {
                is MapLocation.SimplePlaceLocation -> {
                    Text(
                        text = location.type.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = location.type.markerColor,
                    )
                    Text(
                        text = location.description,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                is MapLocation.UserLocation -> {
                    Text(
                        text = "User location",
                        style = MaterialTheme.typography.labelMedium,
                        color = USER_MARKER_COLOR,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    state: SchneaggmapState,
    onAction: (SchneaggmapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        FloatingActionButton(
            onClick = { onAction(SchneaggmapAction.ToggleFilterDropdown) },
        ) {
            Icon(Icons.Default.FilterList, contentDescription = "Filter locations")
        }
        AnimatedVisibility(visible = state.isFilterDropdownVisible) {
            Card(modifier = Modifier.padding(top = 8.dp).width(200.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Location types",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    HorizontalDivider()
                    LocationType.entries.forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = type in state.enabledTypes,
                                onCheckedChange = { onAction(SchneaggmapAction.ToggleLocationType(type)) },
                            )
                            Text(text = type.displayName)
                        }
                    }
                }
            }
        }
    }
}
