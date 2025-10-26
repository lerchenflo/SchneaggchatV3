package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugStatus.Finished
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugStatus.InProgress
import org.lerchenflo.schneaggchatv3mp.todolist.domain.BugStatus.Unfinished
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.bugstatus_finished
import schneaggchatv3mp.composeapp.generated.resources.bugstatus_in_progress
import schneaggchatv3mp.composeapp.generated.resources.bugstatus_unfinished
import schneaggchatv3mp.composeapp.generated.resources.dark_theme
import schneaggchatv3mp.composeapp.generated.resources.light_theme
import schneaggchatv3mp.composeapp.generated.resources.system_theme

enum class PreferenceKey {
    USERNAME,
    PASSWORD,
    OWNID,
    MD_FORMAT,
    THEME
}

enum class ThemeSetting {
    SYSTEM,    // Follow system setting
    LIGHT,     // Always light
    DARK;       // Always dark
    fun toUiText(): UiText = when (this) {
        SYSTEM -> UiText.StringResourceText(Res.string.system_theme)
        LIGHT -> UiText.StringResourceText(Res.string.light_theme)
        DARK   -> UiText.StringResourceText(Res.string.dark_theme)
    }
}

class Preferencemanager(
    private val pref: DataStore<Preferences>
) {
    private val dispatcher = Dispatchers.IO


    suspend fun saveAutologinCreds(username: String, password: String) {
        with(dispatcher) {
            pref.edit { datastore ->
                val usernamekey = stringPreferencesKey(PreferenceKey.USERNAME.toString())
                datastore[usernamekey] = username

                val passwordkey = stringPreferencesKey(PreferenceKey.PASSWORD.toString())
                datastore[passwordkey] = password
            }
        }
    }


    suspend fun getAutologinCreds(): Pair<String, String> {
        return with(dispatcher) {
            val usernameKey = stringPreferencesKey(PreferenceKey.USERNAME.toString())
            val passwordKey = stringPreferencesKey(PreferenceKey.PASSWORD.toString())

            val prefs = pref.data.first()

            val username = prefs[usernameKey] ?: ""
            val password = prefs[passwordKey] ?: ""

            Pair(username, password)
        }
    }

    suspend fun saveOWNID(ownid: Long) {
        with(dispatcher) {
            pref.edit { datastore ->
                val key = longPreferencesKey(PreferenceKey.OWNID.toString())
                datastore[key] = ownid
            }
        }
    }

    suspend fun getOWNID(): Long {
        return with(dispatcher) {
            val key = longPreferencesKey(PreferenceKey.OWNID.toString())
            val prefs = pref.data.first()
            val id = prefs[key] ?: -1

            id
        }
    }

    val mdFormatKey = PreferenceKey.MD_FORMAT.toString()

    suspend fun saveUseMd(value: Boolean){
        with(dispatcher) {
            pref.edit { datastore ->
                val key = booleanPreferencesKey(mdFormatKey)
                datastore[key] = value
            }
        }
    }

    // In PreferenceManager
    fun getUseMdFlow(): Flow<Boolean> = pref.data.map { prefs ->
        prefs[booleanPreferencesKey(mdFormatKey)] ?: false
    }.flowOn(Dispatchers.IO)

    // Theme methods
    suspend fun saveThemeSetting(theme: ThemeSetting) {
        with(dispatcher) {
            pref.edit { datastore ->
                val key = intPreferencesKey(PreferenceKey.THEME.toString())
                datastore[key] = theme.ordinal
            }
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
            ThemeSetting.values()[ordinal]
        }
    }

}
