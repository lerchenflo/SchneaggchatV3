package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction

// ─── Group member action ─────────────────────────────────────────────────────

fun GroupMemberAction.toNetworkGroupMemberAction(): NetworkGroupMemberAction = when (this) {
    GroupMemberAction.ADD_USER -> NetworkGroupMemberAction.ADD_USER
    GroupMemberAction.REMOVE_USER -> NetworkGroupMemberAction.REMOVE_USER
    GroupMemberAction.MAKE_ADMIN -> NetworkGroupMemberAction.MAKE_ADMIN
    GroupMemberAction.REMOVE_ADMIN -> NetworkGroupMemberAction.REMOVE_ADMIN
}
