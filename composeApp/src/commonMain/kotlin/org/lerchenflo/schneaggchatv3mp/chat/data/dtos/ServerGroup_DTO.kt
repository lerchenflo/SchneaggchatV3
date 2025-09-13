package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.utilities.ColorIntSerializer

// --- DTOs expected from server (adjust SerialName fields to match your API) ---

@Serializable
data class ServerGroupMemberDto(
    @SerialName("entryid") val entryid: Long = 0L,
    @SerialName("_id") val userId: Long = 0L,
    @SerialName("_color") @Serializable(with = ColorIntSerializer::class) val color: Int? = null,
    @SerialName("_joindate") val joinDate: String? = null,
    @SerialName("_isadmin") val isAdmin: Boolean = false
)

@Serializable
data class ServerGroupDto(
    @SerialName("id") val id: Long = 0L,
    @SerialName("groupname") val name: String? = null,
    @SerialName("profilepicture") val profilePicture: String,
    @SerialName("gruppenbeschreibung") val description: String,
    @SerialName("createdate") val createDate: String,
    @SerialName("lastchanged") val changeDate: String,
    @SerialName("members") val members: List<ServerGroupMemberDto> = emptyList()
)

// --- Conversion function ---

fun convertServerGroupDtoToGroupWithMembersDto(dto: ServerGroupDto): GroupWithMembersDto {
    val group = GroupDto(
        id = dto.id,
        name = dto.name ?: "",
        profilePicture = dto.profilePicture,
        description = dto.description,
        createDate = dto.createDate,
        changedate = dto.changeDate
    )

    val members: List<GroupMemberDto> = dto.members.map { mDto ->
        GroupMemberDto(
            id = mDto.entryid,
            gid = dto.id,
            uid = mDto.userId,
            color = mDto.color ?: 0,
            joinDate = mDto.joinDate ?: "",
            isAdmin = mDto.isAdmin
        )
    }

    return GroupWithMembersDto(group = group, members = members)
}

