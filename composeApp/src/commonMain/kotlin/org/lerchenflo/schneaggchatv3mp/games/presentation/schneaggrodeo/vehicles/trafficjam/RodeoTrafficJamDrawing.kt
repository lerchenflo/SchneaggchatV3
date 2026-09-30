package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.trafficjam

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWheel
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi

// The jam's fixed colors: car paints, glass, tyres, lights and the honking
private val CAR_PAINTS = listOf(
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFFECEFF1),
    Color(0xFF546E7A),
)
internal val JAM_CAR_COLOR_COUNT = CAR_PAINTS.size
private val TYRE = Color(0xFF212121)
private val HUB = Color(0xFFBDBDBD)
private val BRAKE_LIGHT = Color(0xFFFF1744)
private val HEAD_LIGHT = Color(0xFFFFF59D)
private val HONK = Color(0xFFFFB300)
private val CAR_TRIM = Color(0xFF212121)
private const val WHEEL_RADIUS = 3f

/** One car of the jam: [offset] from the jam's rear end, its [style], paint index and [seed]. */
@Immutable
data class RodeoJamCarUi(
    val offset: Float,
    val style: JamCarStyle,
    val color: Int,
    val seed: Int,
)

/** The traffic jam, facing right: [x] is the rear of the last car; [time] makes them honk. */
@Immutable
data class RodeoTrafficJamUi(
    val x: Float,
    val cars: List<RodeoJamCarUi>,
    val time: Float,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            RodeoLayer.BACK -> Unit
            RodeoLayer.BODY -> cars.forEach { drawJamCar(x + it.offset, it, context) }
            RodeoLayer.FRONT -> cars.forEach { car ->
                // Now and then a driver leans on the horn
                val cycle = (time * 0.6f + car.seed * 0.37f) % 1f
                if (cycle < 0.18f) drawHonk(x + car.offset + car.style.length, 6f, cycle / 0.18f, context)
            }
        }
    }
}

/**
 * A car standing still, drawn like the Ford Escort: a flat body silhouette with dark wheel arches,
 * wheels, bumpers, glass, a driver behind the wheel, glowing brake lights and head lights.
 */
