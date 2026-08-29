package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.events.data.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.newEvent
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_and_group_created
import schneaggchatv3mp.composeapp.generated.resources.event_delete_failed
import kotlin.time.Duration.Companion.milliseconds

class EventsViewModel(
    private val navigator: Navigator,
    private val eventRepository: EventRepository,
    private val appRepository: AppRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val preferenceManager: Preferencemanager,
    private val pictureManager: PictureManager,

    private val initialEntryId: String? = null,
    private val initialEntry: Event? = null
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState(isMobile = appRepository.appVersion.isMobile(), selectedEvent = initialEntry))
    val state = combine(
        _state,
        eventRepository.getAllEventsFlow(),
        userRepository.getAllUsersFlow(),
        groupRepository.getAllGroupswithMembersFlow(),
        preferenceManager.getMapStyleUrlFlow(),
    ) { currentState, events, users, groups, mapStyleUrl ->
        val friendsMap = users
            .filter { it.friendshipStatus == NetworkUtils.FriendshipStatus.ACCEPTED }
            .associateBy { it.id }
        currentState.copy(
            events = events,
            friendsById = friendsMap,
            groups = groups,
            mapStyleUrl = mapStyleUrl
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsState(isMobile = appRepository.appVersion.isMobile(), selectedEvent = initialEntry),
    )

    fun onAction(action: EventsAction) {
        when (action) {
            is EventsAction.OnEventClick -> {
                val clickedEvent = state.value.events.firstOrNull { it.id == action.eventId }
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            selectedEvent = clickedEvent
                        )
                    }
                }
            }

            EventsAction.OnCreateNewEventButtonClick -> {
                val userId = SessionCache.requireLoggedIn()?.userId ?: ""
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            selectedEvent = newEvent(creatorId = userId)
                        )
                    }
                }
            }

            EventsAction.OnEventPopupDismiss -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            selectedEvent = null
                        )
                    }
                }
            }

            is EventsAction.OnSaveEvent -> {
                val event = action.event
                viewModelScope.launch {
                    val profilePic = action.typeIcon?.let { pictureManager.encodeImageBitmap(it) }
                    val eventid = if (event.id == "") null else event.id

                    appRepository.upsertEvent(
                        eventId = eventid,
                        type = event.type,
                        title = event.title,
                        description = event.description,
                        groupId = "",
                        location = event.location,
                        startDate = event.startDate,
                        closeDate = event.closeDate,
                        invitedUsers = event.invitedUsers,
                        visibility = event.visibility,
                        maxUsers = event.maxUsers,
                        groupDeleteDelay = event.groupDeleteDelay,
                        profilePic = profilePic,
                    )

                    //Newly created event
                    if (eventid == null) {
                        SnackbarManager.showMessage(getString(Res.string.event_and_group_created))
                    }

                    _state.update {
                        it.copy(
                            selectedEvent = null
                        )
                    }
                }
            }
            is EventsAction.OnPickLocationClick -> {
                viewModelScope.launch {
                    val passedEvent = action.currentEventState
                    _state.update {
                        it.copy(
                            selectedEvent = null
                        )
                    }
                    navigator.navigate(Route.Schneaggmap(currentlyEditedEvent = passedEvent), navigationOptions = Navigator.NavigationOptions(exitPreviousScreen = true))
                }
            }

            is EventsAction.OnJoinEvent -> {
                viewModelScope.launch {
                    // Guard set synchronously, before any suspend call, so a second
                    // near-simultaneous tap can't slip through while this coroutine is
                    // still suspended inside joinEvent() below.
                    if (_state.value.isJoiningEvent) return@launch
                    _state.update { it.copy(isJoiningEvent = true) }

                    try {
                        val groupId = appRepository.joinEvent(
                            action.eventId
                        )

                        if (groupId != null) {
                            appRepository.dataSync("Started after joining event to get messages")
                            _state.update { it.copy(selectedEvent = null) }
                            navigator.navigate(Route.Chat(chatId = groupId, isGroup = true))
                        }
                    } finally {
                        _state.update { it.copy(isJoiningEvent = false) }
                    }
                }
            }

            is EventsAction.OnDeleteEvent -> {
                viewModelScope.launch {
                    val success = appRepository.deleteEvent(
                        eventId = action.event.id,
                        groupId = action.event.groupId,
                        deleteConnectedGroup = action.deleteGroup,
                    )
                    if (!success) {
                        SnackbarManager.showMessage(getString(Res.string.event_delete_failed))
                    }
                    _state.update { it.copy(selectedEvent = null) }
                }
            }
        }
    }


    init {
        viewModelScope.launch {
            if (initialEntryId != null) {
                var tryCount = 0
                while (!state.value.events.any { it.id == initialEntryId } && 25 > tryCount ) {
                    delay(50.milliseconds)
                    tryCount++
                }

                onAction(EventsAction.OnEventClick(initialEntryId))
            }
        }

    }
}
