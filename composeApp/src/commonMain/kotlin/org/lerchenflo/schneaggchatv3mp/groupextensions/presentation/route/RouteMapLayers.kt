package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import org.maplibre.compose.expressions.value.SymbolPlacement
import org.maplibre.compose.expressions.value.IconRotationAlignment
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteTransportMode
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.safeAdd
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.toPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import kotlin.math.roundToInt

private val POINT_MARKER_SIZE = 28.dp
private val WAYPOINT_MARKER_SIZE = 12.dp
private val SELECTED_PLACE_RING_RADIUS = 20.dp
private val NEW_POINT_MARKER_SIZE = 30.dp
private val LEG_MODE_BADGE_SIZE = 24.dp
private val DIRECTION_ARROW_SIZE = 12.dp
// Room between two arrows along a line
private val DIRECTION_ARROW_SPACING = 70.dp
private val MARKER_BORDER = 2.dp
private val SELECTED_RING = 4.dp

/**
 * What the route planner draws on top of the shared map layers: the route line (the picked day in
 * the colors of its transport modes with a mode badge in the middle of each leg, the others grey;
 * alternative paths dashed, a tap on one shows its points),
 * one marker per point, a ring around the tapped Schneaggmap place and
 * the marker of a point being added. Every source always exists (empty when there is nothing to
 * draw), so no source is removed and added again (CannotAddSourceException).
 */
@Composable
@MaplibreComposable
fun RouteMapLayers(
    state: RouteState,
    onAction: (RouteAction) -> Unit,
) {
    val (activeLegs, dimmedLegs) = remember(state.points, state.paths, state.selectedDayIndex) {
        routeMapLegs(state).partition { !it.isDimmed }
    }
    RouteLineLayers(
        activeLegs = activeLegs,
        dimmedLegs = dimmedLegs,
        onPathClick = { onAction(RouteAction.OnPathSelect(it)) },
    )
    DirectionArrows(legs = activeLegs)
    LegModeBadges(legs = activeLegs, onClick = { onAction(RouteAction.OnLegModeClick(it)) })
    SelectedPlaceRing(state.selectedPlace?.coordinates)

    // Drive-through dots first, so the bigger markers lie on top of them
    val allPoints = state.points + state.paths.flatMap { it.points }
    val (waypoints, others) = allPoints.partition { it.kind == RoutePointKind.WAYPOINT }
    (waypoints + others).forEach { point ->
        key(point.itemId) {
            RoutePointMarker(
                point = point,
                isDimmed = state.selectedDayIndex != null && point.dayIndex != state.selectedDayIndex,
                isSelected = point.itemId == state.selectedPointId,
                onClick = { onAction(RouteAction.OnPointSelect(point.itemId)) },
            )
        }
    }

    val newPoint = state.pointDraft?.takeIf { it.itemId == null && !state.isMovingPoint }?.coordinates
    NewPointMarker(newPoint)
}

/**
 * One straight leg on the map. [modeItemId] owns its mode: the point it leads to, or the path for
 * a path's way back. [pathId] is set for the legs of an alternative path.
 */
private class RouteMapLeg(
    val from: LatLong,
    val to: LatLong,
    val mode: RouteTransportMode,
    val modeItemId: String,
    val pathId: String?,
    val isDimmed: Boolean,
)

// A main leg belongs to the day of the point it leads to, a path to the day of its start
private fun routeMapLegs(state: RouteState): List<RouteMapLeg> {
    fun dimmed(dayIndex: Int) = state.selectedDayIndex != null && dayIndex != state.selectedDayIndex
    val mainLegs = state.points.zipWithNext { from, to ->
        RouteMapLeg(
            from = from.coordinates,
            to = to.coordinates,
            mode = to.arrivalMode ?: RouteTransportMode.DEFAULT,
            modeItemId = to.itemId,
            pathId = null,
            isDimmed = dimmed(to.dayIndex),
        )
    }
    val pathLegs = state.paths.flatMap { path ->
        path.line.zipWithNext().mapIndexed { index, (from, to) ->
            RouteMapLeg(
                from = from,
                to = to,
                mode = path.legModes[index],
                modeItemId = path.legModeItemIds[index],
                pathId = path.itemId,
                isDimmed = dimmed(path.dayIndex),
            )
        }
    }
    return mainLegs + pathLegs
}

