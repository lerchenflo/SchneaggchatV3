package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.lerchenflo.schneaggchatv3mp.app.ApplicationScope
import org.lerchenflo.schneaggchatv3mp.app.SessionCache
import org.lerchenflo.schneaggchatv3mp.app.domain.ErrorChannel
import org.lerchenflo.schneaggchatv3mp.app.domain.isSuccess
import org.lerchenflo.schneaggchatv3mp.app.domain.reportErrorTo
import org.lerchenflo.schneaggchatv3mp.app.launchOnMain
import org.lerchenflo.schneaggchatv3mp.app.navigation.Navigator
import org.lerchenflo.schneaggchatv3mp.app.navigation.Route
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupMember
import org.lerchenflo.schneaggchatv3mp.chat.domain.GroupRepository
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentity
import org.lerchenflo.schneaggchatv3mp.chat.domain.UserIdentityRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.RoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.MAX_NAVIGATION_POINTS
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.PlannedRoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteTransportMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.arrivalMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.mostUsedArrivalMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.googleMapsRouteLink
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.insertPosition
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.nearestSegmentPosition
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.routePointsOf
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.sortIndexUpdates
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.timeOrderPosition
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapEntry
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapLayersSource
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.MapRepository
import org.lerchenflo.schneaggchatv3mp.settings.domain.SettingsRepository
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.reduce
import org.lerchenflo.schneaggchatv3mp.sharedUi.map.layers.withPlacesDropdownClosed
import org.lerchenflo.schneaggchatv3mp.sharedUi.text.userIdentityNameText
import org.lerchenflo.schneaggchatv3mp.utilities.SnackbarManager
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import org.lerchenflo.schneaggchatv3mp.utilities.PermissionState
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.route_kind_waypoint
import schneaggchatv3mp.composeapp.generated.resources.route_navigation_cut
import schneaggchatv3mp.composeapp.generated.resources.route_point_deleted
import schneaggchatv3mp.composeapp.generated.resources.undo
import kotlin.time.Clock

/** Server limits of a route point (ExtensionItemData.RoutePoint.validate on the server). */
const val ROUTE_POINT_NAME_MAX_LENGTH = 100
const val ROUTE_POINT_DESCRIPTION_MAX_LENGTH = 500

/** Stands in for a point that has no item id yet when the order is worked out. */
private const val NEW_POINT_KEY = "new-route-point"

