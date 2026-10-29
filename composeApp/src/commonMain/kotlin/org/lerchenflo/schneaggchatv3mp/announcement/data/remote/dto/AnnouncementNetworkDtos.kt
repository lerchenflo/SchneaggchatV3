package org.lerchenflo.schneaggchatv3mp.announcement.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementTextDto(
    val title: String,
    val body: String = "",
)

/** Field names follow the server (`austrian` is the app's Vori). Times are epoch millis. */
@Serializable
data class AnnouncementResponse(
    val id: Long,
    val german: AnnouncementTextDto,
    val austrian: AnnouncementTextDto? = null,
    val english: AnnouncementTextDto? = null,
    val italian: AnnouncementTextDto? = null,
    val expiresAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    // Only filled in the admin list
    val creatorName: String? = null,
)

/** "Nothing to show" is a normal reply with a null, not an empty body. */
@Serializable
data class LatestAnnouncementResponse(
    val announcement: AnnouncementResponse? = null,
)

/** Create ([id] null) or edit ([id] set). */
@Serializable
data class AnnouncementRequest(
    val id: Long? = null,
    val german: AnnouncementTextDto,
    val austrian: AnnouncementTextDto? = null,
    val english: AnnouncementTextDto? = null,
    val italian: AnnouncementTextDto? = null,
    val expiresAt: Long,
)
