# Map Location Ratings Plan (stars + text + lazily loaded images)

Both repos: client `SchneaggchatV3` (KMP) and server `SchneaggchatV3server` (Spring Boot + Mongo). Written 2026-09-10, nothing implemented yet.

**Goal**: every map entry can be rated by every user: **1-5 stars**, an optional **personal text**, and optionally **up to 3 images**. The entry info sheet shows the **average + count** at a glance; a dedicated **ratings screen** shows all ratings and lets the user write / edit / delete their own. Rating **texts and metadata are synced offline-first** like map entries; **image bytes are fetched lazily** only when the ratings screen is open and the image scrolls into view.

**Core idea**: ratings live in their **own Mongo collection + own Room table + own IdTimeStamp sync stream + own socket push**, exactly mirroring how `map_entries` already works. Map entries stay untouched (no denormalised average, no bloated `MapChange` payloads, no "last changed by" pollution). Averages are computed **client side from Room** (`GROUP BY entryId`), same as V2 did in memory.

---

## 0. What V2 did (reference, verified 2026-09-10)

| V2 piece | Detail |
|---|---|
| Model | `LocationRating(id, locationId, userId, rating: Int, comment: String?, lastChanged)` embedded in `Locationsm.ratings` |
| Constraint | `UNIQUE(location_id, user_id)` - **one rating per user per location**, editing overwrites |
| Endpoints | msgtype 57 `UPDATELOCATIONRATING` (stars header + comment body), 58 `REMOVELOCATIONRATING` |
| Aggregate | `getAverageRating()` client side, `"(%.1f Sterne, %d Bewertungen)"`, "Noch keine Bewertungen" when empty |
| UI | Bottom sheet: average `RatingBar` (0.5 step, read-only) + count; own-rating block (edit/delete); input block (1.0 step + "Kommentar (optional)"); other users' ratings listed below with avatar, own rating skipped in that list |
| Extra | Map settings → "Meine Bewertungen" list, tap zooms to the location and opens its sheet |
| Images | **None** in V2 - new in V3 |

V3 keeps the V2 semantics (one rating per user per entry, whole-star input, half-star average display) and adds images.

---

## 1. Current state V3 (verified 2026-09-10)

