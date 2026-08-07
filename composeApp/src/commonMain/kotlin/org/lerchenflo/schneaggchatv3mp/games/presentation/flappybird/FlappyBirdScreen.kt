package org.lerchenflo.schneaggchatv3mp.games.presentation.flappybird

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_flappybird_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_flappybird_title
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative

@Composable
fun FlappyBirdScreenRoot(
    viewModel: FlappyBirdViewModel = koinInject(),
    onBackClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var explanationDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (explanationDismissed && !state.isGameStarted) {
            viewModel.onAction(FlappyBirdAction.StartGame)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onAction(FlappyBirdAction.ResetGame) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_flappybird_title),
            onBackClick = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(state.isGameStarted, state.isGameOver) {
                    if (!state.isGameStarted || state.isGameOver) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown()
                        viewModel.onAction(FlappyBirdAction.Flap)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            FlappyBirdCanvas(state = state)

            if (state.isGameStarted) {
                GameHud(
                    score = state.score.toLong(),
                    timeMillis = state.elapsedMillis,
                    onStop = {
                        explanationDismissed = false
                        viewModel.onAction(FlappyBirdAction.ResetGame)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            if (!explanationDismissed && !state.isGameStarted) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_flappybird_title),
                    explanation = stringResource(Res.string.games_flappybird_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewModel.onAction(FlappyBirdAction.StartGame)
                    }
                )
            }

            if (state.isGameOver) {
                GameOverOverlay(
                    game = GameId.FLAPPYBIRD,
                    finalScore = state.score.toLong(),
                    finalTimeMillis = state.elapsedMillis,
                    onRestart = {
                        viewModel.onAction(FlappyBirdAction.ResetGame)
                        viewModel.onAction(FlappyBirdAction.StartGame)
                    },
                    onExit = {
                        viewModel.onAction(FlappyBirdAction.ResetGame)
                        onBackClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun FlappyBirdCanvas(
    state: FlappyBirdState,
) {
    val colors = MaterialTheme.colorScheme
    val schneaggIcon = imageResource(Res.drawable.icon_schneagg_alternative)

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val scaleX = size.width / FlappyBirdState.WORLD_WIDTH
        val scaleY = size.height / FlappyBirdState.WORLD_HEIGHT

        // Ground line
        val groundYPx = FlappyBirdState.GROUND_Y * scaleY
        val groundHeightPx = size.height - groundYPx

        // Draw Ground
        drawRect(
            color = colors.secondaryContainer,
            topLeft = Offset(0f, groundYPx),
            size = Size(size.width, groundHeightPx)
        )
        // Ground top accent stripe
        drawRect(
            color = colors.primary,
            topLeft = Offset(0f, groundYPx),
            size = Size(size.width, 6f * scaleY)
        )

        // Draw Pipes
        val pipeColor = colors.primary
        val pipeRimColor = colors.tertiary
        val pipeWidthPx = FlappyBirdState.PIPE_WIDTH * scaleX

        for (pipe in state.pipes) {
            val pipeXPx = pipe.x * scaleX
            val topHeightPx = pipe.topHeight * scaleY
            val gapHeightPx = pipe.gapHeight * scaleY
            val bottomPipeTopPx = topHeightPx + gapHeightPx
            val bottomHeightPx = groundYPx - bottomPipeTopPx

            // Top Pipe Body
            drawRoundRect(
                color = pipeColor,
                topLeft = Offset(pipeXPx, 0f),
                size = Size(pipeWidthPx, topHeightPx),
                cornerRadius = CornerRadius(4f * scaleX, 4f * scaleY)
            )
            // Top Pipe Rim Cap
            drawRoundRect(
                color = pipeRimColor,
                topLeft = Offset(pipeXPx - 4f * scaleX, (topHeightPx - 16f * scaleY).coerceAtLeast(0f)),
                size = Size(pipeWidthPx + 8f * scaleX, 16f * scaleY),
                cornerRadius = CornerRadius(4f * scaleX, 4f * scaleY)
            )

            // Bottom Pipe Body
            if (bottomHeightPx > 0f) {
                drawRoundRect(
                    color = pipeColor,
                    topLeft = Offset(pipeXPx, bottomPipeTopPx),
                    size = Size(pipeWidthPx, bottomHeightPx),
                    cornerRadius = CornerRadius(4f * scaleX, 4f * scaleY)
                )
                // Bottom Pipe Rim Cap
                drawRoundRect(
                    color = pipeRimColor,
                    topLeft = Offset(pipeXPx - 4f * scaleX, bottomPipeTopPx),
                    size = Size(pipeWidthPx + 8f * scaleX, 16f * scaleY),
                    cornerRadius = CornerRadius(4f * scaleX, 4f * scaleY)
                )
            }
        }

        // Draw Snail (Player) with tilt rotation
        val birdXPx = FlappyBirdState.BIRD_X * scaleX
        val birdYPx = state.birdY * scaleY
        val birdSizePx = FlappyBirdState.BIRD_SIZE * scaleX

        val rotationAngle = (state.birdVelocity * 3.5f).coerceIn(-30f, 60f)
        val birdCenter = Offset(birdXPx + birdSizePx / 2f, birdYPx + birdSizePx / 2f)

        withTransform({
            rotate(rotationAngle, pivot = birdCenter)
        }) {
            drawImage(
                image = schneaggIcon,
                dstOffset = IntOffset(birdXPx.toInt(), birdYPx.toInt()),
                dstSize = IntSize(birdSizePx.toInt(), birdSizePx.toInt())
            )
        }
    }
}
