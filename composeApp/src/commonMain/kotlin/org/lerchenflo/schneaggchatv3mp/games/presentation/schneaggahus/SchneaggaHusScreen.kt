package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggahus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
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
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_instructions
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_lives_remaining
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_next_up
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_title
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_wave
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggahus_wave_progress
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative
import schneaggchatv3mp.composeapp.generated.resources.icon_schneaggahus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DEG_TO_RAD = (PI / 180.0).toFloat()

@Composable
fun SchneaggaHusScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewmodel = koinViewModel<SchneaggaHusViewmodel>()
    val state by viewmodel.state.collectAsStateWithLifecycle()
    val restoreChecked by viewmodel.restoreChecked.collectAsStateWithLifecycle()

    var explanationDismissed by rememberSaveable { mutableStateOf(false) }
    val isStarted = state.isPlaying || state.isGameOver

    // After process death the dismissed flag is restored but the ViewModel is new. Wait for the
    // saved-run check first, otherwise this would start a fresh run over the restored one.
    LaunchedEffect(restoreChecked) {
        if (restoreChecked && explanationDismissed && !isStarted) viewmodel.onAction(SchneaggaHusAction.StartGame)
    }

    // Leaving the screen pauses and persists the run so it can be picked up again later
    DisposableEffect(Unit) {
        onDispose { viewmodel.onAction(SchneaggaHusAction.LeaveGame) }
    }

    SchneaggaHusScreen(
        state = state,
        showStartOverlay = restoreChecked && !explanationDismissed && !isStarted,
        onAction = viewmodel::onAction,
        onStart = {
            explanationDismissed = true
            viewmodel.onAction(SchneaggaHusAction.StartGame)
        },
        onStop = {
            explanationDismissed = false
            viewmodel.onAction(SchneaggaHusAction.StopGame)
        },
        onBackClick = onBackClick,
    )
}

@Composable
internal fun SchneaggaHusScreen(
    state: SchneaggaHusState,
    showStartOverlay: Boolean,
    onAction: (SchneaggaHusAction) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBackClick: () -> Unit,
) {
    val isStarted = state.isPlaying || state.isGameOver

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityTitle(
            title = stringResource(Res.string.games_schneaggahus_title),
            onBackClick = onBackClick,
            // The back button looks up the platform via Koin, which previews do not have
            showBackButton = !LocalInspectionMode.current,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            SchneaggaHusBoard(
                state = state,
                onSwitchClick = { onAction(SchneaggaHusAction.OnSwitchClick(it)) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp)
                    // Keep the board clear of the HUD row and the wave progress row below it
                    .padding(top = 96.dp, bottom = 8.dp)
            )

            if (isStarted) {
                LivesRow(
                    lives = state.lives,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 12.dp)
                )

                GameHud(
                    score = state.score.toLong(),
                    timeMillis = state.elapsedMillis,
                    onStop = onStop,
                    isPaused = state.isPaused,
                    onTogglePause = { onAction(SchneaggaHusAction.TogglePause) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )

                WaveProgressRow(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp)
                )
            }

            WaveBanner(
                state = state,
                modifier = Modifier.align(Alignment.Center)
            )

            if (showStartOverlay) {
                GameStartOverlay(
                    title = stringResource(Res.string.games_schneaggahus_title),
                    explanation = stringResource(Res.string.games_schneaggahus_instructions),
                    onStart = onStart
                )
            }

            if (state.isGameOver) {
                GameOverOverlay(
                    game = GameId.SCHNEAGGAHUS,
                    finalScore = state.score.toLong(),
                    finalTimeMillis = state.elapsedMillis,
                    onRestart = { onAction(SchneaggaHusAction.RestartGame) },
                    onExit = {
                        onAction(SchneaggaHusAction.StopGame)
                        onBackClick()
                    }
                )
            } else if (state.isPaused) {
                GamePauseOverlay(onResume = { onAction(SchneaggaHusAction.TogglePause) })
            }
        }
    }
}

@Composable
private fun LivesRow(
    lives: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.games_schneaggahus_lives_remaining, lives)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.semantics { contentDescription = description }
    ) {
        // Spent lives stay visible as outlines so the total is always readable
        repeat(SCHNEAGGHUS_MAX_LIVES) { index ->
            val isRemaining = index < lives
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
}

/** "Wave N" plus one dot per schneagg of the wave: delivered, on the track, still to come. */
@Composable
private fun WaveProgressRow(
    state: SchneaggaHusState,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.games_schneaggahus_wave_progress, state.waveDelivered, state.waveSnailTotal)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.semantics { contentDescription = description }
    ) {
        Text(
            text = stringResource(Res.string.games_schneaggahus_wave, state.wave),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        WaveProgressDots(
            delivered = state.waveDelivered,
            inFlight = state.schneaggList.size,
            total = state.waveSnailTotal,
        )
    }
}

