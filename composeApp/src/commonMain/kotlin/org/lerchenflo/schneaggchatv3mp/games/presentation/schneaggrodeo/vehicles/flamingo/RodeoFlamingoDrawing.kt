package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.flamingo

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.sin

// The flamingo float's fixed colors: pink vinyl with a shine, a white and black beak
private val FLAMINGO_PINK = Color(0xFFF48FB1)
private val FLAMINGO_PINK_DARK = Color(0xFFD81B60)
private val FLAMINGO_SHINE = Color(0xFFFCE4EC)
private val BEAK_WHITE = Color(0xFFFAFAFA)
private val BEAK_BLACK = Color(0xFF212121)

/** The flamingo float, facing right: [x] is its tail, [lift] how high it bounces. */
@Immutable
data class RodeoFlamingoUi(
    val x: Float,
    val lift: Float,
    /** Seconds, for bobbing on the water. */
    val bob: Float,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        if (layer == RodeoLayer.BODY) drawFlamingo(this@RodeoFlamingoUi, context)
    }
}

/** Plump inflatable body with a pointed tail, the long curved neck, head and beak, and a shine. */
private fun DrawScope.drawFlamingo(float: RodeoFlamingoUi, context: RodeoDrawContext) {
    val unit = context.unit
    val bobbing = if (float.lift > 0f) 0f else 0.4f * sin(float.bob * 2.5f)
    fun p(x: Float, y: Float) = context.p(float.x + x, float.lift + bobbing + y)

    // Tail feathers and body
    drawPath(polygonPath(::p, 3f to 6.5f, -1.5f to 9.5f, 0.5f to 5f), FLAMINGO_PINK_DARK)
    drawOval(FLAMINGO_PINK, p(1.5f, FLAMINGO_BACK + 1f), Size(18f * unit, 7f * unit))
    drawOval(FLAMINGO_SHINE, p(4f, FLAMINGO_BACK), Size(7f * unit, 1.4f * unit))
    drawLine(FLAMINGO_PINK_DARK, p(3f, 3.4f), p(18f, 3.4f), 0.2f * unit)

    // Neck: an S-curve from the front of the body up to the head
    val neck = Path().apply {
        val start = p(17.5f, 6f)
        val control1 = p(24f, 9f)
        val control2 = p(15f, 15f)
        val end = p(19.5f, 20f)
        moveTo(start.x, start.y)
        cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
    }
    drawPath(neck, FLAMINGO_PINK, style = Stroke(width = 2.4f * unit, cap = StrokeCap.Round))
    drawPath(neck, FLAMINGO_SHINE.copy(alpha = 0.6f), style = Stroke(width = 0.5f * unit, cap = StrokeCap.Round))

    // Head, eye and the bent beak
    val head = p(20f, 20.4f)
    drawCircle(FLAMINGO_PINK, radius = 1.9f * unit, center = head)
    drawCircle(BEAK_BLACK, radius = 0.3f * unit, center = head + Offset(0.5f * unit, -0.4f * unit))
    drawPath(polygonPath(::p, 21.5f to 21.2f, 24.5f to 20.2f, 24f to 18.8f, 21.4f to 19.6f), BEAK_WHITE)
    drawPath(polygonPath(::p, 23.2f to 20.5f, 24.5f to 20.2f, 24f to 18.8f, 23f to 19.2f), BEAK_BLACK)
}
