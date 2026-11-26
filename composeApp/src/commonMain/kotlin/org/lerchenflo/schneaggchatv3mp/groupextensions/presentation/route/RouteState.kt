package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import kotlinx.datetime.LocalDate
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteTransportMode
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapLayersData
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapLayersAction
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.MapLayersDisplay

enum class RouteTab { MAP, LIST }

data class RouteState(
    val title: String = "",
    val isLoading: Boolean = true,
    val extensionMissing: Boolean = false,
    val mapStyleUrl: String? = null,
    /** The main route in order. */
    val points: List<RoutePointUi> = emptyList(),
    /** The alternative paths, each with its own points. */
    val paths: List<RoutePathUi> = emptyList(),
    val days: List<RouteDayUi> = emptyList(),
    val totalMeters: Double = 0.0,
    val totalMinutes: Int = 0,
    val selectedTab: RouteTab = RouteTab.MAP,
    /** Index into [days]; null = whole route. */
    val selectedDayIndex: Int? = null,
    /** Point whose card is shown on the map. */
    val selectedPointId: String? = null,
    /** The point cards at the bottom; a tap on the empty map hides them, selecting a point shows them again. */
    val isPointRowVisible: Boolean = true,
    /** Path whose points the cards show (one of its points or its line was tapped). */
    val selectedPathId: String? = null,
    /** Places, friends and events of the shared map layers, and which of them are shown. */
    val layers: MapLayersData = MapLayersData(),
    val layerDisplay: MapLayersDisplay = MapLayersDisplay(),
    /** Place whose "add to route" card is open. */
    val selectedPlace: RouteMapPlaceUi? = null,
    val selectedPlaceInsertHint: RouteInsertHint? = null,
    /** The open add/edit sheet; also kept while the point is being moved on the map. */
    val pointDraft: RoutePointDraft? = null,
    /** The open add/edit sheet of an alternative path. */
    val pathDraft: RoutePathDraft? = null,
    /** A long press on an earlier point: close the loop there or add a point. */
    val loopOffer: RouteLoopOffer? = null,
    /** A delete that takes paths with it, waiting for the user's yes. */
    val deleteConfirm: RouteDeleteConfirm? = null,
    /** The crosshair overlay picks a new position for [pointDraft]. */
    val isMovingPoint: Boolean = false,
    val isReorderVisible: Boolean = false,
    /** The leg whose transport mode is being changed (tap on its badge). */
    val legModeEdit: RouteLegModeEdit? = null,
    val locationPermissionGranted: Boolean = false,
    /** One-shot camera move, cleared with [RouteAction.OnCameraRequestHandled]. */
    val cameraRequest: RouteCameraRequest? = null,
) {
    /** Points of the selected day, else the whole route. */
    val shownPoints: List<RoutePointUi>
        get() = selectedDayIndex?.let { day -> points.filter { it.dayIndex == day } } ?: points

    val selectedPath: RoutePathUi? get() = selectedPathId?.let { id -> paths.find { it.itemId == id } }

    /** The points of the part [pointItemId] lies on: the main route or one path. */
    fun partPointsOf(pointItemId: String): List<RoutePointUi> =
        paths.firstOrNull { path -> path.points.any { it.itemId == pointItemId } }?.points ?: points

    /** The cards at the bottom: the selected path's points, else the shown main route points. */
    val rowPoints: List<RoutePointUi> get() = selectedPath?.points ?: shownPoints

    /** Day chips only make sense for a route over more than one day. */
    val showDayChips: Boolean get() = days.size > 1

    val shownMeters: Double get() = selectedDayIndex?.let { days.getOrNull(it)?.meters } ?: totalMeters
    val shownMinutes: Int get() = selectedDayIndex?.let { days.getOrNull(it)?.minutes } ?: totalMinutes
}

/** One route point as the map, the cards and the list show it. */
data class RoutePointUi(
    val itemId: String,
    val kind: RoutePointKind,
    /** Running number of the stops, null for the other kinds (they show their icon). */
    val stopNumber: Int?,
    /** Shown name: the type's label for a drive-through point left without a name. */
    val name: String,
    /** False for a drive-through point left without a name. */
    val hasName: Boolean,
    val description: String,
    val coordinates: LatLong,
    val plannedAt: Long?,
    val dayIndex: Int,
    val authorName: String,
    /** How the group gets here; null for the first point. */
    val arrivalMode: RouteTransportMode?,
    /** The way on to the next point, null for the last one. */
    val nextLeg: RouteLegUi?,
    val mapEntryId: String?,
    /** The linked Schneaggmap entry still exists ("open place" button). */
    val isMapEntryAvailable: Boolean,
    /** Point creator, extension creator or group admin (server rule). */
    val canDelete: Boolean,
    /** The alternative path it lies on; null = main route. */
    val pathId: String? = null,
) {
    /** A name or a description: a card at the bottom of the map; without, a small drive-through dot. */
    val hasDetails: Boolean get() = hasName || description.isNotBlank()
}

