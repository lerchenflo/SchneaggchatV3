package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.utilities.annotations.ComboAnnotationTypes

/** True when this text has a raw `@user/<userId>` annotation (see [ComboAnnotationTypes.USER]). */
fun String.mentionsUser(userId: String): Boolean =
    ComboAnnotationTypes.USER.regex.findAll(this).any { it.groupValues[1] == userId }

/** True when the poll's title, description, an option or a sub poll (at any depth) mentions [userId]. */
fun PollMessage.mentionsUser(userId: String): Boolean =
    title.mentionsUser(userId) ||
        description?.mentionsUser(userId) == true ||
        voteOptions.any { it.text.mentionsUser(userId) || it.subPoll?.mentionsUser(userId) == true }

/**
 * True when this group message is about [userId]: it mentions them in its text or poll, or it
 * answers one of their messages ([repliedMessage]). Never in 1:1 chats, never for system lines or
 * the user's own messages (user decision 2026-11-21).
 */
fun Message.isAboutUser(userId: String, repliedMessage: Message?): Boolean {
    if (!groupMessage || deleted || senderId == userId || msgType == MessageType.SYSTEM) return false
    return content.mentionsUser(userId) ||
        poll?.mentionsUser(userId) == true ||
        repliedMessage?.senderId == userId
}
