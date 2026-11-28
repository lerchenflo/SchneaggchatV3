package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.lerchenflo.schneaggchatv3mp.app.theme.warningColors
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteTransportMode
import org.lerchenflo.schneaggchatv3mp.utilities.formatDistance
import org.lerchenflo.schneaggchatv3mp.utilities.millisToLocalDateTime
import org.lerchenflo.schneaggchatv3mp.utilities.millisToString
import org.lerchenflo.schneaggchatv3mp.utilities.weekdayShortResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.route_day_number
import schneaggchatv3mp.composeapp.generated.resources.route_insert_between
import schneaggchatv3mp.composeapp.generated.resources.route_insert_end
import schneaggchatv3mp.composeapp.generated.resources.route_insert_first
import schneaggchatv3mp.composeapp.generated.resources.route_insert_start
import schneaggchatv3mp.composeapp.generated.resources.route_kind_accommodation
import schneaggchatv3mp.composeapp.generated.resources.route_kind_accommodation_use
import schneaggchatv3mp.composeapp.generated.resources.route_kind_poi
import schneaggchatv3mp.composeapp.generated.resources.route_kind_poi_use
import schneaggchatv3mp.composeapp.generated.resources.route_kind_transfer
import schneaggchatv3mp.composeapp.generated.resources.route_path_between
import schneaggchatv3mp.composeapp.generated.resources.route_path_broken
import schneaggchatv3mp.composeapp.generated.resources.route_path_side_trip
import schneaggchatv3mp.composeapp.generated.resources.route_kind_transfer_use
import schneaggchatv3mp.composeapp.generated.resources.route_kind_stop
import schneaggchatv3mp.composeapp.generated.resources.route_kind_stop_use
import schneaggchatv3mp.composeapp.generated.resources.route_kind_waypoint
import schneaggchatv3mp.composeapp.generated.resources.route_kind_waypoint_use
import schneaggchatv3mp.composeapp.generated.resources.route_transport_bicycle
import schneaggchatv3mp.composeapp.generated.resources.route_transport_car
import schneaggchatv3mp.composeapp.generated.resources.route_transport_foot
import schneaggchatv3mp.composeapp.generated.resources.route_transport_motorbike
import schneaggchatv3mp.composeapp.generated.resources.route_whole_route

// ─── Point kinds ──────────────────────────────────────────────────────────────

fun RoutePointKind.labelRes(): StringResource = when (this) {
    RoutePointKind.STOP -> Res.string.route_kind_stop
    RoutePointKind.WAYPOINT -> Res.string.route_kind_waypoint
    RoutePointKind.ACCOMMODATION -> Res.string.route_kind_accommodation
    RoutePointKind.POI -> Res.string.route_kind_poi
    RoutePointKind.TRANSFER -> Res.string.route_kind_transfer
}

/** What the kind is for, shown in the type dropdown. */
fun RoutePointKind.useCaseRes(): StringResource = when (this) {
    RoutePointKind.STOP -> Res.string.route_kind_stop_use
    RoutePointKind.WAYPOINT -> Res.string.route_kind_waypoint_use
    RoutePointKind.ACCOMMODATION -> Res.string.route_kind_accommodation_use
    RoutePointKind.POI -> Res.string.route_kind_poi_use
    RoutePointKind.TRANSFER -> Res.string.route_kind_transfer_use
}

/** Icon of the kind; a stop shows its number instead wherever it has one. */
fun RoutePointKind.icon(): ImageVector = when (this) {
    RoutePointKind.STOP -> Icons.Default.Flag
    RoutePointKind.WAYPOINT -> Icons.Default.FiberManualRecord
    RoutePointKind.ACCOMMODATION -> Icons.Default.Hotel
    RoutePointKind.POI -> Icons.Default.Star
    RoutePointKind.TRANSFER -> Icons.Default.SyncAlt
}

/** Fill and content color of a point's badge and map marker. */
data class RoutePointColors(val container: Color, val content: Color)

@Composable
fun RoutePointKind.colors(): RoutePointColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        RoutePointKind.STOP -> RoutePointColors(scheme.primaryContainer, scheme.onPrimaryContainer)
        RoutePointKind.WAYPOINT -> RoutePointColors(scheme.surface, scheme.onSurface)
        RoutePointKind.ACCOMMODATION -> RoutePointColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
        RoutePointKind.POI -> RoutePointColors(MaterialTheme.warningColors.warning, MaterialTheme.warningColors.onWarning)
        RoutePointKind.TRANSFER -> RoutePointColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
    }
}

