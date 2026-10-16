# GhettoGuesser plan

GeoGuessr-style game in the games tab. Each round shows a random geotagged Wikimedia Commons photo from inside the chosen map. The player pins where it was taken; the score depends on distance. 5 rounds per game, one leaderboard per map.

## Decisions (2026-10-13)

- **Repo:** SchneaggchatV3 (games tab) + SchneaggchatV3server. Not Hufly.
- **Maps:** created only by the admin, stored on the server, picked by users. Created with an admin-only in-app editor.
- **Map shape:** polygon (list of lat/lon points).
- **Attribution:** small credit line on the photo ("Foto: <author>, <license>"); tapping it opens a sheet with author, license (linked to LicenseUrl) and the Commons file page link. Title and description stay hidden until the guess is locked in.
- **Rounds:** 5 rounds, no timer, unlimited skips without penalty.
- **Scoring:** same as the old SchneaggchatV2 game, see "Scoring (from SchneaggchatV2)". Game total 0..25000. `maxDistance` = largest distance from the real location to a polygon point.
- **Leaderboard:** total of 5 rounds, per map, all-time + weekly (reuse existing highscore board with a new per-map key).
- **Data source:** only the Commons API. Guess map reuses the existing Schneaggmap MapLibre style (no OSM public tile servers).

## Scoring (from SchneaggchatV2)

Source: `github.com/lerchenflo/SchneaggchatV2Android`, `schneaggguess_guessactivity.java` (`calculatePoints`, `interpolate`) and `schneaggguess_selectoractivity.java` (`Region.MaxDistance`, 5 rounds, highscore per region).

Per round, with `d` = distance between guess and real location in meters:

1. `maxDistance` = largest distance from the **real location** to any polygon point (so it changes per photo, not per map).
2. `correctMeters = (int)(maxDistance * 0.005)`; `d <= correctMeters` gives the full 5000 points.
3. `d >= maxDistance` gives 0 points.
4. In between: piecewise linear interpolation over four equal segments of `[correctMeters, maxDistance]`:

| Distance (share of the range) | Points |
|---|---|
| `correctMeters` | 5000 |
| 1/4 | 3750 (6/8) |
| 1/2 | 1562.5 (2.5/8) |
| 3/4 | 312.5 (0.5/8) |
| `maxDistance` | 0 |

5. Result truncated to `Int` (`(int)`), at least 0.

