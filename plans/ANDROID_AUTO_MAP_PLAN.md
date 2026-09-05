# Android Auto Map Plan

Show the Schneaggmap (MapLibre / OSM data) on the car display via Android Auto, with a small set of buttons (zoom, recenter, toggle friends/events) and a friend list. View-only: no creating/editing map entries, no search, no dialogs on the car screen.

## Current state (analysis)

Analysis date: 2026-09-05. Nothing car-related exists in the repo yet (no `androidx.car.app` in code, catalog, or manifest).

### What already helps

- **Map library**: `org.maplibre.compose:maplibre-compose` 0.14.0 (catalog key `map`). On Android it transitively pulls MapLibre Native `org.maplibre.gl:android-sdk` 13.0.2 and renders through a real `MapView` (`AndroidView`). The `MaplibreMap` composable and its `SymbolLayer` / `LineLayer` / `CircleLayer` layer code are reusable as-is inside any Compose host.
- **Tiles**: `MapStyleSetting` (`datasource/preferences`) points at OpenFreeMap vector styles (`https://tiles.openfreemap.org/styles/{liberty,bright,positron,dark,fiord}`), plain HTTPS. The phone fetches tiles, the car only displays the projected surface. No extra networking needed.
- **DI**: Koin is started in `MainApp.onCreate` (`startKoinAndroid`), not in the Activity. A `CarAppService` can therefore inject `MapRepository`, `UserRepository`, `EventRepository`, `LocationService`, `Preferencemanager` directly without `MainActivity` being alive.
- **Data**: `MapRepository.getAllMapEntriesFlow()`, `UserRepository.onlineFriendIdsFlow`, `EventRepository.getAllEventsFlow()` are Room-backed flows. `User.location: UserLocation?` + `User.isLocationValid()` already exist.
- **Location**: `LocationService.android.kt` uses `FusedLocationProviderClient`; `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` are already in `androidApp/src/main/AndroidManifest.xml`.
- **SDK levels**: `androidApp` minSdk 28, compileSdk 37. Car App Library requires minSdk 23. Fine.
- **Android-specific services precedent**: `WakeAlarmService`, `AppFirebaseMessagingService`, `MarkAsReadReceiver` live in `composeApp/src/androidMain/.../utilities/` and are registered in the `androidApp` manifest. The car code follows the same split.

### What gets in the way

1. **Socket and own-location sharing are gated on app foreground.**
   - `app/GlobalViewModel.kt:229` – own location is only pushed when `SessionCache.isLoggedIn() && AppLifecycleManager.isAppInForeground && ownLocationShared`.
   - `datasource/network/socket/SocketConnectionManager.kt:234` / `:242` – reconnect loop bails out when `!AppLifecycleManager.isAppInForeground`.
   - `AppLifecycleTracker()` (Compose, `app/AppLifecycleManager.kt:108`) is the only thing that flips the flag; it is driven by `MainActivity`'s lifecycle.
   - Consequence: phone in pocket / screen off → app backgrounded → socket disconnects → friend positions on the car stale, own position not sent. The car `Session` lifecycle must also count as "foreground".
2. **`SchneaggmapScreen.kt` is a 1449-line monolith.** The map layers (lines ~984–1449: `MaplibreMap`, GeoJSON sources, symbol/line/circle layers, click handling) are inline inside the screen composable together with the phone chrome (FABs, dropdowns, search bar, info cards, dialogs). None of the chrome is usable on the car. The layer block must be extracted into its own composable before it can be shared.
3. **`SchneaggmapViewModel`** takes a `Navigator` and `pickLocationMode`/`currentlyEditedEvent` params. The car screen must not depend on `Navigator` or the Nav3 backstack. Either reuse the ViewModel with a no-op navigator or build a slim car-side state holder that reuses the same repository flows.
4. **Chat notifications on the car** need `NotificationCompat.MessagingStyle`. `utilities/notifications/Notifier.android.kt` only uses plain `addAction` today. Separate, optional feature (see Out of scope).

## Decisions

