package org.lerchenflo.schneaggchatv3mp.chat.data.local.entity

import org.lerchenflo.schneaggchatv3mp.chat.data.local.entity.relations.GroupWithMembers
import org.lerchenflo.schneaggchatv3mp.chat.domain.Group
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember

fun GroupWithMembers.toGroup(): Group = Group(
    id = this.group.id,
    name = this.group.name,
    profilePictureUrl = this.group.profilePictureUrl,
    description = this.group.description,
    createDate = this.group.createDate,
    updatedAt = this.group.updatedAt,
    notisMuted = this.group.notisMuted,
    profilePicUpdatedAt = this.group.profilePicUpdatedAt,
    expiresAt = this.group.expiresAt,
    members = this.members.map { groupMemberEntity ->
        groupMemberEntity.toGroupMember()
    }
)

/** Convert domain Group back to GroupEntity (for persistence) */
fun Group.toEntity(): GroupWithMembers = GroupWithMembers(
    group = GroupEntity(
        id = this.id,
        name = this.name,
        profilePictureUrl = this.profilePictureUrl,
        description = this.description,
        createDate = this.createDate,
        updatedAt = this.updatedAt,
        profilePicUpdatedAt = this.profilePicUpdatedAt,
        expiresAt = this.expiresAt,
        notisMuted = this.notisMuted
    ),
    members = this.members.map { member ->
        member.toEntity()
    }
)

fun GroupMemberEntity.toGroupMember(): GroupMember = GroupMember(
    localPk = localPk,
    groupId = groupId,
    userId = userId,
    joinDate = joinDate,
    admin = admin,
    color = color,
    memberName = memberName
)

/** Convert domain GroupMember -> persistence GroupMemberEntity */
fun GroupMember.toEntity(): GroupMemberEntity = GroupMemberEntity(
    localPk = localPk,
    groupId = groupId,
    userId = userId,
    joinDate = joinDate,
    admin = admin,
    color = color,
    memberName = memberName
)
