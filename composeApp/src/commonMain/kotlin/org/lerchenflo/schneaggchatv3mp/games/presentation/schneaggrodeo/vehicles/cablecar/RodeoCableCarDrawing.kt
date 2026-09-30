package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cablecar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.CABLE_COLOR
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi

/** The gondola, explicitly requested in a fixed red like the ones in the Alps. */
private val GONDOLA_RED = Color(0xFFC62828)

/**
 * The gondola: an open cabin the horse stands in, hanging from its cable. [x] is its left edge,
 * [floor] the height of its floor. With [levelCable] the cable is drawn level across the picture
 * (before and after the mountain, whose own cable RodeoLandscape draws).
 */
@Immutable
data class RodeoCableCarUi(
    val x: Float,
    val floor: Float,
    val levelCable: Boolean,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        val unit = context.unit
        fun p(dx: Float, y: Float) = context.p(x + dx, floor + y)
        when (layer) {
            RodeoLayer.BACK -> if (levelCable) {
                val cableY = context.p(0f, floor + CABIN_HANG).y
                drawLine(CABLE_COLOR, Offset(0f, cableY), Offset(size.width, cableY), 0.4f * unit)
            }
            // Back wall behind the horse, floor under its hooves
            RodeoLayer.BODY -> {
                drawRect(context.colors.surfaceVariant.copy(alpha = 0.35f), p(0f, CABIN_HEIGHT), Size(CABIN_WIDTH * unit, CABIN_HEIGHT * unit))
                drawRect(GONDOLA_RED, p(0f, 0f), Size(CABIN_WIDTH * unit, 1.5f * unit))
            }
            // Low front panel, corner posts, roof and the hanger up to the cable
            RodeoLayer.FRONT -> {
                drawRect(GONDOLA_RED, p(0f, 7f), Size(CABIN_WIDTH * unit, 7f * unit))
                drawLine(context.colors.surface.copy(alpha = 0.6f), p(1f, 4f), p(CABIN_WIDTH - 1f, 4f), 0.4f * unit)
                listOf(0.8f, CABIN_WIDTH - 0.8f).forEach { postX ->
                    drawLine(GONDOLA_RED, p(postX, 0f), p(postX, CABIN_HEIGHT), 1.4f * unit)
                }
                drawRoundRect(GONDOLA_RED, p(-1f, CABIN_HEIGHT + 2.5f), Size((CABIN_WIDTH + 2f) * unit, 2.5f * unit), CornerRadius(1f * unit))
                val hangerX = CABIN_WIDTH / 2f
                drawLine(CABLE_COLOR, p(hangerX, CABIN_HEIGHT + 2.5f), p(hangerX, CABIN_HANG), 0.8f * unit)
                drawLine(CABLE_COLOR, p(hangerX - 3f, CABIN_HANG), p(hangerX + 3f, CABIN_HANG), 1.2f * unit)
                drawCircle(CABLE_COLOR, radius = 1f * unit, center = p(hangerX, CABIN_HANG))
            }
        }
    }
}