| Question | Decision |
|---|---|
| Car app category | `androidx.car.app.category.POI`. Not `NAVIGATION` (would require turn-by-turn navigation and a stricter Play review). |
| Template | `MapWithContentTemplate` (Car App API level 7+). App draws the map on the host `Surface`; host renders action strips and the content pane. `PlaceListMapTemplate` (host-rendered map) is rejected: no MapLibre, no custom layers. |
| How the map reaches the Surface | `VirtualDisplay` on the car `Surface` → `Presentation` → `ComposeView` → existing `MaplibreMap` composable. This is the path Google documents for Compose in car apps. |
| Map code reuse | Extract the layer block of `SchneaggmapScreen` into `SchneaggmapLayers(state, cameraState, onAction)` in `schneaggmap/presentation/uielements/`. Phone screen and car screen both call it. Phone behaviour unchanged. |
| Input handling | `SurfaceCallback.onScroll/onScale/onFling/onClick` → maplibre-compose `CameraState` (`position` / `animateTo`). Requires `Action.PAN` in the map action strip, else the host sends no gestures. |
| Car interactions | View-only. No long-press create, no search, no entry editing, no dialogs. Tap on a marker = select entry/user and show its name in the content pane. |
| Style on the car | Follow `carContext.isDarkMode()`: dark → `MapStyleSetting.DARK`, light → user's phone `mapStyle` setting. Re-evaluate in `Session.onCarConfigurationChanged`. |
| Foreground gating | Add a `carSessionActive` flag to `AppLifecycleManager` (set from `Session` `onStart`/`onStop`) and make `isAppInForeground`-style checks in `GlobalViewModel` and `SocketConnectionManager` treat `carSessionActive` as foreground. Do not fake Activity lifecycle events. |
| Where the code lives | Car service, session, screen, surface renderer: `composeApp/src/androidMain/kotlin/org/lerchenflo/schneaggchatv3mp/car/`. Manifest entries + `res/xml/automotive_app_desc.xml`: `androidApp`. Dependency: `androidMain.dependencies` in `composeApp/build.gradle.kts`. |
| Dependency | `androidx.car.app:app` 1.7.0 (stable, 2025-07-16). Add as `androidx-car-app` in `gradle/libs.versions.toml`. Verify for a newer stable before adding. |
| Build type | Debug build uses `applicationIdSuffix = ".debug"`. Fine for Android Auto; the head unit does not care. |

## Design

### 1. Dependency + manifest

`gradle/libs.versions.toml`:

```toml
[versions]
androidx-car-app = "1.7.0"

[libraries]
androidx-car-app = { module = "androidx.car.app:app", version.ref = "androidx-car-app" }
```

`composeApp/build.gradle.kts` → `androidMain.dependencies { implementation(libs.androidx.car.app) }`.

`androidApp/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="androidx.car.app.MAP_TEMPLATES" />

<application ...>
    <meta-data
        android:name="com.google.android.gms.car.application"
        android:resource="@xml/automotive_app_desc" />
    <meta-data
        android:name="androidx.car.app.minCarApiLevel"
        android:value="7" />

    <service
        android:name="org.lerchenflo.schneaggchatv3mp.car.SchneaggmapCarAppService"
        android:exported="true">
        <intent-filter>
            <action android:name="androidx.car.app.CarAppService" />
            <category android:name="androidx.car.app.category.POI" />
        </intent-filter>
    </service>
</application>
```

`androidApp/src/main/res/xml/automotive_app_desc.xml`:

```xml
<automotiveApp>
    <uses name="template" />
</automotiveApp>
```

### 2. Car service + session (`composeApp/src/androidMain/.../car/`)

| File | Responsibility |
|---|---|
| `SchneaggmapCarAppService.kt` | `CarAppService`. `createHostValidator()` = `ALLOW_ALL_HOSTS_VALIDATOR` in debug, `HostValidator.Builder(context).addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)` in release. `onCreateSession()` → `SchneaggmapCarSession()`. |
| `SchneaggmapCarSession.kt` | `Session`. `onCreateScreen()` → `SchneaggmapCarScreen`. Observes own lifecycle: `ON_START` → `AppLifecycleManager.setCarSessionActive(true)`, `ON_STOP` → `false`. `onCarConfigurationChanged` → tells the screen to re-pick the style. |
| `SchneaggmapCarScreen.kt` | `Screen`. Owns `CarMapSurfaceRenderer`, registers it via `carContext.getCarService(AppManager::class.java).setSurfaceCallback(...)`. `onGetTemplate()` builds the `MapWithContentTemplate`. Holds `CarMapUiState` (see §4). |
| `CarMapSurfaceRenderer.kt` | `SurfaceCallback`. On `onSurfaceAvailable`: create `VirtualDisplay` (`DisplayManager.createVirtualDisplay(name, width, height, dpi, surface, 0)`), `Presentation(carContext, display)`, `ComposeView` with `setViewTreeLifecycleOwner(session)`, `setViewTreeSavedStateRegistryOwner(session)`, `setViewTreeViewModelStoreOwner(...)`, `setContent { CarMapContent(...) }`, `presentation.show()`. On `onSurfaceDestroyed`: dismiss presentation, release display. Forwards `onScroll/onScale/onFling/onClick` to the `CameraState`. Applies `onVisibleAreaChanged` / `onStableAreaChanged` as camera padding so markers are not hidden under the host's action strip. |

Gestures → camera mapping:

