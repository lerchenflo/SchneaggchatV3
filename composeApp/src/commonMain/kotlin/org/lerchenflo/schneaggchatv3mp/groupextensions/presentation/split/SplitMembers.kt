package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split

import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.chat.data.UserRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.split_member_me
import schneaggchatv3mp.composeapp.generated.resources.split_unknown_member

/** A person in a split: a current group member, or someone an older expense still refers to. */
data class SplitMemberUi(
    val userId: String,
    val name: String,
    val isMe: Boolean,
    val profilePictureUrl: String,
)

/**
 * Resolves [userIds] to display names - user name, else the group nickname, else "former member" -
 * and marks the own user. Sorted with the own user first, then by name.
 */
suspend fun resolveSplitMembers(
    userIds: List<String>,
    groupMembers: List<GroupMember>,
    userRepository: UserRepository,
    ownId: String?,
): List<SplitMemberUi> {
    val memberById = groupMembers.associateBy { it.userId }

    return userIds.distinct().map { userId ->
        val user = userRepository.getUserById(userId)
        val name = user?.displayName?.takeIf { it.isNotBlank() }
            ?: memberById[userId]?.memberName?.takeIf { it.isNotBlank() }
            ?: getString(Res.string.split_unknown_member)
        val isMe = userId == ownId

        SplitMemberUi(
            userId = userId,
            name = if (isMe) getString(Res.string.split_member_me, name) else name,
            isMe = isMe,
            profilePictureUrl = user?.profilePictureUrl.orEmpty(),
        )
    }.sortedWith(compareByDescending<SplitMemberUi> { it.isMe }.thenBy { it.name.lowercase() })
}
