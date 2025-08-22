package org.lerchenflo.schneaggchatv3mp.database.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.database.tables.Group
import org.lerchenflo.schneaggchatv3mp.database.tables.GroupMember
import org.lerchenflo.schneaggchatv3mp.database.tables.GroupWithMembers
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
    @SerialName("profilepicture") val profilePicture: String? = null,
    @SerialName("gruppenbeschreibung") val description: String? = null,
    @SerialName("createdate") val createDate: String? = null,
    @SerialName("lastchanged") val changeDate: String? = null,
    @SerialName("members") val members: List<ServerGroupMemberDto> = emptyList()
)

// --- Conversion function ---

fun convertServerGroupDtoToGroupWithMembers(serverList: List<ServerGroupDto>): List<GroupWithMembers> {
    return serverList.map { dto ->
        // pick the real group id (prefer 'id' then 'groupid' alias)

        val group = Group(
            id = dto.id,
            name = dto.name ?: "",                     // require non-null in your local model (adjust if you allow null)
            profilePicture = dto.profilePicture,
            description = dto.description,
            createDate = dto.createDate,
            changedate = dto.changeDate
        )

        val members: List<GroupMember> = dto.members.map { mDto ->
            // determine uid (string) — try string, then numeric fallback
            val uid = mDto.userId

            // determine color: prefer numeric color, else parse hex if present, else 0
            val colorInt: Int = mDto.color ?: 0

            GroupMember(
                id = 0,                // local DB will autogenerate entry id
                gid = dto.id,
                uid = uid,
                color = colorInt,
                joinDate = mDto.joinDate ?: "",
                isAdmin = mDto.isAdmin
            )
        }

        GroupWithMembers(group = group, members = members)
    }
}
