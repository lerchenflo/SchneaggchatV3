package org.lerchenflo.schneaggchatv3mp.games.presentation.oddoneout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import schneaggchatv3mp.composeapp.generated.resources.games_oddoneout_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_oddoneout_lives_remaining
import schneaggchatv3mp.composeapp.generated.resources.games_oddoneout_title

@Composable
fun OddOneOutScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewmodel = koinViewModel<OddOneOutViewmodel>()
    val state by viewmodel.state.collectAsStateWithLifecycle()

    var explanationDismissed by rememberSaveable { mutableStateOf(false) }
    val isStarted = state.isPlaying || state.isGameOver

    // After process death the dismissed flag is restored but the ViewModel run
    // is lost — start a fresh run instead of showing the explanation again.
    LaunchedEffect(Unit) {
        if (explanationDismissed && !isStarted) viewmodel.onAction(OddOneOutAction.StartGame)
    }

    // Leaving the game ends the run so no timer keeps running in the background
    DisposableEffect(Unit) {
        onDispose { viewmodel.onAction(OddOneOutAction.StopGame) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_oddoneout_title),
            onBackClick = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (isStarted) {
                LinearProgressIndicator(
                    progress = {
                        if (state.roundTimeMillis <= 0L) 0f
                        else (state.roundTimeRemainingMillis.toFloat() / state.roundTimeMillis).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            OddOneOutBoard(
                state = state,
                onAction = viewmodel::onAction,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp)
                    // Keep the board clear of the HUD, lives row and the countdown bar at the top
                    .padding(top = 72.dp, bottom = 8.dp)
            )

            if (isStarted) {
                val livesDescription = stringResource(Res.string.games_oddoneout_lives_remaining, state.lives)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp)
                        .semantics {
                            contentDescription = livesDescription
                        }
                ) {
                    repeat(ODDONEOUT_STARTING_LIVES) { index ->
                        Icon(
                            imageVector = if (index < state.lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        if (index != ODDONEOUT_STARTING_LIVES - 1) Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                GameHud(
                    score = state.score.toLong(),
                    timeMillis = state.elapsedMillis,
                    onStop = {
                        explanationDismissed = false
                        viewmodel.onAction(OddOneOutAction.StopGame)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            if (!explanationDismissed && !isStarted) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_oddoneout_title),
                    explanation = stringResource(Res.string.games_oddoneout_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewmodel.onAction(OddOneOutAction.StartGame)
                    }
                )
            }

            if (state.isGameOver) {
                GameOverOverlay(
                    game = GameId.ODDONEOUT,
                    finalScore = state.score.toLong(),
                    finalTimeMillis = state.elapsedMillis,
                    onRestart = { viewmodel.onAction(OddOneOutAction.RestartGame) },
                    onExit = {
                        viewmodel.onAction(OddOneOutAction.StopGame)
                        onBackClick()
                    }
                )
            }
        }
    }
}

private val TILE_COLORS = mapOf(
    TilePalette.A to Color(0xFFEF5350), // red
    TilePalette.B to Color(0xFF42A5F5), // blue
    TilePalette.C to Color(0xFF66BB6A), // green
    TilePalette.D to Color(0xFFFFCA28), // yellow
    TilePalette.E to Color(0xFFAB47BC), // purple
)

@Composable
private fun OddOneOutBoard(
    state: OddOneOutState,
    onAction: (OddOneOutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    if (state.tiles.isEmpty()) return

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .pointerInput(state.gridSize, state.round) {
                detectTapGestures(
                    onTap = { offset ->
                        cellIndexAt(offset, size, state.gridSize)?.let {
                            onAction(OddOneOutAction.OnTileTapped(it))
                        }
                    }
                )
            }
    ) {
        val gridSize = state.gridSize
        val tilePx = size.width / gridSize
        val gap = tilePx * 0.08f
        val baseCornerPx = tilePx * 0.16f
        val oddCornerPx = (tilePx * (0.16f + 0.65f * state.oddDelta)).coerceAtMost(tilePx / 2f)
        val baseColor = TILE_COLORS.getValue(state.tiles.first().palette)

        state.tiles.forEachIndexed { index, tile ->
            val row = index / gridSize
            val col = index % gridSize

            val tileColor = if (tile.isOdd && state.variant == OddTileVariant.COLOR) {
                lerp(baseColor, if (state.oddLighten) Color.White else Color.Black, state.oddDelta)
            } else {
                baseColor
            }
            val corner = if (tile.isOdd && state.variant == OddTileVariant.SHAPE) oddCornerPx else baseCornerPx

            drawRoundRect(
                color = tileColor,
                topLeft = Offset(col * tilePx + gap, row * tilePx + gap),
                size = Size(tilePx - 2 * gap, tilePx - 2 * gap),
                cornerRadius = CornerRadius(corner, corner),
            )

            if (index == state.wrongIndex) {
                drawRoundRect(
                    color = colors.error,
                    topLeft = Offset(col * tilePx + gap, row * tilePx + gap),
                    size = Size(tilePx - 2 * gap, tilePx - 2 * gap),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = tilePx * 0.08f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

private fun cellIndexAt(offset: Offset, size: IntSize, gridSize: Int): Int? {
    val tilePx = size.width.toFloat() / gridSize
    if (tilePx <= 0f) return null
    val col = (offset.x / tilePx).toInt()
    val row = (offset.y / tilePx).toInt()
    if (row !in 0 until gridSize || col !in 0 until gridSize) return null
    return row * gridSize + col
}
