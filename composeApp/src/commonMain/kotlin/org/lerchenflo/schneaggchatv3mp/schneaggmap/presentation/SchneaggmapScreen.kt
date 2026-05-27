package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.ShownLocationsDropdown
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.contains
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.material3.DisappearingCompassButton
import org.maplibre.compose.material3.DisappearingScaleBar
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.StyleState
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

@Composable
fun SchneaggmapScreenRoot() {
    val viewModel = koinViewModel<SchneaggmapViewModel>()
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
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(9.3738, 48.2082),
            zoom = 7.0,
        )
    )
    val styleState = rememberStyleState()

    Box(modifier = Modifier.fillMaxSize()) {
        SchneaggmapMapContent(
            state = state,
            cameraState = cameraState,
            styleState = styleState,
            onAction = onAction,
        )

        DisappearingCompassButton(
            cameraState = cameraState,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
        DisappearingScaleBar(
            metersPerDp = cameraState.metersPerDpAtTarget,
            zoom = cameraState.position.zoom,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp),
        )
        ShownLocationsDropdown(
            state = state,
            onAction = onAction,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )

    }
}

@Composable
private fun SchneaggmapMapContent(
    state: SchneaggmapState,
    cameraState: CameraState,
    styleState: StyleState,
    onAction: (SchneaggmapAction) -> Unit,
) {

    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        cameraState = cameraState,
        styleState = styleState,
        options = MapOptions(
            ornamentOptions = OrnamentOptions.AllDisabled
        )
    ) {

        //println("MapLocations: ${state.entries}")


        if (state.entries.isNotEmpty()) {
            val mapLocationSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(
                    FeatureCollection(
                        features = state.entries.map { entry ->
                            Feature(
                                geometry = Point(
                                    coordinates = Position(
                                        longitude = entry.lon,
                                        latitude = entry.lat,
                                    )
                                ),
                                properties = buildJsonObject {
                                    put("type", JsonPrimitive(entry.mainTypeKey))
                                }
                            )
                        }
                    )
                ),
                options = GeoJsonOptions(
                    cluster = true,
                    clusterRadius = 16,
                    clusterMinPoints = 3
                )
            )

            val filter = const(state.enabledMainTypes.toList()).contains(feature["type"])

            // 3. Apply Filter to Layer
            CircleLayer(
                id = "mapicons",
                source = mapLocationSource,
                color = const(Color.Gray),
                radius = const(10.dp),

                // The layer automatically updates when 'filter' changes
                filter = filter
            )
        }
    }

}

