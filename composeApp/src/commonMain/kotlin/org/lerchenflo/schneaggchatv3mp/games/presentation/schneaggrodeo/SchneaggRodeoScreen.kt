package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
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
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.BackButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_catch_horse
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_fence_height
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_lasso
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_next_to_beat
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_snails
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_super_jump
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_title
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative

private val GAME_HEIGHT = 220.dp
private val COMPACT_LANDSCAPE_MAX_HEIGHT = 480.dp

/** The silhouette sits vertically centered in its square image; its underside is this far down. */
private const val SNAIL_FOOT_FRACTION = 0.785f
/** The background-colored halo that separates the schneagg from poles behind it. */
private const val SNAIL_OUTLINE_SCALE = 1.1f
private const val RIDER_MAX_LEAN_DEGREES = 32f

/** Highscore marker posts; staggered ones are shorter so neighbouring labels don't stack up. */
private const val MARKER_POST_HEIGHT = 36f
private const val MARKER_POST_HEIGHT_STAGGERED = 29f

// Same rainbow palette as the TowerStack game (explicitly requested for the poles)
private val POLE_COLORS = listOf(
    Color(0xFFFF0000), // Red
    Color(0xFFFF7F00), // Orange
    Color(0xFF00FF00), // Green
    Color(0xFF0000FF)  // Blue
)

// Cowboy hat, explicitly requested as a fixed orange
private val HAT_COLOR = Color(0xFFFF8C00)

@Composable
fun SchneaggRodeoRoot(
    onBackClick: () -> Unit,
    viewModel: SchneaggRodeoViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Not delegated: the canvas reads it in the draw phase, so a new frame only redraws
    val frame = viewModel.frame.collectAsStateWithLifecycle()
    val restoreChecked by viewModel.restoreChecked.collectAsStateWithLifecycle()

    var explanationDismissed by rememberSaveable { mutableStateOf(false) }
    val isStarted = state.isPlaying || state.isGameOver

    // After process death the dismissed flag is restored but the ViewModel is new. Wait for the
    // saved-run check first, otherwise this would start a fresh run over the restored one.
    LaunchedEffect(restoreChecked) {
        if (restoreChecked && explanationDismissed && !isStarted) {
            viewModel.onAction(SchneaggRodeoAction.StartGame)
        }
    }

    // Leaving the screen pauses and persists the run so it can be picked up again later
    DisposableEffect(Unit) {
        onDispose { viewModel.onAction(SchneaggRodeoAction.LeaveGame) }
    }

    FrameClock(
        running = state.isPlaying && !state.isPaused,
        onFrame = { viewModel.onAction(SchneaggRodeoAction.OnFrame(it)) },
    )

    SchneaggRodeoScreen(
        state = state,
        frame = { frame.value },
        showStartOverlay = restoreChecked && !explanationDismissed && !isStarted,
        onAction = { action ->
            when (action) {
                SchneaggRodeoAction.StartGame -> explanationDismissed = true
                SchneaggRodeoAction.StopGame -> explanationDismissed = false
                else -> Unit
            }
            viewModel.onAction(action)
        },
        onBackClick = onBackClick,
    )
}

/**
 * Ticks once per display frame while [running]. The world is stepped from here rather than from a
 * delay loop in the ViewModel so it moves in step with vsync, which keeps the scrolling smooth.
 */
@Composable
private fun FrameClock(running: Boolean, onFrame: (Float) -> Unit) {
    val currentOnFrame by rememberUpdatedState(onFrame)
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var lastFrame = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            currentOnFrame((now - lastFrame) / 1_000_000_000f)
            lastFrame = now
        }
    }
}

/**
 * A cowboy on a horse jumps show-jumping fences while a pack of schneaggs chases him. Tap (or
 * space / arrow up on desktop) to jump, hold to jump higher, throw the lasso (button or L / arrow
 * down) to catch snails, and fire a super jump (button or J / arrow right) once charged.
 * [frame] is read in the draw phase only.
 */
