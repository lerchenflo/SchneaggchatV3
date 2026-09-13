package org.lerchenflo.schneaggchatv3mp.car

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.graphics.drawable.IconCompat
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.androidApp.R
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.events.data.EventRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.MapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.AttributeValue
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationData
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.car_back
import schneaggchatv3mp.composeapp.generated.resources.car_nothing_nearby
import schneaggchatv3mp.composeapp.generated.resources.car_radar_added
import schneaggchatv3mp.composeapp.generated.resources.location_type_radar

/**
 * The car's only screen: a [MapWithContentTemplate] with the map drawn by [CarMapSurfaceRenderer]
 * and a single-line, single-row [PaneTemplate] content pane - the name of the nearest
 * currently-visible marker (friend, event, or map entry, respecting the filters from
 * [CarMapFiltersScreen]) by default, or the name of whatever was tapped on the map, with a Back
 * action. [MapWithContentTemplate] requires a content template (no pane-less mode without
 * switching to `NavigationTemplate`), so this is the smallest useful pane. Otherwise view-only -
 * no search, no dialogs, no editing of existing entries - the only write path is the header's
 * one-tap "add radar here" action (see plans/ANDROID_AUTO_MAP_PLAN.md).
 */
class SchneaggmapCarScreen(carContext: CarContext) : Screen(carContext) {

    private val screenScope = newLifecycleScope()

    private val stateHolder = CarMapStateHolder(
        mapRepository = KoinPlatform.getKoin().get<MapRepository>(),
        appRepository = KoinPlatform.getKoin().get<AppRepository>(),
        userRepository = KoinPlatform.getKoin().get<UserRepository>(),
        eventRepository = KoinPlatform.getKoin().get<EventRepository>(),
        preferenceManager = KoinPlatform.getKoin().get<Preferencemanager>(),
        scope = screenScope,
    )

    private val locationService = KoinPlatform.getKoin().get<LocationService>()
    private val appRepository = KoinPlatform.getKoin().get<AppRepository>()

    private val _isDarkMode = MutableStateFlow(carContext.isDarkMode)

    //Follows the car host's dark mode independently of the phone's own map style setting - the
    //phone screen and the car can legitimately show different styles at the same time.
    private val displayState: StateFlow<SchneaggmapState> = combine(
        stateHolder.state, _isDarkMode,
    ) { state, dark ->
        if (dark) state.copy(mapStyleUrl = MapStyleSetting.DARK.tileUrl) else state
    }.stateIn(screenScope, SharingStarted.Eagerly, stateHolder.state.value)

    private val surfaceRenderer = CarMapSurfaceRenderer(
        carContext = carContext,
        displayState = displayState,
        onAction = stateHolder::handleAction,
        locationService = locationService,
        coroutineScope = screenScope,
    )

    //onGetTemplate() must return synchronously; these are compose-resources strings, which only
    //resolve through a suspend call - cached once up front the same way Notifier.android.kt does.
    private val backTitle = runBlocking { getString(Res.string.car_back) }
    private val nothingNearbyMessage = runBlocking { getString(Res.string.car_nothing_nearby) }
    private val radarAddedMessage = runBlocking { getString(Res.string.car_radar_added) }
    private val radarEntryName = runBlocking { getString(Res.string.location_type_radar) }

