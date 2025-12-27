package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "group_members")
data class GroupMemberDto(

    @PrimaryKey(autoGenerate = true)
    val localPk: Long = 0L,

    val groupId: String,

    val userId: String,

    val joinDate: String,

    val admin: Boolean,

    val color: Int


)