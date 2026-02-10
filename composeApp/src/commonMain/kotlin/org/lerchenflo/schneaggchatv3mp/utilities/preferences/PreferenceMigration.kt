package org.lerchenflo.schneaggchatv3mp.utilities.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * One-time migration utility to move sensitive data from DataStore to KSafe.
 * Call this once after adding KSafe integration.
 */

//TODO: Remove in future update (3.0.4??)
class SecureDataMigration(
    private val dataStore: DataStore<Preferences>,
    private val kSafe: KSafe
) {

    private object OldKeys {
        val ACCESS_TOKEN = stringPreferencesKey("ACCESSTOKEN")
        val REFRESH_TOKEN = stringPreferencesKey("REFRESHTOKEN")
        val ENCRYPTION_KEY = stringPreferencesKey("ENCRYPTIONKEY")
        val OWN_ID = stringPreferencesKey("OWNID")
        val PINNED_CHATS = stringSetPreferencesKey("pinned_chats")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Migrates sensitive data from DataStore to KSafe and removes it from DataStore.
     * Returns true if migration was successful, false otherwise.
     */
    suspend fun migrate(): Boolean {
        return try {
            val prefs = dataStore.data.first()

            // Migrate tokens
            val accessToken = prefs[OldKeys.ACCESS_TOKEN]
            val refreshToken = prefs[OldKeys.REFRESH_TOKEN]
            val encryptionKey = prefs[OldKeys.ENCRYPTION_KEY]
            val ownId = prefs[OldKeys.OWN_ID]
            val pinnedChatsSet = prefs[OldKeys.PINNED_CHATS]

            // Only migrate if we have data
            var hasMigrated = false

            if (!accessToken.isNullOrEmpty()) {
                kSafe.put(Preferencemanager.SecureKey.ACCESS_TOKEN.key, accessToken)
                hasMigrated = true
            }

            if (!refreshToken.isNullOrEmpty()) {
                kSafe.put(Preferencemanager.SecureKey.REFRESH_TOKEN.key, refreshToken)
                hasMigrated = true
            }

            if (!encryptionKey.isNullOrEmpty()) {
                kSafe.put(Preferencemanager.SecureKey.ENCRYPTION_KEY.key, encryptionKey)
                hasMigrated = true
            }

            if (!ownId.isNullOrEmpty()) {
                kSafe.put(Preferencemanager.SecureKey.OWN_ID.key, ownId)
                hasMigrated = true
            }

            // Migrate pinned chats from string set to JSON list
            if (!pinnedChatsSet.isNullOrEmpty()) {
                val pinnedChats = pinnedChatsSet.mapNotNull { str ->
                    val parts = str.split("|")
                    if (parts.size >= 2) {
                        PinnedChat(
                            chatId = parts[0],
                            pinTimePoint = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                        )
                    } else null
                }

                if (pinnedChats.isNotEmpty()) {
                    val chatsJson = json.encodeToString(pinnedChats)
                    kSafe.put(OldKeys.PINNED_CHATS.name, chatsJson)
                    hasMigrated = true
                }
            }

            // Remove old data from DataStore
            if (hasMigrated) {
                dataStore.edit { mutablePrefs ->
                    mutablePrefs.remove(OldKeys.ACCESS_TOKEN)
                    mutablePrefs.remove(OldKeys.REFRESH_TOKEN)
                    mutablePrefs.remove(OldKeys.ENCRYPTION_KEY)
                    mutablePrefs.remove(OldKeys.OWN_ID)
                    mutablePrefs.remove(OldKeys.PINNED_CHATS)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if migration is needed (i.e., if old data exists in DataStore)
     */
    suspend fun needsMigration(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[OldKeys.ACCESS_TOKEN] != null ||
                prefs[OldKeys.REFRESH_TOKEN] != null ||
                prefs[OldKeys.ENCRYPTION_KEY] != null ||
                prefs[OldKeys.OWN_ID] != null ||
                prefs[OldKeys.PINNED_CHATS] != null
    }
}

/**
 * Usage example:
 *
 * // In your app initialization (e.g., MainActivity or App class)
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *
 *         lifecycleScope.launch {
 *             val migration = SecureDataMigration(dataStore, kSafe)
 *
 *             if (migration.needsMigration()) {
 *                 val success = migration.migrate()
 *                 if (success) {
 *                     Log.d("Migration", "Successfully migrated secure data to KSafe")
 *                 } else {
 *                     Log.e("Migration", "Failed to migrate secure data")
 *                 }
 *             }
 *         }
 *     }
 * }
 */