    private var ownLocation: LatLong? = null
    private var ownSpeedMetersPerSecond: Double? = null

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceRenderer)

        screenScope.launch {
            displayState.collect { invalidate() }
        }
        //Own position for finding the nearest visible marker - independent of the map's own blue
        //dot (see CarMapSurfaceRenderer), same split the phone code already has elsewhere.
        screenScope.launch {
            locationService.getLocationFlow(fastUpdates = false).collect { location ->
                ownLocation = location?.coordinates
                ownSpeedMetersPerSecond = location?.speed
                invalidate()
            }
        }
    }

    fun onCarConfigurationChanged() {
        _isDarkMode.value = carContext.isDarkMode
    }

    private fun openFilters() {
        carContext.getCarService(ScreenManager::class.java)
            .push(CarMapFiltersScreen(carContext, stateHolder, displayState))
    }

    /** One-tap radar at the car's current fix, speed limit pre-filled from the current speed. */
    private fun addRadarAtCurrentLocation() {
        val location = ownLocation ?: return
        val speedKmh = ((ownSpeedMetersPerSecond ?: 0.0) * 3.6).roundToInt()

        screenScope.launch {
            appRepository.upsertMapEntry(
                entryId = null,
                name = radarEntryName,
                description = "",
                lat = location.lat,
                lon = location.long,
                locationData = listOf(
                    LocationData.Radar(
                        radarSpeedLimit = AttributeValue.IntValue(speedKmh),
                        radarMobile = AttributeValue.BoolValue(true),
                        radarRedLight = AttributeValue.BoolValue(false),
                    )
                )
            )
        }

        CarToast.makeText(carContext, radarAddedMessage, CarToast.LENGTH_SHORT).show()
    }

    override fun onGetTemplate(): Template {
        val state = displayState.value

        val mapActionStrip = ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(
                Action.Builder()
                    .setIcon(iconOf(R.drawable.ic_car_zoom_in, CarColor.DEFAULT))
                    .setOnClickListener { surfaceRenderer.zoomBy(1.0) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(iconOf(R.drawable.ic_car_zoom_out, CarColor.DEFAULT))
                    .setOnClickListener { surfaceRenderer.zoomBy(-1.0) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(iconOf(android.R.drawable.ic_menu_mylocation))
                    .setOnClickListener { surfaceRenderer.recenter() }
                    .build()
            )
            .build()

        val headerActionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(iconOf(android.R.drawable.ic_menu_camera))
                    .setOnClickListener { addRadarAtCurrentLocation() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(iconOf(android.R.drawable.ic_menu_preferences))
                    .setOnClickListener { openFilters() }
                    .build()
            )
            .build()

        val mapController = MapController.Builder()
            .setMapActionStrip(mapActionStrip)
            .build()

        val contentTemplate: Template = when {
            state.selectedEntry != null -> selectionPane(state.selectedEntry.name)
            state.selectedUser != null -> selectionPane(state.selectedUser.displayName)
            else -> nearestVisiblePane(state)
        }

        return MapWithContentTemplate.Builder()
            .setMapController(mapController)
            .setActionStrip(headerActionStrip)
            .setContentTemplate(contentTemplate)
            .build()
    }

    /** Single-line name row + Back action, no title header and no subtitle - as small as the pane gets. */
    private fun selectionPane(name: String): PaneTemplate {
        val pane = Pane.Builder()
            .addRow(Row.Builder().setTitle(name).build())
            .addAction(
                Action.Builder()
                    .setTitle(backTitle)
                    .setOnClickListener { stateHolder.clearSelection() }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane).build()
    }

    /** One name, one line - the nearest marker currently visible under the active filters. */
    private data class VisibleItem(val name: String, val position: LatLong)

    private fun nearestVisiblePane(state: SchneaggmapState): PaneTemplate {
        val visibleItems = buildList {
            if (state.showUsers) {
                state.usersWithLocation.forEach { user ->
                    user.location?.let { add(VisibleItem(user.displayName, LatLong(lat = it.lat, long = it.long))) }
                }
            }
            state.entries.forEach { entry ->
                if (entry.locationData.any { it.locationtype in state.enabledTypes }) {
                    add(VisibleItem(entry.name, entry.coordinates))
                }
            }
            if (state.showEvents) {
                state.eventsWithLocation.forEach { event ->
                    event.location?.let { add(VisibleItem(event.title, it)) }
                }
            }
        }

        val origin = ownLocation
        val nearest = if (origin != null) {
            visibleItems.minByOrNull { distanceMeters(origin, it.position) }
        } else {
            visibleItems.firstOrNull()
        }

        val pane = Pane.Builder()
            .addRow(Row.Builder().setTitle(nearest?.name ?: nothingNearbyMessage).build())
            .build()

        return PaneTemplate.Builder(pane).build()
    }

    private fun iconOf(resId: Int, tint: CarColor? = null): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, resId))
            .apply { tint?.let { setTint(it) } }
            .build()
}
