package org.lerchenflo.schneaggchatv3mp.chat.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.ChatListRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageSearchResult
import org.lerchenflo.schneaggchatv3mp.chat.domain.applySearchAndFilter
import org.lerchenflo.schneaggchatv3mp.chat.domain.toChatListItem
import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.FriendshipStatus
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.PinnedChat
import org.lerchenflo.schneaggchatv3mp.utilities.getCurrentTimeMillisString
import org.lerchenflo.schneaggchatv3mp.utilities.isBirthdayToday
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.UserSettingsRequest
import org.lerchenflo.schneaggchatv3mp.settings.data.remote.SettingsRemoteDataSource

//Message search in the chat selector: how much the user has to type before the message table is
//scanned, and how many hits are kept so a common word cannot build a list of thousands of rows.
private const val MESSAGE_SEARCH_MIN_TERM_LENGTH = 2
private const val MESSAGE_SEARCH_RESULT_LIMIT = 50

/**
 * [ChatListRepository] on top of the local User/Group/Message data sources, with pinned chats from
 * [SettingsRepository]. The code is moved verbatim from AppRepository (getChatListFlow,
 * getChatSelectorFlow, getMessageSearchFlow, getPendingFriends, getUnreadChatCountFlow,
 * setPinnedChat).
 *
 * @param defaultDispatcher runs the in-memory list building (`flowOn`); tests pass their own.
 */
