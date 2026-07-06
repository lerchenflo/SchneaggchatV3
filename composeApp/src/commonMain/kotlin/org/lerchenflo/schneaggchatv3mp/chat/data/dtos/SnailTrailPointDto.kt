package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** One sampled point of a friend's snail trail (see `FriendLocationSnapshotEntry.snailTrail` and
 * `SocketConnectionMessage.SnailTrailPointAdded`). */
@Serializable
@Entity(
    tableName = "snail_trail_points",
    indices = [Index(value = ["userId"])]
)
data class SnailTrailPointDto(
    @PrimaryKey(autoGenerate = true)
    val localPk: Long = 0L,

    val userId: String,

    val lat: Double,
    val long: Double,
    val locationTime: Long,

    val speed: Double? = null,
    val heading: Double? = null,
)
