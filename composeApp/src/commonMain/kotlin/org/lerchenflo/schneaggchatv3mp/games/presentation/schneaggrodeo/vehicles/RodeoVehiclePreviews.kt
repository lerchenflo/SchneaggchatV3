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
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoVehicleAssets
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawPizza
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawPizzaOven
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawVehicles
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPizzaOvenUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoStopKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoTimeOfDay
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoWeather
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.cave
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawKiosk
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawCaveBackdrop
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawSeaBackdrop
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawTimeOfDay
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWaterOverlay
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWeather
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.balloon.RodeoBalloonUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.cow.RodeoCowUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.flamingo.RodeoFlamingoUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.minecart.RodeoMineCartUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.oiltanker.RodeoOilTankerUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rowboat.RodeoFishUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.rowboat.RodeoRowboatUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.uboat.RodeoSeaThingUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.uboat.RodeoUBoatUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.uboat.UBOAT_CAMERA_DEEPEST
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
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFireflyUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoHorsePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoHorseColors
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawFirefly
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawGhostly
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawHorseAndRider
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawRainbow
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.drill.RodeoDrillFindUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.drill.RodeoDrillUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.drill.RodeoTunnelUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pirateship.RodeoCannonballUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pirateship.RodeoPirateShipUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.pirateship.RodeoTreasureChestUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.trafficjam.JamCarStyle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.trafficjam.RodeoJamCarUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.trafficjam.RodeoTrafficJamUi
import androidx.compose.ui.graphics.drawscope.translate
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.carriage.RodeoGoldenCarriageUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.helicopter.HELI_ABOVE_HOOVES
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.helicopter.HELI_CABIN_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.helicopter.HELI_SLING_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.helicopter.RAINBOW_LIFT
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.helicopter.RodeoHelicopterUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.RodeoLayer
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawVehicle
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoDeepThingUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMapWayUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoDeepKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoMap
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEABED_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEA_CAMERA_DOWN
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.TERRAIN_STEP
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.TerrainFeature
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.terrainHeightAt
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawDeepThing
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawGround
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawMapWay
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_candy_bus_sign
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_car_money
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative
import schneaggchatv3mp.composeapp.generated.resources.rodeo_stanislaus
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoBridgeUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoTerrainUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.BRIDGE_LENGTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.BRIDGE_LOAD_DIP
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HOOVES_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawBridge
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawGorge
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render.drawWithGorges

// IDE previews of every vehicle (and the pizza oven) in a typical moment, drawn straight from its render model on a bare
// track (no horse, no engine). Positions are in world units like in the game.

private const val MONEY_PLACEHOLDER = "{amount}"

/** Draws [vehicles] layer by layer over a ground line, the same way the track canvas does. */
@Composable
private fun RodeoVehiclePreviewTrack(vararg vehicles: RodeoVehicleUi) = RodeoPreviewTrack { context ->
    drawVehicles(vehicles.toList(), context)
}

/**
 * A bare track with a ground line; [content] draws on it in world units through the context. The
 * ground follows [ground] (hills, see engine/RodeoTerrain); the [sea] has no ground but water with the
 * seabed below and its surface over everything. The view looks down by [cameraDown] (negative) like in
 * the game. [backdrop] goes behind the ground, on the flat; [bridges] cut their gorges into it.
 * Without [groundLine] there is no ground at all (the rainbow draws its own track).
 */
