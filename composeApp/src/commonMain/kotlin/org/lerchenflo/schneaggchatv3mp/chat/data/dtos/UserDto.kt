@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
@Entity(tableName = "users")
data class UserDto(
    @PrimaryKey(autoGenerate = false)
    var id: String = "",

    var changedate: Long, //lastchanged

    var name: String = "",

    var description: String? = null,

    var status: String? = null,

    var locationLat: Double? = null,

    var locationLong: Double? = null,

    var locationDate: Long? = null,


    // friend stuff
    var frienshipStatus: NetworkUtils.FriendshipStatus?, //Current status of the friendship
    val requesterId: String?, //Who requested the friendship

    var locationShared: Boolean = false,

    var wakeupEnabled: Boolean = false,


    //TODO implement later
    var lastOnline: Long? = null,

    var notisMuted: Boolean = false,

    var birthDate: String? = null,

    var email: String? = null,

    var createdAt: Long?,


)