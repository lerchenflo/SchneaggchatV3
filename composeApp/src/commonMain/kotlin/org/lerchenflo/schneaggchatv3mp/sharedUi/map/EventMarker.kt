package org.lerchenflo.schneaggchatv3mp.sharedUi.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FeaturesClickHandler
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

/**
 * The event pin of every map: the event type's [icon] on a filled circle, with layer and feature
 * id `event-<eventId>`. Joined events are drawn in the primary color, all others in the error
 * color. [onClick] gets the clicked feature, whose id is [eventId].
 */
@Composable
@MaplibreComposable
fun EventMarkerLayer(
    eventId: String,
    position: Position,
    icon: ImageVector,
    isJoined: Boolean,
    onClick: FeaturesClickHandler? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val iconTint = if (isJoined) colorScheme.onPrimary else colorScheme.onError
    // Filled circle behind the icon so the pin stands out from the base map
    val backgroundColor = if (isJoined) colorScheme.primary else colorScheme.error
    val borderColor = colorScheme.surface

    safeAdd(layerId = "event-$eventId") {
        val source = rememberGeoJsonSource(
            data = GeoJsonData.Features(
                FeatureCollection(
                    features = listOf(
                        Feature(
                            geometry = Point(coordinates = position),
                            properties = buildJsonObject { put("type", JsonPrimitive("event")) },
                            id = JsonPrimitive(eventId),
                        )
                    )
                )
            )
        )
        val painter = rememberVectorPainter(icon)

        // Declared before the icon layer so it is drawn underneath it
        CircleLayer(
            id = "event-background-$eventId",
            source = source,
            onClick = onClick,
            color = const(backgroundColor),
            radius = const(16.dp),
            strokeColor = const(borderColor),
            strokeWidth = const(2.dp),
        )

        SymbolLayer(
            id = "event-$eventId",
            source = source,
            onClick = onClick,
            iconImage = image(painter, size = DpSize(22.dp, 22.dp), colorFilter = ColorFilter.tint(iconTint)),
            iconAllowOverlap = const(true),
        )
    }
}
