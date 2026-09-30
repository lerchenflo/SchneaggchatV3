package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.fordescort

import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoFootprint
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.onFootprint
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.HAT_COLOR
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.cloudNoise
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.polygonPath
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoLassoHintUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoVehicleUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// The wreck's fixed colors, explicitly requested: a red Escort, black plastic, rust, sparks, golden
// BBS rims, a wooden crate full of green bills, and the scrapyard's junk
private val CAR_RED = Color(0xFFD32F2F)
private val CAR_RED_DARK = Color(0xFF8E1B1B)
private val CAR_TRIM = Color(0xFF212121)
private val CAR_RUST = Color(0xFF8D5524)
private val CAR_SPARK = Color(0xFFFFC107)
private val RIM_GOLD = Color(0xFFD4AF37)
private val RIM_GOLD_LIGHT = Color(0xFFFFE082)
private val RIM_GOLD_DARK = Color(0xFF8C6D1F)
private val CRATE_WOOD = Color(0xFFA1887F)
private val CRATE_WOOD_DARK = Color(0xFF5D4037)
private val MONEY_GREEN = Color(0xFF43A047)
private val MONEY_GREEN_LIGHT = Color(0xFFA5D6A7)
private val SCRAP_GRAY = Color(0xFF78909C)
private val SCRAP_DARK = Color(0xFF455A64)
private val SCRAP_BLUE = Color(0xFF3F6E9E)
private val SHEET_LIGHT = Color(0xFF9EA7AD)
private val JUNK_GREEN = Color(0xFF5B7F3A)
private val APPLIANCE_WHITE = Color(0xFFE0E0E0)
private val CRANE_YELLOW = Color(0xFFF9A825)
private val CRANE_DARK = Color(0xFF6D5A00)
private val LIFT_BLUE = Color(0xFF1565C0)

private const val RIM_RADIUS = CAR_RIM_LIFT
private const val MONEY_FONT_SIZE = 2.6f

/**
 * The wrecked Escort cabrio, facing right. [x] is its rear bumper, [y] how high it flies (0 on the
 * road), [rotation] degrees clockwise (rattling while shoved, tumbling once kicked away). [sparks]
 * drives the sparks flying from its rims while it scrapes along, null while it doesn't.
 */
@Immutable
data class RodeoFordEscortUi(
    val x: Float,
    val y: Float,
    val rotation: Float,
    /** The cowboy sits behind the wheel. */
    val hasDriver: Boolean,
    val sparks: Float?,
    /** Seconds, for the drips and the smoke. */
    val time: Float,
    /** Rotation of the rims in radians. */
    val rimPhase: Float,
    /** Cash left in the trunk, or null once it is gone for good. */
    val money: Int?,
    /** Broken down: smoke rises from the hood. */
    val smoking: Boolean,
    /** Paint runs down its sides and drips onto the road. */
    val dripping: Boolean,
    /** Lies on the scrapyard's junk pile. */
    val onPile: Boolean,
    /** Left end of the scrapyard once it came up, else null. */
    val yardX: Float?,
    /** The car lift once it came up (the other ending), else null. */
    val lift: RodeoCarLiftUi?,
    /** Stands on the lift's runway. */
    val onLift: Boolean,
    /** Torn off on the car lift. */
    val partsGone: Set<RodeoEscortPart>,
    val flyingParts: List<RodeoFlyingPartUi>,
    val bills: List<RodeoBillUi>,
    val puddles: List<RodeoPuddleUi>,
    override val lassoHint: RodeoLassoHintUi?,
) : RodeoVehicleUi {

    override val footprint get() = RodeoFootprint(x + 7.5f, x + 30.5f)

    override fun DrawScope.draw(layer: RodeoLayer, context: RodeoDrawContext) {
        when (layer) {
            // The scrapyard stands behind the track, the wreck ends up on its pile
            RodeoLayer.BACK -> {
                yardX?.let { drawScrapyard(it, context) }
                flyingParts.filter { it.landed }.forEach { drawFlyingPart(it, context) }
                lift?.let { drawCarLift(it, context) }
                if (onPile || onLift) drawFordEscort(this@RodeoFordEscortUi, context)
            }
            RodeoLayer.BODY -> Unit
            // In front of the horse: its nose disappears behind the trunk it shoves
            RodeoLayer.FRONT -> {
                puddles.forEach { drawPuddle(it, context) }
                if (!onPile && !onLift) onFootprint(context, footprint) { drawFordEscort(this@RodeoFordEscortUi, it) }
                flyingParts.filter { !it.landed }.forEach { drawFlyingPart(it, context) }
                bills.forEach { drawBill(it, context) }
            }
        }
    }
}

