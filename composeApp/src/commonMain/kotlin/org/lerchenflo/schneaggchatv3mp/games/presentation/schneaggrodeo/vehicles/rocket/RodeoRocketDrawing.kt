package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rocket

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.HAT_COLOR
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.cos
import kotlin.math.sin

// Planets, explicitly requested in fixed colors
private val MARS_COLOR = Color(0xFFD9583B)
private val MARS_CRATER_COLOR = Color(0xFFA63D26)
private val JUPITER_COLOR = Color(0xFFD8A06A)
private val JUPITER_BAND_COLOR = Color(0xFFB5764A)
private val NEPTUNE_COLOR = Color(0xFF4F7BD9)
private val NEPTUNE_BAND_COLOR = Color(0xFF86A8EE)
private val SATURN_COLOR = Color(0xFFE3C878)
private val SATURN_RING_COLOR = Color(0xFFB9A064)
private val ALIEN_COLOR = Color(0xFF5DBB63)
private val ALIEN_SPOT_COLOR = Color(0xFF3B8A42)
private val PLANET_SHADOW = Color.Black.copy(alpha = 0.25f)
private val PLANET_SHINE = Color.White.copy(alpha = 0.18f)

// Planet kinds (0 until PLANET_KINDS)
private const val MARS = 0
private const val JUPITER = 1
private const val NEPTUNE = 2
private const val SATURN = 3

/** The rocket, facing right: [x] is its rear end, [y] the underside of its body, [tilt] degrees nose up. */
@Immutable
data class RodeoRocketUi(
    val x: Float,
    val y: Float,
    val tilt: Float,
    /** 0..1: size of the flame. */
    val thrust: Float,
    /** Animates the flame's flicker. */
    val flicker: Float,
    val visible: Boolean,
    /** Horse and rider peek out of the glass dome. */
    val passengers: Boolean,
    val planets: List<RodeoPlanetUi>,
) : RodeoVehicleUi {

    override val lassoHint: RodeoLassoHintUi? get() = null

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        val colors = context.colors
        when (layer) {
            RodeoLayer.BACK -> planets.forEach { drawPlanet(it, context) }
            RodeoLayer.BODY -> if (visible) {
                drawRocket(
                    rocket = this@RodeoRocketUi,
                    context = context,
                    bodyColor = colors.surfaceBright,
                    lineColor = colors.onSurface,
                    accentColor = colors.tertiary,
                    domeColor = colors.primaryContainer,
                    flameColor = colors.error,
                    flameCoreColor = colors.errorContainer,
                )
            }
            RodeoLayer.FRONT -> Unit
        }
    }
}

/** A planet in space; [kind] picks its look, [seed] varies its craters and bands. */
@Immutable
data class RodeoPlanetUi(val x: Float, val y: Float, val radius: Float, val kind: Int, val seed: Int)

/** A round planet with shading and a feature depending on its kind: craters, bands, a ring or spots. */
private fun DrawScope.drawPlanet(planet: RodeoPlanetUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(planet.x, planet.y)
    val radius = planet.radius * unit
    val (base, detail) = when (planet.kind) {
        MARS -> MARS_COLOR to MARS_CRATER_COLOR
        JUPITER -> JUPITER_COLOR to JUPITER_BAND_COLOR
        NEPTUNE -> NEPTUNE_COLOR to NEPTUNE_BAND_COLOR
        SATURN -> SATURN_COLOR to SATURN_RING_COLOR
        else -> ALIEN_COLOR to ALIEN_SPOT_COLOR
    }
    val ringTopLeft = center - Offset(radius * 1.8f, radius * 0.45f)
    val ringSize = Size(radius * 3.6f, radius * 0.9f)
    // Saturn's ring: the back half first, the front half over the planet
    if (planet.kind == SATURN) {
        drawArc(detail, 180f, 180f, useCenter = false, topLeft = ringTopLeft, size = ringSize, style = Stroke(width = radius * 0.22f))
    }
    drawCircle(base, radius = radius, center = center)
    val disc = Path().apply { addOval(Rect(center, radius)) }
    clipPath(disc) {
        when (planet.kind) {
            // Bands across
            JUPITER, NEPTUNE -> listOf(-0.5f, -0.1f, 0.35f).forEachIndexed { index, offset ->
                val height = radius * (0.14f + 0.06f * ((planet.seed + index) % 3))
                drawRect(detail, Offset(center.x - radius, center.y + offset * radius), Size(radius * 2f, height))
            }
            SATURN -> Unit
            // Mars craters and alien spots
            else -> repeat(4) { index ->
                val angle = (planet.seed * 0.7f + index * 1.9f)
                val distance = radius * (0.2f + 0.15f * index)
                drawCircle(
                    detail,
                    radius = radius * (0.12f + 0.05f * ((planet.seed + index) % 3)),
                    center = center + Offset(cos(angle) * distance, sin(angle) * distance)
                )
            }
        }
        // Night side and a soft shine
        drawCircle(PLANET_SHADOW, radius = radius, center = center + Offset(radius * 0.45f, radius * 0.35f))
        drawCircle(PLANET_SHINE, radius = radius * 0.35f, center = center - Offset(radius * 0.4f, radius * 0.4f))
    }
    if (planet.kind == SATURN) {
        drawArc(detail, 0f, 180f, useCenter = false, topLeft = ringTopLeft, size = ringSize, style = Stroke(width = radius * 0.22f))
    }
}

