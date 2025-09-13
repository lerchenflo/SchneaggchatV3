package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class GroupWithMembersDto(
    @Embedded val group: GroupDto,
    @Relation(
        parentColumn = "id",
        entityColumn = "group_id"
    )
    val members: List<GroupMemberDto>
)