/**
 * Backs the route planner: the group's points on a map and as a day list. Every change is saved
 * at once, one item per point; the order lives in each point's sortIndex (see
 * [sortIndexUpdates]). Writes run in the app scope, so they finish after the screen is left.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RouteViewModel(
    private val errorChannel: ErrorChannel,
    private val groupExtensionRepository: GroupExtensionRepository,
    private val groupRepository: GroupRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val mapRepository: MapRepository,
    private val settingsRepository: SettingsRepository,
    private val mapLayersSource: MapLayersSource,
    private val locationService: LocationService,
    private val applicationScope: ApplicationScope,
    private val navigator: Navigator,

    private val extensionId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(RouteState())
    val state = _state.asStateFlow()

    private val _events = Channel<RouteEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val ownId = SessionCache.requireLoggedIn()?.userId

    /** The stored points in route order; every write is worked out from it. */
    private var route: List<PlannedRoutePoint> = emptyList()
    private var extension: GroupExtension? = null
    /** Transport of legs whose point has none of its own, set when the route was created. */
    private var routeDefaultMode = RouteTransportMode.DEFAULT

    private var cameraRequestId = 0L
    private var hasFittedRoute = false

    private data class RouteSource(
        val extension: GroupExtension,
        val route: List<PlannedRoutePoint>,
        val members: List<GroupMember>,
        val users: Map<String, UserIdentity>,
        val mapEntries: List<MapEntry>,
    )

    init {
        observeRoute()
        observeMapStyle()
        observeMapLayers()
        viewModelScope.launch {
            val granted = locationService.checkLocationPermission() == PermissionState.GRANTED
            _state.update { it.copy(locationPermissionGranted = granted) }
        }
    }

    // ─── Server state ─────────────────────────────────────────────────────────

    private fun observeRoute() {
        viewModelScope.launch {
            val nameText = userIdentityNameText()
            groupExtensionRepository.getExtensionFlow(extensionId)
                .flatMapLatest { extension ->
                    if (extension == null) return@flatMapLatest flowOf(null)
                    val points = routePointsOf(extension.items)
                    combine(
                        groupRepository.getGroupByIdFlow(extension.ownerId),
                        userIdentityRepository.observeUsers(points.map { it.createdBy }.toSet()),
                        mapRepository.getAllMapEntriesFlow(),
                    ) { group, users, entries ->
                        RouteSource(extension, points, group?.members.orEmpty(), users, entries)
                    }
                }
                .collect { source ->
                    if (source == null) {
                        _state.update { it.copy(isLoading = false, extensionMissing = true, pointDraft = null) }
                    } else {
                        applySource(source, authorName = { userId -> nameText(source.users[userId]?.name) })
                    }
                }
        }
    }

    private fun applySource(source: RouteSource, authorName: (String) -> String) {
        extension = source.extension
        route = source.route

        routeDefaultMode = RouteTransportMode.fromStored((source.extension.settings as? ExtensionSettings.Route)?.transportMode)
        val isAdmin = source.members.any { it.userId == ownId && it.admin }
        val content = buildRouteContent(
            route = source.route,
            defaultMode = routeDefaultMode,
            authorName = authorName,
            canDelete = { planned ->
                ownId != null && (planned.createdBy == ownId || source.extension.createdBy == ownId || isAdmin)
            },
            availableMapEntryIds = source.mapEntries.map { it.id }.toSet(),
        )

        _state.update { previous ->
            val pointIds = content.points.map { it.itemId }.toSet()
            previous.copy(
                title = source.extension.title,
                isLoading = false,
                extensionMissing = false,
                points = content.points,
                days = content.days,
                totalMeters = content.totalMeters,
                totalMinutes = content.totalMinutes,
                selectedDayIndex = previous.selectedDayIndex?.takeIf { it < content.days.size && content.days.size > 1 },
                selectedPointId = previous.selectedPointId?.takeIf { it in pointIds },
                // The edited point was deleted by someone else: its sheet closes
                pointDraft = previous.pointDraft?.takeIf { draft -> draft.itemId == null || draft.itemId in pointIds },
            )
        }

        if (!hasFittedRoute && route.isNotEmpty()) {
            hasFittedRoute = true
            requestFit(route.map { it.point.coordinates })
        }
    }

    private fun observeMapStyle() {
        viewModelScope.launch {
            settingsRepository.getMapStyleUrlFlow().collect { url ->
                _state.update { it.copy(mapStyleUrl = url) }
            }
        }
    }

    // Places, friends and events, the same as on the Schneaggmap
    private fun observeMapLayers() {
        viewModelScope.launch {
            mapLayersSource.observe().collect { layers -> _state.update { it.copy(layers = layers) } }
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    fun onAction(action: RouteAction) {
        when (action) {
            RouteAction.OnBackClick -> viewModelScope.launch { navigator.navigateBack() }
            is RouteAction.OnTabSelect -> _state.update { it.copy(selectedTab = action.tab) }
            is RouteAction.OnDaySelect -> selectDay(action.dayIndex)
            is RouteAction.OnLayersAction -> _state.update { it.copy(layerDisplay = it.layerDisplay.reduce(action.action)) }
            RouteAction.OnLocateClick -> requestLocationPermission()
            RouteAction.OnCameraRequestHandled -> _state.update { it.copy(cameraRequest = null) }
            RouteAction.OnNavigateClick -> openNavigation()

            is RouteAction.OnMapLongPress -> {
                if (!_state.value.isMovingPoint) openNewDraft(action.coordinates, name = "", mapEntryId = null)
            }
            is RouteAction.OnPointSelect -> selectPoint(action.itemId)
            is RouteAction.OnPointEditClick -> openEditDraft(action.itemId)
            is RouteAction.OnOpenMapEntryClick -> openMapEntry(action.mapEntryId)

            is RouteAction.OnPlaceClick -> selectPlace(action.placeId)
            RouteAction.OnPlaceDismiss -> _state.update { it.copy(selectedPlace = null, selectedPlaceInsertHint = null) }
            RouteAction.OnPlaceAddClick -> {
                val place = _state.value.selectedPlace ?: return
                openNewDraft(place.coordinates, name = place.name.take(ROUTE_POINT_NAME_MAX_LENGTH), mapEntryId = place.id)
            }

            is RouteAction.OnFriendClick -> openFriend(action.userId)
            is RouteAction.OnEventClick -> viewModelScope.launch { navigator.navigate(Route.EventDetail(eventId = action.eventId)) }

            is RouteAction.OnDraftKindChange -> updateDraft { it.copy(kind = action.kind, showNameError = false) }
            is RouteAction.OnDraftNameChange -> updateDraft {
                it.copy(name = action.name.take(ROUTE_POINT_NAME_MAX_LENGTH), showNameError = false)
            }
            is RouteAction.OnDraftDescriptionChange -> updateDraft {
                it.copy(description = action.description.take(ROUTE_POINT_DESCRIPTION_MAX_LENGTH))
            }
            is RouteAction.OnDraftPlannedAtChange -> updateDraft { it.copy(plannedAt = action.plannedAt).withInsertHint() }
            is RouteAction.OnDraftTransportModeChange -> updateDraft { it.copy(transportMode = action.mode) }
            RouteAction.OnDraftSave -> viewModelScope.launch { saveDraft() }
            RouteAction.OnDraftDelete -> deleteDraftPoint()
            RouteAction.OnDraftDismiss -> _state.update { it.copy(pointDraft = null, isMovingPoint = false) }
            RouteAction.OnMovePointClick -> {
                val draft = _state.value.pointDraft ?: return
                _state.update { it.copy(isMovingPoint = true) }
                requestCenter(draft.coordinates)
            }
            is RouteAction.OnMovePointConfirm -> {
                updateDraft { it.copy(coordinates = action.coordinates).withInsertHint() }
                _state.update { it.copy(isMovingPoint = false) }
            }
            RouteAction.OnMovePointCancel -> _state.update { it.copy(isMovingPoint = false) }

            RouteAction.OnReorderClick -> _state.update { it.copy(isReorderVisible = true) }
            is RouteAction.OnReorderSave -> saveOrder(action.order)
            RouteAction.OnReorderDismiss -> _state.update { it.copy(isReorderVisible = false) }
        }
    }

    // ─── Map ──────────────────────────────────────────────────────────────────

    private fun selectDay(dayIndex: Int?) {
        _state.update { it.copy(selectedDayIndex = dayIndex, selectedPointId = null) }
        val shown = _state.value.shownPoints.map { it.coordinates }
        if (shown.isNotEmpty()) requestFit(shown)
    }

    private fun selectPoint(itemId: String) {
        val point = _state.value.points.find { it.itemId == itemId } ?: return
        _state.update { it.copy(selectedPointId = itemId, selectedPlace = null, selectedPlaceInsertHint = null) }
        requestCenter(point.coordinates)
    }

    private fun selectPlace(placeId: String) {
        val entry = _state.value.layers.entries.find { it.id == placeId } ?: return
        val place = RouteMapPlaceUi(id = entry.id, name = entry.name, coordinates = entry.coordinates)
        val position = nearestSegmentPosition(route.map { it.point.coordinates }, place.coordinates)
        _state.update {
            it.copy(
                selectedPlace = place,
                selectedPlaceInsertHint = insertHintFor(route, position, place.coordinates),
                selectedPointId = null,
                layerDisplay = it.layerDisplay.withPlacesDropdownClosed(),
            )
        }
    }

    private fun requestFit(positions: List<LatLong>) {
        _state.update { it.copy(cameraRequest = RouteCameraRequest.Fit(positions, ++cameraRequestId)) }
    }

    private fun requestCenter(position: LatLong) {
        _state.update { it.copy(cameraRequest = RouteCameraRequest.Center(position, ++cameraRequestId)) }
    }

    private fun requestLocationPermission() {
        if (_state.value.locationPermissionGranted) return
        viewModelScope.launch {
            val granted = locationService.requestLocationPermission() == PermissionState.GRANTED
            _state.update { it.copy(locationPermissionGranted = granted) }
        }
    }

    private fun openMapEntry(mapEntryId: String) {
        viewModelScope.launch {
            navigator.navigate(
                Route.Schneaggmap(initialEntryId = mapEntryId, openRequestId = Clock.System.now().toEpochMilliseconds())
            )
        }
    }

    // A friend on the route map: their card on the Schneaggmap
    private fun openFriend(userId: String) {
        viewModelScope.launch {
            navigator.navigate(
                Route.Schneaggmap(focusUserId = userId, openRequestId = Clock.System.now().toEpochMilliseconds())
            )
        }
    }

    private fun openNavigation() {
        // One link has one travel mode: the one most legs use
        val mode = mostUsedArrivalMode(route.map { it.point }, routeDefaultMode)
        val link = googleMapsRouteLink(route.map { it.point.coordinates }, mode) ?: return
        viewModelScope.launch {
            _events.send(RouteEvent.OpenUrl(link.url))
            if (link.isCut) SnackbarManager.showMessage(getString(Res.string.route_navigation_cut, MAX_NAVIGATION_POINTS))
        }
    }

    // ─── Add / edit sheet ─────────────────────────────────────────────────────

    private fun openNewDraft(coordinates: LatLong, name: String, mapEntryId: String?) {
        // Continues the mode of the point before it, so a new point keeps the last pick (user, 2026-11-25)
        val position = insertPosition(route.map { it.point }, coordinates, plannedAt = null)
        val transportMode = route.getOrNull(position - 1)?.point?.arrivalMode(routeDefaultMode) ?: routeDefaultMode
        val draft = RoutePointDraft(
            coordinates = coordinates,
            name = name,
            mapEntryId = mapEntryId,
            transportMode = transportMode,
        ).withInsertHint()
        _state.update {
            it.copy(pointDraft = draft, selectedPlace = null, selectedPlaceInsertHint = null, selectedPointId = null)
        }
    }

    private fun openEditDraft(itemId: String) {
        val planned = route.find { it.itemId == itemId } ?: return
        val point = planned.point
        val canDelete = _state.value.points.find { it.itemId == itemId }?.canDelete ?: false
        _state.update {
            it.copy(
                pointDraft = RoutePointDraft(
                    itemId = itemId,
                    kind = point.kind,
                    name = point.name,
                    description = point.description,
                    coordinates = point.coordinates,
                    plannedAt = point.plannedAt,
                    mapEntryId = point.mapEntryId,
                    transportMode = point.arrivalMode(routeDefaultMode),
                    isFirstPoint = route.indexOf(planned) == 0,
                    canDelete = canDelete,
                )
            )
        }
    }

    private fun updateDraft(change: (RoutePointDraft) -> RoutePointDraft) {
        _state.update { state -> state.copy(pointDraft = state.pointDraft?.let(change)) }
    }

    /** A new point shows where it goes in; an edited one keeps its place unless its time changes. */
    private fun RoutePointDraft.withInsertHint(): RoutePointDraft {
        if (itemId != null) return this
        val position = insertPosition(route.map { it.point }, coordinates, plannedAt)
        return copy(insertHint = insertHintFor(route, position, coordinates), isFirstPoint = position == 0)
    }

    private suspend fun saveDraft() {
        val draft = _state.value.pointDraft ?: return
        if (draft.isSaving) return

        // A drive-through point may stay unnamed, but the server needs a name
        val name = draft.name.trim().ifEmpty {
            if (draft.kind == RoutePointKind.WAYPOINT) getString(Res.string.route_kind_waypoint) else ""
        }
        if (name.isEmpty()) {
            updateDraft { it.copy(showNameError = true) }
            return
        }

        val existing = route.find { it.itemId == draft.itemId }
        val others = route.filter { it.itemId != draft.itemId }
        val position = when {
            existing == null -> insertPosition(others.map { it.point }, draft.coordinates, draft.plannedAt)
            draft.plannedAt != null && draft.plannedAt != existing.point.plannedAt ->
                timeOrderPosition(others.map { it.point }, draft.plannedAt)
            else -> route.indexOf(existing)
        }
        val draftKey = draft.itemId ?: NEW_POINT_KEY
        val order = others.map { it.itemId }.toMutableList().apply { add(position, draftKey) }
        val updates = sortIndexUpdates(order, route.associate { it.itemId to it.point.sortIndex })

        val data = RoutePoint(
            coordinates = draft.coordinates,
            name = name,
            description = draft.description.trim(),
            sortIndex = updates[draftKey] ?: existing?.point?.sortIndex ?: 0,
            kind = draft.kind,
            plannedAt = draft.plannedAt,
            mapEntryId = draft.mapEntryId,
            // The first point has no way leading to it
            transportMode = draft.transportMode.name.takeIf { position > 0 },
        )
        val routeSnapshot = route
        updateDraft { it.copy(isSaving = true) }

        applicationScope.launchOnMain {
            val saved = groupExtensionRepository.upsertItem(extensionId, draft.itemId, data)
                .reportErrorTo(errorChannel)
                .isSuccess
            if (saved) {
                writeSortIndexes(updates - draftKey, routeSnapshot)
                _state.update { it.copy(pointDraft = null) }
            } else {
                updateDraft { it.copy(isSaving = false) }
            }
        }
    }

    private fun deleteDraftPoint() {
        val draft = _state.value.pointDraft ?: return
        val planned = route.find { it.itemId == draft.itemId } ?: return
        if (!draft.canDelete) return
        _state.update { it.copy(pointDraft = null, isMovingPoint = false) }

        applicationScope.launchOnMain {
            val deleted = groupExtensionRepository.deleteItem(extensionId, planned.itemId)
                .reportErrorTo(errorChannel)
                .isSuccess
            if (!deleted) return@launchOnMain
            SnackbarManager.showMessage(
                message = getString(Res.string.route_point_deleted, planned.point.name),
                action = SnackbarManager.SnackbarAction(label = getString(Res.string.undo)) {
                    // Same data again, as a new item
                    applicationScope.launchOnMain {
                        groupExtensionRepository.upsertItem(extensionId, null, planned.point).reportErrorTo(errorChannel)
                    }
                },
            )
        }
    }

    // ─── Order ────────────────────────────────────────────────────────────────

    private fun saveOrder(order: List<String>) {
        _state.update { it.copy(isReorderVisible = false) }
        val routeSnapshot = route
        val updates = sortIndexUpdates(order, routeSnapshot.associate { it.itemId to it.point.sortIndex })
        if (updates.isEmpty()) return
        applicationScope.launchOnMain { writeSortIndexes(updates, routeSnapshot) }
    }

    /** One write per changed point; stops at the first error, the rest keeps its old place. */
    private suspend fun writeSortIndexes(updates: Map<String, Int>, routeSnapshot: List<PlannedRoutePoint>) {
        for ((itemId, sortIndex) in updates) {
            val planned = routeSnapshot.find { it.itemId == itemId } ?: continue
            val saved = groupExtensionRepository.upsertItem(extensionId, itemId, planned.point.copy(sortIndex = sortIndex))
                .reportErrorTo(errorChannel)
                .isSuccess
            if (!saved) return
        }
    }
}