@Composable
fun SchneaggRodeoScreen(
    state: SchneaggRodeoState,
    frame: () -> SchneaggRodeoFrame,
    showStartOverlay: Boolean,
    onAction: (SchneaggRodeoAction) -> Unit,
    onBackClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val currentOnAction by rememberUpdatedState(onAction)
    val isStarted = state.isPlaying || state.isGameOver

    val focusRequester = remember { FocusRequester() }
    // The overlays' buttons take the keyboard focus; hand it back to the play area whenever riding
    // (re)starts so space / L / J keep working on desktop.
    val riding = state.isPlaying && !state.isPaused
    LaunchedEffect(riding) { focusRequester.requestFocus() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Landscape phones are short: the title bar goes, a floating back button keeps the way out,
        // and the play area moves to the bottom so the HUD has the top right to itself. Wide but
        // tall screens (desktop windows, tablets) keep the normal layout.
        val isCompactLandscape = maxWidth > maxHeight && maxHeight < COMPACT_LANDSCAPE_MAX_HEIGHT

        Column(modifier = Modifier.fillMaxSize()) {
            if (!isCompactLandscape) {
                ActivityTitle(
                    title = stringResource(Res.string.games_schneaggrodeo_title),
                    onBackClick = onBackClick
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(colors.background)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        val isJumpKey = event.key == Key.Spacebar || event.key == Key.DirectionUp || event.key == Key.W
                        val isLassoKey = event.key == Key.L || event.key == Key.DirectionDown || event.key == Key.S
                        val isSuperJumpKey = event.key == Key.J || event.key == Key.DirectionRight
                        when {
                            isLassoKey && event.type == KeyEventType.KeyDown -> {
                                currentOnAction(SchneaggRodeoAction.OnLassoClick); true
                            }
                            isSuperJumpKey && event.type == KeyEventType.KeyDown -> {
                                currentOnAction(SchneaggRodeoAction.OnSuperJumpClick); true
                            }
                            !isJumpKey -> false
                            event.type == KeyEventType.KeyDown -> {
                                currentOnAction(SchneaggRodeoAction.OnJumpPressed); true
                            }
                            event.type == KeyEventType.KeyUp -> {
                                currentOnAction(SchneaggRodeoAction.OnJumpReleased); true
                            }
                            else -> false
                        }
                    }
                    // The whole play area is the jump button; the lasso / super jump buttons, the HUD
                    // and the overlays consume their own presses. Holding keeps the jump boosted.
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            currentOnAction(SchneaggRodeoAction.OnJumpPressed)
                            tryAwaitRelease()
                            currentOnAction(SchneaggRodeoAction.OnJumpReleased)
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .align(if (isCompactLandscape) Alignment.BottomCenter else Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = if (isCompactLandscape) 8.dp else 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.games_schneaggrodeo_snails, state.snailsCaught),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    RodeoTrack(
                        frame = frame,
                        onSizeChanged = { size ->
                            onAction(SchneaggRodeoAction.OnWorldSizeChanged(size.width, size.height))
                        },
                        // Takes what is left next to the counter and the buttons, at most GAME_HEIGHT -
                        // a landscape phone gets a lower (and wider) track instead of cut-off buttons.
                        // Everything inside scales with the track height.
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = GAME_HEIGHT)
                    )

                    RodeoControls(
                        superJumpCharges = state.superJumpCharges,
                        isOnFoot = state.isOnFoot,
                        enabled = state.isPlaying && !state.isPaused,
                        onAction = onAction,
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }

                if (isStarted) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        GameHud(
                            score = state.score.toLong(),
                            timeMillis = state.runTimeMillis,
                            onStop = { onAction(SchneaggRodeoAction.StopGame) },
                            isPaused = state.isPaused,
                            onTogglePause = { onAction(SchneaggRodeoAction.TogglePause) },
                        )
                        state.nextToBeat?.let { ghost ->
                            Text(
                                text = stringResource(Res.string.games_schneaggrodeo_next_to_beat, ghost.username, ghost.score),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .padding(top = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }

                if (showStartOverlay) {
                    GameStartOverlay(
                        title = stringResource(Res.string.games_schneaggrodeo_title),
                        explanation = stringResource(Res.string.games_schneaggrodeo_instructions),
                        onStart = { onAction(SchneaggRodeoAction.StartGame) }
                    )
                }

                if (state.isGameOver) {
                    GameOverOverlay(
                        game = GameId.SCHNEAGG_RODEO,
                        finalScore = state.score.toLong(),
                        finalTimeMillis = state.runTimeMillis,
                        onRestart = { onAction(SchneaggRodeoAction.RestartGame) },
                        onExit = {
                            onAction(SchneaggRodeoAction.StopGame)
                            onBackClick()
                        }
                    )
                } else if (state.isPaused) {
                    GamePauseOverlay(onResume = { onAction(SchneaggRodeoAction.TogglePause) })
                }
            }
        }

        if (isCompactLandscape) {
            BackButton(
                onBackClick = onBackClick,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun RodeoControls(
    superJumpCharges: Int,
    isOnFoot: Boolean,
    enabled: Boolean,
    onAction: (SchneaggRodeoAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Super jump on the left, lasso on the right - one per thumb
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = superJumpCharges > 0 && !isOnFoot,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Button(
                onClick = { onAction(SchneaggRodeoAction.OnSuperJumpClick) },
                enabled = enabled,
                modifier = Modifier.focusProperties { canFocus = false }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardDoubleArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(Res.string.games_schneaggrodeo_super_jump, superJumpCharges),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isOnFoot) {
            // Thrown off: the lasso is the way back into the saddle, so it pulses in primary
            val pulse by rememberInfiniteTransition(label = "lassoPulse").animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
                label = "lassoPulseScale"
            )
            Button(
                onClick = { onAction(SchneaggRodeoAction.OnLassoClick) },
                enabled = enabled,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .focusProperties { canFocus = false }
            ) {
                Text(stringResource(Res.string.games_schneaggrodeo_catch_horse))
            }
        } else {
            FilledTonalButton(
                onClick = { onAction(SchneaggRodeoAction.OnLassoClick) },
                enabled = enabled,
                // Keeps keyboard focus on the play area so space / L keep working after a click
                modifier = Modifier.focusProperties { canFocus = false }
            ) {
                Text(stringResource(Res.string.games_schneaggrodeo_lasso))
            }
        }
    }
}

/** The track: ground, highscore markers, fences, snails, the chasing pack, horse, rider and lasso. */
@Composable
private fun RodeoTrack(
    frame: () -> SchneaggRodeoFrame,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    // Only 13 possible heights - resolved in composition since the canvas draws outside of it
    val heightLabels = (MIN_FENCE_CM..MAX_FENCE_CM step 10).associateWith {
        stringResource(Res.string.games_schneaggrodeo_fence_height, it)
    }
    val heightLabelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val bodyLabelStyle = MaterialTheme.typography.labelMedium.copy(color = colors.surface, fontWeight = FontWeight.Bold)
    val markerLabelStyle = MaterialTheme.typography.labelSmall
    val snailImage = imageResource(Res.drawable.icon_schneagg_alternative)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainer,
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged(onSizeChanged)
        ) {
            val world = frame() // read here so every frame only redraws the canvas
            val unit = size.height / WORLD_HEIGHT_UNITS
            val groundY = size.height - GROUND_OFFSET_UNITS * unit
            val snailOutline = colors.surfaceContainer

            fun drawSnailAt(snail: RodeoSnailUi) = drawSnail(
                image = snailImage,
                centerX = snail.x * unit,
                footY = groundY - snail.height * unit,
                size = SNAIL_SIZE * unit,
                facingLeft = snail.facingLeft,
                tiltDeg = snail.tiltDeg,
                outline = snailOutline
            )

            drawGround(groundY, unit, world.distance, colors.onSurfaceVariant)

            // Highscore markers stand behind everything else on the track
            world.markers.forEach { marker ->
                drawMarker(
                    marker = marker,
                    groundY = groundY,
                    unit = unit,
                    postHeight = if (marker.staggered) MARKER_POST_HEIGHT_STAGGERED else MARKER_POST_HEIGHT,
                    color = if (marker.isOwn) colors.primary else colors.secondary,
                    labelStyle = markerLabelStyle,
                    textMeasurer = textMeasurer
                )
            }

            world.fences.forEach { fence ->
                drawFence(fence, groundY, unit, colors.onSurface, heightLabels.getValue(fence.heightCm), heightLabelStyle, textMeasurer)
            }

            world.snails.forEach { drawSnailAt(it) }
            world.pack.forEach { drawSnailAt(it) }

            drawHorseAndRider(
                left = (HORSE_X + world.horse.offsetX) * unit,
                groundY = groundY - world.horse.height * unit,
                unit = unit,
                pose = world.horse,
                glowColor = colors.primary,
                color = colors.onSurface,
                shirtColor = colors.primary,
                bodyLabel = if (world.snailsCaught > 0) textMeasurer.measure(world.snailsCaught.toString(), bodyLabelStyle) else null
            )

            world.cowboy?.let { cowboy ->
                drawCowboy(
                    cowboy = cowboy,
                    groundY = groundY,
                    unit = unit,
                    color = colors.onSurface,
                    shirtColor = colors.primary
                )
            }

            world.splashProgress?.let { splashProgress ->
                // Front and hind hooves both kick up a little spray
                listOf(HORSE_X + 7f, HORSE_X + 18f).forEach { hoofX ->
                    drawLandingSplash(hoofX * unit, groundY, unit, splashProgress, colors.onSurfaceVariant)
                }
            }

            world.dust?.let { dust ->
                drawDust(
                    x = dust.x * unit,
                    groundY = groundY,
                    unit = unit,
                    progress = dust.progress,
                    color = colors.onSurfaceVariant
                )
            }

            world.lasso?.let { lasso ->
                val hand = Offset(lasso.handX * unit, groundY - lasso.handY * unit)
                val tip = Offset(lasso.tipX * unit, groundY - lasso.tipY * unit)
                drawLasso(hand, tip, unit, colors.tertiary)
                lasso.caught?.let { drawSnailAt(it) }
            }
        }
    }
}

private fun DrawScope.drawGround(groundY: Float, unit: Float, distance: Float, color: Color) {
    drawLine(color, Offset(0f, groundY), Offset(size.width, groundY), strokeWidth = unit * 0.6f)

    // Small pebbles scrolling with the ground, so the speed is visible between fences
    val spacing = 9f * unit
    val shift = (distance * unit) % spacing
    var x = -shift
    var index = (distance * unit / spacing).toInt()
    while (x < size.width) {
        val depth = 1.5f + (index * 7 % 5) * 0.8f
        val length = (1f + index * 5 % 3) * unit
        drawLine(
            color = color.copy(alpha = 0.5f),
            start = Offset(x, groundY + depth * unit),
            end = Offset(x + length, groundY + depth * unit),
            strokeWidth = unit * 0.5f,
            cap = StrokeCap.Round
        )
        x += spacing
        index++
    }
}

/**
 * A highscore marker: a thin post with a pennant, the player's name and score above it. The post
 * stands where the run of that highscore ended.
 */
private fun DrawScope.drawMarker(
    marker: RodeoMarkerUi,
    groundY: Float,
    unit: Float,
    postHeight: Float,
    color: Color,
    labelStyle: TextStyle,
    textMeasurer: TextMeasurer,
) {
    val x = marker.x * unit
    val top = groundY - postHeight * unit
    drawLine(
        color = color.copy(alpha = 0.7f),
        start = Offset(x, groundY),
        end = Offset(x, top),
        strokeWidth = 0.5f * unit,
        cap = StrokeCap.Round
    )
    drawPath(
        path = Path().apply {
            moveTo(x, top)
            lineTo(x + 6f * unit, top + 2f * unit)
            lineTo(x, top + 4f * unit)
            close()
        },
        color = color
    )

    val label = textMeasurer.measure(
        text = "${marker.username}\n${marker.score}",
        style = labelStyle.copy(color = color, fontWeight = if (marker.isOwn) FontWeight.Bold else FontWeight.Medium),
        maxLines = 2,
    )
    val labelX = x - label.size.width / 2f
    if (labelX + label.size.width > 0f && labelX < size.width) {
        drawText(label, topLeft = Offset(labelX, max(0f, top - label.size.height - unit)))
    }
}

private fun DrawScope.drawFence(
    fence: RodeoFenceUi,
    groundY: Float,
    unit: Float,
    woodColor: Color,
    heightLabel: String,
    heightLabelStyle: TextStyle,
    textMeasurer: TextMeasurer
) {
    val left = fence.x * unit
    val right = (fence.x + fence.width) * unit
    val postWidth = 1.2f * unit
    val postHeight = (fence.top + 2.5f) * unit

    // Wooden standards with little feet
    listOf(left, right - postWidth).forEach { postX ->
        drawRect(woodColor, Offset(postX, groundY - postHeight), Size(postWidth, postHeight))
        drawRect(
            woodColor,
            Offset(postX - unit, groundY - 0.8f * unit),
            Size(postWidth + 2f * unit, 0.8f * unit)
        )
    }

    // Striped poles resting in the cups, every pole in its own color. Knocked poles lie stacked on
    // the ground in front of the standards.
    val poleThickness = POLE_THICKNESS * unit
    val stripeWidth = 2f * unit
    fence.poleHeights.forEachIndexed { index, height ->
        val poleColor = POLE_COLORS[(fence.colorOffset + index) % POLE_COLORS.size]
        val poleBottom = if (fence.knocked) index * POLE_THICKNESS else height - POLE_THICKNESS
        val poleY = groundY - (poleBottom + POLE_THICKNESS) * unit
        var x = left
        var stripe = 0
        while (x < right) {
            val width = min(stripeWidth, right - x)
            drawRect(
                color = if (stripe % 2 == 0) poleColor else poleColor.copy(alpha = 0.45f),
                topLeft = Offset(x, poleY),
                size = Size(width, poleThickness)
            )
            x += stripeWidth
            stripe++
        }
    }

    // Height label below the ground, centered under the fence
    val label = textMeasurer.measure(heightLabel, heightLabelStyle)
    val labelX = (left + right) / 2f - label.size.width / 2f
    if (labelX + label.size.width > 0f && labelX < size.width) {
        drawText(label, topLeft = Offset(labelX, groundY + 2f * unit))
    }
}

/**
 * Draws the schneagg sprite (which faces right) with its underside at [footY], centered on
 * [centerX], with a halo in [outline]. [facingLeft] mirrors it; [tiltDeg] tilts it around its foot.
 */
private fun DrawScope.drawSnail(
    image: ImageBitmap,
    centerX: Float,
    footY: Float,
    size: Float,
    facingLeft: Boolean,
    tiltDeg: Float,
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
            filterQuality = FilterQuality.Medium,
        )
    }
}

