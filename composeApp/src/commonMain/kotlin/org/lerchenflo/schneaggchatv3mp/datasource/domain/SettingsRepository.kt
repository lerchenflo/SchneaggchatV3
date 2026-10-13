package org.lerchenflo.schneaggchatv3mp.datasource.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventsViewMode
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.SyncedSettings
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.ThemeSetting

/**
 * App preferences. The `set*` functions change a setting that is synced across every device of
 * this account: they write the local preference first, then push the change to the server and
 * ignore the result. `save*` functions only change this device.
 */
interface SettingsRepository {

    // ─── Server URL (this device only) ─────────────────────────────────────────
    fun getServerUrlFlow(): Flow<String>
    suspend fun getServerUrl(): String
    suspend fun saveServerUrl(url: String)

    // ─── Synced personal settings ──────────────────────────────────────────────
    /**
     * Applies the server's settings onto the local preferences, overwriting whatever was there,
     * and switches the language if it changed.
     */
    suspend fun applyServerSettings(settings: SyncedSettings)

    fun getUseMdFlow(): Flow<Boolean>
    suspend fun setUseMd(value: Boolean)

    fun getDevSettingsFlow(): Flow<Boolean>
    suspend fun setDevSettings(value: Boolean)

    fun getHighlightTodaysMessageTimestampFlow(): Flow<Boolean>
    suspend fun setHighlightTodaysMessageTimestamp(value: Boolean)

    fun getThemeFlow(): Flow<ThemeSetting>
    suspend fun setThemeSetting(theme: ThemeSetting)

    suspend fun setLanguageSetting(language: LanguageSetting)

    fun getMergeMapLocationsFlow(): Flow<Boolean>
    suspend fun setMergeMapLocations(value: Boolean)

    fun getMergeMapUsersFlow(): Flow<Boolean>
    suspend fun setMergeMapUsers(value: Boolean)

    fun getMapStyleSettingFlow(): Flow<MapStyleSetting>
    fun getMapStyleUrlFlow(): Flow<String>
    suspend fun setMapStyleSetting(style: MapStyleSetting)

    fun getQuickReactionsFlow(): Flow<List<String>>
    suspend fun setQuickReactions(reactions: List<String>)
    suspend fun resetQuickReactions()

    suspend fun getLastContributePopupShown(): Long
    suspend fun setLastContributePopupShown(epochMillis: Long)

    // ─── This device only ──────────────────────────────────────────────────────
    fun getDemoModeFlow(): Flow<Boolean>
    suspend fun saveDemoMode(value: Boolean)

    suspend fun setOnboardingSeen(value: Boolean)

    suspend fun getLastStartedVersion(): String
    suspend fun saveLastStartedVersion(value: String)

    fun getEventsViewModeFlow(): Flow<EventsViewMode>
    suspend fun saveEventsViewMode(mode: EventsViewMode)

    fun getDraft(chatId: String, group: Boolean): Flow<String?>
    suspend fun saveDraft(chatId: String, group: Boolean, string: String)
}
