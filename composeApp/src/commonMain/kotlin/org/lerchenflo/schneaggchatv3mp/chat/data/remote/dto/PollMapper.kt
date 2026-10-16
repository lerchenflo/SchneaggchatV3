package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.chat.domain.NewPoll
import org.lerchenflo.schneaggchatv3mp.chat.domain.NewPollOption
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoteOption
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVoter
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.PollResponse

/**
 * Local echo of a poll that is still being sent, sub polls included. Option ids are fakes ("0", "0.1",
 * ...), unique across the whole tree, until the server response replaces them.
 */
fun NewPoll.toLocalPollMessage(ownId: String, idPrefix: String = ""): PollMessage {
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
 * Inverse of [toLocalPollMessage]: the [NewPoll] for a poll that is still unsent, sub polls
 * included - used when the offline queue resends it.
 */
fun PollMessage.toNewPoll(): NewPoll {
    return NewPoll(
        title = title,
        description = description,
        maxAnswers = maxAnswers,
        customAnswersEnabled = customAnswersEnabled,
        maxAllowedCustomAnswers = maxAllowedCustomAnswers,
        visibility = visibility,
        closeDate = expiresAt,
        voteOptions = voteOptions.map { option ->
            NewPollOption(
                text = option.text,
                maxVoters = option.maxVoters,
                subPoll = option.subPoll?.toNewPoll(),
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