class OfflineFirstChatListRepository(
    private val userLocalDataSource: UserLocalDataSource,
    private val groupLocalDataSource: GroupLocalDataSource,
    private val messageLocalDataSource: MessageLocalDataSource,
    private val settingsRepository: SettingsRepository,
    private val settingsRemoteDataSource: SettingsRemoteDataSource,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ChatListRepository {

    override fun getPendingFriends(searchTerm: String): Flow<List<ChatListItem>> {
        return userLocalDataSource.getAllUsersFlow(searchTerm)
            .map { list ->
                list.filter {
                    it.friendshipStatus == FriendshipStatus.PENDING
                }.map { user ->
                    user.toChatListItem()
                }
            }
    }


    /** Chats with unread messages, for the chat tab's nav bar badge. */
    override fun getUnreadChatCountFlow(): Flow<Int> = messageLocalDataSource.getUnreadChatCountFlow()

    /**
     * Every chat of the logged in user, unfiltered and sorted, for the chat selector.
     *
     * Counters and last-message previews come straight out of SQL ([MessageLocalDataSource.getChatAggregatesFlow],
     * [MessageLocalDataSource.getLastMessagePerChatFlow]) - one row per chat instead of the whole
     * message table, so nothing here scales with the number of messages.
     *
     * Search and filter are deliberately not part of this flow: they run on the emitted list via
     * [applySearchAndFilter], so typing never re-subscribes to the database.
     */
    override fun getChatListFlow(userId: String): Flow<List<ChatListItem>> {
        val aggregatesFlow = messageLocalDataSource.getChatAggregatesFlow(userId)
        val lastMessagesFlow = messageLocalDataSource.getLastMessagePerChatFlow(userId)
        val usersFlow = userLocalDataSource.getAllUsersFlow()
        val groupsFlow = groupLocalDataSource.getAllGroupswithMembersFlow()
        val pinnedFlow = settingsRepository.getPinnedChatsFlow()

        return combine(
            aggregatesFlow,
            lastMessagesFlow,
            usersFlow,
            groupsFlow,
            pinnedFlow
        ) { aggregates, lastMessages, users, groups, pinnedList ->

            val userIdMap = users.associateBy { it.id }
            val pinnedMap = pinnedList.associate { it.chatId to it.pinTimePoint }
            val aggregateMap = aggregates.associateBy { it.chatId to it.isGroup }
            val lastMessageMap = lastMessages.associateBy { it.chatKey(userId) }

            val userItems = users
                .asSequence()
                .filter { it.id != userId }
                .filter { it.friendshipStatus == FriendshipStatus.ACCEPTED }
                .map { user ->
                    val aggregate = aggregateMap[user.id to false]
                    val last = lastMessageMap[user.id to false]?.let { message ->
                        message.copy(
                            senderAsString = userIdMap[message.senderId]?.name ?: message.senderAsString
                        )
                    }

                    user.toChatListItem(
                        unreadCount = aggregate?.unreadCount ?: 0,
                        unsentCount = aggregate?.unsentCount ?: 0,
                        lastMessage = last,
                        pinned = pinnedMap[user.id] ?: 0L
                    )
                }
                .toList()

            val groupItems = groups
                .asSequence()
                .map { group ->
                    val aggregate = aggregateMap[group.id to true]
                    val last = lastMessageMap[group.id to true]?.let { message ->
                        message.copy(
                            senderAsString = userIdMap[message.senderId]?.name ?: "Unknown"
                        )
                    }

                    group.toChatListItem(
                        unreadCount = aggregate?.unreadCount ?: 0,
                        unsentCount = aggregate?.unsentCount ?: 0,
                        lastMessage = last,
                        pinned = pinnedMap[group.id] ?: 0L
                    )
                }
                .toList()

            (userItems + groupItems).sortedForChatSelector()

        }.flowOn(defaultDispatcher)
    }

    /**
     * Get main screen available items as flow
     */
    override fun getChatSelectorFlow(
        searchTerm: String,
        userId: String,
        filter: ChatFilter
    ): Flow<List<ChatListItem>> {
        return getChatListFlow(userId).map { chats ->
            chats.applySearchAndFilter(searchTerm, filter)
        }
    }


    /**
     * Messages whose text matches [searchTerm], newest first, for the message section of the chat
     * selector search. Returns an empty flow for terms shorter than [MESSAGE_SEARCH_MIN_TERM_LENGTH]
     * so typing a single character never scans the whole message table.
     *
     * Matches message text (which doubles as the caption of image/audio messages) plus poll title,
     * description and vote options. Deleted messages are skipped - their content is a tombstone.
     */
    override fun getMessageSearchFlow(
        searchTerm: String,
        userId: String
    ): Flow<List<MessageSearchResult>> {
        val loweredSearch = searchTerm.trim().lowercase()
        if (loweredSearch.length < MESSAGE_SEARCH_MIN_TERM_LENGTH) return flowOf(emptyList())

        val messagesFlow = messageLocalDataSource.getAllMessages()
        val usersFlow = userLocalDataSource.getAllUsersFlow()
        val groupsFlow = groupLocalDataSource.getAllGroupswithMembersFlow()

        return combine(messagesFlow, usersFlow, groupsFlow) { messages, users, groups ->
            val userIdMap = users.associateBy { it.id }
            val groupIdMap = groups.associateBy { it.id }

            messages
                .asSequence()
                .filter { it.matchesSearchTerm(loweredSearch) }
                .sortedByDescending { it.getSendDateAsLong() }
                .take(MESSAGE_SEARCH_RESULT_LIMIT)
                .mapNotNull { message ->
                    val messageId = message.id ?: return@mapNotNull null

                    //Resolve which chat this message belongs to: for groups that is always the
                    //receiver, for direct messages it is whichever side is not us.
                    val chatId: String
                    val chatName: String
                    val chatPicture: String
                    if (message.isGroupMessage()) {
                        chatId = message.receiverId
                        val group = groupIdMap[chatId] ?: return@mapNotNull null
                        chatName = group.name
                        chatPicture = group.profilePictureUrl
                    } else {
                        chatId = if (message.senderId == userId) message.receiverId else message.senderId
                        val partner = userIdMap[chatId] ?: return@mapNotNull null
                        chatName = partner.nickName?.takeIf { it.isNotBlank() } ?: partner.name
                        chatPicture = partner.profilePictureUrl
                    }

                    MessageSearchResult(
                        messageId = messageId,
                        chatId = chatId,
                        isGroup = message.isGroupMessage(),
                        chatName = chatName,
                        chatProfilePictureUrl = chatPicture,
                        senderName = userIdMap[message.senderId]?.name
                            ?: message.senderAsString,
                        preview = message.searchPreview(),
                        sendDate = message.getSendDateAsLong(),
                    )
                }
                .toList()
        }.flowOn(defaultDispatcher)
    }

    /** True when any searchable text of this message contains the already lowercased [term]. */
    private fun Message.matchesSearchTerm(term: String): Boolean {
        if (deleted) return false
        if (content.lowercase().contains(term)) return true

        val poll = poll ?: return false
        return poll.title.lowercase().contains(term) ||
                poll.description?.lowercase()?.contains(term) == true ||
                poll.voteOptions.any { it.text.lowercase().contains(term) }
    }

    /** Text shown underneath a search result - the poll title stands in for polls without text. */
    private fun Message.searchPreview(): String =
        content.takeIf { it.isNotBlank() } ?: poll?.title.orEmpty()


    // Pinned chats follow the same push pattern as the settings in SettingsRepository: write the
    // local preference first, then fire the partial-update request and ignore its result.

    /** Pins or unpins a chat locally, then pushes the whole resulting list (server stores it wholesale). */
    override suspend fun setPinnedChat(chatId: String, group: Boolean, pinned: Boolean) {
        if (pinned) {
            settingsRepository.addPinnedChat(PinnedChat(chatId, group, getCurrentTimeMillisString().toLong()))
        } else {
            settingsRepository.removePinnedChat(chatId)
        }
        settingsRemoteDataSource.updateSettings(UserSettingsRequest(pinnedChats = settingsRepository.getPinnedChats())).trackConnectivity()
    }
}

/**
 * Which chat a message belongs to: the group for group messages, the other participant for direct
 * ones. Mirrors the PARTITION BY of MessageDao.getLastMessagePerChatFlow so both sides key alike.
 */
private fun Message.chatKey(ownId: String): Pair<String, Boolean> = when {
    groupMessage -> receiverId to true
    senderId == ownId -> receiverId to false
    else -> senderId to false
}

/**
 * Chat selector order: pinned chats first (newest pin on top), then today's birthdays, then
 * everything else by last message. Birthdays are resolved once per item instead of inside the
 * comparator, which would re-parse the date and re-read the clock on every comparison.
 */
private fun List<ChatListItem>.sortedForChatSelector(): List<ChatListItem> =
    map { ChatSortKey(chat = it, birthdayToday = isBirthdayToday(it.birthDate)) }
        .sortedWith { a, b ->
            val aPinned = a.chat.pinned > 0L
            val bPinned = b.chat.pinned > 0L
            when {
                aPinned && bPinned -> b.chat.pinned.compareTo(a.chat.pinned)
                aPinned -> -1
                bPinned -> 1

                a.birthdayToday && !b.birthdayToday -> -1
                b.birthdayToday && !a.birthdayToday -> 1

                else -> {
                    val timeA = a.chat.lastMessage?.getSendDateAsLong() ?: 0L
                    val timeB = b.chat.lastMessage?.getSendDateAsLong() ?: 0L
                    timeB.compareTo(timeA)
                }
            }
        }
        .map { it.chat }

private class ChatSortKey(val chat: ChatListItem, val birthdayToday: Boolean)
