package org.lerchenflo.schneaggchatv3mp.games.presentation.stanislaus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_caught
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_glare_hint
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_label_apparent
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_label_real
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_missed
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_sprite_description
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_streak
import schneaggchatv3mp.composeapp.generated.resources.games_stanislaus_title
import schneaggchatv3mp.composeapp.generated.resources.stanislaus
import kotlin.math.min

@Composable
fun StanislausScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewmodel = koinViewModel<StanislausViewModel>()
    val state by viewmodel.state.collectAsStateWithLifecycle()

    var explanationDismissed by rememberSaveable { mutableStateOf(false) }
    val isStarted = state.isPlaying || state.isGameOver

    // After process death the dismissed flag is restored but the ViewModel run
    // is lost — start a fresh run instead of showing the explanation again.
    LaunchedEffect(Unit) {
        if (explanationDismissed && !isStarted) viewmodel.onAction(StanislausAction.StartGame)
    }

    // Leaving the game ends the run so no loop keeps running in the background
    DisposableEffect(Unit) {
        onDispose { viewmodel.onAction(StanislausAction.StopGame) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = stringResource(Res.string.games_stanislaus_title),
            onBackClick = onBackClick
        )

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            StanislausPond(
                state = state,
                onThrow = { dx, dy -> viewmodel.onAction(StanislausAction.Throw(dx, dy)) },
                modifier = Modifier.fillMaxSize()
            )

            if (isStarted) {
                GameHud(
                    score = state.score.toLong(),
                    timeMillis = state.timeRemainingMillis,
                    onStop = {
                        explanationDismissed = false
                        viewmodel.onAction(StanislausAction.StopGame)
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }

            state.reveal?.let { reveal ->
                val labelRes = if (reveal.result == ThrowResult.HIT) {
                    Res.string.games_stanislaus_caught
                } else {
                    Res.string.games_stanislaus_missed
                }
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(labelRes) +
                                if (reveal.result == ThrowResult.HIT) " " + stringResource(Res.string.games_stanislaus_streak, state.streak) else "",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        RevealLegendRow(
                            swatchColor = StanislausSceneColors.ghost,
                            label = stringResource(Res.string.games_stanislaus_label_apparent),
                        )
                        RevealLegendRow(
                            swatchColor = StanislausSceneColors.fish,
                            label = stringResource(Res.string.games_stanislaus_label_real),
                        )
                    }
                }
            }

            if (state.isPlaying && state.reveal == null && (state.currentSighting?.visibility ?: 1f) < 0.15f) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.games_stanislaus_glare_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            if (!explanationDismissed && !isStarted) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_stanislaus_title),
                    explanation = stringResource(Res.string.games_stanislaus_instructions),
                    onStart = {
                        explanationDismissed = true
                        viewmodel.onAction(StanislausAction.StartGame)
                    }
                )
            }

            if (state.isGameOver) {
                GameOverOverlay(
                    game = GameId.STANISLAUS,
                    finalScore = state.score.toLong(),
                    finalTimeMillis = state.elapsedMillis,
                    onRestart = { viewmodel.onAction(StanislausAction.RestartGame) },
                    onExit = {
                        viewmodel.onAction(StanislausAction.StopGame)
                        onBackClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun RevealLegendRow(swatchColor: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(swatchColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** Uniform world<->screen mapping so angles drawn on screen match the underlying physics. */
private data class WorldTransform(val scale: Float, val offsetX: Float, val offsetY: Float) {
    fun toScreen(wx: Float, wy: Float): Offset =
        Offset(offsetX + wx * scale, offsetY + (wy + AIR_HEIGHT) * scale)

    fun toWorld(sx: Float, sy: Float): Offset =
        Offset((sx - offsetX) / scale, (sy - offsetY) / scale - AIR_HEIGHT)
}

private fun worldTransform(canvasWidth: Float, canvasHeight: Float): WorldTransform {
    val worldHeight = AIR_HEIGHT + WATER_DEPTH
    val scale = min(canvasWidth / WORLD_WIDTH, canvasHeight / worldHeight)
    val offsetX = (canvasWidth - WORLD_WIDTH * scale) / 2f
    val offsetY = (canvasHeight - worldHeight * scale) / 2f
    return WorldTransform(scale, offsetX, offsetY)
}

@Composable
private fun StanislausPond(
    state: StanislausState,
    onThrow: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StanislausSceneColors
    val stanislausSprite = imageResource(Res.drawable.stanislaus)
    val spriteDescription = stringResource(Res.string.games_stanislaus_sprite_description)
    var dragWorld by remember { mutableStateOf<Offset?>(null) }
    val latestState by rememberUpdatedState(state)

    Canvas(
        modifier = modifier
            .semantics { contentDescription = spriteDescription }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (!latestState.canAim) return@detectDragGestures
                        val transform = worldTransform(size.width.toFloat(), size.height.toFloat())
                        dragWorld = transform.toWorld(offset.x, offset.y)
                    },
                    onDrag = { change, _ ->
                        if (!latestState.canAim) return@detectDragGestures
                        change.consume()
                        val transform = worldTransform(size.width.toFloat(), size.height.toFloat())
                        dragWorld = transform.toWorld(change.position.x, change.position.y)
                    },
                    onDragEnd = {
                        dragWorld?.let { target ->
                            if (latestState.canAim) {
                                onThrow(target.x - HAND_X, target.y - HAND_Y)
                            }
                        }
                        dragWorld = null
                    },
                    onDragCancel = { dragWorld = null }
                )
            }
    ) {
        val transform = worldTransform(size.width, size.height)

        drawScene(transform, colors)
        drawStanislaus(transform, stanislausSprite)

        state.fish?.let { fish ->
            state.currentSighting?.let { sighting ->
                if (sighting.visibility > 0.02f) {
                    drawFishShape(
                        center = transform.toScreen(sighting.apparentX, sighting.apparentDepth),
                        scale = transform.scale,
                        facingLeft = fish.directionX < 0f,
                        color = colors.ghost,
                        alpha = sighting.visibility,
                    )
                }
            }
        }

        if (state.liveHint && state.reveal == null) {
            state.fish?.let { fish ->
                state.currentSighting?.let { sighting ->
                    drawRayPath(transform, fish.x, fish.depth, sighting, colors.ray.copy(alpha = 0.35f))
                    drawFishShape(
                        center = transform.toScreen(fish.x, fish.depth),
                        scale = transform.scale,
                        facingLeft = fish.directionX < 0f,
                        color = colors.fish.copy(alpha = 0.35f),
                        alpha = 1f,
                    )
                }
            }
        }

        state.reveal?.let { reveal ->
            val resultColor = if (reveal.result == ThrowResult.HIT) colors.hit else colors.miss
            drawRayPath(transform, reveal.fishX, reveal.fishDepth, reveal.sighting, colors.ray)
            drawFishShape(
                center = transform.toScreen(reveal.fishX, reveal.fishDepth),
                scale = transform.scale,
                facingLeft = true,
                color = colors.fish,
                alpha = 1f,
            )
            drawCircle(
                color = resultColor,
                radius = CATCH_RADIUS * transform.scale,
                center = transform.toScreen(reveal.fishX, reveal.fishDepth),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        state.spear?.let { spear ->
            val tip = transform.toScreen(spear.x, spear.y)
            val tail = transform.toScreen(
                spear.x - spear.directionX * 10f,
                spear.y - spear.directionY * 10f,
            )
            drawLine(colors.spear, tail, tip, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }

        dragWorld?.let { target ->
            val hand = transform.toScreen(HAND_X, HAND_Y)
            val end = transform.toScreen(target.x, target.y)
            drawLine(
                color = colors.spear,
                start = hand,
                end = end,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        }
    }
}

private fun DrawScope.drawScene(transform: WorldTransform, colors: StanislausSceneColors) {
    val topLeft = transform.toScreen(0f, -AIR_HEIGHT)
    val surface = transform.toScreen(0f, 0f)
    val bottomRight = transform.toScreen(WORLD_WIDTH, WATER_DEPTH)

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(colors.sky, colors.skyHorizon),
            startY = topLeft.y,
            endY = surface.y,
        ),
        topLeft = topLeft,
        size = androidx.compose.ui.geometry.Size(bottomRight.x - topLeft.x, surface.y - topLeft.y),
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(colors.water, colors.waterDeep),
            startY = surface.y,
            endY = bottomRight.y,
        ),
        topLeft = surface,
        size = androidx.compose.ui.geometry.Size(bottomRight.x - topLeft.x, bottomRight.y - surface.y),
    )

    // Mirror-like glare over the far side of the pond — the reason fish out there fade out.
    val mirrorEnd = transform.toScreen(POND_LEFT + 70f, 0f)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(colors.mirror.copy(alpha = 0.5f), colors.mirror.copy(alpha = 0f)),
            startX = topLeft.x,
            endX = mirrorEnd.x,
        ),
        topLeft = surface,
        size = androidx.compose.ui.geometry.Size(mirrorEnd.x - topLeft.x, bottomRight.y - surface.y),
    )
}

private fun DrawScope.drawStanislaus(transform: WorldTransform, sprite: androidx.compose.ui.graphics.ImageBitmap) {
    val aspect = sprite.width.toFloat() / sprite.height.toFloat()
    val heightWorld = AIR_HEIGHT * 0.9f
    val widthWorld = heightWorld * aspect
    val topLeft = transform.toScreen(STANI_X - widthWorld / 2f, -heightWorld)
    drawImage(
        image = sprite,
        dstOffset = androidx.compose.ui.unit.IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(
            (widthWorld * transform.scale).toInt(),
            (heightWorld * transform.scale).toInt(),
        ),
    )
}

private fun DrawScope.drawRayPath(
    transform: WorldTransform,
    fishX: Float,
    fishDepth: Float,
    sighting: Sighting,
    color: androidx.compose.ui.graphics.Color,
) {
    val fishPoint = transform.toScreen(fishX, fishDepth)
    val surfacePoint = transform.toScreen(sighting.surfaceX, 0f)
    val eyePoint = transform.toScreen(STANI_X, -EYE_HEIGHT)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))

    drawLine(color, fishPoint, surfacePoint, strokeWidth = 2.dp.toPx(), pathEffect = dashEffect)
    drawLine(color, surfacePoint, eyePoint, strokeWidth = 2.dp.toPx(), pathEffect = dashEffect)
}

