package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GamePauseOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.BackButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_catch_horse
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_crash_pilot
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_fence_height
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_horseshoes
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_lasso
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_lawn_tractor
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_next_to_beat
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_plane_controls
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_plane_controls_keys
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
/** How far a galloping hoof reaches forward and back from under its hip, in horse grid units. */
private const val GALLOP_REACH = 2.6f

/** Highscore marker posts; staggered ones are shorter so neighbouring labels don't stack up. */
private const val MARKER_POST_HEIGHT = 36f
private const val MARKER_POST_HEIGHT_STAGGERED = 29f

private const val SPEED_LINE_COUNT = 9

// Same rainbow palette as the TowerStack game (explicitly requested for the poles)
private val POLE_COLORS = listOf(
    Color(0xFFFF0000), // Red
    Color(0xFFFF7F00), // Orange
    Color(0xFF00FF00), // Green
    Color(0xFF0000FF)  // Blue
)

// Cowboy hat, explicitly requested as a fixed orange
private val HAT_COLOR = Color(0xFFFF8C00)

// Lawn tractor, explicitly requested as a fixed red (plus a darker shade of it for vents and trim)
private val TRACTOR_COLOR = Color(0xFFE53935)
private val TRACTOR_SHADE_COLOR = Color(0xFFB71C1C)

// Speedometer, explicitly requested in the same traffic sign look as the one on the Schneaggmap
private val SPEEDOMETER_FILL = Color.White
private val SPEEDOMETER_RING = Color.Red
private val SPEEDOMETER_TEXT = Color.Black

@Composable
fun SchneaggRodeoRoot(
    onBackClick: () -> Unit,
    viewModel: SchneaggRodeoViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Not delegated: the canvas reads it in the draw phase, so a new frame only redraws
    val frame = viewModel.frame.collectAsStateWithLifecycle()
    val restoreChecked by viewModel.restoreChecked.collectAsStateWithLifecycle()
    // Desktop has a keyboard: the buttons show their keys
    val appVersion = koinInject<AppVersion>()
    val showKeyHints = remember(appVersion) { appVersion.isDesktop() }

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
        showKeyHints = showKeyHints,
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
    showKeyHints: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val currentOnAction by rememberUpdatedState(onAction)
    val isFlying by rememberUpdatedState(state.isFlying)
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
                            // In the plane the lasso keys steer down, the jump keys up
                            isLassoKey && isFlying && event.type == KeyEventType.KeyDown -> {
                                currentOnAction(SchneaggRodeoAction.OnDivePressed); true
                            }
                            isLassoKey && isFlying && event.type == KeyEventType.KeyUp -> {
                                currentOnAction(SchneaggRodeoAction.OnDiveReleased); true
                            }
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
                    // In the plane the left half steers up and the right half down.
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { offset ->
                            val dive = isFlying && offset.x > size.width / 2f
                            currentOnAction(if (dive) SchneaggRodeoAction.OnDivePressed else SchneaggRodeoAction.OnJumpPressed)
                            tryAwaitRelease()
                            currentOnAction(if (dive) SchneaggRodeoAction.OnDiveReleased else SchneaggRodeoAction.OnJumpReleased)
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
                    val horseshoesText: @Composable () -> Unit = {
                        Text(
                            text = stringResource(Res.string.games_schneaggrodeo_horseshoes, state.luckyCharms),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.luckyCharms > 0) colors.secondary else colors.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.games_schneaggrodeo_snails, state.snailsCaught),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            // The HUD covers the top right corner in landscape: both counters go left
                            if (isCompactLandscape) {
                                Spacer(modifier = Modifier.size(12.dp))
                                horseshoesText()
                            }
                        }
                        if (isStarted) RodeoSpeedometer(speedKmh = state.speedKmh)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                            if (!isCompactLandscape) horseshoesText()
                        }
                    }

                    // Takes what is left next to the counter and the buttons, at most GAME_HEIGHT - a
                    // landscape phone gets a lower (and wider) track instead of cut-off buttons.
                    // Everything inside scales with the track height.
                    Box(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = GAME_HEIGHT)
                    ) {
                        RodeoTrack(
                            frame = frame,
                            onSizeChanged = { size ->
                                onAction(SchneaggRodeoAction.OnWorldSizeChanged(size.width, size.height))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        RodeoAnnouncementBanner(
                            announcement = state.announcement,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                        )
                    }

                    RodeoControls(
                        superJumpCharges = state.superJumpCharges,
                        isOnFoot = state.isOnFoot,
                        isFlying = state.isFlying,
                        showKeyHints = showKeyHints,
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
    isFlying: Boolean,
    showKeyHints: Boolean,
    enabled: Boolean,
    onAction: (SchneaggRodeoAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isFlying) {
        // Buttons would swallow the steering taps; the whole play area is the joystick now
        Text(
            text = stringResource(
                if (showKeyHints) Res.string.games_schneaggrodeo_plane_controls_keys else Res.string.games_schneaggrodeo_plane_controls
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 10.dp),
            textAlign = TextAlign.Center
        )
        return
    }
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
                    text = stringResource(Res.string.games_schneaggrodeo_super_jump, superJumpCharges) + keyHint("J", showKeyHints),
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
                Text(stringResource(Res.string.games_schneaggrodeo_catch_horse) + keyHint("L", showKeyHints))
            }
        } else {
            FilledTonalButton(
                onClick = { onAction(SchneaggRodeoAction.OnLassoClick) },
                enabled = enabled,
                // Keeps keyboard focus on the play area so space / L keep working after a click
                modifier = Modifier.focusProperties { canFocus = false }
            ) {
                Text(stringResource(Res.string.games_schneaggrodeo_lasso) + keyHint("L", showKeyHints))
            }
        }
    }
}

