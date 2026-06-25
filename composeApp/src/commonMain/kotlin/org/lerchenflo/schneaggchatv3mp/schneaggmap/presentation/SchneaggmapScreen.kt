package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.UserInfoCard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.mergeProfilePictureWithStatusText
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisLong
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.location.Location
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberNullLocationProvider
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
import org.maplibre.spatialk.units.extensions.inMeters
import kotlin.math.roundToInt
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.icon_badespot
import schneaggchatv3mp.composeapp.generated.resources.icon_beer
import schneaggchatv3mp.composeapp.generated.resources.icon_burger
import schneaggchatv3mp.composeapp.generated.resources.icon_camping
import schneaggchatv3mp.composeapp.generated.resources.icon_chinese_food
import schneaggchatv3mp.composeapp.generated.resources.icon_doener
import schneaggchatv3mp.composeapp.generated.resources.icon_food
import schneaggchatv3mp.composeapp.generated.resources.icon_nutzer
import schneaggchatv3mp.composeapp.generated.resources.icon_partylocation
import schneaggchatv3mp.composeapp.generated.resources.icon_pizza
import schneaggchatv3mp.composeapp.generated.resources.icon_police
import schneaggchatv3mp.composeapp.generated.resources.icon_radar_variant
import schneaggchatv3mp.composeapp.generated.resources.icon_sightseeing
import schneaggchatv3mp.composeapp.generated.resources.icon_street
import schneaggchatv3mp.composeapp.generated.resources.icon_viewpoint
import schneaggchatv3mp.composeapp.generated.resources.icon_wheeliespot
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_user_online
import kotlin.collections.listOf

private const val OWN_LOCATION_START_ZOOM = 14.0
private const val OWN_LOCATION_CLICK_ZOOM = 16.0

//How recent a friend's last location ping must be to show "Online" instead of a last-seen time.
private const val ONLINE_THRESHOLD_MILLIS = 2 * 60 * 1000L

private data class UserMarkerIcon(val bitmap: ImageBitmap, val size: DpSize)

