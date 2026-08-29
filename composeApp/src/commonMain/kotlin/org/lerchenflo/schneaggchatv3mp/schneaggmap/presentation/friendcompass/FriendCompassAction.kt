package org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.friendcompass

sealed interface FriendCompassAction {
    data object OnBackClick : FriendCompassAction

    /** Selects this friend as the highlighted compass target. */
    data class OnFriendClick(val userId: String) : FriendCompassAction
}
