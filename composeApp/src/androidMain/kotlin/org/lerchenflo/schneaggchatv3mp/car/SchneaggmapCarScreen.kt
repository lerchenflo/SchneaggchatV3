package org.lerchenflo.schneaggchatv3mp.car

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.events.data.EventRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.MapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters
import org.lerchenflo.schneaggchatv3mp.utilities.formatDistance
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.car_back
import schneaggchatv3mp.composeapp.generated.resources.car_friends_title
import schneaggchatv3mp.composeapp.generated.resources.no_friends

private const val FALLBACK_CONTENT_LIMIT = 6

/**
 * The car's only screen: a [MapWithContentTemplate] with the map drawn by [CarMapSurfaceRenderer]
 * and a content pane that's either the friend list (sorted by distance) or, once something on the
 * map is tapped, a one-row [PaneTemplate] naming what was selected. View-only - no create/edit,
 * no search, no dialogs (see plans/ANDROID_AUTO_MAP_PLAN.md).
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
    private val friendsTitle = runBlocking { getString(Res.string.car_friends_title) }
    private val backTitle = runBlocking { getString(Res.string.car_back) }
    private val noFriendsMessage = runBlocking { getString(Res.string.no_friends) }

    private var ownLocation: LatLong? = null

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceRenderer)

        screenScope.launch {
            displayState.collect { invalidate() }
        }
        //Own position for sorting the friend list by distance - independent of the map's own blue
        //dot (see CarMapSurfaceRenderer), same split the phone code already has elsewhere.
        screenScope.launch {
            locationService.getLocationFlow(fastUpdates = false).collect { location ->
                ownLocation = location?.coordinates
                invalidate()
            }
        }
    }

    fun onCarConfigurationChanged() {
        _isDarkMode.value = carContext.isDarkMode
    }

    override fun onGetTemplate(): Template {
        val state = displayState.value

        val mapActionStrip = ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(
                Action.Builder()
                    .setIcon(iconOf(android.R.drawable.btn_plus))
                    .setOnClickListener { surfaceRenderer.zoomBy(1.0) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(iconOf(android.R.drawable.btn_minus))
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
                    .setIcon(iconOf(android.R.drawable.sym_contact_card))
                    .setOnClickListener { stateHolder.toggleShowUsers() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(iconOf(android.R.drawable.ic_menu_my_calendar))
                    .setOnClickListener { stateHolder.toggleShowEvents() }
                    .build()
            )
            .build()

        val mapController = MapController.Builder()
            .setMapActionStrip(mapActionStrip)
            .build()

        val contentTemplate: Template = when {
            state.selectedEntry != null -> selectionPane(state.selectedEntry.name, state.selectedEntry.description)
            state.selectedUser != null -> selectionPane(state.selectedUser.displayName, null)
            else -> friendListTemplate(state)
        }

        return MapWithContentTemplate.Builder()
            .setMapController(mapController)
            .setActionStrip(headerActionStrip)
            .setContentTemplate(contentTemplate)
            .build()
    }

    private fun selectionPane(title: String, subtitle: String?): PaneTemplate {
        val row = Row.Builder().setTitle(title)
        subtitle?.takeIf { it.isNotBlank() }?.let { row.addText(it) }

        val pane = Pane.Builder()
            .addRow(row.build())
            .addAction(
                Action.Builder()
                    .setTitle(backTitle)
                    .setOnClickListener { stateHolder.clearSelection() }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle(title)
            .build()
    }

    private fun friendListTemplate(state: SchneaggmapState): ListTemplate {
        val contentLimit = runCatching {
            carContext.getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
        }.getOrDefault(FALLBACK_CONTENT_LIMIT)

        val origin = ownLocation
        val sortedFriends = if (origin != null) {
            state.usersWithLocation.sortedBy { user ->
                user.location?.let { distanceMeters(origin, LatLong(lat = it.lat, long = it.long)) }
                    ?: Double.MAX_VALUE
            }
        } else {
            state.usersWithLocation
        }

        val itemListBuilder = ItemList.Builder().setNoItemsMessage(noFriendsMessage)
        sortedFriends.take(contentLimit).forEach { user ->
            val userLatLong = user.location?.let { LatLong(lat = it.lat, long = it.long) }
            val distanceText = if (origin != null && userLatLong != null) {
                formatDistance(distanceMeters(origin, userLatLong))
            } else null

            val rowBuilder = Row.Builder()
                .setTitle(user.displayName)
                .setOnClickListener {
                    userLatLong?.let { surfaceRenderer.focusOn(it) }
                }
            distanceText?.let { rowBuilder.addText(it) }
            itemListBuilder.addItem(rowBuilder.build())
        }

        return ListTemplate.Builder()
            .setTitle(friendsTitle)
            .setSingleList(itemListBuilder.build())
            .build()
    }

    private fun iconOf(resId: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, resId)).build()
}
