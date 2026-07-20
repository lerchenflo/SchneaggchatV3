package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
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
import schneaggchatv3mp.composeapp.generated.resources.icon_schneaggahus
import schneaggchatv3mp.composeapp.generated.resources.icon_spawn_tunnel

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
                    .align(Alignment.Center)
                    .padding(horizontal = 2.dp)
                    // Keep the board clear of the HUD and the lives display at the top
                    .padding(top = 52.dp, bottom = 4.dp)
            )

            if (isStarted) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 12.dp)
                ) {
                    // Spent lives stay visible as outlines so the total is always readable
                    repeat(SCHNEAGGHUS_MAX_LIVES) { index ->
                        val isRemaining = index < state.lives
                        Icon(
                            imageVector = if (isRemaining) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isRemaining) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

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
                    finalTimeMillis = state.elapsedMillis,
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
        // Largest board that fits both the available width and height
        modifier = modifier.aspectRatio(state.gridWidth.toFloat() / state.gridHeight)
    ) {
        val tileSize = maxWidth / state.gridWidth

        Canvas(modifier = Modifier.fillMaxSize()) {
            val tilePx = size.width / state.gridWidth

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
            for (i in 0..state.gridWidth) {
                drawLine(gridColor, Offset(i * tilePx, 0f), Offset(i * tilePx, size.height))
            }
            for (j in 0..state.gridHeight) {
                drawLine(gridColor, Offset(0f, j * tilePx), Offset(size.width, j * tilePx))
            }

            // A track segment drawn as real rails: sleepers first, then the two
            // rails on top of them.
            val railGap = tilePx * 0.11f
            val railWidth = tilePx * 0.055f
            val sleeperHalfLength = tilePx * 0.17f
            val sleeperSpacing = tilePx * 0.24f

            fun drawRail(start: Offset, end: Offset, color: Color) {
                val delta = end - start
                val length = delta.getDistance()
                if (length <= 0f) return
                val direction = delta / length
                val normal = Offset(-direction.y, direction.x)

                var travelled = sleeperSpacing / 2f
                while (travelled < length) {
                    val point = start + direction * travelled
                    drawLine(
                        color = color,
                        start = point - normal * sleeperHalfLength,
                        end = point + normal * sleeperHalfLength,
                        strokeWidth = railWidth * 0.8f,
                        cap = StrokeCap.Round
                    )
                    travelled += sleeperSpacing
                }

                listOf(railGap, -railGap).forEach { offset ->
                    drawLine(
                        color = color,
                        start = start + normal * offset,
                        end = end + normal * offset,
                        strokeWidth = railWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Tracks: incoming rail plus one rail per exit. On switches the active
            // exit is highlighted and the inactive one dimmed.
            state.trackList.forEach { tile ->
                val center = tileCenter(tile.position)
                drawRail(
                    start = sideMiddle(tile.position, tile.entry),
                    end = center,
                    color = colors.outline
                )
                tile.exits.forEachIndexed { index, direction ->
                    val exitColor = when {
                        !tile.isSwitch -> colors.outline
                        index == tile.activeExit -> colors.primary
                        else -> colors.primary.copy(alpha = 0.25f)
                    }
                    drawRail(
                        start = center,
                        end = sideMiddle(tile.position, direction),
                        color = exitColor
                    )
                }
            }

            // Spawn point
            drawRail(
                start = tileCenter(state.spawn),
                end = sideMiddle(state.spawn, DIRECTION.SOUTH),
                color = colors.outline
            )
        }

        Icon(
            painter = painterResource(Res.drawable.icon_spawn_tunnel),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier
                .offset(tileSize * state.spawn.x, tileSize * state.spawn.y)
                .size(tileSize)
                .padding(2.dp)
        )

        state.schneagghusList.forEach { hus ->
            Icon(
                painter = painterResource(Res.drawable.icon_schneaggahus),
                contentDescription = null,
                tint = hus.color,
                modifier = Modifier
                    .offset(tileSize * hus.position.x, tileSize * hus.position.y)
                    .size(tileSize)
                    .padding(2.dp)
            )
        }

        // Invisible tap targets — the active rail itself already shows which way
        // the switch is set, so it needs no icon on top.
        state.trackList.forEach { tile ->
            if (tile.isSwitch) {
                Box(
                    modifier = Modifier
                        .offset(tileSize * tile.position.x, tileSize * tile.position.y)
                        .size(tileSize)
                        .clickable { onSwitchClick(tile.position) }
                )
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
