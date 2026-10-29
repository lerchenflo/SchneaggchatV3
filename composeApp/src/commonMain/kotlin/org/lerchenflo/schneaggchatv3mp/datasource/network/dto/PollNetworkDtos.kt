package org.lerchenflo.schneaggchatv3mp.datasource.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility

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
