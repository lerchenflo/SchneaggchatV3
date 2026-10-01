package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.minecart

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.LEVEL_GOLD_COLOR
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.cloudNoise
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWheel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.cos
import kotlin.math.sin

// The mine cart's fixed colors: rusty iron, dark rivets, wooden sleepers, steel rails and sparks
private val CART_IRON = Color(0xFF8D5524)
private val CART_IRON_DARK = Color(0xFF4E342E)
private val RAIL_STEEL = Color(0xFF90A4AE)
private val SLEEPER_WOOD = Color(0xFF5D4037)
private val SPARK = Color(0xFFFFC107)

private const val SLEEPER_SPACING = 4f

/** The mine cart, facing right: [x] is its rear; the rails run along the whole track while it's around. */
@Immutable
data class RodeoMineCartUi(
    val x: Float,
    val wheelPhase: Float,
    /** How far the rails scrolled, for the sleepers. */
    val railOffset: Float,
    /** Drives the sparks while racing, else null. */
    val sparks: Float?,
    override val lassoHint: RodeoLassoHintUi?,
    /** Rails across the whole picture (in the cave); off on the surface. */
    val railsAcross: Boolean = true,
    /** Heaped full of gold (the cart out of the gold mine on the surface). */
    val gold: Boolean = false,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            RodeoLayer.BACK -> if (railsAcross) drawRails(railOffset, context)
            RodeoLayer.BODY -> drawCartInside(this@RodeoMineCartUi, context)
            RodeoLayer.FRONT -> {
                if (gold) drawGoldHeap(this@RodeoMineCartUi, context)
                drawCartSide(this@RodeoMineCartUi, context)
            }
        }
    }
}

/** Wooden sleepers and two steel rails across the whole picture. */
private fun DrawScope.drawRails(offset: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val width = size.width / unit
    var sleeper = -(offset % SLEEPER_SPACING)
    while (sleeper < width) {
        drawRect(SLEEPER_WOOD, context.p(sleeper, 0.6f), Size(1.4f * unit, 0.8f * unit))
        sleeper += SLEEPER_SPACING
    }
    drawLine(RAIL_STEEL, context.p(0f, 0.5f), context.p(width, 0.5f), 0.35f * unit)
}

private fun DrawScope.drawCartInside(cart: RodeoMineCartUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(cart.x + x, y)
    drawPath(polygonPath(::p, 1f to CART_RIM, CART_LENGTH - 1f to CART_RIM, CART_LENGTH - 2.5f to CART_FLOOR, 2.5f to CART_FLOOR), CART_IRON_DARK)
}

/** Gold nuggets heaped up over the cart's rim. */
private fun DrawScope.drawGoldHeap(cart: RodeoMineCartUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(cart.x + x, y)
    repeat(12) { index ->
        val row = index / 6
        val nuggetX = 3f + (index % 6) * 3.2f + row * 1.6f
        val nuggetY = CART_RIM + 0.3f + row * 1.4f - 0.8f * kotlin.math.abs((index % 6) - 2.5f) / 2.5f
        drawCircle(LEVEL_GOLD_COLOR, radius = (1.1f + 0.3f * cloudNoise(index, 71)) * unit, center = p(nuggetX, nuggetY))
    }
}

/** Near side of the cart with rivets and rust streaks, its wheels on the rail, and sparks. */
private fun DrawScope.drawCartSide(cart: RodeoMineCartUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(cart.x + x, y)
    drawPath(polygonPath(::p, 0f to CART_RIM, CART_LENGTH to CART_RIM, CART_LENGTH - 2f to 2.6f, 2f to 2.6f), CART_IRON)
    drawLine(CART_IRON_DARK, p(0f, CART_RIM), p(CART_LENGTH, CART_RIM), 0.6f * unit)
    // Rivets along the rim and the bottom
    var rivet = 1.5f
    while (rivet < CART_LENGTH - 1f) {
        drawCircle(CART_IRON_DARK, radius = 0.25f * unit, center = p(rivet, CART_RIM - 1f))
        drawCircle(CART_IRON_DARK, radius = 0.25f * unit, center = p(rivet + 0.6f, 3.4f))
        rivet += 2.5f
    }
    // Rust streaks
    repeat(4) { index ->
        val streakX = 3f + 16f * cloudNoise(index, 41)
        drawLine(CART_IRON_DARK.copy(alpha = 0.5f), p(streakX, CART_RIM - 1.5f), p(streakX + 0.3f, CART_RIM - 3.5f - 2f * cloudNoise(index, 42)), 0.25f * unit, StrokeCap.Round)
    }
    listOf(5f, CART_LENGTH - 5f).forEach { wheelX ->
        drawWheel(p(wheelX, CART_WHEEL_RADIUS), CART_WHEEL_RADIUS * unit, cart.wheelPhase, CART_IRON_DARK, RAIL_STEEL, CART_IRON)
    }
    cart.sparks?.let { phase ->
        listOf(5f, CART_LENGTH - 5f).forEachIndexed { wheel, wheelX ->
            repeat(4) { index ->
                val cycle = (phase * 6f + index * 0.25f + wheel * 0.5f) % 1f
                val angle = 2.5f + 0.6f * cloudNoise(index, 43 + wheel)
                val origin = p(wheelX - 0.5f, 0.3f)
                val length = (1.5f + 2f * cloudNoise(index, 45)) * cycle * unit
                drawLine(SPARK.copy(alpha = 1f - cycle), origin, origin + Offset(cos(angle) * length * 2f, -sin(angle) * length), 0.25f * unit, StrokeCap.Round)
            }
        }
    }
}

