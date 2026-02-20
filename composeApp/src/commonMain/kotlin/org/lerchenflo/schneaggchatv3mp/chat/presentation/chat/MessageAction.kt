package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import org.lerchenflo.schneaggchatv3mp.chat.domain.Message

sealed interface MessageAction {
    // Poll actions
    data class VotePoll(val messageId: String, val optionId: String) : MessageAction
    data class RemovePollVote(val messageId: String, val optionId: String) : MessageAction
    data class AddCustomPollOption(val messageId: String, val text: String) : MessageAction

    // Message actions (moving existing ones to be consistent)
    data class DeleteMessage(val message: Message) : MessageAction
    data class EditMessage(val messageId: String) : MessageAction
    data class ReplyToMessage(val message: Message) : MessageAction
    data class CopyMessage(val content: String) : MessageAction
}