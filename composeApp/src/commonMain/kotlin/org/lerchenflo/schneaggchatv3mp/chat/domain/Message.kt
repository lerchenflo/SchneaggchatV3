package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageDto
import org.lerchenflo.schneaggchatv3mp.datasource.network.PICTUREMESSAGE

data class Message(
    var localPK: Long = 0L,
    var id: String? = null,
    var msgType: String = "",
    var content: String = "",
    var senderId: String,
    var receiverId: String,
    var sendDate: String = "",
    var changeDate: String = "",
    var deleted: Boolean = false,
    var groupMessage: Boolean = false,
    var answerId: String? = null,
    var sent: Boolean = false,
    var senderAsString: String = "",
    var senderColor: Int = 0
) {
    fun isPicture(): Boolean = msgType == PICTUREMESSAGE

    fun getSendDateAsLong(): Long = sendDate.toLongOrNull() ?: 0L

    fun isMyMessage(): Boolean {
        return try {
            senderId.toString() == SessionCache.getOwnIdValue().toString()
        } catch (_: Exception) {
            false
        }
    }
}


fun MessageDto.toMessage(): Message = Message(
    localPK = this.localPK,
    id = this.id,
    msgType = this.msgType,
    content = this.content,
    senderId = this.senderId,
    receiverId = this.receiverId,
    sendDate = this.sendDate,
    changeDate = this.changeDate,
    deleted = this.deleted,
    groupMessage = this.groupMessage,
    answerId = this.answerId,
    sent = this.sent,
    senderAsString = this.senderAsString,
    senderColor = this.senderColor
)

/** Domain -> DTO */
fun Message.toDto(): MessageDto = MessageDto(
    localPK = this.localPK,
    id = this.id,
    msgType = this.msgType,
    content = this.content,
    senderId = this.senderId,
    receiverId = this.receiverId,
    sendDate = this.sendDate,
    changeDate = this.changeDate,
    deleted = this.deleted,
    groupMessage = this.groupMessage,
    answerId = this.answerId,
    sent = this.sent,
)