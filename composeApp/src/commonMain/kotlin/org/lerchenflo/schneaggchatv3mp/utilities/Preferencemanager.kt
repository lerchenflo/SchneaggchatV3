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

class Preferencemanager(
    private val pref: DataStore<Preferences>
) {
    private val dispatcher = Dispatchers.IO


    suspend fun saveAutologinCreds(username: String, password: String) {
        with(dispatcher) {
            pref.edit { datastore ->
                val usernamekey = stringPreferencesKey("username")
                datastore[usernamekey] = username

                val passwordkey = stringPreferencesKey("password")
                datastore[passwordkey] = password
            }
        }
    }


    suspend fun getAutologinCreds(): Pair<String, String> {
        return with(dispatcher) {
            val usernameKey = stringPreferencesKey("username")
            val passwordKey = stringPreferencesKey("password")

            val prefs = pref.data.first()

            val username = prefs[usernameKey] ?: ""
            val password = prefs[passwordKey] ?: ""

            Pair(username, password)
        }
    }

    suspend fun saveOWNID(ownid: Long) {
        with(dispatcher) {
            pref.edit { datastore ->
                val key = longPreferencesKey("OWNID")
                datastore[key] = ownid
            }
        }
    }

    suspend fun getOWNID(): Long {
        return with(dispatcher) {
            val key = longPreferencesKey("OWNID")
            val prefs = pref.data.first()
            val id = prefs[key] ?: -1

            id
        }
    }

    val mdFormatKey = "MdFormat"

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

}
