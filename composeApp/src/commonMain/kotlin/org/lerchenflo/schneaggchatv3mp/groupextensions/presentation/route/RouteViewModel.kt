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
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.RoutePath
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionItemData.RoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.ExtensionSettings
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtension
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.GroupExtensionRepository
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.RoutePointKind
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.MAX_NAVIGATION_POINTS
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.PlannedRoute
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.PlannedRoutePath
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.PlannedRoutePoint
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteInsert
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.RouteTransportMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.arrivalMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.departureMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.googleMapsRouteLink
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.insertInto
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.mode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.mostUsedArrivalMode
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.route.plannedRouteOf
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
import org.lerchenflo.schneaggchatv3mp.utilities.device.PermissionState
import org.lerchenflo.schneaggchatv3mp.utilities.geo.distanceMeters
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import org.lerchenflo.schneaggchatv3mp.utilities.ui.SnackbarManager
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

    /** The stored main route and paths; every write is worked out from it. */
    private var route = PlannedRoute()
    private var extension: GroupExtension? = null
    /** Transport of legs whose point has none of its own, set when the route was created. */
    private var routeDefaultMode = RouteTransportMode.DEFAULT

    /** "Drive-through" in the app's language: the name an unnamed drive-through point is saved with. */
    private var unnamedWaypointLabel = ""

    /** Type of the last point added here: a long press starts with it, e.g. many drive-throughs in a row (user, 2026-11-26). */
    private var lastAddedKind = RoutePointKind.STOP

    /** A path just saved here, selected as soon as it arrives, so the next long presses go into it. */
    private var pendingNewPath: PendingNewPath? = null

    private class PendingNewPath(val fromItemId: String, val knownPathIds: Set<String>, val movePointIds: List<String>)

    private var cameraRequestId = 0L
    private var hasFittedRoute = false

    private data class RouteSource(
        val extension: GroupExtension,
        val route: PlannedRoute,
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
            unnamedWaypointLabel = getString(Res.string.route_kind_waypoint)
            groupExtensionRepository.getExtensionFlow(extensionId)
                .flatMapLatest { extension ->
                    if (extension == null) return@flatMapLatest flowOf(null)
                    val route = plannedRouteOf(extension.items)
                    combine(
                        groupRepository.getGroupByIdFlow(extension.ownerId),
                        userIdentityRepository.observeUsers(route.allPoints.map { it.createdBy }.toSet()),
                        mapRepository.getAllMapEntriesFlow(),
                    ) { group, users, entries ->
                        RouteSource(extension, route, group?.members.orEmpty(), users, entries)
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
            canDelete = { createdBy ->
                ownId != null && (createdBy == ownId || source.extension.createdBy == ownId || isAdmin)
            },
            availableMapEntryIds = source.mapEntries.map { it.id }.toSet(),
            unnamedWaypointLabel = unnamedWaypointLabel,
        )

        _state.update { previous ->
            val pointIds = (content.points + content.paths.flatMap { it.points }).map { it.itemId }.toSet()
            val pathIds = content.paths.map { it.itemId }.toSet()
            previous.copy(
                title = source.extension.title,
                isLoading = false,
                extensionMissing = false,
                points = content.points,
                paths = content.paths,
                days = content.days,
                totalMeters = content.totalMeters,
                totalMinutes = content.totalMinutes,
                selectedDayIndex = previous.selectedDayIndex?.takeIf { it < content.days.size && content.days.size > 1 },
                selectedPointId = previous.selectedPointId?.takeIf { it in pointIds },
                selectedPathId = previous.selectedPathId?.takeIf { it in pathIds },
                // The edited point was deleted by someone else: its sheet closes
                pointDraft = previous.pointDraft?.takeIf { draft -> draft.itemId == null || draft.itemId in pointIds },
                pathDraft = previous.pathDraft?.takeIf { draft -> draft.itemId == null || draft.itemId in pathIds },
                legModeEdit = previous.legModeEdit?.takeIf { it.itemId in pointIds || it.itemId in pathIds },
            )
        }

        pendingNewPath?.let { pending ->
            val created = route.paths.firstOrNull { it.itemId !in pending.knownPathIds && it.path.fromItemId == pending.fromItemId }
            if (created != null) {
                pendingNewPath = null
                // A closed loop: its points move in and the main route goes on (no selection)
                if (pending.movePointIds.isEmpty()) selectPath(created.itemId) else movePointsIntoPath(pending.movePointIds, created.itemId)
            }
        }

        if (!hasFittedRoute && route.main.isNotEmpty()) {
            hasFittedRoute = true
            requestFit(route.allPoints.map { it.point.coordinates })
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

            is RouteAction.OnMapLongPress -> onMapLongPress(action.coordinates, action.hitRadiusMeters)
            RouteAction.OnLoopMakeSideTripClick -> openLoopPathDraft()
            RouteAction.OnLoopNewPointClick -> _state.value.loopOffer?.let { offer ->
                _state.update { it.copy(loopOffer = null) }
                openNewDraft(offer.coordinates, name = "", mapEntryId = null, kind = lastAddedKind)
            }
            RouteAction.OnLoopOfferDismiss -> _state.update { it.copy(loopOffer = null) }
            RouteAction.OnMapClick -> clearSelection()
            is RouteAction.OnPointSelect -> selectPoint(action.itemId)
            is RouteAction.OnPointEditClick -> openEditDraft(action.itemId)
            is RouteAction.OnLegModeClick -> openLegMode(action.itemId)
            is RouteAction.OnLegModeSelect -> saveLegMode(action.mode)
            RouteAction.OnLegModeDismiss -> _state.update { it.copy(legModeEdit = null) }

            is RouteAction.OnPathSelect -> selectPath(action.pathItemId)
            RouteAction.OnPathClose -> _state.update { it.copy(selectedPathId = null, selectedPointId = null) }
            is RouteAction.OnPathEditClick -> openEditPathDraft(action.pathItemId)
            is RouteAction.OnPathDraftNameChange -> updatePathDraft {
                it.copy(name = action.name.take(ROUTE_POINT_NAME_MAX_LENGTH), showNameError = false)
            }
            is RouteAction.OnPathDraftDescriptionChange -> updatePathDraft {
                it.copy(description = action.description.take(ROUTE_POINT_DESCRIPTION_MAX_LENGTH))
            }
            is RouteAction.OnPathDraftModeChange -> updatePathDraft { it.copy(mode = action.mode) }
            is RouteAction.OnPathDraftFromChange -> updatePathDraft { it.withStart(action.pointItemId) }
            is RouteAction.OnPathDraftToChange -> updatePathDraft { it.copy(toItemId = action.pointItemId) }
            RouteAction.OnPathDraftSave -> savePathDraft()
            RouteAction.OnPathDraftDelete -> _state.value.pathDraft?.let { draft ->
                val itemId = draft.itemId ?: return
                if (draft.canDelete) _state.update { it.copy(deleteConfirm = RouteDeleteConfirm.Path(itemId, draft.name)) }
            }
            RouteAction.OnPathDraftDismiss -> _state.update { it.copy(pathDraft = null) }
            RouteAction.OnDeleteConfirm -> confirmDelete()
            RouteAction.OnDeleteDismiss -> _state.update { it.copy(deleteConfirm = null) }
            is RouteAction.OnOpenMapEntryClick -> openMapEntry(action.mapEntryId)

            is RouteAction.OnPlaceClick -> selectPlace(action.placeId)
            RouteAction.OnPlaceDismiss -> _state.update { it.copy(selectedPlace = null, selectedPlaceInsertHint = null) }
            RouteAction.OnPlaceAddClick -> {
                val place = _state.value.selectedPlace ?: return
                openNewDraft(
                    place.coordinates,
                    name = place.name.take(ROUTE_POINT_NAME_MAX_LENGTH),
                    mapEntryId = place.id,
                    kind = RoutePointKind.STOP,
                )
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
            is RouteAction.OnDraftContinueModeChange -> updateDraft { it.copy(continueTransportMode = action.mode) }
            is RouteAction.OnDraftPathChange -> updateDraft { it.copy(pathId = action.pathItemId).withInsertHint() }
            RouteAction.OnDraftStartPathClick -> openNewPathDraft()
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
        _state.update { it.copy(selectedDayIndex = dayIndex, selectedPointId = null, selectedPathId = null) }
        val shown = _state.value.shownPoints.map { it.coordinates }
        if (shown.isNotEmpty()) requestFit(shown)
    }

    private fun selectPoint(itemId: String) {
        val point = route.point(itemId) ?: return
        _state.update {
            it.copy(
                selectedPointId = itemId,
                // A path point shows the cards of its path
                selectedPathId = route.path(point.point.pathId)?.itemId,
                isPointRowVisible = true,
                selectedPlace = null,
                selectedPlaceInsertHint = null,
            )
        }
        requestCenter(point.point.coordinates)
    }

    private fun selectPath(pathItemId: String) {
        val path = _state.value.paths.find { it.itemId == pathItemId } ?: return
        _state.update {
            it.copy(
                // Also from the list: the path is shown on the map
                selectedTab = RouteTab.MAP,
                selectedPathId = pathItemId,
                selectedPointId = null,
                isPointRowVisible = true,
                selectedPlace = null,
                selectedPlaceInsertHint = null,
            )
        }
        if (path.line.isNotEmpty()) requestFit(path.line)
    }

    // A tap on the empty map: nothing focused, no cards, so the camera stays where the user moved it
    private fun clearSelection() {
        _state.update {
            it.copy(
                selectedPointId = null,
                selectedPathId = null,
                isPointRowVisible = false,
                selectedPlace = null,
                selectedPlaceInsertHint = null,
            )
        }
    }

    private fun selectPlace(placeId: String) {
        val entry = _state.value.layers.entries.find { it.id == placeId } ?: return
        val place = RouteMapPlaceUi(id = entry.id, name = entry.name, coordinates = entry.coordinates)
        val insert = insertInto(route, newPointPathId(), plannedAt = null)
        _state.update {
            it.copy(
                selectedPlace = place,
                selectedPlaceInsertHint = insertHint(insert, place.coordinates),
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
        val mode = mostUsedArrivalMode(route.main.map { it.point }, routeDefaultMode)
        val link = googleMapsRouteLink(route.main.map { it.point.coordinates }, mode) ?: return
        viewModelScope.launch {
            _events.send(RouteEvent.OpenUrl(link.url))
            if (link.isCut) SnackbarManager.showMessage(getString(Res.string.route_navigation_cut, MAX_NAVIGATION_POINTS))
        }
    }

    // ─── Add / edit sheet ─────────────────────────────────────────────────────

    private fun openNewDraft(coordinates: LatLong, name: String, mapEntryId: String?, kind: RoutePointKind) {
        val pathId = newPointPathId()
        val transportMode = defaultArrivalMode(insertInto(route, pathId, plannedAt = null))
        val draft = RoutePointDraft(
            coordinates = coordinates,
            kind = kind,
            name = name,
            mapEntryId = mapEntryId,
            transportMode = transportMode,
            continueTransportMode = transportMode,
            pathId = pathId,
        ).withInsertHint()
        _state.update {
            it.copy(pointDraft = draft, selectedPlace = null, selectedPlaceInsertHint = null, selectedPointId = null)
        }
    }

    private fun openEditDraft(itemId: String) {
        val planned = route.point(itemId) ?: return
        val point = planned.point
        val path = route.path(point.pathId)
        val part = route.pointsOf(path?.itemId)
        val partMode = path?.path?.mode(routeDefaultMode) ?: routeDefaultMode
        val index = part.indexOf(planned)
        // "Continue by" shows how the way after it is travelled
        val modeAfter = part.getOrNull(index + 1)?.point?.arrivalMode(partMode) ?: path?.path?.mode(routeDefaultMode)
        val canDelete = (_state.value.points + _state.value.paths.flatMap { it.points })
            .find { it.itemId == itemId }?.canDelete ?: false
        _state.update {
            it.copy(
                pointDraft = RoutePointDraft(
                    itemId = itemId,
                    kind = point.kind,
                    // An unnamed drive-through point opens with an empty name field
                    name = point.name?.takeIf { point.hasOwnName(unnamedWaypointLabel) }.orEmpty(),
                    description = point.description,
                    coordinates = point.coordinates,
                    plannedAt = point.plannedAt,
                    mapEntryId = point.mapEntryId,
                    transportMode = point.arrivalMode(partMode),
                    continueTransportMode = modeAfter ?: point.departureMode(partMode),
                    pathId = path?.itemId,
                    isFirstPoint = path == null && index == 0,
                    canDelete = canDelete,
                )
            )
        }
    }

    private fun updateDraft(change: (RoutePointDraft) -> RoutePointDraft) {
        _state.update { state -> state.copy(pointDraft = state.pointDraft?.let(change)) }
    }

    /**
     * The part a new point goes into: the shown path (its header is on the map), else the main
     * route; the sheet's "Gehört zu" can change it (user, 2026-11-26).
     */
    private fun newPointPathId(): String? =
        _state.value.selectedPathId?.takeIf { id -> route.path(id)?.let(route::lineOf) != null }

    /** A new point shows where it goes in; an edited one keeps its place unless its time or part changes. */
    private fun RoutePointDraft.withInsertHint(): RoutePointDraft {
        if (itemId != null) return this
        val insert = insertInto(route, pathId, plannedAt)
        return copy(
            insertHint = insertHint(insert, coordinates),
            isFirstPoint = insert.pathItemId == null && insert.position == 0,
        )
    }

    /** Names the points [coordinates] goes between at [insert]. */
    private fun insertHint(insert: RouteInsert, coordinates: LatLong): RouteInsertHint {
        val path = route.path(insert.pathItemId)
        return if (path == null) {
            insertHintFor(route.main.map { it.point }, insert.position, coordinates, unnamedWaypointLabel)
        } else {
            // A path's line starts with the point it leaves from
            insertHintFor(route.lineOf(path).orEmpty(), insert.position + 1, coordinates, unnamedWaypointLabel)
        }
    }

    /**
     * Continues the way of the point before it (a TRANSFER's "continue by"), so the last pick goes on
     * (user, 2026-11-25); the first point of a path takes the path's mode.
     */
    private fun defaultArrivalMode(insert: RouteInsert): RouteTransportMode {
        val path = route.path(insert.pathItemId)
        val partMode = path?.path?.mode(routeDefaultMode) ?: routeDefaultMode
        val before = route.pointsOf(path?.itemId).getOrNull(insert.position - 1)
        return before?.point?.departureMode(partMode) ?: partMode
    }

    private suspend fun saveDraft() {
        val draft = _state.value.pointDraft ?: return
        if (draft.isSaving) return

        // Only a drive-through point may stay unnamed (stored as null)
        val name = draft.name.trim().ifEmpty { null }
        if (name == null && draft.kind != RoutePointKind.WAYPOINT) {
            updateDraft { it.copy(showNameError = true) }
            return
        }

        val existing = route.point(draft.itemId)
        val part = route.pointsOf(draft.pathId)
        val others = part.filter { it.itemId != draft.itemId }
        val changedPart = existing != null && route.path(existing.point.pathId)?.itemId != draft.pathId
        val position = when {
            existing == null || changedPart ->
                insertInto(route, draft.pathId, draft.plannedAt, withoutItemId = draft.itemId).position
            draft.plannedAt != null && draft.plannedAt != existing.point.plannedAt ->
                timeOrderPosition(others.map { it.point }, draft.plannedAt)
            else -> part.indexOf(existing)
        }
        val draftKey = draft.itemId ?: NEW_POINT_KEY
        val order = others.map { it.itemId }.toMutableList().apply { add(position, draftKey) }
        val updates = sortIndexUpdates(order, part.associate { it.itemId to it.point.sortIndex })

        val data = RoutePoint(
            coordinates = draft.coordinates,
            name = name,
            description = draft.description.trim(),
            sortIndex = updates[draftKey] ?: existing?.point?.sortIndex ?: 0,
            kind = draft.kind,
            plannedAt = draft.plannedAt,
            mapEntryId = draft.mapEntryId,
            // The first point of the main route has no way leading to it
            transportMode = draft.transportMode.name.takeIf { draft.pathId != null || position > 0 },
            continueTransportMode = draft.continueTransportMode.name.takeIf { draft.kind == RoutePointKind.TRANSFER },
            pathId = draft.pathId,
        )
        val routeSnapshot = route
        val nextItemId = order.getOrNull(position + 1)
        updateDraft { it.copy(isSaving = true) }

        applicationScope.launchOnMain {
            if (!upsert(draft.itemId, data)) {
                updateDraft { it.copy(isSaving = false) }
                return@launchOnMain
            }
            if (draft.itemId == null) lastAddedKind = draft.kind
            writeSortIndexes(updates - draftKey, routeSnapshot.allPoints)
            _state.update { it.copy(pointDraft = null) }
            if (draft.kind == RoutePointKind.TRANSFER) {
                continueAfterTransfer(routeSnapshot, draft.pathId, nextItemId, draft.continueTransportMode)
            }
        }
    }

    /** A TRANSFER's "continue by" is the mode of the leg after it: the next point's, or a path's way back. */
    private suspend fun continueAfterTransfer(
        routeSnapshot: PlannedRoute,
        pathItemId: String?,
        nextItemId: String?,
        mode: RouteTransportMode,
    ) {
        val path = routeSnapshot.path(pathItemId)
        val next = routeSnapshot.point(nextItemId)
        val partMode = path?.path?.mode(routeDefaultMode) ?: routeDefaultMode
        when {
            next != null -> if (next.point.arrivalMode(partMode) != mode) {
                upsert(next.itemId, next.point.copy(transportMode = mode.name))
            }
            path != null -> if (partMode != mode) {
                upsert(path.itemId, path.path.copy(transportMode = mode.name))
            }
        }
    }

    // ─── Leg modes ────────────────────────────────────────────────────────────

    private fun openLegMode(itemId: String) {
        val state = _state.value
        val edit = state.paths.find { it.itemId == itemId }?.let { RouteLegModeEdit(itemId, it.name, it.mode) }
            ?: (state.points + state.paths.flatMap { it.points }).find { it.itemId == itemId }?.let { point ->
                point.arrivalMode?.let { mode -> RouteLegModeEdit(itemId, point.name, mode) }
            }
            ?: return
        _state.update { it.copy(legModeEdit = edit) }
    }

    /** The mode of the tapped leg; every member may change it. */
    private fun saveLegMode(mode: RouteTransportMode) {
        val edit = _state.value.legModeEdit ?: return
        _state.update { it.copy(legModeEdit = null) }
        if (edit.mode == mode) return
        val routeSnapshot = route
        applicationScope.launchOnMain { writeLegMode(routeSnapshot, edit.itemId, mode) }
    }

    /**
     * Sets how the leg to point [itemId] (or a path's way back) is travelled, and keeps the
     * "continue by" of a TRANSFER at the leg's start the same.
     */
    private suspend fun writeLegMode(routeSnapshot: PlannedRoute, itemId: String, mode: RouteTransportMode) {
        val path = routeSnapshot.path(itemId)
        val saved = if (path != null) {
            upsert(path.itemId, path.path.copy(transportMode = mode.name))
        } else {
            val point = routeSnapshot.point(itemId) ?: return
            upsert(point.itemId, point.point.copy(transportMode = mode.name))
        }
        if (!saved) return
        val start = legStart(routeSnapshot, itemId) ?: return
        if (start.point.kind == RoutePointKind.TRANSFER && start.point.continueTransportMode != mode.name) {
            upsert(start.itemId, start.point.copy(continueTransportMode = mode.name))
        }
    }

    /** The point the leg to [itemId] starts at, within the same part (none for a path's first leg). */
    private fun legStart(routeSnapshot: PlannedRoute, itemId: String): PlannedRoutePoint? {
        routeSnapshot.path(itemId)?.let { return it.points.lastOrNull() }
        val point = routeSnapshot.point(itemId) ?: return null
        val part = routeSnapshot.pointsOf(routeSnapshot.path(point.point.pathId)?.itemId)
        return part.getOrNull(part.indexOf(point) - 1)
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    private fun deleteDraftPoint() {
        val draft = _state.value.pointDraft ?: return
        val planned = route.point(draft.itemId) ?: return
        if (!draft.canDelete) return
        // Paths leaving or rejoining here would have nowhere to go: ask to delete them too (user, 2026-11-26)
        val paths = route.pathsAt(planned.itemId)
        if (paths.isNotEmpty()) {
            _state.update {
                it.copy(deleteConfirm = RouteDeleteConfirm.PointWithPaths(planned.itemId, paths.map { path -> path.path.name }))
            }
            return
        }
        _state.update { it.copy(pointDraft = null, isMovingPoint = false) }

        applicationScope.launchOnMain {
            if (!delete(planned.itemId)) return@launchOnMain
            SnackbarManager.showMessage(
                message = getString(Res.string.route_point_deleted, planned.point.displayName(unnamedWaypointLabel)),
                action = SnackbarManager.SnackbarAction(label = getString(Res.string.undo)) {
                    // Same data again, as a new item
                    applicationScope.launchOnMain { upsert(null, planned.point) }
                },
            )
        }
    }

    private fun confirmDelete() {
        val confirm = _state.value.deleteConfirm ?: return
        _state.update { it.copy(deleteConfirm = null, pointDraft = null, pathDraft = null, isMovingPoint = false) }
        val routeSnapshot = route
        applicationScope.launchOnMain {
            when (confirm) {
                is RouteDeleteConfirm.PointWithPaths -> {
                    val planned = routeSnapshot.point(confirm.pointItemId) ?: return@launchOnMain
                    for (path in routeSnapshot.pathsAt(planned.itemId)) {
                        if (!deletePath(path)) return@launchOnMain
                    }
                    if (!delete(planned.itemId)) return@launchOnMain
                    SnackbarManager.showMessage(
                        getString(Res.string.route_point_deleted, planned.point.displayName(unnamedWaypointLabel))
                    )
                }
                is RouteDeleteConfirm.Path -> {
                    val path = routeSnapshot.path(confirm.pathItemId) ?: return@launchOnMain
                    if (deletePath(path)) SnackbarManager.showMessage(getString(Res.string.route_point_deleted, path.path.name))
                }
            }
        }
    }

    /**
     * The paths branching off [path] first (innermost first), then its points one by one, then the
     * path (the server deletes nothing along); stops at the first error.
     */
    private suspend fun deletePath(path: PlannedRoutePath): Boolean {
        val paths = route.nestedPathsOf(path).reversed() + path
        for (each in paths) {
            for (point in each.points) {
                if (!delete(point.itemId)) return false
            }
            if (!delete(each.itemId)) return false
        }
        return true
    }

    // ─── Long press and loops ─────────────────────────────────────────────────

    /**
     * A long press adds a point at the end of the shown part (user, 2026-11-26). On an earlier point
     * of that part it offers to close a loop there; on the start or rejoin point of the shown path
     * it finishes that path, so the next points go to the part it came from.
     */
    private fun onMapLongPress(coordinates: LatLong, hitRadiusMeters: Double) {
        if (_state.value.isMovingPoint) return
        fun isHit(point: PlannedRoutePoint) = distanceMeters(point.point.coordinates, coordinates) <= hitRadiusMeters

        val shownPath = route.path(newPointPathId())
        val pathEnds = shownPath?.let { listOfNotNull(route.point(it.path.fromItemId), route.point(it.path.toItemId)) }.orEmpty()
        if (pathEnds.any(::isHit)) {
            _state.update { it.copy(selectedPathId = null, selectedPointId = null) }
            return
        }
        val loopStart = route.pointsOf(shownPath?.itemId).dropLast(1)
            .filter(::isHit)
            .minByOrNull { distanceMeters(it.point.coordinates, coordinates) }
        if (loopStart != null) {
            _state.update {
                it.copy(loopOffer = RouteLoopOffer(loopStart.itemId, loopStart.point.displayName(unnamedWaypointLabel), coordinates))
            }
            return
        }
        openNewDraft(coordinates, name = "", mapEntryId = null, kind = lastAddedKind)
    }

    /**
     * Back at an earlier point: the points after it become a side trip from it back to it, the main
     * route (or the path it lies on) goes on from there (user, 2026-11-26). They move in once the
     * new path is saved and arrives.
     */
    private fun openLoopPathDraft() {
        val offer = _state.value.loopOffer ?: return
        _state.update { it.copy(loopOffer = null) }
        val start = route.point(offer.pointItemId) ?: return
        val part = route.partPointsOf(start.itemId)
        val after = part.drop(part.indexOf(start) + 1)
        if (after.isEmpty()) return
        val partMode = route.path(start.point.pathId)?.path?.mode(routeDefaultMode) ?: routeDefaultMode
        _state.update {
            it.copy(
                selectedPointId = null,
                pathDraft = RoutePathDraft(
                    mode = after.first().point.arrivalMode(partMode),
                    fromItemId = start.itemId,
                    toItemId = start.itemId,
                    movePointIds = after.map { point -> point.itemId },
                ),
            )
        }
    }

    /** Puts the points into the path one by one, keeping their order (sortIndex); stops at the first error. */
    private fun movePointsIntoPath(pointItemIds: List<String>, pathItemId: String) {
        val routeSnapshot = route
        applicationScope.launchOnMain {
            for (itemId in pointItemIds) {
                val planned = routeSnapshot.point(itemId) ?: continue
                if (!upsert(itemId, planned.point.copy(pathId = pathItemId))) return@launchOnMain
            }
        }
    }

    // ─── Alternative paths ────────────────────────────────────────────────────

    /**
     * "Alternative from here" in the sheet of a saved point: it rejoins at the next point of the same
     * part (main route or the path the point lies on, user 2026-11-26).
     */
    private fun openNewPathDraft() {
        val fromId = _state.value.pointDraft?.itemId ?: return
        val part = route.partPointsOf(fromId)
        val index = part.indexOfFirst { it.itemId == fromId }
        if (index < 0) return
        val from = part[index]
        val partMode = route.path(from.point.pathId)?.path?.mode(routeDefaultMode) ?: routeDefaultMode
        _state.update {
            it.copy(
                pointDraft = null,
                pathDraft = RoutePathDraft(
                    mode = from.point.departureMode(partMode),
                    fromItemId = fromId,
                    toItemId = part.getOrNull(index + 1)?.itemId ?: fromId,
                ),
            )
        }
    }

    private fun openEditPathDraft(pathItemId: String) {
        val path = route.path(pathItemId) ?: return
        val canDelete = _state.value.paths.find { it.itemId == pathItemId }?.canDelete ?: false
        _state.update {
            it.copy(
                pathDraft = RoutePathDraft(
                    itemId = pathItemId,
                    name = path.path.name,
                    description = path.path.description,
                    mode = path.path.mode(routeDefaultMode),
                    fromItemId = path.path.fromItemId,
                    toItemId = path.path.toItemId,
                    canDelete = canDelete,
                )
            )
        }
    }

    private fun updatePathDraft(change: (RoutePathDraft) -> RoutePathDraft) {
        _state.update { state -> state.copy(pathDraft = state.pathDraft?.let(change)) }
    }

    /** A new start in the same part; a rejoin point that now lies before it moves to the point after the start. */
    private fun RoutePathDraft.withStart(fromItemId: String): RoutePathDraft {
        val part = route.partPointsOf(fromItemId)
        val fromIndex = part.indexOfFirst { it.itemId == fromItemId }
        val toIndex = part.indexOfFirst { it.itemId == toItemId }
        val toStillAfter = toIndex > fromIndex || toItemId == this.fromItemId
        return copy(
            fromItemId = fromItemId,
            toItemId = when {
                // A side trip stays one
                toItemId == this.fromItemId -> fromItemId
                toStillAfter -> toItemId
                else -> part.getOrNull(fromIndex + 1)?.itemId ?: fromItemId
            },
        )
    }

    private fun savePathDraft() {
        val draft = _state.value.pathDraft ?: return
        if (draft.isSaving) return
        val name = draft.name.trim()
        if (name.isEmpty()) {
            updatePathDraft { it.copy(showNameError = true) }
            return
        }
        val data = RoutePath(
            name = name,
            description = draft.description.trim(),
            transportMode = draft.mode.name,
            fromItemId = draft.fromItemId,
            toItemId = draft.toItemId,
            memberIds = route.path(draft.itemId)?.path?.memberIds.orEmpty(),
        )
        updatePathDraft { it.copy(isSaving = true) }
        // Set before the write: the new path may arrive before the write returns
        if (draft.itemId == null) {
            pendingNewPath = PendingNewPath(draft.fromItemId, route.paths.map { it.itemId }.toSet(), draft.movePointIds)
        }
        applicationScope.launchOnMain {
            if (upsert(draft.itemId, data)) {
                _state.update { it.copy(pathDraft = null) }
            } else {
                pendingNewPath = null
                updatePathDraft { it.copy(isSaving = false) }
            }
        }
    }

    // ─── Writes ───────────────────────────────────────────────────────────────

    private suspend fun upsert(itemId: String?, data: ExtensionItemData): Boolean =
        groupExtensionRepository.upsertItem(extensionId, itemId, data).reportErrorTo(errorChannel).isSuccess

    private suspend fun delete(itemId: String): Boolean =
        groupExtensionRepository.deleteItem(extensionId, itemId).reportErrorTo(errorChannel).isSuccess

    // ─── Order ────────────────────────────────────────────────────────────────

    // The reorder sheet shows the main route only
    private fun saveOrder(order: List<String>) {
        _state.update { it.copy(isReorderVisible = false) }
        val routeSnapshot = route.main
        val updates = sortIndexUpdates(order, routeSnapshot.associate { it.itemId to it.point.sortIndex })
        if (updates.isEmpty()) return
        applicationScope.launchOnMain { writeSortIndexes(updates, routeSnapshot) }
    }

    /** One write per changed point; stops at the first error, the rest keeps its old place. */
    private suspend fun writeSortIndexes(updates: Map<String, Int>, pointsSnapshot: List<PlannedRoutePoint>) {
        for ((itemId, sortIndex) in updates) {
            val planned = pointsSnapshot.find { it.itemId == itemId } ?: continue
            if (!upsert(itemId, planned.point.copy(sortIndex = sortIndex))) return
        }
    }
}
