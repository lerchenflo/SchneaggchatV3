package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * A single message matching the chat selector search term, flattened for display.
 *
 * [chatId]/[isGroup] address the chat the message lives in, so tapping a result can navigate
 * straight to it, and [messageId] is the message to scroll to and highlight once there.
 */
data class MessageSearchResult(
    val messageId: String,
    val chatId: String,
    val isGroup: Boolean,
    val chatName: String,
    val chatProfilePictureUrl: String,
    val senderName: String,
    val preview: String,
    val sendDate: Long,
)