| Piece | Where | Relevant detail |
|---|---|---|
| Server model | `schneaggmap/model/MapEntry.kt` | `@Document("map_entries")`, `updatedAt` drives sync, `deleted` soft flag, no `@Version` |
| Server endpoints | `schneaggmap/SchneaggmapController.kt` | `POST /map/upsert`, `DELETE /map/delete?entryid=`, `POST /map/sync?page&page_size` (body `List<IdTimeStamp>`) |
| Server sync | `SchneaggmapService.mapSync` | `findByDeletedFalse()` diffed against client timestamps, paged, `deletedEntries` only on page 0 |
| Server push | `NotificationService.notifyMapUpdate` (:466) | `SocketConnectionMessage.MapChange` **broadcast to everyone** (`excludeUserId` = actor) |
| Server images | `util/ImageManager.kt` | Filesystem only: `/app/images` (profile pics), `/app/images_messages` (message images), PNG re-encode, `downscaleIfNeeded` to 700 KB, EXIF upright. Docker volumes in `server_docker/docker-compose.yml:91-92,103-104` |
| Server upload shape | `message/MessageController.kt:53` | `consumes = MULTIPART_FORM_DATA_VALUE`, `@RequestPart("image") MultipartFile` + `@RequestPart("request")` JSON DTO |
| Server validation | `util/ValidationUtils.kt:141` | `validatePicture()`: jpeg/png/gif/webp, ≤ 3 MB; `application.properties` multipart max-file 3 MB / max-request 10 MB |
| Server per-user-per-parent precedent | `group/model/GroupMember.kt:16` | `@CompoundIndex(name = "userid_groupId_idx", def = "{'userid': 1, 'groupId': 1}", unique = true)` |
| Client domain | `schneaggmap/domain/MapEntry.kt` | Plain data class, `updatedByName` resolved server side |
| Client Room | `schneaggmap/data/dtos/MapEntryDto.kt`, `datasource/database/Daos.kt:431 MapEntryDao` | DB version **84**, `fallbackToDestructiveMigration(dropAllTables = true)` (`CreateDatabase.kt:12`), `RoomTypeConverters.stringListToString` exists (:121) |
| Client wire | `datasource/network/requestResponseDataClasses/MapNetworkDtos.kt` + `MapResponseMapper.kt` | `MapEntryResponse`, `MapSyncResponse`, `MapEntryRequest` |
| Client network | `datasource/network/NetworkUtils.kt:1138-1160` | `mapSync`, `upsertMapEntry`, `deleteMapEntry`; `safePostMultipart` (:112) + `appendProfilePic` (:129) |
| Client sync | `datasource/AppRepository.kt:629 mapEntryIdSync()` | Runs inside `dataSync(reason = "mapInit")` job type `MAP`, triggered from `SchneaggmapViewModel.kt:479` on map open |
| Client CRUD | `AppRepository.kt:660-702` | Network call → on success write to `MapRepository` (Room) |
| Client socket | `datasource/network/socket/SocketConnectionMessage.kt:65,420` | `MapChange` → `mapRepository.upsertMapEntry` / `deleteMapEntry` |
| Client image download | `AppRepository.kt:2241 getPicturesForMessageIds` | `networkUtils.getImageForImageMessage(id)` → `pictureManager.savePictureToStorage(bytes, id + PICTURE_FILE_NAME)` → path into Room; `getMissingPics()` (:873) only fetches when `checkImageExists()` is false |
| Client image display | `chat/.../ImageMessageContentView.kt`, `FullScreenImageDialog.kt`, `sharedUi/picture/ProfilePictureView.kt` | Coil3 `AsyncImage(model = absoluteFilePath)`; `AppImageLoader.kt` caps parallel decodes at 3 |
| Client image pick | `chat/presentation/newchat/GroupCreator.kt:230` | `rememberImagePickerKMP(ImagePickerKMPConfig(galleryConfig = GalleryConfig(...)))` → `loadBytes()` → `pictureManager.downscaleImage(bytes, 500_000)` |
| Client info sheet | `schneaggmap/presentation/uielements/MapEntryInfoCard.kt` | `ModalBottomSheet`; `EntryTitleView` has the literal `//TODO: Small row with ratings` (line ~270) |
| Client nav | `app/navigation/Route.kt:42`, `app/App.kt:589,670` | `Route.Schneaggmap`, `Route.SchneaggmapSettings` are entries in the map tab; adding a screen = `Route` + `entry<Route.X>` + Koin `viewModel` |
| Koin | `di/Modules.kt:138,195` | `singleOf(::MapRepository)`, explicit `viewModel { (initialEntryId, currentlyEditedEvent) -> SchneaggmapViewModel(...) }` |
| Star / comment composables | - | **None exist** outside `games/` (which must not be used as reference) |
| Current user id | `app/SessionCache.kt` | `SessionCache.requireLoggedIn()?.userId` |

### 1.1 Why a separate collection instead of embedding in `MapEntry`

1. Every rating edit would rewrite the entry document and re-broadcast the **whole entry with all ratings** to all clients (`MapChange`), and bump `updatedAt`, which the info sheet renders as "last changed by X at Y".
2. Image ids per rating would ride along in every map sync page forever.
3. Concurrent raters would contend on one document that has no `@Version`.
4. A separate stream reuses the existing `IdTimeStamp` sync + broadcast + Room patterns 1:1 (see `schneaggchat-add-sync-endpoint` skill); ratings stay readable **offline**, averages come from one Room query.

---

## 2. Data model

### 2.1 Types (same names on both sides = JSON keys = Mongo fields = Room columns)

