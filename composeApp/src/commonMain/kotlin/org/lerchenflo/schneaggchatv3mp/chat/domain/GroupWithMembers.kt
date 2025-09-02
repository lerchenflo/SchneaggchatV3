package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class GroupWithMembers(
    @Embedded val group: Group,
    @Relation(
        parentColumn = "id",
        entityColumn = "group_id"
    )
    val members: List<GroupMember>
)