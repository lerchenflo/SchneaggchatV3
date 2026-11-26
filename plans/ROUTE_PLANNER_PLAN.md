# Route planner (group extension `ROUTE_PLANNER`)

GitHub #774 (tours by motorbike, car, bicycle, on foot). Decisions: user 2026-10-29/30 and 2026-11-24 (interview + mockups, layout **M3**).
Data layer and server are done (`ExtensionItemData.RoutePoint`, `ExtensionSettings.Route`, `upsertItem` / `deleteItem`); **no server change needed**.

## Behaviour

- **Own screen**, opened from the group's extensions like Money split and Shared text (not the main map). Same OSM tiles as Schneaggmap (`SettingsRepository.getMapStyleUrlFlow()`), attribution bar always visible.
- **Layout M3**: `ActivityTitle` (back, title, Navigation icon, ⋮) + segmented switch `Karte | Liste · n` (like Money split).
  - **Karte**: full map, route line in `sortIndex` order, chips on the map `Alle | Tag 1 | Tag 2 …` (only when the route spans ≥ 2 days; since 2026-11-26 under the Schneaggmap's chips, see `SHARED_MAP_PLAN.md`), summary pill (transport icon, distance, rough time of the shown part), locate button, swipeable point cards at the bottom (swipe = camera flies to that point, tap marker = its card).
  - **Liste**: summary row + "Reihenfolge", one card per day (header: date, day distance + time), drive-through points folded into one "über A, B" line, the same day chips on top.
- **Camera**: points exist → fit the route (or the picked day); empty → own location; no location → Schneaggmap default camera.
- **Point types** (all four): `STOP` "Wegpunkt", numbered green pin (start, meeting point, break, finish), `WAYPOINT` "Durchfahrt" small dot without label (steers the route), `ACCOMMODATION` blue bed pin (overnight), `POI` "Sehenswert" amber star (viewpoint, photo stop). Picked with an "Art" dropdown (like the event type picker), each entry with its use case.
- **Add**: long press on the map → temporary marker + point sheet: Art, Name (required, except Durchfahrt: empty is sent as the translated "Durchfahrt"; server needs 1..100 chars), Beschreibung (≤ 500), Tag + Uhrzeit (optional, also for Durchfahrt), read-only coordinates, hint where it goes in. Saved at once.
- **Order**: a new point without time goes into the **nearest segment** (smallest detour; first/last point compared too). A point **with a time sorts automatically**: it goes before the first later timed point, else to the end. Editing a time re-sorts it.
- **sortIndex**: Int with gaps (step 1024); a new or moved point takes the middle between its neighbours, so normally one call; no room → all points renumbered (one call per changed point, stop at the first error).
- **Reorder sheet** ("Reihenfolge"): drag only points **without** time (timed points show no handle), Save writes changed points one by one.
- **Edit**: tap marker or card/list row → point sheet with the values; "Verschieben" opens the crosshair pick overlay (moved from Schneaggmap to `sharedUi/map`); delete button only for point creator, extension creator or group admin (server rule kept). Every member may edit every point.
- **Delete**: at once, snackbar "„X" gelöscht · Rückgängig" re-adds the same data (new id). `SnackbarManager` gets an optional action.
- **Schneaggmap places**: "Orte" chip shows map entries as grey pins (since 2026-11-26: the shared places chip and type icons, plus sightings, events and friends, see `SHARED_MAP_PLAN.md`); tap → card "Ort öffnen" + "Zur Route hinzufügen" (copies name + coordinates, sets `mapEntryId`, nearest segment), then the point sheet opens prefilled. A linked point's card has "Ort öffnen" (opens Schneaggmap on that entry); hidden when the entry no longer exists.
- **Distance / time**: straight line between points (`distanceMeters`), rough time = distance × 1.3 ÷ speed of the leg's mode (on foot 4.5, bicycle 15, motorbike 50, car 60 km/h), rounded to 5 min, shown with "~" and "Luftlinie".
- **Transport mode per leg** (user, 2026-11-25): every point stores how the group gets TO it (`RoutePoint.transportMode`, enum name `FOOT`, `BICYCLE`, `MOTORBIKE`, `CAR`; server field built 2026-11-25 on `SchneaggchatV3server`, uncommitted then). The first point has none. A new point takes the mode of the point before it, so the last pick continues. The create dialog chips ("Meistens mit", preselected Auto) only set `ExtensionSettings.Route.transportMode`, the default for points without their own; no ⋮ entry. Each leg's time uses its own speed; day and route totals add the legs up. The Google Maps link uses the mode most legs use.
- **Point type names** (user, 2026-11-25): `STOP` is shown as "Wegpunkt" (a place we visit: start, meeting point, break, finish), so the first point is no "stop"; `WAYPOINT` stays "Durchfahrt".
- **Navigation**: title icon opens a Google Maps directions link for the whole route from the first point (`api=1`, origin, destination, up to 9 waypoints, travelmode walking / bicycling / driving). More than 11 points: first 11 + snackbar hint. Coordinates reach Google only on tap.
- No onboarding tour step (user, 2026-11-24). `GroupExtensionType.isAvailable` → true for `ROUTE_PLANNER`.

## Code

| Layer | File | What |
|---|---|---|
| domain | `groupextensions/domain/route/RouteTransportMode.kt` | enum + speed, `fromWire()` |
| domain | `groupextensions/domain/route/RoutePlanning.kt` | ordered points, nearest segment, time sort, sortIndex gaps, distance + time, day grouping |
| domain | `groupextensions/domain/route/RouteNavigationLink.kt` | Google Maps URL |
| presentation | `groupextensions/presentation/route/` | `RouteState`, `RouteAction`, `RouteViewModel`, `RouteScreen` (+Root, preview), `RouteMapLayers`, `RoutePointCards`, `RouteDayList`, `RoutePointSheet`, `RouteReorderSheet`, `RoutePlaceCard`, `RoutePointKindUi`, `RouteTransportModeUi` |
| shared | `sharedUi/map/LocationPickOverlay.kt`, `sharedUi/map/MapLocateButton.kt` | moved out of `schneaggmap/presentation` so both features use them |
| wiring | `GroupExtensionScreenRoot` branch, `Modules.kt`, `AddExtensionDialog` transport chips, `SnackbarManager` action, strings en/de/it |

Mockups (local only): session scratchpad `routemock/mockups.html` (merge, M3 = third frame) and `mockups-v1.html`.

## State 2026-11-25 (uncommitted)

- Built: everything above incl. per-leg transport and "Wegpunkt" naming. JVM compiles; Android / iOS
  compile not re-run after the per-leg change. Server `route_point.transportMode` built on
  `SchneaggchatV3server` (uncommitted, not deployed): `String?`, max 30, null = route default, "" allowed.
- Next: shared base map (`plans/SHARED_MAP_PLAN.md`), then phase 2 below.

## Phase 2 (planned 2026-11-25, user decisions)

1. **Umstieg (switch vehicle)**: new point kind `TRANSFER` (enum name, server `RoutePointKind` too).
   The sheet shows "Anreise mit" (how we get there, `transportMode`) and **"Weiter mit"** (new field
   `continueTransportMode: String?` on `route_point`, only for TRANSFER). "Weiter mit" is the mode of
   the leg after it: saving it also sets the next point's `transportMode` (one extra write), and a new
   point after a TRANSFER defaults to it (default rule: the point before gives its "Weiter mit" if it
   is a TRANSFER, else its own mode). Marker: both mode icons (car → motorbike) on one badge.
2. **Alternative paths**: from a point, start an alternative path with a name, an optional
   description and a mode; it leaves the main route at that point and rejoins at a later point the
   user picks (e.g. car around the mountain, motorbike over it).
   - Data: new item type `route_path` in the same ROUTE_PLANNER extension (`ExtensionItemData.RoutePath`:
     `name` ≤ 100, `description` ≤ 500, `transportMode`, `fromItemId`, `toItemId`, optional
     `memberIds`), and `route_point.pathId: String?` (null = main route). A path's points have their
     own `sortIndex` order inside the path; same insert rules (nearest segment of that path, time sort).
   - Map: every path drawn in its legs' mode colors; the list shows the path as a branch card between
     its start and rejoin point; distance / time per path; day totals count the main route only.
   - Delete a path: its points are deleted one by one, then the path item (same delete rule as points).
3. **Colors per movement type** (theme colors, no hardcoded): car = `secondary`, motorbike =
   `warningColors.warning`, bicycle = `primary`, on foot = `tertiary`; each leg drawn in its mode's
   color (casing stays), small legend in the summary pill / list summary.
4. **Annotations in names and descriptions**: point and path names and descriptions use the existing
   `ComboInputField` (typing `@…` suggests users, places, events…) and show with `ComboText`, so
   `@Joni` or a place annotation resolves like in the chat.
5. **Members on a path** (only if cheap): optional `memberIds`, picked with the participant picker of
   the money split editor (`SplitExpenseEditorScreen`) extracted into a shared member picker; shown as
   avatars on the path card. If that extraction is not small, skip it: the user writes `@names` into the
   description (point 4).

Server hand-over for phase 2 (send to the `SchneaggchatV3server` session, or its `Todo.md`): enum value
`TRANSFER`; `route_point.continueTransportMode: String?` (≤ 30); `route_point.pathId: String?` (an item id
of a `route_path` in the same extension or null); new item data `route_path` with the fields above
(`memberIds` must be group members, like split shares); old clients ignore unknown keys, but an
unknown enum value fails their decoding (route planner not released yet, so fine).
