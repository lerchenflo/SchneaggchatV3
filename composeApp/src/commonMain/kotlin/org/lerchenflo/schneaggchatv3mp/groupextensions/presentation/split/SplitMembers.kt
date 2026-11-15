package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split

import org.lerchenflo.schneaggchatv3mp.sharedUi.text.userIdentityNameText
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.flow.first
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.split_member_me

/** A person in a split: a current group member, or someone an older expense still refers to. */
data class SplitMemberUi(
    val userId: String,
    val name: String,
    val isMe: Boolean,
    val profilePictureUrl: String,
)

/**
 * Resolves [userIds] to display names through [userIdentityRepository] (names known right now; an
 * id not looked up yet shows as "Unknown user") and marks the own user. Sorted with the own user
 * first, then by name.
 */
suspend fun resolveSplitMembers(
    userIds: List<String>,
    userIdentityRepository: UserIdentityRepository,
    ownId: String?,
): List<SplitMemberUi> {
    val users = userIdentityRepository.observeUsers(userIds.distinct()).first()
    val nameText = userIdentityNameText()

    return userIds.distinct().map { userId ->
        val user = users[userId]
        val name = nameText(user?.name)
        val isMe = userId == ownId

        SplitMemberUi(
            userId = userId,
            name = if (isMe) getString(Res.string.split_member_me, name) else name,
            isMe = isMe,
            profilePictureUrl = user?.profilePictureUrl.orEmpty(),
        )
    }.sortedWith(compareByDescending<SplitMemberUi> { it.isMe }.thenBy { it.name.lowercase() })
}
