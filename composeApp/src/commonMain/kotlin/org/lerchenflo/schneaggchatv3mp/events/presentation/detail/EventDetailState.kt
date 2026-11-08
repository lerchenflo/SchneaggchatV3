package org.lerchenflo.schneaggchatv3mp.events.presentation.detail

import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.events.domain.Event
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.statusOf

data class EventDetailState(
    /** Null while loading, or when the event is not (or no longer) stored. */
    val event: Event? = null,
    /** True until the local store answered once. */
    val isLoading: Boolean = true,
    val ownUserId: String? = null,
    /** Friends and the own user, for names and pictures. */
    val usersById: Map<String, User> = emptyMap(),
    /** Member of the event's group chat. */
    val isJoined: Boolean = false,
    val mapStyleUrl: String? = null,
    val isJoining: Boolean = false,
) {
    val isCreator: Boolean get() = event != null && event.creatorId == ownUserId
    val ownStatus: EventParticipationStatus? get() = ownUserId?.let { event?.statusOf(it) }
}

sealed interface EventDetailAction {
    data object OnBackClick : EventDetailAction
    /** Creator only: opens the edit popup on the events screen. */
    data object OnEditClick : EventDetailAction
    data object OnJoinClick : EventDetailAction
    data object OnOpenChatClick : EventDetailAction
    data object OnAcceptClick : EventDetailAction
    data object OnNotInterestedClick : EventDetailAction
    data object OnShowOnMapClick : EventDetailAction
    data object OnAddToCalendarClick : EventDetailAction
}
