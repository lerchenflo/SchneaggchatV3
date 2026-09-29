package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.GROUND_OFFSET_UNITS
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.WORLD_HEIGHT_UNITS
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoDrawContext
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoVehicleAssets
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawPizza
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawPizzaOven
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPizzaOvenUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPizzaUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.bull.RodeoBullUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cablecar.RodeoCableCarUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.candybus.RodeoCandyBusUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.candybus.RodeoStanislausUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.fordescort.RodeoBillUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.fordescort.RodeoCarLiftUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.fordescort.RodeoEscortPart
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.fordescort.RodeoFlyingPartUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.fordescort.RodeoFordEscortUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.fordescort.RodeoPuddleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.milktruck.RodeoMilkCanUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.milktruck.RodeoMilkTruckUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.plane.RodeoBuildingUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.plane.RodeoPlaneUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pocketbike.RodeoPocketBikeUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pocketbike.RodeoSmokePuffUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rocket.RodeoPlanetUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rocket.RodeoRocketUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.shoppingcart.RodeoShoppingCartUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.tractor.RodeoTractorUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.train.RodeoSmokeUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.train.RodeoTrainUi
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_candy_bus_sign
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_car_money
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative
import schneaggchatv3mp.composeapp.generated.resources.rodeo_stanislaus

// IDE previews of every vehicle (and the pizza oven) in a typical moment, drawn straight from its render model on a bare
// track (no horse, no engine). Positions are in world units like in the game.

private const val MONEY_PLACEHOLDER = "{amount}"

/** Draws [vehicles] layer by layer over a ground line, the same way the track canvas does. */
@Composable
private fun RodeoVehiclePreviewTrack(vararg vehicles: RodeoVehicleUi) = RodeoPreviewTrack { context ->
    RodeoLayer.entries.forEach { layer -> vehicles.forEach { drawVehicle(it, layer, context) } }
}

/** A bare track with a ground line; [content] draws on it in world units through the context. */
@Composable
private fun RodeoPreviewTrack(content: DrawScope.(RodeoDrawContext) -> Unit) {
    SchneaggchatTheme {
        val colors = MaterialTheme.colorScheme
        val textMeasurer = rememberTextMeasurer()
        val stanislaus = imageResource(Res.drawable.rodeo_stanislaus)
        val snail = imageResource(Res.drawable.icon_schneagg_alternative)
        val candySign = stringResource(Res.string.games_schneaggrodeo_candy_bus_sign)
        val moneyTemplate = stringResource(Res.string.games_schneaggrodeo_car_money, MONEY_PLACEHOLDER)
        val assets = remember(stanislaus, snail, candySign, moneyTemplate, textMeasurer) {
            RodeoVehicleAssets(
                stanislaus = stanislaus,
                snail = snail,
                candySign = candySign,
                moneyText = { amount -> moneyTemplate.replace(MONEY_PLACEHOLDER, amount.toString()) },
                textMeasurer = textMeasurer,
            )
        }
        Canvas(Modifier.size(width = 560.dp, height = 240.dp).background(colors.surface)) {
            val unit = size.height / WORLD_HEIGHT_UNITS
            val groundY = size.height - GROUND_OFFSET_UNITS * unit
            val context = RodeoDrawContext(groundY = groundY, unit = unit, colors = colors, assets = assets)
            drawRect(colors.surfaceContainer, topLeft = Offset(0f, groundY), size = size.copy(height = size.height - groundY))
            drawLine(colors.outline, Offset(0f, groundY), Offset(size.width, groundY), 0.3f * unit)
            content(context)
        }
    }
}

@Preview
@Composable
private fun CandyBusCapturePreview() = RodeoVehiclePreviewTrack(
    RodeoCandyBusUi(
        x = 20f,
        wheelPhase = 0.4f,
        doorOpen = 1f,
        rearDoorsOpen = 0f,
        hasDriver = true,
        stanislaus = listOf(RodeoStanislausUi(x = 59f, pull = 0.45f), RodeoStanislausUi(x = 120f, pull = 0f)),
        lassoHint = null,
    )
)

@Preview
@Composable
private fun CandyBusBoardingPreview() = RodeoVehiclePreviewTrack(
    RodeoCandyBusUi(
        x = 40f,
        wheelPhase = 0f,
        doorOpen = 0f,
        rearDoorsOpen = 1f,
        hasDriver = false,
        stanislaus = emptyList(),
        lassoHint = null,
    )
)