private fun DrawScope.drawFishShape(
    center: Offset,
    scale: Float,
    facingLeft: Boolean,
    color: androidx.compose.ui.graphics.Color,
    alpha: Float,
) {
    val bodyLength = 9f * scale
    val bodyHeight = 4.5f * scale
    val direction = if (facingLeft) -1f else 1f

    val bodyPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x - direction * bodyLength / 2f, center.y)
        cubicTo(
            center.x - direction * bodyLength / 4f, center.y - bodyHeight / 2f,
            center.x + direction * bodyLength / 4f, center.y - bodyHeight / 2f,
            center.x + direction * bodyLength / 2f, center.y,
        )
        cubicTo(
            center.x + direction * bodyLength / 4f, center.y + bodyHeight / 2f,
            center.x - direction * bodyLength / 4f, center.y + bodyHeight / 2f,
            center.x - direction * bodyLength / 2f, center.y,
        )
        close()
    }
    drawPath(bodyPath, color = color, alpha = alpha)

    val tailPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x - direction * bodyLength / 2f, center.y)
        lineTo(center.x - direction * bodyLength * 0.85f, center.y - bodyHeight / 2f)
        lineTo(center.x - direction * bodyLength * 0.85f, center.y + bodyHeight / 2f)
        close()
    }
    drawPath(tailPath, color = StanislausSceneColors.fishFin, alpha = alpha)
}
