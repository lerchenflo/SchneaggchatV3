package org.lerchenflo.schneaggchatv3mp.datasource.domain.model

data class GroupMember(
    val localPk: Long = 0L,
    val groupId: String,
    val userId: String,
    val joinDate: String,
    val admin: Boolean,
    val color: Int,
    val memberName: String
)
