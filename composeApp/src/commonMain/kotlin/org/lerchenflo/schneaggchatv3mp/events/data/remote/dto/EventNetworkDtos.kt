package org.lerchenflo.schneaggchatv3mp.events.data.remote.dto

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.EventResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupResponse
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.events.domain.GroupDeleteDelay
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

@Serializable
data class EventSyncResponse(
    val updatedEvents: List<EventResponse>,
    val deletedEvents: List<String>,
    val moreEntries: Boolean,
)

@Serializable
data class EventParticipationRequest(
    val eventId: String,
    val status: EventParticipationStatus,
)

@Serializable
data class EventJoinRequest(
    val eventId: String,
)

@Serializable
data class EventJoinResponse(
    val groupResponse: GroupResponse, //Return the group belonging to the event
    val event: EventResponse? = null, //null only against a server that predates it
)

@Serializable
data class EventRequest(
    val eventId: String?,
    val type: EventType,
    val title: String,
    val description: String,
    val location: LatLong?,
    val startDate: Long,
    val closeDate: Long?,
    val invitedUsers: List<String>,
    val visibility: EventVisibility,
    val maxUsers: Int? = null,
    /** Euro cents, 0 = free. Always sent; the server keeps the stored fee only when an older client leaves it out. */
    val entryFeeCents: Long,
    /** "Everyone can invite" for the event's group, written on every save; required by the server, ignored without a group. */
    val membersCanAdd: Boolean,
    val groupDeleteDelay: GroupDeleteDelay,
    val createGroup: Boolean = true,
    /** Lead times in minutes before the start; null keeps the stored ones, empty clears them. */
    val reminderMinutes: List<Int>? = null,
)
