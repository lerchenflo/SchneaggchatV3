package org.lerchenflo.schneaggchatv3mp.games.domain

data class Platform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float = 20f,
    val isMoving: Boolean = false,
    val direction: Float = 1f // 1f for right, -1f for left
)

data class GameState(
    val platforms: List<Platform> = emptyList(),
    val currentPlatform: Platform? = null,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val isGameStarted: Boolean = false,
    val gameSpeed: Float = 2f
)

sealed class GameAction {
    object StartGame : GameAction()
    object PlacePlatform : GameAction()
    object ResetGame : GameAction()
}
