package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageSearchResult
import org.lerchenflo.schneaggchatv3mp.chat.domain.applySearchAndFilter
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter

/**
 * In-memory [ChatListRepository] serving [chats] as the already sorted chat list. Search and
 * filter use the real [applySearchAndFilter], like the real repository.
 *
 * [setPinnedChat] is recorded and also updates the chat's `pinned` in [chats] (1 when pinned,
 * 0 when not), so a test can toggle a pin twice.
 */
class FakeChatListRepository : ChatListRepository {

    data class SelectorRequest(val searchTerm: String, val userId: String, val filter: ChatFilter)
    data class SearchRequest(val searchTerm: String, val userId: String)
    data class PinCall(val chatId: String, val group: Boolean, val pinned: Boolean)

    val chats = MutableStateFlow<List<ChatListItem>>(emptyList())

    /** Users with a pending friend request (either direction), served by [getPendingFriends]. */
    val pendingFriends = MutableStateFlow<List<ChatListItem>>(emptyList())

    /** What [getMessageSearchFlow] serves, whatever the term. */
    val messageSearchResults = MutableStateFlow<List<MessageSearchResult>>(emptyList())

    val unreadChatCount = MutableStateFlow(0)

    val chatListRequests = CallLog<String>()
    val selectorRequests = CallLog<SelectorRequest>()
    val messageSearchRequests = CallLog<SearchRequest>()
    val pendingFriendRequests = CallLog<String>()
    val pinCalls = CallLog<PinCall>()

    override fun getChatListFlow(userId: String): Flow<List<ChatListItem>> {
        chatListRequests.record(userId)
        return chats
    }

    override fun getChatSelectorFlow(searchTerm: String, userId: String, filter: ChatFilter): Flow<List<ChatListItem>> {
        selectorRequests.record(SelectorRequest(searchTerm, userId, filter))
        return chats.map { it.applySearchAndFilter(searchTerm, filter) }
    }

    override fun getMessageSearchFlow(searchTerm: String, userId: String): Flow<List<MessageSearchResult>> {
        messageSearchRequests.record(SearchRequest(searchTerm, userId))
        return messageSearchResults
    }

    override fun getPendingFriends(searchTerm: String): Flow<List<ChatListItem>> {
        pendingFriendRequests.record(searchTerm)
        return pendingFriends.map { list -> list.filter { it.name.contains(searchTerm, ignoreCase = true) } }
    }

    override fun getUnreadChatCountFlow(): Flow<Int> = unreadChatCount

    override suspend fun setPinnedChat(chatId: String, group: Boolean, pinned: Boolean) {
        pinCalls.record(PinCall(chatId, group, pinned))
        chats.update { list ->
            list.map { chat ->
                if (chat.id == chatId && chat.isGroup == group) chat.copy(pinned = if (pinned) 1L else 0L) else chat
            }
        }
    }
}
