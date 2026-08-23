package org.lerchenflo.schneaggchatv3mp.events.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.lerchenflo.schneaggchatv3mp.events.domain.EventType
import org.lerchenflo.schneaggchatv3mp.events.domain.EventVisibility
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong

@Entity(tableName = "events")
data class EventDto(
    @PrimaryKey val id: String,
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
    val createdAt: Long,
    val updatedAt: Long,
    val updatedBy: String,
    val creatorName: String

)
