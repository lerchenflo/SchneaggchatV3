package org.lerchenflo.schneaggchatv3mp.games.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

// Shared by the shared-device board games (Tic Tac Toe, Four in a Row): one color and one symbol per seat

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
