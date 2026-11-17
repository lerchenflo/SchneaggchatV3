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
    EVENT_START_CHANGED,    // the event's start moved, new start in [SystemEventMessage.timestamp]
    EVENT_LOCATION_CHANGED, // the event's location was set, changed or removed
    EVENT_CANCELLED,        // the event was deleted while its group chat stays
    EVENT_REMINDER,         // a reminder went out: leadMinutes before the start in [SystemEventMessage.timestamp]
    GROUP_EXTENSION_CREATED, // text = title
    GROUP_EXTENSION_RENAMED, // text = new title, previousText = old title
    GROUP_EXTENSION_DELETED, // text = title
    FRIENDSHIP_ACCEPTED,
    WAKE_SENT,
    UNKNOWN,
}

@Serializable
data class SystemEventParticipant(
    val userId: String,
)

/**
 * Structured payload embedded on a [MessageType.SYSTEM] [Message]. The server does not render a
 * sentence - it hands the client [eventType] plus user ids, and the client builds the localized
 * string from its own resources and the names it resolves for the ids (see `systemEventText()`).
 */
@Serializable
data class SystemEventMessage(
    val eventType: SystemEventType,
    val actorId: String,
    val targets: List<SystemEventParticipant> = emptyList(),
    val text: String? = null,          // new group name, wake reason, extension title
    val previousText: String? = null,  // old group name or extension title (the *_RENAMED / _CHANGED types)
    val timestamp: Long? = null,       // event start in epoch ms (EVENT_START_CHANGED, EVENT_REMINDER)
    val leadMinutes: Int? = null,      // how long before the start the reminder went out (EVENT_REMINDER only)
) {
    /** Every user id the sentence names: the actor and the targets. */
    fun userIds(): List<String> = listOf(actorId) + targets.map { it.userId }
}
