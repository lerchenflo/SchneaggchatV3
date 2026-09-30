package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFireflyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPizzaOvenUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoRunnerManUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.OVEN_MOUTH_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.OVEN_WIDTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RAINBOW_HALF_WIDTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoTimeOfDay
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoWeather
import kotlin.math.roundToInt
import kotlin.math.sin

// The run's atmosphere (see engine/RodeoRunFlavor) - rain, snow, dusk and night drawn over the
// surface, the rainbow after the rain, fireflies and the ghost horse's glow (see engine/RodeoSkyWonders)
// - plus the other roadside things: the Käsknöpfle kiosk and Stanislaus running along.

private const val RAIN_DROPS = 70
private const val SNOW_FLAKES = 55
/** How far around the horse the lantern lights up the night. */
private const val LANTERN_RADIUS = 32f
private const val STANISLAUS_RUN_HEIGHT = 9f

/** Rain streaks or snow flakes falling over the whole picture; [time] animates them. */
internal fun DrawScope.drawWeather(weather: RodeoWeather, time: Float, unit: Float) {
    when (weather) {
        RodeoWeather.CLEAR -> Unit
        RodeoWeather.RAIN -> repeat(RAIN_DROPS) { index ->
            val x = (cloudNoise(index, 71) * size.width + time * 3f * unit) % size.width
            val y = (cloudNoise(index, 72) * size.height + time * 40f * unit) % size.height
            drawLine(RAIN_COLOR.copy(alpha = 0.5f), Offset(x, y), Offset(x - 0.8f * unit, y + 2.6f * unit), 0.15f * unit, StrokeCap.Round)
        }
        RodeoWeather.SNOW -> repeat(SNOW_FLAKES) { index ->
            val x = (cloudNoise(index, 73) * size.width + sin(time * 0.8f + index) * 2f * unit + size.width) % size.width
            val y = (cloudNoise(index, 74) * size.height + time * 6f * unit) % size.height
            drawCircle(SNOW_COLOR.copy(alpha = 0.85f), radius = (0.2f + 0.25f * cloudNoise(index, 75)) * unit, center = Offset(x, y))
        }
    }
}

/** A white dusting on the ground in the snow. */
internal fun DrawScope.drawSnowCover(groundY: Float, unit: Float) {
    drawRect(SNOW_COLOR.copy(alpha = 0.7f), topLeft = Offset(0f, groundY - 0.3f * unit), size = Size(size.width, 1.2f * unit))
}

/**
 * The light of the time of day over everything: a warm glow at dusk; at night dark all around, with
 * the lantern lighting up [lantern] (the horse).
 */
internal fun DrawScope.drawTimeOfDay(timeOfDay: RodeoTimeOfDay, lantern: Offset, unit: Float) {
    when (timeOfDay) {
        RodeoTimeOfDay.DAY -> Unit
        RodeoTimeOfDay.DUSK -> drawRect(DUSK_TINT.copy(alpha = 0.16f))
        RodeoTimeOfDay.NIGHT -> drawRect(
            Brush.radialGradient(
                0f to Color.Transparent,
                0.55f to NIGHT_SHADE.copy(alpha = 0.35f),
                1f to NIGHT_SHADE.copy(alpha = 0.75f),
                center = lantern,
                radius = LANTERN_RADIUS * unit * 2f,
            )
        )
    }
}

/**
 * A Käsknöpfle kiosk: a wooden booth with a striped awning, a steaming bowl on the counter until the
 * lasso took it, and a pot behind.
 */
internal fun DrawScope.drawKiosk(stop: RodeoPizzaOvenUi, context: RodeoDrawContext, time: Float) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(stop.x + dx, y)
    // Booth and counter
    drawRect(KIOSK_WOOD, p(1f, 12f), Size((OVEN_WIDTH - 2f) * unit, 12f * unit))
    drawRect(context.colors.scrim.copy(alpha = 0.6f), p(2.5f, 11f), Size((OVEN_WIDTH - 5f) * unit, 4.5f * unit))
    drawRect(BOWL_BROWN, p(0.5f, 6.8f), Size((OVEN_WIDTH - 1f) * unit, 1f * unit))
    // Striped awning
    val stripes = 6
    val stripeWidth = (OVEN_WIDTH + 1f) / stripes
    repeat(stripes) { index ->
        drawRect(
            if (index % 2 == 0) KIOSK_AWNING else SNOW_COLOR,
            p(-0.5f + index * stripeWidth, 15.5f),
            Size(stripeWidth * unit, 2.5f * unit)
        )
    }
    // A pot on the stove at the back, steaming
    drawRect(context.colors.outline, p(10f, 9.8f), Size(3f * unit, 2.4f * unit))
    repeat(2) { index ->
        val cycle = (time * 0.7f + index * 0.5f) % 1f
        drawCircle(SNOW_COLOR.copy(alpha = 0.4f * (1f - cycle)), radius = (0.5f + cycle) * unit, center = p(11.5f, 10.5f + 3f * cycle))
    }
    if (stop.hasPizza) drawKnoepfleBowl(p(OVEN_MOUTH_X, 7.8f), unit)
}