```kotlin
// server: ObjectId / Instant   |   client + wire: String / Long (epoch millis)
data class MapEntryRating(
    val id,
    val entryId,               // -> map_entries.id
    val userId,                // rater
    val stars: Int,            // 1..5, whole stars
    val text: String,          // "" when none, max 1000 chars
    val imageIds: List<String>,// 0..3, each a server-generated ObjectId hex, file = /app/images_map/{imageId}.png
    val createdAt,
    val updatedAt,
    val deleted: Boolean = false,   // server only
)

// wire response adds
val userName: String   // resolved server side like MapEntry.updatedByName (raters need not be friends)

// client only, derived from Room
data class RatingSummary(val entryId: String, val average: Double, val count: Int)
```

Rules:
- **One rating per user per entry** → server `@CompoundIndex(entryId, userId, unique = true)`. Soft delete + re-rate **reuses the same document** (undelete + overwrite) so the unique index never trips.
- Server owns all timestamps and image ids. Client never sends an image id it did not receive.
- `stars` is `Int` on the wire; the **average** is a client-side `Double` shown with half stars.

### 2.2 Server Mongo

`schneaggmap/model/MapEntryRating.kt`: `@Document("map_entry_ratings") @TypeAlias("mapentryrating")`, `@Indexed entryId`, compound unique index `entryid_userid_idx`. Repository `repository/MapEntryRatingRepository.kt : MongoRepository<MapEntryRating, ObjectId>` with `findByDeletedFalse()`, `findByEntryIdAndUserId(entryId, userId)`, `findByEntryIdAndDeletedFalse(entryId)`.

### 2.3 Client Room

`schneaggmap/data/dtos/MapEntryRatingDto.kt` → `@Entity(tableName = "map_entry_ratings", indices = [Index("entryId")])`, `imageIds: List<String>` via existing `stringListToString`. DB version **84 → 85** (destructive migration already configured, no migration script needed).

`MapEntryRatingDao` (in `Daos.kt`, next to `MapEntryDao`):

```kotlin
@Upsert suspend fun upsert(dto: MapEntryRatingDto)
@Query("DELETE FROM map_entry_ratings WHERE id = :id") suspend fun delete(id: String)
@Query("DELETE FROM map_entry_ratings WHERE entryId = :entryId") suspend fun deleteByEntryId(entryId: String)
@Query("DELETE FROM map_entry_ratings") suspend fun clear()             // add to the existing clearAll() logout path
@Query("SELECT id, updatedAt FROM map_entry_ratings") suspend fun getIdsWithChangeDates(): List<IdChangeDate>
@Query("SELECT * FROM map_entry_ratings WHERE entryId = :entryId ORDER BY updatedAt DESC") fun getForEntryFlow(entryId: String): Flow<List<MapEntryRatingDto>>
@Query("SELECT entryId, AVG(stars) AS average, COUNT(*) AS count FROM map_entry_ratings GROUP BY entryId") fun getSummariesFlow(): Flow<List<RatingSummary>>
@Query("SELECT * FROM map_entry_ratings WHERE userId = :userId ORDER BY updatedAt DESC") fun getByUserFlow(userId: String): Flow<List<MapEntryRatingDto>>  // for "my ratings" (phase 4)
```

---

## 3. Server

### 3.1 Endpoints (`SchneaggmapController`, all under `/map/ratings`, all `requireAuth()`)

| Method | Path | Body / parts | Returns | Notes |
|---|---|---|---|---|
| `POST` | `/map/ratings/upsert` (multipart) | part `request`: `MapRatingRequest(entryId, stars: Int @Min 1 @Max 5, text: String @Size(max 1000), keepImageIds: List<String>)`; part `images`: `List<MultipartFile>` optional | `MapRatingResponse` | Create or overwrite own rating. Images not in `keepImageIds` are deleted from disk; new parts appended; `keep + new ≤ 3` else 400 |
| `DELETE` | `/map/ratings/delete?entryid=` | - | `Unit` | Soft-delete own rating, delete its image files |
| `POST` | `/map/ratings/sync?page&page_size=400` | `List<IdTimeStamp>` | `MapRatingSyncResponse(updatedRatings, deletedRatings: List<String>, moreEntries)` | Copy of `mapSync`, `deletedRatings` only on page 0 |
| `GET` | `/map/ratings/images/{imageId}?thumb=false` | - | `ByteArray` `image/png` | `thumb=true` returns the ~256 px variant |

