package org.lerchenflo.schneaggchatv3mp.games.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSaveSlot

/**
 * ViewModel-side plumbing for one game's persisted run: restore once when the
 * ViewModel is created, expose [restoreChecked] so the screen does not start a
 * fresh run before the save was looked at, and forward app-background events so
 * the run can be paused and persisted before a possible process kill.
 */
class GameSaveSession<T>(
    private val game: GameSaveSlot,
    private val serializer: KSerializer<T>,
    private val schemaVersion: Int,
    private val repository: GameSaveRepository,
    private val scope: CoroutineScope,
) {
    private val _restoreChecked = MutableStateFlow(false)

    /** True once the stored run was loaded (and restored if it was usable). */
    val restoreChecked: StateFlow<Boolean> = _restoreChecked.asStateFlow()

    fun start(onRestore: (GameSave<T>) -> Unit, onAppBackgrounded: () -> Unit) {
        scope.launch {
            repository.load(game, serializer, schemaVersion)?.let(onRestore)
            _restoreChecked.value = true
            AppLifecycleManager.appBackgroundedEvent.collect { onAppBackgrounded() }
        }
    }

    /** Persists [snapshot]; null means there is nothing worth keeping and removes the save. */
    fun persist(difficulty: GameDifficulty, snapshot: T?) {
        if (snapshot == null) {
            repository.clearAsync(game)
        } else {
            repository.saveAsync(game, serializer, schemaVersion, difficulty, snapshot)
        }
    }

    /** For games without a difficulty setting; the envelope still needs one, so the default is stored. */
    fun persist(snapshot: T?) = persist(GameDifficulty.MEDIUM, snapshot)

    fun clear() = repository.clearAsync(game)
}
