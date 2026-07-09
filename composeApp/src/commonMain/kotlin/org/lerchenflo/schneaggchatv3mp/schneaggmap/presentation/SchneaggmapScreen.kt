package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
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
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.FriendLocationsPreview
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.MapEntryInfoCard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.ShownLocationsDropdown
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.UserInfoCard
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.mergeClusterAvatarsIcon
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.mergeProfilePictureWithStatusText
import org.lerchenflo.schneaggchatv3mp.utilities.millisToTimeDateOrYesterday
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
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
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.inDegrees
import org.maplibre.spatialk.units.extensions.inMeters
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val OWN_LOCATION_START_ZOOM = 14.0
private const val OWN_LOCATION_CLICK_ZOOM = 16.0

//Radius, in dp, within which nearby friends get merged into one marker - converted to meters via
//the camera's current scale so it stays a constant on-screen size regardless of zoom level.
private const val USER_CLUSTER_RADIUS_DP = 40.0
private const val EARTH_RADIUS_METERS = 6371000.0

private data class UserMarkerIcon(val bitmap: ImageBitmap, val size: DpSize)

private data class UserMarkerData(val username: String, val statusText: String, val isOnline: Boolean)

//Distinct, stable color per user for snail trails - picked by hashing the user id into a
//small curated palette so colors stay visually distinguishable from each other.
private val SNAIL_TRAIL_COLORS = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF6D4C41),
)

private fun snailTrailColor(userId: String): Color =
    SNAIL_TRAIL_COLORS[userId.hashCode().mod(SNAIL_TRAIL_COLORS.size)]

//Extends the recorded trail with the user's live position so the line reaches all the way to
//where they actually are right now, not just to the last synced trail point. Skipped if the
//user is already sitting at that last point (no redundant zero-length segment).
private fun appendLiveEndIfMoved(trailPositions: List<Position>, livePosition: Position?): List<Position> {
    if (livePosition == null) return trailPositions
    return if (trailPositions.isEmpty() || trailPositions.last() != livePosition) {
        trailPositions + listOf(livePosition)
    } else {
        trailPositions
    }
}

private data class UserCluster(val users: List<User>, val centroid: Position)

//Stable identity for a cluster (independent of member order) so layers/icons key correctly
//across recompositions and only churn when membership actually changes.
private fun clusterKey(cluster: UserCluster): String =
    cluster.users.map { it.id }.sorted().joinToString("-")

//Flat-earth approximation - accurate enough for grouping markers that are at most a few
//kilometers apart, which is all clustering cares about, without the cost of full Haversine.
private fun approximateDistanceMeters(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
    val avgLatRad = (lat1 + lat2) / 2.0 * PI / 180.0
    val dx = (long2 - long1) * cos(avgLatRad) * EARTH_RADIUS_METERS * PI / 180.0
    val dy = (lat2 - lat1) * EARTH_RADIUS_METERS * PI / 180.0
    return sqrt(dx * dx + dy * dy)
}

