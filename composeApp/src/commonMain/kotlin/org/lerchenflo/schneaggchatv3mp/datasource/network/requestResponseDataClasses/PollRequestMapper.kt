package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.NewPoll
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.NewPollOption
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.PollVote
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

// ─── Poll create ─────────────────────────────────────────────────────────────

fun NewPoll.toPollCreateRequest(): NetworkUtils.PollCreateRequest = NetworkUtils.PollCreateRequest(
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

fun NewPollOption.toPollVoteOptionCreateRequest(): NetworkUtils.PollVoteOptionCreateRequest =
    NetworkUtils.PollVoteOptionCreateRequest(
        text = text,
        maxVoters = maxVoters,
        subPoll = subPoll?.toPollCreateRequest(),
    )

// ─── Poll vote ───────────────────────────────────────────────────────────────

fun PollVote.toPollVoteRequest(): NetworkUtils.PollVoteRequest = NetworkUtils.PollVoteRequest(
    messageId = messageId,
    id = id,
    text = text,
    maxAllowedAnswers = maxAllowedAnswers,
    selected = selected,
    parentOptionId = parentOptionId,
)
