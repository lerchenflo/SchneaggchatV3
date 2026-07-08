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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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
    
    var dragAccumulator by remember { mutableStateOf(0f) }
    var verticalDragAccumulator by remember { mutableStateOf(0f) }
    val dragThreshold = 80f // Pixels to move one cell
    val verticalDragThreshold = 40f // Pixels for vertical swipe detection

    Canvas(
        modifier = Modifier
            .aspectRatio(10f / 20f)
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onRotate() },
                    onLongPress = { onDrop() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        dragAccumulator = 0f
                        verticalDragAccumulator = 0f
                    },
                    onDragEnd = { 
                        onSoftDropEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount.x
                        verticalDragAccumulator += dragAmount.y
                        
                        // Handle horizontal movement
                        if (dragAccumulator > dragThreshold) {
                            onMoveRight()
                            dragAccumulator -= dragThreshold
                        } else if (dragAccumulator < -dragThreshold) {
                            onMoveLeft()
                            dragAccumulator += dragThreshold
                        }
                        
                        // Handle vertical swipe down for soft drop
                        if (verticalDragAccumulator > verticalDragThreshold) {
                            onSoftDropStart()
                        }
                    }
                )
            }
    ) {
        val cellWidth = size.width / 10
        val cellHeight = size.height / 20
        
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
                    drawRect(
                        color = piece.color.copy(alpha = 0.4f),
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
