@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route.Chat
import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.statusOf
import org.lerchenflo.schneaggchatv3mp.events.domain.newEvent
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationGroup
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.stringRes
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import kotlin.time.Clock


class SchneaggmapViewModel(
    private val navigator: Navigator,
    private val mapRepository: MapRepository,
    private val dataSyncCoordinator: DataSyncCoordinator,
    private val settingsRepository: SettingsRepository,
    private val locationService: LocationService,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,

    private val initialEntryId: String?,
    private val currentlyEditedEvent: Event?
) : ViewModel() {

    val pickLocationMode = currentlyEditedEvent != null

    private val _state = MutableStateFlow(SchneaggmapState(pickLocationMode = pickLocationMode))

    // Driven off authState (not a one-shot read) so the event filter picks up the id once autologin finishes
    private val ownUserIdFlow = SessionCache.authState.map {
        (it as? SessionCache.AuthState.LoggedIn)?.userId
    }

    val state = combine(
        _state,
        mapRepository.getAllMapEntriesFlow(),
        userRepository.onlineFriendIdsFlow,
        eventRepository.getAllEventsFlow(),
        ownUserIdFlow,
    ) { state, entries, onlineFriendIds, events, ownUserId ->

        // Types the search is restricted to: explicitly picked types win within their group,
        // a selected group with no individual picks counts as the whole group.
        val searchFilterTypes = state.searchSelectedGroups.flatMapTo(mutableSetOf()) { group ->
            group.types.filter { it in state.searchSelectedTypes }.ifEmpty { group.types }
        }

        val results = if (state.searchTerm.isEmpty() && searchFilterTypes.isEmpty()) {
            emptyList()
        } else {
            // The term also matches localized type names (e.g. typing "Radar" finds all radars)
            val termMatchedTypes = if (state.searchTerm.isEmpty()) emptySet() else {
                LocationType.entries.filterTo(mutableSetOf()) {
                    getString(it.stringRes()).contains(state.searchTerm, ignoreCase = true)
                }
            }

            entries.filter { entry ->
                val matchesTerm = state.searchTerm.isEmpty()
                        || entry.name.contains(state.searchTerm, ignoreCase = true)
                        || entry.description.contains(state.searchTerm, ignoreCase = true)
                        || entry.locationData.any { it.locationtype in termMatchedTypes }

                val matchesTypeFilter = searchFilterTypes.isEmpty()
                        || entry.locationData.any { it.locationtype in searchFilterTypes }

                matchesTerm && matchesTypeFilter
            }
        }

        //println("Results: ${results.size}")

        // Events the user marked as not interested are hidden from the map
        val eventsWithLocation = events.filter { event ->
            event.location != null &&
                (ownUserId == null || event.statusOf(ownUserId) != EventParticipationStatus.DISMISSED)
        }

        state.copy(
            entries = entries,
            onlineFriendIds = onlineFriendIds,
            eventsWithLocation = eventsWithLocation,
            joinedEventIds = ownUserId?.let { id ->
                eventsWithLocation
                    .filter { it.statusOf(id) == EventParticipationStatus.ACCEPTED }
                    .mapTo(mutableSetOf()) { it.id }
            } ?: emptySet(),

            searchResults = results
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SchneaggmapState(pickLocationMode = pickLocationMode),
    )


    fun onAction(action: SchneaggmapAction) {
        when (action) {
            SchneaggmapAction.OnBackClicked -> viewModelScope.launch {
                navigator.navigateBack()
            }
            is SchneaggmapAction.OnConfirmLocationPick -> {

                val newEvent = currentlyEditedEvent?.copy(
                    location = action.coordinates
                )

                _state.update {
                    it.copy(
                        pickLocationMode = false
                    )
                }

                viewModelScope.launch { navigator.navigate(Route.Events(
                    selectedEvent = newEvent
                ), navigationOptions = Navigator.NavigationOptions(exitPreviousScreen = true)) } //Close screen to remove picking ui



            }
            SchneaggmapAction.OnCancelLocationPick -> {

                _state.update {
                    it.copy(
                        pickLocationMode = false
                    )
                }

                viewModelScope.launch { navigator.navigate(Route.Events(selectedEvent = currentlyEditedEvent),
                    navigationOptions = Navigator.NavigationOptions(exitPreviousScreen = true)) }

            }
            SchneaggmapAction.ToggleFilterDropdown -> _state.update {
                it.copy(
                    isFilterDropdownVisible = !it.isFilterDropdownVisible,
                    selectedEntry = null,
                )
            }
            SchneaggmapAction.ToggleAllPlaces -> {
                val allPlaceTypes = LocationGroup.entries.flatMapTo(mutableSetOf()) { it.types }

                // Anything shown (even only some types): hide all. Nothing shown: show every type
                _state.update {
                    it.copy(enabledTypes = if (it.enabledTypes.isNotEmpty()) emptySet() else allPlaceTypes)
                }
            }
            is SchneaggmapAction.ToggleMainType -> {
                val enabled = state.value.enabledTypes
                val updated = if (action.key in enabled) enabled - action.key else enabled + action.key

                _state.update { it.copy(enabledTypes = updated) }
            }
            is SchneaggmapAction.ToggleGroup -> {
                val enabled = state.value.enabledTypes
                val updated = if (enabled.containsAll(action.group.types)) {
                    enabled - action.group.types.toSet()
                } else {
                    enabled + action.group.types
                }

                _state.update { it.copy(enabledTypes = updated) }
            }
            is SchneaggmapAction.ToggleGroupExpanded -> {
                val expanded = state.value.expandedFilterGroups
                val updated = if (action.group in expanded) expanded - action.group else expanded + action.group

                _state.update { it.copy(expandedFilterGroups = updated) }
            }
            is SchneaggmapAction.SelectEntry -> _state.update {
                it.copy(selectedEntry = action.entry, isFilterDropdownVisible = false)
            }

            is SchneaggmapAction.OnMapClick -> {
                println("Onclick. Longclick: ${action.longClick}")

                // In pickLocationMode the user places the pin by panning the map (see
                // LocationPickOverlay's fixed center pin) - taps/long-presses shouldn't also
                // open the unrelated "create a public map entry" flow.
                if (_state.value.pickLocationMode) return

                //Dismiss filter dropdown on map click
                if (_state.value.isFilterDropdownVisible) {
                    _state.update {
                        it.copy(isFilterDropdownVisible = false)
                    }
                }

                //On longclick ask whether to create a map entry or an event here
                if (action.longClick) {
                    _state.update {
                        it.copy(currentlySelectedLocation = action.coordinates)
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

            SchneaggmapAction.OnCreateChoiceDismiss -> {
                _state.update { it.copy(currentlySelectedLocation = null) }
            }

            is SchneaggmapAction.OnCreateMapEntryChoice -> {

                _state.update {
                    it.copy(
                        currentlySelectedLocation = null,
                        selectedEntry = MapEntry(
                            id = "",
                            coordinates = action.location,
                            name = "",
                            description = "",
                            locationData = emptyList(),
                            createdBy = "",
                            createdAt = Clock.System.now().toEpochMilliseconds(),
                            updatedBy = "",
                            updatedAt = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
            }

            is SchneaggmapAction.OnCreateEventChoice -> {
                val userId = SessionCache.requireLoggedIn()?.userId ?: ""

                _state.update { it.copy(currentlySelectedLocation = null) }

                viewModelScope.launch {
                    navigator.navigate(Route.Events(
                        selectedEvent = newEvent(creatorId = userId, location = action.location)
                    ))
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

            SchneaggmapAction.OnFocusEntryHandled -> {
                _state.update { it.copy(focusEntryTarget = null) }
            }

            is SchneaggmapAction.OnEntryPopupSave -> {
                val entry = action.entry


                require(entry.name.isNotEmpty()) {return}
                require(entry.locationData.isNotEmpty()) {return}

                _state.update { s ->
                    s.copy(
                        selectedEntry = if (action.keepPopupOpen) entry else null,
                        enabledTypes = s.enabledTypes + entry.locationData.map { it.locationtype }
                    )
                }

                viewModelScope.launch {

                    println("Map upsert: $entry")

                    mapRepository.upsertMapEntry(
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
                    mapRepository.deleteMapEntry(action.entryId)
                }

                _state.update {
                    it.copy(
                        selectedEntry = null
                    )
                }
            }

            SchneaggmapAction.OnSettingsClick -> {
                viewModelScope.launch {
                    navigator.navigate(
                        destination = Route.SchneaggmapSettings
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

            SchneaggmapAction.OnOwnUserClick -> {
                _state.update { currentState ->
                    currentState.copy(
                        selectedUser = currentState.ownUser,
                        isFilterDropdownVisible = false
                    )
                }
            }

            is SchneaggmapAction.OnOpenChatClick -> {
                viewModelScope.launch {
                    navigator.navigate(Chat(chatId = action.user.id, isGroup = false))
                }

                _state.update { it.copy(selectedUser = null) }
            }

            is SchneaggmapAction.OnOpenCompassClick -> {
                viewModelScope.launch {
                    navigator.navigate(Route.FriendCompass(targetUserId = action.user.id))
                }

                _state.update { it.copy(selectedUser = null) }
            }

            SchneaggmapAction.ToggleSnailTrails -> _state.update {
                it.copy(showSnailTrails = !it.showSnailTrails)
            }

            SchneaggmapAction.ToggleShowUsers -> {
                _state.update {
                    it.copy(
                        showUsers = !it.showUsers
                    )
                }
            }

            SchneaggmapAction.ToggleShowEvents -> {
                _state.update {
                    it.copy(
                        showEvents = !it.showEvents
                    )
                }
            }

            is SchneaggmapAction.OnEventPinClick -> {
                viewModelScope.launch {
                    navigator.navigate(Route.Events(selectedEventId = action.eventId))
                }
            }

            is SchneaggmapAction.OnSearchTermChange -> {
                _state.update {
                    it.copy(
                        searchTerm = action.newTerm
                    )
                }
            }

            is SchneaggmapAction.OnSearchGroupToggle -> {
                _state.update {
                    val deselect = action.group in it.searchSelectedGroups
                    it.copy(
                        searchSelectedGroups = if (deselect) it.searchSelectedGroups - action.group else it.searchSelectedGroups + action.group,
                        // Deselecting a group also drops its individual type picks
                        searchSelectedTypes = if (deselect) it.searchSelectedTypes - action.group.types.toSet() else it.searchSelectedTypes
                    )
                }
            }

            is SchneaggmapAction.OnSearchTypeToggle -> {
                _state.update {
                    it.copy(
                        searchSelectedTypes = if (action.type in it.searchSelectedTypes) {
                            it.searchSelectedTypes - action.type
                        } else {
                            it.searchSelectedTypes + action.type
                        }
                    )
                }
            }

            SchneaggmapAction.OnSearchClose -> {
                _state.update {
                    it.copy(
                        searchTerm = "",
                        searchSelectedGroups = emptySet(),
                        searchSelectedTypes = emptySet()
                    )
                }
            }

            is SchneaggmapAction.OnSearchResultClick -> {
                _state.update {
                    it.copy(
                        selectedEntry = action.entry, //Show popup for this entry
                        focusEntryTarget = action.entry.coordinates, //Zoom to this entry
                        enabledTypes = it.enabledTypes + action.entry.locationData.map { data -> data.locationtype } //Enable the entries types on the map
                    )
                }

            }
        }
    }

    init {
        // Opened with a specific entry (deep link): once the entry shows up in the database,
        // show its popup and hand the screen a one-shot camera target to fly to.
        if (initialEntryId != null) {
            viewModelScope.launch {
                val entry = mapRepository.getAllMapEntriesFlow()
                    .mapNotNull { entries -> entries.firstOrNull { it.id == initialEntryId } }
                    .first()

                _state.update {
                    it.copy(
                        selectedEntry = entry,
                        focusEntryTarget = entry.coordinates,
                        isFilterDropdownVisible = false,
                        // Make sure the entry's marker is actually visible on the map
                        enabledTypes = it.enabledTypes + entry.locationData.map { data -> data.locationtype }
                    )
                }
            }
        }

        viewModelScope.launch {
            dataSyncCoordinator.dataSync(reason = "mapInit")
        }

        viewModelScope.launch {
            settingsRepository.getMergeMapLocationsFlow()
                .collectLatest { clustering ->
                    _state.update {
                        it.copy(
                            useClustering = clustering
                        )
                    }
                }
        }

        viewModelScope.launch {
            settingsRepository.getMergeMapUsersFlow()
                .collectLatest { mergeUsers ->
                    _state.update {
                        it.copy(
                            mergeUsers = mergeUsers
                        )
                    }
                }
        }

        viewModelScope.launch {
            settingsRepository.getMapStyleSettingFlow()
                .collectLatest { style ->
                    _state.update { it.copy(mapStyle = style, mapStyleUrl = style.tileUrl) }
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
            userRepository.getUserByIdFlow(ownId).collectLatest { ownUser ->
                _state.update { it.copy(ownLocationShared = ownUser?.locationShared ?: false, ownUser = ownUser) }
            }
        }

        viewModelScope.launch {
            userRepository.getFriendsFlow("")
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
                        combine(userIds.map { id -> userRepository.getSnailTrailFlow(id).map { id to it } }) { pairs ->
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
