package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.events.data.dtos.EventDto
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
    visibility = visibility,
    groupDeleteDelay = groupDeleteDelay,
    createdAt = createdAt,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    creatorName = creatorName
)

fun Event.toDto(): EventDto = EventDto(
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
    visibility = visibility,
    groupDeleteDelay = groupDeleteDelay,
    createdAt = createdAt,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    creatorName = creatorName
)

fun EventDto.toEvent(): Event = Event(
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
    visibility = visibility,
    groupDeleteDelay = groupDeleteDelay,
    createdAt = createdAt,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    creatorName = creatorName
)

fun EventResponse.toDto(): EventDto = toEvent().toDto()
