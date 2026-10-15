package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSyncResponse(val updatedUsers: List<UserResponse>, val deletedUsers: List<String>)

@Serializable
enum class NetworkFriendshipStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    BLOCKED
}

@Serializable
sealed interface UserResponse {

    //Common data which every response contains
    val id: String
    val username: String
    val updatedAt: Long

    val profilePicUpdatedAt: Long



    //Response for a user (Not yourself and not your friend)
    @Serializable
    @SerialName("simple")
    data class SimpleUserResponse(
        override val id: String,
        override val username: String,
        override val updatedAt: Long,
        override val profilePicUpdatedAt: Long,

        //Custom to simpleuserresponse:
        val friendShipStatus: NetworkFriendshipStatus?,
        val requesterId: String,

        ) : UserResponse

    //Response for a friend (He accepted your request)
    @Serializable
    @SerialName("friend")
    data class FriendUserResponse(
        override val id: String,
        override val username: String,

        override val updatedAt: Long,
        override val profilePicUpdatedAt: Long,


        val requesterId: String?, //Who requested the friendship

        //Custom to friend response:
        val birthDate: String,
        val userDescription: String,
        val userStatus: String,

        //Optional phone number, shared with accepted friends
        val phoneNumber: String? = null,

        val nickName: String?,

        //Do i share the location with this friend
        val shareLocation: Boolean = false,
        //Do i share speed + heading (live driving telemetry) with this friend
        val shareSpeedHeading: Boolean = false,
        //Whether i share my snail trail (full 24h history) with this friend
        val shareSnailTrail: Boolean = false,

        //Do i allow this friend to wake me (alarm ringtone on my devices)
        val allowWake: Boolean = false,

        //Epoch millis this friend was last seen online, null if unknown/never
        val lastSeen: Long? = null,

    ) : UserResponse

    //Response for yourself (You request your own data)
    @Serializable
    @SerialName("self")
    data class SelfUserResponse(
        override val id: String,
        override val username: String,

        override val updatedAt: Long,
        override val profilePicUpdatedAt: Long,


        //Custom to friend response
        val birthDate: String,
        val userDescription: String,
        val userStatus: String,

        //Optional phone number, only visible to yourself and accepted friends
        val phoneNumber: String? = null,

        //Custom to own user response:
        val email: String,
        val emailVerifiedAt: Long?,
        val createdAt: Long,

        //Do i have location sharing enabled
        val locationShared: Boolean = false,

        //Master switch: do i allow anyone to wake me at all
        val allowWakeGlobal: Boolean = false,

        //Per-user app settings (theme, language, pinned chats, ...), synced across devices
        val settings: PersonalUserSettings = PersonalUserSettings(),

    ) : UserResponse
}

@Serializable
data class NewFriendsUserResponse(
    val id: String,
    val username: String,
    val commonFriendCount: Int,
)

@Serializable
data class UserRequest(
    val userId: String,
    val newDescription: String?,
    val newStatus: String?,
    val newEmail: String?,
    val newBirthDate: String?,
    val newNickName: String?,
    val newPhoneNumber: String? = null,
)

// Location data itself is now pushed/pulled over the WebSocket (see SocketConnectionMessage:
// LocationUpdate / FriendLocationChange / FriendLocationsSnapshot), not via HTTP.
@Serializable
data class LocationShareRequest(
    val friendId: String,
    // Full desired per-friend map-sharing state - send all of these every time.
    val share: Boolean,
    val shareSpeedHeading: Boolean = false,
    val shareSnailTrail: Boolean = false,
)

@Serializable
data class WakePermissionRequest(
    val friendId: String,
    val allowWake: Boolean,
)

@Serializable
data class WakeGlobalRequest(
    val enabled: Boolean,
)

/**
 * Mirrors the server's WakeOutcome - why a wake did or did not reach anybody.
 * Waking a non-friend is not represented here: the server rejects it with 403.
 */
@Serializable
enum class NetworkWakeOutcome {
    WOKEN,
    NOT_ALLOWED,
    NO_DEVICES,
}

@Serializable
data class WakeRequest(
    val targetId: String,
    val isGroup: Boolean,
    val reason: String,
)

@Serializable
data class WakeResponse(
    val outcome: NetworkWakeOutcome,
    val userCount: Int = 0,
    val deviceCount: Int = 0,
    val skippedCount: Int = 0,
)
