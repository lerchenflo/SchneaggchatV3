package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.ui.graphics.Color

const val SCHNEAGGHUS_MAX_LIVES = 3

sealed interface SchneaggaHusAction {
    data object StartGame : SchneaggaHusAction
    data object StopGame : SchneaggaHusAction
    data object RestartGame : SchneaggaHusAction
    data class OnSwitchClick(val position: Position) : SchneaggaHusAction
}

data class SchneaggaHusState(
    val isPlaying: Boolean = false,
    val isGameOver: Boolean = false,
    val score: Int = 0,
    val lives: Int = SCHNEAGGHUS_MAX_LIVES,
    val elapsedMillis: Long = 0L,
    val gridWidth: Int = 11,
    val gridHeight: Int = 8,
    val spawn: Position = Position(5, 0),
    val firstTrack: Position = Position(5, 1),
    val schneaggList: List<Schneagg> = emptyList(),
    val schneagghusList: List<Schneaggahus> = emptyList(),
    val trackList: List<TrackTile> = emptyList(),
)

data class Position(
    val x: Int,
    val y: Int
) {
    fun step(direction: DIRECTION): Position = when (direction) {
        DIRECTION.NORTH -> copy(y = y - 1)
        DIRECTION.EAST -> copy(x = x + 1)
        DIRECTION.SOUTH -> copy(y = y + 1)
        DIRECTION.WEST -> copy(x = x - 1)
    }
}

/** A schneagg travelling from the center of [fromTile] to the center of [toTile]. */
data class Schneagg(
    val id: Int,
    val color: Color,
    val fromTile: Position,
    val toTile: Position,
    /** Progress between the two tile centers, 0..1 */
    val progress: Float,
) {
    val renderX: Float get() = fromTile.x + (toTile.x - fromTile.x) * progress
    val renderY: Float get() = fromTile.y + (toTile.y - fromTile.y) * progress
}

data class Schneaggahus(
    val position: Position,
    val color: Color
)

/**
 * One track tile. [entry] is only used for drawing the incoming rail —
 * movement always leaves through the active exit. Tiles with more than
 * one exit are switches the player can toggle.
 */
data class TrackTile(
    val position: Position,
    val entry: DIRECTION,
    val exits: List<DIRECTION>,
    val activeExit: Int = 0,
) {
    val isSwitch: Boolean get() = exits.size > 1
    val exit: DIRECTION get() = exits[activeExit]
}

enum class DIRECTION {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    fun opposite(): DIRECTION = when (this) {
        NORTH -> SOUTH
        EAST -> WEST
        SOUTH -> NORTH
        WEST -> EAST
    }

    fun turnLeft(): DIRECTION = when (this) {
        NORTH -> WEST
        WEST -> SOUTH
        SOUTH -> EAST
        EAST -> NORTH
    }

    fun turnRight(): DIRECTION = when (this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
    }
}
