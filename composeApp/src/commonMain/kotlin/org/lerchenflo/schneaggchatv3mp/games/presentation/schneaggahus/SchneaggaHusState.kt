package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.ui.graphics.Color

const val SCHNEAGGHUS_GRID_WIDTH = 11
const val SCHNEAGGHUS_GRID_HEIGHT = 8
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
    val schneaggList: List<Schneagg> = emptyList(),
    val schneagghusList: List<Schneaggahus> = SCHNEAGGHUS_HOUSES,
    val trackList: List<TrackTile> = SCHNEAGGHUS_TRACK,
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
    WEST,
}

// Level layout: one spawn point, three switches, four houses.
val SCHNEAGGHUS_SPAWN = Position(5, 0)
val SCHNEAGGHUS_FIRST_TRACK = Position(5, 1)

val SCHNEAGGHUS_HOUSES = listOf(
    Schneaggahus(Position(2, 7), Color(0xFFEF5350)), // red
    Schneaggahus(Position(5, 4), Color(0xFF42A5F5)), // blue
    Schneaggahus(Position(8, 7), Color(0xFF66BB6A)), // green
    Schneaggahus(Position(10, 4), Color(0xFFFFCA28)), // yellow
)

val SCHNEAGGHUS_TRACK = listOf(
    TrackTile(Position(5, 1), DIRECTION.NORTH, listOf(DIRECTION.SOUTH)),
    TrackTile(Position(5, 2), DIRECTION.NORTH, listOf(DIRECTION.WEST, DIRECTION.EAST)),

    // West branch towards the red and blue houses
    TrackTile(Position(4, 2), DIRECTION.EAST, listOf(DIRECTION.WEST)),
    TrackTile(Position(3, 2), DIRECTION.EAST, listOf(DIRECTION.WEST)),
    TrackTile(Position(2, 2), DIRECTION.EAST, listOf(DIRECTION.SOUTH)),
    TrackTile(Position(2, 3), DIRECTION.NORTH, listOf(DIRECTION.SOUTH)),
    TrackTile(Position(2, 4), DIRECTION.NORTH, listOf(DIRECTION.SOUTH, DIRECTION.EAST)),
    TrackTile(Position(2, 5), DIRECTION.NORTH, listOf(DIRECTION.SOUTH)),
    TrackTile(Position(2, 6), DIRECTION.NORTH, listOf(DIRECTION.SOUTH)),
    TrackTile(Position(3, 4), DIRECTION.WEST, listOf(DIRECTION.EAST)),
    TrackTile(Position(4, 4), DIRECTION.WEST, listOf(DIRECTION.EAST)),

    // East branch towards the green and yellow houses
    TrackTile(Position(6, 2), DIRECTION.WEST, listOf(DIRECTION.EAST)),
    TrackTile(Position(7, 2), DIRECTION.WEST, listOf(DIRECTION.EAST)),
    TrackTile(Position(8, 2), DIRECTION.WEST, listOf(DIRECTION.SOUTH)),
    TrackTile(Position(8, 3), DIRECTION.NORTH, listOf(DIRECTION.SOUTH)),
    TrackTile(Position(8, 4), DIRECTION.NORTH, listOf(DIRECTION.SOUTH, DIRECTION.EAST)),
    TrackTile(Position(8, 5), DIRECTION.NORTH, listOf(DIRECTION.SOUTH)),
    TrackTile(Position(8, 6), DIRECTION.NORTH, listOf(DIRECTION.SOUTH)),
    TrackTile(Position(9, 4), DIRECTION.WEST, listOf(DIRECTION.EAST)),
)
