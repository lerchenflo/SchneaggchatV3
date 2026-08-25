package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
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
    val groupId: String,
    val location: LatLong?,
    val startDate: Long,
    val closeDate: Long?,
    val invitedUsers: List<String>,
    val visibility: EventVisibility,
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
data class EventJoinRequest(
    val eventId: String,
)

@Serializable
data class EventJoinResponse(
    val groupResponse: NetworkUtils.GroupResponse, //Return the group belonging to the event

)

@Serializable
data class EventRequest(
    val eventId: String?,
    val type: EventType,
    val title: String,
    val description: String,
    val groupId: String,
    val location: LatLong?,
    val startDate: Long,
    val closeDate: Long?,
    val invitedUsers: List<String>,
    val visibility: EventVisibility,
    val groupDeleteDelay: GroupDeleteDelay,
)
