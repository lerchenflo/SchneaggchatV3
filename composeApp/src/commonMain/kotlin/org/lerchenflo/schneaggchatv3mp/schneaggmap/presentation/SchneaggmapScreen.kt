package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.runtime.Composable
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.toJson

@Composable
fun SchneaggmapScreenRoot(
    
){
    SchneaggmapScreen()
}

@Composable
fun SchneaggmapScreen(
    
){
    //https://maplibre.org/maplibre-compose/interaction/#ornaments

    val camera = rememberCameraState()
    //camera.animateTo()


    MaplibreMap(
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        options = MapOptions(
            renderOptions = RenderOptions.Standard,
            gestureOptions = GestureOptions.RotationLocked,
            ornamentOptions = OrnamentOptions.AllDisabled
        ),
        cameraState = camera,
        onMapClick = { pos, offset ->
            val features = camera.projection?.queryRenderedFeatures(offset)
            if (!features.isNullOrEmpty()) {
                println("Clicked on ${features[0].toJson()}")
                ClickResult.Consume
            } else {
                ClickResult.Pass
            }
        },
        onMapLongClick = { pos, offset ->
            println("Long click at $pos")
            ClickResult.Pass
        },
    )

}