package org.lerchenflo.schneaggchatv3mp.chat.domain

/**
 * Sealed class representing different types of items that can be displayed in the chat screen.
 * This allows efficient mixing of messages and date dividers in a single LazyColumn.
 */
sealed class MessageDisplayItem {
    abstract val id: String
    
    /**
     * A message item with pre-resolved sender information.
     * Sender name and color are computed once in the flow transformation
     * to avoid repeated lookups during UI recomposition.
     */
    data class MessageItem(
        override val id: String,
        val message: Message,
        val senderName: String?,  // Pre-resolved from User
        val senderColor: Int,      // Pre-calculated for group messages
        val resolvedReaders: Map<String, String> = emptyMap() // readerId -> readerName, pre-resolved
    ) : MessageDisplayItem()
    
    /**
     * A date divider to separate messages by day.
     * Date string is pre-formatted to avoid repeated date parsing in UI.
     */
    data class DateDivider(
        override val id: String,  // e.g., "divider_2023-12-25"
        val dateMillis: Long,
        val dateString: String     // Pre-formatted date string
    ) : MessageDisplayItem()
}
