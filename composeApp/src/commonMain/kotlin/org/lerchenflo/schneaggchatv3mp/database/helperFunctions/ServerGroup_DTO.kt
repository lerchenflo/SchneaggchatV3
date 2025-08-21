package org.lerchenflo.schneaggchatv3mp.database.helperFunctions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.database.tables.Group
import org.lerchenflo.schneaggchatv3mp.database.tables.GroupMember
import org.lerchenflo.schneaggchatv3mp.database.tables.GroupWithMembers

// --- DTOs expected from server (adjust SerialName fields to match your API) ---

@Serializable
data class ServerGroupMemberDto(
    @SerialName("entryid") val entryid: Long = 0L,           // optional server-side PK for member entry
    @SerialName("user_id") val userId: String? = null,       // preferred: string user id
    @SerialName("userid") val userIdAlt: Long = -1L,         // alternative numeric user id variant
    @SerialName("color") val color: Int? = null,             // numeric color (ARGB/RGB int)
    @SerialName("color_hex") val colorHex: String? = null,   // optional hex string e.g. "#FF3366"
    @SerialName("join_date") val joinDate: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false
)

@Serializable
data class ServerGroupDto(
    @SerialName("id") val id: Long = 0L,
    @SerialName("groupid") val groupidAlt: Long = 0L, // in case server uses groupid instead of id
    @SerialName("name") val name: String? = null,
    @SerialName("profile_picture") val profilePicture: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("create_date") val createDate: String? = null,
    @SerialName("change_date") val changeDate: String? = null,
    @SerialName("creator_id") val creatorId: String? = null,
    @SerialName("creatorid") val creatorIdAlt: Long = -1L, // alternative numeric creator id
    @SerialName("notis_muted") val muted: Boolean = false,
    @SerialName("members") val members: List<ServerGroupMemberDto> = emptyList(), // common name
    @SerialName("mitglieder") val mitglieder: List<ServerGroupMemberDto> = emptyList() // german variant fallback
)

// --- Conversion function ---

fun convertServerGroupDtoToGroupWithMembers(serverList: List<ServerGroupDto>): List<GroupWithMembers> {
    return serverList.map { dto ->
        // pick the real group id (prefer 'id' then 'groupid' alias)
        val groupId = if (dto.id != 0L) dto.id else dto.groupidAlt

        val group = Group(
            id = groupId,
            name = dto.name ?: "",                     // require non-null in your local model (adjust if you allow null)
            profilePicture = dto.profilePicture,
            description = dto.description,
            createDate = dto.createDate,
            changeDate = dto.changeDate,
            creatorid = when {
                dto.creatorId != null -> dto.creatorId
                dto.creatorIdAlt >= 0L -> dto.creatorIdAlt.toString()
                else -> ""
            },
            muted = dto.muted
        )

        // pick members from whichever field the server used
        val serverMembers = if (dto.members.isNotEmpty()) dto.members else dto.mitglieder

        val members: List<GroupMember> = serverMembers.map { mDto ->
            // determine uid (string) — try string, then numeric fallback
            val uid = mDto.userId ?: run {
                if (mDto.userIdAlt >= 0L) mDto.userIdAlt.toString() else ""
            }

            // determine color: prefer numeric color, else parse hex if present, else 0
            val colorInt: Int = mDto.color ?: run {
                mDto.colorHex?.let { hex ->
                    try {
                        // allow leading "#" or not, parse as hex
                        hex.removePrefix("#").toInt()
                    } catch (e: Exception) {
                        0
                    }
                } ?: 0
            }

            GroupMember(
                id = 0,                // local DB will autogenerate entry id
                gid = groupId,
                uid = uid,
                color = colorInt,
                joinDate = mDto.joinDate ?: "",
                isAdmin = mDto.isAdmin
            )
        }

        GroupWithMembers(group = group, members = members)
    }
}
