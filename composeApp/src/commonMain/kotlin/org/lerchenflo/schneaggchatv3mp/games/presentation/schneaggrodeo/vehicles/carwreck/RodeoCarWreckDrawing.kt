package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.carwreck

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
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.cloudNoise
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.cos
import kotlin.math.sin

// The wreck's fixed colors, explicitly requested: a red Escort, black plastic, rust and sparks
private val CAR_RED = Color(0xFFD32F2F)
private val CAR_RED_DARK = Color(0xFF8E1B1B)
private val CAR_TRIM = Color(0xFF212121)
private val CAR_RUST = Color(0xFF8D5524)
private val CAR_SPARK = Color(0xFFFFC107)

/**
 * The wrecked Escort cabrio, facing right. [x] is its rear bumper, [y] its belly above the ground,
 * [rotation] degrees clockwise (rattling while shoved, tumbling once kicked away). [sparks] drives
 * the sparks flying from its belly while it scrapes along, null while it doesn't.
 */
@Immutable
data class RodeoCarWreckUi(
    val x: Float,
    val y: Float,
    val rotation: Float,
    /** The cowboy sits behind the wheel. */
    val hasDriver: Boolean,
    val sparks: Float?,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        // In front of the horse: its nose disappears behind the trunk it shoves
        if (layer == RodeoLayer.FRONT) drawCarWreck(this@RodeoCarWreckUi, context)
    }
}

/** Side view on a grid with x from the rear bumper and y up from the belly. */
private fun DrawScope.drawCarWreck(car: RodeoCarWreckUi, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    fun p(x: Float, y: Float) = context.p(car.x + x, car.y + y)

    car.sparks?.let { drawScrapeSparks(::p, unit, it) }

    rotate(car.rotation, pivot = p(CAR_LENGTH / 2f, 4f)) {
        // Inside the open cabin: rear seat, driver's seat back and the folded, torn soft top
        drawRect(CAR_TRIM, p(12f, 10f), Size(2f * unit, 2.6f * unit))
        drawRect(CAR_TRIM, p(18.2f, 10.8f), Size(1.6f * unit, 3.4f * unit))
        drawRoundRect(CAR_TRIM, p(8.8f, 9.4f), Size(4.8f * unit, 2f * unit), CornerRadius(0.8f * unit))

        if (car.hasDriver) {
            // Torso, arms on the wheel, head and hat above the door
            drawRoundRect(colors.primary, p(CAR_SEAT_X - 1.2f, 11.4f), Size(3f * unit, 4.6f * unit), CornerRadius(1f * unit))
            drawLine(colors.primary, p(CAR_SEAT_X + 1.2f, 10.2f), p(24f, 9.2f), 1f * unit, StrokeCap.Round)
            drawCircle(colors.onSurface, radius = 1.8f * unit, center = p(CAR_SEAT_X + 0.3f, 13.2f))
            drawRoundRect(HAT_COLOR, p(CAR_SEAT_X - 2.9f, 15.4f), Size(6.4f * unit, 0.7f * unit), CornerRadius(0.35f * unit))
            drawRoundRect(HAT_COLOR, p(CAR_SEAT_X - 1.5f, 17.8f), Size(3.6f * unit, 2.6f * unit), CornerRadius(0.8f * unit))
        }
        // Steering wheel, bent
        drawLine(CAR_TRIM, p(23.4f, 7.6f), p(24.6f, 10.4f), 0.5f * unit, StrokeCap.Round)

        // Body: trunk, sills lying on the ground (no wheels), long hood
        drawPath(
            polygonPath(
                ::p,
                0.3f to 0f, 0f to 4.5f, 0.6f to 7.6f, 9.5f to 8.2f, 10f to 7.6f,
                28f to 7.6f, 29f to 7.4f, 37f to 6.4f, 38f to 5f, 37.8f to 0f,
            ),
            CAR_RED
        )
        // Empty wheel arches: nothing but a dark hollow
        listOf(7.5f, 30.5f).forEach { archX ->
            drawArc(
                CAR_TRIM,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = p(archX - 3.6f, 3.6f),
                size = Size(7.2f * unit, 7.2f * unit)
            )
        }
        // Rust around the arches and dents all over
        repeat(5) { index ->
            val dentX = 3f + 32f * cloudNoise(index, 7)
            val dentY = 1.5f + 5f * cloudNoise(index, 8)
            drawOval(
                if (index % 2 == 0) CAR_RUST else CAR_RED_DARK,
                topLeft = p(dentX, dentY),
                size = Size((1.2f + cloudNoise(index, 9) * 1.6f) * unit, 1f * unit)
            )
        }
        // The door hangs askew
        rotate(7f, pivot = p(15f, 7.4f)) {
            drawRect(CAR_RED_DARK, p(15f, 7.2f), Size(12f * unit, 5.6f * unit), style = Stroke(width = 0.35f * unit))
            drawLine(CAR_TRIM, p(24.5f, 5.6f), p(26f, 5.6f), 0.4f * unit, StrokeCap.Round)
        }
        // Black bumpers, a broken tail light and an empty headlight socket
        drawRect(CAR_TRIM, p(-0.6f, 3.6f), Size(2.8f * unit, 1.6f * unit))
        drawRect(CAR_TRIM, p(36f, 3.4f), Size(2.6f * unit, 1.4f * unit))
        drawRect(CAR_RED_DARK, p(0.2f, 6.8f), Size(1.4f * unit, 1.6f * unit))
        drawCircle(CAR_TRIM, radius = 0.9f * unit, center = p(37f, 5.4f))

        // Roll bar and the bent windshield frame with cracked glass
        drawLine(CAR_TRIM, p(15.8f, 7.6f), p(16.8f, 12.8f), 0.8f * unit, StrokeCap.Round)
        drawLine(CAR_TRIM, p(16.8f, 12.8f), p(18f, 12.8f), 0.8f * unit, StrokeCap.Round)
        drawPath(polygonPath(::p, 27.6f to 7.6f, 26.2f to 10.6f, 25f to 12.2f, 23.8f to 11.6f, 25.4f to 7.6f), colors.surfaceVariant.copy(alpha = 0.45f))
        drawLine(CAR_TRIM, p(27.6f, 7.6f), p(26.2f, 10.6f), 0.4f * unit, StrokeCap.Round)
        drawLine(CAR_TRIM, p(26.2f, 10.6f), p(25f, 12.2f), 0.4f * unit, StrokeCap.Round)
        drawLine(colors.outline, p(26.8f, 8.4f), p(25.2f, 10.4f), 0.15f * unit)
        drawLine(colors.outline, p(25.9f, 9.2f), p(26.6f, 10.8f), 0.15f * unit)
    }
}

/** Sparks spraying back and up from the belly scraping over the ground; [phase] animates them. */
private fun DrawScope.drawScrapeSparks(p: (Float, Float) -> Offset, unit: Float, phase: Float) {
    repeat(6) { index ->
        val cycle = (phase * 5f + index * 0.37f) % 1f
        val angle = 2.6f + 0.5f * cloudNoise(index, 3)
        val length = (2f + 3f * cloudNoise(index, 4)) * cycle
        val origin = p(4f + 26f * cloudNoise(index, 5), 0f)
        val end = origin + Offset(cos(angle) * length * unit * 2f, -sin(angle) * length * unit)
        drawLine(CAR_SPARK.copy(alpha = 1f - cycle), origin, end, 0.3f * unit, StrokeCap.Round)
    }
}
