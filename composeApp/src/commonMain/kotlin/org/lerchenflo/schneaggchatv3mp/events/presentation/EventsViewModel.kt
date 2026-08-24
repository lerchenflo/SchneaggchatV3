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
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.events.data.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class EventsViewModel(
    private val navigator: Navigator,
    private val eventRepository: EventRepository,
    private val appRepository: AppRepository,
    private val userRepository: UserRepository,
    private val preferenceManager: Preferencemanager,
    private val pictureManager: PictureManager,
    private val initialEntryId: String? = null
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState())
    val state = combine(
        _state,
        eventRepository.getAllEventsFlow(),
        userRepository.getAllUsersFlow(),
        preferenceManager.getMapStyleUrlFlow(),
    ) { currentState, events, users, mapStyleUrl ->
        val friendsMap = users
            .filter { it.friendshipStatus == NetworkUtils.FriendshipStatus.ACCEPTED }
            .associateBy { it.id }
        currentState.copy(
            events = events,
            friendsById = friendsMap,
            mapStyleUrl = mapStyleUrl
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsState(),
    )

    fun onAction(action: EventsAction) {
        when (action) {
            is EventsAction.OnEventClick -> {
                val clickedEvent = state.value.events.firstOrNull { it.id == action.eventId }
                _state.update { currentState ->
                    currentState.copy(selectedEvent = clickedEvent)
                }
            }

            EventsAction.OnCreateNewEventButtonClick -> {
                val userId = SessionCache.requireLoggedIn()?.userId ?: ""
                val now = Clock.System.now().toEpochMilliseconds()
                val defaultStartDate = (Clock.System.now() + 1.days).toEpochMilliseconds()
                _state.update { currentState ->
                    currentState.copy(
                        selectedEvent = Event(
                            id = "",
                            creatorId = userId,
                            type = EventType.OTHER,
                            title = "",
                            description = "",
                            groupId = "",
                            location = null,
                            startDate = defaultStartDate,
                            closeDate = null,
                            invitedUsers = emptyList(),
                            visibility = EventVisibility.FRIENDS_ONLY,
                            createdAt = now,
                            updatedAt = now,
                            creatorName = "",
                        )
                    )
                }
            }

            EventsAction.OnEventPopupDismiss -> {
                _state.update { currentState ->
                    currentState.copy(
                        selectedEvent = null
                    )
                }
            }

            is EventsAction.OnSaveEvent -> {
                val event = action.event
                viewModelScope.launch {
                    val profilePic = action.typeIcon?.let { pictureManager.encodeImageBitmap(it) }
                    appRepository.upsertEvent(
                        eventId = if (event.id == "") null else event.id,
                        type = event.type,
                        title = event.title,
                        description = event.description,
                        groupId = "",
                        location = event.location,
                        startDate = event.startDate,
                        closeDate = event.closeDate,
                        invitedUsers = event.invitedUsers,
                        visibility = event.visibility,
                        profilePic = profilePic,
                    )
                }
                _state.update {
                    it.copy(
                        selectedEvent = null
                    )
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
