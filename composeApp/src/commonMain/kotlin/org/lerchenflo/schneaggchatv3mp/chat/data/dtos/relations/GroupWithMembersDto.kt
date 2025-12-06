package org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupMemberDto

@Serializable
data class GroupWithMembersDto(
    @Embedded val group: GroupDto,
    @Relation(
        parentColumn = "id",
        entityColumn = "group_id"
    )
    val members: List<GroupMemberDto>
)