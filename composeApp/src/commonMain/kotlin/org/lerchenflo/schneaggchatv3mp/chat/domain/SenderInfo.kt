package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.compose.runtime.Immutable

/**
 * A message's resolved sender display name + per-member color, bundled together since every
 * caller that needs one needs the other. Resolved once by MessageDisplayMapper and threaded as a
 * single value instead of a (name, color) parameter pair through MessageContent, MessageView,
 * MessageViewWithActions, RepliedMessagePreview, ReplyPreview, DeleteMessageAlert and
 * MessageDetailsDialog.
 */
@Immutable
data class SenderInfo(
    val name: String?,
    val color: Int = 0,
)
