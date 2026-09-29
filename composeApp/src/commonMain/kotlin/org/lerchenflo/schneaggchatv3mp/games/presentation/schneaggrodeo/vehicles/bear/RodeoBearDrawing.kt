package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.bear

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.TREE_TRUNK
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawCowboyHat
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// The bear's fixed colors, explicitly requested: a brown bear with a lighter muzzle
private val BEAR_BROWN = Color(0xFF6D4C41)
private val BEAR_MUZZLE = Color(0xFFA1887F)
private val BEAR_DARK = Color(0xFF3E2723)

/** Degrees the bear turns upright while climbing, hugging its tree. */
private const val CLIMB_TILT = -65f
/** Height at which it is fully upright. */
private const val CLIMB_TILT_HEIGHT = 6f

/**
 * The bear, facing right, with the rider on its back while [rider]. [x] is its rear, [height] how
 * far up its tree it climbed (the tree is drawn with it then). [chomping] opens its jaws.
 */
@Immutable
data class RodeoBearUi(
    val x: Float,
    val height: Float,
    val gaitPhase: Float,
    val rider: Boolean,
    val chomping: Boolean,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            // The tree it climbs, behind it
            RodeoLayer.BODY -> if (height > 0f) {
                val trunkX = x + BEAR_LENGTH * 0.75f
                drawLine(
                    TREE_TRUNK,
                    context.p(trunkX, 0f),
                    context.p(trunkX, BEAR_MAX_CLIMB + BEAR_LENGTH + 10f),
                    3.5f * context.unit
                )
            }
            RodeoLayer.FRONT -> drawBear(this@RodeoBearUi, context)
            RodeoLayer.BACK -> Unit
        }
    }
}

/** Side view on a grid with x from the rear and y up from the feet. */
private fun DrawScope.drawBear(bear: RodeoBearUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(bear.x + x, bear.height + y)

    val upright = min(1f, bear.height / CLIMB_TILT_HEIGHT)
    rotate(CLIMB_TILT * upright, pivot = p(BEAR_LENGTH * 0.6f, 4f)) {
        // Legs: lumbering along on the ground, reaching for the bark while climbing
        listOf(5f to 0f, 8f to 1.2f, 17f to 2.4f, 20f to 3.6f).forEach { (legX, offset) ->
            val phase = bear.gaitPhase + offset
            val lift = if (upright > 0f) 0f else max(0f, cos(phase)) * 1.8f
            val reach = if (upright > 0f) 1.5f * sin(phase) else sin(phase) * 1.8f
            drawLine(BEAR_BROWN, p(legX, 6f), p(legX + reach, lift), 2.4f * unit, StrokeCap.Round)
        }
        // Stubby tail, round body and shoulder hump
        drawCircle(BEAR_BROWN, radius = 1.2f * unit, center = p(1.5f, 12f))
        drawRoundRect(BEAR_BROWN, p(2f, 17f), Size(20f * unit, 12f * unit), CornerRadius(6f * unit))
        drawCircle(BEAR_BROWN, radius = 5f * unit, center = p(17f, 15f))

        // Head with ears, muzzle and nose; the jaw drops open while it eats
        val head = p(23.5f, 14f)
        drawCircle(BEAR_BROWN, radius = 1.5f * unit, center = p(21.5f, 18.5f))
        drawCircle(BEAR_BROWN, radius = 1.5f * unit, center = p(25f, 18.5f))
        drawCircle(BEAR_BROWN, radius = 4.3f * unit, center = head)
        drawRoundRect(BEAR_MUZZLE, p(25.5f, 14f), Size(3.5f * unit, 2.8f * unit), CornerRadius(1.2f * unit))
        drawCircle(BEAR_DARK, radius = 0.7f * unit, center = p(28.6f, 13.2f))
        drawCircle(BEAR_DARK, radius = 0.45f * unit, center = p(24.8f, 15.6f))
        if (bear.chomping) {
            drawRoundRect(BEAR_DARK, p(25.6f, 11.4f), Size(3.2f * unit, 1.2f * unit), CornerRadius(0.5f * unit))
            drawRoundRect(BEAR_MUZZLE, p(25.4f, 10.2f), Size(3.2f * unit, 1.2f * unit), CornerRadius(0.5f * unit))
        }

        if (bear.rider) drawBearRider(::p, context)
    }
}

/** The cowboy on the bear's back, hanging on to its fur with one hand, the other one waving. */
private fun DrawScope.drawBearRider(p: (Float, Float) -> Offset, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    drawLine(colors.onSurface, p(BEAR_SEAT_X, BEAR_SEAT_Y), p(BEAR_SEAT_X + 2f, BEAR_SEAT_Y - 5f), 1.3f * unit, StrokeCap.Round)
    rotate(15f, pivot = p(BEAR_SEAT_X, BEAR_SEAT_Y)) {
        drawRoundRect(colors.primary, p(BEAR_SEAT_X - 1.5f, BEAR_SEAT_Y + 6f), Size(3f * unit, 6f * unit), CornerRadius(1f * unit))
        drawLine(colors.primary, p(BEAR_SEAT_X + 1f, BEAR_SEAT_Y + 4.5f), p(BEAR_SEAT_X + 4f, BEAR_SEAT_Y + 1f), 1f * unit, StrokeCap.Round)
        drawLine(colors.primary, p(BEAR_SEAT_X - 1f, BEAR_SEAT_Y + 5f), p(BEAR_SEAT_X - 3.5f, BEAR_SEAT_Y + 9f), 1f * unit, StrokeCap.Round)
        drawCircle(colors.onSurface, radius = 1.8f * unit, center = p(BEAR_SEAT_X + 0.2f, BEAR_SEAT_Y + 7.6f))
        drawCowboyHat(p, BEAR_SEAT_X - 3f, BEAR_SEAT_Y + 9.6f, unit)
    }
}
