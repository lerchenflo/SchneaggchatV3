package org.lerchenflo.schneaggchatv3mp.events.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.DataSyncCoordinator
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.onSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.reportErrorTo
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.EventRepository
import org.lerchenflo.schneaggchatv3mp.events.domain.isUnseenBy
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils
import kotlin.time.Clock

/** One event, for guests and its creator. Everything comes from the local store and follows it live. */
class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository,
    userRepository: UserRepository,
    groupRepository: GroupRepository,
    settingsRepository: SettingsRepository,
    private val dataSyncCoordinator: DataSyncCoordinator,
    private val shareUtils: ShareUtils,
    private val navigator: Navigator,
    private val errorChannel: ErrorChannel,
) : ViewModel() {

    private val _state = MutableStateFlow(EventDetailState())

    private val ownUserIdFlow = SessionCache.authState.map {
        (it as? SessionCache.AuthState.LoggedIn)?.userId
    }

    private val eventFlow = eventRepository.getAllEventsFlow().map { events -> events.firstOrNull { it.id == eventId } }

    val state = combine(
        _state,
        eventFlow,
        userRepository.getAllUsersFlow(),
        groupRepository.getAllGroupsFlow(),
        settingsRepository.getMapStyleUrlFlow(),
    ) { current, event, users, groups, mapStyleUrl ->
        current.copy(
            event = event,
            isLoading = false,
            mapStyleUrl = mapStyleUrl,
            usersById = users
                .filter { it.friendshipStatus == FriendshipStatus.ACCEPTED || it.id == current.ownUserId }
                .associateBy { it.id },
            // Group membership - not the participation status - decides Open chat: a user can
            // accept a group event without joining its chat, and a legacy member has no entry.
            isJoined = event?.groupId != null && groups.firstOrNull { it.id == event.groupId }
                ?.members?.any { it.userId == current.ownUserId } == true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventDetailState(),
    )

    init {
        viewModelScope.launch {
            ownUserIdFlow.collect { ownId -> _state.update { it.copy(ownUserId = ownId) } }
        }
        // Opening the event is what marks it seen, once. A failed call is silent: the server
        // ignores a repeated SEEN, so the next opening just tries again.
        viewModelScope.launch {
            val (event, ownId) = combine(eventFlow, ownUserIdFlow) { event, ownId -> event to ownId }
                .filter { (event, ownId) -> event != null && ownId != null }
                .first()
            if (event != null && ownId != null && event.isUnseenBy(ownId)) {
                eventRepository.setEventParticipation(event.id, EventParticipationStatus.SEEN)
            }
        }
    }

    fun onAction(action: EventDetailAction) {
        val event = state.value.event
        when (action) {
            EventDetailAction.OnBackClick -> viewModelScope.launch { navigator.navigateBack() }

            EventDetailAction.OnEditClick -> {
                if (event == null) return
                viewModelScope.launch {
                    navigator.navigate(
                        Route.Events(selectedEvent = event),
                        navigationOptions = Navigator.NavigationOptions(exitPreviousScreen = true)
                    )
                }
            }

            EventDetailAction.OnJoinClick -> {
                if (event == null) return
                viewModelScope.launch {
                    // Guard set before the first suspend call, so a second fast tap can't join twice
                    if (_state.value.isJoining) return@launch
                    _state.update { it.copy(isJoining = true) }
                    try {
                        eventRepository.joinEvent(event.id)
                            .reportErrorTo(errorChannel)
                            .onSuccess { groupId ->
                                // UNDISPATCHED: started before the navigation can clear this ViewModel
                                viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    dataSyncCoordinator.dataSync("Started after joining event to get messages")
                                }
                                navigator.navigate(Route.Chat(chatId = groupId, isGroup = true))
                            }
                    } finally {
                        _state.update { it.copy(isJoining = false) }
                    }
                }
            }

            EventDetailAction.OnOpenChatClick -> {
                val groupId = event?.groupId ?: return
                viewModelScope.launch { navigator.navigate(Route.Chat(chatId = groupId, isGroup = true)) }
            }

            // The screen stays open, so the user sees themselves appear in the going list
            EventDetailAction.OnAcceptClick -> {
                if (event == null) return
                viewModelScope.launch {
                    eventRepository.setEventParticipation(event.id, EventParticipationStatus.ACCEPTED)
                        .reportErrorTo(errorChannel)
                }
            }

            EventDetailAction.OnNotInterestedClick -> {
                if (event == null) return
                viewModelScope.launch {
                    eventRepository.setEventParticipation(event.id, EventParticipationStatus.DISMISSED)
                        .reportErrorTo(errorChannel)
                        .onSuccess { navigator.navigateBack() }
                }
            }

            EventDetailAction.OnShowOnMapClick -> {
                val location = event?.location ?: return
                viewModelScope.launch {
                    navigator.navigate(
                        Route.Schneaggmap(
                            focusLocation = location,
                            openRequestId = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
            }

            EventDetailAction.OnAddToCalendarClick -> {
                if (event == null) return
                shareUtils.addEventToCalendar(
                    title = event.title,
                    description = event.description,
                    location = event.location?.let { "${it.lat},${it.long}" } ?: "",
                    startDateMillis = event.startDate,
                    endDateMillis = event.closeDate
                )
            }
        }
    }
}
