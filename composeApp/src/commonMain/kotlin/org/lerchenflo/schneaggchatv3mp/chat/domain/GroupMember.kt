package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupMemberDto

data class GroupMember(
    val localPk: Long = 0L,
    val groupId: String,
    val userId: String,
    val joinDate: String,
    val admin: Boolean,
    val color: Int,
    val memberName: String
)

fun GroupMemberDto.toGroupMember(): GroupMember = GroupMember(
    localPk = localPk,
    groupId = groupId,
    userId = userId,
    joinDate = joinDate,
    admin = admin,
    color = color,
    memberName = memberName
)

/** Convert domain GroupMember -> persistence/transport GroupMemberDto */
fun GroupMember.toDto(): GroupMemberDto = GroupMemberDto(
    localPk = localPk,
    groupId = groupId,
    userId = userId,
    joinDate = joinDate,
    admin = admin,
    color = color,
    memberName = memberName
)