@Preview
@Composable
private fun FordEscortDrivingPreview() = RodeoVehiclePreviewTrack(
    RodeoFordEscortUi(
        x = 60f,
        y = 0f,
        rotation = 0.8f,
        hasDriver = true,
        sparks = 0.3f,
        time = 0.4f,
        rimPhase = 0.6f,
        money = 7_400,
        smoking = false,
        dripping = true,
        onPile = false,
        yardX = null,
        lift = null,
        onLift = false,
        partsGone = emptySet(),
        flyingParts = emptyList(),
        bills = listOf(
            RodeoBillUi(x = 55f, y = 18f, rotation = 20f),
            RodeoBillUi(x = 46f, y = 14f, rotation = -35f),
            RodeoBillUi(x = 38f, y = 8f, rotation = 60f),
            RodeoBillUi(x = 30f, y = 0.3f, rotation = 0f),
        ),
        puddles = listOf(RodeoPuddleUi(x = 50f, size = 1.4f), RodeoPuddleUi(x = 36f, size = 2f), RodeoPuddleUi(x = 20f, size = 1f)),
        lassoHint = null,
    )
)

@Preview
@Composable
private fun FordEscortScrapyardPreview() = RodeoVehiclePreviewTrack(
    RodeoFordEscortUi(
        x = 81f,
        y = 12f,
        rotation = 14f,
        hasDriver = false,
        sparks = null,
        time = 0f,
        rimPhase = 0f,
        money = null,
        smoking = false,
        dripping = false,
        onPile = true,
        yardX = 70f,
        lift = null,
        onLift = false,
        partsGone = emptySet(),
        flyingParts = emptyList(),
        bills = emptyList(),
        puddles = emptyList(),
        lassoHint = null,
    )
)

/** The other ending: up on the car lift, half stripped, a rim flying over to the junk pile. */
@Preview
@Composable
private fun FordEscortCarLiftPreview() = RodeoVehiclePreviewTrack(
    RodeoFordEscortUi(
        x = 25f,
        y = 10.2f,
        rotation = 0f,
        hasDriver = false,
        sparks = null,
        time = 0f,
        rimPhase = 0.3f,
        money = null,
        smoking = false,
        dripping = false,
        onPile = false,
        yardX = 74f,
        lift = RodeoCarLiftUi(x = 20f, armHeight = 9f),
        onLift = true,
        partsGone = setOf(RodeoEscortPart.DOOR, RodeoEscortPart.WINDSHIELD, RodeoEscortPart.SEATS, RodeoEscortPart.CRATE, RodeoEscortPart.REAR_RIM),
        flyingParts = listOf(
            RodeoFlyingPartUi(RodeoEscortPart.REAR_RIM, x = 80f, y = 30f, rotation = 120f, landed = false),
            RodeoFlyingPartUi(RodeoEscortPart.DOOR, x = 100f, y = 13f, rotation = -12f, landed = true),
            RodeoFlyingPartUi(RodeoEscortPart.SEATS, x = 106f, y = 13f, rotation = 30f, landed = true),
            RodeoFlyingPartUi(RodeoEscortPart.WINDSHIELD, x = 95f, y = 13f, rotation = 80f, landed = true),
            RodeoFlyingPartUi(RodeoEscortPart.CRATE, x = 110f, y = 13f, rotation = 8f, landed = true),
        ),
        bills = emptyList(),
        puddles = emptyList(),
        lassoHint = null,
    )
)

/** Car lift ending done: the bare hull on the lift, everything else on the junk pile. */
@Preview
@Composable
private fun FordEscortBareHullPreview() = RodeoVehiclePreviewTrack(
    RodeoFordEscortUi(
        x = 25f,
        y = 8f,
        rotation = 0f,
        hasDriver = false,
        sparks = null,
        time = 0f,
        rimPhase = 0f,
        money = null,
        smoking = false,
        dripping = false,
        onPile = false,
        yardX = 74f,
        lift = RodeoCarLiftUi(x = 20f, armHeight = 9f),
        onLift = true,
        partsGone = RodeoEscortPart.entries.toSet(),
        flyingParts = RodeoEscortPart.entries.mapIndexed { index, part ->
            RodeoFlyingPartUi(part, x = 97f + index * 2f, y = 13f, rotation = -40f + index * 25f, landed = true)
        },
        bills = emptyList(),
        puddles = emptyList(),
        lassoHint = null,
    )
)

@Preview
@Composable
private fun MilkTruckPreview() = RodeoVehiclePreviewTrack(
    RodeoMilkTruckUi(
        x = 20f,
        wheelPhase = 0.5f,
        cansInRack = 2,
        cans = listOf(
            RodeoMilkCanUi(x = 90f, y = 30f, rotation = 40f, spilled = false),
            RodeoMilkCanUi(x = 120f, y = 1f, rotation = 90f, spilled = true),
        ),
        lassoHint = RodeoLassoHintUi(x = 42f, y = 23f),
    )
)