- `onScroll(dx, dy)` → shift `cameraState.position.target` by the screen delta converted through `cameraState.screenLocationFromPosition` / `positionFromScreenLocation`.
- `onScale(focusX, focusY, factor)` → `zoom += log2(factor)`, keep focus point.
- `onFling` → optional, start with plain scroll.
- `onClick(x, y)` → `cameraState.queryRenderedFeatures(...)` on the marker layers → select entry/user.

### 3. Map layer extraction (commonMain, shared)

New composable in `schneaggmap/presentation/uielements/SchneaggmapLayers.kt`:

```kotlin
@Composable
fun SchneaggmapLayers(
    state: SchneaggmapState,
    cameraState: CameraState,
    styleState: StyleState,
    onAction: (SchneaggmapAction) -> Unit,
    modifier: Modifier = Modifier,
)
```

Moves lines ~984–1449 of `SchneaggmapScreen.kt` (the `MaplibreMap(...)` call with `baseStyle = BaseStyle.Uri(state.mapStyleUrl)`, the GeoJSON sources, all layers, click callbacks, the own-location marker) into it. `SchneaggmapScreen` keeps its chrome and calls `SchneaggmapLayers(...)` where the block used to be. Behaviour on phone/iOS/desktop unchanged. Long-press create flow stays wired through `onAction(OnMapClick(longClick = true))`; the car simply never sends a long-click.

### 4. Car state holder

`CarMapUiState` (androidMain, `car/`): minimal data class built from the same flows `SchneaggmapViewModel` combines, without `Navigator`:

- `mapRepository.getAllMapEntriesFlow()`
- `userRepository` users with `isLocationValid()`, `onlineFriendIdsFlow`
- `eventRepository.getAllEventsFlow()`
- `preferenceManager` map style + `enabledTypes`
- `locationService.getLocationFlow(fastUpdates = false)`

Mapped into a `SchneaggmapState` (so `SchneaggmapLayers` accepts it) with `pickLocationMode = false`, `isFilterDropdownVisible = false`, etc. Decision: do not instantiate `SchneaggmapViewModel` on the car (it navigates on several actions). If duplication of the combine logic turns out large, move that combine into a `SchneaggmapStateBuilder` in commonMain used by both.

Scope the collection to the car `Session` lifecycle (`session.lifecycleScope`).

### 5. Template + buttons

`MapWithContentTemplate.Builder()`:

- `setMapController(MapController.Builder().setMapActionStrip(...).setPanModeListener(...))` – map action strip, max 4 icon-only actions:
  1. `Action.PAN` (mandatory for gestures; hidden on touch screens)
  2. Zoom in (`zoom + 1`)
  3. Zoom out (`zoom - 1`)
  4. Recenter on own location
- `setActionStrip(...)` – header actions: toggle friends, toggle events, cycle style (light styles only; dark mode is forced by the car).
- `setContentTemplate(ListTemplate)` – list of friends with a valid location, sorted by distance from own location. Row: name, distance, online dot. Tap → `cameraState.animateTo(friend position, zoom 14)`. Row count capped via `carContext.getCarService(ConstraintManager::class.java).getContentLimit(CONTENT_LIMIT_TYPE_LIST)`.
- When a marker is tapped on the surface: content pane switches to a `PaneTemplate` with the entry/user name + "Back to list" action.

Strings for the car UI go through the normal `strings.xml` (en/de/it) – car actions take `CarText`, built from `carContext.getString(...)` on the Android resource IDs. Icons: `CarIcon.Builder(IconCompat.createWithResource(...))` from Material vector drawables.

### 6. Foreground gating

`AppLifecycleManager` (commonMain):

```kotlin
private val _isCarSessionActive = MutableStateFlow(false)
val isCarSessionActive: StateFlow<Boolean>
fun setCarSessionActive(active: Boolean)   // called from SchneaggmapCarSession
val isAppOrCarActive: Boolean get() = isAppInForeground || isCarSessionActive.value
```

Switch the three gates to `isAppOrCarActive`:

- `GlobalViewModel.kt:229` (own location push)
- `SocketConnectionManager.kt:234` and `:242` (reconnect loop)

Leave `SocketConnectionMessage.kt:204` (notification suppression when in foreground) on plain `isAppInForeground`: with only the car active, a chat notification should still be shown on the phone/car.

Also trigger the same "resume" path the phone uses when the car session starts (socket connect + sync), by emitting `appResumedEvent` from `setCarSessionActive(true)` **only if** the phone app is not already in foreground, to avoid a double sync.

### 7. Dark mode + config changes

- `Session.onCarConfigurationChanged(Configuration)` → screen recomputes `mapStyleUrl`: `if (carContext.isDarkMode) DARK.tileUrl else phoneSetting.tileUrl`, calls `invalidate()`.
- `SchneaggmapLayers` already re-keys on `state.mapStyleUrl`.

