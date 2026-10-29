package org.lerchenflo.schneaggchatv3mp.announcement.domain

import org.lerchenflo.schneaggchatv3mp.settings.domain.LanguageSetting

/** Same limits the server enforces. */
object AnnouncementLimits {
    const val TITLE_MAX_LENGTH = 80
    const val BODY_MAX_LENGTH = 500
    const val MAX_EXPIRY_DAYS = 365
}

/** One language's text of an [Announcement]. [body] may be empty. */
data class AnnouncementText(
    val title: String,
    val body: String,
)

/**
 * A message from the admins to every user, shown as a banner in the chat list until it is
 * dismissed or [expiresAt] passes. [id] is a running number: a higher number is a newer
 * announcement, and dismissing one also covers every older one.
 */
data class Announcement(
    val id: Long,
    val german: AnnouncementText,
    val vori: AnnouncementText?,
    val english: AnnouncementText?,
    val italian: AnnouncementText?,
    /** Epoch millis */
    val expiresAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    /** Set only in the admin list. */
    val creatorName: String? = null,
) {
    /**
     * Text for [language], falling back when it has no translation: Vori to German, Italian to
     * English and then German, English to German. Resolve [LanguageSetting.SYSTEM] before calling,
     * it gets German.
     */
    fun textFor(language: LanguageSetting): AnnouncementText = when (language) {
        LanguageSetting.VORI -> vori ?: german
        LanguageSetting.ENGLISH -> english ?: german
        LanguageSetting.ITALIAN -> italian ?: english ?: german
        LanguageSetting.GERMAN, LanguageSetting.SYSTEM -> german
    }

    fun isExpired(nowMillis: Long): Boolean = nowMillis >= expiresAt
}

/** What an admin enters to create or edit an [Announcement]. Languages without a translation are null. */
data class AnnouncementDraft(
    val german: AnnouncementText,
    val vori: AnnouncementText?,
    val english: AnnouncementText?,
    val italian: AnnouncementText?,
    /** Epoch millis */
    val expiresAt: Long,
)
