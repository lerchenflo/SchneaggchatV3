package org.lerchenflo.schneaggchatv3mp.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey()
    @ColumnInfo(name = "id")
    var id: Long = 0,

    @SerialName("lastchanged")
    @ColumnInfo(name = "changedate")
    var lastChanged: Long? = null,


    @SerialName("username")
    @ColumnInfo(name = "name")
    var name: String? = null,

    @SerialName("userdescription")
    @ColumnInfo(name = "description")
    var description: String? = null,

    @SerialName("userstatus")
    @ColumnInfo(name = "status")
    var status: String? = null,


    @ColumnInfo(name = "location_lat")
    var locationLat: Double? = null,

    @ColumnInfo(name = "location_long")
    var locationLong: Double? = null,

    @ColumnInfo(name = "location_date")
    var locationDate: Long? = null,

    @SerialName("locationshared")
    @ColumnInfo(name = "locationshared")
    var locationShared: Boolean? = null,

    @SerialName("wakeupenabled")
    @ColumnInfo(name = "wakeupenabled")
    var wakeupEnabled: Boolean? = null,

    @SerialName("profilepicture")
    @ColumnInfo(name = "profilepicture")
    var profilePicture: String? = null,

    @ColumnInfo(name = "last_online")
    var lastOnline: Long? = null,



    @SerialName("friendaccepted")
    @ColumnInfo(name = "accepted")
    var accepted: Boolean? = null,

    @SerialName("friendrequested")
    @ColumnInfo(name = "requested")
    var requested: Boolean? = null,

    @ColumnInfo(name = "notis_muted")
    var notisMuted: Boolean? = null,

    @Ignore
    var lastmessage: MessageWithReaders? = null
)