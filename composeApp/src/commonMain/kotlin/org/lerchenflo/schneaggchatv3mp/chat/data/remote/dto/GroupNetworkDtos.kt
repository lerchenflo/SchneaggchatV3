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

/** `/groups/settings`: partial update, null = unchanged. expiresAt, clearExpiresAt and membersCanAdd are admin-only. */
@Serializable
data class GroupSettingsRequest(
    val groupId: String,
    val name: String? = null,
    val description: String? = null,
    val expiresAt: Long? = null,
    val clearExpiresAt: Boolean? = null,
    val membersCanAdd: Boolean? = null,
)
