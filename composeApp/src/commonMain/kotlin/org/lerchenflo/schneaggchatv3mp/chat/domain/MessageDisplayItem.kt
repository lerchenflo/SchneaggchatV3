package org.lerchenflo.schneaggchatv3mp.chat.domain

import androidx.compose.runtime.Immutable

/**
 * Sealed class representing different types of items that can be displayed in the chat screen.
 * This allows efficient mixing of messages and date dividers in a single LazyColumn.
 *
 * All variants (and the [Message] they wrap) are safe to mark @Immutable: the mapper that builds
 * them (MessageDisplayMapper) never mutates a Message or MessageReader after construction. That
 * lets Compose skip recomposing a row whose item didn't change, instead of always treating it as
 * unstable because Message happens to declare its fields `var`.
 */
@Immutable
sealed class MessageDisplayItem {
    abstract val id: String

    /**
     * A message item with pre-resolved sender information.
     * Sender name and color are computed once in the flow transformation
     * to avoid repeated lookups during UI recomposition.
     */
    @Immutable
    data class MessageItem(
        override val id: String,
        val message: Message,
        val sender: SenderInfo,  // Pre-resolved name + per-member color
        val messageMinimal: MessageMinimal,
        val resolvedReaders: Map<String, String> = emptyMap(), // readerId -> readerName, pre-resolved
        val resolvedReaderList: List<ReaderUi> = emptyList(), // full per-message reader list (details dialog)
        val resolvedReactions: Map<String, String> = emptyMap() // userId -> userName, pre-resolved
    ) : MessageDisplayItem()

    /**
     * A date divider to separate messages by day.
     * Date string is pre-formatted to avoid repeated date parsing in UI.
     */
    @Immutable
    data class DateDivider(
        override val id: String,  // e.g., "divider_2023-12-25"
        val dateMillis: Long,
        val dateString: String     // Pre-formatted date string
    ) : MessageDisplayItem()

    @Immutable
    data class ReaderBar(
        override val id: String,
        val readerList: List<ReaderUi>
    ): MessageDisplayItem()

    /**
     * A server-authored WhatsApp-style event line (group renamed, member added, wake sent, ...).
     * Rendered as an inert centered pill - never a bubble, no sender row, no read receipts.
     */
    @Immutable
    data class SystemMessage(
        override val id: String,  // e.g. "sys_${localPK}"
        val event: SystemEventMessage,
    ) : MessageDisplayItem()

    data object NewMessagesDivider : MessageDisplayItem() {
        override val id: String = "new_messages_divider"
    }
}
