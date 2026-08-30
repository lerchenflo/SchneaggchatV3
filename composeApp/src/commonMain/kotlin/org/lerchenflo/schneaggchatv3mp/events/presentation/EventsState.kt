package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import kotlin.time.Clock

enum class EventsViewMode {
    LIST, WEEK, MONTH
}

@Stable
data class EventsState(
    val events: List<Event> = emptyList(),
    val eventDayGroups: List<EventDayGroup> = emptyList(),
    val eventsByDate: Map<LocalDate, List<Event>> = emptyMap(),
    val selectedEvent: Event? = null,
    val friendsById: Map<String, User> = emptyMap(),
    val groups: List<Group> = emptyList(),
    val isJoiningEvent: Boolean = false,
    val mapStyleUrl: String = MapStyleSetting.LIBERTY.tileUrl,
    val isMobile: Boolean = true,
    val viewMode: EventsViewMode = EventsViewMode.WEEK,
    val calendarAnchorDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val selectedCalendarDay: LocalDate? = null,
    val birthdaysByMonthDay: Map<Int, List<CalendarBirthday>> = emptyMap(),
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

    data class OnSaveEvent(val event: Event, val typeIcon: ImageBitmap?, val createGroup: Boolean): EventsAction

    data class OnJoinEvent(val eventId: String): EventsAction

    data class OnPickLocationClick(val currentEventState: Event): EventsAction

    data class OnDeleteEvent(val event: Event, val deleteGroup: Boolean, val deleteEvent: Boolean): EventsAction

    data class OnViewModeChange(val mode: EventsViewMode): EventsAction

    /** Moves the week/month view forward ([forward] = true) or back one period. */
    data class OnCalendarNavigate(val forward: Boolean): EventsAction

    data object OnCalendarJumpToToday: EventsAction

    data class OnCalendarDayClick(val date: LocalDate): EventsAction

    data object OnCalendarDayDetailDismiss: EventsAction

    data class OnBirthdayClick(val userId: String): EventsAction
}
