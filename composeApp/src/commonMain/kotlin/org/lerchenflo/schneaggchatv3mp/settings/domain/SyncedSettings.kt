package org.lerchenflo.schneaggchatv3mp.settings.domain

import org.lerchenflo.schneaggchatv3mp.datasource.preferences.PinnedChat

/**
 * The per-user app settings the server syncs across every device of this account, as applied by
 * [org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository.applyServerSettings].
 * Domain copy of the network DTO `NetworkUtils.PersonalUserSettings`, field for field:
 * theme/language/mapStyle stay the enum's name as a string, a null [quickReactions] means
 * "never customised" and [lastContributePopupShown] 0 means "never seeded".
 */
data class SyncedSettings(
    val mdFormat: Boolean,
    val highlightTodaysMessageTimestamp: Boolean,
    val theme: String,
    val language: String,
    val mergeMapLocations: Boolean,
    val mergeMapUsers: Boolean,
    val mapStyle: String,
    val pinnedChats: List<PinnedChat>,
    val quickReactions: List<String>?,
    val developerSettings: Boolean,
    val lastContributePopupShown: Long,
)
