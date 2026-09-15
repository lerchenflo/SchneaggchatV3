package org.lerchenflo.schneaggchatv3mp.games.presentation.towerstack

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GamePauseOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_tower_stack_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_tower_stack_tap_to_place
import schneaggchatv3mp.composeapp.generated.resources.games_stack_tower
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative
import kotlin.math.min
import kotlin.math.roundToInt

/** Side length of the schneagg sprite in game units (a bar is 15 units thick). */
private const val SNAIL_SIZE_UNITS = 40f
/** The silhouette sits vertically centered in its square image; its underside is this far down. */
private const val SNAIL_FOOT_FRACTION = 0.785f
/** The background-colored halo that separates the schneagg from the same-colored bar under it. */
private const val SNAIL_OUTLINE_SCALE = 1.1f
/** Jump-off trajectory in game units per second: forward in driving direction, up, then gravity. */
private const val JUMP_SPEED_X = 90f
private const val JUMP_SPEED_Y = 230f
private const val JUMP_GRAVITY = 900f
/** Nose tilt over the jump: slightly up at take-off, nose down while falling. */
private const val JUMP_TILT_START_DEG = -15f
private const val JUMP_TILT_END_DEG = 40f
/** Fraction of the jump after which the falling schneagg fades out. */
private const val JUMP_FADE_START = 0.6f

@Composable
fun TowerStackScreen(
    // Scoped to the nav entry so the run survives recomposition and onCleared() can persist it
    viewModel: TowerstackViewModel = koinViewModel(),
    onBackClick: () -> Unit = {}
) {
    val gameState by viewModel.gameState.collectAsState()
    val restoreChecked by viewModel.restoreChecked.collectAsState()
    val colors = MaterialTheme.colorScheme
    var explanationDismissed by rememberSaveable { mutableStateOf(false) }

    // After process death the dismissed flag is restored but the ViewModel is new. Wait for the
    // saved-run check first, otherwise this would start a fresh run over the restored one.
    LaunchedEffect(restoreChecked) {
        if (restoreChecked && explanationDismissed && !gameState.isGameStarted) {
            viewModel.onAction(GameAction.StartGame)
        }
    }

    // Leaving the screen pauses and persists the run so it can be picked up again later
    DisposableEffect(Unit) {
        onDispose { viewModel.onAction(GameAction.LeaveGame) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_stack_tower),
            onBackClick = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(colors.background)
                // The bar is placed the moment the finger touches down. A click would only fire on
                // release, and the bar keeps moving during the press, which reads as input lag.
                // Overlays and HUD buttons consume their own presses, so they never place a bar.
                // The ViewModel ignores the action while not started, paused or over.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown().consume()
                        viewModel.onAction(GameAction.PlacePlatform)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            GameContent(
                gameState = gameState,
                onStop = {
                    explanationDismissed = false
                    viewModel.onAction(GameAction.ResetGame)
                },
                onTogglePause = { viewModel.onAction(GameAction.TogglePause) },
                onRestart = {
                    viewModel.onAction(GameAction.ResetGame)
                    viewModel.onAction(GameAction.StartGame)
                },
                onExit = {
                    viewModel.onAction(GameAction.ResetGame)
                    onBackClick()
                }
            )

            if (restoreChecked && !explanationDismissed && !gameState.isGameStarted) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_stack_tower),
                    explanation = stringResource(Res.string.games_tower_stack_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewModel.onAction(GameAction.StartGame)
                    }
                )
            }
        }
    }
}

