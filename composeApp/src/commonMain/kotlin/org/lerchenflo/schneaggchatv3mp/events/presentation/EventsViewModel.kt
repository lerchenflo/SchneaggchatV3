@file:OptIn(ExperimentalCoroutinesApi::class)

package org.lerchenflo.schneaggchatv3mp.events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.data.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.datasource.AppRepository
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.events.data.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.newEvent
import org.lerchenflo.schneaggchatv3mp.utilities.PictureManager
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.millisToLocalDate
import org.lerchenflo.schneaggchatv3mp.utilities.monthDayKey
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.event_and_group_created
import schneaggchatv3mp.composeapp.generated.resources.event_created
import schneaggchatv3mp.composeapp.generated.resources.event_delete_failed
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class EventsViewModel(
    private val navigator: Navigator,
    private val eventRepository: EventRepository,
    private val appRepository: AppRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val preferenceManager: Preferencemanager,
    private val pictureManager: PictureManager,
    private val applicationScope: ApplicationScope,

    private val initialEntryId: String? = null,
    private val initialEntry: Event? = null
) : ViewModel() {

    // Own user isn't in friendsById (that map only keeps ACCEPTED friendships), and driven off
    // authState (not a one-shot read) so it picks up the id once autologin finishes.
    private val ownUserFlow = SessionCache.authState.flatMapLatest { auth ->
        (auth as? SessionCache.AuthState.LoggedIn)
            ?.let { appRepository.getUserByIdFlow(it.userId) }
            ?: flowOf(null)
    }

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

        // Events arrive sorted by startDate ASC; groupBy/append below keep that encounter order,
        // so each day's list stays chronological.
        val dayGroups = groupEventsByStartDate(events).map { (date, dayEvents) ->
            EventDayGroup(
                dayId = "day_$date",
                dateMillis = dayEvents.first().startDate,
                events = dayEvents
            )
        }

        currentState.copy(
            events = events,
            eventDayGroups = dayGroups,
            eventsByDate = groupEventsBySpannedDates(events),
            friendsById = friendsMap,
            groups = groups,
            mapStyleUrl = mapStyleUrl
        )
    }.combine(ownUserFlow) { currentState, ownUser ->
        currentState.copy(
            birthdaysByMonthDay = buildBirthdaysByMonthDay(currentState.friendsById.values, ownUser)
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
                        createGroup = action.createGroup,
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
                        SnackbarManager.showMessage(
                            getString(
                                if (action.createGroup) Res.string.event_and_group_created
                                else Res.string.event_created
                            )
                        )
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
                            applicationScope.launch { //Launch in app scope to run while navigating away
                                appRepository.dataSync("Started after joining event to get messages")
                            }
                            _state.update { it.copy(selectedEvent = null) }
                            navigator.navigate(Route.Chat(chatId = groupId, isGroup = true))
                        }
                    } finally {
                        _state.update { it.copy(isJoiningEvent = false) }
                    }
                }
            }

            is EventsAction.OnOpenGroupChat -> {
                viewModelScope.launch {
                    _state.update { it.copy(selectedEvent = null) }
                    navigator.navigate(Route.Chat(chatId = action.groupId, isGroup = true))
                }
            }

            is EventsAction.OnDeleteEvent -> {
                viewModelScope.launch {
                    val success = appRepository.detachEvent(
                        eventId = action.event.id,
                        groupId = action.event.groupId,
                        deleteGroup = action.deleteGroup,
                        deleteEvent = action.deleteEvent,
                    )
                    if (!success) {
                        SnackbarManager.showMessage(getString(Res.string.event_delete_failed))
                    }
                    _state.update { it.copy(selectedEvent = null) }
                }
            }

            is EventsAction.OnViewModeChange -> {
                _state.update { it.copy(viewMode = action.mode) }
            }

            is EventsAction.OnCalendarNavigate -> {
                _state.update { current ->
                    val step = if (action.forward) 1 else -1
                    val newAnchor = when (current.viewMode) {
                        EventsViewMode.WEEK -> current.calendarAnchorDate.plus(DatePeriod(days = 7 * step))
                        EventsViewMode.MONTH -> current.calendarAnchorDate.plus(DatePeriod(months = step))
                        EventsViewMode.LIST -> current.calendarAnchorDate
                    }
                    current.copy(calendarAnchorDate = newAnchor)
                }
            }

            EventsAction.OnCalendarJumpToToday -> {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                _state.update { it.copy(calendarAnchorDate = today) }
            }

            is EventsAction.OnCalendarDayClick -> {
                _state.update { it.copy(selectedCalendarDay = action.date) }
            }

            EventsAction.OnCalendarDayDetailDismiss -> {
                _state.update { it.copy(selectedCalendarDay = null) }
            }

            is EventsAction.OnBirthdayClick -> {
                if (action.userId == SessionCache.requireLoggedIn()?.userId) return
                _state.update { it.copy(selectedCalendarDay = null) }
                viewModelScope.launch {
                    navigator.navigate(Route.Chat(chatId = action.userId, isGroup = false))
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

/**
 * Buckets [events] by their start day only (device time zone), keyed by [LocalDate]. Feeds the
 * List view's day groups. Assumes [events] already arrive sorted by startDate ascending, same as
 * the upstream flow - groupBy keeps that encounter order, so each day's list stays chronological.
 */
private fun groupEventsByStartDate(events: List<Event>): Map<LocalDate, List<Event>> {
    return events.groupBy { millisToLocalDate(it.startDate) }
}

/** Multi-day events past this many days are treated as start-day-only - guards against bad data. */
private const val MAX_EVENT_SPAN_DAYS = 365

/**
 * Buckets [events] across every day they span, from startDate to closeDate inclusive (device time
 * zone). Feeds the week/month calendar views. A null or before-start closeDate falls back to the
 * start day only; a span longer than [MAX_EVENT_SPAN_DAYS] is capped there too, so one bad
 * closeDate can't blow up the map. Assumes [events] already arrive sorted by startDate ascending,
 * so within a day, events spilling in from earlier days sort before events starting that day.
 */
private fun groupEventsBySpannedDates(events: List<Event>): Map<LocalDate, List<Event>> {
    val result = LinkedHashMap<LocalDate, MutableList<Event>>()
    for (event in events) {
        val startDay = millisToLocalDate(event.startDate)
        val endDay = event.closeDate
            ?.let { millisToLocalDate(it) }
            ?.takeIf { it >= startDay }
            ?.coerceAtMost(startDay.plus(DatePeriod(days = MAX_EVENT_SPAN_DAYS)))
            ?: startDay

        var day = startDay
        while (day <= endDay) {
            result.getOrPut(day) { mutableListOf() }.add(event)
            day = day.plus(DatePeriod(days = 1))
        }
    }
    return result
}

/** Builds the calendar's month/day -> birthdays lookup from [friends] plus the optional [ownUser]. */
private fun buildBirthdaysByMonthDay(friends: Collection<User>, ownUser: User?): Map<Int, List<CalendarBirthday>> {
    val users = buildList {
        ownUser?.let { add(it to true) }
        friends.forEach { add(it to false) }
    }
    return users
        .mapNotNull { (user, isOwn) -> user.toCalendarBirthday(isOwn) }
        .groupBy { it.monthDayKey }
}

private fun User.toCalendarBirthday(isOwn: Boolean): CalendarBirthday? {
    val parsed = birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    return CalendarBirthday(
        userId = id,
        displayName = displayName,
        profilePictureUrl = profilePictureUrl.ifBlank { null },
        monthDayKey = parsed.monthDayKey(),
        birthYear = parsed.year,
        isOwn = isOwn,
    )
}
