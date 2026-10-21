package org.lerchenflo.schneaggchatv3mp.settings.domain

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.PinnedChat

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

    // ─── Announcements (synced) ────────────────────────────────────────────────
    // Number of the newest announcement dismissed on any device of this account. A dismissal
    // that fails to send is not retried - the next sync restores the server's number and the
    // announcement shows once more.
    fun getLastDismissedAnnouncementIdFlow(): Flow<Long>
    suspend fun setLastDismissedAnnouncementId(id: Long)

    // ─── Pinned chats (synced) ─────────────────────────────────────────────────
    // Local reads and writes only, newest pin first. ChatListRepository.setPinnedChat writes
    // here and then pushes the whole list to the server itself.
    fun getPinnedChatsFlow(): Flow<List<PinnedChat>>
    suspend fun getPinnedChats(): List<PinnedChat>
    suspend fun addPinnedChat(chat: PinnedChat)
    suspend fun removePinnedChat(chatId: String)

    // ─── This device only ──────────────────────────────────────────────────────
    fun getDemoModeFlow(): Flow<Boolean>
    suspend fun saveDemoMode(value: Boolean)

    fun getOnboardingSeenFlow(): Flow<Boolean>
    suspend fun setOnboardingSeen(value: Boolean)

    suspend fun getLastStartedVersion(): String
    suspend fun saveLastStartedVersion(value: String)

    fun getEventsViewModeFlow(): Flow<EventsViewMode>
    suspend fun saveEventsViewMode(mode: EventsViewMode)

    fun getDraft(chatId: String, group: Boolean): Flow<String?>
    /**
     * Stores the chat's unsent text. Fire and forget: runs in the app-wide scope, so it also
     * finishes when called while the chat screen is going away (ChatViewModel.onCleared).
     */
    fun saveDraft(chatId: String, group: Boolean, string: String)
}
