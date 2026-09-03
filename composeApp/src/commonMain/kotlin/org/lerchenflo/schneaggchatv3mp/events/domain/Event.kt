package org.lerchenflo.schneaggchatv3mp.events.domain

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Serializable
data class Event(
    val id: String,
    val creatorId: String,

    val type: EventType,

    val title: String,
    val description: String,

    val groupId: String?, //Group connected to this event, null = event without a group chat
    val location: LatLong?, //Optional Location
    val startDate: Long,
    val closeDate: Long?,

    val invitedUsers: List<String>,

    val visibility: EventVisibility,

    val maxUsers: Int? = null, //Optional cap on how many people can join, null = unlimited

    val groupDeleteDelay: GroupDeleteDelay = GroupDeleteDelay.ONE_DAY,

    val createdAt: Long,
    val updatedAt: Long,
    val updatedBy: String = "",
    val creatorName: String

) {
    /*
    fun getDuration(): Long {
        return closeDate - startDate
    }

     */
}

/** Builds a blank [Event] with the app's default field values, ready to hand to the event edit popup. */
fun newEvent(creatorId: String, location: LatLong? = null): Event {
    val now = Clock.System.now().toEpochMilliseconds()
    val defaultStartDate = (Clock.System.now() + 1.days).toEpochMilliseconds()

    return Event(
        id = "",
        creatorId = creatorId,
        type = EventType.OTHER,
        title = "",
        description = "",
        groupId = null,
        location = location,
        startDate = defaultStartDate,
        closeDate = null,
        invitedUsers = emptyList(),
        visibility = EventVisibility.INVITED_FRIENDS_ONLY,
        groupDeleteDelay = GroupDeleteDelay.ONE_DAY,
        createdAt = now,
        updatedAt = now,
        creatorName = "",
    )
}
