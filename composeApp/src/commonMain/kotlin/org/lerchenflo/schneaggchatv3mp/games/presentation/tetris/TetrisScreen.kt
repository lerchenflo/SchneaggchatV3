package org.lerchenflo.schneaggchatv3mp.games.presentation.tetris

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_tetris_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_tetris_title

@Composable
fun TetrisScreen(
    onBackClick: () -> Unit,
    viewModel: TetrisViewModel
) {
    val state by viewModel.state.collectAsState()
    var explanationDismissed by rememberSaveable { mutableStateOf(false) }
    val isStarted = state.isPlaying || state.isGameOver

    // After process death the dismissed flag is restored but the ViewModel run
    // is lost — start a fresh run instead of showing the explanation again.
    LaunchedEffect(Unit) {
        if (explanationDismissed && !isStarted) viewModel.startGame()
    }

    // Leaving the game ends the run so no loop/counter keeps running in the background
    DisposableEffect(Unit) {
        onDispose { viewModel.stopGame() }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_tetris_title),
            onBackClick = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp, top = 4.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            TetrisBoard(
                state = state,
                onRotate = { viewModel.rotate() },
                onMoveLeft = { viewModel.moveLeft() },
                onMoveRight = { viewModel.moveRight() },
                onDrop = { viewModel.hardDrop() },
                onSoftDropStart = { viewModel.setSoftDropping(true) },
                onSoftDropEnd = { viewModel.setSoftDropping(false) }
            )

            val nextPiece = state.nextPiece
            if (nextPiece != null && state.isPlaying) {
                NextPiecePreview(
                    piece = nextPiece,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }

            if (isStarted) {
                GameHud(
                    score = state.score.toLong(),
                    timeMillis = state.gameTime,
                    onStop = {
                        explanationDismissed = false
                        viewModel.stopGame()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            if (!explanationDismissed && !isStarted) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_tetris_title),
                    explanation = stringResource(Res.string.games_tetris_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewModel.startGame()
                    }
                )
            }

            if (state.isGameOver) {
                GameOverOverlay(
                    game = GameId.TETRIS,
                    finalScore = state.score.toLong(),
                    finalTimeMillis = state.gameTime,
                    onRestart = { viewModel.restartGame() },
                    onExit = {
                        viewModel.stopGame()
                        onBackClick()
                    }
                )
            }
        }
    }
}

private const val BOARD_COLUMNS = 10
private const val BOARD_ROWS = 20

/**
 * Shows the piece that will spawn next. All pieces are drawn on the same
 * 4-cell grid so they keep a consistent scale instead of the I piece
 * appearing smaller than the O piece.
 */
