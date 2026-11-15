package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity

import org.lerchenflo.schneaggchatv3mp.chat.domain.User
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentity
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityName

fun User.toUserIdentity(): UserIdentity = UserIdentity(
    userId = id,
    name = displayName.toIdentityName(),
    profilePictureUrl = profilePictureUrl,
)

fun LookedUpUserEntity.toUserIdentity(): UserIdentity = UserIdentity(
    userId = id,
    name = if (deleted) UserIdentityName.Deleted else name.toIdentityName(),
    profilePictureUrl = if (deleted) "" else profilePictureUrl,
)

private fun String.toIdentityName(): UserIdentityName =
    if (isBlank()) UserIdentityName.Unknown else UserIdentityName.Known(this)