Game: 5 rounds, total points 0..25000 plus total time (sum of each round's time from photo shown to guess). Both go to the highscore per map; time is the tiebreaker, matching the existing `GameScore.timeMillis`.

Differences on purpose:
- V2 measured the guess distance with `getdistance` (`111320 * cos(lat) * dLon`), passing degrees to `Math.cos`, which expects radians. Around Dornbirn this overstates east-west distances by about 40 %. `maxDistance` used osmdroid's correct `distanceToAsDouble`. The new game uses haversine for both; the curve stays the same.
- Ported to Kotlin as a pure function `calculatePoints(guessDistance, maxDistance): Int` in the domain layer, with tests for each segment border.

## API check (done 2026-10-13, Dornbirn 47.4136, 9.7446)

Endpoint `https://commons.wikimedia.org/w/api.php`, no key, works.

Findings that change the original idea:

1. **Results are nearest-first.** `generator=geosearch` with radius 10000 and limit 500 returned 500 files, but all within **461 m** of the center (142 distinct spots). One query around the city center would only show the old town. Fix: per round, pick a random point inside the polygon and query a small radius around it (e.g. `ggsradius=1000`, `ggslimit=50`). Retry with a new point when empty (fields, lake, forest).
2. **Coordinates are camera positions.** 499 of 500 have `type=camera`. The "subject vs. camera" noise is small; no filter needed. Still drop files whose coordinate is outside the polygon.
3. **Clusters:** many photos share one spot (photo series, e.g. 20 lecture photos at the same point). Dedupe by rounded coordinate (4 decimals) and avoid photos within 100 m of earlier rounds in the same game.
4. **imageinfo continuation:** with `iiprop=extmetadata` on 50 pages the response can stop early (`continue: ||imageinfo`). Split into two calls: geosearch for candidates (cheap), then `prop=imageinfo&pageids=...` only for the chosen photo(s), with `colimit=max` when coordinates are needed.
5. **Thumbnails:** `iiurlwidth=1200` is served as a 1280 px standard size. Request a standard width (1280) directly.
6. **Mime:** 497 of 500 are JPEG. Accept only `image/jpeg` and `image/png`.

Candidate query (per round):

```
action=query&format=json&formatversion=2
list=geosearch&gscoord=<lat>|<lon>&gsradius=1000&gsnamespace=6&gslimit=50&gsprop=type
```

Detail query (chosen photo, or a small batch for prefetch):

```
action=query&format=json&formatversion=2&pageids=<ids>
prop=imageinfo|coordinates&colimit=max
iiprop=url|size|mime|extmetadata&iiurlwidth=1280
iiextmetadatafilter=Artist|LicenseShortName|LicenseUrl|AttributionRequired|ObjectName|ImageDescription
```

Every request sends `User-Agent: GhettoGuesser/1.0 (schneaggchat@gmail.com)` (native targets). Image downloads from `upload.wikimedia.org` send the same header through Coil's Ktor network fetcher.

## Licensing check

Licenses of the 500 Dornbirn files:

| License | Files | Attribution required |
|---|---|---|
| CC BY-SA 4.0 / 3.0 / 3.0 AT / 2.5 | 465 | yes |
| CC BY 3.0 / 4.0 | 27 | yes |
| Public domain / CC0 | 8 | no |

- Without attribution, showing 98 % of these photos violates their license (copyright infringement). Using only PD/CC0 files would leave ~8 photos per town, which is not playable. So attribution is mandatory.
- CC BY / BY-SA allow attribution "in any reasonable manner based on the medium". A credit line plus a tappable sheet with links meets this.
- Required per photo: author (`Artist`, HTML stripped), license name linked to `LicenseUrl`, link to the file page (`descriptionurl`), and a note if the photo was modified.
- Showing the image with `ContentScale.Fit` and resizing is not a modification. If the UI crops (`ContentScale.Crop`), add "zugeschnitten" to the credit. Plan: use `Fit` with zoom/pan, no crop.
- ShareAlike only applies to adaptations; displaying the photo unchanged does not trigger it.
- Skip any file without `LicenseShortName`, or with a license that is not CC BY*, CC BY-SA*, CC0 or Public domain (e.g. "Attribution only", GFDL-only, fair use).
- Do not use the Wikipedia/Wikimedia logo or name as branding. "Fotos von Wikimedia Commons" as plain text is fine.
- Not legal advice; this is the standard reading of the CC licenses and Wikimedia's reuse guide.

## Client (SchneaggchatV3)

Package `games/presentation/ghettoguesser` plus data/domain files, following the existing game layout.

### Domain
- `GuessMap(id, name, polygon: List<LatLong>, updatedAt)`; reuse `schneaggmap/domain/LatLong`.
- `CommonsPhoto(pageId, imageUrl, width, height, lat, lon, author, licenseName, licenseUrl, filePageUrl, title, description)`.
- Geo helpers: `haversineMeters`, `pointInPolygon`, `randomPointInPolygon` (bounding box + rejection), `maxDistance(realLocation, polygon)`. Check `MapGeoUtils.kt` first and extend it.
- `calculatePoints(guessDistance, maxDistance)` (see Scoring).
- `GameId.GHETTO_GUESSER` (must match the server enum).

### Data
- `CommonsPhotoSource` interface + Ktor implementation (kotlinx.serialization DTOs for geosearch and imageinfo, HTML strip of `Artist`).
- `PhotoPicker`: random point in polygon, candidate query, filters (min 800 px short side, aspect ratio 0.4..2.5, allowed mime, allowed license, inside polygon, not near earlier rounds), retry up to N times, then "no photos" error. Keep a small prefetch queue (next 2 rounds) so rounds start fast.
- `GuessMapRepository`: fetch maps from server, cache locally (offline: last cached list); admin create/update/delete.
- Leaderboard: reuse `GameHighscoreRepository` with the map id as board key.

### Presentation (MVI)
- Map list screen: maps from server, tap to open map detail with its leaderboard (all-time / weekly) and "Spielen".
- Round screen: photo (Coil 3, zoom/pan, Fit), credit line, info sheet, "Überspringen", button to open the guess map.
- Guess map: MapLibre with the Schneaggmap style, polygon outline, tap places pin, "Tipp abgeben".
- Result per round: true location, line between pin and target, distance, points, now photo title and description.
- Game over: total, submit score, reuse `GameOverOverlay` / `HighscoreUpload`.
- Admin editor (only shown for admins): tap points to build the polygon, undo point, name, save; shows how many Commons photos the area has (one test query) before saving.
- Error states: no network, no photos found for the map, server unreachable (map list from cache).
- Links open through `LocalUriHandler`.

### Tests (TDD)
- Geo helpers, scoring, HTML strip, license filter, photo filters, dedupe, DTO parsing from a recorded Commons response (Ktor `MockEngine`), ViewModel round flow with a fake photo source.

## Server (SchneaggchatV3server)

1. `Game.GHETTO_GUESSER` in the Game enum.
2. Per-map board key on scores: add optional `boardKey: String?` to `GameScore` and to the submit/highscore requests; index `{game, difficulty, boardKey, score, timeMillis}`. Highscore queries filter by `boardKey` (null for all other games, so existing boards are unchanged). GhettoGuesser scores are excluded from the global ranking (per-map boards are not comparable).
3. Validate GhettoGuesser submissions: `boardKey` must be an existing, not deleted map id; score in 0..25000.
4. New `guessmap` feature: `GuessMap { id, name, polygon: List<{lat, lon}>, createdBy, updatedAt, deleted }`.
   - `GET /guessmaps` (authenticated): all non-deleted maps.
   - `POST /guessmaps`, `PUT /guessmaps/{id}`, `DELETE /guessmaps/{id}` (soft delete): `AdminGuard.requireAdmin()`.
   - Validation: name 1..40 chars, 3..200 points, valid lat/lon, polygon not self-intersecting.
5. Client needs a way to know it is admin (check if an existing endpoint already returns this; else add `isAdmin` to the own-user response).

## Open points (later)


- Daily challenge per map (same 5 photos for everyone).
- Users creating their own maps.
- Report a bad photo (blocklist on server).
