package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * A user the own user can send a friend request to, as returned by
 * [org.lerchenflo.schneaggchatv3mp.chat.domain.UserRepository.getAvailableUsers].
 * Domain copy of the network DTO `NetworkUtils.NewFriendsUserResponse`, field for field.
 */
data class AvailableUser(
    val id: String,
    val username: String,
    val commonFriendCount: Int,
)
