package org.lerchenflo.schneaggchatv3mp.games.presentation.yatzi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class YatziViewModel : ViewModel() {
    private val _state = MutableStateFlow(YatziState())
    val state: StateFlow<YatziState> = _state.asStateFlow()

    fun addPlayer(name: String) {
        if (name.isBlank()) return
        _state.update {
            it.copy(players = it.players + YatziPlayer(name))
        }
    }

    fun resetAll() {
        _state.value = YatziState()
    }

    fun endGameToSetup() {
        val clearedPlayers = _state.value.players.map { it.copy(scores = emptyMap()) }
        _state.value = YatziState(players = clearedPlayers)
    }

    fun restartGame() {
        val clearedPlayers = _state.value.players.map { it.copy(scores = emptyMap()) }
        _state.value = YatziState(players = clearedPlayers, gameStarted = true)
    }

    fun startGame() {
        if (_state.value.players.isEmpty()) return
        _state.update {
            it.copy(
                gameStarted = true,
                currentPlayerIndex = 0,
                currentRollCount = 0,
                dice = List(5) { YatziDie() },
                potentialScores = emptyMap() // Reset potential scores for new game
            )
        }
    }

    fun rollDice() {
        val currentState = _state.value
        if (!currentState.canRoll) return

        val newDice = currentState.dice.map { die ->
            if (die.isKept) die else die.copy(value = Random.nextInt(1, 7))
        }

        // Calculate potential scores after the roll
        val newPotentialScores = if (currentState.currentRollCount + 1 > 0) {
            YatziCategory.entries.associateWith { category ->
                calculateScore(category, newDice)
            }
        } else {
            emptyMap()
        }

        _state.update {
            it.copy(
                dice = newDice,
                currentRollCount = it.currentRollCount + 1,
                potentialScores = newPotentialScores
            )
        }
    }

    fun toggleDie(index: Int) {
        val currentState = _state.value
        // Can only keep dice after first roll and before the turn is over
        if (currentState.currentRollCount == 0 && !currentState.gameStarted) return 

        // Actually standard rules allow keeping dice even before first roll? 
        // No, you roll all 5 first.
        if (currentState.currentRollCount == 0) return

        val newDice = currentState.dice.toMutableList()
        val die = newDice[index]
        newDice[index] = die.copy(isKept = !die.isKept)
        
        // Recalculate potential scores when dice are toggled (since kept dice affect next roll)
        val newPotentialScores = if (currentState.currentRollCount > 0) {
            YatziCategory.entries.associateWith { category ->
                calculateScore(category, newDice)
            }
        } else {
            emptyMap()
        }
        
        _state.update { 
            it.copy(
                dice = newDice,
                potentialScores = newPotentialScores
            )
        }
    }

    fun selectCategory(category: YatziCategory) {
        val currentState = _state.value
        val currentPlayer = currentState.currentPlayer ?: return
        
        // Cannot select if already scored
        if (currentPlayer.scores.containsKey(category)) return
        
        // Must have rolled at least once to score (or maybe 0 times if you want to score 0 on chance? No, you must roll)
        if (currentState.currentRollCount == 0) return

        val score = calculateScore(category, currentState.dice)
        val newScores = currentPlayer.scores.toMutableMap()
        newScores[category] = score
        val newPlayer = currentPlayer.copy(scores = newScores)

        val newPlayers = currentState.players.toMutableList()
        newPlayers[currentState.currentPlayerIndex] = newPlayer

        // Check for end of game: All players have filled all categories
        val allCategoriesFilled = newPlayers.all { it.scores.size == YatziCategory.entries.size }
        val winner = if (allCategoriesFilled) {
             newPlayers.maxByOrNull { it.totalScore }
        } else null

        // Next player
        val nextPlayerIndex = (currentState.currentPlayerIndex + 1) % currentState.players.size

        _state.update {
            it.copy(
                players = newPlayers,
                currentPlayerIndex = nextPlayerIndex,
                currentRollCount = 0,
                dice = List(5) { YatziDie() }, // Reset dice for next player
                winner = winner,
                potentialScores = emptyMap() // Reset potential scores for next player
            )
        }
    }

    fun calculatePotentialScore(category: YatziCategory): Int {
        return _state.value.potentialScores[category] ?: 0
    }

    private fun calculateScore(category: YatziCategory, dice: List<YatziDie>): Int {
        val values = dice.map { it.value }
        val counts = values.groupingBy { it }.eachCount()

        return when (category) {
            YatziCategory.ONES -> counts.getOrElse(1) { 0 } * 1
            YatziCategory.TWOS -> counts.getOrElse(2) { 0 } * 2
            YatziCategory.THREES -> counts.getOrElse(3) { 0 } * 3
            YatziCategory.FOURS -> counts.getOrElse(4) { 0 } * 4
            YatziCategory.FIVES -> counts.getOrElse(5) { 0 } * 5
            YatziCategory.SIXES -> counts.getOrElse(6) { 0 } * 6
            
            YatziCategory.ONE_PAIR -> {
                10
            }
            YatziCategory.TWO_PAIRS -> {
                20
            }
            YatziCategory.THREE_OF_A_KIND -> {
                val three = counts.filter { it.value >= 3 }.keys.firstOrNull()
                if (three != null) values.sum() else 0
            }
            YatziCategory.FOUR_OF_A_KIND -> {
                val four = counts.filter { it.value >= 4 }.keys.firstOrNull()
                if (four != null) values.sum() else 0
            }
            YatziCategory.FULL_HOUSE -> {
                val three = counts.filter { it.value >= 3 }.keys.firstOrNull()
                val two = counts.filter { it.value >= 2 }.keys.firstOrNull { it != three }
                if (three != null && two != null) 25 else 0
            }
            YatziCategory.SMALL_STRAIGHT -> {
                // 4 consecutive dice
                val sortedUnique = values.distinct().sorted()
                // Check for 1,2,3,4 or 2,3,4,5 or 3,4,5,6
                val s = sortedUnique.joinToString("")
                if (s.contains("1234") || s.contains("2345") || s.contains("3456")) 30 else 0
            }
            YatziCategory.LARGE_STRAIGHT -> {
                 val sortedUnique = values.distinct().sorted()
                 val s = sortedUnique.joinToString("")
                 if (s.contains("12345") || s.contains("23456")) 40 else 0
            }
            YatziCategory.CHANCE -> values.sum()
            YatziCategory.YAHTZEE -> {
                 if (counts.any { it.value == 5 }) 50 + values.sum() else 0
            }
        }
    }
}