/** Rope from the rider's [hand] sagging slightly towards the loop at [tip]. */
private fun DrawScope.drawLasso(hand: Offset, tip: Offset, unit: Float, color: Color) {
    val control = Offset((hand.x + tip.x) / 2f, max(hand.y, tip.y) + 2f * unit)
    drawPath(
        path = Path().apply {
            moveTo(hand.x, hand.y)
            quadraticTo(control.x, control.y, tip.x, tip.y)
        },
        color = color,
        style = Stroke(width = 0.5f * unit, cap = StrokeCap.Round)
    )
    drawCircle(color, radius = 1.8f * unit, center = tip, style = Stroke(width = 0.5f * unit))
}

/** Small dirt droplets flying out of a hoof in short arcs to both sides, fading as they fall. */
private fun DrawScope.drawLandingSplash(x: Float, groundY: Float, unit: Float, progress: Float, color: Color) {
    val alpha = 0.8f * (1f - progress)
    // (horizontal speed, vertical speed) per droplet, in units over the whole splash
    listOf(-5f to 4f, -3f to 6f, -1.5f to 7f, 1.5f to 7f, 3f to 6f, 5f to 4f).forEach { (vx, vy) ->
        val dropX = x + vx * progress * unit
        // Rises and falls back within the splash: parabola peaking halfway
        val dropY = groundY - vy * 4f * progress * (1f - progress) * unit
        drawCircle(color.copy(alpha = alpha), radius = 0.45f * unit, center = Offset(dropX, dropY))
    }
}

