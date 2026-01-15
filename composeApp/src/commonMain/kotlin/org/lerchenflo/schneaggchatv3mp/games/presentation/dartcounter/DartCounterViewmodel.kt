package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class DartCounterViewModel() : ViewModel() {
    data class Player(val name: String, var score: Int, var throws: MutableList<Int> = mutableListOf(), var totalDartsThrown: Int = 0, var isFinished: Boolean = false)

    data class Turn(
        val playerIndex: Int,
        val playerName: String,
        val scoreAtStart: Int,
        val dartsThrown: List<DartThrow>
    )
    
    data class DartThrow(
        val score: Int,
        val isDouble: Boolean,
        val isTriple: Boolean,
        val actualScore: Int
    )

    data class GameManager(
        val doubleOut: Boolean = false,
        val countdown: Int,
        val playerNames: List<String>,
        private val onDartCountChanged: (Int) -> Unit = {},
        private val onThrowAdded: () -> Unit = {}
    ) {
        val playerList: MutableList<Player> = mutableListOf()
        var currentPlayerIndex by mutableStateOf(0)
        private set
        var gameStarted = false
        var gameOver = false
        private var turnStartScore = 0
        private val turnHistory: MutableList<Turn> = mutableListOf()
        private var currentTurnDarts: MutableList<DartThrow> = mutableListOf()
        private val allThrowsHistory: MutableList<DartThrow> = mutableListOf()
        
        init {
            for(name in playerNames){
                playerList.add(Player(name, countdown))
            }
            // Initialize turn start score for first player
            if (playerList.isNotEmpty()) {
                turnStartScore = playerList[0].score
            }
        }
        
        fun startTurn() {
            turnStartScore = getCurrentPlayer().score
        }
        
        fun subtractScore(score: Int, isDouble: Boolean = false, isTriple: Boolean = false): Boolean {
            val currentPlayer = playerList[currentPlayerIndex]
            if (currentPlayer.isFinished) return false
            
            val actualScore = if (isTriple) score * 3 else if (isDouble) score * 2 else score
            val newScore = currentPlayer.score - actualScore
            
            return when {
                gameOver -> false
                doubleOut -> {
                    when {
                        newScore == 0 && isDouble -> {
                            currentPlayer.score = 0
                            currentPlayer.isFinished = true
                            checkIfAllPlayersFinished()
                            true
                        }
                        newScore > 1 -> {
                            currentPlayer.score = newScore
                            true
                        }
                        newScore == 1 -> {
                            // Bust: score of 1 is impossible in double out
                            false
                        }
                        newScore < 0 -> {
                            // Bust: negative score
                            false
                        }
                        else -> {
                            // Bust: score would be 0 without double
                            false
                        }
                    }
                }
                else -> {
                    when {
                        newScore >= 0 -> {
                            currentPlayer.score = newScore
                            if (currentPlayer.score == 0) {
                                currentPlayer.isFinished = true
                                checkIfAllPlayersFinished()
                            }
                            true
                        }
                        else -> {
                            // Bust: score would be negative
                            false
                        }
                    }
                }
            }
        }
        
        fun addDartToTurn(score: Int, isDouble: Boolean, isTriple: Boolean, actualScore: Int) {
            val dartThrow = DartThrow(score, isDouble, isTriple, actualScore)
            currentTurnDarts.add(dartThrow)
            allThrowsHistory.add(dartThrow)
            onDartCountChanged(currentTurnDarts.size)
            onThrowAdded()
        }
        
        fun completeTurn() {
            if (currentTurnDarts.isNotEmpty()) {
                val currentPlayer = getCurrentPlayer()
                turnHistory.add(Turn(
                    playerIndex = currentPlayerIndex,
                    playerName = currentPlayer.name,
                    scoreAtStart = turnStartScore,
                    dartsThrown = currentTurnDarts.toList()
                ))
                currentTurnDarts.clear()
                onDartCountChanged(0)
            }
        }
        
        fun canUndo(): Boolean = allThrowsHistory.isNotEmpty()
        
        fun getAllThrowsHistory(): List<DartThrow> = allThrowsHistory.toList()
        
        fun getCurrentTurnDarts(): List<DartThrow> = currentTurnDarts.toList()
        
        fun undoLastThrow(): Boolean {
            if (allThrowsHistory.isEmpty()) return false
            
            val lastThrow = allThrowsHistory.removeAt(allThrowsHistory.size - 1)
            
            // Rebuild the entire game state from scratch using allThrowsHistory
            rebuildGameStateFromHistory()
            
            return true
        }
        
        private fun rebuildGameStateFromHistory() {
            // Reset all players to their initial scores
            for (player in playerList) {
                player.score = countdown
                player.isFinished = false
                player.totalDartsThrown = 0
            }
            
            // Clear turn history and current turn
            turnHistory.clear()
            currentTurnDarts.clear()
            
            // Reset to first player
            currentPlayerIndex = 0
            turnStartScore = countdown
            
            // Replay all throws in order
            var throwCount = 0
            for (dartThrow in allThrowsHistory) {
                val currentPlayer = getCurrentPlayer()
                
                // Check if this is a new turn (3 darts thrown or bust occurred)
                if (throwCount > 0 && throwCount % 3 == 0) {
                    completeTurn()
                    nextPlayer()
                }
                
                // Apply the throw
                val actualScore = dartThrow.actualScore
                if (subtractScore(dartThrow.score, dartThrow.isDouble, dartThrow.isTriple)) {
                    currentTurnDarts.add(dartThrow)
                    currentPlayer.totalDartsThrown++
                    throwCount++
                } else {
                    // Bust occurred
                    bust()
                    completeTurn()
                    nextPlayer()
                    throwCount++
                }
            }
            
            onDartCountChanged(currentTurnDarts.size)
        }
        
        fun bust() {
            val currentPlayer = playerList[currentPlayerIndex]
            currentPlayer.score = turnStartScore
        }
        
        private fun checkIfAllPlayersFinished() {
            gameOver = playerList.all { it.isFinished }
        }
        
        fun nextPlayer() {
            if (!gameOver) {
                // Skip finished players
                var attempts = 0
                do {
                    currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size
                    attempts++
                } while (playerList[currentPlayerIndex].isFinished && attempts < playerList.size)
                startTurn() // Initialize turn start score for next player
            }
        }
        
        fun getCurrentPlayer(): Player = playerList[currentPlayerIndex]
        
        fun getWinners(): List<Player> = playerList.filter { it.isFinished }
    }

    var gameManager by mutableStateOf<GameManager?>(null)
        private set
    
    var playerNames by mutableStateOf<List<String>>(emptyList())
        private set
    
    var showPlayerSetup by mutableStateOf(false)
        private set
    
    var showGameConfig by mutableStateOf(false)
        private set
    
    var selectedCountdown by mutableStateOf(501)
        private set
    
    var selectedOutMode by mutableStateOf("Double Out")
        private set
    
    var currentThrow by mutableStateOf(0)
        private set
    
    var throwCount by mutableStateOf(0)
        private set
    var showStopGameDialog by mutableStateOf(false)
        private set
    var gameStarted by mutableStateOf(false)
        private set
    
    var currentPlayerName by mutableStateOf("")
        private set
    
    var totalThrowsCount by mutableStateOf(0)
        private set

    fun addPlayerName(name: String) {
        if (name.isNotBlank() && name !in playerNames) {
            playerNames = playerNames + name
        }
    }
    
    fun removePlayerName(name: String) {
        playerNames = playerNames - name
    }
    
    fun startGame() {
        if (playerNames.isNotEmpty()) {
            gameManager = GameManager(
                doubleOut = selectedOutMode == "Double Out",
                countdown = selectedCountdown,
                playerNames = playerNames,
                onDartCountChanged = { count -> /* currentTurnDartCount = count */ },
                onThrowAdded = { totalThrowsCount++ }
            )
            gameManager?.startTurn() // Initialize first player's turn
            updateCurrentPlayerName() // Update current player display
            showPlayerSetup = false
            showGameConfig = false
            resetThrow()
            gameStarted = true
        }
    }
    
    fun showPlayerSetupDialog() {
        showPlayerSetup = true
    }
    
    fun hidePlayerSetupDialog() {
        showPlayerSetup = false
    }
    
    fun showGameConfigDialog() {
        showGameConfig = true
    }
    
    fun hideGameConfigDialog() {
        showGameConfig = false
    }
    
    fun setCountdown(countdown: Int) {
        selectedCountdown = countdown
    }
    
    fun setOutMode(mode: String) {
        selectedOutMode = mode
    }
    
    fun throwDart(score: Int, isDouble: Boolean = false, isTriple: Boolean = false) {
        gameManager?.let { game ->
            val currentPlayer = game.getCurrentPlayer()
            val actualScore = if (isTriple) score * 3 else if (isDouble) score * 2 else score
            
            if (game.subtractScore(score, isDouble, isTriple)) {
                // Track this dart
                game.addDartToTurn(score, isDouble, isTriple, actualScore)
                
                currentThrow += actualScore
                throwCount++
                
                // Track darts thrown for current player
                game.getCurrentPlayer().totalDartsThrown++
                
                if (throwCount >= 3) {
                    if (!game.gameOver) {
                        game.completeTurn()
                        game.nextPlayer()
                        updateCurrentPlayerName()
                    }
                    resetThrow()
                }
            } else {
                // Bust occurred - reset score to turn start and move to next player
                game.bust()
                resetThrow()
                if (!game.gameOver) {
                    game.completeTurn() // Save the busted turn
                    game.nextPlayer()
                    updateCurrentPlayerName()
                }
            }
        }
    }
    
    fun canUndoThrow(): Boolean {
        return gameStarted && totalThrowsCount > 0
    }
    
    fun undoLastThrow() {
        gameManager?.let { game ->
            if (game.undoLastThrow()) {
                // Update total throws count
                totalThrowsCount--
                
                // Update current throw display
                val currentTurnDarts = game.getCurrentTurnDarts()
                currentThrow = currentTurnDarts.sumOf { it.actualScore }
                throwCount = currentTurnDarts.size
                updateCurrentPlayerName()
            }
        }
    }
    
    private fun updateCurrentPlayerName() {
        gameManager?.let { game ->
            currentPlayerName = game.getCurrentPlayer().name
        }
    }
    
    private fun resetThrow() {
        currentThrow = 0
        throwCount = 0
    }
    
    fun resetGame() {
        gameManager = null
        playerNames = emptyList()
        resetThrow()
        gameStarted = false
    }
    
    fun showStopGameConfirmation() {
        showStopGameDialog = true
    }
    
    fun hideStopGameConfirmation() {
        showStopGameDialog = false
    }
    
    fun stopGame() {
        resetGame()
        showStopGameDialog = false
    }
    
    fun canUndo(): Boolean {
        return gameManager?.canUndo() ?: false
    }
}