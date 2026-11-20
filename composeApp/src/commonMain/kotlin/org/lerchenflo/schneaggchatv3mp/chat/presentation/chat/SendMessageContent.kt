package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import androidx.compose.ui.text.input.TextFieldValue
import io.github.lerchenflo.voicemessages.VoiceRecordingState

/**
 * The content currently staged in the chat's input bar - plain text, a set of images with an
 * optional caption, or an in-progress / recorded voice message. Top-level (not nested in
 * ChatViewModel) so the pure-UI input composables under uielements/ don't need a ViewModel
 * import just to reference the type.
 */
sealed class SendMessageContent {
    data class TextContent(val textMessage: TextFieldValue) : SendMessageContent()
    data class ImageContent(val images: List<ByteArray>, var text: TextFieldValue) : SendMessageContent()
    /** A running ([VoiceRecordingState.Recording]) or finished ([VoiceRecordingState.Recorded]) voice message, never Idle. */
    data class AudioContent(val recording: VoiceRecordingState) : SendMessageContent()
}
