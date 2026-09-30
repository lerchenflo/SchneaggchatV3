package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.balloon

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoFootprint
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.onFootprint
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawSeatedCowboy
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.sin

// The balloon's fixed colors: a striped envelope, a wicker basket and the burner flame
private val ENVELOPE_STRIPES = listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF1E88E5), Color(0xFFFDD835))
private val WICKER = Color(0xFFA1887F)
private val WICKER_DARK = Color(0xFF6D4C41)
private val FLAME = Color(0xFFFF9800)
private val FLAME_CORE = Color(0xFFFFEB3B)

/** The hot-air balloon: [x] is the basket's left edge, [y] its floor above the ground. */
@Immutable
data class RodeoBalloonUi(
    val x: Float,
    val y: Float,
    /** Seconds, for the gentle swaying and the flame's flicker. */
    val sway: Float,
    /** The burner is firing. */
    val burning: Boolean,
    /** The cowboy stands in the basket. */
    val hasPilot: Boolean,
    /** Rope ladder hanging down while it drifts by and while the cowboy climbs up. */
    val ladderDown: Boolean,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override val footprint get() = RodeoFootprint(x, x + BASKET_WIDTH, tilts = false)

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        if (layer == RodeoLayer.FRONT) onFootprint(context, footprint) { drawBalloon(this@RodeoBalloonUi, it) }
    }
}

/** Envelope with vertical stripes, ropes, burner with its flame, the wicker basket and the ladder. */
private fun DrawScope.drawBalloon(balloon: RodeoBalloonUi, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    fun p(x: Float, y: Float) = context.p(balloon.x + x, balloon.y + y)
    val centerX = BASKET_WIDTH / 2f
    val tilt = 2.5f * sin(balloon.sway * 1.3f)

    rotate(tilt, pivot = p(centerX, BASKET_HEIGHT)) {
        // Envelope: an oval with vertical stripes, narrowing into a skirt above the burner
        val envelopeRect = Rect(
            topLeft = p(centerX - ENVELOPE_RADIUS_X, ENVELOPE_CENTER_Y + ENVELOPE_RADIUS_Y),
            bottomRight = p(centerX + ENVELOPE_RADIUS_X, ENVELOPE_CENTER_Y - ENVELOPE_RADIUS_Y),
        )
        val envelope = Path().apply {
            addOval(envelopeRect)
            // The skirt narrowing down towards the burner
            addPath(
                polygonPath(
                    ::p,
                    centerX - 6.5f to ENVELOPE_CENTER_Y - 6f,
                    centerX - 2.4f to BASKET_HEIGHT + 6f,
                    centerX + 2.4f to BASKET_HEIGHT + 6f,
                    centerX + 6.5f to ENVELOPE_CENTER_Y - 6f,
                )
            )
        }
        clipPath(envelope) {
            val stripeWidth = 2f * ENVELOPE_RADIUS_X / 6f
            repeat(6) { index ->
                drawRect(
                    ENVELOPE_STRIPES[index % ENVELOPE_STRIPES.size],
                    topLeft = p(centerX - ENVELOPE_RADIUS_X + index * stripeWidth, ENVELOPE_CENTER_Y + ENVELOPE_RADIUS_Y),
                    size = Size(stripeWidth * unit, (ENVELOPE_CENTER_Y + ENVELOPE_RADIUS_Y) * unit),
                )
            }
            // Shading on the right side
            drawOval(Color.Black.copy(alpha = 0.12f), topLeft = p(centerX + 2f, ENVELOPE_CENTER_Y + ENVELOPE_RADIUS_Y), size = Size(8f * unit, 22f * unit))
        }

        // Ropes down to the basket
        listOf(0.2f to -2.4f, BASKET_WIDTH - 0.2f to 2.4f).forEach { (basketX, skirtOffset) ->
            drawLine(WICKER_DARK, p(basketX, BASKET_HEIGHT), p(centerX + skirtOffset, BASKET_HEIGHT + 6f), 0.15f * unit)
        }
        // Burner and its flame
        drawRect(colors.outline, p(centerX - 0.8f, BASKET_HEIGHT + 4.2f), Size(1.6f * unit, 0.9f * unit))
        if (balloon.burning) {
            val flicker = 1f + 0.25f * sin(balloon.sway * 40f)
            drawOval(FLAME, p(centerX - 0.9f, BASKET_HEIGHT + 4.4f + 3.4f * flicker), Size(1.8f * unit, 3.4f * flicker * unit))
            drawOval(FLAME_CORE, p(centerX - 0.45f, BASKET_HEIGHT + 4.4f + 1.9f * flicker), Size(0.9f * unit, 1.9f * flicker * unit))
        }

        // The cowboy peeking out of the basket
        if (balloon.hasPilot) drawSeatedCowboy(::p, hipX = centerX - 0.8f, hipY = BASKET_HEIGHT - 2.4f, unit = unit, colors = colors)

        // Wicker basket with weave lines
        drawRoundRect(WICKER, p(0f, BASKET_HEIGHT), Size(BASKET_WIDTH * unit, BASKET_HEIGHT * unit), CornerRadius(0.4f * unit))
        listOf(1.2f, 2.4f).forEach { weave -> drawLine(WICKER_DARK, p(0.2f, weave), p(BASKET_WIDTH - 0.2f, weave), 0.15f * unit) }
        repeat(4) { index -> drawLine(WICKER_DARK, p(1.2f + index * 1.2f, 0.2f), p(1.2f + index * 1.2f, BASKET_HEIGHT - 0.2f), 0.12f * unit) }
        drawRoundRect(WICKER_DARK, p(-0.2f, BASKET_HEIGHT + 0.3f), Size((BASKET_WIDTH + 0.4f) * unit, 0.6f * unit), CornerRadius(0.3f * unit))

        // Rope ladder
        if (balloon.ladderDown) {
            val ladderX = centerX
            drawLine(WICKER_DARK, p(ladderX - 0.8f, 0f), p(ladderX - 0.8f, -BALLOON_LADDER_LENGTH), 0.15f * unit, StrokeCap.Round)
            drawLine(WICKER_DARK, p(ladderX + 0.8f, 0f), p(ladderX + 0.8f, -BALLOON_LADDER_LENGTH), 0.15f * unit, StrokeCap.Round)
            var rung = -1.5f
            while (rung > -BALLOON_LADDER_LENGTH) {
                drawLine(WICKER_DARK, p(ladderX - 0.8f, rung), p(ladderX + 0.8f, rung), 0.2f * unit)
                rung -= 1.5f
            }
        }
    }
}