/** Three puffs rolling out of the hooves and fading, on stumbles and super jump landings. */
private fun DrawScope.drawDust(x: Float, groundY: Float, unit: Float, progress: Float, color: Color) {
    val alpha = 0.45f * (1f - progress)
    listOf(-1f, 0f, 1f).forEachIndexed { index, direction ->
        val spread = direction * 5f * progress * unit
        val rise = (1f + index % 2) * 1.5f * progress * unit
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = (1.2f + 2.3f * progress) * unit,
            center = Offset(x + spread, groundY - rise - unit)
        )
    }
}

/**
 * Side view of the horse (facing right) with the cowboy on top, drawn on a 30 x 28 unit grid.
 * Grid coordinates are y-up from the hooves; [p] converts them to canvas pixels.
 *
 * Pose: the whole figure is rotated by [RodeoHorsePose.pitchDegrees] (positive = nose down) around
 * the pivot (grid coordinates). hindLegScale > 1 pumps up the hind legs for the super jump, glow
 * lights them up. frontLegFold buckles the front legs at the knee and hatLift pops the hat off the
 * head, both for stumbling.
 */
private fun DrawScope.drawHorseAndRider(
    left: Float,
    groundY: Float,
    unit: Float,
    pose: RodeoHorsePose,
    glowColor: Color,
    color: Color,
    shirtColor: Color,
    bodyLabel: TextLayoutResult?
) {
    val gaitPhase = pose.gaitPhase
    val airborne = pose.airborne
    val hindLegScale = pose.hindLegScale
    val frontLegFold = pose.frontLegFold
    val hatLift = pose.hatLift
    val glow = pose.glow

    fun p(x: Float, y: Float) = Offset(left + x * unit, groundY - y * unit)
    fun polygon(vararg points: Pair<Float, Float>) = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(p(x, y).x, p(x, y).y) else lineTo(p(x, y).x, p(x, y).y) }
        close()
    }

    rotate(degrees = pose.pitchDegrees, pivot = p(pose.pivotX, pose.pivotY)) {
        val legStroke = 1.6f * unit
        // Grown hind legs also get thicker, so they read as pumped up and not just stretched
        val hindStroke = legStroke * (1f + (hindLegScale - 1f) * 1.5f)
        val bounce = if (airborne) 0f else sin(gaitPhase * 2f) * 0.4f

        if (glow > 0f) {
            drawCircle(
                color = glowColor.copy(alpha = 0.35f * glow),
                radius = 7f * hindLegScale * unit,
                center = p(7f, 9f - 4.5f * hindLegScale)
            )
        }

        // Legs
        if (airborne) {
            // Front legs tucked, hind legs stretched back - the classic jumping pose
            listOf(17f, 19f).forEach { x ->
                drawLine(color, p(x, 9f), p(x + 2.5f, 5f), legStroke, StrokeCap.Round)
                drawLine(color, p(x + 2.5f, 5f), p(x + 0.5f, 3f), legStroke, StrokeCap.Round)
            }
            listOf(6f, 8f).forEach { x ->
                drawLine(color, p(x, 9f), p(x - 4f * hindLegScale, 9f - 7f * hindLegScale), hindStroke, StrokeCap.Round)
            }
        } else {
            val swing = sin(gaitPhase) * 2.2f
            // Front legs: straight while galloping, knee buckling forward while stumbling
            listOf(17f to swing, 19f to -swing).forEach { (x, legSwing) ->
                val knee = p(x + legSwing / 2f + 2.5f * frontLegFold, 4.5f + 0.5f * frontLegFold)
                val hoof = p(x + legSwing - 1f * frontLegFold, 3.5f * frontLegFold)
                drawLine(color, p(x, 9f), knee, legStroke, StrokeCap.Round)
                drawLine(color, knee, hoof, legStroke, StrokeCap.Round)
            }
            listOf(6f to -swing, 8f to swing).forEach { (x, legSwing) ->
                drawLine(
                    color,
                    p(x, 9f),
                    p(x + legSwing * hindLegScale, 9f - 9f * hindLegScale),
                    hindStroke,
                    StrokeCap.Round
                )
            }
        }

        // Hindquarter muscle bulging with the legs
        if (hindLegScale > 1.02f) {
            drawCircle(color, radius = 3f * hindLegScale * unit, center = p(7f, 11.5f + bounce))
        }

        // Body
        drawRoundRect(
            color = color,
            topLeft = p(4f, 15f + bounce),
            size = Size(16f * unit, 7f * unit),
            cornerRadius = CornerRadius(3f * unit)
        )

        // Caught snails, painted on the horse's side like a race number
        if (bodyLabel != null) {
            val center = p(8f, 11.5f + bounce) // rear half, clear of the rider's leg
            drawText(bodyLabel, topLeft = center - Offset(bodyLabel.size.width / 2f, bodyLabel.size.height / 2f))
        }

        // Tail
        val tailSwing = if (airborne) 2f else sin(gaitPhase + PI.toFloat()) * 1f
        drawPath(
            path = Path().apply {
                val start = p(4.5f, 14f + bounce)
                val control = p(1f, 14f + tailSwing)
                val end = p(0.5f, 7f + tailSwing)
                moveTo(start.x, start.y)
                quadraticTo(control.x, control.y, end.x, end.y)
            },
            color = color,
            style = Stroke(width = 1.4f * unit, cap = StrokeCap.Round)
        )

        // Neck, head and ear
        drawPath(polygon(15f to 15f + bounce, 20f to 12f + bounce, 24f to 21f, 20f to 22.5f), color)
        drawPath(polygon(20f to 22.5f, 24f to 23f, 29f to 19f, 28f to 17f, 23f to 18.5f), color)
        drawPath(polygon(21f to 22.5f, 22f to 25f, 23f to 22.8f), color)

        // Cowboy: leg stays at the horse's side, the upper body pivots forward at the seat in jumps.
        // Once he is thrown off he is drawn separately (drawCowboy).
        if (!pose.hasRider) return@rotate
        val riderY = bounce * 1.5f
        drawLine(color, p(12f, 16f + riderY), p(13.5f, 12f + riderY), 1.3f * unit, StrokeCap.Round)
        rotate(degrees = RIDER_MAX_LEAN_DEGREES * pose.riderLean, pivot = p(12f, 16f + riderY)) {
            drawRoundRect(
                color = shirtColor,
                topLeft = p(10.5f, 21.5f + riderY),
                size = Size(3f * unit, 6f * unit),
                cornerRadius = CornerRadius(1f * unit)
            )
            drawLine(shirtColor, p(13f, 20.5f + riderY), p(18f, 17.5f + riderY), 1f * unit, StrokeCap.Round)
            drawCircle(color, radius = 1.8f * unit, center = p(12.2f, 23.6f + riderY))
            // Hat, popping off the head (and tilting back) when the horse stumbles
            rotate(degrees = -25f * hatLift / 3f, pivot = p(12.2f, 25.6f + riderY + hatLift)) {
                drawRoundRect(
                    color = HAT_COLOR,
                    topLeft = p(9f, 25.6f + riderY + hatLift),
                    size = Size(6.4f * unit, 0.7f * unit),
                    cornerRadius = CornerRadius(0.35f * unit)
                )
                drawRoundRect(
                    color = HAT_COLOR,
                    topLeft = p(10.4f, 28f + riderY + hatLift),
                    size = Size(3.6f * unit, 2.6f * unit),
                    cornerRadius = CornerRadius(0.8f * unit)
                )
            }
        }
    }
}

