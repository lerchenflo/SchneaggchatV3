package org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto

import org.lerchenflo.schneaggchatv3mp.announcement.domain.Announcement
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementDraft
import org.lerchenflo.schneaggchatv3mp.announcement.domain.AnnouncementText

fun AnnouncementTextDto.toAnnouncementText(): AnnouncementText = AnnouncementText(title = title, body = body)

fun AnnouncementText.toAnnouncementTextDto(): AnnouncementTextDto = AnnouncementTextDto(title = title, body = body)

fun AnnouncementResponse.toAnnouncement(): Announcement = Announcement(
    id = id,
    german = german.toAnnouncementText(),
    vori = austrian?.toAnnouncementText(),
    english = english?.toAnnouncementText(),
    italian = italian?.toAnnouncementText(),
    expiresAt = expiresAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    creatorName = creatorName,
)

/** Trims every text and drops a translation without a title. */
fun AnnouncementDraft.toAnnouncementRequest(id: Long? = null): AnnouncementRequest = AnnouncementRequest(
    id = id,
    german = german.trimmed().toAnnouncementTextDto(),
    austrian = vori.translationOrNull(),
    english = english.translationOrNull(),
    italian = italian.translationOrNull(),
    expiresAt = expiresAt,
)

private fun AnnouncementText.trimmed(): AnnouncementText = AnnouncementText(title = title.trim(), body = body.trim())

private fun AnnouncementText?.translationOrNull(): AnnouncementTextDto? =
    this?.trimmed()?.takeIf { it.title.isNotEmpty() }?.toAnnouncementTextDto()
