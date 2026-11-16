package org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz

import org.jetbrains.compose.resources.StringResource

/**
 * A scored quiz question about a feature the app has.
 *
 * [answers] holds the right answer first, then the wrong ones; the round shuffles them.
 * [noFeatureAnswer] claims the feature doesn't exist: picking it means the player didn't know the
 * feature, which is different from getting a detail wrong.
 * [explanation] is shown after the answer: a short note on the feature and where to find it.
 * [seedTitle] links the question to the feature's seeded entry on the feedback board, whose vote
 * buttons the player rates the feature with. Prototype: matched by the entry's title until the
 * server sends the seed key.
 */
data class FeatureQuizQuestion(
    val seedTitle: String,
    val question: StringResource,
    val answers: List<StringResource>,
    val noFeatureAnswer: StringResource,
    val explanation: StringResource,
)
