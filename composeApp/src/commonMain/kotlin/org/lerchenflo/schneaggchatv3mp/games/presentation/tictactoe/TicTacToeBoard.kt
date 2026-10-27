package org.lerchenflo.schneaggchatv3mp.games.presentation.tictactoe

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.BoardShift
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.Cell
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TicTacToeMark
import org.lerchenflo.schneaggchatv3mp.games.domain.tictactoe.TicTacToeRound
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.tictactoe_cell_description
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Player colors: theme roles only, the symbol shape tells the players apart without color too. */
@Composable
fun playerColor(player: Int): Color {
    val colors = MaterialTheme.colorScheme
    return when (player % 4) {
        0 -> colors.primary
        1 -> colors.tertiary
        2 -> colors.secondary
        else -> colors.error
    }
}

/** Cross, circle, triangle, square - drawn as a stroke, [progress] 0..1 draws it in. */
@Composable
fun PlayerSymbol(
    player: Int,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    color: Color = playerColor(player),
) {
    Canvas(modifier) {
        val strokeWidth = size.minDimension * 0.14f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val inset = strokeWidth / 2
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset

        when (player % 4) {
            0 -> {
                // Two strokes, one after the other
                val first = (progress * 2).coerceIn(0f, 1f)
                val second = (progress * 2 - 1).coerceIn(0f, 1f)
                drawLine(color, Offset(left, top), lerp(Offset(left, top), Offset(right, bottom), first), strokeWidth, StrokeCap.Round)
                if (second > 0f) {
                    drawLine(color, Offset(right, top), lerp(Offset(right, top), Offset(left, bottom), second), strokeWidth, StrokeCap.Round)
                }
            }
            1 -> drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = stroke,
            )
            2 -> drawPartialPath(
                path = Path().apply {
                    moveTo(size.width / 2, top)
                    lineTo(right, bottom)
                    lineTo(left, bottom)
                    close()
                },
                progress = progress,
                color = color,
                stroke = stroke,
            )
            else -> drawPartialPath(
                path = Path().apply {
                    moveTo(left, top)
                    lineTo(right, top)
                    lineTo(right, bottom)
                    lineTo(left, bottom)
                    close()
                },
                progress = progress,
                color = color,
                stroke = stroke,
            )
        }
    }
}

private fun DrawScope.drawPartialPath(path: Path, progress: Float, color: Color, stroke: Stroke) {
    if (progress >= 1f) {
        drawPath(path, color, style = stroke)
        return
    }
    val measure = PathMeasure()
    measure.setPath(path, forceClosed = true)
    val segment = Path()
    measure.getSegment(0f, measure.length * progress, segment, startWithMoveTo = true)
    drawPath(segment, color, style = stroke)
}

/**
 * The board: grid, tap targets, marks that slide when they fall or get shuffled, plus one-shot
 * effects for the last turn (bomb blast, shuffled line, winning line).
 */
@Composable
fun TicTacToeBoard(
    round: TicTacToeRound,
    bombArmed: Boolean,
    onCellClick: (Cell) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardSize = round.config.boardSize
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val shiftColor = MaterialTheme.colorScheme.tertiaryContainer
    val blastColor = MaterialTheme.colorScheme.error
    val winColor = round.winner?.let { playerColor(it) } ?: Color.Unspecified

    val pulse by rememberInfiniteTransition(label = "boardPulse").animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "boardPulseAlpha",
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        val cellSize = maxWidth / boardSize
        val cellPx = with(LocalDensity.current) { cellSize.toPx() }

        Canvas(Modifier.fillMaxSize()) {
            for (line in 1 until boardSize) {
                val position = line * cellPx
                drawLine(gridColor, Offset(position, cellPx * 0.12f), Offset(position, size.height - cellPx * 0.12f), 3.dp.toPx(), StrokeCap.Round)
                drawLine(gridColor, Offset(cellPx * 0.12f, position), Offset(size.width - cellPx * 0.12f, position), 3.dp.toPx(), StrokeCap.Round)
            }
        }

        round.lastShift?.let { shift ->
            key(round.turnCount) {
                ShiftFlash(shift = shift, cellPx = cellPx, color = shiftColor)
            }
        }

        for (row in 0 until boardSize) {
            for (column in 0 until boardSize) {
                val cell = Cell(row, column)
                val description = stringResource(Res.string.tictactoe_cell_description, row + 1, column + 1)
                Box(
                    Modifier
                        .offset(x = cellSize * column, y = cellSize * row)
                        .size(cellSize)
                        .clickable(enabled = !round.isOver) { onCellClick(cell) }
                        .semantics { contentDescription = description }
                )
            }
        }

        // The current player's next mark to fade pulses, everyone else's stays dimmed
        val fadingIds = (0 until round.config.playerCount).mapNotNull { round.nextToVanish(it)?.id }.toSet()
        val currentFadingId = round.nextToVanish(round.currentPlayer)?.id

        round.marks.forEach { mark ->
            key(mark.id) {
                val alpha = when {
                    round.winner != null && mark.cell !in round.winLine -> 0.3f
                    mark.id == currentFadingId && !round.isOver -> pulse
                    mark.id in fadingIds && !round.isOver -> 0.45f
                    else -> 1f
                }
                BoardMark(
                    mark = mark,
                    cellPx = cellPx,
                    dropIn = round.config.gravity && mark.id == round.lastPlacedId,
                    bombTarget = bombArmed && mark.owner != round.currentPlayer,
                    targetAlpha = pulse,
                    modifier = Modifier
                        .size(cellSize)
                        .alpha(alpha),
                )
            }
        }

        round.lastBombCell?.let { cell ->
            key(round.turnCount) {
                BombBlast(cell = cell, cellPx = cellPx, color = blastColor)
            }
        }

        if (round.winLine.isNotEmpty()) {
            key(round.turnCount) {
                WinLine(line = round.winLine, cellPx = cellPx, color = winColor)
            }
        }
    }
}

