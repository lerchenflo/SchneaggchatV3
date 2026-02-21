package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoter


fun PollResponse.toPollMessage(): PollMessage {

    return PollMessage(
        creatorId = this.creatorId,
        title = this.title,
        description = this.description,
        maxAnswers = this.maxAnswers,
        customAnswersEnabled = this.customAnswersEnabled,
        maxAllowedCustomAnswers = this.maxAllowedCustomAnswers,
        visibility = this.visibility,
        expiresAt = this.closeDate,
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
                    }
                )
            }
            is PollResponse.AnonymousPollResponse -> this.voteOptions.map { option ->
                PollVoteOption(
                    id = option.id,
                    text = option.text,
                    custom = false,   // Not available in anonymous response
                    creatorId = this.creatorId, // Not available in anonymous response
                    voters = option.voters.map { voter ->
                        PollVoter(
                            userId = if (voter.myAnswer) SessionCache.getOwnIdValue() else null, // Anonymous — no userId
                            votedAt = voter.votedAt
                        )
                    }
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

        val voteOptions: List<AnonymousPollVoteOptionResponse>,

        ) : PollResponse

}

@Serializable
data class AnonymousPollVoteOptionResponse(
    val id: String,
    val text: String,
    val voters : List<AnonymousPollVoterResponse>
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
    val voters : List<PublicPollVoterResponse>
)

@Serializable
data class PublicPollVoterResponse(
    val userId: String,
    val votedAt: Long,
)