package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class DartCounterViewModel() : ViewModel() {
    data class Player(val name: String, var score: Int, var throws: MutableList<Int> = mutableListOf(), var totalDartsThrown: Int = 0)

    data class GameManager(
        val doubleOut: Boolean = false,
        val countdown: Int,
        val playerNames: List<String>
    ) {
        val playerList: MutableList<Player> = mutableListOf()
        var currentPlayerIndex = 0
        var gameStarted = false
        var gameOver = false
        
        init {
            for(name in playerNames){
                playerList.add(Player(name, countdown))
            }
        }
        
        fun subtractScore(score: Int, isDouble: Boolean = false, isTriple: Boolean = false): Boolean {
            val currentPlayer = playerList[currentPlayerIndex]
            val actualScore = if (isTriple) score * 3 else if (isDouble) score * 2 else score
            
            return when {
                gameOver -> false
                doubleOut -> {
                    when {
                        currentPlayer.score == actualScore && isDouble -> {
                            currentPlayer.score = 0
                            gameOver = true
                            true
                        }
                        currentPlayer.score > actualScore -> {
                            currentPlayer.score -= actualScore
                            true
                        }
                        else -> false
                    }
                }
                else -> {
                    if (currentPlayer.score >= actualScore) {
                        currentPlayer.score -= actualScore
                        if (currentPlayer.score == 0) {
                            gameOver = true
                        }
                        true
                    } else false
                }
            }
        }
        
        fun nextPlayer() {
            if (!gameOver) {
                currentPlayerIndex = (currentPlayerIndex + 1) % playerList.size
            }
        }
        
        fun getCurrentPlayer(): Player = playerList[currentPlayerIndex]
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
                playerNames = playerNames
            )
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
            if (game.subtractScore(score, isDouble, isTriple)) {
                val actualScore = if (isTriple) score * 3 else if (isDouble) score * 2 else score
                currentThrow += actualScore
                throwCount++
                
                // Track darts thrown for current player
                game.getCurrentPlayer().totalDartsThrown++
                
                if (throwCount >= 3 || game.gameOver) {
                    if (!game.gameOver) {
                        game.nextPlayer()
                    }
                    resetThrow()
                }
            }
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
}