@Preview
@Composable
private fun TrainPreview() = RodeoVehiclePreviewTrack(
    RodeoTrainUi(
        x = 20f,
        wheelPhase = 0.3f,
        bridges = listOf(110f),
        smoke = listOf(RodeoSmokeUi(x = 70f, y = 24f, progress = 0.2f), RodeoSmokeUi(x = 62f, y = 30f, progress = 0.6f)),
        lassoHint = null,
    )
)

@Preview
@Composable
private fun PlanePreview() = RodeoVehiclePreviewTrack(
    RodeoPlaneUi(
        x = 60f,
        y = 30f,
        rotation = -8f,
        propellerPhase = 0.7f,
        visible = true,
        hasPilot = true,
        ladderDown = false,
        buildings = listOf(
            RodeoBuildingUi(x = 10f, width = 18f, height = 22f, seed = 1, cloudBottom = null),
            RodeoBuildingUi(x = 110f, width = 14f, height = 30f, seed = 2, cloudBottom = 45f),
        ),
        lassoHint = null,
    )
)

@Preview
@Composable
private fun BullPreview() = RodeoVehiclePreviewTrack(
    RodeoBullUi(x = 50f, gaitPhase = 1.2f, pitch = -12f, rider = true, lean = 0.3f, rideProgress = 0.55f, lassoHint = null)
)

@Preview
@Composable
private fun LawnTractorPreview() = RodeoVehiclePreviewTrack(
    RodeoTractorUi(x = 50f, rotation = -3f, wheelPhase = 0.8f, partsLost = 0, wrecked = false, exhaust = true, debris = emptyList(), lassoHint = null)
)

@Preview
@Composable
private fun CableCarPreview() = RodeoVehiclePreviewTrack(
    RodeoCableCarUi(x = 60f, floor = 30f, levelCable = true, lassoHint = null)
)


@Preview
@Composable
private fun RocketPreview() = RodeoVehiclePreviewTrack(
    RodeoRocketUi(
        x = 70f,
        y = 25f,
        tilt = 20f,
        thrust = 1f,
        flicker = 0.4f,
        visible = true,
        passengers = true,
        planets = listOf(RodeoPlanetUi(x = 150f, y = 55f, radius = 8f, kind = 3, seed = 2)),
    )
)

/** Full throttle in its cloud of two-stroke smoke. */
@Preview
@Composable
private fun PocketBikePreview() = RodeoVehiclePreviewTrack(
    RodeoPocketBikeUi(
        x = 70f,
        rotation = 1.5f,
        wheelPhase = 0.5f,
        hasRider = true,
        smoke = List(24) { index ->
            val life = index / 24f
            RodeoSmokePuffUi(
                x = 70f - 2f - index * 2.4f,
                y = 1.5f + life * 9f + (index % 3) * 0.8f,
                radius = 1.2f * (1f + 3f * life),
                alpha = 1f - life,
            )
        },
        lassoHint = null,
    )
)

/** The engine died: tipped over, still smoking. */
@Preview
@Composable
private fun PocketBikeFallenPreview() = RodeoVehiclePreviewTrack(
    RodeoPocketBikeUi(
        x = 70f,
        rotation = 80f,
        wheelPhase = 0f,
        hasRider = false,
        smoke = List(6) { index -> RodeoSmokePuffUi(x = 68f - index * 3f, y = 3f + index * 2f, radius = 2f + index, alpha = 1f - index / 6f) },
        lassoHint = null,
    )
)

/** The pizza oven by the roadside with a pizza in it, and an empty one next to a pizza in the lasso. */
@Preview
@Composable
private fun PizzaOvenPreview() = RodeoPreviewTrack { context ->
    drawPizzaOven(RodeoPizzaOvenUi(x = 30f, hasPizza = true), context, time = 0.3f)
    drawPizzaOven(RodeoPizzaOvenUi(x = 110f, hasPizza = false), context, time = 0.7f)
    drawPizza(RodeoPizzaUi(x = 95f, y = 22f), context)
}

/** Shoved along with a pile of snails in the basket, and tipped over after the last shove. */
@Preview
@Composable
private fun ShoppingCartPreview() = RodeoVehiclePreviewTrack(
    RodeoShoppingCartUi(x = 40f, rotation = 1f, wheelPhase = 0.5f, snails = 7, lassoHint = null),
    RodeoShoppingCartUi(x = 120f, rotation = 75f, wheelPhase = 0f, snails = 0, lassoHint = null),
)
