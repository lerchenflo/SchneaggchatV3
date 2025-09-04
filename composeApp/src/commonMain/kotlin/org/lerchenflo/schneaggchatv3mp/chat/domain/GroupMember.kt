package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "group_members")
data class GroupMember(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "entryid")
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val gid: Long,

    @SerialName("_id")
    @ColumnInfo(name = "user_id")
    val uid: Long,

    @SerialName("_color")
    @ColumnInfo(name = "color")
    val color: Int,

    @SerialName("_joindate")
    @ColumnInfo(name = "join_date")
    val joinDate: String,

    @SerialName("_isadmin")
    @ColumnInfo(name = "is_admin")
    val isAdmin: Boolean


)