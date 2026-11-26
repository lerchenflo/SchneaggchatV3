package org.lerchenflo.schneaggchatv3mp.games.presentation.fourinarow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.Disc
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.FourInARowRound
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.GridCell
import org.lerchenflo.schneaggchatv3mp.games.domain.fourinarow.TwistKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.PlayerSymbol
import org.lerchenflo.schneaggchatv3mp.games.presentation.playerColor
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.fourinarow_column_description
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A side's disc color: the player's color, or with teams the team's color (1st + 3rd, 2nd + 4th). */
@Composable
fun sideColor(round: FourInARowRound, player: Int): Color = playerColor(round.config.sideOf(player))

/**
 * The board: slots, one tap target per column, discs that drop in with a bounce and slide when they
 * fall or twist, plus one-shot effects for the last turn (bomb blast, anvil, twist, winning line).
 */
@Composable
fun FourInARowBoard(
    round: FourInARowRound,
    popOutArmed: Boolean,
    onColumnClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = round.config
    val holeColor = MaterialTheme.colorScheme.surface
    val flashColor = MaterialTheme.colorScheme.tertiaryContainer
    val blastColor = MaterialTheme.colorScheme.error
    // Drawn across discs of the winner's own color, so it needs the board's text color to stand out
    val winColor = MaterialTheme.colorScheme.onSurface

    val pulse by rememberInfiniteTransition(label = "boardPulse").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "boardPulseAlpha",
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(config.columns.toFloat() / config.rows)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        val cellSize = maxWidth / config.columns
        val cellPx = with(LocalDensity.current) { cellSize.toPx() }

        Canvas(Modifier.fillMaxSize()) {
            for (row in 0 until config.rows) {
                for (column in 0 until config.columns) {
                    drawCircle(
                        color = holeColor,
                        radius = cellPx * 0.4f,
                        center = Offset((column + 0.5f) * cellPx, (row + 0.5f) * cellPx),
                    )
                }
            }
        }

        round.lastTwist?.let { twist ->
            key(round.turnCount) {
                if (twist.kind == TwistKind.FLIP) {
                    Flash(topLeft = Offset.Zero, size = null, color = flashColor)
                } else {
                    Flash(
                        topLeft = Offset(0f, twist.row * cellPx),
                        size = Size(config.columns * cellPx, cellPx),
                        color = flashColor,
                    )
                }
            }
        }
        round.lastAnvilColumn?.let { column ->
            key(round.turnCount) {
                Flash(
                    topLeft = Offset(column * cellPx, 0f),
                    size = Size(cellPx, config.rows * cellPx),
                    color = blastColor,
                )
            }
        }

        val bottomRow = config.rows - 1
        round.discs.forEach { disc ->
            key(disc.id) {
                val alpha = if (round.winnerSide != null && disc.cell !in round.winLine) 0.3f else 1f
                BoardDisc(
                    round = round,
                    disc = disc,
                    cellPx = cellPx,
                    dropIn = disc.id == round.lastDroppedId,
                    popTarget = popOutArmed && disc.cell.row == bottomRow &&
                        round.canPopOut(round.currentPlayer, disc.cell.column),
                    targetAlpha = pulse,
                    modifier = Modifier
                        .size(cellSize)
                        .alpha(alpha),
                )
            }
        }

        round.lastBlast?.let { cell ->
            key(round.turnCount) {
                BombBlast(cell = cell, cellPx = cellPx, color = blastColor)
            }
        }

        for (column in 0 until config.columns) {
            val description = stringResource(Res.string.fourinarow_column_description, column + 1)
            Box(
                Modifier
                    .offset(x = cellSize * column)
                    .width(cellSize)
                    .height(cellSize * config.rows)
                    .clickable(enabled = !round.isOver) { onColumnClick(column) }
                    .semantics { contentDescription = description }
            )
        }

        if (round.winLine.isNotEmpty()) {
            key(round.turnCount) {
                WinLine(line = round.winLine, cellPx = cellPx, color = winColor)
            }
        }
    }
}

