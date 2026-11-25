@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
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
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.reportErrorTo
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route.Chat
import org.lerchenflo.schneaggchatv3mp.chat.domain.SnailTrailPoint
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.newEvent
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapLayersSource
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapLayersAction
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapLayersDisplay
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.reduce
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.showingTypes
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.withPlacesDropdownClosed
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.stringRes
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.schneaggmap_entry_saved
import kotlin.time.Clock


@OptIn(ExperimentalCoroutinesApi::class)
class SchneaggmapViewModel(
    private val errorChannel: ErrorChannel,
    private val navigator: Navigator,
    private val mapRepository: MapRepository,
    private val dataSyncCoordinator: DataSyncCoordinator,
    private val settingsRepository: SettingsRepository,
    private val locationService: LocationService,
    private val userRepository: UserRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val mapLayersSource: MapLayersSource,

    private val initialEntryId: String?,
    private val currentlyEditedEvent: Event?,
    private val focusArea: List<LatLong> = emptyList(),
    private val focusUserId: String? = null,
) : ViewModel() {

    val pickLocationMode = currentlyEditedEvent != null

    //Picking a coordinate hides the filter chips, so nothing could turn the sightings off again
    private val initialState = SchneaggmapState(
        pickLocationMode = pickLocationMode,
        layerDisplay = MapLayersDisplay(showActiveSightings = !pickLocationMode),
    )
    private val _state = MutableStateFlow(initialState)

    val state = _state.map { state ->
        val entries = state.layers.entries

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

        state.copy(searchResults = results)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = initialState,
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
            is SchneaggmapAction.OnLayersAction -> _state.update {
                it.copy(
                    layerDisplay = it.layerDisplay.reduce(action.action),
                    // Opening or closing the place types closes the open entry
                    selectedEntry = if (action.action == MapLayersAction.TogglePlacesDropdown) null else it.selectedEntry,
                )
            }
            is SchneaggmapAction.SelectEntry -> _state.update {
                it.copy(selectedEntry = action.entry, layerDisplay = it.layerDisplay.withPlacesDropdownClosed())
            }

            is SchneaggmapAction.OnMapClick -> {
                println("Onclick. Longclick: ${action.longClick}")

                // In pickLocationMode the user places the pin by panning the map (see
                // LocationPickOverlay's fixed center pin) - taps/long-presses shouldn't also
                // open the unrelated "create a public map entry" flow.
                if (_state.value.pickLocationMode) return

                //Dismiss filter dropdown on map click
                if (_state.value.layerDisplay.isPlacesDropdownVisible) {
                    _state.update {
                        it.copy(layerDisplay = it.layerDisplay.withPlacesDropdownClosed())
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

                val selectedEntry = state.value.layers.entries.firstOrNull { it.id == action.entryId}

                if (selectedEntry == null) {
                    println("selected entry is null")

                }

                _state.update { currentState ->
                    currentState.copy(
                        selectedEntry = selectedEntry,
                        layerDisplay = currentState.layerDisplay.withPlacesDropdownClosed()
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
                _state.update { it.copy(focusEntryTarget = null, focusAreaTarget = null) }
            }

            is SchneaggmapAction.OnEntryPopupSave -> {
                val entry = action.entry


                require(entry.name.isNotEmpty()) {return}
                require(entry.locationData.isNotEmpty()) {return}

                _state.update { s ->
                    s.copy(
                        selectedEntry = if (action.keepPopupOpen) entry else null,
                        layerDisplay = s.layerDisplay.showingTypes(entry.locationData.map { it.locationtype })
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
                    ).reportErrorTo(errorChannel)
                        // The popup already closed (or stays open on a quick change like the radar's
                        // "Now"), so nothing else shows it worked: confirm once the server saved it
                        .onSuccess { SnackbarManager.showMessage(getString(Res.string.schneaggmap_entry_saved)) }
                }
            }

            is SchneaggmapAction.OnEntryPopupDelete -> {
                viewModelScope.launch {
                    mapRepository.deleteMapEntry(action.entryId).reportErrorTo(errorChannel)
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
                val selectedUser = state.value.layers.usersWithLocation.firstOrNull { it.id == action.userId }

                _state.update { currentState ->
                    currentState.copy(
                        selectedUser = selectedUser,
                        layerDisplay = currentState.layerDisplay.withPlacesDropdownClosed()
                    )
                }
            }

            SchneaggmapAction.OnOwnUserClick -> {
                _state.update { currentState ->
                    currentState.copy(
                        selectedUser = currentState.layers.ownUser,
                        layerDisplay = currentState.layerDisplay.withPlacesDropdownClosed()
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

            is SchneaggmapAction.OnEventPinClick -> {
                viewModelScope.launch {
                    navigator.navigate(Route.EventDetail(eventId = action.eventId))
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
                        layerDisplay = it.layerDisplay.showingTypes(action.entry.locationData.map { data -> data.locationtype }) //Enable the entries types on the map
                    )
                }

            }
        }
    }

    init {
        // The editor of the open entry, looked up when this device does not know them
        viewModelScope.launch {
            _state
                .map { it.selectedEntry?.updatedBy?.takeIf { editorId -> editorId.isNotBlank() } }
                .distinctUntilChanged()
                .flatMapLatest { editorId ->
                    if (editorId == null) flowOf(null) else userIdentityRepository.observeUser(editorId).map { it.name }
                }
                .collect { editorName -> _state.update { it.copy(selectedEntryEditorName = editorName) } }
        }

        // Opened on positions (a map preview): one is centered, several are fitted into view
        when {
            focusArea.size == 1 -> _state.update { it.copy(focusEntryTarget = focusArea.first(), layerDisplay = it.layerDisplay.withPlacesDropdownClosed()) }
            focusArea.size > 1 -> _state.update { it.copy(focusAreaTarget = focusArea, layerDisplay = it.layerDisplay.withPlacesDropdownClosed()) }
        }

        // Opened on a user (a chat's map preview): open their card and fly to them once they show
        // up with a valid location
        if (focusUserId != null) {
            viewModelScope.launch {
                val user = _state.mapNotNull { state -> state.layers.usersWithLocation.firstOrNull { it.id == focusUserId } }.first()
                val location = user.location ?: return@launch
                _state.update {
                    it.copy(
                        selectedUser = user,
                        focusEntryTarget = LatLong(lat = location.lat, long = location.long),
                        layerDisplay = it.layerDisplay.withPlacesDropdownClosed(),
                    )
                }
            }
        }

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
                        // Make sure the entry's marker is actually visible on the map
                        layerDisplay = it.layerDisplay
                            .withPlacesDropdownClosed()
                            .showingTypes(entry.locationData.map { data -> data.locationtype })
                    )
                }
            }
        }

        viewModelScope.launch {
            dataSyncCoordinator.dataSync(reason = "mapInit")
        }

        // Places, friends and events, the same as on every other map
        viewModelScope.launch {
            mapLayersSource.observe().collect { layers -> _state.update { it.copy(layers = layers) } }
        }

        viewModelScope.launch {
            combine(
                settingsRepository.getSortMapUsersByPinnedFlow(),
                settingsRepository.getPinnedChatsFlow(),
            ) { sortByPinned, pinnedChats ->
                if (sortByPinned) pinnedChats.filterNot { it.group }.map { it.chatId }.toSet() else emptySet()
            }.collectLatest { pinnedFriendIds ->
                _state.update { it.copy(pinnedFriendIds = pinnedFriendIds) }
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

        // Only fetched while the toggle is on, and only for friends currently shown on the map
        // plus our own trail - re-subscribes whenever any of those changes.
        viewModelScope.launch {
            _state
                .map { state ->
                    val ownId = SessionCache.requireLoggedIn()?.userId
                    val userIds = (state.layers.usersWithLocation.map { user -> user.id } + listOfNotNull(ownId)).distinct()
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
