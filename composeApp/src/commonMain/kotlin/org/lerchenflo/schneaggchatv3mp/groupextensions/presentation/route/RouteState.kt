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
    /** The whole route in order. */
    val points: List<RoutePointUi> = emptyList(),
    val days: List<RouteDayUi> = emptyList(),
    val totalMeters: Double = 0.0,
    val totalMinutes: Int = 0,
    val selectedTab: RouteTab = RouteTab.MAP,
    /** Index into [days]; null = whole route. */
    val selectedDayIndex: Int? = null,
    /** Point whose card is shown on the map. */
    val selectedPointId: String? = null,
    /** Places, friends and events of the shared map layers, and which of them are shown. */
    val layers: MapLayersData = MapLayersData(),
    val layerDisplay: MapLayersDisplay = MapLayersDisplay(),
    /** Place whose "add to route" card is open. */
    val selectedPlace: RouteMapPlaceUi? = null,
    val selectedPlaceInsertHint: RouteInsertHint? = null,
    /** The open add/edit sheet; also kept while the point is being moved on the map. */
    val pointDraft: RoutePointDraft? = null,
    /** The crosshair overlay picks a new position for [pointDraft]. */
    val isMovingPoint: Boolean = false,
    val isReorderVisible: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    /** One-shot camera move, cleared with [RouteAction.OnCameraRequestHandled]. */
    val cameraRequest: RouteCameraRequest? = null,
) {
    /** Points of the selected day, else the whole route. */
    val shownPoints: List<RoutePointUi>
        get() = selectedDayIndex?.let { day -> points.filter { it.dayIndex == day } } ?: points

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
    val name: String,
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
)

data class RouteLegUi(
    val meters: Double,
    val minutes: Int,
    /** How this leg is travelled: the mode of the point it leads to. */
    val mode: RouteTransportMode,
    val toName: String,
)

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
    val isFirstPoint: Boolean = false,
    val insertHint: RouteInsertHint? = null,
    val canDelete: Boolean = false,
    val showNameError: Boolean = false,
    val isSaving: Boolean = false,
)

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
    data class OnMapLongPress(val coordinates: LatLong) : RouteAction
    data class OnPointSelect(val itemId: String) : RouteAction
    data class OnPointEditClick(val itemId: String) : RouteAction
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
