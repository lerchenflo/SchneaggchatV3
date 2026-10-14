package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.presentation.chatselector.ChatFilter

/**
 * Search and filter an already built chat list. Runs in memory so typing in the chat selector never
 * re-subscribes to the database.
 */
fun List<ChatListItem>.applySearchAndFilter(
    searchTerm: String,
    filter: ChatFilter
): List<ChatListItem> {
    val loweredSearch = searchTerm.trim().lowercase()

    return asSequence()
        .filter {
            loweredSearch.isEmpty()
                    || it.name.lowercase().contains(loweredSearch)
                    || it.nickName?.lowercase()?.contains(loweredSearch) == true
        }
        .filter {
            when (filter) {
                ChatFilter.NONE -> true
                ChatFilter.UNREAD -> it.unreadMessageCount > 0
                ChatFilter.GROUPS -> it.isGroup
                ChatFilter.PERSONS -> !it.isGroup
            }
        }
        .toList()
}
