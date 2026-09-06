package org.lerchenflo.schneaggchatv3mp.chat.data.dtos

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageMinimal
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType
import org.lerchenflo.schneaggchatv3mp.chat.domain.PollMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.Reaction
import org.lerchenflo.schneaggchatv3mp.chat.domain.SystemEventMessage

@Serializable
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["id"], unique = true),
        // Both arms of the "(senderId = :id OR receiverId = :id) AND groupMessage = :group" chat
        // lookup, so SQLite's OR-optimization can seek each arm instead of scanning.
        Index(value = ["senderId", "groupMessage"]),
        Index(value = ["receiverId", "groupMessage"]),
        // Covers MessageDao.getChatAggregatesFlow, getLastMessagePerChatFlow and
        // getUnreadChatCountFlow completely: all three re-run on every message change and would
        // otherwise scan whole rows including content/poll/reaction payloads. The leading three
        // columns are exactly getUnreadChatCountFlow's equalities, and receiverId follows them so
        // its group-message arm groups without a sort.
        Index(value = ["readByMe", "groupMessage", "myMessage", "receiverId", "senderId", "msgType", "sent", "sendDate"]),
        Index(value = ["sent"]),
        // MAX(version) on every sync, an index seek instead of a table scan.
        Index(value = ["version"]),
    ]
)
data class MessageDto(

    @PrimaryKey(autoGenerate = true)
    var localPK : Long = 0L,

    var id: String? = null,

    var msgType: MessageType,

    var content: String = "",
    var poll: PollMessage? = null,
    var systemEvent: SystemEventMessage? = null,
    var pictureUrl: String? = null,
    var audioPath: String? = null,

    var senderId: String,

    var receiverId: String,

    var sendDate: String = "",

    var updatedAt: String = "",

    var deleted: Boolean = false,

    var myMessage: Boolean, //Keep track if this is a message sent by me or not
    var readByMe: Boolean,


    var groupMessage: Boolean = false,

    var answerId: String? = null,

    var sent: Boolean = false,

    var reactions: List<Reaction> = emptyList(),

    var version: Long = 0L,

    // Send idempotency key, generated once when the message is first queued and reused on
    // every retry (offline resend, lost response) so the server can dedup a retried send
    // instead of creating a second message. Never set on a message we received.
    var clientMessageId: String? = null,

    ) {
    @Ignore
    var senderAsString: String = ""

    @Ignore
    var minimizeMessage: MessageMinimal = MessageMinimal.NONE

    @Ignore
    var senderColor: Int = 0

}
