package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.sampleSimplePlaceLocations
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.sampleUserLocations

class SchneaggmapViewModel(
    private val navigator: Navigator
) : ViewModel() {

    var state by mutableStateOf(
        SchneaggmapState(
            placeLocations = sampleSimplePlaceLocations,
            userLocations = sampleUserLocations,
        )
    )
        private set

    fun onAction(action: SchneaggmapAction) {
        when (action) {
            SchneaggmapAction.OnBackClicked -> viewModelScope.launch { navigator.navigateBack() }
            SchneaggmapAction.ToggleFilterDropdown -> state = state.copy(
                isFilterDropdownVisible = !state.isFilterDropdownVisible,
                selectedLocation = null,
            )
            is SchneaggmapAction.ToggleLocationType -> {
                val enabled = state.enabledTypes
                val updated = if (action.type in enabled) enabled - action.type else enabled + action.type
                state = state.copy(enabledTypes = updated)
            }
            is SchneaggmapAction.SelectLocation -> state = state.copy(
                selectedLocation = action.location,
                isFilterDropdownVisible = false,
            )
        }
    }

    fun onBackClick() = onAction(SchneaggmapAction.OnBackClicked)
}