## Files touched

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | add `androidx-car-app` version + library |
| `composeApp/build.gradle.kts` | `androidMain` implementation of `libs.androidx.car.app` |
| `androidApp/src/main/AndroidManifest.xml` | `MAP_TEMPLATES` permission, two `meta-data`, `CarAppService` |
| `androidApp/src/main/res/xml/automotive_app_desc.xml` | new |
| `composeApp/src/androidMain/.../car/SchneaggmapCarAppService.kt` | new |
| `composeApp/src/androidMain/.../car/SchneaggmapCarSession.kt` | new |
| `composeApp/src/androidMain/.../car/SchneaggmapCarScreen.kt` | new |
| `composeApp/src/androidMain/.../car/CarMapSurfaceRenderer.kt` | new |
| `composeApp/src/androidMain/.../car/CarMapUiState.kt` | new |
| `composeApp/src/commonMain/.../schneaggmap/presentation/uielements/SchneaggmapLayers.kt` | new, extracted from `SchneaggmapScreen.kt` |
| `composeApp/src/commonMain/.../schneaggmap/presentation/SchneaggmapScreen.kt` | replace inline layer block with `SchneaggmapLayers(...)` call |
| `composeApp/src/commonMain/.../app/AppLifecycleManager.kt` | `isCarSessionActive` flag + `isAppOrCarActive` |
| `composeApp/src/commonMain/.../app/GlobalViewModel.kt` | gate at line ~229 |
| `composeApp/src/commonMain/.../datasource/network/socket/SocketConnectionManager.kt` | gates at lines ~234 / ~242 |
| `composeApp/src/commonMain/composeResources/values{,-de,-it}/strings.xml` | car UI strings (`car_zoom_in`, `car_zoom_out`, `car_recenter`, `car_toggle_friends`, `car_toggle_events`, `car_friends_title`, `car_back_to_list`) |

## Implementation order

1. Version catalog + dependency + manifest + `automotive_app_desc.xml`. Gradle sync (user runs it).
2. `SchneaggmapLayers` extraction in commonMain. Verify phone map still behaves the same (this step alone is a safe refactor and can be a separate PR).
3. `CarAppService` + `Session` + `Screen` returning a `MapWithContentTemplate` with a black surface and the friend list only. Test on DHU: app appears, list works.
4. `CarMapSurfaceRenderer`: VirtualDisplay + Presentation + `SchneaggmapLayers`. Test on DHU: tiles render, markers show.
5. Gesture forwarding + map action strip buttons.
6. `AppLifecycleManager` car flag + socket/location gate changes. Test: phone screen off, friend moves → car marker updates.
7. Dark mode handling.
8. `readme-changelog-updater` + `string-resource-extractor` agents.

## Testing

- **Desktop Head Unit (DHU)**: `android` CLI / SDK Manager → "Android Auto Desktop Head Unit emulator". Phone: enable Developer settings in the Android Auto app, "Start head unit server", `adb forward tcp:5277 tcp:5277`, run `desktop-head-unit`.
- **Real car without Play**: Android Auto app → Developer settings → "Unknown sources" allows sideloaded car apps.
- **Play release**: enable Android Auto under Play Console → App content → "Android Auto", app must pass the car app quality guidelines for POI apps.

## Risks

| Risk | Mitigation |
|---|---|
| MapLibre GL rendering inside `Presentation` on a `VirtualDisplay` | Documented Google path for Compose; MapLibre `MapView` is a standard GLSurfaceView/TextureView. If GL fails, fallback is `MapSnapshotter` → bitmap → `Surface.lockCanvas` per camera change (works, not smooth). |
| Host `Surface` size changes / density | Recreate VirtualDisplay on `onSurfaceDestroyed` + `onSurfaceAvailable`; never resize in place. |
| Two MapLibre instances (phone map open + car) | Acceptable memory-wise. Phone screen is usually off while driving. |
| Play review rejects POI category for a friend map | Description must frame it as "find friends' and shared places on the map". If rejected, sideload / "Unknown sources" still works for private use. |
| Host validator in release | Use the library's `hosts_allowlist_sample` allowlist, not `ALLOW_ALL_HOSTS_VALIDATOR`. |

## Out of scope (later)

- Chat notifications on the car display: needs `NotificationCompat.MessagingStyle` + `CarAppExtender` in `Notifier.android.kt`. Small separate feature.
- Creating/editing map entries or events from the car.
- Search on the car (host `SearchTemplate` exists but only works parked).
- Android Automotive OS (built-in car OS, no phone) – would need a separate APK/flavor and the app running entirely on the car; not planned.
- Instrument cluster map (navigation category only).