/**
 * The rocket on a grid with y up from its underside and x from its rear: fins, body with a stripe,
 * a glass dome with the horse's ears and the cowboy's hat peeking out, nose cone and flame.
 */
private fun DrawScope.drawRocket(
    rocket: RodeoRocketUi,
    context: RodeoDrawContext,
    bodyColor: Color,
    lineColor: Color,
    accentColor: Color,
    domeColor: Color,
    flameColor: Color,
    flameCoreColor: Color,
) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(rocket.x + x, rocket.y + y)
    fun polygon(vararg points: Pair<Float, Float>) = polygonPath(::p, *points)
    val middle = ROCKET_BODY_HEIGHT / 2f

    // Counterclockwise on screen = nose up
    rotate(-rocket.tilt, pivot = p(ROCKET_LENGTH / 2f, middle)) {
        // Flame out of the nozzle, flickering
        if (rocket.thrust > 0f) {
            val length = (4f + 16f * rocket.thrust) * (0.85f + 0.15f * sin(rocket.flicker))
            drawPath(polygon(-1f to middle + 3f, -1f - length to middle, -1f to middle - 3f), flameColor)
            drawPath(polygon(-1f to middle + 1.6f, -1f - length * 0.55f to middle, -1f to middle - 1.6f), flameCoreColor)
        }
        // Fins at the back, top and bottom
        drawPath(polygon(1f to ROCKET_BODY_HEIGHT, 0f to ROCKET_BODY_HEIGHT + 4f, 8f to ROCKET_BODY_HEIGHT), accentColor)
        drawPath(polygon(1f to 0f, 0f to -ROCKET_PAD_HEIGHT, 8f to 0f), accentColor)
        // Nozzle
        drawRect(lineColor, p(-1.5f, middle + 2f), Size(2f * unit, 4f * unit))

        // Glass dome, with the passengers peeking out
        val domeTopLeft = p(DOME_X, ROCKET_BODY_HEIGHT + DOME_HEIGHT)
        val domeSize = Size(DOME_WIDTH * unit, DOME_HEIGHT * 2f * unit)
        drawArc(domeColor, 180f, 180f, useCenter = true, topLeft = domeTopLeft, size = domeSize)
        if (rocket.passengers) {
            // Horse ears and head, cowboy's head and orange hat
            drawPath(polygon(26f to ROCKET_BODY_HEIGHT, 28f to ROCKET_BODY_HEIGHT + 3.5f, 29.5f to ROCKET_BODY_HEIGHT + 2f, 29f to ROCKET_BODY_HEIGHT), lineColor)
            drawPath(polygon(26.5f to ROCKET_BODY_HEIGHT + 3f, 27f to ROCKET_BODY_HEIGHT + 4.6f, 27.6f to ROCKET_BODY_HEIGHT + 3f), lineColor)
            drawCircle(lineColor, radius = 1.4f * unit, center = p(21.5f, ROCKET_BODY_HEIGHT + 1.6f))
            drawRoundRect(HAT_COLOR, p(19.3f, ROCKET_BODY_HEIGHT + 3.3f), Size(4.4f * unit, 0.5f * unit), CornerRadius(0.25f * unit))
            drawRoundRect(HAT_COLOR, p(20.3f, ROCKET_BODY_HEIGHT + 5f), Size(2.4f * unit, 1.8f * unit), CornerRadius(0.5f * unit))
        }
        drawArc(lineColor, 180f, 180f, useCenter = false, topLeft = domeTopLeft, size = domeSize, style = Stroke(width = 0.35f * unit))
        drawArc(bodyColor.copy(alpha = 0.6f), 205f, 40f, useCenter = false, topLeft = domeTopLeft + Offset(unit, unit), size = Size(domeSize.width - 2f * unit, domeSize.height - 2f * unit), style = Stroke(width = 0.5f * unit))

        // Body with a stripe and a porthole
        val bodyTopLeft = p(0f, ROCKET_BODY_HEIGHT)
        val bodySize = Size(ROCKET_BODY_LENGTH * unit, ROCKET_BODY_HEIGHT * unit)
        drawRoundRect(bodyColor, bodyTopLeft, bodySize, CornerRadius(2f * unit))
        drawRect(accentColor, p(9f, ROCKET_BODY_HEIGHT), Size(1.4f * unit, ROCKET_BODY_HEIGHT * unit))
        drawRoundRect(lineColor, bodyTopLeft, bodySize, CornerRadius(2f * unit), style = Stroke(width = 0.35f * unit))
        drawCircle(domeColor, radius = 1.6f * unit, center = p(37f, middle))
        drawCircle(lineColor, radius = 1.6f * unit, center = p(37f, middle), style = Stroke(width = 0.35f * unit))
        // Nose cone
        drawPath(polygon(ROCKET_BODY_LENGTH - 0.5f to ROCKET_BODY_HEIGHT, ROCKET_LENGTH to middle, ROCKET_BODY_LENGTH - 0.5f to 0f), accentColor)
    }
}