// ─── Transport ────────────────────────────────────────────────────────────────

fun RouteTransportMode.labelRes(): StringResource = when (this) {
    RouteTransportMode.FOOT -> Res.string.route_transport_foot
    RouteTransportMode.BICYCLE -> Res.string.route_transport_bicycle
    RouteTransportMode.MOTORBIKE -> Res.string.route_transport_motorbike
    RouteTransportMode.CAR -> Res.string.route_transport_car
}

/** A leg's line and badge color (container) and the badge icon on it (content). */
@Composable
fun RouteTransportMode.colors(): RoutePointColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        RouteTransportMode.FOOT -> RoutePointColors(scheme.tertiary, scheme.onTertiary)
        RouteTransportMode.BICYCLE -> RoutePointColors(scheme.primary, scheme.onPrimary)
        RouteTransportMode.MOTORBIKE -> RoutePointColors(MaterialTheme.warningColors.warning, MaterialTheme.warningColors.onWarning)
        RouteTransportMode.CAR -> RoutePointColors(scheme.secondary, scheme.onSecondary)
    }
}

fun RouteTransportMode.icon(): ImageVector = when (this) {
    RouteTransportMode.FOOT -> Icons.AutoMirrored.Filled.DirectionsWalk
    RouteTransportMode.BICYCLE -> Icons.Default.PedalBike
    RouteTransportMode.MOTORBIKE -> Icons.Default.TwoWheeler
    RouteTransportMode.CAR -> Icons.Default.DirectionsCar
}

// ─── Formatting ───────────────────────────────────────────────────────────────

/** Rough travel time with a tilde, e.g. "~45 min" or "~2 h 05". */
fun formatRoughDuration(minutes: Int): String {
    if (minutes < 60) return "~$minutes min"
    val rest = (minutes % 60).toString().padStart(2, '0')
    return "~${minutes / 60} h $rest"
}

/** "12 km · ~15 min" */
fun formatDistanceAndTime(meters: Double, minutes: Int): String =
    "${formatDistance(meters)} · ${formatRoughDuration(minutes)}"

/** Clock time of a planned point, e.g. "09:30". */
fun formatPlannedTime(millis: Long): String = millisToString(millis, "HH:mm")

/** Short date with weekday, e.g. "Sa 12.6." */
@Composable
fun formatRouteDate(date: LocalDate): String =
    "${stringResource(weekdayShortResource(date.dayOfWeek))} ${date.day}.${date.month.ordinal + 1}."

/** Weekday, date and time of a planned point, e.g. "Sa 12.6. 09:30" */
@Composable
fun formatPlannedDateTime(millis: Long): String =
    "${formatRouteDate(millisToLocalDateTime(millis).date)} ${formatPlannedTime(millis)}"

/** "Tag 1 · Sa 12.6.", or "Route" for a route without dates. */
@Composable
fun dayTitle(day: RouteDayUi): String {
    val date = day.date ?: return stringResource(Res.string.route_whole_route)
    return "${stringResource(Res.string.route_day_number, day.index + 1)} · ${formatRouteDate(date)}"
}

/** Where a new point goes, e.g. "Goes between Bludenz and Schruns (+0.7 km)". */
@Composable
fun insertHintText(hint: RouteInsertHint): String {
    val extra = "+${formatDistance(hint.extraMeters)}"
    return when {
        hint.afterName != null && hint.beforeName != null ->
            stringResource(Res.string.route_insert_between, hint.afterName, hint.beforeName, extra)
        hint.beforeName != null -> stringResource(Res.string.route_insert_start, hint.beforeName, extra)
        hint.afterName != null -> stringResource(Res.string.route_insert_end, hint.afterName, extra)
        else -> stringResource(Res.string.route_insert_first)
    }
}

/** "Side trip from A", "From A, back at B", or that an end was deleted. */
@Composable
fun routePathEndsText(path: RoutePathUi): String = when {
    path.fromName == null || path.toName == null -> stringResource(Res.string.route_path_broken)
    path.isSideTrip -> stringResource(Res.string.route_path_side_trip, path.fromName)
    else -> stringResource(Res.string.route_path_between, path.fromName, path.toName)
}
