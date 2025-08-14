package org.lerchenflo.schneaggchatv3mp.database

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import androidx.room.Entity

@Entity(tableName = "users")
data class User(
    @PrimaryKey()
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @ColumnInfo(name = "last_change")
    var lastChange: Long? = null,

    @ColumnInfo(name = "name")
    var name: String? = null,

    @ColumnInfo(name = "description")
    var description: String? = null,

    @ColumnInfo(name = "status")
    var status: String? = null,

    @ColumnInfo(name = "location_lat")
    var locationLat: Double? = null,

    @ColumnInfo(name = "location_long")
    var locationLong: Double? = null,

    @ColumnInfo(name = "location_date")
    var locationDate: Long? = null,

    @ColumnInfo(name = "locationshared")
    var locationShared: Boolean? = null,

    @ColumnInfo(name = "wakeupenabled")
    var wakeupEnabled: Boolean? = null,

    @ColumnInfo(name = "profile_picture")
    var profilePicture: String? = null,

    @ColumnInfo(name = "last_online")
    var lastOnline: Long? = null,

    @ColumnInfo(name = "read_time")
    var readTime: Long? = null,

    @ColumnInfo(name = "accepted")
    var accepted: Boolean? = null,

    @ColumnInfo(name = "requested")
    var requested: Boolean? = null,

    @ColumnInfo(name = "notis_muted")
    var notisMuted: Boolean? = null
)
