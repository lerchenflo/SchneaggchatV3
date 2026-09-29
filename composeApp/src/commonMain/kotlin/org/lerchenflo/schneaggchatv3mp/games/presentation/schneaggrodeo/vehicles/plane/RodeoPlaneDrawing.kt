package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.plane

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.HAT_COLOR
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.cloudNoise
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * The plane, facing right, and the skyline it flies over. [x] is the left edge of the fuselage, [y]
 * its underside above the ground, [rotation] degrees clockwise while the wreck tumbles.
 */
@Immutable
data class RodeoPlaneUi(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val propellerPhase: Float,
    /** False once the wreck fell out of the picture; the skyline may still be scrolling away. */
    val visible: Boolean,
    /** The cowboy sits in the cockpit. */
    val hasPilot: Boolean,
    /** Rope ladder hanging down while it passes by and while he climbs up. */
    val ladderDown: Boolean,
    val buildings: List<RodeoBuildingUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        val colors = context.colors
        when (layer) {
            // Skyline under the plane, behind everything on the track
            RodeoLayer.BACK -> buildings.forEach { building ->
                drawBuilding(building, context, colors.surfaceVariant, colors.onSurfaceVariant)
                building.cloudBottom?.let { bottom ->
                    drawStormCloud(
                        left = (building.x - CLOUD_OVERHANG) * context.unit,
                        right = (building.x + building.width + CLOUD_OVERHANG) * context.unit,
                        bottomY = context.groundY - bottom * context.unit,
                        unit = context.unit,
                        seed = building.seed,
                        color = colors.onSurfaceVariant,
                        boltColor = colors.tertiary
                    )
                }
            }
            RodeoLayer.BODY -> Unit
            RodeoLayer.FRONT -> if (visible) {
                drawPlane(
                    plane = this@RodeoPlaneUi,
                    context = context,
                    bodyColor = colors.tertiary,
                    wingColor = colors.tertiaryContainer,
                    lineColor = colors.onSurface,
                    pilotColor = colors.onSurface
                )
            }
        }
    }
}

/** A building of the skyline flown over by the plane; [x] is its left edge. */
@Immutable
data class RodeoBuildingUi(
    val x: Float,
    val width: Float,
    val height: Float,
    /** Picks the pattern of lit windows. */
    val seed: Int,
    /** Underside of the storm cloud hanging above, or null for open sky. */
    val cloudBottom: Float?,
)

/** A building of the skyline with a grid of windows, some of them lit. */
private fun DrawScope.drawBuilding(building: RodeoBuildingUi, context: RodeoDrawContext, wallColor: Color, windowColor: Color) {
    val unit = context.unit
    drawRect(wallColor, context.p(building.x, building.height), Size(building.width * unit, building.height * unit))
    var row = 0
    var y = building.height - 3f
    while (y > 3f) {
        var column = 0
        var x = 2f
        while (x + 2f < building.width - 1f) {
            val lit = (building.seed + row * 7 + column * 13) % 3 != 0
            drawRect(
                color = windowColor.copy(alpha = if (lit) 0.45f else 0.15f),
                topLeft = context.p(building.x + x, y),
                size = Size(2f * unit, 2.5f * unit)
            )
            x += 4f
            column++
        }
        y -= 5f
        row++
    }
}

/**
 * A storm cloud hanging from the top of the canvas down to [bottomY]: a dark, billowing mass with
 * puffs of different sizes along its underside, a lighter inner layer for depth, rain streaks and a
 * lightning bolt below some of them. The lowest puff points line up with the collision edge.
 */
