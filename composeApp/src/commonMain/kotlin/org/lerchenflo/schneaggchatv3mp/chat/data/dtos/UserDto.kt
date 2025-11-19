@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
@Entity(tableName = "users")
data class UserDto(
    @PrimaryKey(autoGenerate = false)
    var id: String = "",

    var changedate: Long, //lastchanged

    var name: String = "",

    var description: String = "",

    var status: String = "",

    var locationLat: Double? = null,

    var locationLong: Double? = null,

    var locationDate: Long? = null,

    var locationShared: Boolean = false,

    var wakeupEnabled: Boolean = false,

    var profilePicture: String = "",

    var lastOnline: Long? = null,

    var accepted: Boolean = false,

    var requested: Boolean = false,

    var notisMuted: Boolean = false,

    var birthDate: String = "",

    )