private fun DrawScope.drawJamCar(left: Float, car: RodeoJamCarUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(left + x, y)
    val paint = CAR_PAINTS[car.color]
    val dark = paint.copy(red = paint.red * 0.6f, green = paint.green * 0.6f, blue = paint.blue * 0.6f)
    val glass = context.colors.surfaceVariant.copy(alpha = 0.55f)
    val length = car.style.length
    val roof = car.style.roof
    val beltline = 8f
    val rearWheel = 7f
    val frontWheel = length - 7f

    // Body with the cabin on top
    val body = when (car.style) {
        JamCarStyle.HATCHBACK -> polygonPath(
            ::p,
            0.3f to 2f, 0f to 6f, 0.6f to beltline + 0.4f, 2.5f to roof, 17f to roof, 22f to beltline,
            29f to 7f, 30f to 5f, 29.8f to 2f,
        )
        JamCarStyle.SEDAN -> polygonPath(
            ::p,
            0.3f to 2f, 0f to 6f, 0.6f to beltline, 7f to beltline, 10f to roof, 21f to roof, 25f to beltline,
            35f to 7f, 36f to 5f, 35.8f to 2f,
        )
        JamCarStyle.VAN -> polygonPath(
            ::p,
            0.3f to 2f, 0f to roof - 0.5f, 0.8f to roof, 29f to roof, 33f to beltline + 1f,
            37f to 7.4f, 38f to 5f, 37.8f to 2f,
        )
    }
    drawPath(body, paint)
    // Windows with their pillars
    when (car.style) {
        JamCarStyle.HATCHBACK -> {
            drawPath(polygonPath(::p, 2f to beltline + 0.6f, 3.3f to roof - 0.8f, 9.5f to roof - 0.8f, 9.5f to beltline + 0.6f), glass)
            drawPath(polygonPath(::p, 10.5f to beltline + 0.6f, 10.5f to roof - 0.8f, 16.5f to roof - 0.8f, 20.6f to beltline + 0.6f), glass)
        }
        JamCarStyle.SEDAN -> {
            drawPath(polygonPath(::p, 8f to beltline + 0.6f, 10.4f to roof - 0.8f, 15f to roof - 0.8f, 15f to beltline + 0.6f), glass)
            drawPath(polygonPath(::p, 16f to beltline + 0.6f, 16f to roof - 0.8f, 20.6f to roof - 0.8f, 23.6f to beltline + 0.6f), glass)
        }
        JamCarStyle.VAN -> {
            drawRect(glass, p(2f, roof - 1.2f), Size(8f * unit, 4f * unit))
            drawRect(glass, p(11.5f, roof - 1.2f), Size(8f * unit, 4f * unit))
            drawPath(polygonPath(::p, 22f to roof - 1.2f, 28.4f to roof - 1.2f, 31.6f to beltline + 1.8f, 22f to beltline + 1.8f), glass)
            // Sliding door seam
            drawLine(dark, p(11f, 3f), p(11f, roof - 0.6f), 0.25f * unit)
        }
    }
    // The driver behind the wheel, waiting
    val driverX = when (car.style) {
        JamCarStyle.HATCHBACK -> 15f
        JamCarStyle.SEDAN -> 19f
        JamCarStyle.VAN -> 26f
    }
    drawCircle(context.colors.onSurface, radius = 1.6f * unit, center = p(driverX, roof - 2.8f))
    // Door lines and a handle
    drawLine(dark, p(driverX - 5f, 3f), p(driverX - 5f, beltline), 0.2f * unit)
    drawLine(dark, p(driverX + 3f, 3f), p(driverX + 3f, beltline), 0.2f * unit)
    drawLine(CAR_TRIM, p(driverX + 0.5f, 6.8f), p(driverX + 2f, 6.8f), 0.4f * unit, StrokeCap.Round)
    // Wheel arches with wheels in them
    listOf(rearWheel, frontWheel).forEach { wheelX ->
        drawArc(CAR_TRIM, 180f, 180f, useCenter = true, topLeft = p(wheelX - 3.8f, 6.8f), size = Size(7.6f * unit, 7.6f * unit))
        drawWheel(p(wheelX, WHEEL_RADIUS), WHEEL_RADIUS * unit, car.seed.toFloat(), TYRE, HUB, paint)
    }
    // Bumpers, brake lights glowing red at the back, head lights in front
    drawRect(CAR_TRIM, p(-0.6f, 3.8f), Size(2.8f * unit, 1.6f * unit))
    drawRect(CAR_TRIM, p(length - 2f, 3.6f), Size(2.6f * unit, 1.4f * unit))
    drawRect(BRAKE_LIGHT, p(0f, 7.4f), Size(1.4f * unit, 1.6f * unit))
    drawCircle(BRAKE_LIGHT.copy(alpha = 0.25f), radius = 2.2f * unit, center = p(0.6f, 6.6f))
    drawCircle(HEAD_LIGHT, radius = 0.9f * unit, center = p(length - 1f, 5.8f))
}

/** Honk lines fanning out in front of a car, growing with [progress]. */
private fun DrawScope.drawHonk(frontX: Float, y: Float, progress: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val origin = context.p(frontX + 0.6f, y)
    val length = (1f + 1.6f * progress) * unit
    listOf(-0.6f, 0f, 0.6f).forEach { slope ->
        val start = origin + Offset(0.6f * unit, -slope * 0.8f * unit)
        drawLine(HONK.copy(alpha = 1f - progress * 0.6f), start, start + Offset(length, -slope * length), 0.3f * unit, StrokeCap.Round)
    }
}
