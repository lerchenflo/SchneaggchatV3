package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.GroupEntity
import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.GroupMemberEntity

@Serializable
data class GroupWithMembers(
    @Embedded val group: GroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val members: List<GroupMemberEntity>
)