/** A bill blown out of the trunk; [x] / [y] its center. */
@Immutable
data class RodeoBillUi(val x: Float, val y: Float, val rotation: Float)

/** The parts the car lift strips off, in that order; [carX] / [carY] where each sits on the car's grid. */
enum class RodeoEscortPart(internal val carX: Float, internal val carY: Float) {
    DOOR(21f, 10f),
    WINDSHIELD(25.8f, 9.8f),
    SEATS(15f, 10.5f),
    CRATE(CRATE_X, 9.5f),
    REAR_RIM(7.5f, 0f),
    FRONT_RIM(30.5f, 0f),
    REAR_BUMPER(0.8f, 2.8f),
    FRONT_BUMPER(37.3f, 2.7f),
}

/** A part torn off on the car lift; [x] / [y] its center, [landed] once it lies on the junk pile. */
@Immutable
data class RodeoFlyingPartUi(val part: RodeoEscortPart, val x: Float, val y: Float, val rotation: Float, val landed: Boolean)

/** The car lift: [x] its rear pillar, [armHeight] how high the runway is raised. */
@Immutable
data class RodeoCarLiftUi(val x: Float, val armHeight: Float)

/** A puddle of red paint on the road; [x] its center. */
@Immutable
data class RodeoPuddleUi(val x: Float, val size: Float)

/**
 * Side view on a grid with x from the rear bumper and y up from the belly, which rides on the rims
 * CAR_RIM_LIFT above the road.
 */
private fun DrawScope.drawFordEscort(car: RodeoFordEscortUi, context: RodeoDrawContext) {
    val unit = context.unit
    val colors = context.colors
    fun p(x: Float, y: Float) = context.p(car.x + x, car.y + CAR_RIM_LIFT + y)

    car.sparks?.let { drawScrapeSparks(::p, unit, it) }

    rotate(car.rotation, pivot = p(CAR_LENGTH / 2f, 4f)) {
        val gone = car.partsGone
        fun has(part: RodeoEscortPart) = part !in gone
        if (has(RodeoEscortPart.SEATS)) drawEscortPart(RodeoEscortPart.SEATS, ::p, unit, context, car)

        if (car.hasDriver) {
            // Torso, arms on the wheel, head and hat above the door
            drawRoundRect(colors.primary, p(CAR_SEAT_X - 1.2f, 11.4f), Size(3f * unit, 4.6f * unit), CornerRadius(1f * unit))
            drawLine(colors.primary, p(CAR_SEAT_X + 1.2f, 10.2f), p(24f, 9.2f), 1f * unit, StrokeCap.Round)
            drawCircle(colors.onSurface, radius = 1.8f * unit, center = p(CAR_SEAT_X + 0.3f, 13.2f))
            drawRoundRect(HAT_COLOR, p(CAR_SEAT_X - 2.9f, 15.4f), Size(6.4f * unit, 0.7f * unit), CornerRadius(0.35f * unit))
            drawRoundRect(HAT_COLOR, p(CAR_SEAT_X - 1.5f, 17.8f), Size(3.6f * unit, 2.6f * unit), CornerRadius(0.8f * unit))
        }

        // The cash crate in the trunk, the bills in it sinking as they blow away
        if (has(RodeoEscortPart.CRATE)) drawEscortPart(RodeoEscortPart.CRATE, ::p, unit, context, car)

        // Body: trunk, sills, long hood
        drawPath(
            polygonPath(
                ::p,
                0.3f to 0f, 0f to 4.5f, 0.6f to 7.6f, 9.5f to 8.2f, 10f to 7.6f,
                28f to 7.6f, 29f to 7.4f, 37f to 6.4f, 38f to 5f, 37.8f to 0f,
            ),
            CAR_RED
        )
        // Wheel arches with bare golden BBS rims in them - no tyres left
        listOf(7.5f, 30.5f).forEach { archX ->
            drawArc(
                CAR_TRIM,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = p(archX - 3.6f, 3.6f),
                size = Size(7.2f * unit, 7.2f * unit)
            )
        }
        if (has(RodeoEscortPart.REAR_RIM)) drawEscortPart(RodeoEscortPart.REAR_RIM, ::p, unit, context, car)
        if (has(RodeoEscortPart.FRONT_RIM)) drawEscortPart(RodeoEscortPart.FRONT_RIM, ::p, unit, context, car)
        // Rust around the arches and dents all over
        repeat(5) { index ->
            val dentX = 3f + 32f * cloudNoise(index, 7)
            val dentY = 1.5f + 5f * cloudNoise(index, 8)
            drawOval(
                if (index % 2 == 0) CAR_RUST else CAR_RED_DARK,
                topLeft = p(dentX, dentY),
                size = Size((1.2f + cloudNoise(index, 9) * 1.6f) * unit, 1f * unit)
            )
        }
        if (car.dripping) drawPaintDrips(::p, unit, car.time)
        if (has(RodeoEscortPart.DOOR)) {
            drawEscortPart(RodeoEscortPart.DOOR, ::p, unit, context, car)
        } else {
            // A gaping hole where the door was
            drawRect(CAR_TRIM.copy(alpha = 0.75f), p(15.5f, 7f), Size(11f * unit, 5.2f * unit))
        }
        // A broken tail light and an empty headlight socket
        drawRect(CAR_RED_DARK, p(0.2f, 6.8f), Size(1.4f * unit, 1.6f * unit))
        drawCircle(CAR_TRIM, radius = 0.9f * unit, center = p(37f, 5.4f))
        if (has(RodeoEscortPart.REAR_BUMPER)) drawEscortPart(RodeoEscortPart.REAR_BUMPER, ::p, unit, context, car)
        if (has(RodeoEscortPart.FRONT_BUMPER)) drawEscortPart(RodeoEscortPart.FRONT_BUMPER, ::p, unit, context, car)
        if (has(RodeoEscortPart.WINDSHIELD)) drawEscortPart(RodeoEscortPart.WINDSHIELD, ::p, unit, context, car)
    }

    if (car.smoking) drawBreakdownSmoke(::p, unit, car.time, context)
    // What is left in the trunk, above it and never rotated so it stays readable
    car.money?.let { money -> drawMoneyLabel(money, p(CRATE_X, CRATE_TOP + 5f), context) }
}

