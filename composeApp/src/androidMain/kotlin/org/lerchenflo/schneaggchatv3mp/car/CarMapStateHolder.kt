package org.lerchenflo.schneaggchatv3mp.car

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.events.data.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.schneaggmap.data.MapRepository
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LocationType
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapAction
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState

/**
 * Builds a [SchneaggmapState] for the car surface from the same Room-backed repository flows
 * [org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapViewModel] combines for the
 * phone screen - but without a `Navigator` (the ViewModel navigates on several actions, which
 * makes no sense on the car) and without any of the phone-only UI state (dropdowns, search,
 * pick-location mode, dialogs).
 *
 * The car has no type-filter UI, so every [LocationType] is always enabled - the alternative
 * (nothing shown until someone opens a dropdown that doesn't exist on the car) would make the
 * screen useless out of the box.
 */
class CarMapStateHolder(
    private val mapRepository: MapRepository,
    private val appRepository: AppRepository,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val preferenceManager: Preferencemanager,
    scope: CoroutineScope,
) {

    private val _showUsers = MutableStateFlow(true)
    private val _showEvents = MutableStateFlow(false)
    private val _selectedEntryId = MutableStateFlow<String?>(null)
    private val _selectedUserId = MutableStateFlow<String?>(null)

    private data class FriendData(val friends: List<User>, val ownUser: User?)

    private val friendDataFlow: Flow<FriendData> = SessionCache.authState.flatMapLatest { auth ->
        val ownId = (auth as? SessionCache.AuthState.LoggedIn)?.userId
        if (ownId == null) {
            flowOf(FriendData(friends = emptyList(), ownUser = null))
        } else {
            combine(
                appRepository.getFriendsFlow(""),
                appRepository.getUserByIdFlow(ownId),
            ) { friends, ownUser -> FriendData(friends, ownUser) }
        }
    }

    private data class PrefsData(val useClustering: Boolean, val mergeUsers: Boolean, val mapStyle: MapStyleSetting)

    private val prefsFlow: Flow<PrefsData> = combine(
        preferenceManager.getMergeMapLocationsFlow(),
        preferenceManager.getMergeMapUsersFlow(),
        preferenceManager.getMapStyleSettingFlow(),
    ) { useClustering, mergeUsers, mapStyle -> PrefsData(useClustering, mergeUsers, mapStyle) }

    private data class UiToggles(
        val showUsers: Boolean,
        val showEvents: Boolean,
        val selectedEntryId: String?,
        val selectedUserId: String?,
    )

    private val uiTogglesFlow: Flow<UiToggles> = combine(
        _showUsers, _showEvents, _selectedEntryId, _selectedUserId,
    ) { showUsers, showEvents, entryId, userId ->
        UiToggles(showUsers, showEvents, entryId, userId)
    }

    private val baseFlow = combine(
        mapRepository.getAllMapEntriesFlow(),
        friendDataFlow,
        userRepository.onlineFriendIdsFlow,
        eventRepository.getAllEventsFlow(),
        prefsFlow,
    ) { entries, friendData, onlineFriendIds, events, prefs ->
        BaseState(entries, friendData, onlineFriendIds, events, prefs)
    }

    private data class BaseState(
        val entries: List<MapEntry>,
        val friendData: FriendData,
        val onlineFriendIds: Set<String>,
        val events: List<Event>,
        val prefs: PrefsData,
    )

    val state: StateFlow<SchneaggmapState> = combine(baseFlow, uiTogglesFlow) { base, toggles ->
        val usersWithLocation = base.friendData.friends.filter { it.isLocationValid() }

        SchneaggmapState(
            entries = base.entries,
            usersWithLocation = usersWithLocation,
            onlineFriendIds = base.onlineFriendIds,
            enabledTypes = LocationType.entries.toSet(),
            ownUser = base.friendData.ownUser,
            ownLocationShared = base.friendData.ownUser?.locationShared ?: false,
            useClustering = base.prefs.useClustering,
            mergeUsers = base.prefs.mergeUsers,
            showUsers = toggles.showUsers,
            eventsWithLocation = base.events.filter { it.location != null },
            showEvents = toggles.showEvents,
            mapStyle = base.prefs.mapStyle,
            mapStyleUrl = base.prefs.mapStyle.tileUrl,
            selectedEntry = base.entries.firstOrNull { it.id == toggles.selectedEntryId },
            selectedUser = usersWithLocation.firstOrNull { it.id == toggles.selectedUserId }
                ?: base.friendData.ownUser?.takeIf { it.id == toggles.selectedUserId },
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), SchneaggmapState())

    /** Adapts [SchneaggmapLayers]'s onAction callback - the car is view-only, so everything but marker selection is ignored. */
    fun handleAction(action: SchneaggmapAction) {
        when (action) {
            is SchneaggmapAction.OnEntryClick -> selectEntry(action.entryId)
            is SchneaggmapAction.OnUserClick -> selectUser(action.userId)
            SchneaggmapAction.OnPopupDismiss -> clearSelection()
            else -> Unit
        }
    }

    fun toggleShowUsers() {
        _showUsers.update { !it }
    }

    fun toggleShowEvents() {
        _showEvents.update { !it }
    }

    fun clearSelection() {
        _selectedEntryId.value = null
        _selectedUserId.value = null
    }

    private fun selectEntry(entryId: String) {
        _selectedEntryId.value = entryId
        _selectedUserId.value = null
    }

    private fun selectUser(userId: String) {
        _selectedUserId.value = userId
        _selectedEntryId.value = null
    }
}