@Composable
private fun WaveProgressDots(
    delivered: Int,
    inFlight: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val done = MaterialTheme.colorScheme.primary
    val onTrack = done.copy(alpha = 0.35f)
    val remaining = MaterialTheme.colorScheme.outlineVariant
    val dot = 5.dp
    val gap = 2.dp
    Canvas(
        modifier = modifier
            .width(dot * total + gap * (total - 1).coerceAtLeast(0))
            .height(dot)
    ) {
        val d = dot.toPx()
        val g = gap.toPx()
        repeat(total) { index ->
            val color = when {
                index < delivered -> done
                index < delivered + inFlight -> onTrack
                else -> remaining
            }
            drawCircle(color, radius = d / 2f, center = Offset(index * (d + g) + d / 2f, d / 2f))
        }
    }
}

/**
 * "Wave N" pill: solid during the break between waves (announcing the next one),
 * then fading out over the first moment of the wave it announced.
 */
@Composable
private fun WaveBanner(
    state: SchneaggaHusState,
    modifier: Modifier = Modifier,
) {
    val (wave, alpha) = when {
        state.isIntermission -> state.wave + 1 to 1f
        state.isPlaying && !state.isGameOver -> {
            val age = state.elapsedMillis - state.waveStartedAtElapsed
            if (age in 0 until WAVE_BANNER_MS) {
                val t = age.toFloat() / WAVE_BANNER_MS
                state.wave to (if (t > 0.6f) (1f - t) / 0.4f else 1f)
            } else {
                return
            }
        }
        else -> return
    }

    Surface(
        modifier = modifier.alpha(alpha),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
    ) {
        Text(
            text = stringResource(Res.string.games_schneaggahus_wave, wave),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
        )
    }
}

/**
 * Board palette derived from the theme's greens: a deep green field with darker
 * scenery and rails, bright switch discs, and theme surface for the sticker outlines.
 */
private class BoardStyle(
    val field: Color,
    val scenery: Color,
    val rail: Color,
    val railDim: Color,
    val switchDisc: Color,
    val switchLit: Color,
    val outline: Color,
    val mouth: Color,
    val label: Color,
    val correct: Color,
    val wrong: Color,
)

@Composable
private fun rememberBoardStyle(): BoardStyle {
    val colors = MaterialTheme.colorScheme
    return remember(colors) {
        val field = lerp(colors.onPrimaryContainer, colors.primaryContainer, 0.15f)
        val rail = lerp(field, colors.scrim, 0.5f)
        // The field is the same deep green in every theme, so outlines and labels take
        // whichever of surface / onSurface is the light one to stay readable in dark mode too
        val bright = if (colors.surface.luminance() >= colors.onSurface.luminance()) colors.surface else colors.onSurface
        BoardStyle(
            field = field,
            scenery = lerp(field, colors.scrim, 0.22f),
            rail = rail,
            railDim = rail.copy(alpha = 0.35f),
            switchDisc = colors.primaryContainer,
            switchLit = lerp(colors.primaryContainer, bright, 0.45f),
            outline = bright,
            mouth = colors.scrim.copy(alpha = 0.75f),
            label = lerp(colors.primaryContainer, bright, 0.5f),
            correct = colors.primaryContainer,
            wrong = colors.error,
        )
    }
}

private enum class RailKind { PLAIN, ACTIVE, INACTIVE }

/** Part of the path through one tile, from [from] to [to] along its progress axis. */
private class RailSegment(
    val tile: Position,
    val entry: DIRECTION,
    val exit: DIRECTION,
    val from: Float,
    val to: Float,
    val kind: RailKind,
)

/**
 * The whole board in one Canvas, styled after the classic train-routing look:
 * a green field with pine silhouettes, a mountain tunnel the schneaggs come out
 * of, thin rails with sleepers, round switch discs showing the lit active path,
 * sticker-outlined houses and schneaggs, and the delivery pulses.
 * Taps on (or right next to) a switch call [onSwitchClick].
 */
