package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.lerchenflo.schneaggchatv3mp.datasource.network.NetworkUtils
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.dark_theme
import schneaggchatv3mp.composeapp.generated.resources.light_theme
import schneaggchatv3mp.composeapp.generated.resources.ph_theme
import schneaggchatv3mp.composeapp.generated.resources.system_theme

enum class PreferenceKey {
    ACCESSTOKEN,
    REFRESHTOKEN,
    ENCRYPTIONKEY,
    OWNID,
    MD_FORMAT,
    THEME,
    SERVERURL,
    DEVELOPERSETTINGS
}

enum class ThemeSetting {
    SYSTEM,     // Follow system setting
    LIGHT,      // Always light
    DARK;       // Always dark
    //PHTHEME;    // Anderes Theme ganz sicher ned klaut
    fun toUiText(): UiText = when (this) {
        SYSTEM -> UiText.StringResourceText(Res.string.system_theme)
        LIGHT -> UiText.StringResourceText(Res.string.light_theme)
        DARK   -> UiText.StringResourceText(Res.string.dark_theme)
        //PHTHEME -> UiText.StringResourceText(Res.string.ph_theme)
    }
    fun getIcon(): ImageVector = when (this) {
        SYSTEM -> Icons.Default.Contrast
        LIGHT -> Icons.Default.LightMode
        DARK   -> Icons.Default.DarkMode
        //PHTHEME -> Icons.Default.Male
    }
}

class Preferencemanager(
    private val pref: DataStore<Preferences>
) {
    private val dispatcher = Dispatchers.IO

    suspend fun getEncryptionKey() : String{
        val key = stringPreferencesKey(PreferenceKey.ENCRYPTIONKEY.toString())
        val prefs = pref.data.first()
        return prefs[key] ?: ""
    }

    suspend fun saveTokens(tokenPair: NetworkUtils.TokenPair) {
        pref.edit { datastore ->
            val accesskey = stringPreferencesKey(PreferenceKey.ACCESSTOKEN.toString())
            datastore[accesskey] = tokenPair.accessToken

            val refreshkey = stringPreferencesKey(PreferenceKey.REFRESHTOKEN.toString())
            datastore[refreshkey] = tokenPair.refreshToken

            if (tokenPair.encryptionKey != null) {
                val encryptionKey = stringPreferencesKey(PreferenceKey.ENCRYPTIONKEY.toString())
                datastore[encryptionKey] = tokenPair.encryptionKey
            }
        }
    }


    suspend fun getTokens(): NetworkUtils.TokenPair {
        return with(dispatcher) {
            val accessKey = stringPreferencesKey(PreferenceKey.ACCESSTOKEN.toString())
            val refreshKey = stringPreferencesKey(PreferenceKey.REFRESHTOKEN.toString())

            val prefs = pref.data.first()

            val accesstoken = prefs[accessKey] ?: ""
            val refreshtoken = prefs[refreshKey] ?: ""

            NetworkUtils.TokenPair(
                accessToken = accesstoken,
                refreshToken = refreshtoken
            )
        }
    }

    suspend fun saveOWNID(ownid: String) {
        pref.edit { datastore ->
            val key = stringPreferencesKey(PreferenceKey.OWNID.toString())
            datastore[key] = ownid
        }
    }

    suspend fun getOWNID(): String {
        return with(dispatcher) {
            val key = stringPreferencesKey(PreferenceKey.OWNID.toString())
            val prefs = pref.data.first()
            prefs[key] ?: ""
        }
    }

    val mdFormatKey = PreferenceKey.MD_FORMAT.toString()

    suspend fun saveUseMd(value: Boolean){
        pref.edit { datastore ->
            val key = booleanPreferencesKey(mdFormatKey)
            datastore[key] = value
        }
    }

    // In PreferenceManager
    fun getUseMdFlow(): Flow<Boolean> = pref.data.map { prefs ->
        prefs[booleanPreferencesKey(mdFormatKey)] ?: false
    }.flowOn(Dispatchers.IO)



    // Theme methods
    suspend fun saveThemeSetting(theme: ThemeSetting) {
        pref.edit { datastore ->
            val key = intPreferencesKey(PreferenceKey.THEME.toString())
            datastore[key] = theme.ordinal
        }
    }

    fun getThemeFlow(): Flow<ThemeSetting> = pref.data.map { prefs ->
        val key = intPreferencesKey(PreferenceKey.THEME.toString())
        val ordinal = prefs[key] ?: ThemeSetting.SYSTEM.ordinal
        ThemeSetting.entries[ordinal]
    }.flowOn(Dispatchers.IO)

    // For one-time read (use in non-composable contexts)
    suspend fun getThemeSetting(): ThemeSetting {
        return with(dispatcher) {
            val key = intPreferencesKey(PreferenceKey.THEME.toString())
            val prefs = pref.data.first()
            val ordinal = prefs[key] ?: ThemeSetting.SYSTEM.ordinal
            ThemeSetting.entries[ordinal]
        }
    }

    private val defaultServerUrl = "https://schneaggchatv3.lerchenflo.eu" // default server adresse
    // todo sollama des in na variable speichera wo ma besser findet?


    suspend fun saveServerUrl(url: String) {
        pref.edit { datastore ->
            val key = stringPreferencesKey(PreferenceKey.SERVERURL.toString())
            datastore[key] = url
        }
    }

    fun getServerUrlFlow(): Flow<String> = pref.data.map { prefs ->
        val key = stringPreferencesKey(PreferenceKey.SERVERURL.toString())
        prefs[key] ?: defaultServerUrl


    }.flowOn(Dispatchers.IO)

    // For one-time read (use in non-composable contexts)
    suspend fun getServerUrl(): String {
        return with(dispatcher) {
            val key = stringPreferencesKey(PreferenceKey.SERVERURL.toString())
            val prefs = pref.data.first()
            prefs[key] ?: defaultServerUrl
        }
    }

    suspend fun buildServerUrl(endpoint: String): String {
        if (endpoint.startsWith("http", ignoreCase = true)) return endpoint
        val base = getServerUrl().trimEnd('/')
        val ep = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        return base + ep
    }

    suspend fun saveDevSettings(value: Boolean){
        pref.edit { datastore ->
            val key = booleanPreferencesKey(PreferenceKey.DEVELOPERSETTINGS.toString())
            datastore[key] = value
        }
    }

    suspend fun getDevSettings(): Boolean {
        return with(dispatcher) {
            val key = booleanPreferencesKey(PreferenceKey.DEVELOPERSETTINGS.toString())
            val prefs = pref.data.first()
            prefs[key] ?: false
        }
    }

    // In PreferenceManager
    fun getDevSettingsFlow(): Flow<Boolean> = pref.data.map { prefs ->
        prefs[booleanPreferencesKey(PreferenceKey.DEVELOPERSETTINGS.toString())] ?: false
    }.flowOn(Dispatchers.IO)
}
