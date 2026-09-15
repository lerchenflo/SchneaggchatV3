package org.lerchenflo.schneaggchatv3mp.games.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.utilities.today
import kotlin.time.Clock

/**
 * Stores one in-progress (or, for daily games, finished) run per game as a JSON
 * string in DataStore. Writes are fire-and-forget on [ApplicationScope] so they
 * still complete when triggered from ViewModel.onCleared(), where viewModelScope
 * is already cancelled.
 */
class GameSaveRepository(
    private val prefs: DataStore<Preferences>,
    private val applicationScope: ApplicationScope,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Single lane so a save followed by a clear (or vice versa) lands in call order
    @OptIn(ExperimentalCoroutinesApi::class)
    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)
    private var lastWrite: Job? = null

    /**
     * The stored run, or null when there is none, it cannot be decoded, its
     * [schemaVersion] differs, or (daily games) it was taken on another day.
     * Unusable saves are removed.
     */
    suspend fun <T> load(game: GameId, serializer: KSerializer<T>, schemaVersion: Int): GameSave<T>? {
        // A ViewModel recreated right after the previous one was cleared must see that write
        lastWrite?.join()
        val raw = prefs.data.first()[key(game)] ?: return null
        val save = runCatching { json.decodeFromString(GameSave.serializer(serializer), raw) }.getOrNull()
        val usable = save != null &&
            save.schemaVersion == schemaVersion &&
            (!game.daily || save.epochDay == today().toEpochDays())
        if (!usable) {
            clearAsync(game)
            return null
        }
        return save
    }

    fun <T> saveAsync(
        game: GameId,
        serializer: KSerializer<T>,
        schemaVersion: Int,
        difficulty: GameDifficulty,
        data: T,
    ) {
        val save = GameSave(
            schemaVersion = schemaVersion,
            savedAtMillis = Clock.System.now().toEpochMilliseconds(),
            epochDay = today().toEpochDays(),
            difficulty = difficulty,
            data = data,
        )
        lastWrite = applicationScope.launch(writeDispatcher) {
            val encoded = json.encodeToString(GameSave.serializer(serializer), save)
            prefs.edit { it[key(game)] = encoded }
        }
    }

    fun clearAsync(game: GameId) {
        lastWrite = applicationScope.launch(writeDispatcher) {
            prefs.edit { it.remove(key(game)) }
        }
    }

    private fun key(game: GameId) = stringPreferencesKey("game_save_${game.name.lowercase()}")
}
