package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.Message
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageDisplayItem
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageMinimal
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageReader
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.ReaderUi
import org.lerchenflo.schneaggchatv3mp.chat.domain.SenderInfo
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TIME_TO_MINIMIZE = 30_000L //Time between messages to remove name and space between messages (in ms)

/**
 * Turns the raw message/sender-name/group-member lists into the list the chat's LazyColumn
 * renders.
 *
 * Kept as a class (not a plain function) so [picturePathFor] can cache a reader's profile-picture
 * path across emissions - it's a pure function of the user id, so recomputing it on every single
 * emission (as the old ChatViewModel.processMessages did) was wasted work.
 *
 * Never mutates Message or MessageReader - both are safe to treat as immutable as long as
 * nothing on this path writes to them. That, together with @Immutable on MessageDisplayItem, is
 * what lets Compose skip recomposing a row whose resolved content didn't change; no separate
 * instance-reuse cache is needed here for that - structural equality already gets it.
 */
class MessageDisplayMapper(
    private val getProfilePicFilePath: (userId: String) -> String,
) {
    private val profilePicturePathCache = mutableMapOf<String, String>()

    private fun picturePathFor(userId: String): String =
        profilePicturePathCache.getOrPut(userId) { getProfilePicFilePath(userId) }

    fun map(
        messages: List<Message>,
        senderNames: Map<String, String>,
        groupMembers: List<GroupMember>,
        newMessagesBoundaryId: String?,
    ): List<MessageDisplayItem> {
        val groupMap = groupMembers.associateBy { it.userId }

        fun resolveName(userId: String) = senderNames[userId] ?: groupMap[userId]?.memberName ?: "Unknown"

        // Find the LATEST message each user has read, so their avatar is placed only there
        // (not repeated on every earlier message they've also read).
        val latestReadMap = mutableMapOf<String, MessageReader>()
        messages.forEach { message ->
            message.readers.forEach { reader ->
                val existing = latestReadMap[reader.readerId]
                if (existing == null || reader.getReadDateAsLong() > existing.getReadDateAsLong()) {
                    latestReadMap[reader.readerId] = reader
                }
            }
        }
        val readersToDisplayPerMessage = latestReadMap.values.groupBy { it.messageId }

        val displayItems = mutableListOf<MessageDisplayItem>()

        messages.forEachIndexed { index, message ->
            val currentDateLong = message.sendDate.toLongOrNull() ?: 0
            val currentDate = message.sendDate.toLongOrNull()?.toLocalDate()
            val nextDate = if (index + 1 in messages.indices) {
                messages[index + 1].sendDate.toLongOrNull()?.toLocalDate()
            } else null

            // System messages are server-authored event lines, not chat bubbles - senderId is
            // often a group id (see SystemEventMessage KDoc), so none of the sender/color/reader/
            // reaction resolution below is meaningful. Emit a standalone display item instead and
            // skip straight to the shared new-messages/date-divider bookkeeping.
            if (message.msgType == MessageType.SYSTEM) {
                val event = message.systemEvent
                if (event != null) {
                    displayItems.add(
                        MessageDisplayItem.SystemMessage(
                            id = "sys_${message.localPK}",
                            event = event
                        )
                    )
                }

                if (newMessagesBoundaryId != null && message.id == newMessagesBoundaryId) {
                    displayItems.add(MessageDisplayItem.NewMessagesDivider)
                }

                if (currentDate != nextDate && currentDate != null) {
                    displayItems.add(
                        MessageDisplayItem.DateDivider(
                            id = "divider_${currentDate}",
                            dateMillis = message.sendDate.toLong(),
                            dateString = formatDate(currentDate)
                        )
                    )
                }

                return@forEachIndexed
            }

            val sender = SenderInfo(
                name = resolveName(message.senderId),
                color = groupMap[message.senderId]?.color ?: 0
            )

            val resolvedReaders = message.readers.associate { reader ->
                reader.readerId to resolveName(reader.readerId)
            }

            val resolvedReaderList = message.readers.map { reader ->
                ReaderUi(
                    id = reader.readerId,
                    name = resolveName(reader.readerId),
                    picturePath = picturePathFor(reader.readerId),
                    readAtMillis = reader.getReadDateAsLong(),
                )
            }

            val resolvedReactions = message.reactions.associate { reaction ->
                reaction.userId to resolveName(reaction.userId)
            }

            // Use the message's unique ID to look up the grouped readers
            val readersAtThisMessage = readersToDisplayPerMessage[message.id]
            if (!readersAtThisMessage.isNullOrEmpty()) {
                val readerBarList = readersAtThisMessage.map { reader ->
                    ReaderUi(
                        id = reader.readerId,
                        name = resolveName(reader.readerId),
                        picturePath = picturePathFor(reader.readerId),
                        readAtMillis = reader.getReadDateAsLong(),
                    )
                }
                displayItems.add(
                    MessageDisplayItem.ReaderBar(
                        id = "readers_${message.id}",
                        readerList = readerBarList
                    )
                )
            }

            val hasPrevNeighbor = if (index + 1 in messages.indices) {
                val prevDate_ = messages[index + 1].sendDate.toLongOrNull() ?: 0
                val nextSenderId = messages[index + 1].senderId
                if (currentDateLong - prevDate_ in 0..TIME_TO_MINIMIZE && nextSenderId == message.senderId) {
                    true
                } else false
            } else false

            val hasNextNeighbor = if (index - 1 in messages.indices) {
                val nextDate_ = messages[index - 1].sendDate.toLongOrNull() ?: 0
                val nextSenderId = messages[index - 1].senderId
                if (nextDate_ - currentDateLong in 0..TIME_TO_MINIMIZE && nextSenderId == message.senderId) {
                    true
                } else false
            } else false

            val minimalState = when {
                hasPrevNeighbor && hasNextNeighbor -> MessageMinimal.MIDDLE
                hasPrevNeighbor -> MessageMinimal.LAST
                hasNextNeighbor -> MessageMinimal.FIRST
                else -> MessageMinimal.NONE
            }

            displayItems.add(
                MessageDisplayItem.MessageItem(
                    id = "msg_${message.localPK}",
                    message = message,
                    sender = sender,
                    messageMinimal = minimalState,
                    resolvedReaders = resolvedReaders,
                    resolvedReaderList = resolvedReaderList,
                    resolvedReactions = resolvedReactions
                )
            )

            if (newMessagesBoundaryId != null && message.id == newMessagesBoundaryId) {
                displayItems.add(MessageDisplayItem.NewMessagesDivider)
            }

            if (currentDate != nextDate && currentDate != null) {
                displayItems.add(
                    MessageDisplayItem.DateDivider(
                        id = "divider_${currentDate}",
                        dateMillis = message.sendDate.toLong(),
                        dateString = formatDate(currentDate)
                    )
                )
            }
        }

        return displayItems
    }

    private fun formatDate(date: LocalDate): String = "${date.day}.${date.month.ordinal}.${date.year}"

    @OptIn(ExperimentalTime::class)
    private fun Long.toLocalDate(): LocalDate {
        val instant = Instant.fromEpochMilliseconds(this)
        return instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
}
