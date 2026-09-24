package org.lerchenflo.schneaggchatv3mp.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.car_filter_locations
import schneaggchatv3mp.composeapp.generated.resources.car_filter_radar
import schneaggchatv3mp.composeapp.generated.resources.car_filters_title
import schneaggchatv3mp.composeapp.generated.resources.events_screen_title
import schneaggchatv3mp.composeapp.generated.resources.location_type_user

/**
 * Pushed from [SchneaggmapCarScreen]'s header action strip - the car equivalent of the phone's
 * [org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.ShownLocationsDropdown],
 * collapsed to 4 toggle rows (friends, events, locations, radar) since the phone's per-type group
 * tree has no room on a driving screen. See [CarMapStateHolder]'s toggle functions for how
 * "locations" groups every [LocationType] except [LocationType.RADAR].
 */
class CarMapFiltersScreen(
    carContext: CarContext,
    private val stateHolder: CarMapStateHolder,
    private val displayState: StateFlow<SchneaggmapState>,
) : Screen(carContext) {

    private val screenScope = newLifecycleScope()

    //onGetTemplate() must return synchronously; see SchneaggmapCarScreen for why these are cached.
    private val filtersTitle = runBlocking { getString(Res.string.car_filters_title) }
    private val usersTitle = runBlocking { getString(Res.string.location_type_user) }
    private val eventsTitle = runBlocking { getString(Res.string.events_screen_title) }
    private val locationsTitle = runBlocking { getString(Res.string.car_filter_locations) }
    private val radarTitle = runBlocking { getString(Res.string.car_filter_radar) }

    init {
        screenScope.launch {
            displayState.collect { invalidate() }
        }
    }

    override fun onGetTemplate(): Template {
        val state = displayState.value
        val showRadar = LocationType.RADAR in state.enabledTypes
        val showOtherLocations = state.enabledTypes.any { it != LocationType.RADAR }

        val itemList = ItemList.Builder()
            .addItem(toggleRow(usersTitle, state.showUsers, stateHolder::toggleShowUsers))
            .addItem(toggleRow(eventsTitle, state.showEvents, stateHolder::toggleShowEvents))
            .addItem(toggleRow(locationsTitle, showOtherLocations, stateHolder::toggleShowOtherLocations))
            .addItem(toggleRow(radarTitle, showRadar, stateHolder::toggleShowRadar))
            .build()

        return ListTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(filtersTitle)
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .setSingleList(itemList)
            .build()
    }

    private fun toggleRow(title: String, checked: Boolean, onToggle: () -> Unit): Row =
        Row.Builder()
            .setTitle(title)
            .setToggle(
                Toggle.Builder { onToggle() }
                    .setChecked(checked)
                    .build()
            )
            .build()
}
