package org.lerchenflo.schneaggchatv3mp.games.presentation.ghettoguesser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.buildJsonObject
import org.lerchenflo.schneaggchatv3mp.games.domain.GuessMap
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters
import org.lerchenflo.schneaggchatv3mp.utilities.midpoint
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

private const val INITIAL_ZOOM = 12.0

/** Below this distance fitting both pins would zoom in too far, so a fixed zoom is used instead. */
private const val CLOSE_RESULT_METERS = 150.0
private const val CLOSE_RESULT_ZOOM = 16.0

private val PIN_SIZE = DpSize(36.dp, 46.dp)

/** The pins stick out above their location, so the top needs more room than the other sides. */
private val RESULT_FIT_PADDING = DpPadding(left = 48.dp, top = 80.dp, right = 48.dp, bottom = 48.dp)
private val OUTLINE_FIT_PADDING = DpPadding(left = 32.dp, top = 32.dp, right = 32.dp, bottom = 32.dp)

/**
 * Map of [map] with its outline. While [onMapClick] is set, a tap places the guess. Once [actual]
 * is known, the real location and a line to the guess are drawn and the camera zooms in on both.
 */
@Composable
fun GhettoGuesserMap(
    map: GuessMap,
    styleUrl: String,
    guess: LatLong?,
    actual: LatLong?,
    onMapClick: ((LatLong) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val guessPin = rememberPinPainter(
        icon = Icons.Default.Person,
        fillColor = colors.primary,
        iconColor = colors.onPrimary,
        outlineColor = colors.surface,
    )
    val actualPin = rememberPinPainter(
        icon = Icons.Default.PhotoCamera,
        fillColor = colors.tertiary,
        iconColor = colors.onTertiary,
        outlineColor = colors.surface,
    )
    val outlineColor = colors.primary
    val distanceLineColor = colors.onSurface

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(target = map.center().toPosition(), zoom = INITIAL_ZOOM),
    ) {
        key(map.id) {
            val outlineSource = rememberGeoJsonSource(
                data = lineData(map.polygon + map.polygon.first())
            )
            LineLayer(
                id = "ghettoguesser-outline",
                source = outlineSource,
                color = const(outlineColor),
                width = const(3.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
            )
        }

        if (guess != null && actual != null) {
            key(guess, actual) {
                val distanceSource = rememberGeoJsonSource(data = lineData(listOf(guess, actual)))
                LineLayer(
                    id = "ghettoguesser-distance",
                    source = distanceSource,
                    color = const(distanceLineColor),
                    width = const(3.dp),
                    dasharray = const(listOf(2.0, 1.5)),
                    cap = const(LineCap.Round),
                )
            }
        }

        guess?.let { location ->
            key(location) {
                PinLayer(id = "ghettoguesser-guess", location = location, pin = guessPin)
            }
        }

        actual?.let { location ->
            key(location) {
                PinLayer(id = "ghettoguesser-actual", location = location, pin = actualPin)
            }
        }
    }

    LaunchedEffect(map.id, actual) {
        if (guess != null && actual != null) {
            if (distanceMeters(guess, actual) < CLOSE_RESULT_METERS) {
                mapState.animateCamera(
                    CameraUpdate(target = midpoint(guess, actual).toPosition(), zoom = CLOSE_RESULT_ZOOM)
                )
            } else {
                mapState.animateCameraToBounds(
                    boundingBox = boundsOf(listOf(guess, actual)),
                    fitPadding = RESULT_FIT_PADDING,
                )
            }
        } else {
            mapState.animateCameraToBounds(
                boundingBox = boundsOf(map.polygon),
                fitPadding = OUTLINE_FIT_PADDING,
            )
        }
    }

    MaplibreMap(
        modifier = modifier,
        state = mapState,
        interactions = MapInteractions {
            callbacks {
                click {
                    onUnhandled { event ->
                        val position = event.position
                        if (onMapClick != null && position != null) {
                            onMapClick(LatLong(position.latitude, position.longitude))
                        }
                        ClickResult.Pass
                    }
                }
            }
        },
    )
}

@Composable
@MaplibreComposable
private fun PinLayer(id: String, location: LatLong, pin: Painter) {
    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            FeatureCollection(
                features = listOf(Feature(geometry = Point(location.toPosition()), properties = buildJsonObject {}))
            )
        )
    )
    SymbolLayer(
        id = id,
        source = source,
        iconImage = image(pin, size = PIN_SIZE),
        iconAnchor = const(SymbolAnchor.Bottom),
        iconAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
    )
}

@Composable
private fun rememberPinPainter(
    icon: ImageVector,
    fillColor: Color,
    iconColor: Color,
    outlineColor: Color,
): Painter {
    val iconPainter = rememberVectorPainter(icon)
    return remember(iconPainter, fillColor, iconColor, outlineColor) {
        PinPainter(iconPainter, fillColor, iconColor, outlineColor)
    }
}

/** Teardrop map pin with an icon in its round head; the tip is the bottom center. */
private class PinPainter(
    private val icon: VectorPainter,
    private val fillColor: Color,
    private val iconColor: Color,
    private val outlineColor: Color,
) : Painter() {

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val strokeWidth = size.width * 0.07f
        val radius = size.width / 2 - strokeWidth
        val centerX = size.width / 2
        val centerY = strokeWidth + radius
        val tipY = size.height - strokeWidth / 2

        // The two straight edges touch the head where the tangents from the tip meet the circle
        val tangentAngle = acos((radius / (tipY - centerY)).toDouble())
        val startAngle = PI / 2 - tangentAngle
        val path = Path().apply {
            moveTo(centerX, tipY)
            lineTo(
                centerX + radius * cos(startAngle).toFloat(),
                centerY + radius * sin(startAngle).toFloat(),
            )
            arcTo(
                rect = Rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                startAngleDegrees = startAngle.toDegrees(),
                sweepAngleDegrees = -(360f - 2 * tangentAngle.toDegrees()),
                forceMoveTo = false,
            )
            close()
        }
        drawPath(path, fillColor)
        drawPath(path, outlineColor, style = Stroke(width = strokeWidth))

        val iconSize = radius * 1.2f
        translate(left = centerX - iconSize / 2, top = centerY - iconSize / 2) {
            with(icon) {
                draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(iconColor))
            }
        }
    }
}

private fun Double.toDegrees(): Float = (this * 180.0 / PI).toFloat()

private fun lineData(points: List<LatLong>): GeoJsonData =
    GeoJsonData.Features(
        FeatureCollection(
            Feature(
                geometry = LineString(points.map { it.toPosition() }),
                properties = buildJsonObject {},
            )
        )
    )

private fun boundsOf(points: List<LatLong>) = BoundingBox(
    west = points.minOf { it.long },
    south = points.minOf { it.lat },
    east = points.maxOf { it.long },
    north = points.maxOf { it.lat },
)

private fun LatLong.toPosition() = Position(longitude = long, latitude = lat)

private fun GuessMap.center() = LatLong(
    lat = (polygon.minOf { it.lat } + polygon.maxOf { it.lat }) / 2,
    long = (polygon.minOf { it.long } + polygon.maxOf { it.long }) / 2,
)
