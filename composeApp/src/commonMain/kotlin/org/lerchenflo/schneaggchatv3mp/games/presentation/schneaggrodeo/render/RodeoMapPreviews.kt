package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCarrotUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoDeepThingUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGapUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMapWayUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoHorsePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMoundUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMudUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMushroomUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSectionUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSnailUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.FOREST_WIDTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HOOVES_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MAP_WAY_WIDTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoMap
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.TERRAIN_STEP
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.TerrainFeature
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.terrainHeightAt
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoDeepKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoTimeOfDay
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoWeather
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEABED_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEA_CAMERA_DOWN
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SNAIL_SIZE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoPreviewTrack
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.minecart.RodeoMineCartUi

// IDE previews of each map (surface, cave, sea, rainbow, fossil layer) in a typical moment: the ridden horse with the
// map's fences, snails and pickups in front of it, drawn the way the track canvas dresses them per map.

/** The ridden horse at its spot on the track, mid-gallop. */
private fun DrawScope.drawPreviewHorse(context: RodeoDrawContext) {
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
        maxLives = 4, lives = 3.5f,
    )
    drawHorseAndRider(left = HORSE_X * context.unit, groundY = context.groundYAt(HORSE_X + HOOVES_X), unit = context.unit, pose = pose, colors = horseColors)
}

private fun DrawScope.drawPreviewSnail(snail: RodeoSnailUi, context: RodeoDrawContext) = drawSnail(
    image = context.assets!!.snail,
    centerX = snail.x * context.unit,
    footY = context.p(snail.x, snail.height).y,
    size = SNAIL_SIZE * context.unit,
    facingLeft = snail.facingLeft,
    tiltDeg = snail.tiltDeg,
    outline = ColorFilter.tint(context.colors.surfaceContainer),
)

private fun previewFence(x: Float, width: Float, heightCm: Int) = RodeoFenceUi(
    x = x,
    width = width,
    heightCm = heightCm,
    top = heightCm / 10f,
    colorOffset = 0,
    knocked = false,
    poleHeights = if (width > 12f) listOf(heightCm / 10f - 1f, heightCm / 10f) else listOf(heightCm / 10f),
)

private val CRAWLER = RodeoSnailUi(x = 108f, height = 0f, facingLeft = true, tiltDeg = 0f)
private val RUNNER = RodeoSnailUi(x = 160f, height = 0f, facingLeft = true, tiltDeg = 0f, runner = true)

/** The surface: a forest behind, mud, a fence and an oxer, snails, a horseshoe, a carrot, a gem and a mushroom. */
@Composable
private fun SurfaceScene(snowyNight: Boolean) = RodeoPreviewTrack(ground = { 0f }) { context ->
    drawSection(RodeoSectionUi(isMountain = false, x = -300f, width = FOREST_WIDTH, seed = 3), context)
    if (snowyNight) drawSnowCover(context)
    val label = context.assets!!.textMeasurer.measure("120 cm")
    drawMud(RodeoMudUi(x = 38f, width = 16f, seed = 2), context)
    drawMushroom(RodeoMushroomUi(x = 92f, seed = 1), context)
    drawFence(previewFence(x = 62f, width = 10f, heightCm = 120), context, context.colors.onSurface, label)
    drawFence(previewFence(x = 128f, width = 18f, heightCm = 120), context, context.colors.onSurface, label)
    drawPreviewSnail(CRAWLER, context)
    drawPreviewSnail(RUNNER, context)
    drawHorseshoe(context.p(80f, 22f), context.unit, tiltDeg = 15f, color = context.colors.secondary, nailColor = context.colors.surfaceContainer)
    drawCarrot(RodeoCarrotUi(x = 100f, height = 20f, tiltDeg = -10f), context)
    drawGem(RodeoGemUi(x = 137f, height = 24f, tiltDeg = 10f, hue = 1), context)
    drawPreviewHorse(context)
    if (snowyNight) {
        drawWeather(RodeoWeather.SNOW, time = 3f, unit = context.unit)
        drawTimeOfDay(RodeoTimeOfDay.NIGHT, lantern = context.p(HORSE_X + 15f, 14f), unit = context.unit)
    }
}

@Preview
@Composable
private fun SurfaceMapPreview() = SurfaceScene(snowyNight = false)

/** The same stretch on a snowy night: snow on the ground, the lantern lighting up the horse. */
@Preview
@Composable
private fun SurfaceSnowyNightPreview() = SurfaceScene(snowyNight = true)

/** The cave: stalactites above, stalagmites for fences, glowing crystals, a dirt mound and snails. */
@Preview
@Composable
private fun CaveMapPreview() = RodeoPreviewTrack(
    enclosed = true,
    ground = { 0f },
    backdrop = { drawCaveBackdrop(distance = 40f, context = it) },
) { context ->
    val label = context.assets!!.textMeasurer.measure("110 cm")
    drawStalagmite(previewFence(x = 62f, width = 10f, heightCm = 110), context, label)
    drawStalagmite(previewFence(x = 128f, width = 18f, heightCm = 110), context, label)
    drawPreviewSnail(CRAWLER, context)
    drawPreviewSnail(RUNNER, context)
    drawGem(RodeoGemUi(x = 90f, height = 20f, tiltDeg = 0f, hue = 0), context)
    drawGem(RodeoGemUi(x = 137f, height = 24f, tiltDeg = -15f, hue = 2), context)
    drawMound(RodeoMoundUi(x = 42f, seed = 7), context)
    drawPreviewHorse(context)
}

/**
 * The open sea, the view looking down like in the game: buoys, a shark, a snail on its swim ring, an
 * oil slick, a pearl, life in the deep, the swimming horse and the water over it all.
 */