/**
 * One of the parts the car lift strips off, drawn on the car's grid through [p] - on the car itself,
 * or flying / lying on its own (see [drawFlyingPart]).
 */
private fun DrawScope.drawEscortPart(part: RodeoEscortPart, p: (Float, Float) -> Offset, unit: Float, context: RodeoDrawContext, car: RodeoFordEscortUi?) {
    val colors = context.colors
    when (part) {
        RodeoEscortPart.SEATS -> {
            // Rear seat, driver's seat back, the folded, torn soft top, the bent steering wheel and the roll bar
            drawRect(CAR_TRIM, p(12f, 10f), Size(2f * unit, 2.6f * unit))
            drawRect(CAR_TRIM, p(18.2f, 10.8f), Size(1.6f * unit, 3.4f * unit))
            drawRoundRect(CAR_TRIM, p(8.8f, 9.4f), Size(4.8f * unit, 2f * unit), CornerRadius(0.8f * unit))
            drawLine(CAR_TRIM, p(23.4f, 7.6f), p(24.6f, 10.4f), 0.5f * unit, StrokeCap.Round)
            drawLine(CAR_TRIM, p(15.8f, 7.6f), p(16.8f, 12.8f), 0.8f * unit, StrokeCap.Round)
            drawLine(CAR_TRIM, p(16.8f, 12.8f), p(18f, 12.8f), 0.8f * unit, StrokeCap.Round)
        }
        RodeoEscortPart.CRATE -> drawCashCrate(p, unit, car?.money ?: 0)
        RodeoEscortPart.DOOR -> rotate(7f, pivot = p(15f, 7.4f)) {
            // Hanging askew
            drawRect(CAR_RED, p(15f, 7.2f), Size(12f * unit, 5.6f * unit))
            drawRect(CAR_RED_DARK, p(15f, 7.2f), Size(12f * unit, 5.6f * unit), style = Stroke(width = 0.35f * unit))
            drawLine(CAR_TRIM, p(24.5f, 5.6f), p(26f, 5.6f), 0.4f * unit, StrokeCap.Round)
        }
        RodeoEscortPart.WINDSHIELD -> {
            // Bent frame with cracked glass
            drawPath(polygonPath(p, 27.6f to 7.6f, 26.2f to 10.6f, 25f to 12.2f, 23.8f to 11.6f, 25.4f to 7.6f), colors.surfaceVariant.copy(alpha = 0.45f))
            drawLine(CAR_TRIM, p(27.6f, 7.6f), p(26.2f, 10.6f), 0.4f * unit, StrokeCap.Round)
            drawLine(CAR_TRIM, p(26.2f, 10.6f), p(25f, 12.2f), 0.4f * unit, StrokeCap.Round)
            drawLine(colors.outline, p(26.8f, 8.4f), p(25.2f, 10.4f), 0.15f * unit)
            drawLine(colors.outline, p(25.9f, 9.2f), p(26.6f, 10.8f), 0.15f * unit)
        }
        RodeoEscortPart.REAR_RIM, RodeoEscortPart.FRONT_RIM ->
            drawBbsRim(p(part.carX, part.carY), RIM_RADIUS * unit, car?.rimPhase ?: 0f, unit)
        RodeoEscortPart.REAR_BUMPER -> drawRect(CAR_TRIM, p(-0.6f, 3.6f), Size(2.8f * unit, 1.6f * unit))
        RodeoEscortPart.FRONT_BUMPER -> drawRect(CAR_TRIM, p(36f, 3.4f), Size(2.6f * unit, 1.4f * unit))
    }
}

