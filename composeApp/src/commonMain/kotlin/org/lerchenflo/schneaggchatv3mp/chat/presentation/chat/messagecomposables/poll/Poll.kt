package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.messagecomposables.poll

data class PollMessage(
    val creatorId: String,
    val title: String,
    val description: String?,

    val allowCustomAnswers: Boolean,
    val allowMultipleAnswers: Boolean,
    val showAnswers: Boolean,
    val expiresAt: Long?,

    val voteOptions: List<PollVoteOption> = emptyList(),
)

data class PollVoteOption(
    val id: String,
    val text: String,
    val custom: Boolean,
    val voters : List<PollVoter>
)

data class PollVoter(
    val userId: String,
    val votedAt: Long
)