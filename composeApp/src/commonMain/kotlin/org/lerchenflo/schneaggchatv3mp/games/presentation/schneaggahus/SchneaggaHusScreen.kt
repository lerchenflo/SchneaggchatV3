package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.House
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_title
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative

@Composable
fun SchneaggaHusScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewmodel = koinViewModel<SchneaggaHusViewmodel>()
    val state by viewmodel.state.collectAsStateWithLifecycle()

    var explanationDismissed by rememberSaveable { mutableStateOf(false) }
    val isStarted = state.isPlaying || state.isGameOver

    // After process death the dismissed flag is restored but the ViewModel run
    // is lost — start a fresh run instead of showing the explanation again.
    LaunchedEffect(Unit) {
        if (explanationDismissed && !isStarted) viewmodel.onAction(SchneaggaHusAction.StartGame)
    }

    // Leaving the game ends the run so no loop/counter keeps running in the background
    DisposableEffect(Unit) {
        onDispose { viewmodel.onAction(SchneaggaHusAction.StopGame) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_schneaggahus_title),
            onBackClick = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            SchneaggaHusBoard(
                state = state,
                onSwitchClick = { viewmodel.onAction(SchneaggaHusAction.OnSwitchClick(it)) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )

            if (isStarted) {
                Text(
                    text = "♥".repeat(state.lives),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 12.dp)
                )

                GameHud(
                    score = state.score.toLong(),
                    timeMillis = state.elapsedMillis,
                    onStop = {
                        explanationDismissed = false
                        viewmodel.onAction(SchneaggaHusAction.StopGame)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            if (!explanationDismissed && !isStarted) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_schneaggahus_title),
                    explanation = stringResource(Res.string.games_schneaggahus_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewmodel.onAction(SchneaggaHusAction.StartGame)
                    }
                )
            }

            if (state.isGameOver) {
                GameOverOverlay(
                    game = GameId.SCHNEAGGAHUS,
                    finalScore = state.score.toLong(),
                    onRestart = { viewmodel.onAction(SchneaggaHusAction.RestartGame) },
                    onExit = {
                        viewmodel.onAction(SchneaggaHusAction.StopGame)
                        onBackClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun SchneaggaHusBoard(
    state: SchneaggaHusState,
    onSwitchClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val schneaggIcon = imageResource(Res.drawable.icon_schneagg_alternative)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(SCHNEAGGHUS_GRID_WIDTH.toFloat() / SCHNEAGGHUS_GRID_HEIGHT)
    ) {
        val tileSize = maxWidth / SCHNEAGGHUS_GRID_WIDTH

        Canvas(modifier = Modifier.fillMaxSize()) {
            val tilePx = size.width / SCHNEAGGHUS_GRID_WIDTH

            fun tileCenter(position: Position) =
                Offset((position.x + 0.5f) * tilePx, (position.y + 0.5f) * tilePx)

            fun sideMiddle(position: Position, direction: DIRECTION): Offset {
                val center = tileCenter(position)
                return when (direction) {
                    DIRECTION.NORTH -> Offset(center.x, center.y - tilePx / 2f)
                    DIRECTION.EAST -> Offset(center.x + tilePx / 2f, center.y)
                    DIRECTION.SOUTH -> Offset(center.x, center.y + tilePx / 2f)
                    DIRECTION.WEST -> Offset(center.x - tilePx / 2f, center.y)
                }
            }

            // Grid
            val gridColor = colors.outlineVariant.copy(alpha = 0.4f)
            for (i in 0..SCHNEAGGHUS_GRID_WIDTH) {
                drawLine(gridColor, Offset(i * tilePx, 0f), Offset(i * tilePx, size.height))
            }
            for (j in 0..SCHNEAGGHUS_GRID_HEIGHT) {
                drawLine(gridColor, Offset(0f, j * tilePx), Offset(size.width, j * tilePx))
            }

            // Tracks: incoming rail plus one rail per exit. On switches the active
            // exit is highlighted and the inactive one dimmed.
            val trackWidth = tilePx * 0.28f
            state.trackList.forEach { tile ->
                val center = tileCenter(tile.position)
                drawLine(
                    color = colors.outline,
                    start = sideMiddle(tile.position, tile.entry),
                    end = center,
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )
                tile.exits.forEachIndexed { index, direction ->
                    val exitColor = when {
                        !tile.isSwitch -> colors.outline
                        index == tile.activeExit -> colors.primary
                        else -> colors.primary.copy(alpha = 0.25f)
                    }
                    drawLine(
                        color = exitColor,
                        start = center,
                        end = sideMiddle(tile.position, direction),
                        strokeWidth = trackWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Spawn point
            val spawnCenter = tileCenter(SCHNEAGGHUS_SPAWN)
            drawLine(
                color = colors.outline,
                start = spawnCenter,
                end = sideMiddle(SCHNEAGGHUS_SPAWN, DIRECTION.SOUTH),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round
            )
            drawCircle(colors.primary, radius = tilePx * 0.35f, center = spawnCenter)
        }

        state.schneagghusList.forEach { hus ->
            Icon(
                imageVector = Icons.Default.House,
                contentDescription = null,
                tint = hus.color,
                modifier = Modifier
                    .offset(tileSize * hus.position.x, tileSize * hus.position.y)
                    .size(tileSize)
            )
        }

        state.trackList.forEach { tile ->
            if (tile.isSwitch) {
                Box(
                    modifier = Modifier
                        .offset(tileSize * tile.position.x, tileSize * tile.position.y)
                        .size(tileSize)
                        .clickable { onSwitchClick(tile.position) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = colors.tertiary,
                        modifier = Modifier.fillMaxSize(0.55f)
                    )
                }
            }
        }

        state.schneaggList.forEach { schneagg ->
            Icon(
                bitmap = schneaggIcon,
                contentDescription = null,
                tint = schneagg.color,
                modifier = Modifier
                    .offset(tileSize * schneagg.renderX, tileSize * schneagg.renderY)
                    .size(tileSize)
                    .padding(2.dp)
            )
        }
    }
}
