package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupWithMembersDto

data class Group(
    val id: String,
    val name: String,
    val profilePicture: String,
    val description: String,
    val createDate: String? = null,
    val changedate: String? = null,
    val muted: Boolean = false,
    val members: List<GroupMember>
)

fun GroupWithMembersDto.toGroup(): Group = Group(
    id = this.group.id,
    name = this.group.name,
    profilePicture = this.group.profilePicture,
    description = this.group.description,
    createDate = this.group.createDate,
    changedate = this.group.changedate,
    muted = this.group.muted,
    members = this.members.map { groupMemberDto ->
        groupMemberDto.toGroupMember()
    }
)

/** Convert domain Group back to GroupDto (for persistence/transport) */
fun Group.toDto(): GroupWithMembersDto = GroupWithMembersDto(
    group = GroupDto(
        id = this.id,
        name = this.name,
        profilePicture = this.profilePicture,
        description = this.description,
        createDate = this.createDate,
        changedate = this.changedate,
        muted = this.muted
    ),
    members = this.members.map { member ->
        member.toDto()
    }
)