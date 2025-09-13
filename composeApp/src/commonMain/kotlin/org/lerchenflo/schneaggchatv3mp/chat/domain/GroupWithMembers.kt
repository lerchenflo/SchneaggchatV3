package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupWithMembersDto

data class GroupWithMembers(
    val group: Group,
    val members: List<GroupMember>
) : SelectedChat() {
    override val id: Long
        get() = group.id
    override val isGroup: Boolean
        get() = true
    override val name: String
        get() = group.name
    override val profilePicture: String
        get() = group.profilePicture
    override val status: String
        get() = group.description //TODO Was für an groupstatus oda einfach silo mak a o nix returnen
    override val description: String
        get() = group.description

}

fun GroupWithMembersDto.toGroupWithMembers(): GroupWithMembers =
    GroupWithMembers(
        group = this.group.toGroup(),
        members = this.members.map { it.toGroupMember() }
    )

/** Domain -> DTO */
fun GroupWithMembers.toDto(): GroupWithMembersDto =
    GroupWithMembersDto(
        group = this.group.toDto(),
        members = this.members.map { it.toDto() }
    )