/** The way on from a point to the next point with details (the dots in between add their legs). */
data class RouteLegUi(
    val meters: Double,
    val minutes: Int,
    /** How the first leg is travelled: the mode of the point it leads to. */
    val mode: RouteTransportMode,
    /** Null when the way ends at a point without a name. */
    val toName: String?,
)

/** An alternative way between two main route points (the same one for a side trip). */
data class RoutePathUi(
    val itemId: String,
    val name: String,
    val description: String,
    val mode: RouteTransportMode,
    val fromItemId: String,
    val toItemId: String,
    /** The path it leaves from (a nested path); null = the main route. */
    val parentPathId: String?,
    /** Null while that point is missing (deleted by someone). */
    val fromName: String?,
    val toName: String?,
    val isSideTrip: Boolean,
    val points: List<RoutePointUi>,
    /** Start, own points, rejoin point; empty while an end is missing. */
    val line: List<LatLong>,
    /** Mode of each leg of [line]. */
    val legModes: List<RouteTransportMode>,
    /** Whose mode each leg of [line] is: the path point it leads to, the path itself for the way back. */
    val legModeItemIds: List<String>,
    val meters: Double,
    /** Unrounded, so side trips add up exactly in the day totals. */
    val rawMinutes: Double,
    val minutes: Int,
    /** The day of its start point. */
    val dayIndex: Int,
    /** Path creator, extension creator or group admin (server rule). */
    val canDelete: Boolean,
) {
    val isBroken: Boolean get() = line.isEmpty()
}

data class RouteDayUi(
    val index: Int,
    /** Null when no point has a planned time. */
    val date: LocalDate?,
    val meters: Double,
    val minutes: Int,
)

data class RouteMapPlaceUi(
    val id: String,
    val name: String,
    val coordinates: LatLong,
)

/** Where a new point goes: between [afterName] and [beforeName] (either null at the ends). */
data class RouteInsertHint(
    val afterName: String?,
    val beforeName: String?,
    val extraMeters: Double,
)

/** Values of the add/edit sheet. [itemId] null = new point. */
data class RoutePointDraft(
    val itemId: String? = null,
    val kind: RoutePointKind = RoutePointKind.STOP,
    val name: String = "",
    val description: String = "",
    val coordinates: LatLong,
    val plannedAt: Long? = null,
    val mapEntryId: String? = null,
    /** How the group gets to this point; not shown for the first point. */
    val transportMode: RouteTransportMode = RouteTransportMode.DEFAULT,
    /** TRANSFER only: the mode of the way after it. */
    val continueTransportMode: RouteTransportMode = RouteTransportMode.DEFAULT,
    /** The path it goes into; null = main route. */
    val pathId: String? = null,
    val isFirstPoint: Boolean = false,
    val insertHint: RouteInsertHint? = null,
    val canDelete: Boolean = false,
    val showNameError: Boolean = false,
    val isSaving: Boolean = false,
)

/** Values of the alternative path sheet. [itemId] null = new path. */
data class RoutePathDraft(
    val itemId: String? = null,
    val name: String = "",
    val description: String = "",
    val mode: RouteTransportMode,
    val fromItemId: String,
    /** The same as [fromItemId] for a side trip. */
    val toItemId: String,
    val canDelete: Boolean = false,
    val showNameError: Boolean = false,
    val isSaving: Boolean = false,
    /** Closing a loop: these points (after the start) move into the new side trip once it is saved. */
    val movePointIds: List<String> = emptyList(),
)

/** A long press on [pointName], an earlier point of the shown part; [coordinates] for "new point here". */
data class RouteLoopOffer(
    val pointItemId: String,
    val pointName: String,
    val coordinates: LatLong,
)

/** The leg whose badge was tapped: [itemId] is the point it leads to, or the path for its way back. */
data class RouteLegModeEdit(
    val itemId: String,
    val title: String,
    val mode: RouteTransportMode,
)

sealed interface RouteDeleteConfirm {
    /** A main route point where paths leave or rejoin: they are deleted with it. */
    data class PointWithPaths(val pointItemId: String, val pathNames: List<String>) : RouteDeleteConfirm