/**
 * The cowboy on foot, same proportions as the rider on the horse. Grid is y-up from his feet with
 * x = 0 at his center; he faces right unless mirrored, and rotates around his middle.
 */
private fun DrawScope.drawCowboy(
    cowboy: RodeoCowboyUi,
    groundY: Float,
    unit: Float,
    color: Color,
    shirtColor: Color,
) {
    val centerX = cowboy.x * unit
    val feetY = groundY - cowboy.height * unit
    fun p(x: Float, y: Float) = Offset(centerX + x * unit, feetY - y * unit)
    val middle = p(0f, 9f)

    withTransform({
        rotate(cowboy.rotation, pivot = middle)
        if (cowboy.facingLeft) scale(-1f, 1f, pivot = middle)
    }) {
        // Legs, torso and the arm holding the rope (reaching forward)
        drawLine(color, p(-0.7f, 6f), p(-1.2f, 0f), 1.3f * unit, StrokeCap.Round)
        drawLine(color, p(0.7f, 6f), p(1.2f, 0f), 1.3f * unit, StrokeCap.Round)
        drawRoundRect(
            color = shirtColor,
            topLeft = p(-1.5f, 12f),
            size = Size(3f * unit, 6f * unit),
            cornerRadius = CornerRadius(1f * unit)
        )
        drawLine(shirtColor, p(1f, 11f), p(2f, 10f), 1f * unit, StrokeCap.Round)
        drawCircle(color, radius = 1.8f * unit, center = p(0.2f, 14.1f))
        val hatY = cowboy.hatLift
        drawRoundRect(
            color = HAT_COLOR,
            topLeft = p(-3f, 16.1f + hatY),
            size = Size(6.4f * unit, 0.7f * unit),
            cornerRadius = CornerRadius(0.35f * unit)
        )
        drawRoundRect(
            color = HAT_COLOR,
            topLeft = p(-1.6f, 18.5f + hatY),
            size = Size(3.6f * unit, 2.6f * unit),
            cornerRadius = CornerRadius(0.8f * unit)
        )
    }
}

