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
    /** [fullQuality]: send the originals instead of the small copies; off for every new pick. */
    data class ImageContent(
        val images: List<StagedImage>,
        var text: TextFieldValue,
        val fullQuality: Boolean = false,
    ) : SendMessageContent()
    /** A running ([VoiceRecordingState.Recording]) or finished ([VoiceRecordingState.Recorded]) voice message, never Idle. */
    data class AudioContent(val recording: VoiceRecordingState) : SendMessageContent()
}

/**
 * One image staged for sending: [original] as it was picked or shared (full quality, metadata
 * kept) and [compressed], the small copy that is sent by default and shown in the preview.
 */
class StagedImage(val original: ByteArray, val compressed: ByteArray)
