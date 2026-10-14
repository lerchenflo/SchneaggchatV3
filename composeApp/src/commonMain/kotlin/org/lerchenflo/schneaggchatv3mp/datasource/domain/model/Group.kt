package org.lerchenflo.schneaggchatv3mp.datasource.domain.model

data class Group(
    val id: String,
    val name: String,
    val profilePictureUrl: String,
    val description: String,
    val createDate: Long,

    val updatedAt: Long,
    val profilePicUpdatedAt: Long,

    val expiresAt: Long? = null,

    val notisMuted: Boolean = false,
    val members: List<GroupMember>
)
