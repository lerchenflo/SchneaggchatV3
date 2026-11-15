package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * What the app shows for any user id: the name and the picture, from the users table when the
 * user is stored there (friends, requests, the own user), otherwise from the looked-up users.
 */
data class UserIdentity(
    val userId: String,
    val name: UserIdentityName,
    // Local picture file; empty when hidden, not loaded yet, or the account is gone
    val profilePictureUrl: String = "",
)

/** The name part of a [UserIdentity]. The UI turns [Unknown] and [Deleted] into text. */
sealed interface UserIdentityName {

    /** The own nickname for this user if set, else their name. */
    data class Known(val name: String) : UserIdentityName

    /** Not looked up yet (first appearance, or offline). Shown as "Unknown user". */
    data object Unknown : UserIdentityName

    /** The account no longer exists. Shown as "Deleted user". */
    data object Deleted : UserIdentityName
}

/** The shown text, with [unknown] and [deleted] as the localized placeholders. */
fun UserIdentityName.textOr(unknown: String, deleted: String): String = when (this) {
    is UserIdentityName.Known -> name
    UserIdentityName.Unknown -> unknown
    UserIdentityName.Deleted -> deleted
}
