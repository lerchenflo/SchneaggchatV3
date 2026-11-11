package org.lerchenflo.schneaggchatv3mp.chat.domain

/** Friendship with someone the own user is not friends with (yet). */
enum class FriendRequestState {
    /** No open request either way: a request can be sent. */
    NONE,

    /** The own user sent a request that is still open. */
    SENT,

    /** The other user sent a request the own user can accept. */
    RECEIVED;

    companion object {
        /** Returns null for friends and blocked users, who can't be sent a request. */
        fun of(friendshipStatus: FriendshipStatus?, requesterId: String?, ownId: String): FriendRequestState? =
            when (friendshipStatus) {
                null, FriendshipStatus.DECLINED -> NONE
                FriendshipStatus.PENDING -> if (requesterId == ownId) SENT else RECEIVED
                FriendshipStatus.ACCEPTED, FriendshipStatus.BLOCKED -> null
            }

        /** Null [user]: not stored locally, so no friendship. */
        fun of(user: User?, ownId: String): FriendRequestState? =
            of(user?.friendshipStatus, user?.requesterId, ownId)
    }
}

/** Null for groups and for friends / blocked users. */
fun ChatListItem.friendRequestState(ownId: String): FriendRequestState? =
    if (isGroup) null else FriendRequestState.of(friendshipStatus, requesterId, ownId)
