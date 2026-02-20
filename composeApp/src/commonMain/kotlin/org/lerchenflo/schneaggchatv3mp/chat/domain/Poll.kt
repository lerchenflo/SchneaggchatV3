package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.utilities.UiText
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.poll_visibility_anonym
import schneaggchatv3mp.composeapp.generated.resources.poll_visibility_private
import schneaggchatv3mp.composeapp.generated.resources.poll_visibility_public
import kotlin.time.Clock

@Serializable
data class PollMessage(
    val creatorId: String,
    val title: String,
    val description: String?,

    val maxAnswers: Int?, // null = unlimited

    val customAnswersEnabled: Boolean,
    val maxAllowedCustomAnswers: Int?, // null = unlimited

    val visibility: PollVisibility,

    val expiresAt: Long?,

    val voteOptions: List<PollVoteOption> = emptyList(),
) {
    /**
     * Get total number of votes across all options
     */
    fun getTotalVoteCount(): Int {
        return voteOptions.sumOf { it.voters.size }
    }

    /**
     * Get number of unique users who voted (useful when maxAnswers > 1)
     */
    fun getUniqueVoterCount(): Int {
        return voteOptions
            .flatMap { it.voters }
            .mapNotNull { it.userId }
            .distinct()
            .size
    }

    /**
     * Check if a specific user has voted
     */
    fun hasUserVoted(userId: String): Boolean {
        return voteOptions.any { option ->
            option.voters.any { it.userId == userId }
        }
    }

    /**
     * Get all option IDs that a user voted for
     */
    fun getUserVotes(userId: String): List<String> {
        return voteOptions
            .filter { option -> option.voters.any { it.userId == userId } }
            .map { it.id }
    }

    /**
     * Check if poll has expired
     */
    fun isExpired(): Boolean {
        return expiresAt?.let { it < Clock.System.now().toEpochMilliseconds() } ?: false
    }

    fun acceptsMultipleAnswers(): Boolean {
        return (maxAnswers == null || maxAnswers > 1)
    }
}


@Serializable
data class PollVoteOption(
    val id: String,
    val text: String,
    val custom: Boolean,
    val creatorId: String,
    val voters : List<PollVoter>
)

@Serializable
data class PollVoter(
    val userId: String?,
    val votedAt: Long
)

@Serializable
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