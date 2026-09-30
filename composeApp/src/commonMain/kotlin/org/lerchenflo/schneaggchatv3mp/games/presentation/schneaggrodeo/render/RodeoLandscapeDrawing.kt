package org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoMushroomUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.RodeoSectionUi
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.CABLE_FOOT_HEIGHT
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.CABLE_PEAK_HEIGHT
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.CABLE_STATION_MARGIN
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MOUNTAIN_HEIGHT
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MOUNTAIN_UP
import org.lerchenflo.schneaggchatv3mp.games.presentation.schneaggrodeo.engine.MOUNTAIN_WIDTH
import kotlin.math.floor

// The landscape behind the track: mountains with the cable car's cable and stations, forests, and
// the magic mushrooms growing on the track.

/** Jagged points along each flank of the mountain. */
private const val MOUNTAIN_RIDGE_POINTS = 8
/** Share of the mountain's height covered in snow. */
private const val SNOW_SHARE = 0.28f
/** Level cable reaching out from each station. */
private const val CABLE_REACH = 400f
private const val STATION_WIDTH = 12f
private const val TREE_SPACING = 14f

/** A section of landscape: mountain (with cable car cable) or forest. */
internal fun DrawScope.drawSection(section: RodeoSectionUi, context: RodeoDrawContext) {
    if (section.isMountain) drawMountain(section, context) else drawForest(section, context)
}

private fun DrawScope.drawMountain(mountain: RodeoSectionUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(x: Float, y: Float) = context.p(mountain.x + x, y)
    val peakX = MOUNTAIN_UP

    // Rocky silhouette with a jagged ridge up one flank and down the other
    val ridge = buildList {
        add(0f to 0f)
        for (i in 1 until MOUNTAIN_RIDGE_POINTS) {
            val fraction = i / MOUNTAIN_RIDGE_POINTS.toFloat()
            add(peakX * fraction to MOUNTAIN_HEIGHT * fraction + 3f * (cloudNoise(mountain.seed, i) - 0.5f))
        }
        add(peakX to MOUNTAIN_HEIGHT)
        for (i in 1 until MOUNTAIN_RIDGE_POINTS) {
            val fraction = i / MOUNTAIN_RIDGE_POINTS.toFloat()
            add(peakX + (MOUNTAIN_WIDTH - peakX) * fraction to MOUNTAIN_HEIGHT * (1f - fraction) + 3f * (cloudNoise(mountain.seed, i + 20) - 0.5f))
        }
        add(MOUNTAIN_WIDTH to 0f)
    }
    drawPath(polygonPath(::p, *ridge.toTypedArray()), MOUNTAIN_ROCK)
    // Shadow side down the far flank
    drawPath(
        polygonPath(::p, peakX to MOUNTAIN_HEIGHT, MOUNTAIN_WIDTH to 0f, peakX + 40f to 0f),
        MOUNTAIN_ROCK_DARK.copy(alpha = 0.6f)
    )
    // Snow cap: the part of the silhouette above the snow line
    val snowLine = MOUNTAIN_HEIGHT * (1f - SNOW_SHARE)
    val snowLeft = peakX * (1f - SNOW_SHARE)
    val snowRight = peakX + (MOUNTAIN_WIDTH - peakX) * SNOW_SHARE
    drawPath(
        polygonPath(
            ::p,
            snowLeft to snowLine, snowLeft + 25f to snowLine - 3f, snowLeft + 50f to snowLine + 1f,
            peakX to MOUNTAIN_HEIGHT,
            snowRight - 30f to snowLine - 2f, snowRight to snowLine,
        ),
        MOUNTAIN_SNOW
    )

    // The cable car: stations at both feet, a mast on the peak and the cable over it
    val start = -CABLE_STATION_MARGIN
    val end = MOUNTAIN_WIDTH + CABLE_STATION_MARGIN
    val cable = listOf(
        start - CABLE_REACH to CABLE_FOOT_HEIGHT,
        start to CABLE_FOOT_HEIGHT,
        peakX to CABLE_PEAK_HEIGHT,
        end to CABLE_FOOT_HEIGHT,
        end + CABLE_REACH to CABLE_FOOT_HEIGHT,
    )
    cable.zipWithNext().forEach { (from, to) ->
        drawLine(CABLE_COLOR, p(from.first, from.second), p(to.first, to.second), 0.4f * unit)
    }
    drawLine(CABLE_COLOR, p(peakX, MOUNTAIN_HEIGHT - 2f), p(peakX, CABLE_PEAK_HEIGHT + 1.5f), 1.2f * unit)
    drawLine(CABLE_COLOR, p(peakX - 3f, CABLE_PEAK_HEIGHT + 1.5f), p(peakX + 3f, CABLE_PEAK_HEIGHT + 1.5f), 1f * unit)
    listOf(start, end).forEach { stationX ->
        drawRect(STATION_WALL, p(stationX - STATION_WIDTH / 2f, CABLE_FOOT_HEIGHT - 2f), Size(STATION_WIDTH * unit, (CABLE_FOOT_HEIGHT - 2f) * unit))
        drawPath(
            polygonPath(
                ::p,
                stationX - STATION_WIDTH / 2f - 1.5f to CABLE_FOOT_HEIGHT - 2f,
                stationX to CABLE_FOOT_HEIGHT + 4f,
                stationX + STATION_WIDTH / 2f + 1.5f to CABLE_FOOT_HEIGHT - 2f,
            ),
            CABLE_COLOR
        )
        drawCircle(CABLE_COLOR, radius = 1.5f * unit, center = p(stationX, CABLE_FOOT_HEIGHT))
    }
}

