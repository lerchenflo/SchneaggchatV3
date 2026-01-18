package org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.games.domain.GameAction
import org.lerchenflo.schneaggchatv3mp.games.domain.GameState
import org.lerchenflo.schneaggchatv3mp.games.domain.Platform

class TowerstackViewModel : ViewModel() {
    
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private var gameLoopJob: kotlinx.coroutines.Job? = null
    
    companion object {
        private const val SCREEN_WIDTH = 300f
        private const val SCREEN_HEIGHT = 500f
        private const val PLATFORM_WIDTH = 80f
        private const val PLATFORM_HEIGHT = 20f
        private const val INITIAL_Y = 400f
        private const val BASE_Y = 450f
    }
    
    init {
        resetGame()
    }
    
    fun onAction(action: GameAction) {
        when (action) {
            is GameAction.StartGame -> startGame()
            is GameAction.PlacePlatform -> placePlatform()
            is GameAction.ResetGame -> resetGame()
        }
    }
    
    private fun startGame() {
        val currentState = _gameState.value
        if (currentState.isGameStarted) return
        
        val basePlatform = Platform(
            x = SCREEN_WIDTH / 2 - PLATFORM_WIDTH / 2,
            y = BASE_Y,
            width = PLATFORM_WIDTH,
            height = PLATFORM_HEIGHT,
            isMoving = false
        )
        
        val firstMovingPlatform = Platform(
            x = 0f,
            y = INITIAL_Y,
            width = PLATFORM_WIDTH,
            height = PLATFORM_HEIGHT,
            isMoving = true,
            direction = 1f
        )
        
        _gameState.value = currentState.copy(
            platforms = listOf(basePlatform),
            currentPlatform = firstMovingPlatform,
            isGameStarted = true,
            isGameOver = false
        )
        
        startGameLoop()
    }
    
    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (isActive) {
                updateMovingPlatform()
                delay(16) // ~60 FPS
            }
        }
    }
    
    private fun updateMovingPlatform() {
        val currentState = _gameState.value
        val current = currentState.currentPlatform ?: return
        
        if (!current.isMoving) return
        
        val newX = current.x + (current.direction * currentState.gameSpeed)
        val newDirection = if (newX <= 0f || newX + current.width >= SCREEN_WIDTH) {
            -current.direction
        } else {
            current.direction
        }
        
        val updatedPlatform = current.copy(
            x = if (newX <= 0f) 0f else if (newX + current.width >= SCREEN_WIDTH) SCREEN_WIDTH - current.width else newX,
            direction = newDirection
        )
        
        _gameState.value = currentState.copy(currentPlatform = updatedPlatform)
    }
    
    private fun placePlatform() {
        val currentState = _gameState.value
        if (!currentState.isGameStarted || currentState.isGameOver) return
        
        val current = currentState.currentPlatform ?: return
        val platforms = currentState.platforms
        if (platforms.isEmpty()) return
        
        val topPlatform = platforms.last()
        
        // Calculate overlap
        val overlapStart = maxOf(current.x, topPlatform.x)
        val overlapEnd = minOf(current.x + current.width, topPlatform.x + topPlatform.width)
        val overlapWidth = overlapEnd - overlapStart
        
        if (overlapWidth <= 0) {
            // No overlap - game over
            gameOver()
            return
        }
        
        // Create the placed platform (only the overlapping part)
        val placedPlatform = Platform(
            x = overlapStart,
            y = topPlatform.y - PLATFORM_HEIGHT,
            width = overlapWidth,
            height = PLATFORM_HEIGHT,
            isMoving = false
        )
        
        // Update score and create new moving platform
        val newScore = currentState.score + 1
        val newPlatforms = platforms + placedPlatform
        val newY = placedPlatform.y - PLATFORM_HEIGHT
        
        val newMovingPlatform = Platform(
            x = 0f,
            y = newY,
            width = overlapWidth, // New platform has width of the overlap
            height = PLATFORM_HEIGHT,
            isMoving = true,
            direction = 1f
        )
        
        // Increase speed slightly every 5 successful placements
        val newSpeed = if (newScore % 5 == 0) {
            (currentState.gameSpeed * 1.1f).coerceAtMost(8f)
        } else {
            currentState.gameSpeed
        }
        
        _gameState.value = currentState.copy(
            platforms = newPlatforms,
            currentPlatform = newMovingPlatform,
            score = newScore,
            gameSpeed = newSpeed
        )
    }
    
    private fun gameOver() {
        gameLoopJob?.cancel()
        _gameState.value = _gameState.value.copy(isGameOver = true)
    }
    
    private fun resetGame() {
        gameLoopJob?.cancel()
        _gameState.value = GameState()
    }
    
    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}

