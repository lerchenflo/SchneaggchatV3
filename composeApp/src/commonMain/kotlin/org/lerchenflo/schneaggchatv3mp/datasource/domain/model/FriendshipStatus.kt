package org.lerchenflo.schneaggchatv3mp.datasource.domain.model

import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import org.lerchenflo.schneaggchatv3mp.utilities.UiText.StringResourceText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.accepted
import schneaggchatv3mp.composeapp.generated.resources.blocked
import schneaggchatv3mp.composeapp.generated.resources.declined
import schneaggchatv3mp.composeapp.generated.resources.pending

/**
 * State of the friendship between the own user and another user ([User.friendshipStatus]).
 * Domain copy of the network enum `NetworkUtils.FriendshipStatus`, entry for entry.
 */
enum class FriendshipStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    BLOCKED;

    fun toUiText(): UiText = when (this) {
        PENDING -> StringResourceText(Res.string.pending)
        ACCEPTED -> StringResourceText(Res.string.accepted)
        DECLINED -> StringResourceText(Res.string.declined)
        BLOCKED -> StringResourceText(Res.string.blocked)
    }
}
