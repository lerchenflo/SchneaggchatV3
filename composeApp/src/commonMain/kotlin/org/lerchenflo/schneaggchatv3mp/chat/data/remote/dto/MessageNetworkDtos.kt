package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollVisibility
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.MessageResponse

@Serializable
data class MessageRequest(
    val messageId: String?, //Objectid

    val receiverId: String,
    val groupMessage: Boolean,
    val msgType: MessageType,
    val content: String,
    val answerId: String?,
    val clientMessageId: String,
)

@Serializable
data class ImageMessageRequest(
    val messageId: String?, //Objectid

    val receiverId: String,
    val groupMessage: Boolean,
    val msgType: MessageType,
    val content: String,
    val answerId: String?,
    val clientMessageId: String,
)

@Serializable
data class AudioMessageRequest(
    val messageId: String?, //Objectid

    val receiverId: String,
    val groupMessage: Boolean,
    val msgType: MessageType,
    //val content: String,
    val answerId: String?,
    val clientMessageId: String,
)

@Serializable
data class PollMessageRequest(
    val receiverId: String,
    val groupMessage: Boolean,
    val msgType: MessageType,
    val answerId: String?,
    val poll: PollCreateRequest,
    val clientMessageId: String,
)

@Serializable
data class PollCreateRequest(
    val title: String,
    val description: String?,

    val maxAnswers: Int?, // null = unlimited
    val customAnswersEnabled: Boolean,
    val maxAllowedCustomAnswers: Int?, // null = unlimited

    val visibility: PollVisibility,

    val closeDate: Long?,

    val voteOptions: List<PollVoteOptionCreateRequest>,

    val allowDeleteOptions: Boolean = false,
    val showCheckboxes: Boolean = true,

    //Only read on sub polls: may users who didn't pick the parent option look at it
    val visibleToAll: Boolean = true,
)

/**
 * Data class needed for appending options when creating a poll
 */
@Serializable
data class PollVoteOptionCreateRequest(
    //Ids get assigned by the server
    val text: String,
    val maxVoters: Int? = null, // null = unlimited
    val subPoll: PollCreateRequest? = null, //Follow-up poll for users who pick this option
)

/**
 * Vote in a poll
 */
@Serializable
data class PollVoteRequest(
    val messageId: String,
    val id: String?, //Pass if available, else this is a new custom option
    val text: String?, //Pass if the id is null (New custom option with this text)
    val maxAllowedAnswers: Int?, //Pass if the user creates a custom entry and the poll supports restricting the entries for votes
    val selected: Boolean, //Did the user select or unselect this item
    val parentOptionId: String? = null, //New custom option only: the option whose sub poll gets it (null = root poll)
)

/**
 * Delete a vote option from a poll
 */
@Serializable
data class PollOptionDeleteRequest(
    val messageId: String,
    val optionId: String,
)

@Serializable
data class MessageSyncResponse(
    val updatedMessages: List<MessageResponse>,
    val deletedMessages: List<String>,
    val newVersion: Long,
    val moreMessages: Boolean
)

@Serializable
data class EditMessageRequest(
    val messageId: String,
    val newContent: String,
)

@Serializable
data class ReactionRequest(
    val messageId: String,
    val content: String
)