@Composable
private fun GameContent(
    gameState: GameState,
    onStop: () -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val snailImage = imageResource(Res.drawable.icon_schneagg_alternative)
    val snailOutline = colors.background
    val currentPlatformY = gameState.currentPlatform?.y ?: (gameState.platforms.lastOrNull()?.y ?: 450f)
    val targetCameraOffsetY = (250f - currentPlatformY).coerceAtLeast(0f)
    val cameraOffsetY by animateFloatAsState(
        targetValue = targetCameraOffsetY,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "towerCameraOffset"
    )
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Game canvas - now uses full available space
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 80.dp)
                // A schneagg that jumped off falls out of the play area, not over the HUD
                .clipToBounds()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Scale game coordinates to canvas size
            val scaleX = canvasWidth / 300f
            val scaleY = canvasHeight / 500f
            // The sprite keeps its aspect ratio even though the play area is stretched
            val snailSize = SNAIL_SIZE_UNITS * min(scaleX, scaleY)

            // Schneaggs that hopped off a placed bar fall behind the tower
            gameState.snailJumps.forEach { jump ->
                val t = (gameState.elapsedMillis - jump.atElapsedMillis) / SNAIL_JUMP_MS.toFloat()
                if (t < 0f || t >= 1f) return@forEach
                val seconds = t * SNAIL_JUMP_MS / 1000f
                val x = jump.x + jump.direction * JUMP_SPEED_X * seconds
                val y = jump.y - JUMP_SPEED_Y * seconds + 0.5f * JUMP_GRAVITY * seconds * seconds
                val alpha = ((1f - t) / (1f - JUMP_FADE_START)).coerceIn(0f, 1f)
                drawSnail(
                    image = snailImage,
                    centerX = x * scaleX,
                    footY = (y + cameraOffsetY) * scaleY,
                    size = snailSize,
                    facingLeft = jump.direction < 0f,
                    tiltDeg = jump.direction * (JUMP_TILT_START_DEG + (JUMP_TILT_END_DEG - JUMP_TILT_START_DEG) * t),
                    color = getRainbowColor(jump.colorIndex).copy(alpha = alpha),
                    outline = snailOutline.copy(alpha = alpha),
                )
            }
            
            // Draw placed platforms with rainbow colors
            gameState.platforms.forEachIndexed { index, platform ->
                val color = getRainbowColor(index)
                drawRoundedPlatform(
                    platform = platform,
                    color = color,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    cameraOffsetY = cameraOffsetY
                )
            }
            
            // Draw current moving platform with its schneagg riding on top
            gameState.currentPlatform?.let { platform ->
                val color = if (gameState.isGameOver) 
                    Color.Red 
                else 
                    getRainbowColor(gameState.platforms.size)
                drawRoundedPlatform(
                    platform = platform,
                    color = color,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    cameraOffsetY = cameraOffsetY
                )
                // Sits in the middle of the bar and looks where the bar is heading
                drawSnail(
                    image = snailImage,
                    centerX = (platform.x + platform.width / 2f) * scaleX,
                    footY = (platform.y + cameraOffsetY) * scaleY,
                    size = snailSize,
                    facingLeft = platform.direction < 0f,
                    tiltDeg = 0f,
                    color = color,
                    outline = snailOutline,
                )
            }
        }
        
        // Unified time/points counter with stop button
        if (gameState.isGameStarted) {
            GameHud(
                score = gameState.score.toLong(),
                timeMillis = gameState.elapsedMillis,
                onStop = onStop,
                isPaused = gameState.isPaused,
                onTogglePause = onTogglePause,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }


        // Unified game over overlay with restart, difficulty selection and highscores
        if (gameState.isGameOver) {
            GameOverOverlay(
                game = GameId.TOWERSTACK,
                finalScore = gameState.score.toLong(),
                finalTimeMillis = gameState.elapsedMillis,
                onRestart = onRestart,
                onExit = onExit
            )
        } else if (gameState.isPaused) {
            GamePauseOverlay(onResume = onTogglePause)
        }
        
        // Instructions for first move
        if (gameState.isGameStarted && gameState.score == 0 && !gameState.isGameOver) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = colors.secondaryContainer.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.games_tower_stack_tap_to_place),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawRoundedPlatform(
    platform: Platform,
    color: Color,
    scaleX: Float,
    scaleY: Float,
    cameraOffsetY: Float = 0f
) {
    val scaledX = platform.x * scaleX
    val scaledY = (platform.y + cameraOffsetY) * scaleY
    val scaledWidth = platform.width * scaleX
    val scaledHeight = maxOf(platform.height * scaleY, 4f) // Minimum thickness for visibility
    
    drawRoundRect(
        color = color,
        topLeft = Offset(scaledX, scaledY),
        size = Size(scaledWidth, scaledHeight),
        cornerRadius = CornerRadius(scaledHeight / 2f) // Rounded corners based on thickness
    )
}

/**
 * Draws the schneagg sprite (which faces right) with its underside at [footY], centered on
 * [centerX]. [facingLeft] mirrors it so it always looks in driving direction; [tiltDeg] is
 * the nose tilt in that direction (positive = nose down).
 */
private fun DrawScope.drawSnail(
    image: ImageBitmap,
    centerX: Float,
    footY: Float,
    size: Float,
    facingLeft: Boolean,
    tiltDeg: Float,
    color: Color,
    outline: Color,
) {
    val pivot = Offset(size / 2f, size * SNAIL_FOOT_FRACTION)
    val outlineSize = size * SNAIL_OUTLINE_SCALE
    val outlineInset = ((size - outlineSize) / 2f).roundToInt()
    withTransform({
        translate(centerX - size / 2f, footY - size * SNAIL_FOOT_FRACTION)
        rotate(tiltDeg, pivot = pivot)
        if (facingLeft) scale(-1f, 1f, pivot = pivot)
    }) {
        drawImage(
            image = image,
            dstOffset = IntOffset(outlineInset, outlineInset),
            dstSize = IntSize(outlineSize.roundToInt(), outlineSize.roundToInt()),
            colorFilter = ColorFilter.tint(outline),
            filterQuality = FilterQuality.Medium,
        )
        drawImage(
            image = image,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.roundToInt(), size.roundToInt()),
            colorFilter = ColorFilter.tint(color),
            filterQuality = FilterQuality.Medium,
        )
    }
}

private fun getRainbowColor(index: Int): Color {
    val colors = listOf(
        Color(0xFFFF0000), // Red
        Color(0xFFFF7F00), // Orange
        Color(0xFF00FF00), // Green
        Color(0xFF0000FF)  // Blue
    )
    return colors[index % colors.size]
}

/** Mid-run state: five placed bars, the sixth moving left with its schneagg, and one schneagg mid-jump. */
private fun previewTowerStackState(): GameState {
    val elapsedMillis = 12_400L
    return GameState(
        platforms = listOf(
            Platform(x = 110f, y = 450f, width = 80f, height = 15f),
            Platform(x = 118f, y = 435f, width = 72f, height = 15f),
            Platform(x = 118f, y = 420f, width = 66f, height = 15f),
            Platform(x = 124f, y = 405f, width = 60f, height = 15f),
            Platform(x = 124f, y = 390f, width = 55f, height = 15f),
        ),
        currentPlatform = Platform(x = 60f, y = 375f, width = 55f, height = 15f, isMoving = true, direction = -1f),
        snailJumps = listOf(
            SnailJump(x = 151f, y = 390f, direction = 1f, colorIndex = 4, atElapsedMillis = elapsedMillis - 300L),
        ),
        score = 4,
        isGameStarted = true,
        elapsedMillis = elapsedMillis,
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun TowerStackMidGamePreview() {
    SchneaggchatTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            GameContent(
                gameState = previewTowerStackState(),
                onStop = {},
                onTogglePause = {},
                onRestart = {},
                onExit = {},
            )
        }
    }
}
