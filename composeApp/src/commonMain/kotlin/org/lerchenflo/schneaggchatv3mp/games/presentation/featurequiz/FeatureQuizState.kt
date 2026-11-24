package org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz

import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizCard
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizOption

/** Scored quiz, or only voting on every feature without a vote (no questions, no highscore). */
enum class FeatureQuizMode { QUIZ, VOTE }

data class FeatureQuizState(
    /** Null on the start page, where the player picks the mode. */
    val mode: FeatureQuizMode? = null,
    val isLoading: Boolean = true,
    val cards: List<FeatureQuizCard> = emptyList(),
    val currentIndex: Int = 0,
    /** Answer picked on the current question card, null while the player is still answering. */
    val pickedOption: FeatureQuizOption? = null,
    /** Vote chosen on the current card, sent when the player goes on. */
    val pendingVote: FeedbackVote = FeedbackVote.NONE,
    val points: Int = 0,
    /** Answering time of the round; stands still while the player votes. */
    val elapsedMillis: Long = 0L,
    val isFinished: Boolean = false,
    /** Cards left without a vote; only the vote-only mode can skip. */
    val skippedCount: Int = 0,
) {
    val isChoosingMode: Boolean get() = mode == null

    val currentCard: FeatureQuizCard? get() = cards.getOrNull(currentIndex)

    /** Vote-only mode: every card starts without a vote, so each one not skipped got one. */
    val votedCount: Int get() = currentIndex - skippedCount

    val canSkip: Boolean get() = mode == FeatureQuizMode.VOTE && currentCard != null

    val questionCount: Int get() = cards.count { it is FeatureQuizCard.Question }

    /** A card is on screen: mode picked, not loading, not empty, round not over. */
    val isPlaying: Boolean get() = !isChoosingMode && !isLoading && !isFinished && currentCard != null

    val isLastCard: Boolean get() = currentIndex == cards.lastIndex

    /** A question is on screen and not answered yet: the only time the timer runs. */
    val isAnswering: Boolean
        get() = currentCard is FeatureQuizCard.Question && pickedOption == null

    /** The vote buttons show after an answer and on a wish, unless the entry already has a vote. */
    val showsVoteRow: Boolean
        get() = currentCard?.needsVote == true && !isAnswering

    /** Every card asks for a vote before it can be left; an existing vote counts. */
    val canContinue: Boolean
        get() = currentCard != null && !isAnswering && (!showsVoteRow || pendingVote != FeedbackVote.NONE)
}

sealed interface FeatureQuizAction {
    data object OnBackClick : FeatureQuizAction
    data class OnModeClick(val mode: FeatureQuizMode) : FeatureQuizAction
    data class OnOptionClick(val option: FeatureQuizOption) : FeatureQuizAction
    data class OnVoteClick(val vote: FeedbackVote) : FeatureQuizAction
    data object OnNextClick : FeatureQuizAction
    data object OnSkipClick : FeatureQuizAction
    data object OnPlayAgainClick : FeatureQuizAction
}
