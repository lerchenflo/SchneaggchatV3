package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "groups")
data class Group(

    @PrimaryKey(autoGenerate = false)
    @SerialName("Id")
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @SerialName("groupname")
    @ColumnInfo(name = "name")
    val name: String,

    @SerialName("profilepicture")
    @ColumnInfo(name = "profile_picture")
    var profilePicture: String,

    @SerialName("groupdescription")
    @ColumnInfo(name = "description")
    val description: String,

    @SerialName("createdate")
    @ColumnInfo(name = "create_date")
    val createDate: String?,

    @SerialName("lastchanged")
    @ColumnInfo(name = "changedate")
    val changedate: String?,


    @ColumnInfo(name = "notis_muted")
    val muted: Boolean = false
)