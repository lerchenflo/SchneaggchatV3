package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMemberAction
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupResponse

// ─── Group member action ─────────────────────────────────────────────────────

fun GroupMemberAction.toNetworkGroupMemberAction(): NetworkGroupMemberAction = when (this) {
    GroupMemberAction.ADD_USER -> NetworkGroupMemberAction.ADD_USER
    GroupMemberAction.REMOVE_USER -> NetworkGroupMemberAction.REMOVE_USER
    GroupMemberAction.MAKE_ADMIN -> NetworkGroupMemberAction.MAKE_ADMIN
    GroupMemberAction.REMOVE_ADMIN -> NetworkGroupMemberAction.REMOVE_ADMIN
}

// ─── Group ───────────────────────────────────────────────────────────────────

/**
 * The server's group as a domain [Group]. The profile picture URL and the mute switch are local
 * only, so they are taken from [existing] (the stored row, if any).
 */
fun GroupResponse.toGroup(existing: Group? = null): Group = Group(
    id = id,
    name = name,
    profilePictureUrl = existing?.profilePictureUrl ?: "",
    description = description,
    createDate = createdAt,
    updatedAt = updatedAt,
    profilePicUpdatedAt = profilePicUpdatedAt,
    expiresAt = expiresAt,
    notisMuted = existing?.notisMuted ?: false,
    members = members.map { groupMemberresp ->
        GroupMember(
            groupId = id,
            userId = groupMemberresp.userid,
            joinDate = groupMemberresp.joinedAt,
            admin = groupMemberresp.admin,
            color = groupMemberresp.color,
            memberName = groupMemberresp.memberName
        )
    }
)
