package org.lerchenflo.schneaggchatv3mp.games.domain.featurequiz

/** How well the player knew the app in a round, for the message on the result screen. */
enum class FeatureQuizResultTier {
    GREAT,
    GOOD,
    LOW;

    companion object {
        private const val GREAT_SHARE = 0.8f
        private const val GOOD_SHARE = 0.5f

        fun of(points: Int, questionCount: Int): FeatureQuizResultTier {
            if (questionCount == 0) return LOW
            val share = points.toFloat() / questionCount
            return when {
                share >= GREAT_SHARE -> GREAT
                share >= GOOD_SHARE -> GOOD
                else -> LOW
            }
        }
    }
}