/** A part torn off on the car lift, flying over to the junk pile or lying on it; [part.x] / [part.y] its center. */
private fun DrawScope.drawFlyingPart(part: RodeoFlyingPartUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(part.x, part.y)
    // The part's own spot on the car's grid ends up at the center
    fun p(x: Float, y: Float) = center + Offset((x - part.part.carX) * unit, -(y - part.part.carY) * unit)
    rotate(part.rotation, pivot = center) {
        drawEscortPart(part.part, ::p, unit, context, car = null)
    }
}

/**
 * A two-pillar car lift, [lift.x] its rear pillar: base plates, the pillars with their warning
 * stripes, and the runway between them at [lift.armHeight].
 */
private fun DrawScope.drawCarLift(lift: RodeoCarLiftUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(lift.x + dx, y)
    val line = context.colors.onSurface
    listOf(0f, LIFT_SPAN - LIFT_PILLAR_WIDTH).forEach { pillar ->
        drawRect(SCRAP_DARK, p(pillar - 1f, 0.6f), Size((LIFT_PILLAR_WIDTH + 2f) * unit, 0.6f * unit))
        drawRect(LIFT_BLUE, p(pillar, LIFT_PILLAR_HEIGHT), Size(LIFT_PILLAR_WIDTH * unit, LIFT_PILLAR_HEIGHT * unit))
        drawRect(line.copy(alpha = 0.5f), p(pillar, LIFT_PILLAR_HEIGHT), Size(LIFT_PILLAR_WIDTH * unit, LIFT_PILLAR_HEIGHT * unit), style = Stroke(width = 0.2f * unit))
        // Yellow and black warning stripes at the foot
        repeat(3) { stripe ->
            drawRect(CRANE_YELLOW, p(pillar, 1.2f + stripe * 1.6f + 0.8f), Size(LIFT_PILLAR_WIDTH * unit, 0.8f * unit))
            drawRect(CAR_TRIM, p(pillar, 1.2f + stripe * 1.6f + 1.6f), Size(LIFT_PILLAR_WIDTH * unit, 0.8f * unit))
        }
        // Carriage riding up the pillar with the runway
        drawRect(SHEET_LIGHT, p(pillar - 0.4f, lift.armHeight + RUNWAY_THICKNESS + 2f), Size((LIFT_PILLAR_WIDTH + 0.8f) * unit, 3.2f * unit))
    }
    // Runway between the pillars
    drawRect(SCRAP_GRAY, p(LIFT_PILLAR_WIDTH, lift.armHeight + RUNWAY_THICKNESS), Size((LIFT_SPAN - 2f * LIFT_PILLAR_WIDTH) * unit, RUNWAY_THICKNESS * unit))
    drawLine(line.copy(alpha = 0.5f), p(LIFT_PILLAR_WIDTH, lift.armHeight + RUNWAY_THICKNESS), p(LIFT_SPAN - LIFT_PILLAR_WIDTH, lift.armHeight + RUNWAY_THICKNESS), 0.2f * unit)
    // Control box on the front pillar
    drawRect(CAR_TRIM, p(LIFT_SPAN + 0.2f, 12f), Size(1.6f * unit, 2.4f * unit))
    drawCircle(CRANE_YELLOW, radius = 0.35f * unit, center = p(LIFT_SPAN + 1f, 11f))
}