@Composable
internal fun RodeoPreviewTrack(
    enclosed: Boolean = false,
    ground: ((Float) -> Float)? = null,
    sea: Boolean = false,
    cameraDown: Float = 0f,
    backdrop: DrawScope.(RodeoDrawContext) -> Unit = {},
    bridges: List<RodeoBridgeUi> = emptyList(),
    groundLine: Boolean = true,
    content: DrawScope.(RodeoDrawContext) -> Unit,
) {
    SchneaggchatTheme {
        val colors = if (enclosed) MaterialTheme.colorScheme.cave() else MaterialTheme.colorScheme
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
            val context = RodeoDrawContext(groundY = groundY, unit = unit, colors = colors, assets = assets, ground = ground ?: { 0f })
            translate(top = cameraDown * unit) {
                backdrop(context.flat())
                when {
                    !groundLine -> Unit
                    sea -> drawSeaBackdrop(distance = 0f, context = context.flat())
                    ground != null -> {
                        drawWithGorges(bridges, context) { drawGround(context, distance = 0f, enclosed = enclosed) }
                        bridges.forEach {
                            drawGorge(it, context, distance = 0f)
                            drawBridge(it, context)
                        }
                    }
                    else -> {
                        drawRect(colors.surfaceContainer, topLeft = Offset(0f, groundY), size = size.copy(height = size.height - groundY))
                        drawLine(colors.outline, Offset(0f, groundY), Offset(size.width, groundY), 0.3f * unit)
                    }
                }
                content(context)
                if (sea) drawWaterOverlay(distance = 0f, context = context.flat())
            }
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
        gearDown = false,
        airportX = null,
        buildings = listOf(
            RodeoBuildingUi(x = 10f, width = 18f, height = 22f, seed = 1, cloudBottom = null),
            RodeoBuildingUi(x = 110f, width = 14f, height = 30f, seed = 2, cloudBottom = 45f),
        ),
        lassoHint = null,
    )
)

