package org.lerchenflo.schneaggchatv3mp.games.presentation.tetris

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TetrisScreen(
    onBackClick: () -> Unit,
    viewModel: TetrisViewModel
) {
    val state by viewModel.state.collectAsState()
    
    // Start game initially if not playing
    LaunchedEffect(Unit) {
        if (!state.isPlaying && !state.isGameOver) {
            viewModel.startGame()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tetris") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if(state.isPlaying) viewModel.pauseGame() else viewModel.resumeGame() 
                    }) {
                        Icon(
                            if(state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Pause/Resume"
                        )
                    }
                     IconButton(onClick = { viewModel.restartGame() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Score Board
            Text(
                text = "Score: ${state.score}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )
            
            // Game Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
               TetrisBoard(
                   state = state,
                   onRotate = { viewModel.rotate() },
                   onMoveLeft = { viewModel.moveLeft() },
                   onMoveRight = { viewModel.moveRight() },
                   onDrop = { viewModel.hardDrop() }
               )
               
               if (state.isGameOver) {
                   Box(
                       modifier = Modifier
                           .fillMaxSize()
                           .background(Color.Black.copy(alpha = 0.7f)),
                       contentAlignment = Alignment.Center
                   ) {
                       Column(horizontalAlignment = Alignment.CenterHorizontally) {
                           Text(
                               text = "GAME OVER",
                               style = MaterialTheme.typography.displayMedium,
                               color = Color.Red
                           )
                           Text(
                               text = "Final Score: ${state.score}",
                               style = MaterialTheme.typography.titleLarge,
                               color = Color.White
                           )
                       }
                   }
               }
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
    onDrop: () -> Unit
) {
    // Determine cell size based on screen width/height available
    // Tetris board is 10x20
    
    var dragAccumulator by remember { mutableStateOf(0f) }
    val dragThreshold = 50f // Pixels to move one cell

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
                    onDragStart = { dragAccumulator = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount.x
                        
                        if (dragAccumulator > dragThreshold) {
                            onMoveRight()
                            dragAccumulator -= dragThreshold
                        } else if (dragAccumulator < -dragThreshold) {
                            onMoveLeft()
                            dragAccumulator += dragThreshold
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