/** Wooden crate in the trunk, filled with bills up to how much of the money is left. */
private fun DrawScope.drawCashCrate(p: (Float, Float) -> Offset, unit: Float, money: Int) {
    val fill = money.toFloat() / START_MONEY
    // Bills sticking out of the crate
    if (fill > 0f) {
        repeat(4) { index ->
            val billX = CRATE_X - 2.6f + index * 1.4f
            val top = CRATE_TOP - 0.6f + fill * (1.2f + 0.4f * (index % 2))
            rotate(-12f + index * 8f, pivot = p(billX + 0.8f, CRATE_TOP - 1f)) {
                drawRect(MONEY_GREEN, p(billX, top), Size(1.6f * unit, (top - CRATE_TOP + 1.6f) * unit))
                drawRect(MONEY_GREEN_LIGHT, p(billX + 0.3f, top - 0.3f), Size(1f * unit, 0.4f * unit))
            }
        }
    }
    drawRect(CRATE_WOOD, p(CRATE_X - 3.2f, CRATE_TOP - 0.6f), Size(6.4f * unit, (CRATE_TOP - 7.6f) * unit))
    drawRect(CRATE_WOOD_DARK, p(CRATE_X - 3.2f, CRATE_TOP - 0.6f), Size(6.4f * unit, (CRATE_TOP - 7.6f) * unit), style = Stroke(width = 0.3f * unit))
    drawLine(CRATE_WOOD_DARK, p(CRATE_X - 3.2f, CRATE_TOP - 2.2f), p(CRATE_X + 3.2f, CRATE_TOP - 2.2f), 0.25f * unit)
}

/** The money left as a small label on a pill, e.g. "12000 €". */
private fun DrawScope.drawMoneyLabel(money: Int, anchor: Offset, context: RodeoDrawContext) {
    val assets = context.assets ?: return
    val unit = context.unit
    val layout = assets.textMeasurer.measure(
        assets.moneyText(money),
        TextStyle(fontSize = (MONEY_FONT_SIZE * unit).toSp(), fontWeight = FontWeight.Bold, color = context.colors.onSurface),
    )
    val topLeft = anchor - Offset(layout.size.width / 2f, layout.size.height / 2f)
    drawRoundRect(
        context.colors.surface.copy(alpha = 0.85f),
        topLeft - Offset(0.8f * unit, 0.2f * unit),
        Size(layout.size.width + 1.6f * unit, layout.size.height + 0.4f * unit),
        CornerRadius(layout.size.height / 2f)
    )
    drawText(layout, topLeft = topLeft)
}

/**
 * A golden BBS-style cross-spoke rim: polished lip with rivets, a mesh of spokes crossing each other
 * in V pairs, and a hub cap; [phase] turns it.
 */
private fun DrawScope.drawBbsRim(center: Offset, radius: Float, phase: Float, unit: Float) {
    fun at(angle: Float, distance: Float) = center + Offset(cos(angle), sin(angle)) * distance
    drawCircle(RIM_GOLD_DARK, radius = radius, center = center)
    val spokes = 10
    repeat(spokes) { index ->
        val angle = phase + index * 2f * PI.toFloat() / spokes
        val root = at(angle, radius * 0.3f)
        drawLine(RIM_GOLD, root, at(angle + 0.34f, radius * 0.82f), radius * 0.09f, StrokeCap.Round)
        drawLine(RIM_GOLD, root, at(angle - 0.34f, radius * 0.82f), radius * 0.09f, StrokeCap.Round)
    }
    drawCircle(RIM_GOLD_LIGHT, radius = radius * 0.9f, center = center, style = Stroke(width = radius * 0.18f))
    repeat(16) { index ->
        drawCircle(RIM_GOLD_DARK, radius = 0.06f * unit, center = at(phase + index * 2f * PI.toFloat() / 16, radius * 0.9f))
    }
    drawCircle(RIM_GOLD_LIGHT, radius = radius * 0.3f, center = center)
    drawCircle(RIM_GOLD_DARK, radius = radius * 0.12f, center = center)
}

