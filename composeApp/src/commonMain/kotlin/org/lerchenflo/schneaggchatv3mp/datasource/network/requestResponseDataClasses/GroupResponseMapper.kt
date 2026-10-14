package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

// ─── Group member action ─────────────────────────────────────────────────────

fun GroupMemberAction.toNetworkGroupMemberAction(): NetworkUtils.GroupMemberAction = when (this) {
    GroupMemberAction.ADD_USER -> NetworkUtils.GroupMemberAction.ADD_USER
    GroupMemberAction.REMOVE_USER -> NetworkUtils.GroupMemberAction.REMOVE_USER
    GroupMemberAction.MAKE_ADMIN -> NetworkUtils.GroupMemberAction.MAKE_ADMIN
    GroupMemberAction.REMOVE_ADMIN -> NetworkUtils.GroupMemberAction.REMOVE_ADMIN
}
