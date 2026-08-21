package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
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
    val public: Boolean,
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
    val public: Boolean,
)
