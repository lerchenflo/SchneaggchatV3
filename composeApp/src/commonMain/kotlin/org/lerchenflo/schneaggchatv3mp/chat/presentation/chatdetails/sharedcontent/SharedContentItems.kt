package org.lerchenflo.schneaggchatv3mp.chat.presentation.chatdetails.sharedcontent

import androidx.compose.runtime.Immutable

/**
 * One image shared in the chat, flattened for the grid. [messageId] is null for a message that has
 * not reached the server yet - such a row can still be shown, it just can not be jumped to.
 */
@Immutable
data class SharedImageItem(
    val messageId: String?,
    val pictureUrl: String,
    val caption: String,
    val senderName: String,
    val sendDate: Long,
)

/**
 * One url shared in the chat. A message carrying several links produces one item per link, so
 * [messageId] is not unique across the list - see [SharedContentState.links] for the list key.
 */
@Immutable
data class SharedLinkItem(
    val messageId: String?,
    val url: String,
    val senderName: String,
    val sendDate: Long,
)