private fun DrawScope.drawStormCloud(
    left: Float,
    right: Float,
    bottomY: Float,
    unit: Float,
    seed: Int,
    color: Color,
    boltColor: Color,
) {
    val cloudColor = color.copy(alpha = 0.8f)
    val shadeColor = color.copy(alpha = 0.35f)
    val basePuff = 2.4f * unit
    val width = right - left
    val puffCount = max(2, (width / (basePuff * 1.5f)).toInt())
    val spacing = width / puffCount

    drawRect(cloudColor, Offset(left, 0f), Size(width, max(0f, bottomY - basePuff * 1.3f)))
    // Big billows along the underside, each one touching the collision edge
    for (i in 0..puffCount) {
        val radius = basePuff * (0.85f + 0.5f * cloudNoise(seed, i))
        val x = (left + i * spacing).coerceIn(left + radius * 0.6f, right - radius * 0.6f)
        drawCircle(cloudColor, radius = radius, center = Offset(x, bottomY - radius))
    }
    // Darker bellies between the billows give the mass some depth
    for (i in 0 until puffCount) {
        val radius = basePuff * (0.6f + 0.3f * cloudNoise(seed, i + 20))
        val x = left + (i + 0.5f) * spacing
        drawCircle(shadeColor, radius = radius, center = Offset(x, bottomY - basePuff * 1.6f - radius * 0.3f))
    }

    // Rain streaks slanting down to the left
    val rainColor = color.copy(alpha = 0.3f)
    for (i in 0 until puffCount) {
        val x = left + (i + 0.3f + 0.4f * cloudNoise(seed, i + 40)) * spacing
        val top = bottomY + 0.8f * unit
        drawLine(
            rainColor,
            start = Offset(x, top),
            end = Offset(x - 0.8f * unit, top + 2.5f * unit),
            strokeWidth = 0.3f * unit,
            cap = StrokeCap.Round
        )
    }

    if (seed % 3 == 0) {
        // Filled zigzag bolt with a soft glow
        val boltX = left + width * (0.35f + 0.3f * cloudNoise(seed, 60))
        val top = bottomY - 0.5f * unit
        val bolt = Path().apply {
            moveTo(boltX + 0.6f * unit, top)
            lineTo(boltX - 1.4f * unit, top + 3f * unit)
            lineTo(boltX - 0.1f * unit, top + 3f * unit)
            lineTo(boltX - 1.3f * unit, top + 6.5f * unit)
            lineTo(boltX + 1.5f * unit, top + 2.3f * unit)
            lineTo(boltX + 0.2f * unit, top + 2.3f * unit)
            lineTo(boltX + 1.6f * unit, top)
            close()
        }
        drawCircle(boltColor.copy(alpha = 0.15f), radius = 3f * unit, center = Offset(boltX, top + 3f * unit))
        drawPath(bolt, color = boltColor)
    }
}

/**
 * A small biplane facing right, drawn on a grid with y up from the fuselage underside and x from
 * its left edge. The rope ladder hangs from [PLANE_LADDER_X].
 */
private fun DrawScope.drawPlane(
    plane: RodeoPlaneUi,
    context: RodeoDrawContext,
    bodyColor: Color,
    wingColor: Color,
    lineColor: Color,
    pilotColor: Color,
) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(plane.x + x, plane.y + y)
    val center = p(PLANE_LENGTH / 2f, 3.5f)

    rotate(plane.rotation, pivot = center) {
        if (plane.ladderDown) {
            val bottom = -PLANE_LADDER_LENGTH
            drawLine(lineColor, p(PLANE_LADDER_X - 0.8f, 0f), p(PLANE_LADDER_X - 0.8f, bottom), 0.3f * unit)
            drawLine(lineColor, p(PLANE_LADDER_X + 0.8f, 0f), p(PLANE_LADDER_X + 0.8f, bottom), 0.3f * unit)
            var rung = -1.5f
            while (rung >= bottom) {
                drawLine(lineColor, p(PLANE_LADDER_X - 0.8f, rung), p(PLANE_LADDER_X + 0.8f, rung), 0.3f * unit)
                rung -= 2f
            }
        }

        // Lower wing and struts behind the fuselage
        drawRoundRect(wingColor, p(6f, 1f), Size(10f * unit, 1.2f * unit), CornerRadius(0.6f * unit))
        listOf(8f, 14f).forEach { x -> drawLine(lineColor, p(x, 1f), p(x, 8.5f), 0.35f * unit) }

        // Tail fin and fuselage
        drawPath(polygonPath(::p, 0f to 5f, 0.5f to 10f, 3f to 10f, 5f to 5f), bodyColor)
        drawRoundRect(bodyColor, p(0f, 6f), Size(20f * unit, 5f * unit), CornerRadius(2.5f * unit))

        // Pilot: head and hat sticking out of the cockpit
        if (plane.hasPilot) {
            drawCircle(pilotColor, radius = 1.6f * unit, center = p(PLANE_PILOT_X, 7.6f))
            drawRoundRect(HAT_COLOR, p(PLANE_PILOT_X - 2.8f, 9.6f), Size(5.6f * unit, 0.6f * unit), CornerRadius(0.3f * unit))
            drawRoundRect(HAT_COLOR, p(PLANE_PILOT_X - 1.5f, 11.6f), Size(3f * unit, 2.2f * unit), CornerRadius(0.7f * unit))
        }

        // Upper wing on top
        drawRoundRect(wingColor, p(5f, 9.5f), Size(12f * unit, 1.2f * unit), CornerRadius(0.6f * unit))

        // Spinning propeller: a blade whose visible length pulses
        val blade = 3.2f * abs(sin(plane.propellerPhase))
        drawLine(lineColor, p(20.5f, 3.5f - blade), p(20.5f, 3.5f + blade), 0.6f * unit, StrokeCap.Round)
        drawCircle(lineColor, radius = 0.6f * unit, center = p(20.5f, 3.5f))
    }
}
