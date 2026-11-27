package org.lerchenflo.schneaggchatv3mp.datasource.network.dto

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
    val membersCanAdd: Boolean = false, //default keeps decoding working against a server without the field
    val members: List<GroupMemberResponse>
)

@Serializable
data class GroupMemberResponse(
    val userid: String,
    val joinedAt: String,
    val admin: Boolean,
    val color: Int,
)
