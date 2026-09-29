package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.candybus

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.smoothstep
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWheel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Explicitly requested fixed colors: the gray bus and its sign sprayed on in many colors
private val BUS_GRAY = Color(0xFF9E9E9E)
private val BUS_GRAY_DARK = Color(0xFF757575)
private val BUS_TRIM = Color(0xFF424242)
private val CANDY_COLORS = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFEC407A),
)

/** Where the sign sits on the side: its center, and the space it may take. */
private const val SIGN_CENTER_X = 16f
private const val SIGN_CENTER_Y = 13f
private const val SIGN_WIDTH = 27f
private const val SIGN_HEIGHT = 14f
private const val SIGN_FONT_SIZE = 5.5f
private const val SIGN_TILT_DEGREES = -4f

/** The free candy bus, facing right: [x] is its rear bumper. */
@Immutable
data class RodeoCandyBusUi(
    val x: Float,
    val wheelPhase: Float,
    /** 0..1 how far the side door is slid open. */
    val doorOpen: Float,
    /** Waiting by the roadside, or on his way in through the door. */
    val stanislaus: List<RodeoStanislausUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            RodeoLayer.BACK -> Unit
            RodeoLayer.BODY -> drawCandyBus(this@RodeoCandyBusUi, context)
            // By the roadside, in front of the bus
            RodeoLayer.FRONT -> stanislaus.forEach { drawStanislaus(it, this@RodeoCandyBusUi, context) }
        }
    }
}

/** [x] is his center on the ground; [pull] 0..1 on his way in through the door. */
@Immutable
data class RodeoStanislausUi(val x: Float, val pull: Float)

/**
 * Stanislaus standing by the road. Pulled in, he is lifted up to the door's floor, shrinks into the
 * dark opening and fades away.
 */
private fun DrawScope.drawStanislaus(man: RodeoStanislausUi, bus: RodeoCandyBusUi, context: RodeoDrawContext) {
    val image = context.assets?.stanislaus ?: return
    val unit = context.unit
    val pull = smoothstep(man.pull)
    val doorX = bus.x + DOOR_LEFT + DOOR_WIDTH / 2f
    val footX = man.x + (doorX - man.x) * pull
    val footY = DOOR_FLOOR * pull + 2f * sin(PI.toFloat() * man.pull)
    val scale = 1f - 0.35f * pull
    val alpha = 1f - smoothstep(((man.pull - 0.5f) * 2f).coerceIn(0f, 1f))
    val height = STANISLAUS_HEIGHT * scale * unit
    val width = height * image.width / image.height
    val foot = context.p(footX, footY)
    drawImage(
        image = image,
        dstOffset = IntOffset((foot.x - width / 2f).roundToInt(), (foot.y - height).roundToInt()),
        dstSize = IntSize(width.roundToInt(), height.roundToInt()),
        alpha = alpha,
    )
}

/**
 * The bus on a grid with y up from the ground and x from its rear bumper: a tall gray panel van with
 * no windows in the back, "FREE CANDY" sprayed on its side in many colors, a sliding side door, and
 * a sloped windshield with the driver's window in front.
 */
