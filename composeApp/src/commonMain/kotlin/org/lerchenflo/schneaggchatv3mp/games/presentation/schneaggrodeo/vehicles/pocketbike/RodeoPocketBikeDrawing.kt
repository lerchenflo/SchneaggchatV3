package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pocketbike

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.HAT_COLOR
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// The pocket bike's fixed colors, explicitly requested: red plastics with cream flame decals, a black
// seat and grips, silver engine, fork and frame, a rusty exhaust
private val BIKE_RED = Color(0xFFD32F2F)
private val BIKE_FLAME = Color(0xFFF3E5C8)
private val BIKE_BLACK = Color(0xFF212121)
private val BIKE_CHROME = Color(0xFFB0BEC5)
private val BIKE_RUST = Color(0xFF8D5524)

/**
 * The pocket bike, facing right: [x] is its rear end, [rotation] degrees clockwise (rattling at full
 * throttle, tipped over once the engine died). The smoke cloud drifts behind it.
 */
@Immutable
data class RodeoPocketBikeUi(
    val x: Float,
    val rotation: Float,
    val wheelPhase: Float,
    /** The cowboy sits on it, knees up at his chin. */
    val hasRider: Boolean,
    val smoke: List<RodeoSmokePuffUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        // Ahead of the horse; the smoke drifts back over it
        if (layer == RodeoLayer.FRONT) {
            drawPocketBike(this@RodeoPocketBikeUi, context)
            smoke.forEach { puff ->
                drawCircle(
                    context.colors.outline.copy(alpha = 0.55f * puff.alpha),
                    radius = puff.radius * context.unit,
                    center = context.p(puff.x, puff.y)
                )
            }
        }
    }
}

