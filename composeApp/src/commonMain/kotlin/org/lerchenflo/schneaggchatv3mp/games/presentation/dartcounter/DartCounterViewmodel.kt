package org.lerchenflo.schneaggchatv3mp.games.presentation.dartcounter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.lerchenflo.schneaggchatv3mp.games.data.GameHighscoreRepository
import org.lerchenflo.schneaggchatv3mp.games.data.GameSaveRepository
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.domain.GamePlayer
import org.lerchenflo.schneaggchatv3mp.games.domain.dartCounterDifficulty
import org.lerchenflo.schneaggchatv3mp.games.domain.GameSave
import org.lerchenflo.schneaggchatv3mp.games.domain.LocalGameSaveSlot
import org.lerchenflo.schneaggchatv3mp.games.domain.dartcounter.DartSegment
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameSaveSession
import org.lerchenflo.schneaggchatv3mp.games.presentation.HighscoreUploadController
import kotlin.math.roundToLong

class DartCounterViewModel(
    gameSaveRepository: GameSaveRepository,
    gameHighscoreRepository: GameHighscoreRepository,
) : ViewModel() {
    enum class OutMode {
        SINGLE_OUT,
        DOUBLE_OUT
    }

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
                currentPlayer.totalDartsThrown++
                if (subtractScore(dartThrow.score, dartThrow.isDouble, dartThrow.isTriple)) {
                    currentTurnDarts.add(dartThrow)
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

        /** The turn display counters live in the ViewModel, so it passes them in. */
        fun toSnapshot(currentThrow: Int, throwCount: Int, totalThrowsCount: Int, userIds: List<String?>) = DartCounterSnapshot(
            doubleOut = doubleOut,
            countdown = countdown,
            players = playerList.mapIndexed { index, it ->
                DartPlayerSnapshot(
                    name = it.name,
                    score = it.score,
                    totalDartsThrown = it.totalDartsThrown,
                    isFinished = it.isFinished,
                    userId = userIds.getOrNull(index),
                )
            },
            currentPlayerIndex = currentPlayerIndex,
            turnStartScore = turnStartScore,
            turnHistory = turnHistory.map { turn ->
                DartTurnSnapshot(
                    playerIndex = turn.playerIndex,
                    playerName = turn.playerName,
                    scoreAtStart = turn.scoreAtStart,
                    dartsThrown = turn.dartsThrown.map { it.toSnapshot() },
                )
            },
            currentTurnDarts = currentTurnDarts.map { it.toSnapshot() },
            allThrows = allThrowsHistory.map { it.toSnapshot() },
            currentThrow = currentThrow,
            throwCount = throwCount,
            totalThrowsCount = totalThrowsCount,
        )

        /** Overwrites the freshly created manager (same players and settings) with a saved game. */
        fun restoreFrom(snapshot: DartCounterSnapshot) {
            snapshot.players.forEachIndexed { index, saved ->
                playerList.getOrNull(index)?.apply {
                    score = saved.score
                    totalDartsThrown = saved.totalDartsThrown
                    isFinished = saved.isFinished
                }
            }
            currentPlayerIndex = snapshot.currentPlayerIndex.coerceIn(0, playerList.lastIndex)
            turnStartScore = snapshot.turnStartScore
            turnHistory.clear()
            turnHistory.addAll(snapshot.turnHistory.map { turn ->
                Turn(
                    playerIndex = turn.playerIndex,
                    playerName = turn.playerName,
                    scoreAtStart = turn.scoreAtStart,
                    dartsThrown = turn.dartsThrown.map { it.toDartThrow() },
                )
            })
            currentTurnDarts = snapshot.currentTurnDarts.map { it.toDartThrow() }.toMutableList()
            allThrowsHistory.clear()
            allThrowsHistory.addAll(snapshot.allThrows.map { it.toDartThrow() })
            gameStarted = true
            checkIfAllPlayersFinished()
        }
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
    
    var selectedOutMode by mutableStateOf(OutMode.DOUBLE_OUT)
        private set
    
    var currentThrow by mutableStateOf(0)
        private set
    
    var throwCount by mutableStateOf(0)
        private set

    val dartsLeft: Int get() = if (gameStarted) 3 - throwCount else 0

    var showStopGameDialog by mutableStateOf(false)
        private set
    var gameStarted by mutableStateOf(false)
        private set
    
    var currentPlayerName by mutableStateOf("")
        private set
    
    var totalThrowsCount by mutableStateOf(0)
        private set

    /**
     * Bumped after every change to [gameManager]'s internals. Scores and turn darts live in plain
     * mutable fields, so the UI reads this to reliably recompose (e.g. a bust on the first dart
     * changes nothing else that is observable).
     */
    var gameRevision by mutableStateOf(0)
        private set

    // Same order as playerNames; keeps the user ids of platform users for the leaderboard upload
    private var gamePlayers: List<GamePlayer> = emptyList()

    fun setPlayers(players: List<GamePlayer>) {
        gamePlayers = players
        playerNames = players.map { it.name }
    }

    fun addPlayerName(name: String) {
        if (name.isNotBlank() && name !in playerNames) {
            setPlayers(gamePlayers + GamePlayer(name = name))
        }
    }
    
    fun removePlayerName(name: String) {
        setPlayers(gamePlayers.filterNot { it.name == name })
    }
    
    fun startGame() {
        if (playerNames.isNotEmpty()) {
            highscoreUpload.reset()
            gameManager = GameManager(
                doubleOut = selectedOutMode == OutMode.DOUBLE_OUT,
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

    fun setOutMode(mode: OutMode) {
        selectedOutMode = mode
    }
    
    fun throwDart(segment: DartSegment) {
        gameManager?.let { game ->
            val score = segment.base
            val isDouble = segment.isDouble
            val isTriple = segment.isTriple
            val actualScore = segment.points

            // Track darts thrown for current player, including busts
            game.getCurrentPlayer().totalDartsThrown++

            if (game.subtractScore(score, isDouble, isTriple)) {
                // Track this dart
                game.addDartToTurn(score, isDouble, isTriple, actualScore)

                currentThrow += actualScore
                throwCount++

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

            if (game.gameOver) offerHighscoreUpload(game)
        }
        gameRevision++
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
                // The finish was taken back, so there is no final result to upload anymore
                if (!game.gameOver) highscoreUpload.reset()
            }
        }
        gameRevision++
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
        saveSession.clear()
        highscoreUpload.reset()
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

    // Declared after all state properties so a restore never sees them uninitialized
    private val saveSession = GameSaveSession(
        game = LocalGameSaveSlot.DART_COUNTER,
        serializer = DartCounterSnapshot.serializer(),
        schemaVersion = DART_COUNTER_SNAPSHOT_VERSION,
        repository = gameSaveRepository,
        scope = viewModelScope,
    )

    init {
        saveSession.start(onRestore = ::restore, onAppBackgrounded = ::persist)
    }

    /** Leaving the screen or backgrounding the app keeps the running game for the next visit. */
    fun persist() = saveSession.persist(snapshotOrNull())

    /** Null when there is no game worth keeping (not started or already over). */
    private fun snapshotOrNull(): DartCounterSnapshot? {
        val game = gameManager ?: return null
        if (!gameStarted || game.gameOver || game.playerList.isEmpty()) return null
        return game.toSnapshot(
            currentThrow = currentThrow,
            throwCount = throwCount,
            totalThrowsCount = totalThrowsCount,
            userIds = gamePlayers.map { it.userId },
        )
    }

    private fun restore(save: GameSave<DartCounterSnapshot>) {
        val data = save.data
        if (data.players.isEmpty()) return
        val names = data.players.map { it.name }
        val game = GameManager(
            doubleOut = data.doubleOut,
            countdown = data.countdown,
            playerNames = names,
            onThrowAdded = { totalThrowsCount++ }
        )
        game.restoreFrom(data)
        if (game.gameOver) return

        gamePlayers = data.players.map { GamePlayer(name = it.name, userId = it.userId) }
        playerNames = names
        selectedCountdown = data.countdown
        selectedOutMode = if (data.doubleOut) OutMode.DOUBLE_OUT else OutMode.SINGLE_OUT
        currentThrow = data.currentThrow
        throwCount = data.throwCount
        totalThrowsCount = data.totalThrowsCount
        gameManager = game
        updateCurrentPlayerName()
        gameStarted = true
    }

    private val highscoreUpload = HighscoreUploadController(
        game = GameId.DART_COUNTER,
        repository = gameHighscoreRepository,
        scope = viewModelScope,
    )
    /** Leaderboard upload offer once every player finished; never uploads without confirmation. */
    val highscoreUploadState = highscoreUpload.state

    fun uploadHighscores() = highscoreUpload.upload()

    fun declineHighscoreUpload() = highscoreUpload.decline()

    /**
     * Only a finished game counts: each player's three-dart average over the whole game (x100),
     * on the board of the game's countdown. Averages during a running game are never offered.
     */
    private fun offerHighscoreUpload(game: GameManager) {
        val results = game.playerList.mapIndexedNotNull { index, player ->
            if (player.totalDartsThrown == 0) return@mapIndexedNotNull null
            val average = game.countdown * 300.0 / player.totalDartsThrown
            val gamePlayer = gamePlayers.getOrNull(index) ?: GamePlayer(name = player.name)
            gamePlayer to average.roundToLong()
        }
        highscoreUpload.offer(dartCounterDifficulty(game.countdown), results)
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled here; the write runs on the application scope
        persist()
    }
}