    /** A path and its points. */
    data class Path(val pathItemId: String, val name: String) : RouteDeleteConfirm
}

sealed interface RouteCameraRequest {
    val requestId: Long

    /** Fit all [positions] into view; a single position is centered. */
    data class Fit(val positions: List<LatLong>, override val requestId: Long) : RouteCameraRequest

    /** Move to [position] without changing the zoom. */
    data class Center(val position: LatLong, override val requestId: Long) : RouteCameraRequest
}

sealed interface RouteEvent {
    data class OpenUrl(val url: String) : RouteEvent
}

sealed interface RouteAction {
    data object OnBackClick : RouteAction
    data class OnTabSelect(val tab: RouteTab) : RouteAction
    data class OnDaySelect(val dayIndex: Int?) : RouteAction
    data class OnLayersAction(val action: MapLayersAction) : RouteAction
    data object OnLocateClick : RouteAction
    data object OnCameraRequestHandled : RouteAction
    data object OnNavigateClick : RouteAction

    // Points
    /** [hitRadiusMeters]: how close to a point counts as on it, at the map's zoom. */
    data class OnMapLongPress(val coordinates: LatLong, val hitRadiusMeters: Double = 0.0) : RouteAction
    data object OnLoopMakeSideTripClick : RouteAction
    data object OnLoopNewPointClick : RouteAction
    data object OnLoopOfferDismiss : RouteAction
    // A tap on the map that hit no marker, place, friend or event
    data object OnMapClick : RouteAction
    data class OnPointSelect(val itemId: String) : RouteAction
    // The badge in the middle of the leg leading to the point
    data class OnLegModeClick(val itemId: String) : RouteAction
    data class OnLegModeSelect(val mode: RouteTransportMode) : RouteAction
    data object OnLegModeDismiss : RouteAction
    data class OnPointEditClick(val itemId: String) : RouteAction

    // Alternative paths
    data class OnPathSelect(val pathItemId: String) : RouteAction
    data object OnPathClose : RouteAction
    data class OnPathEditClick(val pathItemId: String) : RouteAction
    data class OnPathDraftNameChange(val name: String) : RouteAction
    data class OnPathDraftDescriptionChange(val description: String) : RouteAction
    data class OnPathDraftModeChange(val mode: RouteTransportMode) : RouteAction
    data class OnPathDraftFromChange(val pointItemId: String) : RouteAction
    data class OnPathDraftToChange(val pointItemId: String) : RouteAction
    data object OnPathDraftSave : RouteAction
    data object OnPathDraftDelete : RouteAction
    data object OnPathDraftDismiss : RouteAction
    data object OnDeleteConfirm : RouteAction
    data object OnDeleteDismiss : RouteAction
    data class OnOpenMapEntryClick(val mapEntryId: String) : RouteAction

    // Schneaggmap places
    data class OnPlaceClick(val placeId: String) : RouteAction
    data object OnPlaceDismiss : RouteAction
    data object OnPlaceAddClick : RouteAction

    // Shared map layers
    data class OnFriendClick(val userId: String) : RouteAction
    data class OnEventClick(val eventId: String) : RouteAction

    // Add/edit sheet
    data class OnDraftKindChange(val kind: RoutePointKind) : RouteAction
    data class OnDraftNameChange(val name: String) : RouteAction
    data class OnDraftDescriptionChange(val description: String) : RouteAction
    data class OnDraftPlannedAtChange(val plannedAt: Long?) : RouteAction
    data class OnDraftTransportModeChange(val mode: RouteTransportMode) : RouteAction
    data class OnDraftContinueModeChange(val mode: RouteTransportMode) : RouteAction
    /** Main route or the item id of a path. */
    data class OnDraftPathChange(val pathItemId: String?) : RouteAction
    /** "Alternative from here": a new path leaving the route at the edited point. */
    data object OnDraftStartPathClick : RouteAction
    data object OnDraftSave : RouteAction
    data object OnDraftDelete : RouteAction
    data object OnDraftDismiss : RouteAction
    data object OnMovePointClick : RouteAction
    data class OnMovePointConfirm(val coordinates: LatLong) : RouteAction
    data object OnMovePointCancel : RouteAction

    // Reorder sheet
    data object OnReorderClick : RouteAction
    /** Item ids in the new order. */
    data class OnReorderSave(val order: List<String>) : RouteAction
    data object OnReorderDismiss : RouteAction
}
