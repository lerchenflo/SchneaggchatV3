package org.lerchenflo.schneaggchatv3mp.sharedUi.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private val PIN_SIZE = 36.dp

/**
 * Small map that only shows [location] with a pin, e.g. inside a popup. Gestures are off, so a
 * scrolling parent keeps scrolling; a tap anywhere on it calls [onClick].
 */
@Composable
fun StaticMapPreview(
    location: LatLong,
    styleUrl: String,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    zoom: Double = 13.0,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // The camera is only read on the first composition, so a new location needs a new map state
        key(location, styleUrl) {
            val mapState = rememberMapState(
                baseStyle = BaseStyle.Uri(styleUrl),
                initialCameraPosition = CameraPosition(
                    center = Position(longitude = location.long, latitude = location.lat),
                    zoom = zoom,
                ),
            )
            // Empty overlay: no compass, scale bar or attribution button, the strip below replaces them
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                state = mapState,
                interactions = MapInteractions.None,
                overlay = {},
            )

            // Tip of the pin on the location
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -PIN_SIZE / 2)
                    .size(PIN_SIZE)
            )

            // On top of the native map view, so the tap reaches Compose on every platform
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics { this.contentDescription = contentDescription }
            )

            // Legally required copyright notice, same strip as on the Schneaggmap; above the
            // tap layer so its links stay tappable
            MapAttributionBar(
                mapState = mapState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
