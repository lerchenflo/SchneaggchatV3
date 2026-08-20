package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.compose.runtime.Stable
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.utilities.UiText

@Stable
data class EventsState(
    val events: List<Event> = emptyList(),
    val selectedEvent: Event? = null
)

sealed interface EventsAction {
    data class OnEventClick(val eventId: String) : EventsAction

    data object OnCreateNewEventButtonClick: EventsAction
}
