package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoter
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.PollResponse

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
