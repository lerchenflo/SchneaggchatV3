package org.lerchenflo.schneaggchatv3mp.games.presentation.crossword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.lerchenflo.schneaggchatv3mp.games.data.CrosswordRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordDirection
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordLanguage
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordPuzzle
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import kotlin.time.Clock

class CrosswordViewmodel(
    private val crosswordRepository: CrosswordRepository,
    private val gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CrosswordState())
    val state = _state.asStateFlow()

    private var timerJob: Job? = null

    fun onAction(action: CrosswordAction) {
        when (action) {
            is CrosswordAction.SelectLanguage -> loadPuzzle(action.language)
            CrosswordAction.RetryLoad -> _state.value.language?.let { loadPuzzle(it) }
            CrosswordAction.StopGame -> stopGame()
            CrosswordAction.RestartGame -> restartGame()
            is CrosswordAction.CellTapped -> onCellTapped(action.index)
            is CrosswordAction.KeyPressed -> onKeyPressed(action.letter)
            CrosswordAction.Backspace -> onBackspace()
            CrosswordAction.NextClue -> moveClue(1)
            CrosswordAction.PreviousClue -> moveClue(-1)
            CrosswordAction.CheckPuzzle -> checkPuzzle()
        }
    }

    /** Today's local daily puzzle — restarting on the same day reproduces it. */
    private fun loadPuzzle(language: CrosswordLanguage) {
        timerJob?.cancel()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        _state.value = CrosswordState(language = language, isLoading = true)

        viewModelScope.launch {
            val puzzle = when (language) {
                CrosswordLanguage.GERMAN -> generateGermanDailyPuzzle(today.toEpochDays())
                CrosswordLanguage.ENGLISH -> crosswordRepository.getEnglishDailyPuzzle(today)
            }
            if (puzzle == null) {
                _state.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }
            startWithPuzzle(language, puzzle)
        }
    }

    private fun startWithPuzzle(language: CrosswordLanguage, puzzle: CrosswordPuzzle) {
        val firstClue = puzzle.clues.minByOrNull { (if (it.direction == CrosswordDirection.DOWN) 1_000_000 else 0) + it.number }
        _state.value = CrosswordState(
            language = language,
            puzzle = puzzle,
            entries = List(puzzle.rows * puzzle.cols) { null },
            selectedCell = firstClue?.cells?.firstOrNull() ?: -1,
            direction = firstClue?.direction ?: CrosswordDirection.ACROSS,
        )
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (!_state.value.isSolved) {
                    _state.update { it.copy(elapsedMillis = it.elapsedMillis + 1000) }
                }
            }
        }
    }

    private fun restartGame() {
        val current = _state.value
        val puzzle = current.puzzle ?: return
        val language = current.language ?: return
        startWithPuzzle(language, puzzle)
    }

    private fun stopGame() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun onCellTapped(index: Int) {
        val current = _state.value
        val puzzle = current.puzzle ?: return
        if (current.isSolved) return
        if (index !in puzzle.solution.indices || puzzle.solution[index] == null) return

        val hasAcross = puzzle.clues.any { it.direction == CrosswordDirection.ACROSS && index in it.cells }
        val hasDown = puzzle.clues.any { it.direction == CrosswordDirection.DOWN && index in it.cells }

        val newDirection = when {
            // Second tap on the selected cell toggles direction (when both exist)
            index == current.selectedCell && hasAcross && hasDown ->
                if (current.direction == CrosswordDirection.ACROSS) CrosswordDirection.DOWN else CrosswordDirection.ACROSS
            current.direction == CrosswordDirection.ACROSS && !hasAcross -> CrosswordDirection.DOWN
            current.direction == CrosswordDirection.DOWN && !hasDown -> CrosswordDirection.ACROSS
            else -> current.direction
        }
        _state.update { it.copy(selectedCell = index, direction = newDirection) }
    }

    private fun onKeyPressed(letter: Char) {
        val current = _state.value
        val puzzle = current.puzzle ?: return
        if (current.isSolved || current.selectedCell < 0) return

        val entries = current.entries.toMutableList()
        entries[current.selectedCell] = letter.uppercaseChar()

        val clueCells = current.currentClue?.cells ?: emptyList()
        val position = clueCells.indexOf(current.selectedCell)
        val nextCell = if (position >= 0 && position < clueCells.size - 1) clueCells[position + 1] else current.selectedCell

        val solved = puzzle.solution.indices.all { i ->
            puzzle.solution[i] == null || entries[i] == puzzle.solution[i]
        }
        _state.update {
            it.copy(
                entries = entries,
                selectedCell = if (solved) it.selectedCell else nextCell,
                wrongCells = emptySet(),
                isSolved = solved,
            )
        }
        if (solved) {
            timerJob?.cancel()
            submitSolveTime()
        }
    }

    private fun submitSolveTime() {
        val current = _state.value
        // The leaderboard is a race: score stays 0, ranking falls to the time tiebreaker.
        // Difficulty encodes the puzzle language (LOW = German, HIGH = English).
        val difficulty = when (current.language) {
            CrosswordLanguage.GERMAN -> GameDifficulty.LOW
            else -> GameDifficulty.HIGH
        }
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.CROSSWORD,
                difficulty = difficulty,
                score = 0L,
                timeMillis = current.elapsedMillis,
            )
        }
    }

    private fun onBackspace() {
        val current = _state.value
        if (current.isSolved || current.selectedCell < 0) return

        val entries = current.entries.toMutableList()
        if (entries[current.selectedCell] != null) {
            entries[current.selectedCell] = null
            _state.update { it.copy(entries = entries, wrongCells = emptySet()) }
            return
        }
        // Cell already empty: step back within the word and clear that cell
        val clueCells = current.currentClue?.cells ?: return
        val position = clueCells.indexOf(current.selectedCell)
        if (position > 0) {
            val previousCell = clueCells[position - 1]
            entries[previousCell] = null
            _state.update { it.copy(entries = entries, selectedCell = previousCell, wrongCells = emptySet()) }
        }
    }

    private fun moveClue(offset: Int) {
        val current = _state.value
        val puzzle = current.puzzle ?: return
        if (puzzle.clues.isEmpty()) return
        // Ordered as displayed: all across clues, then all down clues
        val ordered = puzzle.clues.sortedWith(compareBy({ it.direction }, { it.number }))
        val currentIndex = ordered.indexOfFirst { it == current.currentClue }
        val nextIndex = ((if (currentIndex < 0) 0 else currentIndex + offset) + ordered.size) % ordered.size
        val clue = ordered[nextIndex]
        // Jump to the first empty cell of the clue (or its start when complete)
        val target = clue.cells.firstOrNull { current.entries[it] == null } ?: clue.cells.first()
        _state.update { it.copy(selectedCell = target, direction = clue.direction) }
    }

    private fun checkPuzzle() {
        val current = _state.value
        val puzzle = current.puzzle ?: return
        if (current.isSolved) return
        val wrong = puzzle.solution.indices.filterTo(mutableSetOf()) { i ->
            current.entries[i] != null && current.entries[i] != puzzle.solution[i]
        }
        _state.update { it.copy(wrongCells = wrong) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
