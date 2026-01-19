package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PinDrop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.jetbrains.compose.resources.imageResource
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.ast.BitmapLiteral
import org.maplibre.compose.expressions.ast.BooleanLiteral
import org.maplibre.compose.expressions.ast.ColorLiteral
import org.maplibre.compose.expressions.ast.CompiledFunctionCall
import org.maplibre.compose.expressions.ast.CompiledListLiteral
import org.maplibre.compose.expressions.ast.CompiledMapLiteral
import org.maplibre.compose.expressions.ast.CompiledOptions
import org.maplibre.compose.expressions.ast.DpLiteral
import org.maplibre.compose.expressions.ast.DpOffsetLiteral
import org.maplibre.compose.expressions.ast.DpPaddingLiteral
import org.maplibre.compose.expressions.ast.EnumLiteral
import org.maplibre.compose.expressions.ast.FloatLiteral
import org.maplibre.compose.expressions.ast.FunctionCall
import org.maplibre.compose.expressions.ast.IntLiteral
import org.maplibre.compose.expressions.ast.ListLiteral
import org.maplibre.compose.expressions.ast.MapLiteral
import org.maplibre.compose.expressions.ast.MillisecondsLiteral
import org.maplibre.compose.expressions.ast.NullLiteral
import org.maplibre.compose.expressions.ast.OffsetLiteral
import org.maplibre.compose.expressions.ast.Options
import org.maplibre.compose.expressions.ast.PainterLiteral
import org.maplibre.compose.expressions.ast.StringLiteral
import org.maplibre.compose.expressions.ast.TextUnitCalculation
import org.maplibre.compose.expressions.ast.TextUnitOffsetCalculation
import org.maplibre.compose.expressions.dsl.Case
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.StringValue
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.toJson
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.filter
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap

