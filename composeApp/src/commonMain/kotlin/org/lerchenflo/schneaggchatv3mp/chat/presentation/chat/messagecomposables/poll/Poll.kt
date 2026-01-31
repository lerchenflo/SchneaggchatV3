package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll

import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.poll_visibility_anonym
import schneaggchatv3mp.composeapp.generated.resources.poll_visibility_private
import schneaggchatv3mp.composeapp.generated.resources.poll_visibility_public

data class PollMessage(
    val creatorId: String,
    val title: String,
    val description: String?,

    val allowCustomAnswers: Boolean,
    val allowMultipleAnswers: Boolean,
    val showAnswers: Boolean,
    val maxCustomAnswers: Int?,

    val expiresAt: Long?,

    val voteOptions: List<PollVoteOption> = emptyList(),
)

data class PollVoteOption(
    val id: String,
    val text: String,
    val custom: Boolean,
    val creatorId: String,
    val voters : List<PollVoter>
)

data class PollVoter(
    val userId: String,
    val votedAt: Long
)

enum class PollVisibility{
    PUBLIC,
    PRIVATE,
    ANONYMOUS;

    fun toUiText() : UiText {
        return when (this) {
            PollVisibility.PUBLIC -> UiText.StringResourceText(Res.string.poll_visibility_public)
            PollVisibility.PRIVATE -> UiText.StringResourceText(Res.string.poll_visibility_private)
            PollVisibility.ANONYMOUS -> UiText.StringResourceText(Res.string.poll_visibility_anonym)
        }
    }
}