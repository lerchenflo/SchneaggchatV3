package org.lerchenflo.schneaggchatv3mp.chat.domain

/** What a message to send carries. */
sealed class MessageContent {
    data class TextContent(val message: String) : MessageContent()
    data class ImageContent(val image: ByteArray, val text: String) : MessageContent()
    data class AudioContent(val audio: ByteArray) : MessageContent()

    data class PollContent(val poll: NewPoll) : MessageContent()
}
