package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation
/*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.login.presentation.signup.SignupState
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.Coordinate
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.MapLocation
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.samplePlaceLocations
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.map.sampleUserLocations


class SchneaggmapViewmodel(
    private val navigator: Navigator
): ViewModel() {
    var state by mutableStateOf(SchneaggmapState())
        private set

    init {
        state = state.copy(
            userLocations = sampleUserLocations,
            
            placeLocations = samplePlaceLocations
        )
    }



    fun onAction(action: SchneaggmapAction) {
        viewModelScope.launch {
            when (action) {
                SchneaggmapAction.OnBackClicked -> {
                    navigator.navigateBack()
                }
            }
        }
    }

}

 */