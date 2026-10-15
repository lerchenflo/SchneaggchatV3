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
    //Ids get assigned by the server
    val text: String,
    val maxVoters: Int? = null, // null = unlimited
    val subPoll: NewPoll? = null, //Follow-up poll for users who pick this option
)
