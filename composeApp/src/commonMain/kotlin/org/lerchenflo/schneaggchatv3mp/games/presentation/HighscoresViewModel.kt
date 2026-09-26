package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.datasource.network.util.NetworkResult
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.HighscoreEntry
import org.lerchenflo.schneaggchatv3mp.games.domain.LeaderboardPeriod
import org.lerchenflo.schneaggchatv3mp.games.domain.defaultLeaderboardPeriod
import org.lerchenflo.schneaggchatv3mp.games.domain.leaderboardDifficulties

data class HighscoresState(
    val game: GameId? = null,
    /** The boards this game actually has, one per difficulty. */
    val boards: List<GameDifficulty> = emptyList(),
    val selectedDifficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val selectedPeriod: LeaderboardPeriod = LeaderboardPeriod.YEARLY,
    val entries: List<HighscoreEntry> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

sealed interface HighscoresAction {
    /** Opens the leaderboard of one game; [initialDifficulty] is only a starting point. */
    data class OnOpen(val game: GameId, val initialDifficulty: GameDifficulty) : HighscoresAction
    data class OnSelectDifficulty(val difficulty: GameDifficulty) : HighscoresAction
    data class OnSelectPeriod(val period: LeaderboardPeriod) : HighscoresAction
}

/** Loads the server leaderboard of a single game for the highscores dialog. */
class HighscoresViewModel(
    private val repository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HighscoresState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    fun onAction(action: HighscoresAction) {
        when (action) {
            is HighscoresAction.OnOpen -> open(action.game, action.initialDifficulty)
            is HighscoresAction.OnSelectDifficulty -> {
                if (action.difficulty == _state.value.selectedDifficulty) return
                _state.update { it.copy(selectedDifficulty = action.difficulty) }
                load()
            }
            is HighscoresAction.OnSelectPeriod -> {
                if (action.period == _state.value.selectedPeriod) return
                _state.update { it.copy(selectedPeriod = action.period) }
                load()
            }
        }
    }

    private fun open(game: GameId, initialDifficulty: GameDifficulty) {
        val boards = game.leaderboardDifficulties
        _state.update {
            it.copy(
                game = game,
                boards = boards,
                selectedDifficulty = initialDifficulty.takeIf { difficulty -> difficulty in boards }
                    ?: boards.first(),
                selectedPeriod = game.defaultLeaderboardPeriod,
                entries = emptyList(),
                hasError = false,
            )
        }
        load()
    }

    private fun load() {
        val current = _state.value
        val game = current.game ?: return
        // Switching boards quickly must not let an older response win
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, hasError = false) }
        loadJob = viewModelScope.launch {
            val result = repository.getHighscores(game, current.selectedDifficulty, current.selectedPeriod)
            _state.update {
                when (result) {
                    is NetworkResult.Success -> it.copy(entries = result.data, isLoading = false, hasError = false)
                    is NetworkResult.Error -> it.copy(entries = emptyList(), isLoading = false, hasError = true)
                }
            }
        }
    }
}
