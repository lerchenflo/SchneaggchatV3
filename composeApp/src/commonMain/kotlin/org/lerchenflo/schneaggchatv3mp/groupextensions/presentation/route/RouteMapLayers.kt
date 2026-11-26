package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
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
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.safeAdd
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.toPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FeaturesClickHandler
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import kotlin.math.roundToInt

private val POINT_MARKER_SIZE = 28.dp
private val WAYPOINT_MARKER_SIZE = 12.dp
private val PLACE_MARKER_SIZE = 24.dp
private val NEW_POINT_MARKER_SIZE = 30.dp
private val MARKER_BORDER = 2.dp
private val SELECTED_RING = 4.dp

/**
 * Everything the route planner draws on the map: the route line (the picked day bright, the
 * others grey), one marker per point, the Schneaggmap places while "Orte" is on, the marker of a
 * point being added, and the own position. Every source always exists (empty when there is
 * nothing to draw), so no source is removed and added again (CannotAddSourceException).
 */
@Composable
@MaplibreComposable
fun RouteMapLayers(
    state: RouteState,
    ownPosition: Position?,
    onAction: (RouteAction) -> Unit,
) {
    RouteLineLayers(state)
    MapPlaceLayers(state, onAction)

    // Drive-through dots first, so the bigger markers lie on top of them
    val (waypoints, others) = state.points.partition { it.kind == RoutePointKind.WAYPOINT }
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
    OwnPositionLayer(ownPosition)
}

@Composable
@MaplibreComposable
private fun RouteLineLayers(state: RouteState) {
    val scheme = MaterialTheme.colorScheme
    // A leg belongs to the day of the point it leads to
    val (activeLegs, dimmedLegs) = state.points.zipWithNext()
        .partition { (_, to) -> state.selectedDayIndex == null || to.dayIndex == state.selectedDayIndex }

    val dimmedSource = rememberGeoJsonSource(data = legsData(dimmedLegs))
    LineLayer(
        id = "route-line-dimmed",
        source = dimmedSource,
        color = const(scheme.outline),
        width = const(3.dp),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )

    val activeSource = rememberGeoJsonSource(data = legsData(activeLegs))
    // Casing under the line, so it stands out on every map color
    LineLayer(
        id = "route-line-casing",
        source = activeSource,
        color = const(scheme.surface.copy(alpha = 0.8f)),
        width = const(7.dp),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )
    LineLayer(
        id = "route-line",
        source = activeSource,
        color = const(scheme.primary),
        width = const(4.dp),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )
}

private fun legsData(legs: List<Pair<RoutePointUi, RoutePointUi>>) = GeoJsonData.Features(
    FeatureCollection(
        features = legs.map { (from, to) ->
            Feature(
                geometry = LineString(listOf(from.coordinates.toPosition(), to.coordinates.toPosition())),
                properties = buildJsonObject {},
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

@Composable
@MaplibreComposable
private fun MapPlaceLayers(state: RouteState, onAction: (RouteAction) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val placePainter = rememberVectorPainter(Icons.Default.Place)
    val placeIcon = remember(scheme, density) {
        drawRouteMarker(
            diameter = PLACE_MARKER_SIZE, fill = scheme.surfaceVariant, border = scheme.outline,
            content = MarkerContent.Icon(placePainter, scheme.onSurfaceVariant),
            textMeasurer = textMeasurer, density = density,
        )
    }
    val selectedPlaceIcon = remember(scheme, density) {
        drawRouteMarker(
            diameter = PLACE_MARKER_SIZE, fill = scheme.surfaceVariant, border = scheme.outline, ring = scheme.primary,
            content = MarkerContent.Icon(placePainter, scheme.onSurfaceVariant),
            textMeasurer = textMeasurer, density = density,
        )
    }

    val onPlaceClick: FeaturesClickHandler = { clicked ->
        val id = clicked.firstOrNull()?.id?.content
        if (id != null) {
            onAction(RouteAction.OnPlaceClick(id))
            ClickResult.Consume
        } else ClickResult.Pass
    }

    val places = if (state.showMapPlaces) state.mapPlaces.map { it.id to it.coordinates } else emptyList()
    SymbolLayer(
        id = "route-places",
        source = rememberGeoJsonSource(data = pointsData(places)),
        onClick = onPlaceClick,
        iconImage = image(placeIcon),
        iconAllowOverlap = const(true),
    )

    val selected = state.selectedPlace?.let { listOf(it.id to it.coordinates) }.orEmpty()
    SymbolLayer(
        id = "route-place-selected",
        source = rememberGeoJsonSource(data = pointsData(selected)),
        iconImage = image(selectedPlaceIcon),
        iconAllowOverlap = const(true),
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

@Composable
@MaplibreComposable
private fun OwnPositionLayer(position: Position?) {
    val scheme = MaterialTheme.colorScheme
    val features = listOfNotNull(position?.let { Feature(geometry = Point(it), properties = buildJsonObject {}) })
    CircleLayer(
        id = "route-own-position",
        source = rememberGeoJsonSource(data = GeoJsonData.Features(FeatureCollection(features = features))),
        color = const(scheme.secondary),
        radius = const(7.dp),
        strokeColor = const(scheme.surface),
        strokeWidth = const(2.dp),
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
 * thread waits, which froze the Schneaggmap with many markers (see SchneaggmapLayers).
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
