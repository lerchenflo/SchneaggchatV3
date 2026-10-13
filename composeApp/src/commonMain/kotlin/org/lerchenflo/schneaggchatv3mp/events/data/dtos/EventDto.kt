package org.lerchenflo.schneaggchatv3mp.events.data.dtos

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventParticipation
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventType
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventVisibility
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.GroupDeleteDelay
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.LatLong

@Entity(
    tableName = "events",
    indices = [Index(value = ["groupId"])]
)
data class EventDto(
    @PrimaryKey val id: String,
    val creatorId: String,
    val type: EventType,
    val title: String,
    val description: String,
    val groupId: String?,
    val location: LatLong?,
    val startDate: Long,
    val closeDate: Long?,
    val invitedUsers: List<String>,
    val participations: List<EventParticipation>,
    val visibility: EventVisibility,
    val maxUsers: Int? = null,
    val groupDeleteDelay: GroupDeleteDelay,
    val createdAt: Long,
    val updatedAt: Long,
    val updatedBy: String,
    val creatorName: String

)
