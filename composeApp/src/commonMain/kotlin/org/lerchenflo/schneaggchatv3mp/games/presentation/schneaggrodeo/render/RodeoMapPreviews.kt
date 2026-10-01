package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoCarrotUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoDeepThingUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoFenceUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoGemUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoHorsePose
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMoundUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMudUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMushroomUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSectionUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSnailUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.FOREST_WIDTH
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HOOVES_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoDeepKind
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoTimeOfDay
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.RodeoWeather
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEABED_Y
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SEA_CAMERA_DOWN
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SNAIL_SIZE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.vehicles.RodeoPreviewTrack

// IDE previews of each map (surface, cave, sea, mine) in a typical moment: the ridden horse with the
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

/** The cave: stalactites above, stalagmites for fences, glowing crystals and snails. */
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

/** The mine: crate stacks (one wide with rocks), a gold nugget, a dirt mound with a shovel, snails with helmets. */
@Preview
@Composable
private fun MineMapPreview() = RodeoPreviewTrack(
    enclosed = true,
    ground = { 0f },
    backdrop = { drawMineBackdrop(distance = 40f, context = it) },
) { context ->
    val label = context.assets!!.textMeasurer.measure("100 cm")
    drawCrateStack(previewFence(x = 62f, width = 10f, heightCm = 100), context, label)
    drawCrateStack(previewFence(x = 128f, width = 18f, heightCm = 100), context, label)
    drawNugget(RodeoGemUi(x = 90f, height = 18f, tiltDeg = 10f, hue = 0), context)
    drawMound(RodeoMoundUi(x = 40f, seed = 7), context)
    listOf(CRAWLER, RUNNER).forEach {
        drawPreviewSnail(it, context)
        drawMinerHelmet(it, context)
    }
    drawPreviewHorse(context)
}