@Composable
private fun NextPiecePreview(
    piece: Tetromino,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .size(64.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        val shape = piece.getShape()
        val minRow = shape.minOf { it.first }
        val maxRow = shape.maxOf { it.first }
        val minCol = shape.minOf { it.second }
        val maxCol = shape.maxOf { it.second }

        val cell = minOf(size.width, size.height) / 4f
        // Center the piece's bounding box inside the preview
        val originX = (size.width - (maxCol - minCol + 1) * cell) / 2f
        val originY = (size.height - (maxRow - minRow + 1) * cell) / 2f

        shape.forEach { (rOffset, cOffset) ->
            val topLeft = Offset(
                originX + (cOffset - minCol) * cell,
                originY + (rOffset - minRow) * cell
            )
            drawRect(
                color = piece.color,
                topLeft = topLeft,
                size = Size(cell, cell)
            )
            drawRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = topLeft,
                size = Size(cell, cell),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/** Axis a drag gesture has committed to, so horizontal swipes never trigger the soft drop. */
private enum class DragAxis { Undecided, Horizontal, Vertical }

@Composable
fun TetrisBoard(
    state: TetrisState,
    onRotate: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDrop: () -> Unit,
    onSoftDropStart: () -> Unit,
    onSoftDropEnd: () -> Unit
) {
    // Determine cell size based on screen width/height available
    // Tetris board is 10x20

    Canvas(
        modifier = Modifier
            .aspectRatio(BOARD_COLUMNS.toFloat() / BOARD_ROWS)
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onRotate() },
                    onLongPress = { onDrop() }
                )
            }
            .pointerInput(Unit) {
                // One cell of travel moves the piece one cell, so it follows the
                // finger like in the original Tetris app.
                val cellWidth = (size.width.toFloat() / BOARD_COLUMNS).coerceAtLeast(1f)
                // A gesture locks onto an axis once it has moved half a cell in a
                // clear direction. Without this, the vertical drift of a left/right
                // swipe would start the soft drop and speed the whole game up.
                val axisLockThreshold = cellWidth / 2f

                var horizontalAccumulator = 0f
                var verticalAccumulator = 0f
                var axis = DragAxis.Undecided

                detectDragGestures(
                    onDragStart = {
                        horizontalAccumulator = 0f
                        verticalAccumulator = 0f
                        axis = DragAxis.Undecided
                    },
                    onDragEnd = {
                        onSoftDropEnd()
                    },
                    onDragCancel = {
                        // Without this the soft drop would stay latched on and the
                        // game would keep running at 10x speed.
                        onSoftDropEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        horizontalAccumulator += dragAmount.x
                        verticalAccumulator += dragAmount.y

                        if (axis == DragAxis.Undecided) {
                            val absHorizontal = abs(horizontalAccumulator)
                            val absVertical = abs(verticalAccumulator)
                            if (absHorizontal >= axisLockThreshold && absHorizontal > absVertical) {
                                axis = DragAxis.Horizontal
                                verticalAccumulator = 0f
                            } else if (absVertical >= axisLockThreshold && absVertical > absHorizontal) {
                                axis = DragAxis.Vertical
                                horizontalAccumulator = 0f
                            }
                        }

                        when (axis) {
                            DragAxis.Horizontal -> {
                                while (horizontalAccumulator >= cellWidth) {
                                    onMoveRight()
                                    horizontalAccumulator -= cellWidth
                                }
                                while (horizontalAccumulator <= -cellWidth) {
                                    onMoveLeft()
                                    horizontalAccumulator += cellWidth
                                }
                            }
                            // Dragging back up above the start point releases the soft drop again
                            DragAxis.Vertical -> {
                                if (verticalAccumulator > 0f) onSoftDropStart() else onSoftDropEnd()
                            }
                            DragAxis.Undecided -> Unit
                        }
                    }
                )
            }
    ) {
        val cellWidth = size.width / BOARD_COLUMNS
        val cellHeight = size.height / BOARD_ROWS
        
        // Draw Board
        state.board.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, color ->
                if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIndex * cellWidth, rowIndex * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                    drawRect(
                        color = Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(colIndex * cellWidth, rowIndex * cellHeight),
                        size = Size(cellWidth, cellHeight),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
        
        // Draw ghost piece: outline where the current piece would land
        state.currentPiece?.let { piece ->
            val landingRow = state.landingRow() ?: return@let
            if (landingRow == state.piecePosition.first) return@let // Piece already at its landing spot

            val pCol = state.piecePosition.second
            piece.getShape().forEach { (rOffset, cOffset) ->
                val drawRow = landingRow + rOffset
                val drawCol = pCol + cOffset

                if (drawRow >= 0) {
                    // Lighter, washed-out version of the falling piece's color
                    val ghostColor = lerp(piece.color, Color.White, 0.55f)
                    drawRect(
                        color = ghostColor.copy(alpha = 0.35f),
                        topLeft = Offset(drawCol * cellWidth, drawRow * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                    drawRect(
                        color = ghostColor.copy(alpha = 0.7f),
                        topLeft = Offset(drawCol * cellWidth, drawRow * cellHeight),
                        size = Size(cellWidth, cellHeight),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Draw Current Piece
        state.currentPiece?.let { piece ->
            val (pRow, pCol) = state.piecePosition
            piece.getShape().forEach { (rOffset, cOffset) ->
                val drawRow = pRow + rOffset
                val drawCol = pCol + cOffset
                
                if (drawRow >= 0) { // Don't draw if above board (shouldn't really happen but safety)
                     drawRect(
                        color = piece.color,
                        topLeft = Offset(drawCol * cellWidth, drawRow * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                     drawRect(
                        color = Color.White.copy(alpha = 0.5f),
                        topLeft = Offset(drawCol * cellWidth, drawRow * cellHeight),
                        size = Size(cellWidth, cellHeight),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        
        // Draw Grid Lines (Optional, but helps visualization)
        for (i in 0..10) {
             drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(i * cellWidth, 0f),
                end = Offset(i * cellWidth, size.height)
            )
        }
        for (i in 0..20) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, i * cellHeight),
                end = Offset(size.width, i * cellHeight)
            )
        }
    }
}