@Composable
internal fun SchneaggaHusBoard(
    state: SchneaggaHusState,
    onSwitchClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = rememberBoardStyle()
    val snailImage = imageResource(Res.drawable.icon_schneagg_alternative)
    val housePainter = painterResource(Res.drawable.icon_schneaggahus)
    val textMeasurer = rememberTextMeasurer()
    val nextUpLabel = stringResource(Res.string.games_schneaggahus_next_up)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
    val pointsStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val nextUpLayout = remember(nextUpLabel, labelStyle) { textMeasurer.measure(nextUpLabel, labelStyle) }
    val switches by rememberUpdatedState(state.trackList.filter { it.isSwitch })
    val gridWidth = state.gridWidth
    val gridHeight = state.gridHeight

    Box(
        // Largest board that fits both the available width and height
        modifier = modifier
            .aspectRatio(gridWidth.toFloat() / gridHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(style.field)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gridWidth) {
                    detectTapGestures { tap ->
                        val tilePx = size.width / gridWidth.toFloat()
                        hitSwitch(switches, tap, tilePx)?.let { onSwitchClick(it.position) }
                    }
                }
        ) {
            drawSchneaggaHus(
                state = state,
                style = style,
                snailImage = snailImage,
                housePainter = housePainter,
                textMeasurer = textMeasurer,
                nextUpLayout = nextUpLayout,
                pointsStyle = pointsStyle,
            )
        }
    }
}

/** The switch on the tapped tile, or the nearest one within three quarters of a tile so small tiles stay tappable. */
private fun hitSwitch(switches: List<TrackTile>, tap: Offset, tilePx: Float): TrackTile? {
    val tileX = (tap.x / tilePx).toInt()
    val tileY = (tap.y / tilePx).toInt()
    switches.firstOrNull { it.position.x == tileX && it.position.y == tileY }?.let { return it }

    fun distance(tile: TrackTile) = hypot(
        (tile.position.x + 0.5f) * tilePx - tap.x,
        (tile.position.y + 0.5f) * tilePx - tap.y,
    )
    return switches.minByOrNull { distance(it) }?.takeIf { distance(it) <= 0.75f * tilePx }
}

