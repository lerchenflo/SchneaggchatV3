package org.lerchenflo.schneaggchatv3mp.events.domain

import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

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

    val public: Boolean,

    val createdAt: Long,
    val updatedAt: Long,
    val updatedBy: String = "",
    val updatedByName: String = "",
) {
    /*
    fun getDuration(): Long {
        return closeDate - startDate
    }

     */
}