/** Pops up over the track when the cowboy boards the plane or the tractor. */
@Composable
private fun RodeoAnnouncementBanner(announcement: RodeoAnnouncement?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = announcement,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))) togetherWith
                    (fadeOut() + scaleOut(targetScale = 1.3f))
        },
        contentAlignment = Alignment.Center,
        label = "rodeoAnnouncement",
        modifier = modifier
    ) { shown ->
        if (shown == null) return@AnimatedContent
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Text(
                text = stringResource(
                    when (shown) {
                        RodeoAnnouncement.CRASH_PILOT -> Res.string.games_schneaggrodeo_crash_pilot
                        RodeoAnnouncement.LAWN_TRACTOR -> Res.string.games_schneaggrodeo_lawn_tractor
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

/** Round speed sign like the one on the Schneaggmap. */
@Composable
private fun RodeoSpeedometer(speedKmh: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(SPEEDOMETER_FILL, CircleShape)
            .border(width = 3.dp, color = SPEEDOMETER_RING, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$speedKmh",
            color = SPEEDOMETER_TEXT,
            fontWeight = FontWeight.Bold,
            // The tractor's five digits need a smaller font to fit the sign
            style = if (speedKmh >= 10_000) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
    }
}

/** Key name appended to a button label on desktop, e.g. "Lasso [L]". */
private fun keyHint(key: String, show: Boolean): String = if (show) " [$key]" else ""

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
    // Text is laid out once and reused, instead of being measured again for every frame
    val heightLabelLayouts = remember(textMeasurer, heightLabels, heightLabelStyle) {
        heightLabels.mapValues { (_, label) -> textMeasurer.measure(label, heightLabelStyle) }
    }
    val bodyLabelLayouts = remember(textMeasurer, bodyLabelStyle) { mutableMapOf<Int, TextLayoutResult>() }
    val markerLabelLayouts = remember(textMeasurer, markerLabelStyle, colors) {
        mutableMapOf<Triple<String, Long, Boolean>, TextLayoutResult>()
    }
    val snailOutline = remember(colors.surfaceContainer) { ColorFilter.tint(colors.surfaceContainer) }

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

            fun drawSnailAt(snail: RodeoSnailUi) = drawSnail(
                image = snailImage,
                centerX = snail.x * unit,
                footY = groundY - snail.height * unit,
                size = SNAIL_SIZE * unit,
                facingLeft = snail.facingLeft,
                tiltDeg = snail.tiltDeg,
                outline = snailOutline
            )

            // The whole picture rattles while the tractor races
            translate(world.shakeX * unit, world.shakeY * unit) {
                drawGround(groundY, unit, world.distance, colors.onSurfaceVariant, world.speedBlur)
                if (world.speedBlur) drawSpeedLines(groundY, unit, world.distance, colors.onSurfaceVariant)

                // Skyline under the plane, behind everything on the track
                world.buildings.forEach { building ->
                    drawBuilding(building, groundY, unit, colors.surfaceVariant, colors.onSurfaceVariant)
                    building.cloudBottom?.let { bottom ->
                        drawStormCloud(
                            left = (building.x - building.cloudOverhang) * unit,
                            right = (building.x + building.width + building.cloudOverhang) * unit,
                            bottomY = groundY - bottom * unit,
                            unit = unit,
                            seed = building.seed,
                            color = colors.onSurfaceVariant,
                            boltColor = colors.tertiary
                        )
                    }
                }

                // Highscore markers stand behind everything else on the track
                world.markers.forEach { marker ->
                    val color = if (marker.isOwn) colors.primary else colors.secondary
                    drawMarker(
                        marker = marker,
                        groundY = groundY,
                        unit = unit,
                        postHeight = if (marker.staggered) MARKER_POST_HEIGHT_STAGGERED else MARKER_POST_HEIGHT,
                        color = color,
                        label = markerLabelLayouts.getOrPut(Triple(marker.username, marker.score, marker.isOwn)) {
                            textMeasurer.measure(
                                text = "${marker.username}\n${marker.score}",
                                style = markerLabelStyle.copy(
                                    color = color,
                                    fontWeight = if (marker.isOwn) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 2,
                            )
                        }
                    )
                }

                world.fences.forEach { fence ->
                    drawFence(fence, groundY, unit, colors.onSurface, heightLabelLayouts.getValue(fence.heightCm))
                }

                world.snails.forEach { drawSnailAt(it) }
                world.pack.forEach { drawSnailAt(it) }

                world.horseshoes.forEach { shoe ->
                    drawHorseshoe(
                        center = Offset(shoe.x * unit, groundY - shoe.height * unit),
                        unit = unit,
                        tiltDeg = shoe.tiltDeg,
                        color = colors.secondary,
                        nailColor = colors.surfaceContainer
                    )
                }

                // Faint lucky aura around the horse, stronger with every stored charm
                if (world.luckyCharms > 0 && world.horse.hasRider) {
                    val auraCenter = Offset(
                        (HORSE_X + world.horse.offsetX + 15f) * unit,
                        groundY - (world.horse.height + 14f) * unit
                    )
                    drawCircle(
                        color = colors.secondary.copy(alpha = 0.1f + 0.08f * world.luckyCharms),
                        radius = 17f * unit,
                        center = auraCenter,
                        style = Stroke(width = (0.4f + 0.3f * world.luckyCharms) * unit)
                    )
                }

                // The horse stands on the tractor's deck, so the tractor goes first
                world.tractor?.let { tractor ->
                    drawTractor(
                        tractor = tractor,
                        groundY = groundY,
                        unit = unit,
                        lineColor = colors.onSurface,
                        deckColor = colors.onSurfaceVariant,
                        hubColor = colors.surfaceContainer,
                        lightColor = colors.surfaceBright,
                        smokeColor = colors.onSurfaceVariant,
                        clippingColor = colors.secondary
                    )
                }

                drawHorseAndRider(
                    left = (HORSE_X + world.horse.offsetX) * unit,
                    groundY = groundY - world.horse.height * unit,
                    unit = unit,
                    pose = world.horse,
                    glowColor = colors.primary,
                    color = colors.onSurface,
                    shirtColor = colors.primary,
                    bodyLabel = if (world.snailsCaught > 0) {
                        bodyLabelLayouts.getOrPut(world.snailsCaught) { textMeasurer.measure(world.snailsCaught.toString(), bodyLabelStyle) }
                    } else null
                )

                world.debris.forEach { piece ->
                    drawDebris(
                        piece = piece,
                        groundY = groundY,
                        unit = unit,
                        lineColor = colors.onSurface,
                        deckColor = colors.onSurfaceVariant,
                        hubColor = colors.surfaceContainer
                    )
                }

                world.plane?.let { plane ->
                    drawPlane(
                        plane = plane,
                        groundY = groundY,
                        unit = unit,
                        bodyColor = colors.tertiary,
                        wingColor = colors.tertiaryContainer,
                        lineColor = colors.onSurface,
                        pilotColor = colors.onSurface
                    )
                }

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

                world.sparkle?.let { sparkle ->
                    drawSparkle(
                        center = Offset(sparkle.x * unit, groundY - sparkle.y * unit),
                        unit = unit,
                        progress = sparkle.progress,
                        color = colors.secondary
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
}

private fun DrawScope.drawGround(groundY: Float, unit: Float, distance: Float, color: Color, speedBlur: Boolean) {
    drawLine(color, Offset(0f, groundY), Offset(size.width, groundY), strokeWidth = unit * 0.6f)

    // Small pebbles scrolling with the ground, so the speed is visible between fences. At tractor
    // speed they smear into long streaks.
    val spacing = 9f * unit
    val shift = (distance * unit) % spacing
    var x = -shift
    var index = (distance * unit / spacing).toInt()
    val pebbleColor = color.copy(alpha = if (speedBlur) 0.3f else 0.5f)
    while (x < size.width) {
        val depth = 1.5f + (index * 7 % 5) * 0.8f
        val length = if (speedBlur) (14f + index * 5 % 3 * 6f) * unit else (1f + index * 5 % 3) * unit
        drawLine(
            color = pebbleColor,
            start = Offset(x, groundY + depth * unit),
            end = Offset(x + length, groundY + depth * unit),
            strokeWidth = unit * 0.5f,
            cap = StrokeCap.Round
        )
        x += spacing
        index++
    }
}

/** Wind streaks racing through the sky while the tractor goes flat out. */
private fun DrawScope.drawSpeedLines(groundY: Float, unit: Float, distance: Float, color: Color) {
    val lineColor = color.copy(alpha = 0.35f)
    repeat(SPEED_LINE_COUNT) { index ->
        val height = 4f + (index * 37 % 45)
        val length = (18f + index * 11 % 20) * unit
        val period = size.width + length
        // Each line moves at its own pace, so they don't march along in lockstep
        val travelled = distance * unit * (0.6f + (index % 3) * 0.2f) + index * 97f * unit
        val x = size.width - travelled % period
        drawLine(
            color = lineColor,
            start = Offset(x, groundY - height * unit),
            end = Offset(x + length, groundY - height * unit),
            strokeWidth = 0.4f * unit,
            cap = StrokeCap.Round
        )
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
    label: TextLayoutResult,
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
    label: TextLayoutResult,
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
    outline: ColorFilter,
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
            colorFilter = outline,
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

/** A building of the skyline with a grid of windows, some of them lit. */
private fun DrawScope.drawBuilding(building: RodeoBuildingUi, groundY: Float, unit: Float, wallColor: Color, windowColor: Color) {
    val left = building.x * unit
    val top = groundY - building.height * unit
    drawRect(wallColor, Offset(left, top), Size(building.width * unit, building.height * unit))
    var row = 0
    var y = building.height - 3f
    while (y > 3f) {
        var column = 0
        var x = 2f
        while (x + 2f < building.width - 1f) {
            val lit = (building.seed + row * 7 + column * 13) % 3 != 0
            drawRect(
                color = windowColor.copy(alpha = if (lit) 0.45f else 0.15f),
                topLeft = Offset(left + x * unit, groundY - y * unit),
                size = Size(2f * unit, 2.5f * unit)
            )
            x += 4f
            column++
        }
        y -= 5f
        row++
    }
}

/**
 * A storm cloud hanging from the top of the canvas down to [bottomY]: a dark block with a puffy
 * underside and a small lightning bolt below some of them.
 */
private fun DrawScope.drawStormCloud(
    left: Float,
    right: Float,
    bottomY: Float,
    unit: Float,
    seed: Int,
    color: Color,
    boltColor: Color,
) {
    val cloudColor = color.copy(alpha = 0.55f)
    val puff = 2.2f * unit
    drawRect(cloudColor, Offset(left, 0f), Size(right - left, max(0f, bottomY - puff)))
    // Puffs along the underside; their bottoms line up with the collision edge
    var x = left + puff
    while (x <= right - puff + 0.1f) {
        drawCircle(cloudColor, radius = puff, center = Offset(x, bottomY - puff))
        x += puff * 1.4f
    }
    if (seed % 3 == 0) {
        val boltX = (left + right) / 2f
        drawPath(
            Path().apply {
                moveTo(boltX, bottomY - 0.5f * unit)
                lineTo(boltX - 1.2f * unit, bottomY + 2.5f * unit)
                lineTo(boltX + 0.3f * unit, bottomY + 2.5f * unit)
                lineTo(boltX - 0.8f * unit, bottomY + 5f * unit)
            },
            color = boltColor,
            style = Stroke(width = 0.5f * unit, cap = StrokeCap.Round)
        )
    }
}

/**
 * A small biplane facing right, drawn on a grid with y up from the fuselage underside and x from
 * its left edge. The rope ladder hangs from [PLANE_LADDER_X].
 */
private fun DrawScope.drawPlane(
    plane: RodeoPlaneUi,
    groundY: Float,
    unit: Float,
    bodyColor: Color,
    wingColor: Color,
    lineColor: Color,
    pilotColor: Color,
) {
    fun p(x: Float, y: Float) = Offset((plane.x + x) * unit, groundY - (plane.y + y) * unit)
    val center = p(PLANE_LENGTH / 2f, 3.5f)

    rotate(plane.rotation, pivot = center) {
        if (plane.ladderDown) {
            val bottom = -PLANE_LADDER_LENGTH
            drawLine(lineColor, p(PLANE_LADDER_X - 0.8f, 0f), p(PLANE_LADDER_X - 0.8f, bottom), 0.3f * unit)
            drawLine(lineColor, p(PLANE_LADDER_X + 0.8f, 0f), p(PLANE_LADDER_X + 0.8f, bottom), 0.3f * unit)
            var rung = -1.5f
            while (rung >= bottom) {
                drawLine(lineColor, p(PLANE_LADDER_X - 0.8f, rung), p(PLANE_LADDER_X + 0.8f, rung), 0.3f * unit)
                rung -= 2f
            }
        }

        // Lower wing and struts behind the fuselage
        drawRoundRect(wingColor, p(6f, 1f), Size(10f * unit, 1.2f * unit), CornerRadius(0.6f * unit))
        listOf(8f, 14f).forEach { x -> drawLine(lineColor, p(x, 1f), p(x, 8.5f), 0.35f * unit) }

        // Tail fin and fuselage
        drawPath(
            Path().apply {
                val a = p(0f, 5f); val b = p(0.5f, 10f); val c = p(3f, 10f); val d = p(5f, 5f)
                moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
            },
            bodyColor
        )
        drawRoundRect(bodyColor, p(0f, 6f), Size(20f * unit, 5f * unit), CornerRadius(2.5f * unit))

        // Pilot: head and hat sticking out of the cockpit
        if (plane.hasPilot) {
            drawCircle(pilotColor, radius = 1.6f * unit, center = p(PLANE_PILOT_X, 7.6f))
            drawRoundRect(HAT_COLOR, p(PLANE_PILOT_X - 2.8f, 9.6f), Size(5.6f * unit, 0.6f * unit), CornerRadius(0.3f * unit))
            drawRoundRect(HAT_COLOR, p(PLANE_PILOT_X - 1.5f, 11.6f), Size(3f * unit, 2.2f * unit), CornerRadius(0.7f * unit))
        }

        // Upper wing on top
        drawRoundRect(wingColor, p(5f, 9.5f), Size(12f * unit, 1.2f * unit), CornerRadius(0.6f * unit))

        // Spinning propeller: a blade whose visible length pulses
        val blade = 3.2f * abs(sin(plane.propellerPhase))
        drawLine(lineColor, p(20.5f, 3.5f - blade), p(20.5f, 3.5f + blade), 0.6f * unit, StrokeCap.Round)
        drawCircle(lineColor, radius = 0.6f * unit, center = p(20.5f, 3.5f))
    }
}

/**
 * The lawn tractor, facing right, on a grid with y up from the ground and x from its rear end: a
 * flat deck for the horse over a big fendered rear wheel, a sloped hood with a headlight in front,
 * and the mower deck low between the wheels. Parts come off in the order exhaust, steering wheel,
 * hood, mower deck, front wheel.
 */
private fun DrawScope.drawTractor(
    tractor: RodeoTractorUi,
    groundY: Float,
    unit: Float,
    lineColor: Color,
    deckColor: Color,
    hubColor: Color,
    lightColor: Color,
    smokeColor: Color,
    clippingColor: Color,
) {
    fun p(x: Float, y: Float) = Offset((tractor.x + x) * unit, groundY - y * unit)
    fun polygon(vararg points: Pair<Float, Float>) = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(p(x, y).x, p(x, y).y) else lineTo(p(x, y).x, p(x, y).y) }
        close()
    }
    val lost = tractor.partsLost

    // Exhaust puffs trailing back in the wind
    if (tractor.exhaust && lost == 0) {
        repeat(3) { index ->
            val drift = (tractor.wheelPhase * 0.3f + index / 3f) % 1f
            drawCircle(
                color = smokeColor.copy(alpha = 0.35f * (1f - drift)),
                radius = (0.8f + 1.4f * drift) * unit,
                center = p(35.5f - 12f * drift, 16f + 2f * drift)
            )
        }
    }
    // Grass clippings spraying out behind the mower deck while it races
    if (tractor.exhaust && lost <= 3) {
        repeat(6) { index ->
            val t = (tractor.wheelPhase * 0.17f + index / 6f) % 1f
            val x = 10f - 9f * t
            val y = 1.5f + 6f * t * (1.2f - t)
            val flip = if (index % 2 == 0) 0.5f else -0.5f
            drawLine(clippingColor.copy(alpha = 1f - t), p(x, y), p(x - 0.9f, y + flip), 0.35f * unit, StrokeCap.Round)
        }
    }

    rotate(-tractor.rotation, pivot = p(7f, 0f)) {
        // Mower deck housing, low between the wheels
        if (lost <= 3) {
            drawPath(polygon(11f to 3.8f, 29f to 3.8f, 30.5f to 1.2f, 10f to 1.2f), deckColor)
            drawLine(hubColor.copy(alpha = 0.5f), p(12f, 2.5f), p(28.5f, 2.5f), 0.25f * unit)
        }
        // Frame rail between the axles
        drawRect(lineColor, p(6f, 5.4f), Size(28f * unit, 1f * unit))

        // Deck the horse stands on, with a darker skirt
        drawRoundRect(TRACTOR_COLOR, p(0f, TRACTOR_DECK_HEIGHT), Size(30f * unit, 2.4f * unit), CornerRadius(0.6f * unit))
        drawRect(TRACTOR_SHADE_COLOR, p(0.6f, 5.3f), Size(28.8f * unit, 0.7f * unit))

        // Hood with vents, grille and headlight, or the bare engine once it flew off
        if (lost <= 2) {
            drawPath(polygon(27.5f to 4.8f, 27.5f to 11.5f, 37f to 11.5f, 40.5f to 9f, 40.5f to 4.8f), TRACTOR_COLOR)
            drawLine(hubColor.copy(alpha = 0.35f), p(28.2f, 11f), p(36.6f, 11f), 0.35f * unit, StrokeCap.Round)
            listOf(30f, 31.4f, 32.8f).forEach { x ->
                drawLine(TRACTOR_SHADE_COLOR, p(x, 9.8f), p(x, 7.4f), 0.4f * unit, StrokeCap.Round)
            }
            drawLine(lineColor.copy(alpha = 0.6f), p(40f, 8.4f), p(40f, 5.4f), 0.35f * unit)
            drawCircle(lightColor, radius = 0.75f * unit, center = p(38.8f, 9.3f))
        } else {
            drawRoundRect(deckColor, p(28.5f, 10f), Size(7f * unit, 5f * unit), CornerRadius(0.5f * unit))
            listOf(30f, 32f, 34f).forEach { x ->
                drawLine(lineColor, p(x, 10f), p(x, 6f), 0.3f * unit)
            }
        }
        if (lost <= 1) {
            drawLine(lineColor, p(28.5f, 11.5f), p(26.5f, 14.5f), 0.5f * unit, StrokeCap.Round)
            drawLine(lineColor, p(25f, 15.2f), p(28f, 14f), 0.7f * unit, StrokeCap.Round)
        }
        if (lost <= 0) {
            drawLine(lineColor, p(35.5f, 11.5f), p(35.5f, 15.5f), 0.7f * unit, StrokeCap.Round)
            drawLine(lineColor, p(35f, 15.6f), p(36.4f, 16f), 0.6f * unit, StrokeCap.Round)
        }

        drawWheel(p(7f, 4.5f), 4.5f * unit, tractor.wheelPhase, lineColor, hubColor)
        // Fender over the rear wheel
        drawArc(
            color = TRACTOR_COLOR,
            startAngle = 180f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = p(7f - 5.6f, 4.5f + 5.6f),
            size = Size(11.2f * unit, 11.2f * unit),
            style = Stroke(width = 1.2f * unit, cap = StrokeCap.Round)
        )
        if (lost <= 4) drawWheel(p(33f, 2.8f), 2.8f * unit, tractor.wheelPhase * 1.6f, lineColor, hubColor)
    }
}

/** A treaded tyre with a rim, red hub and three spokes turned by [phase] (radians). */
private fun DrawScope.drawWheel(center: Offset, radius: Float, phase: Float, tyreColor: Color, hubColor: Color) {
    drawCircle(tyreColor, radius = radius, center = center)
    // Tread blocks around the tyre, turning with it
    repeat(10) { index ->
        val angle = phase + index * 2f * PI.toFloat() / 10f
        val direction = Offset(cos(angle), sin(angle))
        drawLine(hubColor.copy(alpha = 0.3f), center + direction * (radius * 0.8f), center + direction * radius, radius * 0.12f)
    }
    drawCircle(hubColor, radius = radius * 0.55f, center = center)
    repeat(3) { index ->
        val angle = phase + index * 2f * PI.toFloat() / 3f
        drawLine(tyreColor, center, center + Offset(cos(angle), sin(angle)) * (radius * 0.5f), radius * 0.1f)
    }
    drawCircle(TRACTOR_COLOR, radius = radius * 0.22f, center = center)
}

/** A part torn off the tractor, tumbling through the air. */
private fun DrawScope.drawDebris(
    piece: RodeoDebrisUi,
    groundY: Float,
    unit: Float,
    lineColor: Color,
    deckColor: Color,
    hubColor: Color,
) {
    val center = Offset(piece.x * unit, groundY - piece.y * unit)
    rotate(piece.rotation, pivot = center) {
        when (piece.part) {
            0 -> drawLine(lineColor, center - Offset(0f, 1.5f * unit), center + Offset(0f, 1.5f * unit), 0.8f * unit, StrokeCap.Round)
            1 -> {
                drawLine(lineColor, center, center + Offset(0f, 3f * unit), 0.5f * unit, StrokeCap.Round)
                drawLine(lineColor, center - Offset(1.5f * unit, 0f), center + Offset(1.5f * unit, 0f), 0.6f * unit, StrokeCap.Round)
            }
            2 -> {
                drawRoundRect(TRACTOR_COLOR, center - Offset(4f * unit, 2f * unit), Size(8f * unit, 4f * unit), CornerRadius(1f * unit))
                listOf(-1f, 0.5f).forEach { x ->
                    drawLine(TRACTOR_SHADE_COLOR, center + Offset(x * unit, -1f * unit), center + Offset(x * unit, 1f * unit), 0.4f * unit)
                }
            }
            3 -> drawRoundRect(deckColor, center - Offset(4f * unit, 0.75f * unit), Size(8f * unit, 1.5f * unit), CornerRadius(0.5f * unit))
            4 -> drawWheel(center, 2.8f * unit, 0f, lineColor, hubColor)
            else -> drawRect(TRACTOR_COLOR, center - Offset(0.75f * unit, 0.75f * unit), Size(1.5f * unit, 1.5f * unit))
        }
    }
}

/** A lucky horseshoe, opening up, with a few nail holes. */
private fun DrawScope.drawHorseshoe(center: Offset, unit: Float, tiltDeg: Float, color: Color, nailColor: Color) {
    val radius = 2.4f * unit
    val stroke = 1.1f * unit
    rotate(tiltDeg, pivot = center) {
        // Open side up: the arc runs from the left prong over the bottom to the right prong
        drawArc(
            color = color,
            startAngle = -20f,
            sweepAngle = 220f,
            useCenter = false,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        listOf(20f, 70f, 110f, 160f).forEach { angle ->
            val radians = angle * PI.toFloat() / 180f
            drawCircle(
                color = nailColor,
                radius = 0.22f * unit,
                center = center + Offset(cos(radians) * radius, sin(radians) * radius)
            )
        }
    }
}

/** Eight short sparks flying outwards and fading. */
private fun DrawScope.drawSparkle(center: Offset, unit: Float, progress: Float, color: Color) {
    val alpha = 1f - progress
    repeat(8) { index ->
        val radians = index * PI.toFloat() / 4f
        val direction = Offset(cos(radians), sin(radians))
        val inner = (1f + 5f * progress) * unit
        val outer = inner + 2f * (1f - progress) * unit + 0.5f * unit
        drawLine(
            color = color.copy(alpha = alpha),
            start = center + direction * inner,
            end = center + direction * outer,
            strokeWidth = 0.5f * unit,
            cap = StrokeCap.Round
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
 * lights them up, frontLegRaise lifts the front legs while the horse rears up for it. frontLegFold buckles the front legs at the knee and hatLift pops the hat off the
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
        // The body rises in the moment of suspension after the front legs pushed off
        val bounce = if (airborne) 0f else cos(gaitPhase - 3.4f) * 0.6f

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
            // Four-beat gallop: hind left, hind right, front left, front right, then a short
            // moment with the legs gathered. Each hoof reaches forward lifted and pushes back on
            // the ground; knees fold forward on the front legs, hocks backwards on the hind legs.
            val raise = pose.frontLegRaise
            listOf(17f to 2.0f, 19f to 2.7f).forEachIndexed { index, (x, offset) ->
                val phase = gaitPhase + offset
                val lift = max(0f, cos(phase))
                val reach = sin(phase) * GALLOP_REACH
                val gallopHoof = p(x + reach - 1.2f * lift, 2.2f * lift)
                val gallopKnee = p(x + reach / 2f + 1.8f * lift, 4.5f + 1.2f * lift)
                // Stumbling: the knee buckles forward and the hoof folds under
                val knee = lerp(gallopKnee, p(x + 2.5f, 5f), frontLegFold)
                val hoof = lerp(gallopHoof, p(x - 1f, 3.5f), frontLegFold)
                // Rearing up for the super jump: lifted and pawing the air
                val paw = sin(gaitPhase * 6f + index * PI.toFloat()) * 1.2f
                val raisedKnee = p(x + 3f, 7f + paw)
                val raisedHoof = p(x + 1f, 4.5f + paw * 1.5f)
                drawLine(color, p(x, 9f), lerp(knee, raisedKnee, raise), legStroke, StrokeCap.Round)
                drawLine(color, lerp(knee, raisedKnee, raise), lerp(hoof, raisedHoof, raise), legStroke, StrokeCap.Round)
            }
            listOf(6f to 0f, 8f to 0.7f).forEach { (x, offset) ->
                val phase = gaitPhase + offset
                val lift = max(0f, cos(phase))
                val reach = sin(phase) * GALLOP_REACH * hindLegScale
                val hoofY = 9f - 9f * hindLegScale + 2.2f * lift
                val hoof = p(x + reach + 0.6f * lift, hoofY)
                val hock = p(x + reach / 2f - 1.6f - 0.8f * lift, (9f + hoofY) / 2f + 0.8f * lift)
                drawLine(color, p(x, 9f), hock, hindStroke, StrokeCap.Round)
                drawLine(color, hock, hoof, hindStroke, StrokeCap.Round)
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
