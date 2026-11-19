package org.lerchenflo.schneaggchatv3mp.games.presentation.featurequiz

import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizCard
import org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz.FeatureQuizOption

data class FeatureQuizState(
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
) {
    val currentCard: FeatureQuizCard? get() = cards.getOrNull(currentIndex)

    val questionCount: Int get() = cards.count { it is FeatureQuizCard.Question }

    /** A card is on screen: not loading, not empty, round not over. */
    val isPlaying: Boolean get() = !isLoading && !isFinished && currentCard != null

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
    data class OnOptionClick(val option: FeatureQuizOption) : FeatureQuizAction
    data class OnVoteClick(val vote: FeedbackVote) : FeatureQuizAction
    data object OnNextClick : FeatureQuizAction
    data object OnPlayAgainClick : FeatureQuizAction
}