private fun DrawScope.drawSchneaggaHus(
    state: SchneaggaHusState,
    style: BoardStyle,
    snailImage: ImageBitmap,
    housePainter: Painter,
    textMeasurer: TextMeasurer,
    nextUpLayout: TextLayoutResult,
    pointsStyle: TextStyle,
) {
    val tile = size.width / state.gridWidth
    val railGap = tile * 0.12f
    val railWidth = tile * 0.045f
    val sleeperHalf = tile * 0.09f
    val sleeperWidth = tile * 0.035f
    val sleeperSpacing = tile * 0.2f
    val thinStroke = 1.5.dp.toPx()
    val pulseStroke = 3.dp.toPx()

    fun center(position: Position) = Offset((position.x + 0.5f) * tile, (position.y + 0.5f) * tile)

    val housePositions = state.schneagghusList.mapTo(HashSet()) { it.position }
    val trackPositions = state.trackList.mapTo(HashSet()) { it.position }

    // 1. Scenery: pines on free cells (same spots every frame for a given map) and the tunnel mountain
    for (y in 1 until state.gridHeight) {
        for (x in 0 until state.gridWidth) {
            val cell = Position(x, y)
            if (cell in trackPositions || cell in housePositions) continue
            val hash = sceneryHash(x, y, state.spawn.x, state.gridHeight)
            if (hash % 5 >= 2) continue
            val pineSize = tile * (0.26f + (hash % 7) / 7f * 0.2f)
            val pineX = (x + 0.3f + (hash % 11) / 11f * 0.4f) * tile
            val pineY = (y + 0.3f + (hash % 13) / 13f * 0.4f) * tile
            drawPine(pineX, pineY, pineSize, style.scenery)
        }
    }

    val spawnLeft = state.spawn.x * tile
    val spawnTop = state.spawn.y * tile
    val mountain = Path().apply {
        moveTo(spawnLeft - tile * 0.4f, spawnTop + tile * 0.98f)
        lineTo(spawnLeft + tile * 0.22f, spawnTop + tile * 0.14f)
        cubicTo(
            spawnLeft + tile * 0.36f, spawnTop - tile * 0.04f,
            spawnLeft + tile * 0.64f, spawnTop - tile * 0.04f,
            spawnLeft + tile * 0.78f, spawnTop + tile * 0.14f,
        )
        lineTo(spawnLeft + tile * 1.4f, spawnTop + tile * 0.98f)
        close()
    }
    drawPath(mountain, style.scenery)
    val mouth = RoundRect(
        rect = Rect(spawnLeft + tile * 0.32f, spawnTop + tile * 0.5f, spawnLeft + tile * 0.68f, spawnTop + tile * 1.02f),
        topLeft = CornerRadius(tile * 0.18f),
        topRight = CornerRadius(tile * 0.18f),
        bottomLeft = CornerRadius.Zero,
        bottomRight = CornerRadius.Zero,
    )
    drawPath(Path().apply { addRoundRect(mouth) }, style.mouth)

    // 2. Every piece of rail: full tiles (both branches of a switch), the lower
    //    half of the tunnel tile and a stub leading into each house.
    val segments = ArrayList<RailSegment>()
    state.trackList.forEach { track ->
        val from = if (track.position == state.spawn) 0.5f else 0f
        track.exits.forEachIndexed { index, exit ->
            val kind = when {
                !track.isSwitch -> RailKind.PLAIN
                index == track.activeExit -> RailKind.ACTIVE
                else -> RailKind.INACTIVE
            }
            segments += RailSegment(track.position, track.entry, exit, from, 1f, kind)
            val target = track.position.step(exit)
            if (target in housePositions) {
                segments += RailSegment(target, exit.opposite(), exit, 0f, 0.5f, RailKind.PLAIN)
            }
        }
    }

    fun drawRail(segment: RailSegment, color: Color, width: Float, offset: Float) {
        if (isTurn(segment.entry, segment.exit)) {
            val arc = arcSpec(segment.tile, segment.entry, segment.exit)
            val radius = tile / 2f + offset
            drawArc(
                color = color,
                startAngle = arc.startAngleDeg + arc.sweepAngleDeg * segment.from,
                sweepAngle = arc.sweepAngleDeg * (segment.to - segment.from),
                useCenter = false,
                topLeft = Offset(arc.cornerX * tile - radius, arc.cornerY * tile - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = width, cap = StrokeCap.Butt),
            )
        } else {
            val start = trackPoint(segment.tile, segment.entry, segment.exit, segment.from)
            val end = trackPoint(segment.tile, segment.entry, segment.exit, segment.to)
            val shift = Offset(-segment.exit.dy * offset, segment.exit.dx * offset)
            drawLine(
                color = color,
                start = Offset(start.x * tile, start.y * tile) + shift,
                end = Offset(end.x * tile, end.y * tile) + shift,
                strokeWidth = width,
                cap = StrokeCap.Butt,
            )
        }
    }

    fun drawSleepers(segment: RailSegment, color: Color) {
        val length = pathLength(segment.entry, segment.exit) * (segment.to - segment.from) * tile
        val count = (length / sleeperSpacing).toInt()
        for (index in 0 until count) {
            val progress = segment.from + (segment.to - segment.from) * (index + 0.5f) / count
            val point = trackPoint(segment.tile, segment.entry, segment.exit, progress)
            val heading = point.headingDeg * DEG_TO_RAD
            val across = Offset(-sin(heading) * sleeperHalf, cos(heading) * sleeperHalf)
            val at = Offset(point.x * tile, point.y * tile)
            drawLine(color, at - across, at + across, sleeperWidth, StrokeCap.Butt)
        }
    }

    /** Two rails with sleepers between them. */
    fun drawTrack(segment: RailSegment, color: Color) {
        drawSleepers(segment, color)
        drawRail(segment, color, railWidth, railGap)
        drawRail(segment, color, railWidth, -railGap)
    }

    segments.forEach { if (it.kind == RailKind.PLAIN) drawTrack(it, style.rail) }

    // 3. Switches: a disc with the inactive branch faint and the active one lit.
    state.trackList.forEach { track ->
        if (!track.isSwitch) return@forEach
        val at = center(track.position)
        val discRadius = tile * 0.4f
        drawCircle(style.switchDisc, radius = discRadius, center = at)
        val discClip = Path().apply { addOval(Rect(at, discRadius)) }
        segments.forEach { segment ->
            if (segment.tile != track.position) return@forEach
            when (segment.kind) {
                RailKind.INACTIVE -> drawTrack(segment, style.railDim)
                RailKind.ACTIVE -> {
                    clipPath(discClip) { drawRail(segment, style.switchLit, tile * 0.3f, 0f) }
                    drawTrack(segment, style.rail)
                }
                RailKind.PLAIN -> Unit
            }
        }
    }

    // 4. Schneaggs, facing the way they crawl, with a sticker outline. The icon
    //    faces east; heading west it is mirrored instead of turned upside down.
    val snailSize = tile * 0.8f
    val outlineScale = 1.18f
    val snailPivot = Offset(snailSize / 2f, snailSize / 2f)
    val snailDstSize = IntSize(snailSize.roundToInt(), snailSize.roundToInt())
    val snailOutlineSize = IntSize((snailSize * outlineScale).roundToInt(), (snailSize * outlineScale).roundToInt())
    val snailOutlineOffset = IntOffset(
        ((snailSize - snailSize * outlineScale) / 2f).roundToInt(),
        ((snailSize - snailSize * outlineScale) / 2f).roundToInt(),
    )
    // While still in the tunnel a schneagg only shows through the mouth or once it is out below
    val tunnelClip = Path().apply {
        addRoundRect(mouth)
        addRect(
            Rect(
                left = (state.spawn.x - 1) * tile,
                top = spawnTop + tile * 0.98f,
                right = (state.spawn.x + 2) * tile,
                bottom = (state.spawn.y + 2) * tile,
            )
        )
    }

    fun drawSchneagg(point: TrackPoint, color: Color) {
        withTransform({
            translate(point.x * tile - snailSize / 2f, point.y * tile - snailSize / 2f)
            rotate(point.headingDeg, pivot = snailPivot)
            if (cos(point.headingDeg * DEG_TO_RAD) < 0f) scale(1f, -1f, pivot = snailPivot)
        }) {
            drawImage(
                image = snailImage,
                dstOffset = snailOutlineOffset,
                dstSize = snailOutlineSize,
                colorFilter = ColorFilter.tint(style.outline),
                filterQuality = FilterQuality.Medium,
            )
            drawImage(
                image = snailImage,
                dstOffset = IntOffset.Zero,
                dstSize = snailDstSize,
                colorFilter = ColorFilter.tint(color),
                filterQuality = FilterQuality.Medium,
            )
        }
    }

    state.schneaggList.forEach { schneagg ->
        val point = trackPoint(schneagg.tile, schneagg.entry, schneagg.exit, schneagg.progress)
        if (schneagg.tile == state.spawn) {
            clipPath(tunnelClip) { drawSchneagg(point, schneagg.color) }
        } else {
            drawSchneagg(point, schneagg.color)
        }
    }

    // 5. Houses above the schneaggs so they disappear inside
    val houseSize = tile * 0.72f
    state.schneagghusList.forEach { house ->
        val at = center(house.position)
        val outlineSize = houseSize * outlineScale
        translate(at.x - outlineSize / 2f, at.y - outlineSize / 2f) {
            with(housePainter) {
                draw(Size(outlineSize, outlineSize), colorFilter = ColorFilter.tint(style.outline))
            }
        }
        translate(at.x - houseSize / 2f, at.y - houseSize / 2f) {
            with(housePainter) {
                draw(Size(houseSize, houseSize), colorFilter = ColorFilter.tint(house.color))
            }
        }
    }

    // 6. Next-up preview beside the mountain (the top row is kept free for it)
    if (state.upcoming.isNotEmpty()) {
        val discs = state.upcoming.take(3)
        val discStep = tile * 0.42f
        val labelWidth = nextUpLayout.size.width.toFloat()
        val totalWidth = labelWidth + tile * 0.3f + discs.size * discStep
        val margin = tile * 0.45f
        val rightSpace = (state.gridWidth - state.spawn.x - 1) * tile - margin
        val startX = if (rightSpace >= totalWidth) {
            (state.spawn.x + 1) * tile + margin
        } else {
            spawnLeft - margin - totalWidth
        }
        val midY = tile * 0.5f
        drawText(nextUpLayout, color = style.label, topLeft = Offset(startX, midY - nextUpLayout.size.height / 2f))
        discs.forEachIndexed { index, color ->
            val centerX = startX + labelWidth + tile * 0.3f + index * discStep + discStep / 2f
            val radius = if (index == 0) tile * 0.17f else tile * 0.13f
            drawCircle(color, radius = radius, center = Offset(centerX, midY))
            drawCircle(style.outline, radius = radius, center = Offset(centerX, midY), style = Stroke(width = thinStroke))
        }
    }

    // 7. Delivery pulses
    state.feedback.forEach { feedback ->
        val t = ((state.elapsedMillis - feedback.atElapsedMillis).toFloat() / FEEDBACK_DURATION_MS).coerceIn(0f, 1f)
        val fade = 1f - t
        val at = center(feedback.position)
        if (feedback.correct) {
            drawCircle(
                color = style.correct.copy(alpha = fade),
                radius = tile * (0.45f + 0.45f * t),
                center = at,
                style = Stroke(width = pulseStroke),
            )
            val points = textMeasurer.measure("+${feedback.points}", pointsStyle)
            drawText(
                textLayoutResult = points,
                color = style.label,
                topLeft = Offset(
                    at.x - points.size.width / 2f,
                    at.y - tile * 0.55f - t * tile * 0.6f - points.size.height / 2f,
                ),
                alpha = fade,
            )
        } else {
            val shake = sin(t * 6f * PI.toFloat()) * fade * tile * 0.08f
            val shaken = Offset(at.x + shake, at.y)
            val color = style.wrong.copy(alpha = fade)
            drawCircle(color, radius = tile * 0.5f, center = shaken, style = Stroke(width = pulseStroke))
            val arm = tile * 0.18f
            drawLine(color, shaken + Offset(-arm, -arm), shaken + Offset(arm, arm), pulseStroke, StrokeCap.Round)
            drawLine(color, shaken + Offset(-arm, arm), shaken + Offset(arm, -arm), pulseStroke, StrokeCap.Round)
        }
    }
}