@Composable
@MaplibreComposable
private fun RouteLineLayers(
    activeLegs: List<RouteMapLeg>,
    dimmedLegs: List<RouteMapLeg>,
    onPathClick: (pathItemId: String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    LineLayer(
        id = "route-line-dimmed",
        source = rememberGeoJsonSource(data = legsData(dimmedLegs)),
        color = const(scheme.outline),
        width = const(3.dp),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )

    // Casing under the line, so it stands out on every map color
    LineLayer(
        id = "route-line-casing",
        source = rememberGeoJsonSource(data = legsData(activeLegs)),
        color = const(scheme.surface.copy(alpha = 0.8f)),
        width = const(7.dp),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )
    // One layer per mode and part (always there, maybe empty), each in its mode's color; paths dashed
    val (pathLegs, mainLegs) = activeLegs.partition { it.pathId != null }
    RouteTransportMode.entries.forEach { mode ->
        key(mode) {
            LineLayer(
                id = "route-line-${mode.name}",
                source = rememberGeoJsonSource(data = legsData(mainLegs.filter { it.mode == mode })),
                color = const(mode.colors().container),
                width = const(4.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
            )
            LineLayer(
                id = "route-path-line-${mode.name}",
                source = rememberGeoJsonSource(data = legsData(pathLegs.filter { it.mode == mode })),
                onClick = { clicked ->
                    val pathId = clicked.firstOrNull()?.id?.content?.substringBefore(LEG_ID_SEPARATOR)
                    if (pathId != null) {
                        onPathClick(pathId)
                        ClickResult.Consume
                    } else ClickResult.Pass
                },
                color = const(mode.colors().container),
                width = const(4.dp),
                dasharray = const(listOf<Number>(2, 1.5)),
                join = const(LineJoin.Round),
            )
        }
    }
}

/**
 * Small arrows along the shown legs in driving direction (each leg is drawn from its start to its
 * end, and line placement turns the icon along it). They never hide markers or badges.
 */
@Composable
@MaplibreComposable
private fun DirectionArrows(legs: List<RouteMapLeg>) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val arrow = remember(scheme.surface, density) { drawDirectionArrow(scheme.surface, density) }
    SymbolLayer(
        id = "route-direction-arrows",
        source = rememberGeoJsonSource(data = legsData(legs)),
        placement = const(SymbolPlacement.Line),
        spacing = const(DIRECTION_ARROW_SPACING),
        iconImage = image(arrow),
        iconRotationAlignment = const(IconRotationAlignment.Map),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
    )
}

/** A chevron pointing right (the line's direction), as a ready bitmap like the markers. */
private fun drawDirectionArrow(color: Color, density: Density): ImageBitmap {
    val sizePx = with(density) { DIRECTION_ARROW_SIZE.roundToPx() }.coerceAtLeast(1)
    val strokePx = with(density) { 2.dp.toPx() }
    val bitmap = ImageBitmap(width = sizePx, height = sizePx, config = ImageBitmapConfig.Argb8888, hasAlpha = true)
    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = Size(sizePx.toFloat(), sizePx.toFloat()),
    ) {
        val chevron = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.2f)
            lineTo(size.width * 0.7f, size.height * 0.5f)
            lineTo(size.width * 0.3f, size.height * 0.8f)
        }
        drawPath(path = chevron, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    return bitmap
}

/**
 * The mode's icon in the middle of each leg; tapping it changes how the group gets to the point the
 * leg leads to. Badges that would overlap each other or a marker are left out.
 */
