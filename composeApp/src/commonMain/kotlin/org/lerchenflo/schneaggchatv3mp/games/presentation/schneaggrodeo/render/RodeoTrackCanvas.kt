package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoPeopleUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSnailUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.SchneaggRodeoFrame
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.GROUND_OFFSET_UNITS
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.HORSE_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.OVEN_MOUTH_X
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MAX_FENCE_CM
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MIN_FENCE_CM
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.SNAIL_SIZE
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.WORLD_HEIGHT_UNITS
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_candy_bus_sign
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_car_money
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_fence_height
import schneaggchatv3mp.composeapp.generated.resources.games_schneaggrodeo_lasso_hint
import schneaggchatv3mp.composeapp.generated.resources.icon_schneagg_alternative
import schneaggchatv3mp.composeapp.generated.resources.rodeo_stanislaus

/** Stands in for the amount in the money text, filled in while drawing. */
private const val MONEY_PLACEHOLDER = "{amount}"

/** Highscore marker posts; staggered ones are shorter so neighbouring labels don't stack up. */
private const val MARKER_POST_HEIGHT = 36f
private const val MARKER_POST_HEIGHT_STAGGERED = 29f

/**
 * The track: space and sky, ground, highscore markers, mud, fences,
 * snails, the chasing pack, other horses, vehicles, horse, rider and lasso - drawn back to front.
 * [frame] and [people] are read in the draw phase only, so a new frame redraws the canvas without
 * recomposing anything.
 */
