package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * A vote in a poll, or a new custom option. Domain copy of the network DTO
 * `NetworkUtils.PollVoteRequest`, field for field.
 */
data class PollVote(
    val messageId: String,
    val id: String?, //Pass if available, else this is a new custom option
    val text: String?, //Pass if the id is null (New custom option with this text)
    val maxAllowedAnswers: Int?, //Pass if the user creates a custom entry and the poll supports restricting the entries for votes
    val selected: Boolean, //Did the user select or unselect this item
    val parentOptionId: String? = null, //New custom option only: the option whose sub poll gets it (null = root poll)
)
