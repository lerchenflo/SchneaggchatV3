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
    val closeDate: Long,

    val invitedUsers: List<String>,
    val acceptedUsers: List<String>,

    val public: Boolean

) {
    fun getDuration(): Long {
        return closeDate - startDate
    }
}
