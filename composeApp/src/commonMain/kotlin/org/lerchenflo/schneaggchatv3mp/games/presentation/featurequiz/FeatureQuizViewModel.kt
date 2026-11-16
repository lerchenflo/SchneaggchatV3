package org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.onError
import org.lerchenflo.schneaggchatv3mp.app.domain.sendError
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackRepository
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizAnswerResult
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizOption
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizQuestions
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizRounds
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.initialVote
import kotlin.time.TimeSource

/**
 * Feature quiz: questions about the app's features plus open wishes from the feedback board. After
 * every card the player votes on its board entry with the board's own votes. At the end of a round
 * the correct answers go on the leaderboard, the answering time breaks ties.
 */
class FeatureQuizViewModel(
    private val feedbackRepository: FeedbackRepository,
    private val gameHighscoreRepository: GameHighscoreRepository,
    private val navigator: Navigator,
    private val applicationScope: ApplicationScope,
    private val errorChannel: ErrorChannel,
) : ViewModel() {

    private val _state = MutableStateFlow(FeatureQuizState())
    val state = _state.asStateFlow()

    private var timerJob: Job? = null
    private var answeringSince: TimeSource.Monotonic.ValueTimeMark? = null
    private var elapsedBeforeAnswering = 0L

    init {
        viewModelScope.launch {
            // Offline the round is built from the stored board
            feedbackRepository.syncFeedback()
            startRound()
        }
    }

    fun onAction(action: FeatureQuizAction) {
        when (action) {
            FeatureQuizAction.OnBackClick -> viewModelScope.launch { navigator.navigateBack() }
            is FeatureQuizAction.OnOptionClick -> pickOption(action.option)
            is FeatureQuizAction.OnVoteClick -> _state.update {
                // Tapping the chosen vote again clears it, like on the board
                it.copy(pendingVote = if (it.pendingVote == action.vote) FeedbackVote.NONE else action.vote)
            }
            FeatureQuizAction.OnNextClick -> goToNextCard()
            FeatureQuizAction.OnPlayAgainClick -> viewModelScope.launch { startRound() }
        }
    }

    private suspend fun startRound() {
        stopTimer()
        val entries = feedbackRepository.getEntriesFlow().first()
        _state.value = FeatureQuizState(
            isLoading = false,
            cards = FeatureQuizRounds.build(entries, FeatureQuizQuestions.ALL),
        )
        showCard()
    }

    private fun pickOption(option: FeatureQuizOption) {
        val current = _state.value
        val card = current.currentCard ?: return
        if (!current.isAnswering) return
        stopTimer()
        _state.update {
            it.copy(
                pickedOption = option,
                pendingVote = card.initialVote(option),
                points = it.points + if (option.result == FeatureQuizAnswerResult.CORRECT) 1 else 0,
            )
        }
    }

    private fun goToNextCard() {
        val current = _state.value
        val card = current.currentCard ?: return
        if (!current.canContinue) return
        if (current.showsVoteRow) sendVote(card.entry, current.pendingVote)

        val nextIndex = current.currentIndex + 1
        _state.update {
            it.copy(
                currentIndex = nextIndex,
                pickedOption = null,
                pendingVote = FeedbackVote.NONE,
                isFinished = nextIndex >= it.cards.size,
            )
        }
        if (_state.value.isFinished) submitScore() else showCard()
    }

    /** A round of wishes only asked nothing, so it doesn't count. */
    private fun submitScore() {
        val finished = _state.value
        if (finished.questionCount == 0) return
        // Runs on after the screen closes, like the votes
        applicationScope.launchOnMain {
            gameHighscoreRepository.submitScore(
                game = GameId.FEATURE_QUIZ,
                difficulty = GameDifficulty.MEDIUM,
                score = finished.points.toLong(),
                timeMillis = finished.elapsedMillis,
            )
        }
    }

    /** Starts the timer for a question; on a wish the player only votes, so it stays paused. */
    private fun showCard() {
        if (_state.value.isAnswering) startTimer()
    }

    private fun sendVote(entry: FeedbackEntry, vote: FeedbackVote) {
        // Runs on after the screen closes; the repository stores the vote at once and the server answer after it
        applicationScope.launchOnMain {
            feedbackRepository.vote(entry, vote).onError { errorChannel.sendError(it) }
        }
    }

    private fun startTimer() {
        stopTimer()
        val since = TimeSource.Monotonic.markNow()
        answeringSince = since
        elapsedBeforeAnswering = _state.value.elapsedMillis
        timerJob = viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(elapsedMillis = elapsedBeforeAnswering + since.elapsedNow().inWholeMilliseconds) }
                delay(TIMER_TICK_MILLIS)
            }
        }
    }

    /** Stops the timer and books the exact time since the last tick. */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        val since = answeringSince ?: return
        answeringSince = null
        _state.update { it.copy(elapsedMillis = elapsedBeforeAnswering + since.elapsedNow().inWholeMilliseconds) }
    }

    private companion object {
        const val TIMER_TICK_MILLIS = 100L
    }
}
