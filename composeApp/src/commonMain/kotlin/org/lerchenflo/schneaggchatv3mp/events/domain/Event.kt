package org.lerchenflo.schneaggchatv3mp.events.domain

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

@Serializable
data class Event(
    val id: String,
    val creatorId: String,

    val type: EventType,

    val title: String,
    val description: String,

    val groupId: String, //Group connected to this event
    val location: LatLong?, //Optional Location
    val startDate: Long,
    val closeDate: Long?,

    val invitedUsers: List<String>,

    val visibility: EventVisibility,

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