@Composable
fun SchneaggmapScreenRoot() {

    val schneaggmapViewmodel: SchneaggmapViewmodel = koinViewModel<SchneaggmapViewmodel>()

    SchneaggmapScreen(
        state = schneaggmapViewmodel.state,
        onAction = schneaggmapViewmodel::onAction
    )
}


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun SchneaggmapScreen(
    state: SchneaggmapState = SchneaggmapState(),
    onAction : (SchneaggmapAction) -> Unit = {}
){
    //https://maplibre.org/maplibre-compose/interaction/#ornaments

    val camera = rememberCameraState()
    //camera.animateTo()


    val markers = FeatureCollection(
        features = listOf(
            Feature(
                geometry = Point(9.7437, 47.4125),
                properties = mapOf("name" to "Location 1", "type" to "restaurant")
            ),
            Feature(
                geometry = Point(9.7500, 47.4200),
                properties = mapOf("name" to "Location 2", "type" to "cafe")
            ),
            Feature(
                geometry = Point(9.7300, 47.4100),
                properties = mapOf("name" to "Location 3", "type" to "restaurant")
            ),
            Feature(
                geometry = Point(9.7380, 47.4150),
                properties = mapOf("name" to "Location 4", "type" to "cafe")
            ),
            Feature(
                geometry = Point(9.7420, 47.4180),
                properties = mapOf("name" to "Location 5", "type" to "bar")
            ),
            Feature(
                geometry = Point(9.7350, 47.4130),
                properties = mapOf("name" to "Location 6", "type" to "restaurant")
            ),
            Feature(
                geometry = Point(9.7480, 47.4160),
                properties = mapOf("name" to "Location 7", "type" to "cafe")
            ),
            Feature(
                geometry = Point(9.7320, 47.4140),
                properties = mapOf("name" to "Location 8", "type" to "bar")
            ),
            Feature(
                geometry = Point(9.7460, 47.4110),
                properties = mapOf("name" to "Location 9", "type" to "restaurant")
            ),
            Feature(
                geometry = Point(9.7390, 47.4190),
                properties = mapOf("name" to "Location 10", "type" to "cafe")
            ),
            Feature(
                geometry = Point(9.7340, 47.4170),
                properties = mapOf("name" to "Location 11", "type" to "bar")
            ),
            Feature(
                geometry = Point(9.7510, 47.4120),
                properties = mapOf("name" to "Location 12", "type" to "restaurant")
            ),
            Feature(
                geometry = Point(9.7290, 47.4115),
                properties = mapOf("name" to "Location 13", "type" to "cafe")
            ),
            Feature(
                geometry = Point(9.7450, 47.4145),
                properties = mapOf("name" to "Location 14", "type" to "bar")
            ),
            Feature(
                geometry = Point(9.7370, 47.4105),
                properties = mapOf("name" to "Location 15", "type" to "restaurant")
            ),
            Feature(
                geometry = Point(9.7490, 47.4185),
                properties = mapOf("name" to "Location 16", "type" to "cafe")
            ),
            Feature(
                geometry = Point(9.7310, 47.4125),
                properties = mapOf("name" to "Location 17", "type" to "bar")
            ),
            Feature(
                geometry = Point(9.7470, 47.4135),
                properties = mapOf("name" to "Location 18", "type" to "restaurant")
            ),
            Feature(
                geometry = Point(9.7360, 47.4195),
                properties = mapOf("name" to "Location 19", "type" to "cafe")
            ),
            Feature(
                geometry = Point(9.7410, 47.4108),
                properties = mapOf("name" to "Location 20", "type" to "bar")
            )
        )
    )


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
    ) {
        val userSource = GeoJsonSource(
            id = "user-locations-source",
            data = GeoJsonData.JsonString(
                state.getUserLocationFeatureCollection().toJson()
            ),
            options = GeoJsonOptions()
        )

        SymbolLayer(
            id = "user-locations-layer",
            source = userSource,
            iconSize = const(0.25f),
            iconImage = image(imageResource(Res.drawable.icon_nutzer)),
            textField = format(
                span(get("username").cast<StringValue>()),
            ),
            textFont = const(listOf("Noto Sans Regular")),
            textSize = const(0.8.em),
            textOffset = offset(0.em, 2.5.em),
            onClick = { features ->
                features.firstOrNull()?.let { feature ->
                    val username = feature.properties?.get("username")
                    val lastSeen = feature.properties?.get("lastSeen")
                    println("User clicked: $username (last seen: $lastSeen)")
                }
                ClickResult.Consume
            }
        )


        // Place locations layer
        val placeSource = GeoJsonSource(
            id = "place-locations-source",
            data = GeoJsonData.JsonString(
                state.getPlaceLocationFeatureCollection().toJson()
            ),
            options = GeoJsonOptions()
        )

        SymbolLayer(
            id = "place-locations-layer",
            source = placeSource,
            iconSize = const(0.2f),
            // Use match expression to show different icons based on place type
            textField = format(
                span(get("name").cast<StringValue>()),
            ),
            iconImage = switch(
                input = get("placeType").cast<StringValue>(),
                case("RADARBOX", image(imageResource(Res.drawable.icon_nutzer))),
                case("DOENER", image(imageResource(Res.drawable.icon_nutzer))),
                case("PIZZA", image(imageResource(Res.drawable.icon_nutzer))),
                fallback = image(imageResource(Res.drawable.icon_nutzer))
            ),
            textFont = const(listOf("Noto Sans Regular")),
            textSize = const(0.7.em),
            textOffset = offset(0.em, 2.em),
            onClick = { features ->
                features.firstOrNull()?.let { feature ->
                    val name = feature.properties?.get("name") as? String
                    val placeType = feature.properties?.get("placeType") as? String
                    println("Place clicked: $name (type: $placeType)")
                }
                ClickResult.Consume
            }
        )

    }


    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        IconButton(
            onClick = {},
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(30.dp)

                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(15.dp)
                )

        ){
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
            )
        }
    }



}