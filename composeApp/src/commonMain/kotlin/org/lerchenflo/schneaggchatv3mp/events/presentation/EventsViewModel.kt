package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.events.data.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import kotlin.time.Clock

class EventsViewModel(
    private val navigator: Navigator,
    private val eventRepository: EventRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EventsState())
    val state = combine(
        _state,
        eventRepository.getAllEventsFlow(),
    ) { currentState, events ->
        currentState.copy(
            events = events
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
                            startDate = now,
                            closeDate = null,
                            invitedUsers = emptyList(),
                            public = false,
                            createdAt = now,
                            updatedAt = now,
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

            is EventsAction.OnCreateNewEventSave -> {
                val event = action.event
                viewModelScope.launch {
                    appRepository.upsertEvent(
                        eventId = null,
                        type = event.type,
                        title = event.title,
                        description = event.description,
                        groupId = "",
                        location = event.location,
                        startDate = event.startDate,
                        closeDate = event.closeDate,
                        invitedUsers = event.invitedUsers,
                        public = event.public
                    )
                }
            }
            is EventsAction.OnJoinEvent -> {
                viewModelScope.launch {
                    appRepository.joinEvent(
                        action.eventId
                    )
                }
            }
        }
    }
}
