package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.compose.runtime.Stable
import org.lerchenflo.schneaggchatv3mp.events.domain.Event

@Stable
data class EventsState(
    val events: List<Event> = emptyList(),
    val selectedEvent: Event? = null
)

sealed interface EventsAction {
    data class OnEventClick(val eventId: String) : EventsAction

    data object OnCreateNewEventButtonClick: EventsAction

    data object OnEventPopupDismiss: EventsAction
}
