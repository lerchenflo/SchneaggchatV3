package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.bull

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoFootprint
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.onFootprint
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawCowboyHat
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Degrees the rider's upper body tips at the very edge of his balance. */
private const val RIDER_MAX_LEAN_DEGREES = 55f
private const val GAUGE_RADIUS = 5f
/** Share of the gauge at either end that means "about to fall off". */
private const val GAUGE_DANGER = 0.15f

/**
 * The bull, facing right, with the rider on its back while [rider]. [x] is its rear, [pitch]
 * degrees clockwise (positive kicks the rear up), [lean] the rider's balance from -1 (back) to 1
 * (forward). While riding ([rideProgress] set) a balance gauge and the time to the bell float above.
 */
@Immutable
data class RodeoBullUi(
    val x: Float,
    val gaitPhase: Float,
    val pitch: Float,
    val rider: Boolean,
    val lean: Float,
    val rideProgress: Float?,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override val footprint get() = RodeoFootprint(x + 6f, x + 23f)

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        if (layer != RodeoLayer.FRONT) return
        onFootprint(context, footprint) { drawBull(this@RodeoBullUi, it) }
        rideProgress?.let { drawBalanceGauge(this@RodeoBullUi, it, context) }
    }
}

/** Side view on a grid with x from the rear and y up from the hooves. */
private fun DrawScope.drawBull(bull: RodeoBullUi, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    val bodyColor = colors.onSurfaceVariant
    val hornColor = colors.surfaceBright
    fun p(x: Float, y: Float) = context.p(bull.x + x, y)

    // Kicking the rear up pivots around the front hooves, rearing up around the hind ones
    val pivot = if (bull.pitch >= 0f) p(22f, 0f) else p(7f, 0f)
    rotate(bull.pitch, pivot = pivot) {
        // Legs, galloping
        listOf(6f to 0f, 9f to 0.9f, 20f to 2.1f, 23f to 3f).forEach { (legX, offset) ->
            val phase = bull.gaitPhase + offset
            val lift = max(0f, cos(phase)) * 2f
            val reach = sin(phase) * 2.2f
            drawLine(bodyColor, p(legX, 10f), p(legX + reach, lift), 1.9f * unit, StrokeCap.Round)
        }

        // Tail with its tuft, swishing
        val swish = sin(bull.gaitPhase * 0.7f) * 1.5f
        val tail = Path().apply {
            val start = p(3.5f, 17f)
            val control = p(0f, 15f + swish)
            val end = p(0.5f + swish * 0.4f, 9f)
            moveTo(start.x, start.y)
            quadraticTo(control.x, control.y, end.x, end.y)
        }
        drawPath(tail, bodyColor, style = Stroke(width = 0.8f * unit, cap = StrokeCap.Round))
        drawCircle(bodyColor, radius = 1.1f * unit, center = p(0.5f + swish * 0.4f, 8.6f))

        // Heavy body with the shoulder hump
        drawRoundRect(bodyColor, p(3f, 19f), Size(21f * unit, 11f * unit), CornerRadius(4f * unit))
        drawCircle(bodyColor, radius = 4.5f * unit, center = p(19f, 18.5f))

        // Lowered head, eye, nose ring and horns
        drawPath(polygonPath(::p, 22f to 18f, 28.5f to 13.5f, 30.5f to 10f, 28f to 8.5f, 23f to 11f), bodyColor)
        drawCircle(colors.surface, radius = 0.5f * unit, center = p(27.2f, 13f))
        drawCircle(colors.tertiary, radius = 0.8f * unit, center = p(30f, 9.2f), style = Stroke(width = 0.3f * unit))
        listOf(0f, -1.2f).forEach { shift ->
            val horn = Path().apply {
                val base = p(25.5f + shift, 15.5f)
                val control = p(28f + shift, 16f)
                val tip = p(29.5f + shift, 19.5f)
                moveTo(base.x, base.y)
                quadraticTo(control.x, control.y, tip.x, tip.y)
            }
            drawPath(horn, hornColor, style = Stroke(width = 1f * unit, cap = StrokeCap.Round))
        }

        if (bull.rider) drawBullRider(::p, bull.lean, context)
    }
}

/** The cowboy on the bull's back, one hand up in the air, tipping with his balance. */
private fun DrawScope.drawBullRider(p: (Float, Float) -> Offset, lean: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    val hips = p(BULL_SEAT_X, BULL_SEAT_Y)
    drawLine(colors.onSurface, hips, p(BULL_SEAT_X + 2f, BULL_SEAT_Y - 5f), 1.3f * unit, StrokeCap.Round)
    rotate(RIDER_MAX_LEAN_DEGREES * lean, pivot = hips) {
        drawRoundRect(
            colors.primary,
            p(BULL_SEAT_X - 1.5f, BULL_SEAT_Y + 6f),
            Size(3f * unit, 6f * unit),
            CornerRadius(1f * unit)
        )
        // One hand on the rope, the other one waving high
        drawLine(colors.primary, p(BULL_SEAT_X + 1f, BULL_SEAT_Y + 4.5f), p(BULL_SEAT_X + 4f, BULL_SEAT_Y + 1.5f), 1f * unit, StrokeCap.Round)
        drawLine(colors.primary, p(BULL_SEAT_X - 1f, BULL_SEAT_Y + 5f), p(BULL_SEAT_X - 4f, BULL_SEAT_Y + 9f), 1f * unit, StrokeCap.Round)
        drawCircle(colors.onSurface, radius = 1.8f * unit, center = p(BULL_SEAT_X + 0.2f, BULL_SEAT_Y + 7.6f))
        drawCowboyHat(p, BULL_SEAT_X - 3f, BULL_SEAT_Y + 9.6f, unit)
    }
}

/**
 * Balance gauge above the bull: a half circle from "flat back" on the left to "over the horns" on
 * the right, red at both ends, with a needle at the rider's lean. Below it a bar fills up to the bell.
 */
private fun DrawScope.drawBalanceGauge(bull: RodeoBullUi, rideProgress: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    val center = context.p(bull.x + BULL_LENGTH / 2f, 38f)
    val radius = GAUGE_RADIUS * unit
    val topLeft = center - Offset(radius, radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    val stroke = Stroke(width = 0.8f * unit, cap = StrokeCap.Butt)
    val danger = 180f * GAUGE_DANGER
    drawArc(colors.outlineVariant, startAngle = 180f + danger, sweepAngle = 180f - 2f * danger, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
    drawArc(colors.error, startAngle = 180f, sweepAngle = danger, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
    drawArc(colors.error, startAngle = 360f - danger, sweepAngle = danger, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
    val angle = (270f + 90f * bull.lean.coerceIn(-1f, 1f)) * PI.toFloat() / 180f
    drawLine(colors.onSurface, center, center + Offset(cos(angle), sin(angle)) * (radius * 0.9f), 0.4f * unit, StrokeCap.Round)
    drawCircle(colors.onSurface, radius = 0.5f * unit, center = center)

    val barTop = center + Offset(-radius, 1.2f * unit)
    val barSize = Size(radius * 2f, 0.8f * unit)
    drawRoundRect(colors.outlineVariant, barTop, barSize, CornerRadius(0.4f * unit))
    drawRoundRect(colors.primary, barTop, Size(barSize.width * rideProgress, barSize.height), CornerRadius(0.4f * unit))
}
