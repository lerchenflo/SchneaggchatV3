package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import io.github.ismoy.imagepickerkmp.picker.GalleryPhotoResult
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.SenderInfo
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

@Stable
data class ChatState(
    val chatId: String = "",
    val isGroup: Boolean = false,
    val chatPartner: ChatListItem? = null,
    val displayItems: List<MessageDisplayItem> = emptyList(),
    val sendContent: SendMessageContent = SendMessageContent.TextContent(TextFieldValue("")),
    val replyMessage: Message? = null,
    val replyMessageSender: SenderInfo? = null,
    val editMessage: Message? = null,
    val markdownEnabled: Boolean = false,
    val isDesktop: Boolean = false,
    val maxVoiceMsgTime: Long = 0L,
)

sealed interface ChatAction {
    data object OnBackClick : ChatAction
    data object OnChatDetailsClick : ChatAction

    data class OnSendContentChange(val content: SendMessageContent) : ChatAction
    data object OnSendClick : ChatAction

    data object OnConfirmEditClick : ChatAction
    data object OnCancelReply : ChatAction

    data class OnImagesSelected(val results: List<GalleryPhotoResult>) : ChatAction
    data class OnCreatePoll(val poll: NetworkUtils.PollCreateRequest) : ChatAction

    data object OnStartRecording : ChatAction
    data object OnStopRecording : ChatAction
    data object OnDiscardRecording : ChatAction

    // Message-level actions (reply/react/edit/delete/copy/details/poll/audio playback/...),
    // unchanged - see MessageAction. Wrapped so composables only need one onAction callback.
    data class OnMessageAction(val action: MessageAction) : ChatAction
}