/** A bowl of Käsknöpfle: a brown bowl, yellow cheesy noodles and fried onions on top. */
internal fun DrawScope.drawKnoepfleBowl(bottomCenter: Offset, unit: Float) {
    drawArc(BOWL_BROWN, 0f, 180f, useCenter = true, topLeft = bottomCenter - Offset(2.2f * unit, 1.6f * unit), size = Size(4.4f * unit, 3.2f * unit))
    drawOval(KNOEPFLE_YELLOW, bottomCenter - Offset(2f * unit, 0.8f * unit), Size(4f * unit, 1.4f * unit))
    repeat(4) { index ->
        drawLine(ONION_BROWN, bottomCenter + Offset((-1.3f + index * 0.8f) * unit, -0.4f * unit), bottomCenter + Offset((-0.8f + index * 0.8f) * unit, -0.7f * unit), 0.2f * unit, StrokeCap.Round)
    }
}

/** Stanislaus running along, hopping with every stride; dangling at an angle once lassoed. */
internal fun DrawScope.drawRunningStanislaus(runner: RodeoRunnerManUi, image: ImageBitmap, context: RodeoDrawContext) {
    val unit = context.unit
    val height = STANISLAUS_RUN_HEIGHT * unit
    val width = height * image.width / image.height
    val foot = context.p(runner.x, runner.y)
    val tilt = if (runner.caught) -30f else 6f * sin(runner.hop)
    rotate(tilt, pivot = foot) {
        drawImage(
            image = image,
            dstOffset = IntOffset((foot.x - width / 2f).roundToInt(), (foot.y - height).roundToInt()),
            dstSize = IntSize(width.roundToInt(), height.roundToInt()),
        )
    }
}


/** How long the double points last, for the badge's bar. */
private const val DOUBLE_POINTS_BAR_SECONDS = 10f

/** The rainbow after the rain: an arch of colored bands with its feet on the track around [x]. */
internal fun DrawScope.drawRainbow(x: Float, context: RodeoDrawContext) {
    val unit = context.unit
    val band = 1.4f
    RAINBOW_BANDS.forEachIndexed { index, color ->
        val radius = RAINBOW_HALF_WIDTH - index * band - band / 2f
        drawArc(
            color = color.copy(alpha = 0.55f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = context.p(x - radius, radius),
            size = Size(radius * 2f * unit, radius * 2f * unit),
            style = Stroke(width = band * unit),
        )
    }
}

/** A firefly: a soft glow with a bright core, pulsing with [RodeoFireflyUi.glow]. */
internal fun DrawScope.drawFirefly(firefly: RodeoFireflyUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(firefly.x, firefly.y)
    drawCircle(FIREFLY_GLOW.copy(alpha = 0.25f * firefly.glow), radius = 2.4f * unit, center = center)
    drawCircle(FIREFLY_GLOW.copy(alpha = 0.5f * firefly.glow), radius = 1.1f * unit, center = center)
    drawCircle(FIREFLY_GLOW, radius = 0.4f * unit, center = center)
}

/**
 * Draws [block] see-through with a pale glow around [center]: the ghost horse, or the ridden one
 * under its spell. [time] makes the glow breathe.
 */
internal fun DrawScope.drawGhostly(center: Offset, unit: Float, time: Float, block: DrawScope.() -> Unit) {
    drawCircle(GHOST_GLOW.copy(alpha = 0.18f + 0.08f * sin(time)), radius = 18f * unit, center = center)
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(-size.width, -size.height, size.width * 2f, size.height * 2f), Paint().apply { alpha = 0.5f })
    }
    block()
    drawIntoCanvas { it.restore() }
}

/**
 * The "×2" badge at the top while the points count double: [label] on a pill with rainbow stripes,
 * a bar below running down with the [seconds] left.
 */
internal fun DrawScope.drawDoublePointsBadge(label: TextLayoutResult, seconds: Float, unit: Float) {
    val padding = 1.2f * unit
    val width = label.size.width + padding * 2f
    val height = label.size.height + padding
    val topLeft = Offset((size.width - width) / 2f, 2f * unit)
    val stripe = width / RAINBOW_BANDS.size
    RAINBOW_BANDS.forEachIndexed { index, color ->
        drawRect(color.copy(alpha = 0.85f), topLeft + Offset(index * stripe, 0f), Size(stripe + 0.5f, height))
    }
    drawText(label, topLeft = topLeft + Offset(padding, padding / 2f))
    val share = (seconds / DOUBLE_POINTS_BAR_SECONDS).coerceIn(0f, 1f)
    drawRoundRect(
        SNOW_COLOR,
        topLeft + Offset(0f, height + 0.4f * unit),
        Size(width * share, 0.5f * unit),
        CornerRadius(0.25f * unit),
    )
}