private data class UserMarkerData(val username: String, val statusText: String, val isOnline: Boolean)

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

    //Own position, resolved once here (not in SchneaggmapMapContent) so we don't open a second,
    //redundant GPS subscription just to also show the speed readout below.
    val locationProvider = if (state.locationPermissionGranted) {
        rememberDefaultLocationProvider()
    } else {
        rememberNullLocationProvider()
    }
    val ownLocation by locationProvider.location.collectAsState()

    //Center on our own location once, on start, but only if we actually share it - otherwise
    //the user has no reason to expect the map to jump there.
    var hasAutoCentered by remember { mutableStateOf(false) }
    LaunchedEffect(ownLocation, state.ownLocationShared) {
        val position = ownLocation?.position?.value
        if (!hasAutoCentered && state.ownLocationShared && position != null) {
            hasAutoCentered = true
            cameraState.animateTo(CameraPosition(target = position, zoom = OWN_LOCATION_START_ZOOM))
        }
    }

    //"Follow me" mode, toggled on by the locate button. Stops as soon as the user manually
    //pans/zooms the map - any GESTURE-driven camera move is treated as "I don't want to follow".
    var isFollowingLocation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { cameraState.moveReason }
            .collect { reason ->
                if (reason == CameraMoveReason.GESTURE) {
                    isFollowingLocation = false
                }
            }
    }
    LaunchedEffect(isFollowingLocation) {
        if (!isFollowingLocation) return@LaunchedEffect

        //Zoom in once when following starts, then keep re-centering on the latest location
        //at whatever zoom the user leaves it at.
        ownLocation?.position?.value?.let { position ->
            cameraState.animateTo(CameraPosition(target = position, zoom = OWN_LOCATION_CLICK_ZOOM))
        }

        snapshotFlow { ownLocation }
            .collect { location ->
                location?.position?.value?.let { position ->
                    cameraState.animateTo(cameraState.position.copy(target = position))
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SchneaggmapMapContent(
            state = state,
            cameraState = cameraState,
            styleState = styleState,
            ownLocation = ownLocation,
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

        ownLocation?.speed?.let { speed ->
            if (!speed.distancePerSecond.isZero) {
                val speedKmh = (speed.distancePerSecond.inMeters * 3.6).roundToInt()
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .size(56.dp)
                        .background(Color.White, CircleShape)
                        .border(width = 4.dp, color = Color.Red, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$speedKmh",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
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

        ownLocation?.let {
            FloatingActionButton(
                onClick = { isFollowingLocation = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }
        }

        state.selectedEntry?.let { entry ->
            MapEntryInfoCard(
                entry = entry,
                onDismiss = {
                    onAction(SchneaggmapAction.OnPopupDismiss)
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

        state.selectedUser?.let { user ->
            UserInfoCard(
                user = user,
                ownLocation = ownLocation?.position?.value?.let { position ->
                    LatLong(lat = position.latitude, long = position.longitude)
                },
                onDismiss = {
                    onAction(SchneaggmapAction.OnPopupDismiss)
                },
                onOpenChat = { clickedUser ->
                    onAction(SchneaggmapAction.OnOpenChatClick(clickedUser))
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
    ownLocation: Location?,
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

    //Resolve user profile pictures off the composition (file read + bitmap decode), keyed by
    //path so a changed profile picture re-resolves but unrelated state changes don't. Each
    //bitmap has the user's last-online status baked in underneath the picture, so the size
    //varies per user (the status pill width depends on the text) and is tracked alongside it.
    var userIcons by remember { mutableStateOf<Map<String, UserMarkerIcon>>(emptyMap()) }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val pillColor = MaterialTheme.colorScheme.surface
    val onlineColor = MaterialTheme.colorScheme.primary
    val offlineColor = MaterialTheme.colorScheme.onSurface
    val onlineLabel = stringResource(Res.string.schneaggmap_user_online)

    //Recency-based presence: a friend counts as "online" if their last location ping is
    //recent, otherwise we show the formatted last-seen time instead.
    val userMarkerData = state.usersWithLocation.associate { user ->
        val date = user.location?.date
        val isOnline = date != null && (getCurrentTimeMillisLong() - date) < ONLINE_THRESHOLD_MILLIS
        val statusText = if (isOnline) onlineLabel else date?.let { millisToTimeDateOrYesterday(it) } ?: "-"
        val username = user.nickName?.takeIf { it.isNotBlank() } ?: user.name
        user.id to UserMarkerData(username = username, statusText = statusText, isOnline = isOnline)
    }

    val userPicturePaths = state.usersWithLocation.map { it.profilePictureUrl }
    LaunchedEffect(userPicturePaths, userMarkerData, pillColor, onlineColor, offlineColor) {
        userIcons = state.usersWithLocation.mapNotNull { user ->
            if (user.profilePictureUrl.isBlank()) return@mapNotNull null
            val markerData = userMarkerData[user.id] ?: return@mapNotNull null
            runCatching {
                val bytes = SystemFileSystem.source(Path(user.profilePictureUrl)).buffered().readByteArray()
                val mergedBitmap = mergeProfilePictureWithStatusText(
                    profilePicture = bytes.decodeToImageBitmap(),
                    username = markerData.username,
                    statusText = markerData.statusText,
                    backgroundColor = pillColor,
                    nameColor = offlineColor,
                    statusColor = if (markerData.isOnline) onlineColor else offlineColor,
                    textMeasurer = textMeasurer,
                    density = density,
                )
                val markerSize = with(density) {
                    DpSize(mergedBitmap.width.toDp(), mergedBitmap.height.toDp())
                }
                user.id to UserMarkerIcon(bitmap = mergedBitmap, size = markerSize)
            }.getOrNull()
        }.toMap()
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


        //Show user locations
        if (state.usersWithLocation.isNotEmpty()) {
            state.usersWithLocation.forEach { user ->
                val mapLocationSource = rememberGeoJsonSource(
                    data = GeoJsonData.Features(
                        FeatureCollection(
                            features = listOf(Feature(
                                geometry = Point(
                                    coordinates = Position(
                                        longitude = user.location!!.long,
                                        latitude = user.location.lat,
                                    )
                                ),
                                properties = buildJsonObject {
                                    put("type", JsonPrimitive(user.name))
                                },
                                id = JsonPrimitive(user.id)
                            ))
                        )
                    )
                )

                val markerIcon = userIcons[user.id]
                val profilePicturePainter = markerIcon?.let { BitmapPainter(it.bitmap) }
                    ?: painterResource(Res.drawable.icon_nutzer)
                val markerSize = markerIcon?.size ?: DpSize(33.dp, 33.dp)

                SymbolLayer(
                    id = "user-${user.id}",
                    source = mapLocationSource,
                    onClick = { clickedItems ->
                        if (clickedItems.isNotEmpty()) {
                            onAction(SchneaggmapAction.OnUserClick(clickedItems.first().id!!.content))
                            ClickResult.Consume
                        } else ClickResult.Pass
                    },
                    iconImage = image(profilePicturePainter, size = markerSize),
                    iconAllowOverlap = const(true)
                )
            }
        }

        //Own position, as a plain dot (no profile picture needed for yourself)
        ownLocation?.let { location ->
            val ownLocationSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(
                    FeatureCollection(features = listOf(Feature(
                        geometry = Point(coordinates = location.position.value),
                        properties = buildJsonObject {
                            put("type", JsonPrimitive("self"))
                        },
                        id = JsonPrimitive("self")
                    )))
                )
            )

            CircleLayer(
                id = "own-location",
                source = ownLocationSource,
                color = const(Color(0xFF4285F4)),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp)
            )
        }
    }

}

