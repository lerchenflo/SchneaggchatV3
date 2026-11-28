package org.lerchenflo.schneaggchatv3mp.chat.presentation.unknownuser

import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendRequestState
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName

data class UnknownUserState(
    /** Resolved through the one name path; "Unknown user" until it is known. */
    val name: UserIdentityName = UserIdentityName.Unknown,
    /** Stored picture file (users table or looked-up users); the popup shows a placeholder when empty. */
    val profilePictureFilePath: String = "",
    /** Only known when opened from the user search. */
    val commonFriendCount: Int? = null,
    /** Null while the user is being loaded, and for a friend; the popup shows no request button then. */
    val requestState: FriendRequestState? = null,
    /** A friend gets Message, Call and Info instead of the request button. */
    val isFriend: Boolean = false,
    /** Only a friend shares it; the Call button shows only with a number. */
    val phoneNumber: String? = null,
)

sealed interface UnknownUserAction {
    /** Sends a request, or accepts the one the other user sent - the same call. */
    data object OnSendRequestClick : UnknownUserAction
    /** Friend only: opens the chat with them. */
    data object OnMessageClick : UnknownUserAction
    /** Friend with a shared phone number only: opens the phone dialer. */
    data object OnCallClick : UnknownUserAction
    /** Friend only: opens their chat details. */
    data object OnInfoClick : UnknownUserAction
    data object OnDismissClick : UnknownUserAction
}
