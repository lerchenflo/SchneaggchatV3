package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.oiltanker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi

// The tanker's fixed colors: black hull with a red bottom, white bridge, deck pipes and funnel
private val HULL_BLACK = Color(0xFF263238)
private val HULL_RED = Color(0xFFB71C1C)
private val DECK_RED = Color(0xFF8D3A2E)
private val BRIDGE_WHITE = Color(0xFFECEFF1)
private val WINDOW = Color(0xFF455A64)
private val PIPE = Color(0xFF9E9E9E)
private val SMOKE = Color(0xFF616161)

/** The oil tanker, facing right: [x] is its stern. */
@Immutable
data class RodeoOilTankerUi(
    val x: Float,
    /** Seconds, driving the smoke. */
    val smoke: Float,
    /** 0..1 while the horn blares, else null. */
    val horn: Float?,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            RodeoLayer.BACK -> Unit
            RodeoLayer.BODY -> drawTanker(this@RodeoOilTankerUi, context)
            RodeoLayer.FRONT -> drawFunnelSmoke(this@RodeoOilTankerUi, context)
        }
    }
}

/** Hull with a red bottom and a raked bow, deck with pipes and railing posts, bridge and funnel. */
private fun DrawScope.drawTanker(tanker: RodeoOilTankerUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(tanker.x + x, y)

    // Hull: black above, red antifouling below the waterline
    drawPath(polygonPath(::p, 0f to TANKER_DECK, TANKER_LENGTH + 3f to TANKER_DECK + 1f, TANKER_LENGTH - 3f to 0.5f, 3f to 0.5f), HULL_BLACK)
    drawPath(polygonPath(::p, 2.5f to 4f, TANKER_LENGTH - 1.8f to 4f, TANKER_LENGTH - 3f to 0.5f, 3f to 0.5f), HULL_RED)
    drawLine(BRIDGE_WHITE, p(1f, TANKER_DECK - 1.2f), p(TANKER_LENGTH + 2f, TANKER_DECK - 0.2f), 0.3f * unit)
    // Deck: red deck, pipes along it, a manifold and railing posts
    drawRect(DECK_RED, p(0f, TANKER_DECK + 0.6f), Size((TANKER_LENGTH + 2f) * unit, 0.6f * unit))
    drawLine(PIPE, p(BRIDGE_X + BRIDGE_WIDTH, TANKER_DECK + 1.4f), p(TANKER_LENGTH - 4f, TANKER_DECK + 1.4f), 0.4f * unit)
    drawLine(PIPE, p(BRIDGE_X + BRIDGE_WIDTH, TANKER_DECK + 2.2f), p(TANKER_LENGTH - 8f, TANKER_DECK + 2.2f), 0.3f * unit)
    drawRect(PIPE, p(55f, TANKER_DECK + 3.4f), Size(3f * unit, 2.8f * unit))
    var post = 1f
    while (post < TANKER_LENGTH) {
        drawLine(PIPE, p(post, TANKER_DECK + 0.6f), p(post, TANKER_DECK + 2.6f), 0.15f * unit)
        post += 4f
    }
    drawLine(PIPE, p(0f, TANKER_DECK + 2.6f), p(TANKER_LENGTH, TANKER_DECK + 2.6f), 0.15f * unit)

    // Bridge with rows of windows, and the funnel on top
    drawRect(BRIDGE_WHITE, p(BRIDGE_X, BRIDGE_TOP), Size(BRIDGE_WIDTH * unit, (BRIDGE_TOP - TANKER_DECK) * unit))
    listOf(BRIDGE_TOP - 2f, BRIDGE_TOP - 6f, BRIDGE_TOP - 10f).forEach { rowY ->
        var windowX = BRIDGE_X + 1f
        while (windowX < BRIDGE_X + BRIDGE_WIDTH - 1f) {
            drawRect(WINDOW, p(windowX, rowY), Size(1.4f * unit, 1.2f * unit))
            windowX += 2.4f
        }
    }
    drawRect(HULL_BLACK, p(BRIDGE_X - 0.6f, BRIDGE_TOP + 0.6f), Size((BRIDGE_WIDTH + 1.2f) * unit, 0.6f * unit))
    drawRect(HULL_BLACK, p(FUNNEL_X, BRIDGE_TOP + 5f), Size(4f * unit, 5f * unit))
    drawRect(HULL_RED, p(FUNNEL_X, BRIDGE_TOP + 3.6f), Size(4f * unit, 1.2f * unit))

    // The horn blaring: sound waves off the funnel
    tanker.horn?.let { horn ->
        repeat(3) { index ->
            val radius = (2f + 4f * horn + index * 1.6f) * unit
            drawArc(
                BRIDGE_WHITE.copy(alpha = (1f - horn) * 0.8f),
                startAngle = -60f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = p(FUNNEL_X + 4f, BRIDGE_TOP + 3f) - Offset(radius, radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 0.3f * unit)
            )
        }
    }
}

/** Puffs of smoke pouring from the funnel and drifting back. */
private fun DrawScope.drawFunnelSmoke(tanker: RodeoOilTankerUi, context: RodeoDrawContext) {
    val unit = context.unit
    repeat(6) { index ->
        val cycle = (tanker.smoke * 0.5f + index / 6f) % 1f
        drawCircle(
            SMOKE.copy(alpha = 0.5f * (1f - cycle)),
            radius = (1.2f + 3f * cycle) * unit,
            center = context.p(tanker.x + FUNNEL_X + 2f - 12f * cycle, BRIDGE_TOP + 5.5f + 8f * cycle)
        )
    }
}
