package org.lerchenflo.schneaggchatv3mp.datasource.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
