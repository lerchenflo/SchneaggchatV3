package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter

/**
 * The chat list behind the chat selector: every chat with its counters and last message, message
 * search, pending friend requests, the unread badge and pinned chats. Reads come from the local
 * database only.
 */
interface ChatListRepository {

    /**
     * Every chat of [userId], unfiltered and sorted (pinned first, then today's birthdays, then by
     * last message). Search and filter run on the emitted list via [applySearchAndFilter].
     */
    fun getChatListFlow(userId: String): Flow<List<ChatListItem>>

    /** [getChatListFlow] with [applySearchAndFilter] applied to every emission. */
    fun getChatSelectorFlow(
        searchTerm: String,
        userId: String,
        filter: ChatFilter = ChatFilter.NONE
    ): Flow<List<ChatListItem>>

    /**
     * Messages whose text matches [searchTerm], newest first and capped. Emits an empty list for
     * terms that are too short.
     */
    fun getMessageSearchFlow(
        searchTerm: String,
        userId: String
    ): Flow<List<MessageSearchResult>>

    /** Users with a pending friend request (either direction) whose name matches [searchTerm]. */
    fun getPendingFriends(searchTerm: String): Flow<List<ChatListItem>>

    /** Chats with unread messages, for the chat tab's nav bar badge. */
    fun getUnreadChatCountFlow(): Flow<Int>

    /** Pins or unpins a chat locally, then pushes the whole resulting list to the server. */
    suspend fun setPinnedChat(chatId: String, group: Boolean, pinned: Boolean)
}
