package org.lerchenflo.schneaggchatv3mp.car

import android.app.Presentation
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapAction
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.SchneaggmapState
import org.lerchenflo.schneaggchatv3mp.schneaggmap.presentation.uielements.SchneaggmapLayers
import org.lerchenflo.schneaggchatv3mp.utilities.location.DeviceLocation
import org.lerchenflo.schneaggchatv3mp.utilities.location.LocationService
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.location.LocationAccuracy
import org.maplibre.compose.location.LocationRequest
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Position
import kotlin.math.ln

private const val DEFAULT_ZOOM = 7.0
private const val RECENTER_ZOOM = 14.0
private const val MIN_ZOOM = 2.0
private const val MAX_ZOOM = 20.0

/**
 * Draws [SchneaggmapLayers] onto the car's display surface: [android.hardware.display.DisplayManager]
 * creates a [VirtualDisplay] pointed at the host's [android.view.Surface], a [Presentation] hosts a
 * [ComposeView] on that display, and the existing map-layer composable renders inside it unchanged.
 * This is the documented path for putting Compose UI on a car app's map surface.
 *
 * Owns the [MapState] itself (outside Compose - created directly through [DefaultMapRuntime]
 * instead of `rememberMapState`, which requires a composition) so the map action strip buttons and
 * the gesture callbacks below can drive the camera without reaching into the composition.
 */
class CarMapSurfaceRenderer(
    private val carContext: CarContext,
    private val displayState: StateFlow<SchneaggmapState>,
    private val onAction: (SchneaggmapAction) -> Unit,
    private val locationService: LocationService,
    private val coroutineScope: CoroutineScope,
) : SurfaceCallback {

    //`mapState` is captured by `content` before it is assigned - safe because `content` isn't
    //invoked until the map actually composes it, by which point the assignment below has run (see
    //the same lateinit pattern in SchneaggmapScreen).
    lateinit var mapState: MapState
        private set

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var composeOwners: CarComposeOwners? = null
    private var surfaceCenter: Offset? = null
    private var surfaceDensity: Float = 1f

    //Independent of the maplibre "blue dot" location subscription inside CarMapContent below -
    //same split the phone side already has between the map's own puck and GlobalViewModel's/
    //FriendCompassViewModel's LocationService-based tracking. Used only for the recenter button.
    private val lastKnownLocation = MutableStateFlow<DeviceLocation?>(null)

    init {
        mapState = DefaultMapRuntime.instance.createMapState(
            baseStyle = BaseStyle.Uri(displayState.value.mapStyleUrl),
            cameraPosition = CameraPosition(target = Position(9.92, 47.32), zoom = DEFAULT_ZOOM),
        ) {
            CarMapContent(displayState = displayState, mapState = mapState, onAction = onAction)
        }

        locationService.getLocationFlow(fastUpdates = false)
            .onEach { lastKnownLocation.value = it }
            .launchIn(coroutineScope)

        //Created directly through DefaultMapRuntime rather than rememberMapState, so the base
        //style isn't auto-declared - the car and phone can independently pick dark/light styles,
        //so this has to be pushed in manually whenever the resolved style URL actually changes.
        displayState.map { it.mapStyleUrl }.distinctUntilChanged()
            .onEach { url -> mapState.style.asMutable?.baseStyle = BaseStyle.Uri(url) }
            .launchIn(coroutineScope)

        //rememberMapState would close this for us on leaving composition; created directly through
        //DefaultMapRuntime instead (see the class doc), so it's closed explicitly when the
        //surrounding screen (and its scope) is destroyed.
        coroutineScope.coroutineContext[Job]?.invokeOnCompletion { mapState.close() }
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        surfaceCenter = Offset(surfaceContainer.width / 2f, surfaceContainer.height / 2f)
        surfaceDensity = surfaceContainer.dpi / 160f

        val displayManager = carContext.getSystemService(DisplayManager::class.java)
        val display = displayManager.createVirtualDisplay(
            "SchneaggmapCarDisplay",
            surfaceContainer.width,
            surfaceContainer.height,
            surfaceContainer.dpi,
            surfaceContainer.surface,
            0,
        )
        virtualDisplay = display

        val owners = CarComposeOwners()
        composeOwners = owners

        val newPresentation = object : Presentation(carContext, display.display) {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                val view = ComposeView(context).apply {
                    setViewTreeLifecycleOwner(owners)
                    setViewTreeViewModelStoreOwner(owners)
                    setViewTreeSavedStateRegistryOwner(owners)
                    setContent {
                        CarMapSurface(mapState = mapState)
                    }
                }
                setContentView(view)
            }
        }
        newPresentation.show()
        presentation = newPresentation
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        presentation?.dismiss()
        presentation = null
        composeOwners?.destroy()
        composeOwners = null
        virtualDisplay?.release()
        virtualDisplay = null
        surfaceCenter = null
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        val center = surfaceCenter ?: return
        val density = Density(surfaceDensity)
        val from = with(density) { mapState.positionFromScreenLocation(DpOffset(center.x.toDp(), center.y.toDp())) } ?: return
        val to = with(density) {
            mapState.positionFromScreenLocation(DpOffset((center.x + distanceX).toDp(), (center.y + distanceY).toDp()))
        } ?: return
        val current = mapState.cameraPosition
        mapState.setCameraPosition(
            current.copy(
                target = Position(
                    longitude = current.target.longitude + (to.longitude - from.longitude),
                    latitude = current.target.latitude + (to.latitude - from.latitude),
                )
            )
        )
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        zoomBy(ln(scaleFactor.toDouble()) / ln(2.0))
    }

    override fun onClick(x: Float, y: Float) {
        coroutineScope.launch {
            val dpOffset = with(Density(surfaceDensity)) { DpOffset(x.toDp(), y.toDp()) }
            val features = mapState.queryRenderedFeatures(dpOffset)
            val clickedId = features.firstOrNull()?.id?.content ?: return@launch

            val state = displayState.value
            when {
                state.entries.any { it.id == clickedId } -> onAction(SchneaggmapAction.OnEntryClick(clickedId))
                state.usersWithLocation.any { it.id == clickedId } -> onAction(SchneaggmapAction.OnUserClick(clickedId))
            }
        }
    }

    fun zoomBy(delta: Double) {
        val current = mapState.cameraPosition
        mapState.setCameraPosition(current.copy(zoom = (current.zoom + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)))
    }

    fun recenter() {
        val location = lastKnownLocation.value ?: return
        mapState.setCameraPosition(
            mapState.cameraPosition.copy(
                target = Position(longitude = location.coordinates.long, latitude = location.coordinates.lat),
                zoom = RECENTER_ZOOM,
            )
        )
    }
}

@Composable
private fun CarMapSurface(mapState: MapState) {
    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        state = mapState,
    )
}

@Composable
@MaplibreComposable
private fun CarMapContent(
    displayState: StateFlow<SchneaggmapState>,
    mapState: MapState,
    onAction: (SchneaggmapAction) -> Unit,
) {
    val state by displayState.collectAsState()
    val locationProvider = rememberDefaultLocationProvider()
    val locationState = rememberLocationState(
        enabled = true,
        provider = locationProvider,
        request = LocationRequest(accuracy = LocationAccuracy.BestForNavigation),
    )

    SchneaggmapLayers(
        state = state,
        mapState = mapState,
        ownLocation = locationState.lastLocation,
        onAction = onAction,
    )
}
