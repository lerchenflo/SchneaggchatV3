package org.lerchenflo.schneaggchatv3mp.chat.presentation.unknownuser

import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendRequestState

data class UnknownUserState(
    /** Null when nobody knows the name yet: the popup shows "Unknown user". */
    val name: String? = null,
    /** Locally stored picture; the popup shows a placeholder when there is none. */
    val profilePictureFilePath: String = "",
    /** Only known when opened from the user search. */
    val commonFriendCount: Int? = null,
    /** Null while the user is being loaded; the popup shows nothing until then. */
    val requestState: FriendRequestState? = null,
)

sealed interface UnknownUserAction {
    /** Sends a request, or accepts the one the other user sent - the same call. */
    data object OnSendRequestClick : UnknownUserAction
    data object OnDismissClick : UnknownUserAction
}
