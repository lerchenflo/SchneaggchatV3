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

    //If true, the creator may delete any option, and users may delete options they created
    val allowDeleteOptions: Boolean = false,

    //If false, the poll renders as a plain read-only list - no voting, no checkboxes
    val showCheckboxes: Boolean = true,

    //Only meaningful on a sub poll: true = users who did not pick the parent option may still look at it
    //(read-only). If false the server does not even send it to them.
    val visibleToAll: Boolean = true,
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

    /**
     * Check if an option has reached its own per-entry vote limit for anyone other than excludingUserId
     */
    fun optionIsFull(option: PollVoteOption, excludingUserId: String): Boolean {
        return option.maxVoters != null && option.voters.count { it.userId != excludingUserId } >= option.maxVoters
    }

    /**
     * Whether ownId may delete this option: the poll creator may delete any option,
     * and a user may delete an option they created themselves (works for anonymous polls too,
     * since createdByMe is computed server-side rather than compared via creatorId).
     */
    fun canDeleteOption(option: PollVoteOption, ownId: String): Boolean =
        allowDeleteOptions && !isExpired() && (creatorId == ownId || option.createdByMe)

    /** Number of sub polls in this poll tree (not counting this poll itself). */
    fun subPollCount(): Int = voteOptions.sumOf { option ->
        option.subPoll?.let { 1 + it.subPollCount() } ?: 0
    }

    /** Whether anyone answered anything in this poll tree, sub polls included. */
    fun hasAnyVotes(): Boolean =
        voteOptions.any { it.voters.isNotEmpty() || it.subPoll?.hasAnyVotes() == true }

    /**
     * The option whose vote the server drops when [userId] picks one more option on a poll with an
     * answer limit (their oldest vote), or null if picking does not push anything out.
     */
    fun optionDroppedBySelecting(userId: String): PollVoteOption? {
        val max = maxAnswers ?: return null
        if (getUserVotes(userId).size < max) return null
        return voteOptions
            .flatMap { option -> option.voters.filter { it.userId == userId }.map { option to it } }
            .minByOrNull { (_, voter) -> voter.votedAt }
            ?.first
    }

    /** Whether userId answered anything inside this poll tree, sub polls included. */
    fun hasUserVotedAnywhere(userId: String): Boolean =
        hasUserVoted(userId) || voteOptions.any { it.subPoll?.hasUserVotedAnywhere(userId) == true }
}


@Serializable
data class PollVoteOption(
    val id: String,
    val text: String,
    val custom: Boolean,
    val creatorId: String,
    val voters : List<PollVoter>,
    val maxVoters: Int? = null, // null = unlimited

    //True if the current user created this option - computed server-side so it also works on anonymous polls
    val createdByMe: Boolean = false,

    //Follow-up poll for users who pick this option. Null if there is none, or the server hides it from us
    val subPoll: PollMessage? = null,
) {
    /**
     * Get list of user IDs who voted for this option
     */
    fun getVoterIdsForOption(): List<String?> {
        return voters.map { it.userId }
    }

}

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