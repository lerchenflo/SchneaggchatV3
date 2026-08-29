package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.events.domain.Event

@Stable
data class EventsState(
    val events: List<Event> = emptyList(),
    val eventDayGroups: List<EventDayGroup> = emptyList(),
    val selectedEvent: Event? = null,
    val friendsById: Map<String, User> = emptyMap(),
    val groups: List<Group> = emptyList(),
    val isJoiningEvent: Boolean = false,
    val mapStyleUrl: String = MapStyleSetting.LIBERTY.tileUrl,
    val isMobile: Boolean = true,
)

/**
 * All events starting on the same calendar day, shown under one sticky date header.
 */
@Stable
data class EventDayGroup(
    val dayId: String,     // stable LazyColumn key, e.g. "day_2026-08-29"
    val dateMillis: Long,  // representative millis of that day (first event's startDate)
    val events: List<Event>
)

sealed interface EventsAction {
    data class OnEventClick(val eventId: String) : EventsAction

    data object OnCreateNewEventButtonClick: EventsAction

    data object OnEventPopupDismiss: EventsAction

    data class OnSaveEvent(val event: Event, val typeIcon: ImageBitmap?): EventsAction

    data class OnJoinEvent(val eventId: String): EventsAction

    data class OnPickLocationClick(val currentEventState: Event): EventsAction

    data class OnDeleteEvent(val event: Event, val deleteGroup: Boolean): EventsAction
}
