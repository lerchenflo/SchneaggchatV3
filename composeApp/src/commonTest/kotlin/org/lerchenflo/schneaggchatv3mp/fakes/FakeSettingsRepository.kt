package org.lerchenflo.schneaggchatv3mp.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.chat.domain.DEFAULT_QUICK_REACTIONS
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.settings.domain.EventsViewMode
import org.lerchenflo.schneaggchatv3mp.settings.domain.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.settings.domain.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.settings.domain.SyncedSettings
import org.lerchenflo.schneaggchatv3mp.settings.domain.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.PinnedChat

/**
 * In-memory [SettingsRepository]: every setting is a public `MutableStateFlow` a test can preset
 * or read back. Nothing is pushed to a server; [applyServerSettings] only records its argument.
 * Pinned chats keep the newest pin first, like `Preferencemanager`.
 */
class FakeSettingsRepository : SettingsRepository {

    val serverUrl = MutableStateFlow(BASE_SERVER_URL)
    val useMd = MutableStateFlow(false)
    val devSettings = MutableStateFlow(false)
    val highlightTodaysMessageTimestamp = MutableStateFlow(true)
    val theme = MutableStateFlow(ThemeSetting.SYSTEM)
    val language = MutableStateFlow(LanguageSetting.SYSTEM)
    val mergeMapLocations = MutableStateFlow(false)
    val mergeMapUsers = MutableStateFlow(false)
    val mapStyle = MutableStateFlow(MapStyleSetting.entries.first())
    val mapStyleUrl = MutableStateFlow("")
    val quickReactions = MutableStateFlow(DEFAULT_QUICK_REACTIONS)
    val lastContributePopupShown = MutableStateFlow(0L)
    val lastDismissedAnnouncementId = MutableStateFlow(0L)
    val pinnedChats = MutableStateFlow<List<PinnedChat>>(emptyList())
    val demoMode = MutableStateFlow(false)
    val onboardingSeen = MutableStateFlow(true)
    val lastStartedVersion = MutableStateFlow("")
    val eventsViewMode = MutableStateFlow(EventsViewMode.LIST)

    /** Drafts keyed by `chatId to isGroup`. */
    val drafts = MutableStateFlow<Map<Pair<String, Boolean>, String>>(emptyMap())

    val appliedServerSettings = CallLog<SyncedSettings>()

    // ─── Server URL ───────────────────────────────────────────────────────────

    override fun getServerUrlFlow(): Flow<String> = serverUrl
    override suspend fun getServerUrl(): String = serverUrl.value
    override suspend fun saveServerUrl(url: String) {
        serverUrl.value = url
    }

    // ─── Synced personal settings ─────────────────────────────────────────────

    override suspend fun applyServerSettings(settings: SyncedSettings) = appliedServerSettings.record(settings)

    override fun getUseMdFlow(): Flow<Boolean> = useMd
    override suspend fun setUseMd(value: Boolean) {
        useMd.value = value
    }

    override fun getDevSettingsFlow(): Flow<Boolean> = devSettings
    override suspend fun setDevSettings(value: Boolean) {
        devSettings.value = value
    }

    override fun getHighlightTodaysMessageTimestampFlow(): Flow<Boolean> = highlightTodaysMessageTimestamp
    override suspend fun setHighlightTodaysMessageTimestamp(value: Boolean) {
        highlightTodaysMessageTimestamp.value = value
    }

    override fun getThemeFlow(): Flow<ThemeSetting> = theme
    override suspend fun setThemeSetting(theme: ThemeSetting) {
        this.theme.value = theme
    }

    override suspend fun setLanguageSetting(language: LanguageSetting) {
        this.language.value = language
    }

    override fun getMergeMapLocationsFlow(): Flow<Boolean> = mergeMapLocations
    override suspend fun setMergeMapLocations(value: Boolean) {
        mergeMapLocations.value = value
    }

    override fun getMergeMapUsersFlow(): Flow<Boolean> = mergeMapUsers
    override suspend fun setMergeMapUsers(value: Boolean) {
        mergeMapUsers.value = value
    }

    override fun getMapStyleSettingFlow(): Flow<MapStyleSetting> = mapStyle
    override fun getMapStyleUrlFlow(): Flow<String> = mapStyleUrl
    override suspend fun setMapStyleSetting(style: MapStyleSetting) {
        mapStyle.value = style
    }

    override fun getQuickReactionsFlow(): Flow<List<String>> = quickReactions
    override suspend fun setQuickReactions(reactions: List<String>) {
        quickReactions.value = reactions
    }

    override suspend fun resetQuickReactions() {
        quickReactions.value = DEFAULT_QUICK_REACTIONS
    }

    override suspend fun getLastContributePopupShown(): Long = lastContributePopupShown.value
    override suspend fun setLastContributePopupShown(epochMillis: Long) {
        lastContributePopupShown.value = epochMillis
    }

    override fun getLastDismissedAnnouncementIdFlow(): Flow<Long> = lastDismissedAnnouncementId
    override suspend fun setLastDismissedAnnouncementId(id: Long) {
        lastDismissedAnnouncementId.value = id
    }

    // ─── Pinned chats ─────────────────────────────────────────────────────────

    override fun getPinnedChatsFlow(): Flow<List<PinnedChat>> = pinnedChats
    override suspend fun getPinnedChats(): List<PinnedChat> = pinnedChats.value

    override suspend fun addPinnedChat(chat: PinnedChat) {
        pinnedChats.update { list -> listOf(chat) + list.filterNot { it.chatId == chat.chatId } }
    }

    override suspend fun removePinnedChat(chatId: String) {
        pinnedChats.update { list -> list.filterNot { it.chatId == chatId } }
    }

    // ─── This device only ─────────────────────────────────────────────────────

    override fun getDemoModeFlow(): Flow<Boolean> = demoMode
    override suspend fun saveDemoMode(value: Boolean) {
        demoMode.value = value
    }

    override fun getOnboardingSeenFlow(): Flow<Boolean> = onboardingSeen
    override suspend fun setOnboardingSeen(value: Boolean) {
        onboardingSeen.value = value
    }

    override suspend fun getLastStartedVersion(): String = lastStartedVersion.value
    override suspend fun saveLastStartedVersion(value: String) {
        lastStartedVersion.value = value
    }

    override fun getEventsViewModeFlow(): Flow<EventsViewMode> = eventsViewMode
    override suspend fun saveEventsViewMode(mode: EventsViewMode) {
        eventsViewMode.value = mode
    }

    override fun getDraft(chatId: String, group: Boolean): Flow<String?> = drafts.map { it[chatId to group] }

    override fun saveDraft(chatId: String, group: Boolean, string: String) {
        drafts.update { it + ((chatId to group) to string) }
    }
}