/** Stable pseudo-random value per cell so the scenery does not flicker between frames. */
private fun sceneryHash(x: Int, y: Int, spawnX: Int, gridHeight: Int): Int {
    var hash = x * 73856093 xor y * 19349663 xor spawnX * 83492791 xor gridHeight * 49979687
    hash = hash xor (hash ushr 13)
    hash *= 1274126177
    hash = hash xor (hash ushr 16)
    return hash and 0x7fffffff
}

/** A pine silhouette: two stacked triangles on a short trunk, [size] tall. */
private fun DrawScope.drawPine(centerX: Float, baseY: Float, size: Float, color: Color) {
    val pine = Path().apply {
        moveTo(centerX - size * 0.07f, baseY)
        lineTo(centerX + size * 0.07f, baseY)
        lineTo(centerX + size * 0.07f, baseY - size * 0.18f)
        lineTo(centerX + size * 0.42f, baseY - size * 0.18f)
        lineTo(centerX + size * 0.12f, baseY - size * 0.55f)
        lineTo(centerX + size * 0.28f, baseY - size * 0.55f)
        lineTo(centerX, baseY - size)
        lineTo(centerX - size * 0.28f, baseY - size * 0.55f)
        lineTo(centerX - size * 0.12f, baseY - size * 0.55f)
        lineTo(centerX - size * 0.42f, baseY - size * 0.18f)
        lineTo(centerX - size * 0.07f, baseY - size * 0.18f)
        close()
    }
    drawPath(pine, color)
}