/** Mid-run: the horse clearing a fence, a crawler on the next one, a lasso out and two highscore markers ahead. */
private fun previewFrame(): SchneaggRodeoFrame {
    fun fence(x: Float, width: Float, heightCm: Int, colorOffset: Int, knocked: Boolean = false): RodeoFenceUi {
        val top = heightCm / 10f
        return RodeoFenceUi(
            x = x,
            width = width,
            heightCm = heightCm,
            top = top,
            colorOffset = colorOffset,
            knocked = knocked,
            poleHeights = generateSequence(top) { it - 4f }.takeWhile { it >= 2f }.toList(),
        )
    }
    return SchneaggRodeoFrame(
        distance = 4_210f,
        fences = listOf(
            fence(x = 26f, width = 10f, heightCm = 110, colorOffset = 12),
            fence(x = 96f, width = 18f, heightCm = 130, colorOffset = 13),
            fence(x = 168f, width = 10f, heightCm = 90, colorOffset = 14),
        ),
        snails = listOf(
            RodeoSnailUi(x = 105f, height = 13f, facingLeft = true, tiltDeg = 2f),
            RodeoSnailUi(x = 140f, height = 0f, facingLeft = true, tiltDeg = -2f),
        ),
        pack = listOf(
            RodeoSnailUi(x = 4f, height = 1f, facingLeft = false, tiltDeg = 4f),
        ),
        horse = RodeoHorsePose(
            height = 14f,
            gaitPhase = 3.2f,
            airborne = true,
            riderLean = 1f,
            pitchDegrees = 0f,
            pivotX = 12f,
            pivotY = 12f,
            hindLegScale = 1f,
            frontLegFold = 0f,
            hatLift = 0f,
            glow = 0f,
        ),
        lasso = RodeoLassoUi(handX = 30f, handY = 32f, tipX = 50f, tipY = 24f, caught = null),
        markers = listOf(
            RodeoMarkerUi(x = 70f, username = "flo", score = 468L, isOwn = true, staggered = false),
            RodeoMarkerUi(x = 150f, username = "schneaggkönig", score = 476L, isOwn = false, staggered = true),
        ),
        snailsCaught = 7,
    )
}

@Composable
private fun SchneaggRodeoPreviewContent() {
    SchneaggchatTheme {
        val frame = previewFrame()
        SchneaggRodeoScreen(
            state = SchneaggRodeoState(
                isPlaying = true,
                score = 463,
                runTimeMillis = 47_000L,
                snailsCaught = 7,
                superJumpCharges = 1,
                nextToBeat = RodeoGhostUi(username = "flo", score = 468L, isOwn = true),
            ),
            frame = { frame },
            showStartOverlay = false,
            onAction = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 720)
@Composable
private fun SchneaggRodeoScreenPreview() {
    SchneaggRodeoPreviewContent()
}

/** Phone in landscape: lower, wider track at the bottom, HUD top right. */
@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun SchneaggRodeoScreenLandscapePreview() {
    SchneaggRodeoPreviewContent()
}
