package org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz

import org.jetbrains.compose.resources.StringResource
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackStatus
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackType
import org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackVote
import kotlin.random.Random

/** What a picked answer says about the player's knowledge of the feature. */
enum class FeatureQuizAnswerResult {
    CORRECT,

    /** Wrong detail, but the player knows the feature exists. */
    WRONG_DETAIL,

    /** The player claimed the feature doesn't exist. */
    DIDNT_KNOW,
}

data class FeatureQuizOption(
    val text: StringResource,
    val result: FeatureQuizAnswerResult,
)

/** One step of a round. Every card belongs to a feedback board entry the player can vote on. */
sealed interface FeatureQuizCard {
    val entry: FeedbackEntry

    /** Scored question about a feature the app has, its options already shuffled. */
    data class Question(
        val question: StringResource,
        val options: List<FeatureQuizOption>,
        val explanation: StringResource,
        override val entry: FeedbackEntry,
    ) : FeatureQuizCard {
        val correctOption: FeatureQuizOption
            get() = options.first { it.result == FeatureQuizAnswerResult.CORRECT }
    }

    /** Unscored: an open wish from the board, shown only so the player votes on it. */
    data class Wish(override val entry: FeedbackEntry) : FeatureQuizCard

    /** An existing vote is never asked for or changed again, also not in a later round. */
    val needsVote: Boolean get() = entry.myVote == FeedbackVote.NONE
}

/** The vote preselected after an answer: "didn't know" when the player claimed the feature doesn't exist. */
fun FeatureQuizCard.initialVote(picked: FeatureQuizOption?): FeedbackVote = when {
    !needsVote -> entry.myVote
    picked?.result == FeatureQuizAnswerResult.DIDNT_KNOW -> FeedbackVote.DIDNT_KNOW
    else -> FeedbackVote.NONE
}

object FeatureQuizRounds {
    const val QUESTIONS_PER_ROUND = 10
    const val WISHES_PER_ROUND = 3

    /**
     * A new round from the current board: up to [QUESTIONS_PER_ROUND] questions whose feature is on
     * the board (at most one per entry, so no entry is rated twice) and up to [WISHES_PER_ROUND]
     * open wishes the player hasn't voted on yet, all in random order.
     */
    fun build(
        entries: List<FeedbackEntry>,
        questions: List<FeatureQuizQuestion>,
        random: Random = Random,
    ): List<FeatureQuizCard> {
        val seededByKey = entries.filter { it.seedKey != null }.associateBy { it.seedKey }
        val questionCards = questions.shuffled(random)
            .mapNotNull { question -> seededByKey[question.seedKey]?.let { question.toCard(it, random) } }
            .distinctBy { it.entry.id }
            .take(QUESTIONS_PER_ROUND)
        val wishCards = entries.filter { it.isOpenWish() && it.myVote == FeedbackVote.NONE }
            .shuffled(random)
            .take(WISHES_PER_ROUND)
            .map { FeatureQuizCard.Wish(it) }
        return (questionCards + wishCards).shuffled(random)
    }

    private fun FeatureQuizQuestion.toCard(entry: FeedbackEntry, random: Random): FeatureQuizCard.Question {
        val answerOptions = answers.mapIndexed { index, text ->
            FeatureQuizOption(
                text = text,
                result = if (index == 0) FeatureQuizAnswerResult.CORRECT else FeatureQuizAnswerResult.WRONG_DETAIL,
            )
        }
        val noFeatureOption = FeatureQuizOption(noFeatureAnswer, FeatureQuizAnswerResult.DIDNT_KNOW)
        return FeatureQuizCard.Question(
            question = question,
            // "Doesn't exist" always comes last, like a "none of these" option
            options = answerOptions.shuffled(random) + noFeatureOption,
            explanation = explanation,
            entry = entry,
        )
    }

    /** Someone else's feature wish that isn't built yet. */
    private fun FeedbackEntry.isOpenWish(): Boolean =
        type == FeedbackType.FEATURE &&
            !isOwn &&
            (status == FeedbackStatus.REQUESTED || status == FeedbackStatus.PLANNED)
}