@Composable
@Preview(showBackground = true)
private fun SchneaggaHusMidGamePreview() {
    SchneaggchatTheme {
        Box(modifier = Modifier.size(360.dp, 720.dp)) {
            SchneaggaHusScreen(
                state = previewSchneaggaHusState(),
                showStartOverlay = false,
                onAction = {},
                onStart = {},
                onStop = {},
                onBackClick = {},
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun SchneaggaHusWaveBannerPreview() {
    SchneaggchatTheme {
        Box(modifier = Modifier.size(360.dp, 720.dp)) {
            SchneaggaHusScreen(
                state = previewSchneaggaHusState(showBanner = true),
                showStartOverlay = false,
                onAction = {},
                onStart = {},
                onStop = {},
                onBackClick = {},
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun SchneaggaHusMidGameDarkPreview() {
    SchneaggchatTheme(darkTheme = true) {
        Box(modifier = Modifier.size(360.dp, 720.dp).background(MaterialTheme.colorScheme.background)) {
            SchneaggaHusScreen(
                state = previewSchneaggaHusState(),
                showStartOverlay = false,
                onAction = {},
                onStart = {},
                onStop = {},
                onBackClick = {},
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun SchneaggaHusBoardPreview() {
    SchneaggchatTheme {
        Box(modifier = Modifier.size(320.dp, 500.dp).padding(8.dp)) {
            SchneaggaHusBoard(
                state = previewSchneaggaHusState(),
                onSwitchClick = {},
            )
        }
    }
}
