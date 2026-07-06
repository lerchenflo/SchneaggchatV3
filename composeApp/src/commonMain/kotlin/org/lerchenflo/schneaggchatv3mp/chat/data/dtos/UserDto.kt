@file:OptIn(ExperimentalTime::class)

package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import kotlin.time.ExperimentalTime

@Serializable
@Entity(tableName = "users")
data class UserDto(
    @PrimaryKey(autoGenerate = false)
    var id: String = "",

    var updatedAt: Long,
    val profilePicUpdatedAt: Long,

    var name: String = "",
    var nickName: String? = null,

    var description: String? = null,

    var status: String? = null,

    val profilePictureUrl: String,

    var locationLat: Double? = null,

    var locationLong: Double? = null,

    var locationDate: Long? = null,

    // Optional telemetry, all shown when available. Speed/heading are only ever populated when
    // the location-owner has "Advanced location sharing" enabled; altitude/battery/distance are
    // sent whenever location sharing is on at all.
    var locationSpeed: Double? = null, // meters/second
    var locationHeading: Double? = null, // degrees, 0-360
    var locationAltitude: Double? = null, // meters above sea level
    var locationBattery: Int? = null, // percent, 0-100
    var locationDistance24h: Double? = null, // meters traveled in the last 24h


    // friend stuff
    var frienshipStatus: NetworkUtils.FriendshipStatus?, //Current status of the friendship
    val requesterId: String?, //Who requested the friendship

    var locationShared: Boolean = false,

    // Per-friend advanced-location settings (what we share TOWARDS this friend)
    var shareSpeedHeading: Boolean = false,
    var snailTrail: Boolean = false,

    var wakeupEnabled: Boolean = false,

    var notisMuted: Boolean = false,

    // Epoch millis this friend was last seen online, null if unknown/never. Synced like any
    // other user field (via UserChange) - unrelated to live "online right now" presence, which
    // is never persisted (see UserRepository.onlineFriendIdsFlow).
    var lastSeen: Long? = null,

    var birthDate: String? = null,

    var email: String? = null,
    var emailVerifiedAt: Long?,


    var createdAt: Long?,


    )