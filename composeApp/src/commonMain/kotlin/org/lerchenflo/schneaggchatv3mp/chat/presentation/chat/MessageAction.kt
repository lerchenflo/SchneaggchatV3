package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import org.lerchenflo.schneaggchatv3mp.chat.domain.Message

sealed interface MessageAction {
    // Poll actions
    data class VotePoll(val messageId: String, val optionId: String, val checked: Boolean) : MessageAction
    data class AddCustomPollOption(val messageId: String, val text: String) : MessageAction

    // Audio action
    data class PlayAudio(val messageId: String, val audioPath: String, ) : MessageAction
    data class PauseAudio(val dummy: Boolean = true) : MessageAction

    data class SeekAudio(val position: Long) : MessageAction


    // Message actions (moving existing ones to be consistent)
    data class DeleteMessage(val message: Message) : MessageAction
    data class StartEditMessage(val message: Message) : MessageAction

    data object CancelEditMessage: MessageAction
    data class ReplyToMessage(val message: Message) : MessageAction
}