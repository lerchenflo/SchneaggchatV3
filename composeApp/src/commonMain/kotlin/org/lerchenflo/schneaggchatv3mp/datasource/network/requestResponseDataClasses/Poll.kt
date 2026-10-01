package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoter


/**
 * Local echo of a poll that is still being sent, sub polls included. Option ids are fakes ("0", "0.1",
 * ...), unique across the whole tree, until the server response replaces them.
 */
fun NetworkUtils.PollCreateRequest.toLocalPollMessage(ownId: String, idPrefix: String = ""): PollMessage {
    return PollMessage(
        creatorId = ownId,
        title = title,
        description = description,
        maxAnswers = maxAnswers,
        customAnswersEnabled = customAnswersEnabled,
        maxAllowedCustomAnswers = maxAllowedCustomAnswers,
        visibility = visibility,
        expiresAt = closeDate,
        allowDeleteOptions = allowDeleteOptions,
        showCheckboxes = showCheckboxes,
        visibleToAll = visibleToAll,
        voteOptions = voteOptions.mapIndexed { index, request ->
            val id = idPrefix + index
            PollVoteOption(
                id = id,
                text = request.text,
                custom = false,
                creatorId = ownId,
                voters = emptyList(),
                maxVoters = request.maxVoters,
                createdByMe = true,
                subPoll = request.subPoll?.toLocalPollMessage(ownId, idPrefix = "$id."),
            )
        }
    )
}

/**
 * Inverse of [toLocalPollMessage]: the create request for a poll that is still unsent, sub polls
 * included - used when the offline queue resends it.
 */
fun PollMessage.toCreateRequest(): NetworkUtils.PollCreateRequest {
    return NetworkUtils.PollCreateRequest(
        title = title,
        description = description,
        maxAnswers = maxAnswers,
        customAnswersEnabled = customAnswersEnabled,
        maxAllowedCustomAnswers = maxAllowedCustomAnswers,
        visibility = visibility,
        closeDate = expiresAt,
        voteOptions = voteOptions.map { option ->
            NetworkUtils.PollVoteOptionCreateRequest(
                text = option.text,
                maxVoters = option.maxVoters,
                subPoll = option.subPoll?.toCreateRequest(),
            )
        },
        allowDeleteOptions = allowDeleteOptions,
        showCheckboxes = showCheckboxes,
        visibleToAll = visibleToAll,
    )
}

fun PollResponse.toPollMessage(ownId: String): PollMessage {

    return PollMessage(
        creatorId = this.creatorId,
        title = this.title,
        description = this.description,
        maxAnswers = this.maxAnswers,
        customAnswersEnabled = this.customAnswersEnabled,
        maxAllowedCustomAnswers = this.maxAllowedCustomAnswers,
        visibility = this.visibility,
        expiresAt = this.closeDate,
        allowDeleteOptions = this.allowDeleteOptions,
        showCheckboxes = this.showCheckboxes,
        visibleToAll = this.visibleToAll,
        voteOptions = when (this) {
            is PollResponse.PublicPollResponse -> this.voteOptions.map { option ->
                PollVoteOption(
                    id = option.id,
                    text = option.text,
                    custom = option.custom,
                    creatorId = option.creatorId,
                    voters = option.voters.map { voter ->
                        PollVoter(
                            userId = voter.userId,
                            votedAt = voter.votedAt
                        )
                    },
                    maxVoters = option.maxVoters,
                    createdByMe = option.creatorId == ownId,
                    subPoll = option.subPoll?.toPollMessage(ownId),
                )
            }
            is PollResponse.AnonymousPollResponse -> this.voteOptions.map { option ->
                PollVoteOption(
                    id = option.id,
                    text = option.text,
                    custom = option.custom,
                    creatorId = this.creatorId, // Not available per-option in anonymous response
                    voters = option.voters.map { voter ->
                        PollVoter(
                            userId = if (voter.myAnswer) ownId else null, // Anonymous — no userId
                            votedAt = voter.votedAt
                        )
                    },
                    maxVoters = option.maxVoters,
                    createdByMe = option.createdByMe,
                    subPoll = option.subPoll?.toPollMessage(ownId),
                )
            }
            else -> emptyList()
        }
    )
}




interface PollResponse {

    val creatorId: String
    val title: String
    val description: String?


    val maxAnswers: Int? // null = unlimited
    val customAnswersEnabled: Boolean
    val maxAllowedCustomAnswers: Int? // null = unlimited

    val visibility: PollVisibility


    val closeDate: Long?

    val allowDeleteOptions: Boolean
    val showCheckboxes: Boolean
    val visibleToAll: Boolean



    @Serializable
    @SerialName("public")
    data class PublicPollResponse (
        override val creatorId: String,
        override val title: String,
        override val description: String?,
        override val maxAnswers: Int?,
        override val customAnswersEnabled: Boolean,
        override val maxAllowedCustomAnswers: Int?,
        override val visibility: PollVisibility,
        override val closeDate: Long?,
        override val allowDeleteOptions: Boolean = false,
        override val showCheckboxes: Boolean = true,
        override val visibleToAll: Boolean = true,

        val voteOptions: List<PublicPollVoteOptionResponse>,

        ) : PollResponse

    @Serializable
    @SerialName("anonymous")
    data class AnonymousPollResponse (
        override val creatorId: String,
        override val title: String,
        override val description: String?,
        override val maxAnswers: Int?,
        override val customAnswersEnabled: Boolean,
        override val maxAllowedCustomAnswers: Int?,
        override val visibility: PollVisibility,
        override val closeDate: Long?,
        override val allowDeleteOptions: Boolean = false,
        override val showCheckboxes: Boolean = true,
        override val visibleToAll: Boolean = true,

        val voteOptions: List<AnonymousPollVoteOptionResponse>,

        ) : PollResponse

}

@Serializable
data class AnonymousPollVoteOptionResponse(
    val id: String,
    val text: String,
    val custom: Boolean = false,
    val createdByMe: Boolean = false,
    val voters : List<AnonymousPollVoterResponse>,
    val maxVoters: Int? = null,
    val subPoll: PollResponse? = null,
)

@Serializable
data class AnonymousPollVoterResponse(
    val myAnswer: Boolean,
    val votedAt: Long,
)



@Serializable
data class PublicPollVoteOptionResponse(
    val id: String,
    val text: String,
    val custom: Boolean,
    val creatorId: String,
    val voters : List<PublicPollVoterResponse>,
    val maxVoters: Int? = null,
    val subPoll: PollResponse? = null,
)

@Serializable
data class PublicPollVoterResponse(
    val userId: String,
    val votedAt: Long,
)