//Greedily grows each cluster from a seed user, re-checking the running centroid against the
//remaining users so chains of nearby users merge together rather than only the seed's neighbors.
private fun clusterUsersByProximity(users: List<User>, radiusMeters: Double): List<UserCluster> {
    val remaining = users.filter { it.location != null }.toMutableList()
    val clusters = mutableListOf<UserCluster>()

    while (remaining.isNotEmpty()) {
        val members = mutableListOf(remaining.removeAt(0))
        var grew = true
        while (grew) {
            grew = false
            val centroidLat = members.map { it.location!!.lat }.average()
            val centroidLong = members.map { it.location!!.long }.average()
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                val location = candidate.location!!
                if (approximateDistanceMeters(centroidLat, centroidLong, location.lat, location.long) <= radiusMeters) {
                    members.add(candidate)
                    iterator.remove()
                    grew = true
                }
            }
        }
        val centroidLat = members.map { it.location!!.lat }.average()
        val centroidLong = members.map { it.location!!.long }.average()
        clusters.add(UserCluster(users = members, centroid = Position(longitude = centroidLong, latitude = centroidLat)))
    }

    return clusters
}

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
    val scope = rememberCoroutineScope()

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

        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

        }

        ownLocation?.speed?.let { speed ->
            if (speed.distancePerSecond.inMeters > 3) {
                val speedKmh = (speed.distancePerSecond.inMeters * 3.6).roundToInt()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
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

        ShownLocationsDropdown(
            state = state,
            onAction = onAction,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )


        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (state.usersWithLocation.isNotEmpty()) {
                FriendLocationsPreview(
                    friends = state.usersWithLocation,
                    onlineFriendIds = state.onlineFriendIds,
                    onUserClick = { user ->
                        val loc = user.location ?: return@FriendLocationsPreview
                        isFollowingLocation = false
                        scope.launch {
                            cameraState.animateTo(
                                CameraPosition(
                                    target = Position(longitude = loc.long, latitude = loc.lat),
                                    zoom = OWN_LOCATION_CLICK_ZOOM
                                )
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Left: Scale bar
                DisappearingScaleBar(
                    metersPerDp = cameraState.metersPerDpAtTarget,
                    zoom = cameraState.position.zoom,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                // Center: Own location FAB
                ownLocation?.let {
                    FloatingActionButton(
                        onClick = { isFollowingLocation = true },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    }
                }

                // Right: Snail trails toggle
                Card(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Polyline,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = state.showSnailTrails,
                            onCheckedChange = { onAction(SchneaggmapAction.ToggleSnailTrails) },
                        )
                    }
                }
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
                isOnline = user.id in state.onlineFriendIds,
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

    val ownId = SessionCache.requireLoggedIn()?.userId

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

    //Raw, undecorated profile pictures (no status pill) kept alongside userIcons so merged
    //cluster icons can composite them without re-reading the file from disk.
    var rawAvatarBitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val pillColor = MaterialTheme.colorScheme.surface
    val onlineColor = MaterialTheme.colorScheme.primary
    val offlineColor = MaterialTheme.colorScheme.onSurface
    val onlineLabel = stringResource(Res.string.schneaggmap_user_online)

    //Hock (group hangout) cluster marker ingredients: a translucent version of the marker pill
    //background as the "table", a beer for the center/decorations, and a generic person icon for
    //cluster members whose profile picture hasn't loaded (so the ring still shows every member).
    val clusterBackgroundColor = pillColor.copy(alpha = 0.85f)
    val beerIcon = imageResource(Res.drawable.icon_beer)
    val defaultAvatarBitmap = imageResource(Res.drawable.icon_nutzer)

    //Live presence: a friend counts as "online" if the server currently has them connected,
    //otherwise we show their last-seen time instead.
    val userMarkerData = state.usersWithLocation.associate { user ->
        val isOnline = user.id in state.onlineFriendIds
        val statusText = if (isOnline) onlineLabel else user.lastSeen?.let { millisToTimeDateOrYesterday(it) } ?: "-"
        val username = user.displayName
        user.id to UserMarkerData(username = username, statusText = statusText, isOnline = isOnline)
    }

    val userPicturePaths = state.usersWithLocation.map { it.profilePictureUrl }
    LaunchedEffect(userPicturePaths, userMarkerData, pillColor, onlineColor, offlineColor) {
        val rawBitmaps = mutableMapOf<String, ImageBitmap>()
        userIcons = state.usersWithLocation.mapNotNull { user ->
            if (user.profilePictureUrl.isBlank()) return@mapNotNull null
            val markerData = userMarkerData[user.id] ?: return@mapNotNull null
            runCatching {
                val bytes = SystemFileSystem.source(Path(user.profilePictureUrl)).buffered().readByteArray()
                val profilePicture = bytes.decodeToImageBitmap()
                rawBitmaps[user.id] = profilePicture
                val mergedBitmap = mergeProfilePictureWithStatusText(
                    profilePicture = profilePicture,
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
        rawAvatarBitmaps = rawBitmaps
    }

    //Merge nearby friends into a single marker once they're closer together on screen than
    //USER_CLUSTER_RADIUS_DP - converted to meters via the camera's current scale, then quantized
    //so a continuous pinch/pan gesture doesn't re-cluster (and churn the GL layers) every frame.
    val rawClusterRadiusMeters = USER_CLUSTER_RADIUS_DP * cameraState.metersPerDpAtTarget
    val clusterRadiusMeters = (rawClusterRadiusMeters / 5.0).roundToInt() * 5.0
    val userClusters = remember(state.usersWithLocation, clusterRadiusMeters) {
        clusterUsersByProximity(state.usersWithLocation, clusterRadiusMeters)
    }

    val clusterIcons: Map<String, UserMarkerIcon> = remember(
        userClusters, rawAvatarBitmaps, clusterBackgroundColor, beerIcon, defaultAvatarBitmap
    ) {
        userClusters.filter { it.users.size >= 2 }.associate { cluster ->
            //Always one avatar per member (falling back to the generic icon) so the ring
            //accurately reflects how many people are in the cluster.
            val avatarBitmaps = cluster.users.map { rawAvatarBitmaps[it.id] ?: defaultAvatarBitmap }
            val bitmap = mergeClusterAvatarsIcon(
                profilePictures = avatarBitmaps,
                beerIcon = beerIcon,
                backgroundColor = clusterBackgroundColor,
                density = density,
            )
            val size = with(density) { DpSize(bitmap.width.toDp(), bitmap.height.toDp()) }
            clusterKey(cluster) to UserMarkerIcon(bitmap = bitmap, size = size)
        }
    }


    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        baseStyle = BaseStyle.Uri(state.mapStyleUrl),
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


                val iconRes = typeIcons[type] ?: return@forEach

                safeAdd(layerId = "type-${type.name}") {
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


        //Show snail trails (drawn before the user markers so the avatars sit on top of the lines)
        if (state.showSnailTrails) {
            state.usersWithLocation.forEach { user ->
                key(user.id) {
                    val trail = state.snailTrails[user.id]
                    val trailPositions = trail?.map { point ->
                        Position(longitude = point.long, latitude = point.lat)
                    } ?: emptyList()
                    val livePosition = user.location?.let { Position(longitude = it.long, latitude = it.lat) }
                    val fullTrailPositions = appendLiveEndIfMoved(trailPositions, livePosition)
                    if (fullTrailPositions.size >= 2) {
                        safeAdd(layerId = "snailtrail-${user.id}") {
                            val trailSource = rememberGeoJsonSource(
                                data = GeoJsonData.Features(
                                    FeatureCollection(
                                        Feature(
                                            geometry = LineString(fullTrailPositions),
                                            properties = buildJsonObject {},

                                        )
                                    )
                                )
                            )

                            LineLayer(
                                id = "snailtrail-${user.id}",
                                source = trailSource,
                                color = const(snailTrailColor(user.id)),
                                width = const(3.dp),
                                cap = const(LineCap.Round),
                                join = const(LineJoin.Round),
                            )
                        }
                    }
                }
            }

            // Our own snail trail - drawn in the theme's primary color so it stands out from
            // friends' hashed palette colors.
            if (ownId != null) {
                key(ownId) {
                    val ownTrail = state.snailTrails[ownId]
                    val ownTrailPositions = ownTrail?.map { point ->
                        Position(longitude = point.long, latitude = point.lat)
                    } ?: emptyList()
                    val ownLivePosition = ownLocation?.position?.value
                    val fullOwnTrailPositions = appendLiveEndIfMoved(ownTrailPositions, ownLivePosition)
                    if (fullOwnTrailPositions.size >= 2) {
                        safeAdd(layerId = "snailtrail-$ownId") {
                            val ownTrailColor = MaterialTheme.colorScheme.primary
                            val ownTrailSource = rememberGeoJsonSource(
                                data = GeoJsonData.Features(
                                    FeatureCollection(
                                        Feature(
                                            geometry = LineString(fullOwnTrailPositions),
                                            properties = buildJsonObject {},

                                        )
                                    )
                                )
                            )

                            LineLayer(
                                id = "snailtrail-$ownId",
                                source = ownTrailSource,
                                color = const(ownTrailColor),
                                width = const(3.dp),
                                cap = const(LineCap.Round),
                                join = const(LineJoin.Round),
                            )
                        }
                    }
                }
            }
        }

        //Show user locations - friends close enough together at the current zoom are merged into
        //a single "stacked avatars" marker (see userClusters) instead of overlapping pins.
        if (state.usersWithLocation.isNotEmpty()) {
            userClusters.forEach { cluster ->
                if (cluster.users.size == 1) {
                    val user = cluster.users.first()
                    key(user.id) {
                        safeAdd(layerId = "user-${user.id}") {
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

                            //TODO: rotate this marker using user.location?.heading once we have a directional
                            // marker design - heading is already stored/synced per friend but unused for rendering.
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
                } else {
                    val clusterId = clusterKey(cluster)
                    key(clusterId) {
                        safeAdd(layerId = "cluster-$clusterId") {
                            val clusterSource = rememberGeoJsonSource(
                                data = GeoJsonData.Features(
                                    FeatureCollection(
                                        features = listOf(Feature(
                                            geometry = Point(coordinates = cluster.centroid),
                                            properties = buildJsonObject {},
                                            id = JsonPrimitive(clusterId)
                                        ))
                                    )
                                )
                            )

                            val clusterIcon = clusterIcons[clusterId]
                            if (clusterIcon != null) {
                                //Tapping a cluster zooms in on it rather than opening a specific
                                //user - once it splits apart, individual pins are clickable as usual.
                                SymbolLayer(
                                    id = "cluster-$clusterId",
                                    source = clusterSource,
                                    onClick = {
                                        scope.launch {
                                            cameraState.animateTo(
                                                cameraState.position.copy(
                                                    target = cluster.centroid,
                                                    zoom = cameraState.position.zoom + 2,
                                                )
                                            )
                                        }
                                        ClickResult.Consume
                                    },
                                    iconImage = image(BitmapPainter(clusterIcon.bitmap), size = clusterIcon.size),
                                    iconAllowOverlap = const(true)
                                )
                            }
                        }
                    }
                }
            }
        }

        //Own position: a dot, or a heading-rotated arrow while moving (no profile picture needed for yourself)
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

            // Once we're moving fast enough to have a reliable heading, show a rotated arrow
            // instead of a plain dot. Both layers always exist to avoid source remove+re-add
            // when heading appears/disappears (CannotAddSourceException).
            val heading = location.speed
                ?.takeIf { it.distancePerSecond.inMeters > 3 }
                ?.let { location.course?.value }

            val arrowPainter = rememberVectorPainter(Icons.Default.Navigation)
            SymbolLayer(
                id = "own-location-arrow",
                source = ownLocationSource,
                visible = heading != null,
                iconImage = image(
                    arrowPainter,
                    size = DpSize(28.dp, 28.dp),
                    colorFilter = ColorFilter.tint(Color(0xFF4285F4))
                ),
                iconRotate = const(((heading ?: Bearing.North) - Bearing.North).inDegrees.toFloat()),
                iconAllowOverlap = const(true)
            )
            CircleLayer(
                id = "own-location-dot",
                source = ownLocationSource,
                visible = heading == null,
                color = const(Color(0xFF4285F4)),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp)
            )
        }
    }

}