@Preview
@Composable
private fun SeaMapPreview() = RodeoPreviewTrack(sea = true, cameraDown = SEA_CAMERA_DOWN) { context ->
    listOf(
        RodeoDeepThingUi(x = 20f, y = -20f, kind = RodeoDeepKind.WHALE, seed = 4, time = 1f),
        RodeoDeepThingUi(x = 70f, y = -10f, kind = RodeoDeepKind.FISH, seed = 1, time = 1f),
        RodeoDeepThingUi(x = 105f, y = -8f, kind = RodeoDeepKind.JELLYFISH, seed = 2, time = 1f),
        RodeoDeepThingUi(x = 130f, y = -18f, kind = RodeoDeepKind.SHARK, seed = 3, time = 1f),
        RodeoDeepThingUi(x = 140f, y = SEABED_Y, kind = RodeoDeepKind.WRECK, seed = 5, time = 1f),
    ).forEach { drawDeepThing(it, context) }
    val label = context.assets!!.textMeasurer.measure("80 cm")
    drawBuoy(previewFence(x = 62f, width = 10f, heightCm = 80), context, label)
    drawBuoy(previewFence(x = 128f, width = 18f, heightCm = 80), context, label)
    drawOilSlick(RodeoMudUi(x = 38f, width = 24f, seed = 3), context)
    drawShark(RUNNER, context)
    drawSwimRing(CRAWLER, context)
    drawPreviewSnail(CRAWLER, context)
    drawPearl(RodeoGemUi(x = 90f, height = 20f, tiltDeg = 0f, hue = 0), context)
    drawPreviewHorse(context)
}

/** The rainbow: the sea of clouds below, the rainbow track with two gaps, a star over one and a carrot. */
@Preview
@Composable
private fun RainbowMapPreview() = RodeoPreviewTrack(
    groundLine = false,
    backdrop = { drawRainbowBackdrop(distance = 40f, context = it) },
) { context ->
    drawRainbowTrack(context, listOf(RodeoGapUi(x = 58f, width = 16f), RodeoGapUi(x = 120f, width = 22f)))
    drawStar(RodeoGemUi(x = 66f, height = 16f, tiltDeg = 10f, hue = 0), context)
    drawCarrot(RodeoCarrotUi(x = 100f, height = 16f, tiltDeg = -10f), context)
    drawPreviewHorse(context)
}

/** The rainbow's end: sliding down into the cloud that takes the run back to the surface. */
@Preview
@Composable
private fun RainbowEndPreview() {
    val ground = previewSlide(at = 130f)
    RodeoPreviewTrack(
        groundLine = false,
        ground = ground,
        cameraDown = -10f,
        backdrop = { drawRainbowBackdrop(distance = 40f, context = it) },
    ) { context ->
        drawRainbowTrack(context, emptyList())
        drawRainbowLanding(x = 130f, width = MAP_WAY_WIDTH, context = context)
        drawPreviewHorse(context)
    }
}

/** Where the mine entrance's way begins in the previews below. */
private const val PREVIEW_MINE_AT = 64f

/** The surface with the gold mine behind the track, its cart full of gold coming out to roll along. */
@Preview
@Composable
private fun MineEntrancePreview() = RodeoPreviewTrack(
    ground = previewFeature(TerrainFeature.SHAFT, PREVIEW_MINE_AT),
) { context ->
    val way = RodeoMapWayUi(x = PREVIEW_MINE_AT, width = MAP_WAY_WIDTH, exit = false, destination = RodeoMap.CAVE)
    drawMineEntrance(way, context, distance = 30f)
    val cart = RodeoMineCartUi(
        x = PREVIEW_MINE_AT - 4f, wheelPhase = 0f, railOffset = 0f, sparks = null, lassoHint = null,
        railsAcross = false, gold = true,
    )
    drawVehicle(cart, RodeoLayer.BODY, context)
    drawVehicle(cart, RodeoLayer.FRONT, context)
    drawPreviewHorse(context)
}

/** The fossil layer: rock in layers with fossils, bone fences, ammonites and snails. */
@Preview
@Composable
private fun FossilMapPreview() = RodeoPreviewTrack(
    enclosed = true,
    ground = { 0f },
    backdrop = { drawFossilBackdrop(distance = 40f, context = it) },
) { context ->
    val label = context.assets!!.textMeasurer.measure("120 cm")
    drawBoneFence(previewFence(x = 62f, width = 10f, heightCm = 120), context, label)
    drawBoneFence(previewFence(x = 128f, width = 18f, heightCm = 120), context, label)
    drawPreviewSnail(CRAWLER, context)
    drawPreviewSnail(RUNNER, context)
    drawAmmonite(RodeoGemUi(x = 90f, height = 20f, tiltDeg = 0f, hue = 0), context)
    drawAmmonite(RodeoGemUi(x = 137f, height = 24f, tiltDeg = 90f, hue = 0), context)
    drawPreviewHorse(context)
}

/** The ground of the rainbow sliding down to its end at [at], eased like the game's (see TerrainFeature.SLIDE_DOWN). */
private fun previewSlide(at: Float): (Float) -> Float = previewFeature(TerrainFeature.SLIDE_DOWN, at)

/** The ground shaped for a way to another map at [at], eased like the game's (see RodeoTerrain.addFeature). */
private fun previewFeature(feature: TerrainFeature, at: Float): (Float) -> Float {
    val heights = feature.shape
    val xs = heights.indices.map { at - 2f * TERRAIN_STEP + it * TERRAIN_STEP }
    return { x -> terrainHeightAt(xs, heights, x) }
}