/** Paint running down the sides and dropping off the sills onto the road; [time] animates the drops. */
private fun DrawScope.drawPaintDrips(p: (Float, Float) -> Offset, unit: Float, time: Float) {
    repeat(9) { index ->
        val dripX = 1.5f + 35f * cloudNoise(index, 11)
        // Skip the wheel arches
        if (dripX in 3.6f..11.4f || dripX in 26.6f..34.4f) return@repeat
        val top = 3f + 3.5f * cloudNoise(index, 12)
        val hang = 0.4f + 0.8f * cloudNoise(index, 13)
        drawLine(CAR_RED, p(dripX, top), p(dripX, -hang), 0.45f * unit, StrokeCap.Round)
        drawCircle(CAR_RED, radius = 0.35f * unit, center = p(dripX, -hang))
        // A drop falling to the road, speeding up
        val cycle = (time * 1.4f + cloudNoise(index, 14)) % 1f
        val dropY = -hang - (CAR_RIM_LIFT - hang) * cycle * cycle
        drawCircle(CAR_RED.copy(alpha = 1f - cycle * 0.5f), radius = 0.3f * unit, center = p(dripX, dropY))
    }
}

/** Gray puffs rising from the hood of the broken-down wreck. */
private fun DrawScope.drawBreakdownSmoke(p: (Float, Float) -> Offset, unit: Float, time: Float, context: RodeoDrawContext) {
    repeat(4) { index ->
        val cycle = (time * 0.9f + index * 0.25f) % 1f
        drawCircle(
            context.colors.outline.copy(alpha = 0.6f * (1f - cycle)),
            radius = (1.2f + 2.5f * cycle) * unit,
            center = p(33f - 4f * cycle + index * 0.5f, 7f + 9f * cycle)
        )
    }
}

/** Sparks spraying back and up from the rims scraping over the road; [phase] animates them. */
private fun DrawScope.drawScrapeSparks(p: (Float, Float) -> Offset, unit: Float, phase: Float) {
    repeat(6) { index ->
        val cycle = (phase * 5f + index * 0.37f) % 1f
        val angle = 2.6f + 0.5f * cloudNoise(index, 3)
        val length = (2f + 3f * cloudNoise(index, 4)) * cycle
        val origin = p(if (index % 2 == 0) 7.5f else 30.5f, -CAR_RIM_LIFT)
        val end = origin + Offset(cos(angle) * length * unit * 2f, -sin(angle) * length * unit)
        drawLine(CAR_SPARK.copy(alpha = 1f - cycle), origin, end, 0.3f * unit, StrokeCap.Round)
    }
}

private fun DrawScope.drawBill(bill: RodeoBillUi, context: RodeoDrawContext) {
    val unit = context.unit
    val center = context.p(bill.x, bill.y)
    rotate(bill.rotation, pivot = center) {
        drawRect(MONEY_GREEN, center - Offset(1f * unit, 0.5f * unit), Size(2f * unit, 1f * unit))
        drawCircle(MONEY_GREEN_LIGHT, radius = 0.3f * unit, center = center)
    }
}

private fun DrawScope.drawPuddle(puddle: RodeoPuddleUi, context: RodeoDrawContext) {
    val unit = context.unit
    val width = puddle.size * 2.2f
    drawOval(
        CAR_RED_DARK.copy(alpha = 0.85f),
        topLeft = context.p(puddle.x - width / 2f, 0.25f),
        size = Size(width * unit, puddle.size * 0.5f * unit)
    )
}

/**
 * The scrapyard behind the track, [x] its left end: a rusty corrugated fence topped with barbed
 * wire, crushed car cubes stacked up, the junk pile the wreck is thrown onto (tyres, doors, pipes,
 * an old washing machine sticking out of it), a stack of tyres and a crane with a magnet.
 */
