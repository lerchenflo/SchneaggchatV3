package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName
import org.lerchenflo.schneaggchatv3mp.chat.domain.textOr
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.deleted_user
import schneaggchatv3mp.composeapp.generated.resources.unknown_user

// The only place that turns a resolved user name into shown text. A missing entry counts as not
// looked up yet ("Unknown user"), a deleted account shows as "Deleted user".

/** The shown name, inside composition. */
@Composable
fun UserIdentityName?.text(): String =
    (this ?: UserIdentityName.Unknown).textOr(
        unknown = stringResource(Res.string.unknown_user),
        deleted = stringResource(Res.string.deleted_user),
    )

/** The shown-name function inside composition, for mapping many names at once. */
@Composable
fun rememberUserIdentityNameText(): (UserIdentityName?) -> String {
    val unknown = stringResource(Res.string.unknown_user)
    val deleted = stringResource(Res.string.deleted_user)
    return remember(unknown, deleted) {
        { name -> (name ?: UserIdentityName.Unknown).textOr(unknown, deleted) }
    }
}

/**
 * The shown-name function outside composition (ViewModels, mappers, notifications). Reads the two
 * placeholder strings once, so a list can map many names with it.
 */
suspend fun userIdentityNameText(): (UserIdentityName?) -> String {
    val unknown = getString(Res.string.unknown_user)
    val deleted = getString(Res.string.deleted_user)
    return { name -> (name ?: UserIdentityName.Unknown).textOr(unknown, deleted) }
}
