package org.lerchenflo.schneaggchatv3mp.games.presentation.tetris

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable

const val TETRIS_SNAPSHOT_VERSION = 1

/** A piece with its color stored as ARGB, since Compose [Color] is not serializable. */
@Serializable
data class TetrominoSnapshot(
    val type: TetrominoType,
    val rotation: Int,
    val colorArgb: Int,
)

/** Persisted mid-run Tetris state; board cells store their color as ARGB (null = empty). */
@Serializable
data class TetrisSnapshot(
    val board: List<List<Int?>>,
    val currentPiece: TetrominoSnapshot?,
    val nextPiece: TetrominoSnapshot?,
    val pieceRow: Int,
    val pieceCol: Int,
    val score: Int,
    val gameTime: Long,
)

fun Tetromino.toSnapshot() = TetrominoSnapshot(type = type, rotation = rotation, colorArgb = color.toArgb())

fun TetrominoSnapshot.toTetromino() = Tetromino(type = type, rotation = rotation, color = Color(colorArgb))
