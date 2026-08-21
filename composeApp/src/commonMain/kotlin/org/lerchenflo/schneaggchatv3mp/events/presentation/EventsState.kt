package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.compose.runtime.Stable
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event

@Stable
data class EventsState(
    val events: List<Event> = emptyList(),
    val selectedEvent: Event? = null,
    val friendsById: Map<String, User> = emptyMap()
)

sealed interface EventsAction {
    data class OnEventClick(val eventId: String) : EventsAction

    data object OnCreateNewEventButtonClick: EventsAction

    data object OnEventPopupDismiss: EventsAction

    data class OnSaveEvent(val event: Event): EventsAction

    data class OnJoinEvent(val eventId: String): EventsAction
}
