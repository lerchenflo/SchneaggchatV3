package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_members",
    indices = [
        // A standalone groupId index would duplicate the unique index's leading column.
        Index(value = ["userId"]),
        Index(value = ["groupId", "userId"], unique = true),
    ]
)
data class GroupMemberEntity(

    @PrimaryKey(autoGenerate = true)
    val localPk: Long = 0L,

    val groupId: String,

    val userId: String,

    val joinDate: String,

    val admin: Boolean,

    val color: Int,

    val memberName: String


)