package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

/**
 * Per-chat message counters, aggregated in SQL instead of by loading every message into memory.
 * [chatId] is the group id for group chats and the other participant's user id for direct chats.
 */
data class ChatAggregateDto(
    val chatId: String,
    val isGroup: Boolean,
    val unreadCount: Int,
    val unsentCount: Int,
)
