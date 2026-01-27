package org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi

import kotlinx.serialization.Serializable

enum class YatziCategory(val displayName: String, val isUpper: Boolean) {
    ONES("Ones", true),
    TWOS("Twos", true),
    THREES("Threes", true),
    FOURS("Fours", true),
    FIVES("Fives", true),
    SIXES("Sixes", true),
    
    // Sum and Bonus are calculated properties, not selectable categories for scoring directly in the same way, 
    // but useful to enumerate if we want to display rows.
    // However, for "selectable" categories, we stick to the ones the user clicks.
    
    ONE_PAIR("One Pair", false),
    TWO_PAIRS("Two Pairs", false),
    THREE_OF_A_KIND("Three of a Kind", false),
    FOUR_OF_A_KIND("Four of a Kind", false),
    SMALL_STRAIGHT("Small Straight", false),
    LARGE_STRAIGHT("Large Straight", false),
    FULL_HOUSE("Full House", false),
    CHANCE("Chance", false),
    YAHTZEE("Yahtzee", false);

    companion object {
        val selectable = entries
    }
}

data class YatziDie(
    val value: Int = 1,
    val isKept: Boolean = false
)

data class YatziPlayer(
    val name: String,
    val scores: Map<YatziCategory, Int> = emptyMap()
) {
    val upperScore: Int get() = scores.filterKeys { it.isUpper }.values.sum()
    val bonus: Int get() = if (upperScore >= 63) 35 else 0
    val lowerScore: Int get() = scores.filterKeys { !it.isUpper }.values.sum()
    val totalScore: Int get() = upperScore + bonus + lowerScore
}

data class YatziState(
    val players: List<YatziPlayer> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val currentRollCount: Int = 0, // 0 means haven't rolled yet. 1, 2, 3 allowed.
    val dice: List<YatziDie> = List(5) { YatziDie() },
    val gameStarted: Boolean = false,
    val winner: YatziPlayer? = null,
    val potentialScores: Map<YatziCategory, Int> = emptyMap(),
    val showPlayerSelector: Boolean = false
) {
    val currentPlayer: YatziPlayer? get() = players.getOrNull(currentPlayerIndex)
    val canRoll: Boolean get() = currentRollCount < 3 && winner == null
    val canScore: Boolean get() = currentRollCount > 0 && winner == null
}
