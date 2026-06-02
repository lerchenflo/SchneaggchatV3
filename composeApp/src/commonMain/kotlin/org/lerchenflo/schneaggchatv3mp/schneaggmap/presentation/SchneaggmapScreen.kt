package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.typeKey
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapEntryInfoCard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.ShownLocationsDropdown
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.layers.SymbolLayer
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
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap

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
            target = Position(9.92, 47.32),
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
        state.selectedEntry?.let { entry ->
            println("Showing card: $entry")
            MapEntryInfoCard(
                entry = entry,
                onDismiss = {
                    onAction(SchneaggmapAction.OnEntryPopupDismiss)
                },
                onSave = {
                    onAction(SchneaggmapAction.OnEntryPopupSave(it))
                }
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

    //Resolve all icons (Cycle trough the entrys, code is more garbage otherwise (Auto resolves new types)
    val typeIcons: Map<LocationType, DrawableResource> = remember {
        LocationType.entries.associateWith { type ->
            when (type) {
                LocationType.RADAR       -> Res.drawable.icon_nutzer
                LocationType.STREET      -> Res.drawable.schneaggmap
                LocationType.CAMPING     -> Res.drawable.icon_nutzer
                LocationType.SIGHTSEEING -> Res.drawable.icon_nutzer
                LocationType.SWIMMING    -> Res.drawable.icon_nutzer
                LocationType.PARTY       -> Res.drawable.icon_nutzer
                LocationType.FOOD        -> Res.drawable.icon_nutzer
            }
        }
    }


    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        cameraState = cameraState,
        styleState = styleState,
        options = MapOptions(
            ornamentOptions = OrnamentOptions.AllDisabled
        ),
        onMapClick = { position, _ ->
            onAction(SchneaggmapAction.OnMapClick(LatLong(position.latitude, position.longitude), longClick = false))

            ClickResult.Pass
        },
        onMapLongClick = { position, _ ->
            onAction(SchneaggmapAction.OnMapClick(LatLong(position.latitude, position.longitude), longClick = true))

            ClickResult.Consume
        }
    ) {

        //println("MapLocations: ${state.entries}")


        //If no entry is loaded, dont render anything (Crash)
        if (state.entries.isNotEmpty()) {

            val enabledTypeKeysMap = state.enabledTypes.toSet()

            LocationType.entries.forEach { type ->

                //Skip if not enabled on map
                if (!enabledTypeKeysMap.contains(type.typeKey)) return@forEach

                val entriesForType = state.entries.filter { entry ->
                    entry.locationData.any { locationData ->
                        locationData.typeKey == type.typeKey
                    }
                }
                if (entriesForType.isEmpty()) return@forEach


                val mapLocationSource = rememberGeoJsonSource(
                    data = GeoJsonData.Features(
                        FeatureCollection(
                            features = entriesForType
                                .map { entry ->
                                Feature(
                                    geometry = Point(
                                        coordinates = Position(
                                            longitude = entry.coordinates.long,
                                            latitude = entry.coordinates.lat,
                                        )
                                    ),
                                    properties = buildJsonObject {
                                        put("type", JsonPrimitive(type.typeKey))
                                    },
                                    id = JsonPrimitive(entry.id)
                                )
                            }
                        )
                    ),
                    options = GeoJsonOptions(
                        cluster = true,
                        clusterRadius = 13,
                        clusterMinPoints = 6
                    )

                )

                val iconRes = typeIcons[type] ?: return@forEach


                /*
                SymbolLayer(
                    id = "type-${type.name}",
                    source = mapLocationSource,
                    iconImage = image(iconBitmap)
                )

                 */


                SymbolLayer(
                    id = "type-${type.name}",
                    source = mapLocationSource,
                    onClick = { clickedItems ->
                        if (clickedItems.isNotEmpty()) {
                            onAction(SchneaggmapAction.OnEntryClick(clickedItems.first().id!!.content))
                            ClickResult.Consume
                        } else ClickResult.Pass
                    },
                    iconImage = image(painterResource(iconRes), size = DpSize(20.dp, 20.dp))
                )
            }




            //val filter = const(state.enabledTypes.toList()).contains(feature["type"])

            // 3. Apply Filter to Layer
            /*
            CircleLayer(
                id = "mapicons",
                source = mapLocationSource,
                color = const(Color.Gray),
                radius = const(10.dp),

                // The layer automatically updates when 'filter' changes
                filter = filter
            )

             */



        }
    }

}