Validation: `validateObjectId` on `entryId`, `imageId`, each `keepImageIds`; `ValidationUtils.validatePicture` per part; `keepImageIds` must all belong to the caller's existing rating (otherwise 400 - never let a user "keep" someone else's image id). Entry must exist and not be deleted (404).

### 3.2 Service (`SchneaggmapService` or new `MapRatingService` in the same package)

- `upsertRating(requesterId, request, images)`: load `findByEntryIdAndUserId`; if present (even `deleted = true`) overwrite `stars/text/imageIds/updatedAt/deleted=false`, keep `createdAt`; else create. Then `notificationService.notifyMapRatingUpdate(saved, deleted = false, excludeUserId = requesterId)`. Log `LogType.MAP_RATING_CREATED / MAP_RATING_EDITED / MAP_RATING_DELETED` (add to `LoggingService`'s `LogType`).
- `deleteRating(requesterId, entryId)`: soft delete + `imageManager.deleteMapRatingImages(imageIds)` + notify.
- `ratingSync(...)`: same shape as `mapSync`, resolve `userName` per page with one `userLookupService.findAllById`.
- **Cascade**: `deleteMapEntry` additionally soft-deletes all `findByEntryIdAndDeletedFalse(entryId)` and removes their image files. No per-rating broadcast needed - clients drop local ratings when the entry's `MapChange(deleted = true)` arrives (see 4.4) and the next ratings sync confirms via `deletedRatings`.
- Only the owner ever writes a rating document → no optimistic-lock concerns.

### 3.3 Images (`util/ImageManager.kt`)

- New dir `/app/images_map`, new docker volume `app-images-map:/app/images_map` (compose `volumes:` in both places).
- `saveMapRatingImage(image: MultipartFile, imageId: ObjectId)`: `readUprightImage` → `downscaleIfNeeded` (700 KB cap) → `{imageId}.png`; additionally a thumbnail `{imageId}_thumb.png` scaled to 256 px longest edge (new small helper next to `downscaleIfNeeded`).
- `loadMapRatingImage(imageId, thumb)`, `deleteMapRatingImages(ids)`.
- Rating image access = any authenticated user (map data is global and `MapChange` is already broadcast to all). Path traversal impossible because the file name is built from a validated ObjectId, never from user input.

### 3.4 Socket push

`SocketConnectionMessage.MapRatingChange(rating: MapRatingResponse, deleted: Boolean)` with `@SerialName("mapratingchange")`; `NotificationService.notifyMapRatingUpdate` = copy of `notifyMapUpdate` (broadcast, exclude actor). No FCM/APNs (same as map entries). Follow `schneaggchat-add-realtime-push` skill.

### 3.5 After implementation

Run `schneaggchat-security-check` on the four new endpoints (ownership of `keepImageIds`, cross-user delete, image id validation).

---

## 4. Client data layer

### 4.1 Domain (`schneaggmap/domain/`)

`MapEntryRating.kt` (fields from 2.1 + `userName`), `RatingSummary.kt`. Constants: `MAX_RATING_IMAGES = 3`, `MAX_RATING_TEXT_LENGTH = 1000`, `RATING_STARS_RANGE = 1..5`.

### 4.2 Wire + mappers

`MapNetworkDtos.kt`: `MapRatingResponse`, `MapRatingSyncResponse`, `MapRatingRequest(entryId, stars, text, keepImageIds)`. `MapResponseMapper.kt`: `MapRatingResponse.toMapEntryRating()`, `MapEntryRating.toDto()`, `MapEntryRatingDto.toMapEntryRating()`, `RatingSummary` needs no mapper (Room projection class).

`NetworkUtils.kt` (Map section):

```kotlin
suspend fun mapRatingSync(entries: List<IdTimeStamp>, page: Int): NetworkResult<MapRatingSyncResponse, NetworkingError>
suspend fun upsertMapRating(request: MapRatingRequest, images: List<ByteArray>): NetworkResult<MapRatingResponse, NetworkingError>
    // safePostMultipart("/map/ratings/upsert") { append("request", json, ContentType.Application.Json); images.forEachIndexed { i, b -> appendImage("images", b, "rating_$i.jpg") } }
suspend fun deleteMapRating(entryId: String): NetworkResult<Unit, NetworkingError>
suspend fun getMapRatingImage(imageId: String, thumb: Boolean): NetworkResult<ByteArray, NetworkingError>
```

`appendImage(partName, bytes, fileName)` = generalisation of the existing `appendProfilePic` (keep the old one untouched, add the new helper beside it).

### 4.3 Repositories

Keep the existing split: **`MapRepository` = Room only, `AppRepository` = network + sync + writes into `MapRepository`** (same as map entries today).

`MapRepository` additions: `getRatingChangeIds()`, `upsertRating()`, `deleteRating(id)`, `deleteRatingsForEntry(entryId)`, `getRatingsForEntryFlow(entryId)`, `getRatingSummariesFlow()`, `getOwnRatingsFlow(userId)`. `deleteMapEntry(id)` also calls `deleteRatingsForEntry(id)`.

`AppRepository` additions:
- `mapRatingIdSync()` - clone of `mapEntryIdSync()`; called right after it inside the `MAP` sync job so the map's existing `dataSync(reason = "mapInit")` trigger covers both.
- `upsertMapRating(entryId, stars, text, keepImageIds, newImages: List<ByteArray>)` and `deleteMapRating(entryId)` - clone of the map CRUD pair (`sendErrorSuspend` on error, upsert/delete Room on success).
- `getMapRatingImagePath(imageId: String, thumb: Boolean): String?` - the **lazy loader**: `pictureManager.getMapRatingImageFilePath(imageId, thumb)`; if `checkImageExists` → return path; else download via `getMapRatingImage`, `savePictureToStorage(bytes, fileName)`, return path; `null` on error (logged with `loggingRepository.logWarning`). No Room column needed - the file name is derived from the id, exactly like profile pictures.
- `clearAll()`/logout path: clear the new table.

`PictureManager` (expect + 3 actuals: `androidMain`, `iosMain`, `jvmMain`): `fun getMapRatingImageFilePath(imageId: String, thumb: Boolean): String` using a new constant `MAP_RATING_IMAGE_FILE_NAME = "_maprating.drawable"` in `constants.kt` (thumb → `"${imageId}_thumb$MAP_RATING_IMAGE_FILE_NAME"`). Same directory as message images. No eviction (message images have none either); note as future work.

### 4.4 Socket

`SocketConnectionMessage.MapRatingChange` + handler next to `MapChange` (:420): `deleted` → `mapRepository.deleteRating(id)` else `upsertRating(toMapEntryRating())`. `MapChange(deleted = true)` already routes through `mapRepository.deleteMapEntry`, which now cascades.

---

## 5. Client UI

### 5.1 Shared composable `sharedUi/rating/StarRatingBar.kt` (new, reusable)

```kotlin
@Composable fun StarRatingBar(
    rating: Float,                         // 0f..5f, half stars rendered for display
    modifier: Modifier = Modifier,
    onRatingChange: ((Int) -> Unit)? = null,   // null = read-only indicator
    starSize: Dp = 20.dp,
    contentDescription: String? = null,
)
```
Icons from `material-icons-extended` (already a dependency): `Icons.Filled.Star`, `Icons.AutoMirrored.Filled.StarHalf`, `Icons.Outlined.StarOutline`. Tint `MaterialTheme.colorScheme.primary` (filled) / `onSurfaceVariant` (empty) - no hardcoded colors. Interactive mode: tap a star = whole value 1..5.

### 5.2 Info sheet (`MapEntryInfoCard.kt`)

- New params: `ratingSummary: RatingSummary?`, `onOpenRatings: (entryId: String) -> Unit`.
- Replace the `//TODO: Small row with ratings` in `EntryTitleView` with `RatingSummaryRow`: read-only `StarRatingBar(average)` + `"4.2 · 7 ratings"` (or "No ratings yet"), whole row clickable → `onOpenRatings`. Hidden while `entry.id.isEmpty()` (unsaved new entry).
- `SchneaggmapState` gains `ratingSummaries: Map<String, RatingSummary>` (combined from `mapRepository.getRatingSummariesFlow()` in the existing `combine`). `SchneaggmapAction.OnOpenRatings(entryId)` → `navigator.navigate(Route.MapEntryRatings(entryId))` and clears `selectedEntry`.

### 5.3 Ratings screen `schneaggmap/presentation/ratings/` (MVI per `android-presentation-mvi`)

Files: `MapEntryRatingsState.kt`, `MapEntryRatingsAction.kt`, `MapEntryRatingsViewModel.kt`, `MapEntryRatingsScreen.kt` (`Root` + `Screen` split), `uielements/RatingListItem.kt`, `uielements/OwnRatingEditor.kt`, `uielements/RatingImageThumbnail.kt`.

State:
```kotlin
data class MapEntryRatingsState(
    val entry: MapEntry? = null,
    val summary: RatingSummary? = null,
    val ownRating: MapEntryRating? = null,
    val otherRatings: List<MapEntryRating> = emptyList(),   // newest first, own excluded (V2 behaviour)
    // editor draft
    val isEditing: Boolean = false,          // true when no own rating yet, or after "edit"
    val draftStars: Int = 0,
    val draftText: String = "",
    val draftKeepImageIds: List<String> = emptyList(),
    val draftNewImages: List<ByteArray> = emptyList(),      // already downscaled, previewed via AsyncImage(model = bytes)
    val isSaving: Boolean = false,
    // lazy images
    val imagePaths: Map<String, String> = emptyMap(),       // imageId -> local thumb path, filled on demand
    val fullScreenImagePath: String? = null,
    val error: UiText? = null,
)
```

Actions: `OnBack`, `OnStartEdit`, `OnDraftStars(Int)`, `OnDraftText(String)`, `OnImagePicked(ByteArray)`, `OnRemoveNewImage(index)`, `OnRemoveKeptImage(imageId)`, `OnSave`, `OnDelete`, `OnCancelEdit`, `OnRatingImageVisible(imageId)`, `OnRatingImageClick(imageId)`, `OnDismissFullScreen`.

ViewModel: `combine(mapRepository.getAllMapEntriesFlow().map { it.firstOrNull { e -> e.id == entryId } }, mapRepository.getRatingsForEntryFlow(entryId), SessionCache.authState)` → splits own / others, computes summary from the list. Constructor params `entryId: String` via Koin lambda (same style as `SchneaggmapViewModel`). Image visibility handler keeps an in-flight `MutableSet<String>` so a thumb is fetched once; result goes into `imagePaths`. `OnRatingImageClick` fetches the full-size variant (`thumb = false`) then sets `fullScreenImagePath`.

Screen layout (`Scaffold` + `TopAppBar` with entry name, back arrow):
1. Header: big read-only `StarRatingBar(average)` + count text.
2. Own rating card: either the editor (`StarRatingBar` interactive, `OutlinedTextField` multiline with counter, image row = kept thumbs + new previews + "+" button while `< 3`, Save / Cancel) or the read-only own rating with Edit / Delete (delete behind the same `AlertDialog` confirm pattern as entry delete).
3. `LazyColumn` of `RatingListItem`: `ProfilePictureView(pictureManager.getProfilePicFilePath(userId, false))` with placeholder (non-friends have no local picture, placeholder is expected), `userName`, stars, date (`millisToTimeDateOrYesterday`), text, horizontal row of `RatingImageThumbnail`.
4. `RatingImageThumbnail(imageId, path, onVisible, onClick)`: `LaunchedEffect(imageId) { onVisible(imageId) }` - because it lives in a `LazyColumn` item this fires only when the row is composed, i.e. **when the ratings are opened and scrolled to**; shows a `MaterialTheme.colorScheme.surfaceVariant` box until `path != null`, then `AsyncImage(model = path)`. Tap → `FullScreenImageDialog` (reuse from chat).
5. Image picking: `rememberImagePickerKMP` exactly as `GroupCreator.kt:230` (gallery only, `CompressionLevel.LOW`), `loadBytes()` → `pictureManager.downscaleImage(bytes, 500_000)` → `OnImagePicked`.

Save validation (mirrors server): `draftStars in 1..5` else inline error, text trimmed ≤ 1000, images ≤ 3. Optimistic UI is **not** needed - the HTTP response upserts Room and the flow re-renders (same as map entries).

### 5.4 Navigation + DI

- `Route.MapEntryRatings(entryId: String)` in `Route.kt`; `entry<Route.MapEntryRatings>` in `App.kt` next to `Route.SchneaggmapSettings` (:670), inside the map tab so back returns to the map.
- `di/Modules.kt`: `viewModel { (entryId: String) -> MapEntryRatingsViewModel(entryId = entryId, navigator = get(), mapRepository = get(), appRepository = get(), pictureManager = get()) }`.

### 5.5 Strings (`values/strings.xml` + `values-de` + `values-it`; never `values-de-rAT`)

`ratings_title`, `ratings_none_yet`, `ratings_summary` (`%1$s · %2$d ratings`), `ratings_your_rating`, `ratings_write_rating`, `ratings_text_hint` ("Your comment (optional)"), `ratings_add_image`, `ratings_max_images` ("Max. %1$d images"), `ratings_select_stars_error`, `ratings_delete_confirm`, `ratings_edit`, `ratings_image_content_description`, `ratings_saving`. Run the `string-resource-extractor` agent afterwards to catch stragglers.

---

## 6. Phases / order of work

| # | Phase | Repo | Deliverable |
|---|---|---|---|
| 1 | Server data + endpoints | server | Model, repository, service, controller (4 endpoints), `ImageManager` map dir + thumbnail, docker volume, cascade on entry delete, `LogType`s, socket message + `notifyMapRatingUpdate`, security-check pass |
| 2 | Client data | client | Domain, Room entity/DAO/version 85, wire DTOs + mappers, `NetworkUtils`, `MapRepository`, `AppRepository` sync/CRUD/lazy image loader, `PictureManager` path fn (3 actuals), socket handler, logout clear |
| 3 | Client UI | client | `StarRatingBar`, summary row in `MapEntryInfoCard`, ratings screen (state/action/VM/screen + 3 uielements), route + `App.kt` entry + Koin, strings en/de/it |
| 4 | Optional follow-ups | both | "My ratings" list in map settings (V2 parity, `getOwnRatingsFlow` already planned); average badge on map markers; local image cache eviction; FCM for ratings on own entries |
| post | Agents | client | `readme-changelog-updater`, then `string-resource-extractor` |

Phase 2 can start against the server contract before phase 1 is deployed (DTOs are fixed above); phase 3 depends on 2. Nothing in phases 1-3 touches `MapEntry`, `MapChange` or the existing map sync.

---

## 7. Open decisions (defaults assumed above)

1. **One rating per user per entry** (V2 semantics, edit in place) - vs. allowing several comments per user. Default: one.
2. **Dedicated ratings screen** (room for images + lazy list) - vs. an expandable section inside the existing `MapEntryInfoCard` sheet, which is already an edit form with its own Save/Cancel diffing. Default: dedicated screen.
3. **Max 3 images per rating**, 700 KB full + 256 px thumbnail server side, thumb in the list and full only on tap. Default: yes to thumbnails (otherwise every visible rating pulls up to 3 × 700 KB).
4. **Text optional, stars mandatory** (V2). Default: yes.
5. **Visibility = every logged-in user**, same as map entries today. Default: yes.
6. **Whole-star input, half-star average display** (V2). Default: yes.
7. "My ratings" in map settings and average on the marker icon: **phase 4**, not part of the first implementation.
