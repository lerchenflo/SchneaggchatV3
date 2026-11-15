package org.lerchenflo.schneaggchatv3mp.chat.presentation.unknownuser

import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendRequestState

data class UnknownUserState(
    /** Resolved through the one name path; "Unknown user" until it is known. */
    val name: UserIdentityName = UserIdentityName.Unknown,
    /** Stored picture file (users table or looked-up users); the popup shows a placeholder when empty. */
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
