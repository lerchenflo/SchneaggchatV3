package org.lerchenflo.schneaggchatv3mp.games.presentation.wordle

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
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.data.WordleRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.domain.WORDLE_MAX_GUESSES
import org.lerchenflo.schneaggchatv3mp.games.domain.WORDLE_WORD_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleGuess
import org.lerchenflo.schneaggchatv3mp.games.domain.WordleLanguage
import org.lerchenflo.schneaggchatv3mp.games.domain.WordlePuzzle
import org.lerchenflo.schneaggchatv3mp.games.domain.evaluateWordleGuess
import org.lerchenflo.schneaggchatv3mp.games.domain.wordleScoreFor
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import kotlin.random.Random

class WordleViewModel(
    private val wordleRepository: WordleRepository,
    private val gameHighscoreRepository: GameHighscoreRepository,
    gameSaveRepository: GameSaveRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WordleState())
    val state = _state.asStateFlow()

    private val saveSession = GameSaveSession(
        game = GameId.WORDLE,
        serializer = WordleSnapshot.serializer(),
        schemaVersion = WORDLE_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )
    /** The screen shows the loading state until this is true, so a restored game is not preceded by the language chooser. */
    val restoreChecked = saveSession.restoreChecked

    private var timerJob: Job? = null
    /** Accepted English guesses; null means "not available", then any five letters pass. */
    private var englishGuessList: Set<String>? = null

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = ::onAppBackgrounded)
        viewModelScope.launch {
            AppLifecycleManager.appResumedEvent.collect { resumeTimerIfNeeded() }
        }
    }

    fun onAction(action: WordleAction) {
        when (action) {
            is WordleAction.SelectLanguage -> loadPuzzle(action.language)
            WordleAction.RetryLoad -> _state.value.language?.let { loadPuzzle(it) }
            WordleAction.NewWord -> _state.value.language?.let { loadPuzzle(it) }
            WordleAction.LeaveGame -> persist()
            is WordleAction.KeyPressed -> onKeyPressed(action.letter)
            WordleAction.Backspace -> onBackspace()
            WordleAction.SubmitGuess -> submitGuess()
        }
    }

    /** A fresh random word — there is no limit on how many can be played. */
    private fun loadPuzzle(language: WordleLanguage) {
        timerJob?.cancel()
        timerJob = null
        saveSession.clear()
        _state.value = WordleState(language = language, isLoading = true)

        viewModelScope.launch {
            val puzzle = when (language) {
                WordleLanguage.GERMAN -> randomGermanPuzzle()
                WordleLanguage.ENGLISH -> wordleRepository.getRandomEnglishPuzzle()
            }
            if (puzzle == null) {
                _state.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }
            startWithPuzzle(puzzle)
        }
    }

    private fun randomGermanPuzzle(): WordlePuzzle = WordlePuzzle(
        language = WordleLanguage.GERMAN,
        solution = germanWordleWords[Random.nextInt(germanWordleWords.size)],
    )

    private fun startWithPuzzle(puzzle: WordlePuzzle) {
        _state.value = WordleState(language = puzzle.language, puzzle = puzzle)
        startTimer()
        if (puzzle.language == WordleLanguage.ENGLISH) loadEnglishGuessList()
    }

    /** Fetched alongside the game so a typo can be rejected instead of costing a try. */
    private fun loadEnglishGuessList() {
        if (englishGuessList != null) return
        viewModelScope.launch {
            englishGuessList = wordleRepository.getEnglishGuessList()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (!_state.value.isFinished) {
                    _state.update { it.copy(elapsedMillis = it.elapsedMillis + 1000) }
                }
            }
        }
    }

    /** The app went to background: stop counting time and keep the progress for a possible process kill. */
    private fun onAppBackgrounded() {
        timerJob?.cancel()
        timerJob = null
        persist()
    }

    private fun resumeTimerIfNeeded() {
        val current = _state.value
        if (current.puzzle != null && !current.isFinished && timerJob == null) startTimer()
    }

    private fun persist() = saveSession.persist(difficultyFor(_state.value.language), snapshotOrNull())

    /**
     * Null until a word is loaded or while no guess was submitted (only the clock ran), so an untouched
     * game is dropped and the next visit starts at the language chooser. Finished games are kept so the
     * result survives leaving the screen.
     */
    private fun snapshotOrNull(): WordleSnapshot? {
        val current = _state.value
        val puzzle = current.puzzle ?: return null
        if (current.guesses.isEmpty()) return null
        return WordleSnapshot(
            puzzle = puzzle,
            guesses = current.guesses,
            isSolved = current.isSolved,
            isFailed = current.isFailed,
            elapsedMillis = current.elapsedMillis,
        )
    }

    /** Brings the saved progress back; an unfinished game starts counting again right away. */
    private fun restore(save: GameSave<WordleSnapshot>) {
        val data = save.data
        timerJob?.cancel()
        timerJob = null
        _state.value = WordleState(
            language = data.puzzle.language,
            puzzle = data.puzzle,
            guesses = data.guesses,
            isSolved = data.isSolved,
            isFailed = data.isFailed,
            elapsedMillis = data.elapsedMillis,
        )
        if (!data.isSolved && !data.isFailed) {
            startTimer()
            if (data.puzzle.language == WordleLanguage.ENGLISH) loadEnglishGuessList()
        }
    }

    /** The leaderboard difficulty encodes the word language (LOW = German, HIGH = English). */
    private fun difficultyFor(language: WordleLanguage?): GameDifficulty = when (language) {
        WordleLanguage.GERMAN -> GameDifficulty.LOW
        else -> GameDifficulty.HIGH
    }

    private fun onKeyPressed(letter: Char) {
        val current = _state.value
        if (current.puzzle == null || current.isFinished) return
        if (current.currentInput.length >= WORDLE_WORD_LENGTH) return
        if (!letter.isLetter()) return

        val input = current.currentInput + letter.uppercaseChar()
        _state.update { it.copy(currentInput = input, inputError = null) }

        // A complete row is checked right away — no extra confirmation step.
        // A rejected word stays in the row so it can be corrected with backspace.
        if (input.length == WORDLE_WORD_LENGTH) submitGuess()
    }

    private fun onBackspace() {
        val current = _state.value
        if (current.puzzle == null || current.isFinished || current.currentInput.isEmpty()) return
        _state.update { it.copy(currentInput = it.currentInput.dropLast(1), inputError = null) }
    }

    private fun submitGuess() {
        val current = _state.value
        val puzzle = current.puzzle ?: return
        if (current.isFinished) return

        val guess = current.currentInput
        if (guess.length != WORDLE_WORD_LENGTH) {
            rejectGuess(guess, WordleInputError.TOO_SHORT)
            return
        }
        // German has no dictionary (the local list is a solution pool, not a word list),
        // so only English guesses are checked — and only when the list was fetched.
        // The solution always passes: the public list predates newer NYT words, and a
        // solution missing from it would otherwise be impossible to enter.
        val allowed = englishGuessList
        if (puzzle.language == WordleLanguage.ENGLISH &&
            allowed != null &&
            guess !in allowed &&
            guess != puzzle.solution
        ) {
            rejectGuess(guess, WordleInputError.UNKNOWN_WORD)
            return
        }

        val evaluated = WordleGuess(
            word = guess,
            states = evaluateWordleGuess(guess, puzzle.solution),
        )
        val guesses = current.guesses + evaluated
        val solved = guess == puzzle.solution
        val failed = !solved && guesses.size >= WORDLE_MAX_GUESSES

        _state.update {
            it.copy(
                guesses = guesses,
                currentInput = "",
                inputError = null,
                isSolved = solved,
                isFailed = failed,
            )
        }

        if (solved || failed) {
            timerJob?.cancel()
            timerJob = null
            if (solved) submitScore(guesses.size)
            // Keep the finished game so coming back shows the result until a new word is started
            persist()
        }
    }

    private fun rejectGuess(word: String, error: WordleInputError) {
        _state.update {
            it.copy(inputError = error, rejectedWord = word, errorNonce = it.errorNonce + 1)
        }
    }

    private fun submitScore(guessesUsed: Int) {
        val current = _state.value
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.WORDLE,
                difficulty = difficultyFor(current.language),
                score = wordleScoreFor(guessesUsed),
                timeMillis = current.elapsedMillis,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        // viewModelScope is already cancelled here; the write runs on the application scope
        persist()
    }
}
