package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupMemberDto

data class GroupMember(
    val id: String,
    val groupId: String,
    val userId: String,
    val color: Int,
    val joinDate: String,
    val isAdmin: Boolean
)

fun GroupMemberDto.toGroupMember(): GroupMember = GroupMember(
    id = this.id,
    groupId = this.gid,
    userId = this.uid,
    color = this.color,
    joinDate = this.joinDate,
    isAdmin = this.isAdmin
)

/** Convert domain GroupMember -> persistence/transport GroupMemberDto */
fun GroupMember.toDto(): GroupMemberDto = GroupMemberDto(
    id = this.id,
    gid = this.groupId,
    uid = this.userId,
    color = this.color,
    joinDate = this.joinDate,
    isAdmin = this.isAdmin
)