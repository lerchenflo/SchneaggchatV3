package org.lerchenflo.schneaggchatv3mp.datasource.network.requestResponseDataClasses

import org.lerchenflo.schneaggchatv3mp.settings.domain.SyncedSettings

fun PersonalUserSettings.toSyncedSettings(): SyncedSettings = SyncedSettings(
    mdFormat = mdFormat,
    highlightTodaysMessageTimestamp = highlightTodaysMessageTimestamp,
    theme = theme,
    language = language,
    mergeMapLocations = mergeMapLocations,
    mergeMapUsers = mergeMapUsers,
    mapStyle = mapStyle,
    pinnedChats = pinnedChats,
    quickReactions = quickReactions,
    developerSettings = developerSettings,
    lastContributePopupShown = lastContributePopupShown,
)
