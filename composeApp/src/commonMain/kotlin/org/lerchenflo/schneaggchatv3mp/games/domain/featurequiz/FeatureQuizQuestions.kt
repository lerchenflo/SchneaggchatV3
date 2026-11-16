package org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz

import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_app_broken_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_dartcounter_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_reminders_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_event_title_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_group_extensions_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_language_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_nicknames_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_polls_subpoll_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_quick_reactions_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_reactions_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_tictactoe_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wake_question
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_answer_1
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_answer_2
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_answer_3
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_explanation
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_none
import schneaggchatv3mp.composeapp.generated.resources.featurequiz_wordle_question

/**
 * The questions of the feature quiz. The first answer is the right one. [FeatureQuizQuestion.seedKey]
 * must be a `seedKey` of the server's feedback seed file (`seed/feedback_features.json`), or the
 * question is left out of every round.
 */
object FeatureQuizQuestions {
    val ALL: List<FeatureQuizQuestion> = listOf(
        FeatureQuizQuestion(
            seedKey = "polls-custom-answers",
            question = Res.string.featurequiz_polls_subpoll_question,
            answers = listOf(Res.string.featurequiz_polls_subpoll_answer_1, Res.string.featurequiz_polls_subpoll_answer_2, Res.string.featurequiz_polls_subpoll_answer_3),
            noFeatureAnswer = Res.string.featurequiz_polls_subpoll_none,
            explanation = Res.string.featurequiz_polls_subpoll_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "language-vorarlbergerisch",
            question = Res.string.featurequiz_language_question,
            answers = listOf(Res.string.featurequiz_language_answer_1, Res.string.featurequiz_language_answer_2, Res.string.featurequiz_language_answer_3),
            noFeatureAnswer = Res.string.featurequiz_language_none,
            explanation = Res.string.featurequiz_language_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "wake-feature",
            question = Res.string.featurequiz_wake_question,
            answers = listOf(Res.string.featurequiz_wake_answer_1, Res.string.featurequiz_wake_answer_2, Res.string.featurequiz_wake_answer_3),
            noFeatureAnswer = Res.string.featurequiz_wake_none,
            explanation = Res.string.featurequiz_wake_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "events",
            question = Res.string.featurequiz_event_reminders_question,
            answers = listOf(Res.string.featurequiz_event_reminders_answer_1, Res.string.featurequiz_event_reminders_answer_2, Res.string.featurequiz_event_reminders_answer_3),
            noFeatureAnswer = Res.string.featurequiz_event_reminders_none,
            explanation = Res.string.featurequiz_event_reminders_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "events",
            question = Res.string.featurequiz_event_title_question,
            answers = listOf(Res.string.featurequiz_event_title_answer_1, Res.string.featurequiz_event_title_answer_2, Res.string.featurequiz_event_title_answer_3),
            noFeatureAnswer = Res.string.featurequiz_event_title_none,
            explanation = Res.string.featurequiz_event_title_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-wordle",
            question = Res.string.featurequiz_wordle_question,
            answers = listOf(Res.string.featurequiz_wordle_answer_1, Res.string.featurequiz_wordle_answer_2, Res.string.featurequiz_wordle_answer_3),
            noFeatureAnswer = Res.string.featurequiz_wordle_none,
            explanation = Res.string.featurequiz_wordle_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-tictactoe",
            question = Res.string.featurequiz_tictactoe_question,
            answers = listOf(Res.string.featurequiz_tictactoe_answer_1, Res.string.featurequiz_tictactoe_answer_2, Res.string.featurequiz_tictactoe_answer_3),
            noFeatureAnswer = Res.string.featurequiz_tictactoe_none,
            explanation = Res.string.featurequiz_tictactoe_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "game-dartcounter",
            question = Res.string.featurequiz_dartcounter_question,
            answers = listOf(Res.string.featurequiz_dartcounter_answer_1, Res.string.featurequiz_dartcounter_answer_2, Res.string.featurequiz_dartcounter_answer_3),
            noFeatureAnswer = Res.string.featurequiz_dartcounter_none,
            explanation = Res.string.featurequiz_dartcounter_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "app-broken-button",
            question = Res.string.featurequiz_app_broken_question,
            answers = listOf(Res.string.featurequiz_app_broken_answer_1, Res.string.featurequiz_app_broken_answer_2, Res.string.featurequiz_app_broken_answer_3),
            noFeatureAnswer = Res.string.featurequiz_app_broken_none,
            explanation = Res.string.featurequiz_app_broken_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "nicknames",
            question = Res.string.featurequiz_nicknames_question,
            answers = listOf(Res.string.featurequiz_nicknames_answer_1, Res.string.featurequiz_nicknames_answer_2, Res.string.featurequiz_nicknames_answer_3),
            noFeatureAnswer = Res.string.featurequiz_nicknames_none,
            explanation = Res.string.featurequiz_nicknames_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "group-extensions",
            question = Res.string.featurequiz_group_extensions_question,
            answers = listOf(Res.string.featurequiz_group_extensions_answer_1, Res.string.featurequiz_group_extensions_answer_2, Res.string.featurequiz_group_extensions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_group_extensions_none,
            explanation = Res.string.featurequiz_group_extensions_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "message-reactions",
            question = Res.string.featurequiz_reactions_question,
            answers = listOf(Res.string.featurequiz_reactions_answer_1, Res.string.featurequiz_reactions_answer_2, Res.string.featurequiz_reactions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_reactions_none,
            explanation = Res.string.featurequiz_reactions_explanation,
        ),
        FeatureQuizQuestion(
            seedKey = "quick-reactions",
            question = Res.string.featurequiz_quick_reactions_question,
            answers = listOf(Res.string.featurequiz_quick_reactions_answer_1, Res.string.featurequiz_quick_reactions_answer_2, Res.string.featurequiz_quick_reactions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_quick_reactions_none,
            explanation = Res.string.featurequiz_quick_reactions_explanation,
        ),
    )
}