private fun DrawScope.drawScrapyard(x: Float, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(x + dx, y)
    val line = context.colors.onSurface

    drawCorrugatedFence(::p, unit, line)

    // Oil stain on the ground
    drawOval(CAR_TRIM.copy(alpha = 0.5f), p(44f, 0.4f), Size(9f * unit, 0.8f * unit))

    // Crushed car cubes stacked on the left, three rows
    val cubeColors = listOf(SCRAP_BLUE, CAR_RUST, JUNK_GREEN, SCRAP_GRAY, CAR_RED_DARK, SCRAP_BLUE, CAR_RUST, SCRAP_GRAY)
    var cube = 0
    listOf(3, 2, 1).forEachIndexed { row, count ->
        repeat(count) { column ->
            val left = 0.5f + row * 2.6f + column * 5.4f
            val bottom = row * 4.2f
            val color = cubeColors[cube++ % cubeColors.size]
            drawRect(color, p(left, bottom + 4f), Size(5f * unit, 4f * unit))
            drawRect(line.copy(alpha = 0.6f), p(left, bottom + 4f), Size(5f * unit, 4f * unit), style = Stroke(width = 0.2f * unit))
            // Crumpled sheet metal lines and a squashed wheel
            drawLine(SCRAP_DARK, p(left + 0.5f, bottom + 2.8f), p(left + 4.4f, bottom + 3.3f), 0.2f * unit)
            drawLine(SCRAP_DARK, p(left + 0.6f, bottom + 1.6f), p(left + 3.8f, bottom + 1.2f), 0.2f * unit)
            drawOval(CAR_TRIM, p(left + 3.2f, bottom + 1.2f), Size(1.6f * unit, 0.9f * unit))
        }
    }

    // The junk pile, flat on top where the wreck lands
    drawPath(
        polygonPath(::p, 14f to 0f, 17f to 5f, 20f to 9f, 23f to PILE_TOP, 37f to PILE_TOP, 41f to 9.5f, 46f to 5f, 52f to 0f),
        SCRAP_DARK
    )
    // Junk sticking out of it: panels, pipes, a door, tyres, a washing machine, a steering wheel
    repeat(9) { index ->
        val junkX = 17f + 31f * cloudNoise(index, 21)
        val top = pileHeightAt(junkX) - 0.6f
        val width = 1.5f + 2.5f * cloudNoise(index, 22)
        val color = listOf(SCRAP_GRAY, CAR_RUST, SCRAP_BLUE, JUNK_GREEN)[index % 4]
        rotate(-35f + 70f * cloudNoise(index, 23), pivot = p(junkX, top)) {
            drawRect(color, p(junkX - width / 2f, top + 0.9f), Size(width * unit, 1.3f * unit))
        }
    }
    drawLine(SCRAP_GRAY, p(19f, 6f), p(24f, 10.5f), 0.6f * unit, StrokeCap.Round)
    drawLine(CAR_RUST, p(40f, 8f), p(45.5f, 10f), 0.7f * unit, StrokeCap.Round)
    // A car door with its window frame
    rotate(-18f, pivot = p(44f, 5f)) {
        drawRect(SCRAP_BLUE, p(42f, 7.5f), Size(5f * unit, 3.2f * unit))
        drawRect(line.copy(alpha = 0.6f), p(42.6f, 9.8f), Size(3.2f * unit, 1.6f * unit), style = Stroke(width = 0.2f * unit))
    }
    // An old washing machine
    drawRect(APPLIANCE_WHITE, p(26f, 5.6f), Size(3.6f * unit, 4f * unit))
    drawCircle(SCRAP_GRAY, radius = 1.1f * unit, center = p(27.8f, 3.4f))
    drawCircle(SCRAP_DARK, radius = 0.6f * unit, center = p(27.8f, 3.4f))
    // Half-buried tyres and a steering wheel
    listOf(21f to 3.5f, 35f to 5.5f, 48f to 1.8f).forEach { (tyreX, tyreY) -> drawTyre(p(tyreX, tyreY), unit) }
    drawCircle(CAR_TRIM, radius = 1.2f * unit, center = p(32f, 8.5f), style = Stroke(width = 0.35f * unit))
    drawLine(CAR_TRIM, p(30.8f, 8.5f), p(33.2f, 8.5f), 0.3f * unit)

    // Stack of tyres by the crane
    repeat(5) { index -> drawTyre(p(56f, 1.1f + index * 1.9f), unit, flat = true) }

    drawCrane(::p, unit, line)
}

/** Height of the junk pile's outline at [dx] from the scrapyard's left end. */
private fun pileHeightAt(dx: Float): Float = when {
    dx < 14f || dx > 52f -> 0f
    dx < 23f -> PILE_TOP * (dx - 14f) / 9f
    dx <= 37f -> PILE_TOP
    else -> PILE_TOP * (52f - dx) / 15f
}

