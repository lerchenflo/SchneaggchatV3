package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: Long = 0L,

    @SerialName("lastchanged")
    @ColumnInfo(name = "changedate")
    var lastChanged: Long = 0L,

    @SerialName("username")
    @ColumnInfo(name = "name")
    var name: String = "",

    @SerialName("userdescription")
    @ColumnInfo(name = "description")
    var description: String = "",

    @SerialName("userstatus")
    @ColumnInfo(name = "status")
    var status: String = "",

    @ColumnInfo(name = "location_lat")
    var locationLat: Double? = null,

    @ColumnInfo(name = "location_long")
    var locationLong: Double? = null,

    @ColumnInfo(name = "location_date")
    var locationDate: Long? = null,

    @SerialName("locationshared")
    @ColumnInfo(name = "locationshared")
    var locationShared: Boolean = false,

    @SerialName("wakeupenabled")
    @ColumnInfo(name = "wakeupenabled")
    var wakeupEnabled: Boolean = false,

    @SerialName("profilepicture")
    @ColumnInfo(name = "profilepicture")
    var profilePicture: String = "",

    @ColumnInfo(name = "last_online")
    var lastOnline: Long? = null,

    @SerialName("friendaccepted")
    @ColumnInfo(name = "accepted")
    var accepted: Boolean = false,

    @SerialName("friendrequested")
    @ColumnInfo(name = "requested")
    var requested: Boolean = false,

    @ColumnInfo(name = "notis_muted")
    var notisMuted: Boolean = false,

    @SerialName("birthdate")
    @ColumnInfo(name = "birthdate")
    var birthDate: String = "",

    @SerialName("gender")
    @ColumnInfo(name = "gender")
    var gender: String = "",

    @SerialName("settings")
    @ColumnInfo(name = "settings")
    var settings: String? = ""
)