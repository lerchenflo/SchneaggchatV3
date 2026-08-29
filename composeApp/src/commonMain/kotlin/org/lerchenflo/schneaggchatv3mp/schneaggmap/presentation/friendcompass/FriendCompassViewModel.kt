package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.friendcompass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService

class FriendCompassViewModel(
    targetUserId: String?,
    private val navigator: Navigator,
    appRepository: AppRepository,
    locationService: LocationService,
) : ViewModel() {

    private val _state = MutableStateFlow(FriendCompassState(targetUserId = targetUserId))
    val state = _state.asStateFlow()

    init {
        val ownUserId = SessionCache.requireLoggedIn()?.userId

        viewModelScope.launch {
            appRepository.getFriendsFlow("").collect { users ->
                _state.update { currentState ->
                    currentState.copy(
                        friends = users.filter { it.isLocationValid() && it.id != ownUserId }
                    )
                }
            }
        }

        viewModelScope.launch {
            locationService.getLocationFlow().collect { fix ->
                fix?.let { deviceLocation ->
                    _state.update {
                        it.copy(
                            ownLocation = deviceLocation.coordinates,
                            azimuthDegrees = deviceLocation.heading?.toFloat() ?: it.azimuthDegrees
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: FriendCompassAction) {
        when (action) {
            FriendCompassAction.OnBackClick -> {
                viewModelScope.launch { navigator.navigateBack() }
            }

            is FriendCompassAction.OnFriendClick -> {
                _state.update { it.copy(targetUserId = action.userId) }
            }
        }
    }
}
