package org.lerchenflo.schneaggchatv3mp.chat.domain

data class GroupMember(
    val localPk: Long = 0L,
    val groupId: String,
    val userId: String,
    val joinDate: String,
    val admin: Boolean,
    val color: Int,
    val memberName: String
)
