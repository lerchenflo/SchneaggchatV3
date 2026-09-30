package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.shoppingcart

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoFootprint
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.onFootprint
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// The cart's fixed colors: chrome wire and a red plastic handle
private val CART_CHROME = Color(0xFFB0BEC5)
private val CART_RED = Color(0xFFD32F2F)
private val CART_BLACK = Color(0xFF212121)

private const val SNAIL_SIZE_IN_BASKET = 2.8f
private const val SNAILS_PER_ROW = 4

/** The shopping cart, facing right: [x] is its handle at the rear, [snails] shown in the basket. */
@Immutable
data class RodeoShoppingCartUi(
    val x: Float,
    val rotation: Float,
    val wheelPhase: Float,
    val snails: Int,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override val footprint get() = RodeoFootprint(x + 3f, x + 12f)

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        // In front of the horse, which shoves it with its chest
        if (layer == RodeoLayer.FRONT) onFootprint(context, footprint) { drawShoppingCart(this@RodeoShoppingCartUi, it) }
    }
}

/**
 * The cart on a grid with y up from the ground and x from the handle: a wire basket (wider at the
 * top) with the caught snails piled up in it, the chassis with its lower tray, four casters (two
 * seen from the side) and the red handle at the back.
 */
private fun DrawScope.drawShoppingCart(cart: RodeoShoppingCartUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(cart.x + x, y)
    val wire = 0.18f * unit
    val bottomRear = BASKET_REAR + 1.5f
    val bottomFront = BASKET_FRONT - 1.5f

    rotate(cart.rotation, pivot = p(CART_LENGTH / 2f, 0f)) {
        // Snails piled up in the basket, behind the wires
        context.assets?.snail?.let { image ->
            repeat(cart.snails) { index ->
                val column = index % SNAILS_PER_ROW
                val row = index / SNAILS_PER_ROW
                val size = SNAIL_SIZE_IN_BASKET * unit
                val foot = p(bottomRear + 1.2f + column * 2.3f + (row % 2) * 1f, BASKET_FLOOR + row * 2f)
                drawImage(
                    image = image,
                    dstOffset = IntOffset((foot.x - size / 2f).roundToInt(), (foot.y - size).roundToInt()),
                    dstSize = IntSize(size.roundToInt(), size.roundToInt()),
                )
            }
        }

        // Wire basket: outline, vertical wires fanning out, horizontal wires
        val corners = listOf(BASKET_REAR to BASKET_TOP, BASKET_FRONT to BASKET_TOP, bottomFront to BASKET_FLOOR, bottomRear to BASKET_FLOOR)
        corners.zipWithNext().plus(corners.last() to corners.first()).forEach { (from, to) ->
            drawLine(CART_CHROME, p(from.first, from.second), p(to.first, to.second), 0.35f * unit, StrokeCap.Round)
        }
        val columns = 9
        repeat(columns) { index ->
            val share = (index + 1f) / (columns + 1f)
            val top = BASKET_REAR + (BASKET_FRONT - BASKET_REAR) * share
            val bottom = bottomRear + (bottomFront - bottomRear) * share
            drawLine(CART_CHROME, p(top, BASKET_TOP), p(bottom, BASKET_FLOOR), wire)
        }
        var row = BASKET_FLOOR + 1.5f
        while (row < BASKET_TOP) {
            val share = (row - BASKET_FLOOR) / (BASKET_TOP - BASKET_FLOOR)
            drawLine(CART_CHROME, p(bottomRear + (BASKET_REAR - bottomRear) * share, row), p(bottomFront + (BASKET_FRONT - bottomFront) * share, row), wire)
            row += 1.5f
        }

        // Handle bar with the red grip, sloping back from the basket
        drawLine(CART_CHROME, p(BASKET_REAR, BASKET_TOP), p(0.3f, BASKET_TOP + 1.6f), 0.35f * unit, StrokeCap.Round)
        drawLine(CART_RED, p(-0.2f, BASKET_TOP + 1.9f), p(0.8f, BASKET_TOP + 1.3f), 0.8f * unit, StrokeCap.Round)

        // Chassis with the lower tray, legs down to the casters
        drawLine(CART_CHROME, p(bottomRear, BASKET_FLOOR), p(2.6f, 2.4f), 0.35f * unit, StrokeCap.Round)
        drawLine(CART_CHROME, p(2.6f, 2.4f), p(12.6f, 2.4f), 0.3f * unit)
        drawLine(CART_CHROME, p(bottomFront, BASKET_FLOOR), p(12.6f, 2.4f), 0.35f * unit, StrokeCap.Round)
        listOf(3f, 12f).forEach { casterX ->
            drawLine(CART_CHROME, p(casterX, 2.4f), p(casterX, CART_WHEEL_RADIUS), 0.3f * unit)
            drawCaster(p(casterX, CART_WHEEL_RADIUS), unit, cart.wheelPhase)
        }
    }
}

/** A small caster: black wheel with a gray hub and a spoke showing it turn. */
private fun DrawScope.drawCaster(center: Offset, unit: Float, phase: Float) {
    val radius = CART_WHEEL_RADIUS * unit
    drawCircle(CART_BLACK, radius = radius, center = center)
    drawCircle(CART_CHROME, radius = radius * 0.4f, center = center)
    drawLine(CART_BLACK, center, center + Offset(cos(phase), sin(phase)) * (radius * 0.4f), 0.1f * unit)
    drawCircle(CART_CHROME, radius = radius, center = center, style = Stroke(width = 0.08f * unit))
    drawLine(CART_CHROME, center, center + Offset(cos(phase + PI.toFloat()), sin(phase + PI.toFloat())) * (radius * 0.4f), 0.08f * unit)
}
