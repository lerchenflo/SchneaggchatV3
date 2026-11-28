package org.lerchenflo.schneaggchatv3mp.events.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.datasource.network.dto.EventResponse
import org.lerchenflo.schneaggchatv3mp.events.domain.Event

// ─── Event ───────────────────────────────────────────────────────────────────

fun EventResponse.toEvent(): Event = Event(
    id = id,
    creatorId = creatorId,
    type = type,
    title = title,
    description = description,
    groupId = groupId,
    location = location,
    startDate = startDate,
    closeDate = closeDate,
    invitedUsers = invitedUsers,
    reminderMinutes = reminderMinutes,
    participations = participations,
    visibility = visibility,
    maxUsers = maxUsers,
    entryFeeCents = entryFeeCents,
    groupDeleteDelay = groupDeleteDelay,
    createdAt = createdAt,
    updatedAt = updatedAt,
    updatedBy = updatedBy
)
