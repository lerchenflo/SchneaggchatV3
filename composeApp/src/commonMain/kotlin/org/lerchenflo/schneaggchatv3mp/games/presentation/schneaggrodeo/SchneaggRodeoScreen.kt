package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.games.domain.GameId
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameHud
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameOverOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GamePauseOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.GameStartOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoTrack
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.ui.RodeoAnnouncementBanner
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.ui.RodeoControls
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.ui.RodeoSpeedometer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.ui.rodeoInput
import org.lerchenflo.schneaggchatv3mp.settings.data.AppVersion
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.BackButton
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_friend_joined
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_friend_joined_title
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_horseshoes
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_next_to_beat
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_snails
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_title

private val GAME_HEIGHT = 220.dp
private val MAX_CONTENT_WIDTH = 720.dp
private val COMPACT_LANDSCAPE_MAX_HEIGHT = 480.dp
/** Area under the track for the buttons; two stacked buttons fit, with room to tap around them. */
private val CONTROLS_HEIGHT = 150.dp
private val CONTROLS_HEIGHT_COMPACT = 120.dp

@Composable
fun SchneaggRodeoRoot(
    onBackClick: () -> Unit,
    viewModel: SchneaggRodeoViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Not delegated: the canvas reads it in the draw phase, so a new frame only redraws
    val frame = viewModel.frame.collectAsStateWithLifecycle()
    val people = viewModel.people.collectAsStateWithLifecycle()
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
        people = { people.value },
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
 * A cowboy on a horse jumps show-jumping fences while a pack of schneaggs chases him. The whole
 * play area is the jump button (see rodeoInput); the counters sit above the track, the buttons
 * below it (see RodeoControls). [frame] is read in the draw phase only.
 */
@Composable
fun SchneaggRodeoScreen(
    state: SchneaggRodeoState,
    frame: () -> SchneaggRodeoFrame,
    showStartOverlay: Boolean,
    onAction: (SchneaggRodeoAction) -> Unit,
    onBackClick: () -> Unit,
    showKeyHints: Boolean = false,
    people: () -> RodeoPeopleUi = { RodeoPeopleUi() },
) {
    val colors = MaterialTheme.colorScheme
    val currentOnAction by rememberUpdatedState(onAction)
    val steers by rememberUpdatedState(state.steers)
    val isStarted = state.isPlaying || state.isGameOver

    val focusRequester = remember { FocusRequester() }
    // The overlays' buttons take the keyboard focus; hand it back to the play area whenever riding
    // (re)starts so the keys keep working on desktop.
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
                    .rodeoInput(steers = { steers }, onAction = { currentOnAction(it) }),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .align(if (isCompactLandscape) Alignment.BottomCenter else Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RodeoCounters(state = state, isStarted = isStarted, isCompactLandscape = isCompactLandscape)

                    // Takes what is left next to the counters and the buttons, at most GAME_HEIGHT - a
                    // landscape phone gets a lower (and wider) track instead of cut-off buttons.
                    // Everything inside scales with the track height.
                    Box(
                        modifier = Modifier
                            .widthIn(max = MAX_CONTENT_WIDTH)
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = GAME_HEIGHT)
                    ) {
                        RodeoTrack(
                            frame = frame,
                            people = people,
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
                        ride = state.ride,
                        rocketReady = state.rocketReady,
                        carriageReady = state.carriageReady,
                        showKeyHints = showKeyHints,
                        enabled = state.isPlaying && !state.isPaused,
                        onAction = onAction,
                        modifier = Modifier
                            .widthIn(max = MAX_CONTENT_WIDTH)
                            .fillMaxWidth()
                            .height(if (isCompactLandscape) CONTROLS_HEIGHT_COMPACT else CONTROLS_HEIGHT)
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
                    // A lassoed friend joined: the pause tells whose highscore the run raises now
                    val friend = state.friendJoined
                    GamePauseOverlay(
                        onResume = { onAction(SchneaggRodeoAction.TogglePause) },
                        title = friend?.let { stringResource(Res.string.games_schneaggrodeo_friend_joined_title, it) },
                        message = friend?.let { stringResource(Res.string.games_schneaggrodeo_friend_joined, it) },
                    )
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

/** Caught snails and stored horseshoes, with the speedometer between them. */
@Composable
private fun RodeoCounters(state: SchneaggRodeoState, isStarted: Boolean, isCompactLandscape: Boolean) {
    val horseshoesText: @Composable () -> Unit = {
        Text(
            text = stringResource(Res.string.games_schneaggrodeo_horseshoes, state.luckyCharms),
            style = MaterialTheme.typography.labelLarge,
            color = if (state.luckyCharms > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(
        modifier = Modifier
            .widthIn(max = MAX_CONTENT_WIDTH)
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