/** Conifers of different heights, only the ones in the picture. */
private fun DrawScope.drawForest(forest: RodeoSectionUi, context: RodeoDrawContext) {
    val unit = context.unit
    val visibleWidth = size.width / unit
    val first = maxOf(0, floor(-forest.x / TREE_SPACING).toInt() - 1)
    val last = minOf((forest.width / TREE_SPACING).toInt(), floor((visibleWidth - forest.x) / TREE_SPACING).toInt() + 2)
    for (index in first..last) {
        val treeX = forest.x + index * TREE_SPACING + 5f * cloudNoise(forest.seed, index)
        val height = 30f + 22f * cloudNoise(forest.seed, index + 100)
        val width = height * 0.42f
        fun p(dx: Float, y: Float) = context.p(treeX + dx, y)
        drawRect(TREE_TRUNK, p(-1f, height * 0.25f), Size(2f * unit, height * 0.25f * unit))
        val leaves = if (index % 2 == 0) TREE_LEAVES else TREE_LEAVES_DARK
        // Three stacked tiers, narrowing to the top
        listOf(0.2f to 1f, 0.45f to 0.78f, 0.68f to 0.55f).forEach { (bottom, widthShare) ->
            val tierWidth = width * widthShare
            drawPath(
                polygonPath(::p, -tierWidth / 2f to height * bottom, 0f to height * (bottom + 0.34f), tierWidth / 2f to height * bottom),
                leaves
            )
        }
    }
}

/** A red toadstool with white dots, its stem standing on the ground. */
internal fun DrawScope.drawMushroom(mushroom: RodeoMushroomUi, context: RodeoDrawContext) {
    val unit = context.unit
    fun p(dx: Float, y: Float) = context.p(mushroom.x + dx, y)
    drawRoundRect(MUSHROOM_STEM, p(-0.7f, 2.2f), Size(1.4f * unit, 2.2f * unit), CornerRadius(0.5f * unit))
    val cap = Path().apply {
        val left = p(-2.2f, 2f)
        val right = p(2.2f, 2f)
        val top = p(0f, 5.2f)
        moveTo(left.x, left.y)
        cubicTo(left.x, top.y, right.x, top.y, right.x, right.y)
        close()
    }
    drawPath(cap, MUSHROOM_RED)
    listOf(-1.1f to 3.1f, 0.4f to 3.9f, 1.3f to 2.8f).forEachIndexed { index, (dx, y) ->
        drawCircle(MUSHROOM_DOTS, radius = (0.3f + 0.15f * cloudNoise(mushroom.seed, index)) * unit, center = p(dx, y))
    }
}

/** A few swirls over the picture while a slow-motion mushroom works. */
internal fun DrawScope.drawSlowMotionHaze(color: Color, unit: Float, distance: Float) {
    drawRect(color.copy(alpha = 0.08f))
    repeat(5) { index ->
        val center = Offset(
            (cloudNoise(index, 1) * size.width + distance * unit * 0.1f) % size.width,
            cloudNoise(index, 2) * size.height
        )
        drawCircle(color.copy(alpha = 0.1f), radius = (4f + 3f * cloudNoise(index, 3)) * unit, center = center)
    }
}
