package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.CAMPING
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.FOOD_ASIAN
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.FOOD_BEER
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.FOOD_BURGER
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.FOOD_GREEK
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.FOOD_KEBAB
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.FOOD_OTHER
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.FOOD_PIZZA
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.MOUNTAIN_STREET
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.PARTY
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.POLICE
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.RADAR
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.SIGHTSEEING
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.SWIMMING
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.VIEWPOINT
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType.WHEELIESPOT
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapEntryInfoCard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.ShownLocationsDropdown
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
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
import schneaggchatv3mp.composeapp.generated.resources.icon_badespot
import schneaggchatv3mp.composeapp.generated.resources.icon_beer
import schneaggchatv3mp.composeapp.generated.resources.icon_burger
import schneaggchatv3mp.composeapp.generated.resources.icon_camping
import schneaggchatv3mp.composeapp.generated.resources.icon_chinese_food
import schneaggchatv3mp.composeapp.generated.resources.icon_doener
import schneaggchatv3mp.composeapp.generated.resources.icon_food
import schneaggchatv3mp.composeapp.generated.resources.icon_partylocation
import schneaggchatv3mp.composeapp.generated.resources.icon_pizza
import schneaggchatv3mp.composeapp.generated.resources.icon_police
import schneaggchatv3mp.composeapp.generated.resources.icon_radar_variant
import schneaggchatv3mp.composeapp.generated.resources.icon_sightseeing
import schneaggchatv3mp.composeapp.generated.resources.icon_street
import schneaggchatv3mp.composeapp.generated.resources.icon_viewpoint
import schneaggchatv3mp.composeapp.generated.resources.icon_wheeliespot

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
            onAction = onAction
        )

        Row(
            Modifier.align(Alignment.TopStart).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { onAction(SchneaggmapAction.OnSettingsClick) },
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }

            DisappearingCompassButton(
                cameraState = cameraState,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        DisappearingScaleBar(
            metersPerDp = cameraState.metersPerDpAtTarget,
            zoom = cameraState.position.zoom,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp),
        )

        ShownLocationsDropdown(
            state = state,
            onAction = onAction,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )

        state.selectedEntry?.let { entry ->
            MapEntryInfoCard(
                entry = entry,
                onDismiss = {
                    onAction(SchneaggmapAction.OnEntryPopupDismiss)
                },
                onSave = { changedEntry ->
                    onAction(SchneaggmapAction.OnEntryPopupSave(
                        entry = changedEntry,
                    ))
                },
                onDelete = {
                    onAction(SchneaggmapAction.OnEntryPopupDelete(it))
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

    }
}

@Composable
private fun SchneaggmapMapContent(
    state: SchneaggmapState,
    cameraState: CameraState,
    styleState: StyleState,
    onAction: (SchneaggmapAction) -> Unit
) {


    //Resolve all icons (Cycle trough the entrys, code is more garbage otherwise (Auto resolves new types)
    val typeIcons: Map<LocationType, DrawableResource> = remember {
        LocationType.entries.associateWith { type ->
            when (type) {
                RADAR -> Res.drawable.icon_radar_variant
                CAMPING -> Res.drawable.icon_camping
                SIGHTSEEING -> Res.drawable.icon_sightseeing
                SWIMMING -> Res.drawable.icon_badespot
                PARTY -> Res.drawable.icon_partylocation

                POLICE -> Res.drawable.icon_police
                MOUNTAIN_STREET -> Res.drawable.icon_street
                WHEELIESPOT -> Res.drawable.icon_wheeliespot
                VIEWPOINT -> Res.drawable.icon_viewpoint
                FOOD_KEBAB -> Res.drawable.icon_doener
                FOOD_PIZZA -> Res.drawable.icon_pizza
                FOOD_BURGER -> Res.drawable.icon_burger
                FOOD_BEER -> Res.drawable.icon_beer
                FOOD_ASIAN -> Res.drawable.icon_chinese_food
                FOOD_GREEK -> Res.drawable.icon_food //TODO: CHANGE ICON
                FOOD_OTHER -> Res.drawable.icon_food
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

            val enabledTypeKeysMap = state.enabledTypes

            LocationType.entries.forEach { type ->

                //Skip if not enabled on map
                if (!enabledTypeKeysMap.contains(type)) return@forEach

                val entriesForType = state.entries.filter { entry ->
                    entry.locationData.any { locationData ->
                        locationData.locationtype == type
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
                                        put("type", JsonPrimitive(type.name))
                                    },
                                    id = JsonPrimitive(entry.id)
                                )
                            }
                        )
                    ),

                    options = GeoJsonOptions(
                        cluster = state.useClustering,
                        clusterRadius = 12,
                        clusterMinPoints = 6,
                        //synchronousUpdate = true
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
                    iconImage = image(painterResource(iconRes), size = DpSize(33.dp, 33.dp)),
                    iconAllowOverlap = const(!state.useClustering)
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