@Composable
internal fun RodeoTrack(
    frame: () -> SchneaggRodeoFrame,
    people: () -> RodeoPeopleUi,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeColors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    // Only 13 possible heights - resolved in composition since the canvas draws outside of it
    val heightLabels = (MIN_FENCE_CM..MAX_FENCE_CM step 10).associateWith {
        stringResource(Res.string.games_schneaggrodeo_fence_height, it)
    }
    // Text is laid out once per map and reused, instead of being measured again for every frame
    val surfacePaint = rememberRodeoPaint(themeColors, textMeasurer, heightLabels)
    val cavePaint = rememberRodeoPaint(remember(themeColors) { themeColors.cave() }, textMeasurer, heightLabels)
    val snailImage = imageResource(Res.drawable.icon_schneagg_alternative)
    val stanislausImage = imageResource(Res.drawable.rodeo_stanislaus)
    val candySign = stringResource(Res.string.games_schneaggrodeo_candy_bus_sign)
    val moneyTemplate = stringResource(Res.string.games_schneaggrodeo_car_money, MONEY_PLACEHOLDER)
    val vehicleAssets = remember(stanislausImage, snailImage, candySign, moneyTemplate, textMeasurer) {
        RodeoVehicleAssets(
            stanislaus = stanislausImage,
            snail = snailImage,
            candySign = candySign,
            moneyText = { amount -> moneyTemplate.replace(MONEY_PLACEHOLDER, amount.toString()) },
            textMeasurer = textMeasurer,
        )
    }
    val lassoHintText = stringResource(Res.string.games_schneaggrodeo_lasso_hint)
    val lassoHintStyle = MaterialTheme.typography.labelMedium.copy(
        color = themeColors.onPrimaryContainer,
        fontWeight = FontWeight.Bold
    )
    val lassoHintLayout = remember(textMeasurer, lassoHintText, lassoHintStyle) {
        textMeasurer.measure(lassoHintText, lassoHintStyle)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = themeColors.surfaceContainer,
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged(onSizeChanged)
        ) {
            val world = frame() // read here so every frame only redraws the canvas
            val crowd = people()
            val paint = if (world.inCave) cavePaint else surfacePaint
            val colors = paint.colors
            val unit = size.height / WORLD_HEIGHT_UNITS
            val groundY = size.height - GROUND_OFFSET_UNITS * unit
            val context = RodeoDrawContext(groundY = groundY, unit = unit, colors = colors, assets = vehicleAssets)

            fun drawSnailAt(snail: RodeoSnailUi) = drawSnail(
                image = snailImage,
                centerX = snail.x * unit,
                footY = groundY - snail.height * unit,
                size = SNAIL_SIZE * unit,
                facingLeft = snail.facingLeft,
                tiltDeg = snail.tiltDeg,
                outline = paint.snailOutline
            )

            // Up in space (rocket) the sky turns dark and starry - fixed to the screen
            drawSpace(world.distance, unit, world.space)
            // Down in the cave: rock all around, stalactites and torches
            if (world.inCave) drawCaveBackdrop(world.distance, context)

            // The whole picture rattles while a vehicle races, pans up with the horse and tilts with
            // the mountain around the horse's hooves
            withTransform({
                translate(world.shakeX * unit, (world.shakeY + world.cameraY) * unit)
                rotate(world.tiltDegrees, pivot = Offset((HORSE_X + 15f) * unit, groundY))
            }) {
                // Fair-weather clouds high above the track, only seen from up there
                if (world.cameraY > 0f && world.space < 1f) {
                    val cloudAlpha = 1f - world.space
                    drawSkyClouds(
                        groundY,
                        unit,
                        world.distance,
                        colors.surfaceBright.copy(alpha = cloudAlpha),
                        colors.outlineVariant.copy(alpha = cloudAlpha)
                    )
                }

                // Mountains and forests behind everything on the track
                world.sections.forEach { drawSection(it, context) }

                drawGround(groundY, unit, world.distance, colors.onSurfaceVariant, world.speedBlur)
                // Mine shafts in the track, light shafts out of the cave
                world.portals.forEach { drawPortal(it, context) }
                if (world.speedBlur) drawSpeedLines(groundY, unit, world.distance, colors.onSurfaceVariant)

                // Skylines, bridge piers and planets stand behind everything on the track
                world.vehicles.forEach { drawVehicle(it, RodeoLayer.BACK, context) }

                world.markers.forEach { marker ->
                    val color = if (marker.isOwn) colors.primary else colors.secondary
                    drawMarker(
                        marker = marker,
                        groundY = groundY,
                        unit = unit,
                        postHeight = if (marker.staggered) MARKER_POST_HEIGHT_STAGGERED else MARKER_POST_HEIGHT,
                        color = color,
                        label = paint.markerLabels.getOrPut(Triple(marker.username, marker.score, marker.isOwn)) {
                            textMeasurer.measure(
                                text = "${marker.username}\n${marker.score}",
                                style = paint.markerLabelStyle.copy(
                                    color = color,
                                    fontWeight = if (marker.isOwn) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 2,
                            )
                        }
                    )
                }

                // Pizza ovens stand by the roadside, behind the track
                world.pizzaOvens.forEach { drawPizzaOven(it, context, world.distance * 0.02f) }
                world.mud.forEach { drawMud(it, groundY, unit) }
                world.mushrooms.forEach { drawMushroom(it, context) }

                world.fences.forEach { fence ->
                    val label = paint.heightLabels.getValue(fence.heightCm)
                    // In the cave the fences are stalagmites
                    if (world.inCave) drawStalagmite(fence, context, label) else drawFence(fence, groundY, unit, colors.onSurface, label)
                }

                world.snails.forEach { drawSnailAt(it) }
                world.pack.forEach { drawSnailAt(it) }

                world.horseshoes.forEach { shoe ->
                    drawHorseshoe(
                        center = context.p(shoe.x, shoe.height),
                        unit = unit,
                        tiltDeg = shoe.tiltDeg,
                        color = colors.secondary,
                        nailColor = colors.surfaceContainer
                    )
                }

                world.carrots.forEach { drawCarrot(it, context) }
                world.gems.forEach { drawGem(it, context) }

                // Wild horses and friends on their own horse, behind the ridden one
                world.wildHorses.forEach { wild ->
                    drawHorseAndRider(
                        left = wild.x * unit,
                        groundY = groundY - wild.pose.height * unit,
                        unit = unit,
                        pose = wild.pose,
                        colors = paint.horseColors,
                        bodyLabel = null,
                        pictures = crowd.pictures,
                    )
                    wild.friendName?.let { name ->
                        val label = paint.friendNames.getOrPut(name) { textMeasurer.measure(name, paint.friendNameStyle, maxLines = 1) }
                        val labelTop = context.p(wild.x + 12f, wild.pose.height + 34f)
                        drawText(label, topLeft = labelTop - Offset(label.size.width / 2f, label.size.height.toFloat()))
                    }
                }

                // Faint lucky aura around the horse, stronger with every stored charm
                if (world.luckyCharms > 0 && world.horse.hasRider && world.horse.visible) {
                    drawCircle(
                        color = colors.secondary.copy(alpha = 0.1f + 0.08f * world.luckyCharms),
                        radius = 17f * unit,
                        center = context.p(HORSE_X + world.horse.offsetX + 15f, world.horse.height + 14f),
                        style = Stroke(width = (0.4f + 0.3f * world.luckyCharms) * unit)
                    )
                }

                // The horse stands on the vehicles, so they go first
                world.vehicles.forEach { drawVehicle(it, RodeoLayer.BODY, context) }

                if (world.horse.visible) {
                    drawHorseAndRider(
                        left = (HORSE_X + world.horse.offsetX) * unit,
                        groundY = groundY - world.horse.height * unit,
                        unit = unit,
                        pose = world.horse,
                        colors = paint.horseColors,
                        bodyLabel = if (world.snailsCaught > 0) {
                            paint.bodyLabels.getOrPut(world.snailsCaught) { textMeasurer.measure(world.snailsCaught.toString(), paint.bodyLabelStyle) }
                        } else null,
                        pictures = crowd.pictures,
                    )
                }

                // Planes, debris, smoke, bridge decks and flying cans pass in front of the horse
                world.vehicles.forEach { drawVehicle(it, RodeoLayer.FRONT, context) }

                // "Lasso it" hint over a vehicle passing by, and over the other horses
                val hints = world.vehicles.mapNotNull { it.lassoHint?.let { hint -> hint.x to hint.y } } +
                        world.wildHorses.filter { it.lassoable }.map { it.x + 14f to it.pose.height + if (it.friendName != null) 40f else 32f } +
                        world.pizzaOvens.filter { it.hasPizza }.map { it.x + OVEN_MOUTH_X to 18f }
                hints.forEach { (hintX, hintY) ->
                    drawLassoHint(
                        layout = lassoHintLayout,
                        centerX = hintX * unit,
                        bottomY = groundY - hintY * unit,
                        unit = unit,
                        color = colors.primaryContainer
                    )
                }

                world.pizza?.let { drawPizza(it, context) }

                world.cowboy?.let { cowboy ->
                    drawCowboy(cowboy = cowboy, groundY = groundY, unit = unit, color = colors.onSurface, shirtColor = colors.primary)
                }

                // Wading through mud: brown spray keeps flying from the hooves
                if (world.inMud) {
                    val sprayProgress = (world.distance * 0.08f) % 1f
                    listOf(HORSE_X + 7f, HORSE_X + 18f).forEach { hoofX ->
                        drawLandingSplash(hoofX * unit, groundY, unit, sprayProgress, MUD_COLOR)
                    }
                }

                world.splashProgress?.let { splashProgress ->
                    // Front and hind hooves both kick up a little spray
                    listOf(HORSE_X + 7f, HORSE_X + 18f).forEach { hoofX ->
                        drawLandingSplash(hoofX * unit, groundY, unit, splashProgress, colors.onSurfaceVariant)
                    }
                }

                world.dust?.let { dust ->
                    drawDust(x = dust.x * unit, groundY = groundY, unit = unit, progress = dust.progress, color = colors.onSurfaceVariant)
                }

                world.sparkle?.let { sparkle ->
                    drawSparkle(center = context.p(sparkle.x, sparkle.y), unit = unit, progress = sparkle.progress, color = colors.secondary)
                }

                world.lasso?.let { lasso ->
                    drawLasso(context.p(lasso.handX, lasso.handY), context.p(lasso.tipX, lasso.tipY), unit, colors.tertiary)
                    lasso.caught?.let { drawSnailAt(it) }
                }
            }

            // Slow-motion mushroom: a dreamy haze over everything
            if (world.slowMotion) drawSlowMotionHaze(colors.tertiary, unit, world.distance)
            // Switching maps fades through black
            if (world.fade > 0f) drawRect(colors.scrim.copy(alpha = world.fade))
        }
    }
}