private fun DrawScope.drawCandyBus(bus: RodeoCandyBusUi, context: RodeoDrawContext) {
    val unit = context.unit
    val lineColor = context.colors.onSurface
    fun p(x: Float, y: Float) = context.p(bus.x + x, y)
    fun polygon(vararg points: Pair<Float, Float>) = polygonPath(::p, *points)

    // Wheel arches behind the wheels
    listOf(9f, 46f).forEach { wheelX -> drawCircle(BUS_TRIM, radius = 4f * unit, center = p(wheelX, 4f)) }

    // Body with its outline
    val body = polygon(
        0f to BUS_BOTTOM, 0f to BUS_ROOF - 1.5f, 1.5f to BUS_ROOF, 45f to BUS_ROOF, 47f to BUS_ROOF - 0.5f,
        51f to 14f, 55.3f to 13f, 56f to 11f, 56f to BUS_BOTTOM,
    )
    drawPath(body, BUS_GRAY)
    drawPath(body, lineColor, style = Stroke(width = 0.35f * unit))
    // Dirt along the rocker panel and a dent
    drawRect(BUS_GRAY_DARK, p(0.2f, BUS_BOTTOM + 1.2f), Size(55.6f * unit, 1.2f * unit))
    drawOval(BUS_GRAY_DARK, p(3f, 19f), Size(3f * unit, 1.4f * unit))

    drawCandySign(context, bus.x)

    // Windshield and the driver's window - the only windows
    val glass = context.colors.primaryContainer
    drawPath(polygon(47.2f to 20.8f, 50.4f to 14.4f, 47.2f to 14.4f), glass)
    drawRoundRect(glass, p(41.8f, 20.5f), Size(4.4f * unit, 5.8f * unit), CornerRadius(0.5f * unit))

    // Sliding side door: a dark opening once it slides back along its rail
    val doorHeight = (DOOR_TOP - DOOR_FLOOR) * unit
    if (bus.doorOpen > 0f) {
        drawRect(context.colors.scrim, p(DOOR_LEFT, DOOR_TOP), Size(DOOR_WIDTH * unit, doorHeight))
    }
    drawLine(BUS_TRIM, p(DOOR_LEFT - DOOR_WIDTH, DOOR_TOP + 0.6f), p(DOOR_LEFT + DOOR_WIDTH, DOOR_TOP + 0.6f), 0.35f * unit)
    val doorLeft = DOOR_LEFT - bus.doorOpen * DOOR_WIDTH * 0.95f
    drawRect(BUS_GRAY, p(doorLeft, DOOR_TOP), Size(DOOR_WIDTH * unit, doorHeight))
    drawRect(lineColor, p(doorLeft, DOOR_TOP), Size(DOOR_WIDTH * unit, doorHeight), style = Stroke(width = 0.3f * unit))
    drawLine(lineColor, p(doorLeft + DOOR_WIDTH - 1.2f, 12.5f), p(doorLeft + DOOR_WIDTH - 1.2f, 10.5f), 0.45f * unit, StrokeCap.Round)

    // Bumpers, headlight, tail light
    drawRect(BUS_TRIM, p(-0.6f, BUS_BOTTOM + 2f), Size(2f * unit, 2f * unit))
    drawRect(BUS_TRIM, p(54.8f, BUS_BOTTOM + 2f), Size(1.8f * unit, 2f * unit))
    drawCircle(context.colors.surfaceBright, radius = 0.8f * unit, center = p(55f, 9.8f))
    drawRect(BUS_TRIM, p(0.3f, 15f), Size(0.8f * unit, 2.5f * unit))

    drawWheel(p(9f, BUS_BOTTOM), 3.5f * unit, bus.wheelPhase, lineColor, context.colors.surfaceContainer, BUS_GRAY)
    drawWheel(p(46f, BUS_BOTTOM), 3.5f * unit, bus.wheelPhase, lineColor, context.colors.surfaceContainer, BUS_GRAY)
}

/** "FREE CANDY" sprayed on the side, a bit crooked, every letter in another color, one word per line. */
private fun DrawScope.drawCandySign(context: RodeoDrawContext, busX: Float) {
    val assets = context.assets ?: return
    val unit = context.unit
    val text = assets.candySign.replace(' ', '\n')
    var letter = 0
    val colored = buildAnnotatedString {
        text.forEach { char ->
            if (char.isWhitespace()) {
                append(char)
            } else {
                withStyle(SpanStyle(color = CANDY_COLORS[letter++ % CANDY_COLORS.size])) { append(char) }
            }
        }
    }
    val style = TextStyle(
        fontSize = (SIGN_FONT_SIZE * unit).toSp(),
        lineHeight = (SIGN_FONT_SIZE * 1.05f * unit).toSp(),
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
    )
    val layout = assets.textMeasurer.measure(colored, style)
    val shadow = assets.textMeasurer.measure(text, style.copy(color = BUS_TRIM))
    val center = context.p(busX + SIGN_CENTER_X, SIGN_CENTER_Y)
    val fit = min(1f, min(SIGN_WIDTH * unit / layout.size.width, SIGN_HEIGHT * unit / layout.size.height))
    val topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f)
    rotate(SIGN_TILT_DEGREES, pivot = center) {
        scale(fit, pivot = center) {
            drawText(shadow, topLeft = topLeft + Offset(0.3f * unit, 0.3f * unit))
            drawText(layout, topLeft = topLeft)
        }
    }
}
