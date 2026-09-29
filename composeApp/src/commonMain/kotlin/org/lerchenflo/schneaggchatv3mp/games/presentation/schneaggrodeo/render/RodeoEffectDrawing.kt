package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// Short-lived effects (see RodeoEffects) plus the lasso rope and the "lasso it" hint.
// [progress] always runs from 0 (just started) to 1 (gone).

/** Eight short sparks flying outwards and fading. */
internal fun DrawScope.drawSparkle(center: Offset, unit: Float, progress: Float, color: Color) {
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

/** Small dirt droplets flying out of a hoof in short arcs to both sides, fading as they fall. */
internal fun DrawScope.drawLandingSplash(x: Float, groundY: Float, unit: Float, progress: Float, color: Color) {
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
internal fun DrawScope.drawDust(x: Float, groundY: Float, unit: Float, progress: Float, color: Color) {
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

/** Rope from the rider's [hand] sagging slightly towards the loop at [tip]. */
internal fun DrawScope.drawLasso(hand: Offset, tip: Offset, unit: Float, color: Color) {
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

/** A rounded label with a small pointer below, telling the player to lasso what is under it. */
internal fun DrawScope.drawLassoHint(
    layout: TextLayoutResult,
    centerX: Float,
    bottomY: Float,
    unit: Float,
    color: Color,
) {
    val padX = 1.2f * unit
    val padY = 0.6f * unit
    val pointer = 1.2f * unit
    val width = layout.size.width + padX * 2f
    val height = layout.size.height + padY * 2f
    // Kept inside the canvas horizontally while the target enters from the right edge
    val left = (centerX - width / 2f).coerceIn(0f, max(0f, size.width - width))
    val top = bottomY - pointer - height
    drawRoundRect(color, Offset(left, top), Size(width, height), CornerRadius(height / 2f))
    val pointerX = centerX.coerceIn(left + height / 2f, left + width - height / 2f)
    drawPath(
        Path().apply {
            moveTo(pointerX - pointer, top + height - 0.1f * unit)
            lineTo(pointerX + pointer, top + height - 0.1f * unit)
            lineTo(pointerX, bottomY)
            close()
        },
        color = color
    )
    drawText(layout, topLeft = Offset(left + padX, top + padY))
}
