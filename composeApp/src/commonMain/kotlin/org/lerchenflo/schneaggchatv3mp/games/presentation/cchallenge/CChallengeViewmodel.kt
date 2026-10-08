package org.lerchenflo.schneaggchatv3mp.games.presentation.cchallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.AppLifecycleManager
import org.lerchenflo.schneaggchatv3mp.games.data.CChallengeStreak
import org.lerchenflo.schneaggchatv3mp.games.data.CChallengeStreakRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.games.presentation.awaitResume
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.today
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_daily_reset
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class CChallengeViewmodel(
    private val gameHighscoreRepository: GameHighscoreRepository,
    private val streakRepository: CChallengeStreakRepository,
    gameSaveRepository: GameSaveRepository,
) : ViewModel() {

    /** Local day the current challenge was generated for (declared before the state, which sets it). */
    private var challengeEpochDay = 0L

    private val _state = MutableStateFlow(baseState())
    val state = _state.asStateFlow()

    private val saveSession = GameSaveSession(
        game = GameId.C_CHALLENGE,
        serializer = CChallengeSnapshot.serializer(),
        schemaVersion = C_CHALLENGE_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )
    /** The screen waits for this before showing the start overlay, so a restored run is never overwritten. */
    val restoreChecked = saveSession.restoreChecked

    private var timerJob: Job? = null
    private var runStartTime = 0L
    // The clock stands still while the app is in the background
    private var inBackground = false

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = {
            inBackground = true
            persist()
        })
        viewModelScope.launch {
            AppLifecycleManager.appResumedEvent.collect {
                inBackground = false
                // Coming back from the background may be on a new day
                checkDayChanged()
            }
        }
        refreshStreak()
    }

    fun onAction(action: CChallengeAction) {
        when (action) {
            CChallengeAction.Start -> start()
            CChallengeAction.Leave -> persist()
            CChallengeAction.CheckDayChanged -> checkDayChanged()
            is CChallengeAction.SelectOption -> selectOption(action.index)
            is CChallengeAction.SelectLine -> selectLine(action.index)
            is CChallengeAction.PickOrderLine -> pickOrderLine(action.poolIndex)
            is CChallengeAction.RemoveOrderLine -> removeOrderLine(action.position)
            CChallengeAction.SubmitOrder -> submitOrder()
        }
    }

    /** Today's challenge, untouched - regenerating on the same day gives the same challenge. */
    private fun baseState(streak: CChallengeStreak = CChallengeStreak()): CChallengeState {
        challengeEpochDay = today().toEpochDays()
        return CChallengeState(
            challenge = generateDailyCChallenge(challengeEpochDay),
            streak = streak,
        )
    }

    private fun start() {
        val current = _state.value
        if (current.isStarted || current.isFinished) return
        _state.update { it.copy(isStarted = true) }
        startTimer()
        persist()
    }

    private fun startTimer(alreadyElapsed: Long = 0L) {
        timerJob?.cancel()
        runStartTime = Clock.System.now().toEpochMilliseconds() - alreadyElapsed
        timerJob = viewModelScope.launch {
            while (isActive) {
                val drift = awaitResume { inBackground }
                if (drift > 0) runStartTime += drift

                delay(200L.milliseconds)
                _state.update { it.copy(elapsedMillis = Clock.System.now().toEpochMilliseconds() - runStartTime) }
            }
        }
    }

    private fun persist() = saveSession.persist(snapshotOrNull())

    /** Null before the challenge was started; finished runs are kept so today's result stays visible. */
    private fun snapshotOrNull(): CChallengeSnapshot? {
        val current = _state.value
        if (!current.isStarted && !current.isFinished) return null
        return CChallengeSnapshot(
            isFinished = current.isFinished,
            solved = current.solved,
            triesUsed = current.triesUsed,
            wrongPicks = current.wrongPicks,
            orderPicked = current.orderPicked,
            orderCorrectCount = current.orderCorrectCount,
            elapsedMillis = current.elapsedMillis,
            score = current.score,
        )
    }

    private fun restore(save: GameSave<CChallengeSnapshot>) {
        val data = save.data
        challengeEpochDay = save.epochDay
        val challenge = generateDailyCChallenge(challengeEpochDay)
        // Drop picks that do not fit the regenerated challenge (e.g. after the pool changed in an update)
        val orderPicked = data.orderPicked.filter { it in challenge.orderPool.indices }.distinct()
        _state.update {
            it.copy(
                challenge = challenge,
                isStarted = !data.isFinished,
                isFinished = data.isFinished,
                solved = data.solved,
                triesUsed = data.triesUsed,
                wrongPicks = data.wrongPicks,
                orderPicked = orderPicked,
                orderCorrectCount = data.orderCorrectCount,
                elapsedMillis = data.elapsedMillis,
                score = data.score,
            )
        }
        if (!data.isFinished) startTimer(alreadyElapsed = data.elapsedMillis)
    }

    /** The daily challenge changed underneath a run that is still on screen: swap in today's. */
    private fun checkDayChanged() {
        if (challengeEpochDay == today().toEpochDays()) return
        val wasStarted = _state.value.isStarted && !_state.value.isFinished
        timerJob?.cancel()
        saveSession.clear()
        _state.value = baseState(_state.value.streak)
        refreshStreak()
        if (wasStarted) {
            viewModelScope.launch { SnackbarManager.showMessage(getString(Res.string.games_daily_reset)) }
        }
    }

    private fun refreshStreak() {
        viewModelScope.launch {
            val streak = streakRepository.load(today().toEpochDays())
            _state.update { it.copy(streak = streak) }
        }
    }

    private fun canAnswer(): Boolean = _state.value.let { it.isStarted && !it.isFinished }

    private fun selectOption(index: Int) {
        val current = _state.value
        if (!canAnswer() || index in current.wrongPicks) return
        if (index !in current.challenge.options.indices) return
        evaluate(correct = index == current.challenge.correctOption, wrongPick = index)
    }

    private fun selectLine(index: Int) {
        val current = _state.value
        if (!canAnswer() || current.challenge.type != CChallengeType.FIX_SYNTAX) return
        if (index in current.wrongPicks || current.challenge.codeLines.getOrNull(index).isNullOrBlank()) return
        evaluate(correct = index == current.challenge.highlightLine, wrongPick = index)
    }

    private fun pickOrderLine(poolIndex: Int) {
        if (!canAnswer()) return
        _state.update {
            if (poolIndex in it.orderPicked || poolIndex !in it.challenge.orderPool.indices) it
            else it.copy(orderPicked = it.orderPicked + poolIndex)
        }
    }

    private fun removeOrderLine(position: Int) {
        if (!canAnswer()) return
        _state.update {
            if (position !in it.orderPicked.indices) it
            else it.copy(orderPicked = it.orderPicked.toMutableList().apply { removeAt(position) })
        }
    }

    private fun submitOrder() {
        val current = _state.value
        val challenge = current.challenge
        if (!canAnswer() || current.orderPicked.size != challenge.orderPool.size) return
        // Compared without indentation: two identical "}" lines are interchangeable
        val answer = current.orderPicked.map { challenge.orderPool[it].trim() }
        val solution = challenge.orderSolution
        val correctCount = answer.indices.count { answer[it] == solution[it] }
        _state.update { it.copy(orderCorrectCount = correctCount) }
        evaluate(correct = correctCount == solution.size, wrongPick = null)
    }

    private fun evaluate(correct: Boolean, wrongPick: Int?) {
        val current = _state.value
        val triesUsed = current.triesUsed + 1
        val failed = !correct && triesUsed >= C_CHALLENGE_MAX_TRIES

        if (!correct && !failed) {
            _state.update {
                it.copy(triesUsed = triesUsed, wrongPicks = it.wrongPicks + listOfNotNull(wrongPick))
            }
            persist()
            return
        }

        timerJob?.cancel()
        val elapsedMillis = Clock.System.now().toEpochMilliseconds() - runStartTime
        val score = if (correct) C_CHALLENGE_POINTS[triesUsed - 1] else 0
        _state.update {
            it.copy(
                isStarted = false,
                isFinished = true,
                solved = correct,
                triesUsed = triesUsed,
                wrongPicks = it.wrongPicks + listOfNotNull(wrongPick),
                elapsedMillis = elapsedMillis,
                score = score,
            )
        }
        // Daily game: keep today's result so coming back shows it instead of new tries
        persist()

        val epochDay = challengeEpochDay
        viewModelScope.launch {
            streakRepository.recordResult(epochDay, solved = correct)
            val streak = streakRepository.load(today().toEpochDays())
            _state.update { it.copy(streak = streak) }
        }
        if (correct) submitScore(score, elapsedMillis)
    }

    private fun submitScore(score: Int, timeMillis: Long) {
        viewModelScope.launch {
            gameHighscoreRepository.submitScore(
                game = GameId.C_CHALLENGE,
                difficulty = GameDifficulty.MEDIUM,
                score = score.toLong(),
                timeMillis = timeMillis,
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
