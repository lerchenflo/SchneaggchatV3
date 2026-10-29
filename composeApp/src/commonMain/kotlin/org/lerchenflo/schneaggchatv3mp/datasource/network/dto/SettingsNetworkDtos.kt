package org.lerchenflo.schneaggchatv3mp.datasource.network.dto

import kotlinx.serialization.Serializable
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.PinnedChat

/**
 * Per-user app settings, synced across every device of this account. Mirrors the server's
 * PersonalUserSettings 1:1 - theme/language/mapStyle are sent as the enum's [Enum.name], since
 * the server treats them as opaque strings and never interprets them.
 */
@Serializable
data class PersonalUserSettings(
    val mdFormat: Boolean = true,
    val highlightTodaysMessageTimestamp: Boolean = false,

    val theme: String = "SYSTEM",
    val language: String = "SYSTEM",

    val mergeMapLocations: Boolean = true,
    val mergeMapUsers: Boolean = true,
    val mapStyle: String = "LIBERTY",

    val pinnedChats: List<PinnedChat> = emptyList(),

    // Quick reaction emojis for the message long-press popup. Null means "never customised",
    // so the client falls back to DEFAULT_QUICK_REACTIONS and keeps following that set as it
    // changes; an empty list is a deliberate "show none" and is stored as such.
    val quickReactions: List<String>? = null,

    val developerSettings: Boolean = false,

    // Epoch millis of the last time the contribute popup was shown on any device.
    // 0 means never seeded - the server backfills this at registration and via migration.
    val lastContributePopupShown: Long = 0L,

    // Number of the newest announcement dismissed on any device - dismissing one also covers
    // every older announcement. 0 means none dismissed yet.
    val lastDismissedAnnouncementId: Long = 0L,
)

/**
 * Partial update for [PersonalUserSettings] - every field nullable, a null means "leave
 * unchanged" on the server, so only the fields that actually changed need to be sent.
 */
@Serializable
data class UserSettingsRequest(
    val mdFormat: Boolean? = null,
    val highlightTodaysMessageTimestamp: Boolean? = null,
    val theme: String? = null,
    val language: String? = null,
    val mergeMapLocations: Boolean? = null,
    val mergeMapUsers: Boolean? = null,
    val mapStyle: String? = null,
    val pinnedChats: List<PinnedChat>? = null,
    val quickReactions: List<String>? = null,
    // Null already means "leave unchanged" here, so resetting the list back to the app
    // defaults (a stored null) needs its own flag.
    val resetQuickReactions: Boolean? = null,
    val developerSettings: Boolean? = null,
    val lastContributePopupShown: Long? = null,
    // The server keeps the higher of its stored and this value
    val lastDismissedAnnouncementId: Long? = null,
)
