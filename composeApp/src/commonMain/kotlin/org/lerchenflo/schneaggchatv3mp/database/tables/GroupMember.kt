package org.lerchenflo.schneaggchatv3mp.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "group_members")
data class GroupMember(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "entryid")
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val gid: Long,

    @ColumnInfo(name = "user_id")
    val uid: String,

    @ColumnInfo(name = "color")
    val color: Int,

    @ColumnInfo(name = "join_date")
    val joinDate: String,

    @ColumnInfo(name = "is_admin")
    val isAdmin: Boolean


)
