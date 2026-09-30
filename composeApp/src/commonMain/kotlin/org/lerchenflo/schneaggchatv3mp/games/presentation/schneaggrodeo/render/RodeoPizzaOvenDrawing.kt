package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPizzaOvenUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPizzaUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.OVEN_MOUTH_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.OVEN_WIDTH
import kotlin.math.cos
import kotlin.math.sin

// The pizza oven by the roadside (see engine/RodeoPizzaOvens) and the pizza the lasso pulls out of it.

private const val OVEN_BASE_HEIGHT = 4f
private const val DOME_TOP = 14f

/**
 * A wood-fired oven: stone base, brick dome with a chimney, the fire glowing in its arched mouth,
 * a pizza on the oven floor until the lasso took it, and a stack of logs next to it.
 */
internal fun DrawScope.drawPizzaOven(oven: RodeoPizzaOvenUi, context: RodeoDrawContext, time: Float) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(oven.x + dx, y)

    // Stone base with joints
    drawRect(OVEN_STONE, p(0f, OVEN_BASE_HEIGHT), Size(OVEN_WIDTH * unit, OVEN_BASE_HEIGHT * unit))
    drawLine(OVEN_BRICK_DARK.copy(alpha = 0.4f), p(0f, OVEN_BASE_HEIGHT / 2f), p(OVEN_WIDTH, OVEN_BASE_HEIGHT / 2f), 0.2f * unit)
    listOf(3f, 8f, 13f).forEach { joint -> drawLine(OVEN_BRICK_DARK.copy(alpha = 0.4f), p(joint, OVEN_BASE_HEIGHT), p(joint, OVEN_BASE_HEIGHT / 2f), 0.2f * unit) }

    // Chimney with a little smoke, behind the dome
    drawRect(OVEN_BRICK_DARK, p(11f, DOME_TOP + 2f), Size(1.8f * unit, 4f * unit))
    repeat(3) { index ->
        val cycle = (time * 0.6f + index / 3f) % 1f
        drawCircle(context.colors.outline.copy(alpha = 0.35f * (1f - cycle)), radius = (0.8f + 1.5f * cycle) * unit, center = p(11.9f - 2f * cycle, DOME_TOP + 3f + 6f * cycle))
    }

    // Brick dome with rows of bricks
    val domeTopLeft = p(0.5f, DOME_TOP)
    val domeSize = Size((OVEN_WIDTH - 1f) * unit, (DOME_TOP - OVEN_BASE_HEIGHT) * 2f * unit)
    drawArc(OVEN_BRICK, 180f, 180f, useCenter = true, topLeft = domeTopLeft, size = domeSize)
    listOf(0.35f, 0.65f).forEach { row ->
        val inset = domeSize.width * (1f - row) * 0.02f
        drawArc(
            OVEN_BRICK_DARK.copy(alpha = 0.5f), 185f, 170f, useCenter = false,
            topLeft = domeTopLeft + Offset(domeSize.width * (1f - row) / 2f + inset, domeSize.height * (1f - row) / 2f),
            size = Size(domeSize.width * row, domeSize.height * row),
            style = Stroke(width = 0.2f * unit)
        )
    }

    // Arched mouth with the fire inside
    val mouthLeft = OVEN_MOUTH_X - 3.2f
    drawArc(context.colors.scrim, 180f, 180f, useCenter = true, topLeft = p(mouthLeft, 9.4f), size = Size(6.4f * unit, 6.4f * unit))
    drawRect(context.colors.scrim, p(mouthLeft, 6.2f), Size(6.4f * unit, (6.2f - OVEN_BASE_HEIGHT) * unit))
    repeat(4) { index ->
        val flicker = 0.7f + 0.3f * sin(time * 12f + index * 1.7f)
        val flameX = mouthLeft + 1f + index * 1.4f
        drawLine(FIRE_COLOR, p(flameX, OVEN_BASE_HEIGHT + 0.3f), p(flameX + 0.2f, OVEN_BASE_HEIGHT + 0.3f + 3f * flicker), 0.9f * unit, StrokeCap.Round)
        drawLine(FIRE_CORE_COLOR, p(flameX, OVEN_BASE_HEIGHT + 0.3f), p(flameX + 0.1f, OVEN_BASE_HEIGHT + 0.3f + 1.6f * flicker), 0.4f * unit, StrokeCap.Round)
    }
    drawArc(OVEN_BRICK_DARK, 180f, 180f, useCenter = false, topLeft = p(mouthLeft - 0.4f, 9.8f), size = Size(7.2f * unit, 7.2f * unit), style = Stroke(width = 0.6f * unit))

    // The pizza on the oven floor, seen from the side
    if (oven.hasPizza) {
        drawOval(PIZZA_CRUST, p(OVEN_MOUTH_X - 2.6f, OVEN_BASE_HEIGHT + 1.3f), Size(5.2f * unit, 1.3f * unit))
        drawOval(PIZZA_SAUCE, p(OVEN_MOUTH_X - 2f, OVEN_BASE_HEIGHT + 1.25f), Size(4f * unit, 0.8f * unit))
        drawCircle(PIZZA_CHEESE, radius = 0.35f * unit, center = p(OVEN_MOUTH_X - 0.8f, OVEN_BASE_HEIGHT + 0.85f))
        drawCircle(PIZZA_SALAMI, radius = 0.35f * unit, center = p(OVEN_MOUTH_X + 0.8f, OVEN_BASE_HEIGHT + 0.85f))
    }

    // Stack of logs next to it
    listOf(OVEN_WIDTH + 1.2f to 0.8f, OVEN_WIDTH + 2.8f to 0.8f, OVEN_WIDTH + 2f to 2.2f).forEach { (logX, logY) ->
        drawCircle(TREE_TRUNK, radius = 0.8f * unit, center = p(logX, logY))
        drawCircle(OVEN_BRICK_DARK.copy(alpha = 0.5f), radius = 0.35f * unit, center = p(logX, logY))
    }
}

/** A whole pizza seen from above, dangling in the lasso: crust, sauce, cheese, salami and basil. */
internal fun DrawScope.drawPizza(pizza: RodeoPizzaUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(pizza.x, pizza.y)
    drawCircle(PIZZA_CRUST, radius = 2.4f * unit, center = center)
    drawCircle(PIZZA_SAUCE, radius = 1.9f * unit, center = center)
    repeat(5) { index ->
        val angle = index * 1.26f
        drawCircle(PIZZA_CHEESE, radius = 0.45f * unit, center = center + Offset(cos(angle + 0.6f), sin(angle + 0.6f)) * (1.1f * unit))
        drawCircle(PIZZA_SALAMI, radius = 0.4f * unit, center = center + Offset(cos(angle), sin(angle)) * (0.9f * unit))
    }
    drawCircle(PIZZA_BASIL, radius = 0.3f * unit, center = center)
}
