package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * A poll the user is about to send, sub polls included. Domain copy of the network DTO
 * `PollCreateRequest`, field for field.
 */
data class NewPoll(
    val title: String,
    val description: String?,

    val maxAnswers: Int?, // null = unlimited
    val customAnswersEnabled: Boolean,
    val maxAllowedCustomAnswers: Int?, // null = unlimited

    val visibility: PollVisibility,

    val closeDate: Long?,

    val voteOptions: List<NewPollOption>,

    val allowDeleteOptions: Boolean = false,
    val showCheckboxes: Boolean = true,

    //Only read on sub polls: may users who didn't pick the parent option look at it
    val visibleToAll: Boolean = true,
)

/**
 * One option of a [NewPoll]. Domain copy of the network DTO `PollVoteOptionCreateRequest`,
 * field for field.
 */
data class NewPollOption(
    val text: String,
    val maxVoters: Int? = null, // null = unlimited
    val subPoll: NewPoll? = null, //Follow-up poll for users who pick this option
    //Only set on a poll edit: the server id of the existing option. Null = new option, the server assigns the id
    val id: String? = null,
)

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

private fun NewPoll.allOptions(): List<NewPollOption> =
    voteOptions.flatMap { option -> listOf(option) + option.subPoll?.allOptions().orEmpty() }

private fun PollMessage.allOptions(): List<PollVoteOption> =
    voteOptions.flatMap { option -> listOf(option) + option.subPoll?.allOptions().orEmpty() }

/** Ids of the options, sub polls included, that are gone in [edited]. The server deletes only these. */
fun PollMessage.optionIdsRemovedBy(edited: NewPoll): List<String> {
    val keptIds = edited.allOptions().mapNotNull { it.id }.toSet()
    return allOptions().map { it.id }.filter { it !in keptIds }
}

/** Whether saving [edited] drops votes: the server drops them for a removed option and for a changed text. */
fun PollMessage.losesVotesBy(edited: NewPoll): Boolean {
    val editedTexts = edited.allOptions().filter { it.id != null }.associate { it.id to it.text.trim() }
    return allOptions().any { option -> option.voters.isNotEmpty() && editedTexts[option.id] != option.text.trim() }
}
