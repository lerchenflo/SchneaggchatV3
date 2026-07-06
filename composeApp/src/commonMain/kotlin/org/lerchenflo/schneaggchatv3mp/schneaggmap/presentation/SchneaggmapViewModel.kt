@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.GlobalViewModel
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.chat.domain.toSelectedChat
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.MapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import kotlin.time.Clock


class SchneaggmapViewModel(
    private val navigator: Navigator,
    private val mapRepository: MapRepository,
    private val appRepository: AppRepository,
    private val preferenceManager: Preferencemanager,
    private val globalViewModel: GlobalViewModel,
    private val locationService: LocationService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SchneaggmapState())

    val state = combine(
        _state,
        mapRepository.getAllMapEntriesFlow(),
        userRepository.onlineFriendIdsFlow,
    ) { state, entries, onlineFriendIds ->

        state.copy(
            entries = entries,
            onlineFriendIds = onlineFriendIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SchneaggmapState(),
    )


    fun onAction(action: SchneaggmapAction) {
        when (action) {
            SchneaggmapAction.OnBackClicked -> viewModelScope.launch { navigator.navigateBack() }
            SchneaggmapAction.ToggleFilterDropdown -> _state.update {
                it.copy(isFilterDropdownVisible = !it.isFilterDropdownVisible, selectedEntry = null)
            }
            is SchneaggmapAction.ToggleMainType -> {
                val enabled = state.value.enabledTypes
                val updated = if (action.key in enabled) enabled - action.key else enabled + action.key

                _state.update { it.copy(enabledTypes = updated) }
            }
            is SchneaggmapAction.SelectEntry -> _state.update {
                it.copy(selectedEntry = action.entry, isFilterDropdownVisible = false)
            }

            is SchneaggmapAction.OnMapClick -> {
                println("Onclick. Longclick: ${action.longClick}")

                //Dismiss filter dropdown on map click
                if (_state.value.isFilterDropdownVisible) {
                    _state.update {
                        it.copy(isFilterDropdownVisible = false)
                    }
                }

                //On longclick show add location dialog
                if (action.longClick) {
                    _state.update {
                        it.copy(selectedEntry = MapEntry(
                            id = "",
                            coordinates = action.coordinates,
                            name = "",
                            description = "",
                            locationData = emptyList(),
                            createdBy = "",
                            createdAt = Clock.System.now().toEpochMilliseconds(),
                            updatedBy = "",
                            updatedAt = Clock.System.now().toEpochMilliseconds()
                        ))
                    }
                } else {

                    //Dismiss popup
                    if (_state.value.selectedEntry != null) {
                        _state.update {
                            it.copy(selectedEntry = null)
                        }
                    }
                }
            }

            is SchneaggmapAction.OnEntryClick -> {
                println("On entry clicked: ${action.entryId}")

                val selectedEntry = state.value.entries.firstOrNull { it.id == action.entryId}

                if (selectedEntry == null) {
                    println("selected entry is null")

                }

                _state.update { currentState ->
                    currentState.copy(
                        selectedEntry = selectedEntry,
                        isFilterDropdownVisible = false
                    )
                }
            }

            SchneaggmapAction.OnPopupDismiss -> {
                _state.update {
                    it.copy(
                        selectedEntry = null,
                        selectedUser = null
                    )
                }
            }

            is SchneaggmapAction.OnEntryPopupSave -> {
                val entry = action.entry


                require(entry.name.isNotEmpty()) {return}
                require(entry.locationData.isNotEmpty()) {return}

                _state.update {
                    it.copy(
                        selectedEntry = null
                    )
                }

                viewModelScope.launch {

                    println("Map upsert: $entry")

                    appRepository.upsertMapEntry(
                        entryId = entry.id.ifEmpty { null },
                        name = entry.name,
                        description = entry.description,
                        lat = entry.coordinates.lat,
                        lon = entry.coordinates.long,
                        locationData = entry.locationData
                    )
                }
            }

            is SchneaggmapAction.OnEntryPopupDelete -> {
                viewModelScope.launch {
                    appRepository.deleteMapEntry(action.entryId)
                }

                _state.update {
                    it.copy(
                        selectedEntry = null
                    )
                }
            }

            SchneaggmapAction.OnSettingsClick -> {
                viewModelScope.launch {
                    navigator.navigateToSubRoute(
                        rootRoute = Route.Settings,
                        destination = Route.Settings.SchneaggmapSettings
                    )
                }
            }

            is SchneaggmapAction.OnUserClick -> {
                val selectedUser = state.value.usersWithLocation.firstOrNull { it.id == action.userId }

                _state.update { currentState ->
                    currentState.copy(
                        selectedUser = selectedUser,
                        isFilterDropdownVisible = false
                    )
                }
            }

            is SchneaggmapAction.OnOpenChatClick -> {
                viewModelScope.launch {
                    globalViewModel.onSelectChat(
                        action.user.toSelectedChat(
                            unreadCount = 0,
                            unsentCount = 0,
                            lastMessage = null
                        )
                    )
                    navigator.navigate(Route.Chat)
                }

                _state.update { it.copy(selectedUser = null) }
            }

            SchneaggmapAction.ToggleSnailTrails -> _state.update {
                it.copy(showSnailTrails = !it.showSnailTrails)
            }
        }
    }

    init {
        viewModelScope.launch {
            appRepository.dataSync()
        }

        viewModelScope.launch {
            preferenceManager.getMergeMapLocationsFlow()
                .collectLatest { clustering ->
                    _state.update {
                        it.copy(
                            useClustering = clustering
                        )
                    }
                }
        }

        viewModelScope.launch {
            preferenceManager.getMapStyleSettingFlow()
                .collectLatest { style ->
                    _state.update { it.copy(mapStyleUrl = style.tileUrl) }
                }
        }

        // Permission for showing our own position on the map (separate from the "share my
        // location with friends" setting, which drives server-side sync via GlobalViewModel).
        viewModelScope.launch {
            var permission = locationService.checkLocationPermission()
            if (permission != PermissionState.GRANTED) {
                permission = locationService.requestLocationPermission()
            }
            _state.update { it.copy(locationPermissionGranted = permission == PermissionState.GRANTED) }
        }

        viewModelScope.launch {
            val ownId = SessionCache.requireLoggedIn()?.userId ?: return@launch
            appRepository.getUserByIdFlow(ownId).collectLatest { ownUser ->
                _state.update { it.copy(ownLocationShared = ownUser?.locationShared ?: false) }
            }
        }

        viewModelScope.launch {
            appRepository.getFriendsFlow("")
                .collectLatest { userList ->
                    userList.mapNotNull {
                        if (it.isLocationValid()) {
                            it
                        } else null
                    }.also {
                        _state.update { state ->
                            state.copy(
                                usersWithLocation = it
                            )
                    } }
                }
        }

        // Only fetched while the toggle is on, and only for friends currently shown on the map
        // plus our own trail - re-subscribes whenever any of those changes.
        viewModelScope.launch {
            _state
                .map { state ->
                    val ownId = SessionCache.requireLoggedIn()?.userId
                    val userIds = (state.usersWithLocation.map { user -> user.id } + listOfNotNull(ownId)).distinct()
                    state.showSnailTrails to userIds
                }
                .distinctUntilChanged()
                .flatMapLatest { (enabled, userIds) ->
                    if (!enabled || userIds.isEmpty()) {
                        flowOf(emptyMap<String, List<SnailTrailPoint>>())
                    } else {
                        combine(userIds.map { id -> appRepository.getSnailTrailFlow(id).map { id to it } }) { pairs ->
                            pairs.toMap()
                        }
                    }
                }
                .collectLatest { trails ->
                    _state.update { it.copy(snailTrails = trails) }
                }
        }
    }


}
