package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

const val SCHNEAGGHUS_MAX_LIVES = 3

sealed interface SchneaggaHusAction {
    data object StartGame : SchneaggaHusAction
    data object StopGame : SchneaggaHusAction
    data object RestartGame : SchneaggaHusAction
    data object TogglePause : SchneaggaHusAction
    /** Screen is leaving (back, rotation, tab switch): pause and keep the run for the next visit. */
    data object LeaveGame : SchneaggaHusAction
    data class OnSwitchClick(val position: Position) : SchneaggaHusAction
}

data class SchneaggaHusState(
    val isPlaying: Boolean = false,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val score: Int = 0,
    val lives: Int = SCHNEAGGHUS_MAX_LIVES,
    val elapsedMillis: Long = 0L,
    val gridWidth: Int = 7,
    val gridHeight: Int = 11,
    val spawn: Position = Position(3, 0),
    val schneaggList: List<Schneagg> = emptyList(),
    val schneagghusList: List<Schneaggahus> = emptyList(),
    val trackList: List<TrackTile> = emptyList(),
    /** Current wave, starting at 1. */
    val wave: Int = 1,
    /** How many schneaggs this wave sends in total. */
    val waveSnailTotal: Int = 0,
    /** How many of them already arrived somewhere, right or wrong. */
    val waveDelivered: Int = 0,
    /** Pre-rolled colors still to spawn this wave; the head is the next one. */
    val upcoming: List<Color> = emptyList(),
    /** Run time (ms) the current wave started at; drives the wave banner fade. */
    val waveStartedAtElapsed: Long = 0L,
    /** Run time (ms) the next wave starts at; non-null only during the break between waves. */
    val intermissionUntilElapsed: Long? = null,
    /** Recent deliveries for the house pulse / floating points; pruned by the game loop. */
    val feedback: List<DeliveryFeedback> = emptyList(),
) {
    val isIntermission: Boolean get() = intermissionUntilElapsed != null
}

@Serializable
data class Position(
    val x: Int,
    val y: Int
) {
    fun step(direction: DIRECTION): Position = Position(x + direction.dx, y + direction.dy)
}

/**
 * One schneagg sitting on exactly one tile. [progress] runs from 0 at the middle
 * of the [entry] side to 1 at the middle of the [exit] side. The exit is locked
 * the moment the schneagg enters the tile, so a switch only counts when it was
 * set before the schneagg reached it.
 */
data class Schneagg(
    val id: Int,
    val color: Color,
    val tile: Position,
    val entry: DIRECTION,
    val exit: DIRECTION,
    val progress: Float,
)

data class Schneaggahus(
    val position: Position,
    val color: Color
)

/** A schneagg just arrived at the house on [position]; shown briefly as a pulse. */
data class DeliveryFeedback(
    val position: Position,
    val correct: Boolean,
    /** Points awarded, 0 for a wrong delivery. */
    val points: Int,
    val atElapsedMillis: Long,
)

/**
 * One track tile. [entry] is the side the rail comes in from; movement always
 * leaves through the active exit. Tiles with more than one exit are switches
 * the player can toggle.
 */
@Serializable
data class TrackTile(
    val position: Position,
    val entry: DIRECTION,
    val exits: List<DIRECTION>,
    val activeExit: Int = 0,
) {
    val isSwitch: Boolean get() = exits.size > 1
    val exit: DIRECTION get() = exits[activeExit]
}

/** Screen directions: y grows downwards, angles are clockwise with 0° pointing east. */
enum class DIRECTION(val dx: Int, val dy: Int, val angleDeg: Float) {
    NORTH(0, -1, -90f),
    EAST(1, 0, 0f),
    SOUTH(0, 1, 90f),
    WEST(-1, 0, 180f);

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
