package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.GroupResponse

@Serializable
data class GroupSyncResponse(
    val updatedGroups: List<GroupResponse>,
    val deletedGroups: List<String>
)

enum class NetworkGroupMemberAction {
    ADD_USER,
    REMOVE_USER,
    MAKE_ADMIN,
    REMOVE_ADMIN
}

@Serializable
data class GroupActionRequest(
    val action: NetworkGroupMemberAction,
    val groupMemberId: String,
    val groupId: String
)

@Serializable
data class SetGroupExpiryRequest(
    val groupId: String,
    val expiresAt: Long?
)
