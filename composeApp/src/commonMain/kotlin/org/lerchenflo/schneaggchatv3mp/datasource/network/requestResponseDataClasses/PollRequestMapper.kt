package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.chat.domain.NewPoll
import org.lerchenflo.schneaggchatv3mp.chat.domain.NewPollOption
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVote

// ─── Poll create ─────────────────────────────────────────────────────────────

fun NewPoll.toPollCreateRequest(): PollCreateRequest = PollCreateRequest(
    title = title,
    description = description,
    maxAnswers = maxAnswers,
    customAnswersEnabled = customAnswersEnabled,
    maxAllowedCustomAnswers = maxAllowedCustomAnswers,
    visibility = visibility,
    closeDate = closeDate,
    voteOptions = voteOptions.map { it.toPollVoteOptionCreateRequest() },
    allowDeleteOptions = allowDeleteOptions,
    showCheckboxes = showCheckboxes,
    visibleToAll = visibleToAll,
)

fun NewPollOption.toPollVoteOptionCreateRequest(): PollVoteOptionCreateRequest =
    PollVoteOptionCreateRequest(
        text = text,
        maxVoters = maxVoters,
        subPoll = subPoll?.toPollCreateRequest(),
    )

// ─── Poll vote ───────────────────────────────────────────────────────────────

fun PollVote.toPollVoteRequest(): PollVoteRequest = PollVoteRequest(
    messageId = messageId,
    id = id,
    text = text,
    maxAllowedAnswers = maxAllowedAnswers,
    selected = selected,
    parentOptionId = parentOptionId,
)