/** A puff of two-stroke smoke; [x] / [y] its center. */
@Immutable
data class RodeoSmokePuffUi(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/**
 * The bike on a grid with y up from the ground and x from its rear, modelled on a real pit bike:
 * knobby tyres on silver spoked rims, a long raked fork with a tall red number plate, high red
 * padded handlebars, red plastics with cream flames, a long black seat, a silver engine with a
 * rusty exhaust, and the chain to the rear wheel. The rider, if any, sits on it with his knees up
 * at his chin.
 */
private fun DrawScope.drawPocketBike(bike: RodeoPocketBikeUi, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    fun p(x: Float, y: Float) = context.p(bike.x + x, y)
    val rearAxle = BIKE_REAR_WHEEL_X to BIKE_WHEEL_RADIUS
    val frontAxle = BIKE_FRONT_WHEEL_X to BIKE_WHEEL_RADIUS

    rotate(bike.rotation, pivot = p(BIKE_LENGTH / 2f, 0f)) {
        // Swingarm and chain to the rear wheel, frame tubes
        drawLine(BIKE_CHROME, p(rearAxle.first, rearAxle.second), p(4.4f, 2.4f), 0.35f * unit, StrokeCap.Round)
        drawLine(BIKE_BLACK, p(rearAxle.first, rearAxle.second + 0.4f), p(4.2f, 2f), 0.15f * unit)
        drawLine(BIKE_BLACK, p(rearAxle.first, rearAxle.second - 0.4f), p(4.2f, 1.4f), 0.15f * unit)
        drawLine(BIKE_CHROME, p(4.2f, 1.2f), p(6.8f, 1.2f), 0.3f * unit, StrokeCap.Round)
        drawLine(BIKE_CHROME, p(6.8f, 1.2f), p(7.2f, 4.6f), 0.3f * unit, StrokeCap.Round)

        // Engine with cooling fins, and the rusty exhaust running back under the seat
        drawRect(BIKE_CHROME, p(4.2f, 3.2f), Size(2.4f * unit, 1.9f * unit))
        repeat(3) { fin -> drawLine(colors.outline, p(5.2f + fin * 0.45f, 3.6f), p(5.2f + fin * 0.45f, 4.4f), 0.12f * unit) }
        drawLine(BIKE_RUST, p(6.4f, 2f), p(4f, 2.3f), 0.45f * unit, StrokeCap.Round)
        drawLine(BIKE_RUST, p(4f, 2.3f), p(BIKE_EXHAUST_X, BIKE_EXHAUST_Y), 0.4f * unit, StrokeCap.Round)

        // Red tail, side panel and tank with cream flames, the long black seat on top
        drawPath(polygonPath(::p, 0.2f to 5f, 1.8f to 4.8f, 6.2f to 4.9f, 7.4f to 6f, 7.6f to 4.8f, 6.8f to 3.3f, 3.2f to 3.4f, 1.6f to 4.2f), BIKE_RED)
        drawPath(polygonPath(::p, 6.9f to 5.1f, 5.6f to 4.4f, 4.4f to 4.35f, 5.2f to 4.1f, 3.6f to 3.8f, 5.4f to 3.75f, 6.4f to 3.5f, 7f to 4.2f), BIKE_FLAME)
        drawPath(polygonPath(::p, 2.8f to 4.5f, 1.9f to 4.35f, 2.6f to 4.2f, 3.6f to 4.2f), BIKE_FLAME)
        drawRoundRect(BIKE_BLACK, p(1.6f, 5.5f), Size(4.8f * unit, 0.7f * unit), CornerRadius(0.35f * unit))
        drawCircle(BIKE_BLACK, radius = 0.3f * unit, center = p(6.9f, 6.1f)) // fuel cap

        // Long raked fork, tall red number plate and high padded handlebars
        drawLine(BIKE_CHROME, p(frontAxle.first, frontAxle.second), p(7.3f, 6.6f), 0.4f * unit, StrokeCap.Round)
        drawPath(polygonPath(::p, 7.1f to 4.2f, 7.9f to 4.4f, 8.2f to 7f, 7.3f to 6.9f), BIKE_RED)
        drawCircle(BIKE_FLAME, radius = 0.35f * unit, center = p(7.7f, 6.1f))
        drawLine(BIKE_RED, p(7.3f, 7.4f), p(6.6f, 7.6f), 0.55f * unit, StrokeCap.Round)
        drawLine(BIKE_BLACK, p(7.3f, 7.2f), p(6.5f, 6.8f), 0.35f * unit, StrokeCap.Round)

        listOf(rearAxle, frontAxle).forEach { (axleX, axleY) -> drawKnobbyWheel(p(axleX, axleY), unit, bike.wheelPhase, front = axleX == BIKE_FRONT_WHEEL_X) }

        if (bike.hasRider) {
            val pants = colors.onSurface
            val shirt = colors.primary
            // Legs folded up: thigh up to the knee at chin height, shin down to the foot peg
            drawLine(pants, p(BIKE_SEAT_X, BIKE_SEAT_Y + 0.6f), p(6.2f, 8.4f), 1f * unit, StrokeCap.Round)
            drawLine(pants, p(6.2f, 8.4f), p(4.8f, 2.6f), 0.9f * unit, StrokeCap.Round)
            // Torso leaning forward, arm to the grip, head and hat
            drawLine(shirt, p(BIKE_SEAT_X, BIKE_SEAT_Y + 0.8f), p(4.6f, 11f), 2.4f * unit, StrokeCap.Round)
            drawLine(shirt, p(4.9f, 10.2f), p(6.7f, 7f), 0.8f * unit, StrokeCap.Round)
            drawCircle(colors.onSurface, radius = 1.6f * unit, center = p(5.2f, 12.8f))
            drawRoundRect(HAT_COLOR, p(2.9f, 14.6f), Size(5.2f * unit, 0.6f * unit), CornerRadius(0.3f * unit))
            drawRoundRect(HAT_COLOR, p(4f, 16.7f), Size(2.8f * unit, 2.2f * unit), CornerRadius(0.7f * unit))
        }
    }
}

/** A knobby tyre on a silver spoked rim turned by [phase]; the [front] one has a brake disc. */
private fun DrawScope.drawKnobbyWheel(center: Offset, unit: Float, phase: Float, front: Boolean) {
    val radius = BIKE_WHEEL_RADIUS * unit
    drawCircle(BIKE_BLACK, radius = radius, center = center)
    // Knobs around the tyre
    repeat(12) { index ->
        val angle = phase + index * 2f * PI.toFloat() / 12f
        drawCircle(BIKE_BLACK, radius = 0.16f * unit, center = center + Offset(cos(angle), sin(angle)) * (radius + 0.05f * unit))
    }
    drawCircle(BIKE_CHROME, radius = radius * 0.62f, center = center)
    repeat(8) { index ->
        val angle = phase + index * 2f * PI.toFloat() / 8f
        drawLine(BIKE_BLACK.copy(alpha = 0.6f), center, center + Offset(cos(angle), sin(angle)) * (radius * 0.6f), 0.07f * unit)
    }
    if (front) drawCircle(BIKE_BLACK.copy(alpha = 0.5f), radius = radius * 0.35f, center = center, style = Stroke(width = 0.12f * unit))
    drawCircle(BIKE_BLACK, radius = radius * 0.14f, center = center)
}
