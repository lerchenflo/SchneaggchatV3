package org.lerchenflo.schneaggchatv3mp.chat.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.chat.domain.SystemEventMessage
import org.lerchenflo.schneaggchatv3mp.chat.domain.SystemEventParticipant
import org.lerchenflo.schneaggchatv3mp.chat.domain.SystemEventType
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.SystemEventParticipantResponse
import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.SystemEventResponse

fun SystemEventResponse.toSystemEventMessage(): SystemEventMessage {
    return SystemEventMessage(
        // Forward-compat: an eventType this build doesn't know yet (added server-side after this
        // client shipped) must not throw - it degrades to UNKNOWN so the message (and, over
        // /messages/sync, the whole page) still decodes.
        eventType = runCatching { SystemEventType.valueOf(this.eventType) }.getOrDefault(SystemEventType.UNKNOWN),
        actorId = this.actorId,
        actorName = this.actorName,
        targets = this.targets.map { it.toSystemEventParticipant() },
        text = this.text,
        previousText = this.previousText,
    )
}

fun SystemEventParticipantResponse.toSystemEventParticipant(): SystemEventParticipant {
    return SystemEventParticipant(
        userId = this.userId,
        userName = this.userName,
    )
}
