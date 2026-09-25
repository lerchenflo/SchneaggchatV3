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
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.games.data.CrosswordRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordDirection
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordLanguage
import org.lerchenflo.schneaggchatv3mp.games.domain.CrosswordPuzzle
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.domain.letterCellCount
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import kotlin.random.Random

class CrosswordViewmodel(
    private val crosswordRepository: CrosswordRepository,
    private val gameHighscoreRepository: GameHighscoreRepository,
    gameSaveRepository: GameSaveRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CrosswordState())
    val state = _state.asStateFlow()

    private val saveSession = GameSaveSession(
        game = GameId.CROSSWORD,
        serializer = CrosswordSnapshot.serializer(),
        schemaVersion = CROSSWORD_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )
    /** The screen shows the loading state until this is true, so a restored puzzle is not preceded by the language chooser. */
    val restoreChecked = saveSession.restoreChecked

    private var timerJob: Job? = null

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = ::onAppBackgrounded)
        viewModelScope.launch {
            AppLifecycleManager.appResumedEvent.collect { resumeTimerIfNeeded() }
        }
    }

    fun onAction(action: CrosswordAction) {
        when (action) {
            is CrosswordAction.SelectLanguage -> loadPuzzle(action.language)
            CrosswordAction.RetryLoad -> _state.value.language?.let { loadPuzzle(it) }
            CrosswordAction.NewPuzzle -> _state.value.language?.let { loadPuzzle(it) }
            CrosswordAction.LeaveGame -> persist()
            CrosswordAction.RestartGame -> restartGame()
            is CrosswordAction.CellTapped -> onCellTapped(action.index)
            is CrosswordAction.KeyPressed -> onKeyPressed(action.letter)
            CrosswordAction.Backspace -> onBackspace()
            CrosswordAction.NextClue -> moveClue(1)
            CrosswordAction.PreviousClue -> moveClue(-1)
            CrosswordAction.CheckPuzzle -> checkPuzzle()
        }
    }

    /** A fresh random puzzle — there is no limit on how many can be played. */
    private fun loadPuzzle(language: CrosswordLanguage) {
        timerJob?.cancel()
        timerJob = null
        saveSession.clear()
        _state.value = CrosswordState(language = language, isLoading = true)

        viewModelScope.launch {
            val puzzle = when (language) {
                CrosswordLanguage.GERMAN -> generateGermanCrosswordPuzzle(Random.nextLong())
                CrosswordLanguage.ENGLISH -> crosswordRepository.getRandomEnglishPuzzle()
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

    /** Wipes the entered letters and the clock but keeps the grid the player is on. */
    private fun restartGame() {
        val current = _state.value
        val puzzle = current.puzzle ?: return
        val language = current.language ?: return
        saveSession.clear()
        startWithPuzzle(language, puzzle)
    }

    /** The app went to background: stop counting time and keep the progress for a possible process kill. */
    private fun onAppBackgrounded() {
        timerJob?.cancel()
        timerJob = null
        persist()
    }

    private fun resumeTimerIfNeeded() {
        val current = _state.value
        if (current.puzzle != null && !current.isSolved && timerJob == null) startTimer()
    }

    private fun persist() = saveSession.persist(difficultyFor(_state.value.language), snapshotOrNull())

    /**
     * Null until a puzzle is loaded or while no letter was entered (only the clock ran), so an untouched
     * puzzle is dropped and the next visit starts at the language chooser. Solved puzzles are kept so the
     * result survives leaving the screen.
     */
    private fun snapshotOrNull(): CrosswordSnapshot? {
        val current = _state.value
        val puzzle = current.puzzle ?: return null
        val language = current.language ?: return null
        if (current.entries.all { it == null }) return null
        return CrosswordSnapshot(
            language = language,
            puzzle = puzzle,
            entries = current.entries,
            selectedCell = current.selectedCell,
            direction = current.direction,
            isSolved = current.isSolved,
            elapsedMillis = current.elapsedMillis,
        )
    }

    /** Brings the saved progress back; an unsolved puzzle starts counting again right away. */
    private fun restore(save: GameSave<CrosswordSnapshot>) {
        val data = save.data
        timerJob?.cancel()
        timerJob = null
        _state.value = CrosswordState(
            language = data.language,
            puzzle = data.puzzle,
            entries = data.entries,
            selectedCell = data.selectedCell,
            direction = data.direction,
            isSolved = data.isSolved,
            elapsedMillis = data.elapsedMillis,
        )
        if (!data.isSolved) startTimer()
    }

    /** The leaderboard difficulty encodes the puzzle language (LOW = German, HIGH = English). */
    private fun difficultyFor(language: CrosswordLanguage?): GameDifficulty = when (language) {
        CrosswordLanguage.GERMAN -> GameDifficulty.LOW
        else -> GameDifficulty.HIGH
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
            submitScore(puzzle)
            // Keep the solved puzzle so coming back shows the result until a new one is started
            persist()
        }
    }

    /**
     * Puzzles are drawn at random and vary in size, so the score is the number of
     * letters the grid took to fill; the solve time ranks equally sized grids.
     */
    private fun submitScore(puzzle: CrosswordPuzzle) {
        val current = _state.value
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.CROSSWORD,
                difficulty = difficultyFor(current.language),
                score = puzzle.letterCellCount.toLong(),
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
        // viewModelScope is already cancelled here; the write runs on the application scope
        persist()
    }
}
