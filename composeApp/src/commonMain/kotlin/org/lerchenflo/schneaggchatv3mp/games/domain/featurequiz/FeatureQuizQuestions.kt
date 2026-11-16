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
 * The questions of the feature quiz. The first answer is the right one. [FeatureQuizQuestion.seedTitle]
 * must match the title of the feature's entry in the server's feedback seed file
 * (`seed/feedback_features.json`), or the question is left out of every round.
 */
object FeatureQuizQuestions {
    val ALL: List<FeatureQuizQuestion> = listOf(
        FeatureQuizQuestion(
            seedTitle = "Umfragen mit eigenen Antworten, Unterumfragen und Stimmenlimit",
            question = Res.string.featurequiz_polls_subpoll_question,
            answers = listOf(Res.string.featurequiz_polls_subpoll_answer_1, Res.string.featurequiz_polls_subpoll_answer_2, Res.string.featurequiz_polls_subpoll_answer_3),
            noFeatureAnswer = Res.string.featurequiz_polls_subpoll_none,
            explanation = Res.string.featurequiz_polls_subpoll_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Vorarlbergerisch als App-Sprache",
            question = Res.string.featurequiz_language_question,
            answers = listOf(Res.string.featurequiz_language_answer_1, Res.string.featurequiz_language_answer_2, Res.string.featurequiz_language_answer_3),
            noFeatureAnswer = Res.string.featurequiz_language_none,
            explanation = Res.string.featurequiz_language_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Freunde aufwecken",
            question = Res.string.featurequiz_wake_question,
            answers = listOf(Res.string.featurequiz_wake_answer_1, Res.string.featurequiz_wake_answer_2, Res.string.featurequiz_wake_answer_3),
            noFeatureAnswer = Res.string.featurequiz_wake_none,
            explanation = Res.string.featurequiz_wake_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Events mit Kalender, Erinnerungen und Teilnehmern",
            question = Res.string.featurequiz_event_reminders_question,
            answers = listOf(Res.string.featurequiz_event_reminders_answer_1, Res.string.featurequiz_event_reminders_answer_2, Res.string.featurequiz_event_reminders_answer_3),
            noFeatureAnswer = Res.string.featurequiz_event_reminders_none,
            explanation = Res.string.featurequiz_event_reminders_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Events mit Kalender, Erinnerungen und Teilnehmern",
            question = Res.string.featurequiz_event_title_question,
            answers = listOf(Res.string.featurequiz_event_title_answer_1, Res.string.featurequiz_event_title_answer_2, Res.string.featurequiz_event_title_answer_3),
            noFeatureAnswer = Res.string.featurequiz_event_title_none,
            explanation = Res.string.featurequiz_event_title_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Wordle",
            question = Res.string.featurequiz_wordle_question,
            answers = listOf(Res.string.featurequiz_wordle_answer_1, Res.string.featurequiz_wordle_answer_2, Res.string.featurequiz_wordle_answer_3),
            noFeatureAnswer = Res.string.featurequiz_wordle_none,
            explanation = Res.string.featurequiz_wordle_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Tic Tac Toe",
            question = Res.string.featurequiz_tictactoe_question,
            answers = listOf(Res.string.featurequiz_tictactoe_answer_1, Res.string.featurequiz_tictactoe_answer_2, Res.string.featurequiz_tictactoe_answer_3),
            noFeatureAnswer = Res.string.featurequiz_tictactoe_none,
            explanation = Res.string.featurequiz_tictactoe_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Dartcounter",
            question = Res.string.featurequiz_dartcounter_question,
            answers = listOf(Res.string.featurequiz_dartcounter_answer_1, Res.string.featurequiz_dartcounter_answer_2, Res.string.featurequiz_dartcounter_answer_3),
            noFeatureAnswer = Res.string.featurequiz_dartcounter_none,
            explanation = Res.string.featurequiz_dartcounter_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "App-kaputt-Button",
            question = Res.string.featurequiz_app_broken_question,
            answers = listOf(Res.string.featurequiz_app_broken_answer_1, Res.string.featurequiz_app_broken_answer_2, Res.string.featurequiz_app_broken_answer_3),
            noFeatureAnswer = Res.string.featurequiz_app_broken_none,
            explanation = Res.string.featurequiz_app_broken_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Spitznamen",
            question = Res.string.featurequiz_nicknames_question,
            answers = listOf(Res.string.featurequiz_nicknames_answer_1, Res.string.featurequiz_nicknames_answer_2, Res.string.featurequiz_nicknames_answer_3),
            noFeatureAnswer = Res.string.featurequiz_nicknames_none,
            explanation = Res.string.featurequiz_nicknames_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Chat-Erweiterungen für Gruppen",
            question = Res.string.featurequiz_group_extensions_question,
            answers = listOf(Res.string.featurequiz_group_extensions_answer_1, Res.string.featurequiz_group_extensions_answer_2, Res.string.featurequiz_group_extensions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_group_extensions_none,
            explanation = Res.string.featurequiz_group_extensions_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Nachrichten-Reaktionen (auch mit Text)",
            question = Res.string.featurequiz_reactions_question,
            answers = listOf(Res.string.featurequiz_reactions_answer_1, Res.string.featurequiz_reactions_answer_2, Res.string.featurequiz_reactions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_reactions_none,
            explanation = Res.string.featurequiz_reactions_explanation,
        ),
        FeatureQuizQuestion(
            seedTitle = "Anpassbare Schnellreaktionen",
            question = Res.string.featurequiz_quick_reactions_question,
            answers = listOf(Res.string.featurequiz_quick_reactions_answer_1, Res.string.featurequiz_quick_reactions_answer_2, Res.string.featurequiz_quick_reactions_answer_3),
            noFeatureAnswer = Res.string.featurequiz_quick_reactions_none,
            explanation = Res.string.featurequiz_quick_reactions_explanation,
        ),
    )
}
