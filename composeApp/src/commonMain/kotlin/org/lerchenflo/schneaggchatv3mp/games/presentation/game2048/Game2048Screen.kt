package org.lerchenflo.schneaggchatv3mp.games.presentation.game2048

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_2048_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_2048_title
import kotlin.math.abs

@Composable
fun Game2048ScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<Game2048ViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var explanationDismissed by rememberSaveable { mutableStateOf(false) }
    val isStarted = state.isGameStarted || state.isGameOver

    LaunchedEffect(Unit) {
        if (explanationDismissed && !isStarted) {
            viewModel.onAction(Game2048Action.StartGame)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onAction(Game2048Action.StopGame) }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isStarted) {
        if (isStarted && !state.isGameOver) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown || state.isGameOver || !state.isGameStarted) {
                    return@onKeyEvent false
                }
                when (keyEvent.key) {
                    Key.DirectionUp, Key.W -> {
                        viewModel.onAction(Game2048Action.Move(MoveDirection.UP))
                        true
                    }
                    Key.DirectionDown, Key.S -> {
                        viewModel.onAction(Game2048Action.Move(MoveDirection.DOWN))
                        true
                    }
                    Key.DirectionLeft, Key.A -> {
                        viewModel.onAction(Game2048Action.Move(MoveDirection.LEFT))
                        true
                    }
                    Key.DirectionRight, Key.D -> {
                        viewModel.onAction(Game2048Action.Move(MoveDirection.RIGHT))
                        true
                    }
                    else -> false
                }
            }
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_2048_title),
            onBackClick = onBackClick,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Game2048Content(
                state = state,
                onMove = { direction -> viewModel.onAction(Game2048Action.Move(direction)) },
                onStop = {
                    explanationDismissed = false
                    viewModel.onAction(Game2048Action.StopGame)
                }
            )

            if (!explanationDismissed && !isStarted) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_2048_title),
                    explanation = stringResource(Res.string.games_2048_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewModel.onAction(Game2048Action.StartGame)
                    }
                )
            }

            if (state.isGameOver) {
                GameOverOverlay(
                    game = GameId.GAME_2048,
                    finalScore = state.score.toLong(),
                    finalTimeMillis = state.elapsedMillis,
                    onRestart = { viewModel.onAction(Game2048Action.RestartGame) },
                    onExit = {
                        viewModel.onAction(Game2048Action.StopGame)
                        onBackClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun Game2048Content(
    state: Game2048State,
    onMove: (MoveDirection) -> Unit,
    onStop: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 30.dp.toPx() }

    var dragAmountX by remember { mutableFloatStateOf(0f) }
    var dragAmountY by remember { mutableFloatStateOf(0f) }
    var dragTriggered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.isGameStarted, state.isGameOver) {
                if (!state.isGameStarted || state.isGameOver) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        dragAmountX = 0f
                        dragAmountY = 0f
                        dragTriggered = false
                    },
                    onDragEnd = {
                        dragTriggered = false
                    },
                    onDragCancel = {
                        dragTriggered = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragTriggered) return@detectDragGestures

                        dragAmountX += dragAmount.x
                        dragAmountY += dragAmount.y

                        val absX = abs(dragAmountX)
                        val absY = abs(dragAmountY)

                        if (absX >= swipeThresholdPx || absY >= swipeThresholdPx) {
                            dragTriggered = true
                            if (absX > absY) {
                                if (dragAmountX > 0) {
                                    onMove(MoveDirection.RIGHT)
                                } else {
                                    onMove(MoveDirection.LEFT)
                                }
                            } else {
                                if (dragAmountY > 0) {
                                    onMove(MoveDirection.DOWN)
                                } else {
                                    onMove(MoveDirection.UP)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        if (state.isGameStarted) {
            GameHud(
                score = state.score.toLong(),
                timeMillis = state.elapsedMillis,
                onStop = onStop,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = state.hasReached2048,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "★ 2048 reached! ★",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 4) {
                                val value = state.grid[row * 4 + col]
                                TileView(
                                    value = value,
                                    colors = colors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TileView(
    value: Int,
    colors: ColorScheme,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) = getTileColors(value, colors)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            val text = value.toString()
            val fontSize = when {
                text.length <= 2 -> 26.sp
                text.length == 3 -> 22.sp
                text.length == 4 -> 18.sp
                else -> 14.sp
            }

            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
    }
}

private fun getTileColors(value: Int, colors: ColorScheme): Pair<Color, Color> {
    return when (value) {
        0 -> Pair(colors.surfaceVariant.copy(alpha = 0.35f), colors.onSurfaceVariant)
        2 -> Pair(colors.surfaceVariant, colors.onSurfaceVariant)
        4 -> Pair(colors.secondaryContainer.copy(alpha = 0.65f), colors.onSecondaryContainer)
        8 -> Pair(colors.secondaryContainer, colors.onSecondaryContainer)
        16 -> Pair(colors.tertiaryContainer.copy(alpha = 0.65f), colors.onTertiaryContainer)
        32 -> Pair(colors.tertiaryContainer, colors.onTertiaryContainer)
        64 -> Pair(colors.primaryContainer.copy(alpha = 0.75f), colors.onPrimaryContainer)
        128 -> Pair(colors.primaryContainer, colors.onPrimaryContainer)
        256 -> Pair(colors.secondary.copy(alpha = 0.85f), colors.onSecondary)
        512 -> Pair(colors.secondary, colors.onSecondary)
        1024 -> Pair(colors.tertiary, colors.onTertiary)
        2048 -> Pair(colors.primary, colors.onPrimary)
        else -> Pair(colors.error, colors.onError)
    }
}
