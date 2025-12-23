package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupDto
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.relations.GroupWithMembersDto

data class Group(
    val id: String,
    val name: String,
    val profilePictureUrl: String,
    val description: String,
    val createDate: String? = null,
    val changedate: String? = null,
    val notisMuted: Boolean = false,
    val members: List<GroupMember>
)

fun GroupWithMembersDto.toGroup(): Group = Group(
    id = this.group.id,
    name = this.group.name,
    profilePictureUrl = this.group.profilePictureUrl,
    description = this.group.description,
    createDate = this.group.createDate,
    changedate = this.group.changedate,
    notisMuted = this.group.notisMuted,
    members = this.members.map { groupMemberDto ->
        groupMemberDto.toGroupMember()
    }
)

/** Convert domain Group back to GroupDto (for persistence/transport) */
fun Group.toDto(): GroupWithMembersDto = GroupWithMembersDto(
    group = GroupDto(
        id = this.id,
        name = this.name,
        profilePictureUrl = this.profilePictureUrl,
        description = this.description,
        createDate = this.createDate,
        changedate = this.changedate,
        notisMuted = this.notisMuted
    ),
    members = this.members.map { member ->
        member.toDto()
    }
)