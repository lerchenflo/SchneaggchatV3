package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.Serializable

@Serializable
data class GroupResponse(
    val id: String,
    val name: String,
    val description: String,

    val updatedAt: Long,
    val profilePicUpdatedAt: Long,

    val createdAt: Long,
    val creatorId: String,
    val expiresAt: Long?,
    val members: List<GroupMemberResponse>
)

@Serializable
data class GroupMemberResponse(
    val userid: String,
    val joinedAt: String,
    val admin: Boolean,
    val color: Int,
    val memberName: String = ""
)

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