@Composable
@MaplibreComposable
private fun LegModeBadges(legs: List<RouteMapLeg>, onClick: (toItemId: String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    RouteTransportMode.entries.forEach { mode ->
        key(mode) {
            val colors = mode.colors()
            val iconPainter = rememberVectorPainter(mode.icon())
            val badge = remember(mode, colors, scheme.surface, density) {
                drawRouteMarker(
                    diameter = LEG_MODE_BADGE_SIZE,
                    fill = colors.container,
                    border = scheme.surface,
                    content = MarkerContent.Icon(iconPainter, colors.content),
                    textMeasurer = textMeasurer,
                    density = density,
                )
            }
            val midpoints = legs.filter { it.mode == mode }.map { leg ->
                leg.modeItemId to LatLong(lat = (leg.from.lat + leg.to.lat) / 2, long = (leg.from.long + leg.to.long) / 2)
            }
            SymbolLayer(
                id = "route-leg-mode-${mode.name}",
                source = rememberGeoJsonSource(data = pointsData(midpoints)),
                onClick = { clicked ->
                    val itemId = clicked.firstOrNull()?.id?.content
                    if (itemId != null) {
                        onClick(itemId)
                        ClickResult.Consume
                    } else ClickResult.Pass
                },
                iconImage = image(badge),
            )
        }
    }
}

// A path leg's feature id is "<path id>#<leg>", so a tap on the line finds its path
private const val LEG_ID_SEPARATOR = "#"

private fun legsData(legs: List<RouteMapLeg>) = GeoJsonData.Features(
    FeatureCollection(
        features = legs.mapIndexed { index, leg ->
            Feature(
                geometry = LineString(listOf(leg.from.toPosition(), leg.to.toPosition())),
                properties = buildJsonObject {},
                id = leg.pathId?.let { JsonPrimitive("$it$LEG_ID_SEPARATOR$index") },
            )
        }
    )
)

private fun pointsData(points: List<Pair<String, LatLong>>) = GeoJsonData.Features(
    FeatureCollection(
        features = points.map { (id, coordinates) ->
            Feature(
                geometry = Point(coordinates.toPosition()),
                properties = buildJsonObject {},
                id = JsonPrimitive(id),
            )
        }
    )
)

/** Ring around the tapped place (the shared layers draw its icon), so its card has a visible match. */
@Composable
@MaplibreComposable
private fun SelectedPlaceRing(coordinates: LatLong?) {
    val scheme = MaterialTheme.colorScheme
    CircleLayer(
        id = "route-place-selected",
        source = rememberGeoJsonSource(data = pointsData(listOfNotNull(coordinates?.let { "selected" to it }))),
        opacity = const(0f),
        radius = const(SELECTED_PLACE_RING_RADIUS),
        strokeColor = const(scheme.primary),
        strokeWidth = const(3.dp),
    )
}

@Composable
@MaplibreComposable
private fun RoutePointMarker(
    point: RoutePointUi,
    isDimmed: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val kindColors = point.kind.colors()
    val iconPainter = rememberVectorPainter(point.kind.icon())

    safeAdd(layerId = "route-point-${point.itemId}") {
        val fill = if (isDimmed) scheme.outline else kindColors.container
        val contentColor = if (isDimmed) scheme.surface else kindColors.content
        val marker = remember(point.kind, point.stopNumber, isDimmed, isSelected, fill, contentColor, density) {
            val isWaypoint = point.kind == RoutePointKind.WAYPOINT
            drawRouteMarker(
                diameter = if (isWaypoint) WAYPOINT_MARKER_SIZE else POINT_MARKER_SIZE,
                fill = fill,
                border = if (isWaypoint) contentColor else scheme.surface,
                ring = scheme.primary.takeIf { isSelected },
                content = when {
                    isWaypoint -> MarkerContent.None
                    point.stopNumber != null -> MarkerContent.Text(point.stopNumber.toString(), contentColor)
                    else -> MarkerContent.Icon(iconPainter, contentColor)
                },
                textMeasurer = textMeasurer,
                density = density,
            )
        }

        SymbolLayer(
            id = "route-point-${point.itemId}",
            source = rememberGeoJsonSource(data = pointsData(listOf(point.itemId to point.coordinates))),
            onClick = { clicked ->
                if (clicked.isNotEmpty()) {
                    onClick()
                    ClickResult.Consume
                } else ClickResult.Pass
            },
            iconImage = image(marker),
            iconAllowOverlap = const(true),
        )
    }
}

/** The point the open sheet would add, a "+" in the primary color. */
@Composable
@MaplibreComposable
private fun NewPointMarker(coordinates: LatLong?) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val addPainter = rememberVectorPainter(Icons.Default.Add)
    val marker = remember(scheme, density) {
        drawRouteMarker(
            diameter = NEW_POINT_MARKER_SIZE, fill = scheme.surface, border = scheme.primary,
            ring = scheme.primary.copy(alpha = 0.35f),
            content = MarkerContent.Icon(addPainter, scheme.primary),
            textMeasurer = textMeasurer, density = density,
        )
    }
    SymbolLayer(
        id = "route-new-point",
        source = rememberGeoJsonSource(data = pointsData(listOfNotNull(coordinates?.let { "new" to it }))),
        iconImage = image(marker),
        iconAllowOverlap = const(true),
    )
}

// ─── Marker bitmaps ───────────────────────────────────────────────────────────

private sealed interface MarkerContent {
    data object None : MarkerContent
    data class Text(val text: String, val color: Color) : MarkerContent
    data class Icon(val painter: Painter, val color: Color) : MarkerContent
}

/**
 * A round marker as a ready bitmap: maplibre renders a Painter on its render thread while the main
 * thread waits, which froze the Schneaggmap with many markers (see MapContentLayers).
 */
private fun drawRouteMarker(
    diameter: Dp,
    fill: Color,
    border: Color,
    content: MarkerContent,
    textMeasurer: TextMeasurer,
    density: Density,
    ring: Color? = null,
): ImageBitmap {
    val radiusPx = with(density) { diameter.toPx() } / 2f
    val ringPx = if (ring != null) with(density) { SELECTED_RING.toPx() } else 0f
    val borderPx = with(density) { MARKER_BORDER.toPx() }
    val sizePx = ((radiusPx + ringPx) * 2).roundToInt().coerceAtLeast(1)

    val bitmap = ImageBitmap(width = sizePx, height = sizePx, config = ImageBitmapConfig.Argb8888, hasAlpha = true)
    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = Size(sizePx.toFloat(), sizePx.toFloat()),
    ) {
        ring?.let { drawCircle(color = it, radius = radiusPx + ringPx, center = center) }
        drawCircle(color = border, radius = radiusPx, center = center)
        drawCircle(color = fill, radius = radiusPx - borderPx, center = center)

        when (content) {
            MarkerContent.None -> Unit
            is MarkerContent.Text -> {
                val layout = textMeasurer.measure(
                    text = AnnotatedString(content.text),
                    style = TextStyle(color = content.color, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    density = density,
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f),
                )
            }
            is MarkerContent.Icon -> {
                val iconPx = radiusPx * 1.2f
                translate(left = center.x - iconPx / 2f, top = center.y - iconPx / 2f) {
                    with(content.painter) {
                        draw(size = Size(iconPx, iconPx), colorFilter = ColorFilter.tint(content.color))
                    }
                }
            }
        }
    }
    return bitmap
}