/** A tyre seen from the side, or lying [flat] in a stack. */
private fun DrawScope.drawTyre(center: Offset, unit: Float, flat: Boolean = false) {
    if (flat) {
        drawRoundRect(CAR_TRIM, center - Offset(2.6f * unit, 0.9f * unit), Size(5.2f * unit, 1.8f * unit), CornerRadius(0.9f * unit))
        drawLine(SCRAP_GRAY, center - Offset(1.6f * unit, 0f), center + Offset(1.6f * unit, 0f), 0.2f * unit)
    } else {
        drawCircle(CAR_TRIM, radius = 1.9f * unit, center = center)
        drawCircle(SCRAP_GRAY, radius = 0.8f * unit, center = center)
    }
}

/** Rusty corrugated sheets of different heights along the back, with barbed wire on top. */
private fun DrawScope.drawCorrugatedFence(p: (Float, Float) -> Offset, unit: Float, line: Color) {
    val sheetWidth = 6f
    var left = 0f
    var index = 0
    while (left < YARD_WIDTH) {
        val height = 15f + 3f * cloudNoise(index, 31)
        val color = listOf(SCRAP_GRAY, SHEET_LIGHT, CAR_RUST, SCRAP_GRAY, SHEET_LIGHT)[index % 5]
        val tilt = -3f + 6f * cloudNoise(index, 32)
        rotate(tilt, pivot = p(left + sheetWidth / 2f, 0f)) {
            drawRect(color, p(left, height), Size(sheetWidth * unit, height * unit))
            // Ridges
            var ridge = left + 0.75f
            while (ridge < left + sheetWidth) {
                drawLine(line.copy(alpha = 0.18f), p(ridge, height), p(ridge, 0f), 0.25f * unit)
                ridge += 1.5f
            }
            // Rust running down from the top
            drawRect(CAR_RUST.copy(alpha = 0.5f), p(left + 1f + 3f * cloudNoise(index, 33), height), Size(1.2f * unit, (2f + 4f * cloudNoise(index, 34)) * unit))
        }
        left += sheetWidth - 0.3f
        index++
    }
    // Barbed wire along the top
    val wireY = 18.8f
    drawLine(line.copy(alpha = 0.7f), p(0f, wireY), p(YARD_WIDTH, wireY), 0.15f * unit)
    var barb = 0.8f
    while (barb < YARD_WIDTH) {
        drawLine(line.copy(alpha = 0.7f), p(barb - 0.4f, wireY - 0.4f), p(barb + 0.4f, wireY + 0.4f), 0.15f * unit)
        drawLine(line.copy(alpha = 0.7f), p(barb - 0.4f, wireY + 0.4f), p(barb + 0.4f, wireY - 0.4f), 0.15f * unit)
        barb += 2f
    }
}

/** A yellow crane on the right: lattice tower, cab, boom over the pile, and a magnet on its cable. */
private fun DrawScope.drawCrane(p: (Float, Float) -> Offset, unit: Float, line: Color) {
    val left = 62f
    val right = 65f
    val top = 40f
    // Lattice tower
    drawLine(CRANE_YELLOW, p(left, 0f), p(left, top), 0.6f * unit)
    drawLine(CRANE_YELLOW, p(right, 0f), p(right, top), 0.6f * unit)
    var brace = 0f
    while (brace < top) {
        drawLine(CRANE_YELLOW, p(left, brace), p(right, brace + 3f), 0.3f * unit)
        drawLine(CRANE_YELLOW, p(right, brace), p(left, brace + 3f), 0.3f * unit)
        brace += 3f
    }
    // Cab
    drawRect(CRANE_YELLOW, p(right - 0.5f, top - 3f), Size(4f * unit, 4f * unit))
    drawRect(SHEET_LIGHT, p(right + 0.5f, top - 3.6f), Size(2.2f * unit, 1.8f * unit))
    // Boom reaching over the pile, with its counterweight
    drawLine(CRANE_YELLOW, p(right + 3f, top + 1f), p(38f, top + 4f), 0.8f * unit, StrokeCap.Round)
    drawLine(CRANE_YELLOW, p(left, top + 1f), p(38f, top + 4f), 0.3f * unit)
    drawRect(CRANE_DARK, p(right + 2.5f, top + 2.5f), Size(3f * unit, 2.5f * unit))
    // Cable and magnet
    drawLine(line, p(40f, top + 3.8f), p(40f, 31f), 0.15f * unit)
    drawOval(SCRAP_DARK, p(37.5f, 31f), Size(5f * unit, 1.6f * unit))
    drawRect(CRANE_DARK, p(39.3f, 31.8f), Size(1.4f * unit, 0.9f * unit))
}
