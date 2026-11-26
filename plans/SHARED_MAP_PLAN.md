# Shared base map (Schneaggmap + route planner)

User request 2026-11-25, plan approved the same day. One base map with overlays that every map screen
adds as needed. **Reusing code has top priority** (memory `reuse-first`). Today the route planner has
small copies (grey place pins, own locate wiring, own position dot) and lacks the zoom slider, place
type icons, the "shown places" dropdown and the other layers.

## User decisions (2026-11-25)

- Shown place types stay per map and are not saved (same as the Schneaggmap today: empty on open).
- The route map also offers **events, friends' live locations, police/radar sightings**, each with the
  same chip as on the Schneaggmap (no snail trails, no search bar, no friends bar).
- Tapping a Schneaggmap place on the route map: the small add card ("Zur Route hinzufügen", where it
  goes) **plus "Ort öffnen"** (opens the Schneaggmap on that entry, existing deep link).
- Defaults on the route map: as on the Schneaggmap (sightings, events and users on, places off).
- Route map taps on other layers (my choice, tell the user): event pin → `Route.EventDetail(eventId)`;
  friend → `Route.Schneaggmap(focusUserId = id, openRequestId = now)`; own position → nothing.

## Target structure

Shared UI in `sharedUi/map/` (sharedUi may read feature `domain/`, it already uses `LatLong`). Shared
data loading in `schneaggmap/domain/` (domain may read other features' `domain/`).

| New | From | Content |
|---|---|---|
| `schneaggmap/domain/MapLayersData.kt` | fields of `SchneaggmapState` | `entries`, `activeSightings: Map<String, Set<LocationType>>`, `usersWithLocation`, `onlineFriendIds`, `ownUser`, `eventsWithLocation`, `joinedEventIds`, `useClustering`, `mergeUsers` |
| `schneaggmap/domain/MapLayersSource.kt` (Koin `factory`) | `SchneaggmapViewModel` init + `state` combine | `fun observe(): Flow<MapLayersData>`: map entries, sightings ticking every minute (`activeSightingTypes`), friends with valid location (`getFriendsFlow("")` + `isLocationValid()`), `onlineFriendIdsFlow`, own user (`getUserByIdFlow(ownId)`), events with location minus DISMISSED, joined = ACCEPTED, settings `getMergeMapLocationsFlow` / `getMergeMapUsersFlow` |
| `sharedUi/map/layers/MapLayersDisplay.kt` | toggle fields + VM actions | `MapLayersDisplay(enabledTypes, expandedGroups, isPlacesDropdownVisible, showActiveSightings = true, showEvents = true, showUsers = true)`, `sealed interface MapLayersAction` (ToggleActiveSightings, ToggleEvents, ToggleUsers, ToggleAllPlaces, ToggleType, ToggleGroup, ToggleGroupExpanded, TogglePlacesDropdown, ShowTypes(types) for "make the entry visible") and pure `fun MapLayersDisplay.reduce(action): MapLayersDisplay` |
| `sharedUi/map/layers/MapContentLayers.kt` | `SchneaggmapLayers.kt` minus snail trails | places (type icons, merged multi-type icon, clustering, sighting highlight), friends + hock clusters, events, own position; params `data`, `display`, `mapState`, `ownLocation`, `clicks: MapLayerClicks(onEntryClick, onUserClick, onEventClick, onOwnUserClick)`; helpers (`clusterUsersByProximity` …) move along |
| `sharedUi/map/layers/MapFilterChipsRow.kt` (+ `ShownLocationsDropdown`, `LocationDropdownContent`, `PlacesSplitButton`) | `schneaggmap/presentation/uielements/` | takes `display` + `onLayersAction`, optional `extraChips` slot (Schneaggmap: snail trails chip) and `placesChipModifier` (keeps the tour target `schneaggmap_location_dropdown`) |
| `sharedUi/map/MapZoomSlider.kt` | `schneaggmap/presentation/uielements/` | moved as is |
| `sharedUi/map/BaseMap.kt` | `SchneaggmapScreen` + `RouteMap` | `rememberBaseMapState(styleUrl, locationPermissionGranted, initialCamera) { ownLocation -> layers }` (location provider/state, MapState) and `BaseMap(state, onMapClick, onMapLongClick, zoomSnapPositions, bottomCenter = {} /* speed badge */, bottomContent = {} /* friends bar */, pickLocation: PickRequest? /* crosshair */, content = {} /* top overlays, cards */)`: zoom slider (CenterEnd, snapping), scale bar, locate/compass + follow mode loop, attribution strip; optional one-time auto-center on own location |

Already shared (2026-11-24): `MapLocateButton`, `MapAttributionBar`, `LocationPickOverlay`, `CoordinateView`, `fitCameraTo`.

What stays in the features:
- **Schneaggmap**: search bar, snail trails (`SnailTrailLayers` + chip in `extraChips`), friends bar,
  speed badge, info cards, focus effects (entry / area / user), long press = new entry. `SchneaggmapState`
  replaces the moved fields by `layers: MapLayersData` and `layerDisplay: MapLayersDisplay`;
  `SchneaggmapAction` gets `OnLayersAction(MapLayersAction)` instead of its toggle actions (keep
  closing the dropdown on entry select, `ShowTypes` when an entry is opened by deep link / search).
- **Route planner**: route line + point markers + new-point marker (`RouteMapLayers` keeps only these),
  day chips, point cards, list, sheets, long press = new point. `RouteState` gets `layers` +
  `layerDisplay`; `RouteViewModel` collects `MapLayersSource`; its own place pins, `OwnPositionLayer`,
  locate wiring and "Orte" chip go away; `RoutePlaceCard` gets "Ort öffnen".

## Steps

1. Domain: `MapLayersData` + `MapLayersSource` (Koin); Schneaggmap VM uses it (no behaviour change).
2. `MapLayersDisplay` + `reduce`; Schneaggmap state/actions switch to it.
3. Move `MapZoomSlider`, extract `MapContentLayers` (+ `SnailTrailLayers` stays in Schneaggmap) and
   `MapFilterChipsRow`; Schneaggmap uses them.
4. `BaseMap`; Schneaggmap screen switches to it (same look and behaviour).
5. Route planner on `BaseMap` + `MapContentLayers` + shared chips; delete its copies.
6. Compile JVM / Android / iOS (+ iOS tests); device test both maps (Schneaggmap must look and work
   exactly as before: chips, dropdown, clustering, hocks, sightings, follow mode, zoom snap, pick mode).

Risk: steps 2-4 touch the Schneaggmap (biggest map); pure move/extract, no behaviour change.
