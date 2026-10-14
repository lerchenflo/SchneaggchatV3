package org.lerchenflo.schneaggchatv3mp.chat.domain

import kotlinx.serialization.Serializable

/**
 * Kind of server-authored event a [MessageType.SYSTEM] message describes - the in-chat equivalent
 * of WhatsApp's "X joined the group" lines. Mirrors the server's
 * `com.lerchenflo.schneaggchatv3server.message.messagemodel.SystemEventType`.
 *
 * [UNKNOWN] is not sent by the server - it is the client-side fallback used when decoding an
 * eventType string the running build doesn't recognize yet, so an older client degrades to a
 * generic line instead of failing to decode the whole message (and, over `/messages/sync`, the
 * whole sync page).
 */
@Serializable
enum class SystemEventType {
    GROUP_CREATED,
    GROUP_MEMBER_ADDED,
    GROUP_MEMBER_JOINED_EVENT, // joined by themselves via an event, not added by an admin
    GROUP_MEMBER_REMOVED,   // kicked by an admin
    GROUP_MEMBER_LEFT,      // left voluntarily
    GROUP_ADMIN_GRANTED,
    GROUP_ADMIN_REVOKED,
    GROUP_NAME_CHANGED,
    GROUP_DESCRIPTION_CHANGED,
    GROUP_PICTURE_CHANGED,
    EVENT_CHANGED,      // an event's details changed - posted into the event's connected group chat
    FRIENDSHIP_ACCEPTED,
    WAKE_SENT,
    UNKNOWN,
}

@Serializable
data class SystemEventParticipant(
    val userId: String,
    val userName: String,
)

/**
 * Structured payload embedded on a [MessageType.SYSTEM] [Message]. The server does not render a
 * sentence - it hands the client [eventType] plus ids and name snapshots, and the client builds
 * the localized string from its own resources (see `systemEventText()`).
 */
@Serializable
data class SystemEventMessage(
    val eventType: SystemEventType,
    val actorId: String,
    val actorName: String,
    val targets: List<SystemEventParticipant> = emptyList(),
    val text: String? = null,          // new group name, wake reason
    val previousText: String? = null,  // old group name (GROUP_NAME_CHANGED only)
)