@Composable
private fun BoardDisc(
    round: FourInARowRound,
    disc: Disc,
    cellPx: Float,
    dropIn: Boolean,
    popTarget: Boolean,
    targetAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val target = IntOffset(
        x = (disc.cell.column * cellPx).roundToInt(),
        y = (disc.cell.row * cellPx).roundToInt(),
    )
    // A new disc drops in from above the board and bounces; later moves (falls, twists) slide
    val position = remember(disc.id) {
        Animatable(if (dropIn) IntOffset(target.x, -cellPx.roundToInt()) else target, IntOffset.VectorConverter)
    }
    LaunchedEffect(target) {
        position.animateTo(target, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    val fill = if (disc.joker) MaterialTheme.colorScheme.inverseSurface else sideColor(round, disc.owner)
    val content = contentColorFor(fill)
    val ringColor = MaterialTheme.colorScheme.error
    val discPadding = with(LocalDensity.current) { (cellPx * 0.1f).toDp() }
    val symbolPadding = with(LocalDensity.current) { (cellPx * 0.3f).toDp() }

    Box(modifier.offset { position.value }) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(discPadding)
                .clip(CircleShape)
                .background(fill)
        )
        if (disc.joker) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = content,
                modifier = Modifier.fillMaxSize().padding(symbolPadding),
            )
        } else {
            // The symbol tells the players apart without color, also within a team
            PlayerSymbol(
                player = disc.owner,
                color = content,
                modifier = Modifier.fillMaxSize().padding(symbolPadding),
            )
        }
        if (popTarget) {
            Canvas(Modifier.fillMaxSize().padding(2.dp)) {
                drawCircle(ringColor.copy(alpha = targetAlpha), style = Stroke(width = 3.dp.toPx()))
            }
        }
    }
}

/** Fades a highlight out over the area of the last twist or anvil; a null [size] covers the whole board. */
@Composable
private fun Flash(topLeft: Offset, size: Size?, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 1000)) }
    val value = progress.value
    if (value >= 1f) return

    Canvas(Modifier.fillMaxSize()) {
        drawRect(color.copy(alpha = 0.6f * (1f - value)), topLeft = topLeft, size = size ?: this.size)
    }
}

@Composable
private fun BombBlast(cell: GridCell, cellPx: Float, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 650)) }
    val value = progress.value
    if (value >= 1f) return

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset((cell.column + 0.5f) * cellPx, (cell.row + 0.5f) * cellPx)
        val fade = 1f - value
        // The blast covers the cleared cells around the landing spot
        drawCircle(color.copy(alpha = 0.35f * fade), radius = cellPx * (0.3f + 1.2f * value), center = center)
        drawCircle(
            color = color.copy(alpha = fade),
            radius = cellPx * (0.4f + 1.1f * value),
            center = center,
            style = Stroke(width = cellPx * 0.08f * fade + 1f),
        )
        repeat(8) { index ->
            val angle = index * PI / 4
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(
                color = color.copy(alpha = fade),
                start = center + direction * (cellPx * (0.5f + 0.7f * value)),
                end = center + direction * (cellPx * (0.7f + 0.9f * value)),
                strokeWidth = cellPx * 0.07f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun WinLine(line: List<GridCell>, cellPx: Float, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // Let the last disc land first
        delay(400)
        progress.animateTo(1f, tween(durationMillis = 450))
    }

    Canvas(Modifier.fillMaxSize()) {
        val first = line.first()
        val last = line.last()
        val start = Offset((first.column + 0.5f) * cellPx, (first.row + 0.5f) * cellPx)
        val end = Offset((last.column + 0.5f) * cellPx, (last.row + 0.5f) * cellPx)
        if (progress.value > 0f) {
            drawLine(color.copy(alpha = 0.7f), start, lerp(start, end, progress.value), cellPx * 0.16f, StrokeCap.Round)
        }
    }
}
