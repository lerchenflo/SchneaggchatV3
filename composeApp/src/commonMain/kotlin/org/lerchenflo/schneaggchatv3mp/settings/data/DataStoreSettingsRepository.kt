package org.lerchenflo.schneaggchatv3mp.settings.data

import kotlinx.coroutines.flow.Flow
import org.lerchenflo.schneaggchatv3mp.datasource.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.EventsViewMode
import org.lerchenflo.schneaggchatv3mp.datasource.domain.model.SyncedSettings
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils.UserSettingsRequest
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.trackConnectivity
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Draft
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.LanguageSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.MapStyleSetting
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.Preferencemanager
import org.lerchenflo.schneaggchatv3mp.datasource.preferences.ThemeSetting
import org.lerchenflo.schneaggchatv3mp.utilities.LanguageService

/** [SettingsRepository] on top of the DataStore wrapper [Preferencemanager], pushing synced settings via [NetworkUtils]. */
class DataStoreSettingsRepository(
    private val preferencemanager: Preferencemanager,
    private val networkUtils: NetworkUtils,
    private val languageService: LanguageService,
) : SettingsRepository {

    // ─── Server URL ────────────────────────────────────────────────────────────

    override fun getServerUrlFlow(): Flow<String> {
        return preferencemanager.getServerUrlFlow()
    }

    override suspend fun getServerUrl(): String {
        return preferencemanager.getServerUrl()
    }

    override suspend fun saveServerUrl(url: String) {
        preferencemanager.saveServerUrl(url)
    }

    // ─── Personal settings (theme, language, pinned chats, ...) ────────────────
    // Synced across every device of this account via SelfUserResponse.settings. Push side:
    // write the local preference first so the UI reacts instantly, then fire the partial-update
    // request in the background and ignore its result - an offline edit is reconciled by the next
    // /users/sync or UserChange socket push, which always wins (see applyServerSettings below).

    /**
     * Applies the server's settings onto local DataStore, overwriting whatever was there. Called
     * from both the /users/sync SelfUserResponse branch and the UserChange socket push, so a
     * settings change made on another device of the same account takes effect here without the
     * user touching a setting themselves.
     */
    override suspend fun applyServerSettings(settings: SyncedSettings) {
        preferencemanager.applySyncedSettings(settings)

        // Language has to go through LanguageService/LanguageManager to actually switch the
        // platform locale (recreates the Activity on Android), so only touch it when it changed.
        val newLanguage = LanguageSetting.entries.firstOrNull { it.name == settings.language } ?: LanguageSetting.SYSTEM
        if (preferencemanager.getLanguageSetting() != newLanguage) {
            languageService.applyLanguage(newLanguage)
        }
    }

    override fun getUseMdFlow(): Flow<Boolean> {
        return preferencemanager.getUseMdFlow()
    }

    override suspend fun setUseMd(value: Boolean) {
        preferencemanager.saveUseMd(value)
        networkUtils.updateSettings(UserSettingsRequest(mdFormat = value)).trackConnectivity()
    }

    override fun getDevSettingsFlow(): Flow<Boolean> {
        return preferencemanager.getDevSettingsFlow()
    }

    override suspend fun setDevSettings(value: Boolean) {
        preferencemanager.saveDevSettings(value)
        networkUtils.updateSettings(UserSettingsRequest(developerSettings = value)).trackConnectivity()
    }

    override fun getHighlightTodaysMessageTimestampFlow(): Flow<Boolean> {
        return preferencemanager.getHighlightTodaysMessageTimestampFlow()
    }

    override suspend fun setHighlightTodaysMessageTimestamp(value: Boolean) {
        preferencemanager.saveHighlightTodaysMessageTimestamp(value)
        networkUtils.updateSettings(UserSettingsRequest(highlightTodaysMessageTimestamp = value)).trackConnectivity()
    }

    override fun getThemeFlow(): Flow<ThemeSetting> {
        return preferencemanager.getThemeFlow()
    }

    override suspend fun setThemeSetting(theme: ThemeSetting) {
        preferencemanager.saveThemeSetting(theme)
        networkUtils.updateSettings(UserSettingsRequest(theme = theme.name)).trackConnectivity()
    }

    override suspend fun setLanguageSetting(language: LanguageSetting) {
        languageService.applyLanguage(language)
        networkUtils.updateSettings(UserSettingsRequest(language = language.name)).trackConnectivity()
    }

    override fun getMergeMapLocationsFlow(): Flow<Boolean> {
        return preferencemanager.getMergeMapLocationsFlow()
    }

    override suspend fun setMergeMapLocations(value: Boolean) {
        preferencemanager.saveMergeMapLocations(value)
        networkUtils.updateSettings(UserSettingsRequest(mergeMapLocations = value)).trackConnectivity()
    }

    override fun getMergeMapUsersFlow(): Flow<Boolean> {
        return preferencemanager.getMergeMapUsersFlow()
    }

    override suspend fun setMergeMapUsers(value: Boolean) {
        preferencemanager.saveMergeMapUsers(value)
        networkUtils.updateSettings(UserSettingsRequest(mergeMapUsers = value)).trackConnectivity()
    }

    override fun getMapStyleSettingFlow(): Flow<MapStyleSetting> {
        return preferencemanager.getMapStyleSettingFlow()
    }

    override fun getMapStyleUrlFlow(): Flow<String> {
        return preferencemanager.getMapStyleUrlFlow()
    }

    override suspend fun setMapStyleSetting(style: MapStyleSetting) {
        preferencemanager.saveMapStyleSetting(style)
        networkUtils.updateSettings(UserSettingsRequest(mapStyle = style.name)).trackConnectivity()
    }

    override fun getQuickReactionsFlow(): Flow<List<String>> {
        return preferencemanager.getQuickReactionsFlow()
    }

    /**
     * Replaces the quick reaction list wholesale (server stores it as one value, like pinned chats).
     * An empty list is a valid choice and is stored as such - use [resetQuickReactions] to go back
     * to the app defaults.
     */
    override suspend fun setQuickReactions(reactions: List<String>) {
        preferencemanager.saveQuickReactions(reactions)
        networkUtils.updateSettings(UserSettingsRequest(quickReactions = reactions)).trackConnectivity()
    }

    /** Clears the customised list so DEFAULT_QUICK_REACTIONS applies again, here and on every device. */
    override suspend fun resetQuickReactions() {
        preferencemanager.clearQuickReactions()
        networkUtils.updateSettings(UserSettingsRequest(resetQuickReactions = true)).trackConnectivity()
    }

    override suspend fun getLastContributePopupShown(): Long {
        return preferencemanager.getLastContributePopupShown()
    }

    override suspend fun setLastContributePopupShown(epochMillis: Long) {
        preferencemanager.saveLastContributePopupShown(epochMillis)
        networkUtils.updateSettings(UserSettingsRequest(lastContributePopupShown = epochMillis))
    }

    // ─── This device only ──────────────────────────────────────────────────────

    override fun getDemoModeFlow(): Flow<Boolean> {
        return preferencemanager.getDemoModeFlow()
    }

    override suspend fun saveDemoMode(value: Boolean) {
        preferencemanager.saveDemoMode(value)
    }

    override suspend fun setOnboardingSeen(value: Boolean) {
        preferencemanager.setOnboardingSeen(value)
    }

    override suspend fun getLastStartedVersion(): String {
        return preferencemanager.getLastStartedVersion()
    }

    override suspend fun saveLastStartedVersion(value: String) {
        preferencemanager.saveLastStartedVersion(value)
    }

    override fun getEventsViewModeFlow(): Flow<EventsViewMode> {
        return preferencemanager.getEventsViewModeFlow()
    }

    override suspend fun saveEventsViewMode(mode: EventsViewMode) {
        preferencemanager.saveEventsViewMode(mode)
    }

    override fun getDraft(chatId: String, group: Boolean) : Flow<String?> {
        return preferencemanager.getDraftsFlow(
            chatId = chatId,
            group = group
        )
    }

    override suspend fun saveDraft(chatId: String, group: Boolean, string: String) {
        preferencemanager.addDraft(
            draft = Draft(
                chatId = chatId,
                group = group,
                string = string
            )
        )
    }

}