@Composable
private fun BoardMark(
    mark: TicTacToeMark,
    cellPx: Float,
    dropIn: Boolean,
    bombTarget: Boolean,
    targetAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val target = IntOffset(
        x = (mark.cell.column * cellPx).roundToInt(),
        y = (mark.cell.row * cellPx).roundToInt(),
    )
    // Gravity drops a new mark in from above the board; later moves (falls, shuffles) slide
    val position = remember(mark.id) {
        Animatable(if (dropIn) IntOffset(target.x, -cellPx.roundToInt()) else target, IntOffset.VectorConverter)
    }
    LaunchedEffect(target) {
        position.animateTo(target, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    val drawIn = remember(mark.id) { Animatable(0f) }
    LaunchedEffect(mark.id) {
        drawIn.animateTo(1f, tween(durationMillis = 380))
    }

    val ringColor = MaterialTheme.colorScheme.error
    // Breathing room around the symbol, relative to the cell so it scales with the board size
    val symbolPadding = with(LocalDensity.current) { (cellPx * 0.22f).toDp() }
    Box(modifier.offset { position.value }) {
        if (bombTarget) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                drawCircle(ringColor.copy(alpha = targetAlpha), style = Stroke(width = 3.dp.toPx()))
            }
        }
        PlayerSymbol(
            player = mark.owner,
            progress = drawIn.value,
            modifier = Modifier
                .fillMaxSize()
                .padding(symbolPadding),
        )
    }
}

@Composable
private fun BombBlast(cell: Cell, cellPx: Float, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 650)) }
    val value = progress.value
    if (value >= 1f) return

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset((cell.column + 0.5f) * cellPx, (cell.row + 0.5f) * cellPx)
        val fade = 1f - value
        drawCircle(color.copy(alpha = 0.35f * fade), radius = cellPx * (0.15f + 0.5f * value), center = center)
        drawCircle(
            color = color.copy(alpha = fade),
            radius = cellPx * (0.2f + 0.6f * value),
            center = center,
            style = Stroke(width = cellPx * 0.06f * fade + 1f),
        )
        // Sparks flying outwards
        repeat(8) { index ->
            val angle = index * PI / 4
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(
                color = color.copy(alpha = fade),
                start = center + direction * (cellPx * (0.25f + 0.35f * value)),
                end = center + direction * (cellPx * (0.35f + 0.5f * value)),
                strokeWidth = cellPx * 0.05f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ShiftFlash(shift: BoardShift, cellPx: Float, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis = 1200)) }
    val value = progress.value
    if (value >= 1f) return

    Canvas(Modifier.fillMaxSize()) {
        val alpha = 0.8f * (1f - value)
        if (shift.isRow) {
            drawRect(color.copy(alpha = alpha), topLeft = Offset(0f, shift.index * cellPx), size = Size(size.width, cellPx))
        } else {
            drawRect(color.copy(alpha = alpha), topLeft = Offset(shift.index * cellPx, 0f), size = Size(cellPx, size.height))
        }
    }
}

@Composable
private fun WinLine(line: List<Cell>, cellPx: Float, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // Let the last mark land first
        delay(300)
        progress.animateTo(1f, tween(durationMillis = 450))
    }

    Canvas(Modifier.fillMaxSize()) {
        val first = line.first()
        val last = line.last()
        val start = Offset((first.column + 0.5f) * cellPx, (first.row + 0.5f) * cellPx)
        val end = Offset((last.column + 0.5f) * cellPx, (last.row + 0.5f) * cellPx)
        if (progress.value > 0f) {
            drawLine(color, start, lerp(start, end, progress.value), cellPx * 0.12f, StrokeCap.Round)
        }
    }
}
