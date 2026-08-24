package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.safeAdd
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

// Default center/zoom when no location was picked yet - mirrors SchneaggmapScreen's own default
// firstPosition (SchneaggmapScreen.kt).
private val DEFAULT_MAP_POSITION = Position(9.92, 47.32)
private const val DEFAULT_ZOOM = 7.0
private const val SELECTED_ZOOM = 14.0

/**
 * Small, self-contained MaplibreMap for picking a single point - tap anywhere to drop/move a pin.
 * Reuses the same MaplibreMap/GeoJsonSource/SymbolLayer building blocks as the main map screen
 * (SchneaggmapScreen.kt) so a single-point picker doesn't need its own copy of that plumbing.
 */
@Composable
fun LocationPickerMap(
    selectedLocation: LatLong?,
    mapStyleUrl: String,
    onLocationSelected: (LatLong) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = selectedLocation?.let { Position(longitude = it.long, latitude = it.lat) }
                ?: DEFAULT_MAP_POSITION,
            zoom = if (selectedLocation != null) SELECTED_ZOOM else DEFAULT_ZOOM,
        )
    )
    val styleState = rememberStyleState()

    MaplibreMap(
        modifier = modifier,
        baseStyle = BaseStyle.Uri(mapStyleUrl),
        cameraState = cameraState,
        styleState = styleState,
        options = MapOptions(ornamentOptions = OrnamentOptions.AllDisabled),
        onMapClick = { position, _ ->
            onLocationSelected(LatLong(lat = position.latitude, long = position.longitude))
            ClickResult.Consume
        }
    ) {
        selectedLocation?.let { location ->
            safeAdd(layerId = "picked-location-pin") {
                val pinSource = rememberGeoJsonSource(
                    data = GeoJsonData.Features(
                        FeatureCollection(features = listOf(Feature(
                            geometry = Point(coordinates = Position(longitude = location.long, latitude = location.lat)),
                            properties = buildJsonObject {
                                put("type", JsonPrimitive("picked-location"))
                            },
                            id = JsonPrimitive("picked-location")
                        )))
                    )
                )

                val pinPainter = rememberVectorPainter(Icons.Default.LocationOn)
                SymbolLayer(
                    id = "picked-location-pin-symbol",
                    source = pinSource,
                    iconImage = image(
                        pinPainter,
                        size = DpSize(36.dp, 36.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )
                )
            }
        }
    }
}