@Preview
@Composable
private fun PlaneAirportPreview() = RodeoVehiclePreviewTrack(
    RodeoPlaneUi(
        x = 48f,
        y = 2.5f,
        rotation = 0f,
        propellerPhase = 0.7f,
        visible = true,
        hasPilot = false,
        gearDown = true,
        airportX = 20f,
        buildings = emptyList(),
        lassoHint = RodeoLassoHintUi(59f, 18f),
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

@Preview
@Composable
private fun BalloonPreview() = RodeoVehiclePreviewTrack(
    RodeoBalloonUi(x = 60f, y = 20f, sway = 0.4f, burning = true, hasPilot = true, ladderDown = false, lassoHint = null),
    RodeoBalloonUi(x = 130f, y = 26f, sway = 1.2f, burning = false, hasPilot = false, ladderDown = true, lassoHint = null),
)

@Preview
@Composable
private fun CowPreview() = RodeoVehiclePreviewTrack(
    RodeoCowUi(x = 40f, gait = 0.8f, hasRider = true, ring = 0.3f, lassoHint = null),
    RodeoCowUi(x = 120f, gait = 2f, hasRider = false, ring = null, lassoHint = RodeoLassoHintUi(x = 131f, y = 18f)),
)

/** The boats on the sea's surface: the rowboat with a fish on the line, the U-boat surfaced, the flamingo. */
@Preview
@Composable
private fun SeaVehiclesPreview() = RodeoPreviewTrack(sea = true, cameraDown = SEA_CAMERA_DOWN) { context ->
    val vehicles = listOf(
        RodeoRowboatUi(
            x = 5f,
            rowPhase = 0.6f,
            fish = listOf(RodeoFishUi(x = 50f, y = 18f, rotation = -30f)),
            lassoHint = null,
        ),
        RodeoUBoatUi(
            x = 70f,
            y = 0f,
            propeller = 1f,
            crewAboard = false,
            boom = null,
            pearls = emptyList(),
            mines = emptyList(),
            lassoHint = null,
        ),
        RodeoFlamingoUi(x = 140f, lift = 3f, bob = 0f, lassoHint = null),
    )
    drawVehicles(vehicles, context)
}

/** The U-boat deep down by the seabed: a pearl, mines on their chains and a treasure chest in the sand. */
@Preview
@Composable
private fun UBoatDeepPreview() = RodeoPreviewTrack(sea = true, cameraDown = UBOAT_CAMERA_DEEPEST) { context ->
    drawDeepThing(RodeoDeepThingUi(x = 60f, y = -8f, kind = RodeoDeepKind.FISH, seed = 1, time = 1f), context)
    val uBoat = RodeoUBoatUi(
        x = 8f,
        y = -22f,
        propeller = 1f,
        crewAboard = true,
        boom = null,
        pearls = listOf(RodeoSeaThingUi(x = 70f, y = -14f)),
        mines = listOf(RodeoSeaThingUi(x = 95f, y = -18f), RodeoSeaThingUi(x = 140f, y = -6f)),
        treasures = listOf(RodeoSeaThingUi(x = 120f, y = SEABED_Y + 1.5f)),
        lassoHint = null,
    )
    drawVehicles(listOf(uBoat), context)
}

@Preview
@Composable
private fun OilTankerPreview() = RodeoVehiclePreviewTrack(
    RodeoOilTankerUi(x = 10f, smoke = 0.7f, horn = 0.4f, lassoHint = null),
)

@Preview
@Composable
private fun MineCartPreview() = RodeoVehiclePreviewTrack(
    RodeoMineCartUi(x = 50f, wheelPhase = 0.4f, railOffset = 3f, sparks = 0.3f, lassoHint = null),
)

/** The Käsknöpfle kiosk on a snowy night: lantern light around the horse's spot. */
@Preview
@Composable
private fun KioskNightPreview() = RodeoPreviewTrack { context ->
    drawKiosk(RodeoPizzaOvenUi(x = 60f, hasPizza = true, kind = RodeoStopKind.KIOSK), context, time = 0.5f)
    drawWeather(RodeoWeather.SNOW, time = 3f, unit = context.unit)
    drawTimeOfDay(RodeoTimeOfDay.NIGHT, lantern = context.p(40f, 14f), unit = context.unit)
}

/** Four cars of the traffic jam, bumper to bumper. */
private val PREVIEW_JAM_CARS = listOf(
    RodeoJamCarUi(offset = 0f, style = JamCarStyle.SEDAN, color = 0, seed = 1),
    RodeoJamCarUi(offset = 40f, style = JamCarStyle.VAN, color = 4, seed = 2),
    RodeoJamCarUi(offset = 82f, style = JamCarStyle.HATCHBACK, color = 2, seed = 3),
    RodeoJamCarUi(offset = 116f, style = JamCarStyle.SEDAN, color = 1, seed = 4),
)

@Preview
@Composable
private fun TrafficJamPreview() = RodeoVehiclePreviewTrack(
    RodeoTrafficJamUi(
        x = 20f,
        cars = PREVIEW_JAM_CARS,
        time = 0.05f,
        lassoHint = RodeoLassoHintUi(x = 38f, y = 19f),
    )
)

@Preview
@Composable
private fun PirateShipPreview() = RodeoVehiclePreviewTrack(
    RodeoPirateShipUi(
        x = 10f,
        time = 0.4f,
        muzzleSmoke = 0.3f,
        cannonballs = listOf(RodeoCannonballUi(x = 95f, y = 16f)),
        chests = listOf(RodeoTreasureChestUi(x = 110f, y = 18f, seed = 3)),
        lassoHint = null,
    )
)

@Preview
@Composable
private fun DrillPreview() = RodeoVehiclePreviewTrack(
    RodeoDrillUi(
        x = 40f, y = 0f, drillPhase = 0.3f, hatInHatch = false, earthVisible = false,
        tunnel = emptyList(), finds = emptyList(), shake = 0f,
        lassoHint = RodeoLassoHintUi(x = 63f, y = 23f),
    )
)

/** Digging deep: the view pans down like in the game, showing the tunnel, gold and a rock. */
@Preview
@Composable
private fun DrillDiggingPreview() = RodeoPreviewTrack(
    enclosed = true,
    cameraDown = -26f,
    backdrop = { drawCaveBackdrop(distance = 40f, context = it) },
) { context ->
    val drill = RodeoDrillUi(
        x = 20f, y = -26f, drillPhase = 0.6f, hatInHatch = true, earthVisible = true,
        tunnel = List(10) { RodeoTunnelUi(x = 4f + it * 2.5f, y = -10f - it * 0.9f) },
        finds = listOf(
            RodeoDrillFindUi(x = 70f, y = -12f, rock = false, seed = 1),
            RodeoDrillFindUi(x = 90f, y = -6f, rock = true, seed = 2),
            RodeoDrillFindUi(x = 110f, y = -17f, rock = false, seed = 3),
        ),
        shake = 0f,
        lassoHint = null,
    )
    drawVehicles(listOf(drill), context)
}

/** A night run after the rain: the rainbow, fireflies and a ghost horse galloping along. */
@Preview
@Composable
private fun NightWondersPreview() = RodeoPreviewTrack { context ->
    val unit = context.unit
    drawRainbow(x = 50f, context = context)
    val colors = context.colors
    val horseColors = RodeoHorseColors(
        body = colors.onSurface,
        shirt = colors.primary,
        glow = colors.primary,
        canopy = colors.secondary,
        friendShirt = colors.tertiary,
        headRing = colors.surfaceContainer,
        rope = colors.tertiary,
        pants = colors.secondary,
    )
    val pose = RodeoHorsePose(
        height = 0f, gaitPhase = 1f, airborne = false, riderLean = 0f, pitchDegrees = 0f,
        pivotX = 12f, pivotY = 12f, hindLegScale = 1f, frontLegFold = 0f, hatLift = 0f, glow = 0f,
        hasRider = false, coat = 2, ghost = true,
    )
    drawGhostly(context.p(100f + 15f, 14f), unit, time = 1f) {
        drawHorseAndRider(left = 100f * unit, groundY = context.groundY, unit = unit, pose = pose, colors = horseColors)
    }
    drawTimeOfDay(RodeoTimeOfDay.NIGHT, lantern = context.p(40f, 14f), unit = unit)
    listOf(RodeoFireflyUi(20f, 18f, 1f), RodeoFireflyUi(70f, 24f, 0.5f), RodeoFireflyUi(140f, 16f, 0.8f)).forEach { drawFirefly(it, context) }
}

/** A gorge with its plank bridge, bending under the horse galloping across. */
@Preview
@Composable
private fun BridgePreview() {
    val terrain = RodeoTerrainUi(
        xs = listOf(-70f, 0f, 70f, 140f, 210f),
        hs = List(5) { 4f },
        bridges = listOf(RodeoBridgeUi(x = 30f, dip = BRIDGE_LOAD_DIP, loadAt = 0.4f)),
    )
    RodeoPreviewTrack(ground = terrain::heightAt, bridges = terrain.bridges, cameraDown = -12f) { context ->
        val colors = context.colors
        val horseColors = RodeoHorseColors(
            body = colors.onSurface,
            shirt = colors.primary,
            glow = colors.primary,
            canopy = colors.secondary,
            friendShirt = colors.tertiary,
            headRing = colors.surfaceContainer,
            rope = colors.tertiary,
            pants = colors.secondary,
        )
        val pose = RodeoHorsePose(
            height = 0f, gaitPhase = 1f, airborne = false, riderLean = 0f, pitchDegrees = 0f,
            pivotX = 12f, pivotY = 12f, hindLegScale = 1f, frontLegFold = 0f, hatLift = 0f, glow = 0f,
            maxLives = 4, lives = 3f,
        )
        val hooves = 30f + 0.4f * BRIDGE_LENGTH
        drawHorseAndRider(left = (hooves - HOOVES_X) * context.unit, groundY = context.groundYAt(hooves), unit = context.unit, pose = pose, colors = horseColors)
    }
}

/** Hovering by with the empty sling under its hook, waiting for the lasso. */
@Preview
@Composable
private fun HelicopterPreview() = RodeoVehiclePreviewTrack(
    RodeoHelicopterUi(x = 60f, y = 36f, rotor = 0.4f, carrying = false, slingY = null, rainbowAt = null, lassoHint = RodeoLassoHintUi(x = 79f, y = 51f)),
)

/** Carrying horse and rider up towards the rainbow, the view panned up after them. */
@Preview
@Composable
private fun HelicopterCarryingPreview() = RodeoPreviewTrack(cameraDown = 22f) { context ->
    val lift = 30f
    val colors = context.colors
    val horseColors = RodeoHorseColors(
        body = colors.onSurface,
        shirt = colors.primary,
        glow = colors.primary,
        canopy = colors.secondary,
        friendShirt = colors.tertiary,
        headRing = colors.surfaceContainer,
        rope = colors.tertiary,
        pants = colors.secondary,
    )
    val heli = RodeoHelicopterUi(
        x = HORSE_X + 15f - HELI_CABIN_X, y = lift + HELI_ABOVE_HOOVES, rotor = 1.1f, carrying = true,
        slingY = lift + HELI_SLING_Y, rainbowAt = RAINBOW_LIFT, lassoHint = null,
    )
    drawVehicle(heli, RodeoLayer.BACK, context)
    val pose = RodeoHorsePose(
        height = lift, gaitPhase = 0.5f, airborne = true, riderLean = 0f, pitchDegrees = 2f,
        pivotX = 12f, pivotY = 12f, hindLegScale = 1f, frontLegFold = 0f, hatLift = 0f, glow = 0f,
    )
    drawHorseAndRider(left = HORSE_X * context.unit, groundY = context.groundY - lift * context.unit, unit = context.unit, pose = pose, colors = horseColors)
    drawVehicle(heli, RodeoLayer.FRONT, context)
}

@Preview
@Composable
private fun GoldenCarriagePreview() = RodeoVehiclePreviewTrack(
    RodeoGoldenCarriageUi(x = 20f, wheelPhase = 0.5f, gait = 1.2f),
)

/** A ground over [heights] at control points one terrain step apart from [start], eased like the game's. */
private fun previewGround(start: Float, heights: List<Float>): (Float) -> Float {
    val xs = heights.indices.map { start + it * TERRAIN_STEP }
    return { x -> terrainHeightAt(xs, heights, x) }
}

/** The ground [feature] shapes, with the feature at world x [at] (see RodeoTerrain.addFeature). */
private fun previewGround(feature: TerrainFeature, at: Float) = previewGround(at - 2f * TERRAIN_STEP, feature.shape)

/** The cave shaft with its glowing crystals at the foot of a hill: gallop in, or jump it and ride on up the hill. */
@Preview
@Composable
private fun CaveShaftPreview() = RodeoPreviewTrack(ground = previewGround(TerrainFeature.SHAFT, at = 60f)) { context ->
    drawMapWay(RodeoMapWayUi(x = 60f, width = 24f, exit = false, destination = RodeoMap.CAVE), context, distance = 30f)
}

/** Down the beach into the sea. */
@Preview
@Composable
private fun BeachPreview() = RodeoPreviewTrack(ground = previewGround(TerrainFeature.BEACH, at = 120f), cameraDown = -8f) { context ->
    drawMapWay(RodeoMapWayUi(x = 120f, width = 24f, exit = false, destination = RodeoMap.SEA), context, distance = 0f)
}

/** The plank ramp up to daylight, out of the cave. */
@Preview
@Composable
private fun RampOutPreview() = RodeoPreviewTrack(
    enclosed = true,
    ground = previewGround(TerrainFeature.RAMP_UP, at = 150f),
    backdrop = { drawCaveBackdrop(distance = 40f, context = it) },
) { context ->
    drawMapWay(RodeoMapWayUi(x = 126f, width = 24f, exit = true, destination = RodeoMap.SURFACE, origin = RodeoMap.CAVE), context, distance = 0f)
}

/** The harbour pier out of the sea. */
@Preview
@Composable
private fun HarbourPierPreview() = RodeoPreviewTrack(
    ground = previewGround(TerrainFeature.RAMP_UP, at = 150f),
    sea = true,
    cameraDown = -10f,
) { context ->
    drawMapWay(RodeoMapWayUi(x = 126f, width = 24f, exit = true, destination = RodeoMap.SURFACE, origin = RodeoMap.SEA), context, distance = 0f)
}

/** Vehicles over gentle hills: each one tilts as a whole, resting on the ground at both ends. */
@Preview
@Composable
private fun VehiclesOnHillsPreview() = RodeoPreviewTrack(ground = previewGround(-50f, listOf(0f, 8f, -2f, 6f))) { context ->
    drawVehicles(
        listOf(
            RodeoMilkTruckUi(x = 10f, wheelPhase = 0.5f, cansInRack = 3, cans = emptyList(), lassoHint = null),
            RodeoCowUi(x = 80f, gait = 0.8f, hasRider = false, ring = null, lassoHint = null),
            RodeoMilkTruckUi(x = 130f, wheelPhase = 1.5f, cansInRack = 1, cans = emptyList(), lassoHint = null),
        ),
        context,
    )
}
