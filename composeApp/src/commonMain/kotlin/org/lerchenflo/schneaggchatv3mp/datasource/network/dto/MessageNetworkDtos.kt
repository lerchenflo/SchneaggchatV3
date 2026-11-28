package org.lerchenflo.schneaggchatv3mp.datasource.network.dto

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.chat.domain.MessageType

@Serializable
data class MessageResponse(
    val messageId: String, //Objectid
    val senderId: String,
    val receiverId: String,
    val groupMessage: Boolean,
    val msgType: MessageType,

    val content: String,
    val pollResponse: PollResponse?,
    val systemEventResponse: SystemEventResponse? = null,

    val answerId: String?,

    val sendDate: Long,
    val lastChanged: Long,
    val deleted: Boolean,
    val edited: Boolean = false,
    val readers: List<ReaderResponse>,
    val reactions: List<ReactionResponse> = emptyList(),
    val version: Long = 0L,
    // Only non-null when this response is about our own sent message - lets sync/socket
    // reconciliation find a still-pending local row before the send's own response arrives.
    val clientMessageId: String? = null,
    // The server deleted the image / voice file (older than 60 days); the message itself stays.
    // Default for an older server that doesn't send it yet.
    val mediaExpired: Boolean = false,
)

@Serializable
data class ReaderResponse(
    val userId: String,
    val readAt: Long
)

@Serializable
data class ReactionResponse(
    val userId: String,
    val content: String,
    val reactedAt: Long = 0L,
)
