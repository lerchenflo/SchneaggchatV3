package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.UserResponse

@Serializable
data class UserSyncResponse(val updatedUsers: List<UserResponse>, val deletedUsers: List<String>)

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
