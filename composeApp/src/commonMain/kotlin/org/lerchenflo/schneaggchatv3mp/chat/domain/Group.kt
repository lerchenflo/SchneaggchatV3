package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.GroupDto

data class Group(
    val id: Long = 0L,
    val name: String,
    val profilePicture: String,
    val description: String,
    val createDate: String? = null,
    val changedate: String? = null,
    val muted: Boolean = false
)

fun GroupDto.toGroup(): Group = Group(
    id = this.id,
    name = this.name,
    profilePicture = this.profilePicture,
    description = this.description,
    createDate = this.createDate,
    changedate = this.changedate,
    muted = this.muted
)

/** Convert domain Group back to GroupDto (for persistence/transport) */
fun Group.toDto(): GroupDto = GroupDto(
    id = this.id,
    name = this.name,
    profilePicture = this.profilePicture,
    description = this.description,
    createDate = this.createDate,
    changedate = this.changedate,
    muted = this.muted
)