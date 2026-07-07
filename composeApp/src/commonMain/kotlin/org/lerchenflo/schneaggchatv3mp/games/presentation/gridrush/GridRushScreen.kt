package org.lerchenflo.schneaggchatv3mp.games.presentation.gridrush

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_gridrush_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_gridrush_moves
import schneaggchatv3mp.composeapp.generated.resources.games_gridrush_title

@Composable
fun GridRushScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewmodel = koinViewModel<GridRushViewmodel>()
    val state by viewmodel.state.collectAsStateWithLifecycle()

    var explanationDismissed by rememberSaveable { mutableStateOf(false) }
    val isStarted = state.isPlaying || state.isGameOver

    // After process death the dismissed flag is restored but the ViewModel run
    // is lost — start a fresh run instead of showing the explanation again.
    LaunchedEffect(Unit) {
        if (explanationDismissed && !isStarted) viewmodel.onAction(GridRushAction.StartGame)
    }

    // Leaving the game ends the run so no timer keeps running in the background
    DisposableEffect(Unit) {
        onDispose { viewmodel.onAction(GridRushAction.StopGame) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_gridrush_title),
            onBackClick = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            GridRushBoard(
                state = state,
                onAction = viewmodel::onAction,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp)
                    // Keep the board clear of the HUD and the move counter at the top
                    .padding(top = 72.dp, bottom = 8.dp)
            )

            if (isStarted) {
                Text(
                    text = stringResource(Res.string.games_gridrush_moves, state.movesUsed, state.parMoves),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp)
                )

                GameHud(
                    score = state.score.toLong(),
                    timeMillis = state.elapsedMillis,
                    onStop = {
                        explanationDismissed = false
                        viewmodel.onAction(GridRushAction.StopGame)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            if (!explanationDismissed && !isStarted) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_gridrush_title),
                    explanation = stringResource(Res.string.games_gridrush_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewmodel.onAction(GridRushAction.StartGame)
                    }
                )
            }

            if (state.isGameOver) {
                GameOverOverlay(
                    game = GameId.GRIDRUSH,
                    finalScore = state.score.toLong(),
                    onRestart = { viewmodel.onAction(GridRushAction.RestartGame) },
                    onExit = {
                        viewmodel.onAction(GridRushAction.StopGame)
                        onBackClick()
                    }
                )
            }
        }
    }
}

private val TILE_COLORS = mapOf(
    GridTileColor.A to Color(0xFFEF5350), // red
    GridTileColor.B to Color(0xFF42A5F5), // blue
    GridTileColor.C to Color(0xFF66BB6A), // green
    GridTileColor.D to Color(0xFFFFCA28), // yellow
    GridTileColor.E to Color(0xFFAB47BC), // purple
)

@Composable
private fun GridRushBoard(
    state: GridRushState,
    onAction: (GridRushAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    if (state.board.isEmpty()) return

    Canvas(
        // Largest square board that fits both the available width and height
        modifier = modifier
            .aspectRatio(state.cols.toFloat() / state.rows)
            .fillMaxSize()
            .pointerInput(state.rows, state.cols) {
                detectDragGestures(
                    onDragStart = { offset ->
                        cellAt(offset, size, state.rows, state.cols)?.let {
                            onAction(GridRushAction.OnDragStart(it))
                        }
                    },
                    onDrag = { change, _ ->
                        cellAt(change.position, size, state.rows, state.cols)?.let {
                            onAction(GridRushAction.OnDragMove(it))
                        }
                    },
                    onDragEnd = { onAction(GridRushAction.OnDragEnd) },
                    onDragCancel = { onAction(GridRushAction.OnDragEnd) },
                )
            }
    ) {
        val tilePx = size.width / state.cols
        val gap = tilePx * 0.06f
        val corner = CornerRadius(tilePx * 0.18f, tilePx * 0.18f)
        val pathCells = state.dragPath.toSet()

        state.board.forEachIndexed { row, rowTiles ->
            rowTiles.forEachIndexed { col, tile ->
                if (tile != null) {
                    val inPath = Cell(row, col) in pathCells
                    drawRoundRect(
                        color = TILE_COLORS.getValue(tile),
                        topLeft = Offset(col * tilePx + gap, row * tilePx + gap),
                        size = Size(tilePx - 2 * gap, tilePx - 2 * gap),
                        cornerRadius = corner,
                        alpha = if (inPath) 1f else 0.85f,
                    )
                    if (inPath) {
                        drawRoundRect(
                            color = colors.onBackground,
                            topLeft = Offset(col * tilePx + gap, row * tilePx + gap),
                            size = Size(tilePx - 2 * gap, tilePx - 2 * gap),
                            cornerRadius = corner,
                            style = Stroke(width = tilePx * 0.06f),
                        )
                    }
                }
            }
        }

        // Line through the dragged chain; dimmed while it is still too short to clear
        if (state.dragPath.size >= 2) {
            val lineColor = if (state.dragValid) colors.onBackground else colors.onBackground.copy(alpha = 0.35f)
            state.dragPath.zipWithNext().forEach { (from, to) ->
                drawLine(
                    color = lineColor,
                    start = Offset((from.col + 0.5f) * tilePx, (from.row + 0.5f) * tilePx),
                    end = Offset((to.col + 0.5f) * tilePx, (to.row + 0.5f) * tilePx),
                    strokeWidth = tilePx * 0.12f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun cellAt(offset: Offset, size: IntSize, rows: Int, cols: Int): Cell? {
    val tilePx = size.width.toFloat() / cols
    if (tilePx <= 0f) return null
    val col = (offset.x / tilePx).toInt()
    val row = (offset.y / tilePx).toInt()
    return if (row in 0 until rows && col in 0 until cols) Cell(row, col) else null
}
