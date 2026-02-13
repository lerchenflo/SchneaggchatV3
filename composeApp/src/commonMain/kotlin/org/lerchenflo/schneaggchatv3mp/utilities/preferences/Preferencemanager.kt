package org.lerchenflo.schneaggchatv3mp.utilities.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.BASE_SERVER_URL
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils

class Preferencemanager(
    private val prefs: DataStore<Preferences>,
    private val securePrefs: KSafe
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    // ========== SECURE STORAGE (KSafe - Encrypted) ==========

    enum class SecureKey(val key: String) {
        ACCESS_TOKEN("access_token"),
        REFRESH_TOKEN("refresh_token"),
        ENCRYPTION_KEY("encryption_key"),
        OWN_ID("own_id");

        companion object {
            fun getAllKeys(): List<String> = entries.map { it.key }
        }
    }

    suspend fun getEncryptionKey(): String {
        return securePrefs.get(SecureKey.ENCRYPTION_KEY.key, "")
    }

    suspend fun saveTokens(tokenPair: NetworkUtils.TokenPair) {
        securePrefs.put(SecureKey.ACCESS_TOKEN.key, tokenPair.accessToken)
        securePrefs.put(SecureKey.REFRESH_TOKEN.key, tokenPair.refreshToken)
        tokenPair.encryptionKey?.let {
            securePrefs.put(SecureKey.ENCRYPTION_KEY.key, it)
        }
    }

    suspend fun getTokens(): NetworkUtils.TokenPair {
        val accessToken = securePrefs.get(SecureKey.ACCESS_TOKEN.key, "")
        val refreshToken = securePrefs.get(SecureKey.REFRESH_TOKEN.key, "")
        val encryptionKey = securePrefs.get(SecureKey.ENCRYPTION_KEY.key, "").takeIf { it.isNotEmpty() }

        return NetworkUtils.TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            encryptionKey = encryptionKey
        )
    }

    suspend fun saveOWNID(ownid: String) {
        securePrefs.put(SecureKey.OWN_ID.key, ownid)
    }

    suspend fun getOWNID(): String {
        return securePrefs.get(SecureKey.OWN_ID.key, "")
    }

    suspend fun clearSecureData() {
        SecureKey.entries.forEach { secureKey ->
            securePrefs.delete(secureKey.key)
        }
    }

    // ========== NON-SECURE STORAGE (DataStore - Preferences) ==========

    private object PrefsKeys {
        val MD_FORMAT = booleanPreferencesKey("md_format")
        val THEME = intPreferencesKey("theme")
        val LANGUAGE = intPreferencesKey("language")
        val SERVER_URL = stringPreferencesKey("server_url")
        val DEVELOPER_SETTINGS = booleanPreferencesKey("developer_settings")
        val PINNED_CHATS = stringPreferencesKey("pinned_chats")
    }

    // Markdown Format
    suspend fun saveUseMd(value: Boolean) {
        prefs.edit { it[PrefsKeys.MD_FORMAT] = value }
    }

    fun getUseMdFlow(): Flow<Boolean> = prefs.data.map { prefs ->
        prefs[PrefsKeys.MD_FORMAT] ?: true //Default to true
    }

    // Theme methods
    suspend fun saveThemeSetting(theme: ThemeSetting) {
        prefs.edit { it[PrefsKeys.THEME] = theme.ordinal }
    }

    fun getThemeFlow(): Flow<ThemeSetting> = prefs.data.map { prefs ->
        val ordinal = prefs[PrefsKeys.THEME] ?: ThemeSetting.SYSTEM.ordinal
        ThemeSetting.entries.getOrNull(ordinal) ?: ThemeSetting.SYSTEM
    }

    suspend fun getThemeSetting(): ThemeSetting {
        val ordinal = prefs.data.first()[PrefsKeys.THEME] ?: ThemeSetting.SYSTEM.ordinal
        return ThemeSetting.entries.getOrNull(ordinal) ?: ThemeSetting.SYSTEM
    }

    // Language methods
    suspend fun saveLanguageSetting(language: LanguageSetting) {
        prefs.edit { it[PrefsKeys.LANGUAGE] = language.ordinal }
    }

    fun getLanguageFlow(): Flow<LanguageSetting> = prefs.data.map { prefs ->
        val ordinal = prefs[PrefsKeys.LANGUAGE] ?: LanguageSetting.SYSTEM.ordinal
        LanguageSetting.entries.getOrNull(ordinal) ?: LanguageSetting.SYSTEM
    }

    suspend fun getLanguageSetting(): LanguageSetting {
        val ordinal = prefs.data.first()[PrefsKeys.LANGUAGE] ?: LanguageSetting.SYSTEM.ordinal
        return LanguageSetting.entries.getOrNull(ordinal) ?: LanguageSetting.SYSTEM
    }

    // Server URL methods
    suspend fun saveServerUrl(url: String) {
        prefs.edit { it[PrefsKeys.SERVER_URL] = url }
    }

    fun getServerUrlFlow(): Flow<String> = prefs.data.map { prefs ->
        prefs[PrefsKeys.SERVER_URL] ?: BASE_SERVER_URL
    }

    suspend fun getServerUrl(): String {
        return prefs.data.first()[PrefsKeys.SERVER_URL] ?: BASE_SERVER_URL
    }

    suspend fun buildServerUrl(endpoint: String): String {
        if (endpoint.startsWith("http", ignoreCase = true)) return endpoint
        val base = getServerUrl().trimEnd('/')
        val ep = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        return base + ep
    }

    // Developer Settings
    suspend fun saveDevSettings(value: Boolean) {
        prefs.edit { it[PrefsKeys.DEVELOPER_SETTINGS] = value }
    }

    suspend fun getDevSettings(): Boolean {
        return prefs.data.first()[PrefsKeys.DEVELOPER_SETTINGS] ?: false
    }

    fun getDevSettingsFlow(): Flow<Boolean> = prefs.data.map { prefs ->
        prefs[PrefsKeys.DEVELOPER_SETTINGS] ?: false
    }

    // Pinned Chats - Stored as JSON
    suspend fun addPinnedChat(chat: PinnedChat) {
        val currentChats = getPinnedChats().toMutableList()

        // Remove existing entry for this ID to avoid duplicates
        currentChats.removeAll { it.chatId == chat.chatId }

        // Add new chat
        currentChats.add(chat)

        // Save to DataStore as JSON
        prefs.edit {
            it[PrefsKeys.PINNED_CHATS] = json.encodeToString(currentChats)
        }
    }

    suspend fun removePinnedChat(chatId: String) {
        val currentChats = getPinnedChats().toMutableList()
        currentChats.removeAll { it.chatId == chatId }

        prefs.edit {
            it[PrefsKeys.PINNED_CHATS] = json.encodeToString(currentChats)
        }
    }

    fun getPinnedChatsFlow(): Flow<List<PinnedChat>> = prefs.data.map { prefs ->
        val chatsJson = prefs[PrefsKeys.PINNED_CHATS] ?: ""
        if (chatsJson.isEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<PinnedChat>>(chatsJson)
                    .sortedByDescending { it.pinTimePoint }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getPinnedChats(): List<PinnedChat> {
        val chatsJson = prefs.data.first()[PrefsKeys.PINNED_CHATS] ?: ""
        return if (chatsJson.isEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<PinnedChat>>(chatsJson)
                    .sortedByDescending { it.pinTimePoint }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

@Serializable
data class PinnedChat(
    val chatId: String,
    val pinTimePoint: Long
)