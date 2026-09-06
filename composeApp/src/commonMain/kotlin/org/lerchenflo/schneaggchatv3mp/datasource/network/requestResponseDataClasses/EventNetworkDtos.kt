package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipation
import org.lerchenflo.schneaggchatv3mp.events.domain.EventParticipationStatus
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.events.domain.GroupDeleteDelay
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

@Serializable
data class EventResponse(
    val id: String,
    val creatorId: String,
    val type: EventType,
    val title: String,
    val description: String,
    val groupId: String?,
    val location: LatLong?,
    val startDate: Long,
    val closeDate: Long?,
    val invitedUsers: List<String>,
    val participations: List<EventParticipation> = emptyList(), //default keeps decoding working against a server without the feature
    val visibility: EventVisibility,
    val maxUsers: Int? = null,
    val groupDeleteDelay: GroupDeleteDelay,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val updatedBy: String,
    val creatorName: String

)

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
    val groupResponse: NetworkUtils.GroupResponse, //Return the group belonging to the event
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
    val groupDeleteDelay: GroupDeleteDelay,
    val createGroup: Boolean = true,
)
