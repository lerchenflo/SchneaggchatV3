package org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz

import org.jetbrains.compose.resources.StringResource

/**
 * A scored quiz question about a feature the app has.
 *
 * [answers] holds the right answer first, then the wrong ones; the round shuffles them.
 * [noFeatureAnswer] claims the feature doesn't exist: picking it means the player didn't know the
 * feature, which is different from getting a detail wrong.
 * [explanation] is shown after the answer: a short note on the feature and where to find it.
 * [seedKey] links the question to the feature's seeded entry on the feedback board
 * ([org.lerchenflo.schneaggchatv3mp.feedback.domain.FeedbackEntry.seedKey]), whose vote buttons the
 * player rates the feature with.
 */
data class FeatureQuizQuestion(
    val seedKey: String,
    val question: StringResource,
    val answers: List<StringResource>,
    val noFeatureAnswer: StringResource,
    val explanation: StringResource,
)
