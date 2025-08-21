package org.lerchenflo.schneaggchatv3mp.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "groups")
data class Group(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "groupid")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "profile_picture")
    val profilePicture: String?,   // nullable in case no picture is set

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "create_date")
    val createDate: String?,

    @ColumnInfo(name = "change_date")
    val changeDate: String?,

    @ColumnInfo(name = "creator_id")
    val creatorid: String,

    @ColumnInfo(name = "notis_muted")
    val muted: Boolean
)
