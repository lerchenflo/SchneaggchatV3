package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
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



}
