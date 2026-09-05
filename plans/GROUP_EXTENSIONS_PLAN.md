# Group Extensions Plan

Companion docs: `E2E_ENCRYPTION_PLAN_V2.md`, `OFFLINE_FIRST_ANALYSIS.md` (same folder). Both repos: client `SchneaggchatV3` (KMP) and server `SchneaggchatV3server` (Spring Boot + Mongo).

**Goal**: a group can own any number of "extensions" (shared text, split/tricount, quotes, route planner, ...) that are created from the group details screen, synced to every member, updated live over the socket, and stored in Room. Adding a new extension type must never require a new endpoint, a new sync job, a new socket message, or a new Room table.

**Core idea**: one generic container (`GroupExtension`) + one generic entry (`GroupExtensionItem`) whose payload is a polymorphic sealed class (`ExtensionItemData`), exactly the way `MapEntry.locationData: List<LocationData>` already works. The wire, Mongo, and Room shapes are all fixed; only the sealed payload grows per extension type.

---

## 1. Current state - how things are synced today (verified 2026-09-05)

### 1.1 Four synced collections, two sync shapes

| Collection | Client entity | Server model | Sync shape | Live push | Scope |
|---|---|---|---|---|---|
| map entries | `MapEntryDto` (`map_entries`) | `MapEntry` (`map_entries`) | **IdTimeStamp**: client POSTs `List<IdTimeStamp(id, updatedAt)>`, server diffs by `updatedAt` (`SchneaggmapService.mapSync`) | `MapChange` broadcast to everyone | global |
| events | `EventDto` (`events`) | `Event` (`events`) | IdTimeStamp, filtered by visibility (`EventService.eventIdSync`) | `EventChange` targeted to invited + friends | per user |
| groups | `GroupDto` + `GroupMemberDto` | `Group` + `GroupMember` | IdTimeStamp (`/groups/sync`) | `GroupChange` targeted to members | per user |
| messages | `MessageDto` (has `version`) | `Message` (has `version`) | **Version watermark**: `GET /messages/sync?since=` bounded by `VersionCounterService.safeWatermark`, race-safe, deletions on every page | `MessageChange` targeted | per user + group membership |

Only `SyncCollection.MESSAGES` exists in the counter enum today; the comment there already reserves `USERS/GROUPS/EVENTS/MAP` for a later migration.

### 1.2 The `LocationData` pattern (the template for extension payloads)

- Client: `@Serializable sealed class LocationData` with `@SerialName("radar")`-style discriminators, stored in Room through `RoomTypeConverters.locationDataListToString` (JSON in a TEXT column), and sent over the wire unchanged inside `MapEntryRequest` / `MapEntryResponse`.
- Server: identical sealed class with Jackson `@JsonTypeInfo(property = "_class")` + `@JsonSubTypes`, persisted in Mongo through `LocationDataWriteConverter`/`LocationDataReadConverter` (`core/MongoConfig.kt`), which serialize with `Json.mapper` and stamp `_class` from `@TypeAlias`.
- Both sides carry a "HOW TO ADD A NEW LOCATION ENTRY" checklist in the file header, and the compiler forces exhaustiveness (`getValueByKey`, `labelRes`, `stringRes`).
- Server validation is generic: `schema(): List<AttributeDefinition>` + `SchneaggmapService.validate()`.

The discriminator names and property names must be identical on both sides because they are simultaneously the JSON keys, the Mongo field names, and the Room JSON.

### 1.3 Client sync plumbing

- `AppRepository.dataSync()` runs one `async` job per `DataSyncJobType` (`USERS, GROUPS, MESSAGES, MAP, EVENTS, MEDIA`) and reports progress through `DataSyncState`.
- Each feature has a thin Room-only repository (`MapRepository`, `EventRepository`) exposing `getXChangeIds()`, `upsertX()`, `deleteX()`, `getAllXFlow()`; the network calls live in `NetworkUtils`, orchestration in `AppRepository`.
- Socket handling is one `when` in `SocketConnectionMessage.kt:172-468`; `MapChange`/`EventChange` just upsert/delete the local row.
- Group removal purge: `GroupRepository.deleteGroup` deletes messages + readers for the group, and `dataSync` repeats the purge after all jobs finished (`AppRepository.kt:542`) because the message job runs in parallel.
- Room DB is at version 82 with `fallbackToDestructiveMigration(dropAllTables = true)`; a schema change is a version bump + full re-sync.

### 1.4 Server plumbing to reuse

- `requireAuth()`, `ValidationUtils.*`, `withOptimisticRetry {}`, `LoggingService.log(userId, LogType)`.
- `GroupLookupService.isUserInGroup(userId, groupId)`, `getUserGroupIds(userId)`, `getGroupMembers(groupId)`, `isAdmin(userId, members)`, `getGroupIdsJoinedAfterVersion(userId, version)`.
- `VersionCounterService.withVersion / withVersions / safeWatermark` and the `GroupMember.joinedAtVersion` trick that lets a member who joins an existing group pull that group's full history although their `since` is already high (`MessageLookupService.getMessagesSince`).
- `NotificationService` is the only fan-out point; `notifyGroupUpdate` is the "targeted to all members" shape.

### 1.5 Consequences for the design

1. Extensions are **per-group** data with a potentially high write rate (shared text edits, expenses). That is the messages profile, not the map profile, so use the **version-watermark sync**, not IdTimeStamp.
2. The payload must be **polymorphic** on both sides, so copy the `LocationData` mechanics one-to-one (sealed class + `_class` discriminator + Mongo converters + Room type converter).
3. "Unified endpoints" is achievable because the container/item split covers all four planned extensions (see §2.2), so new types only add a payload subclass.

---

## 2. Data model

### 2.1 Two generic entities

```
GroupExtension (container, one per extension instance in a group)
  id            ObjectId / String
  groupId       ObjectId / String        - owner group
  type          GroupExtensionType       - SHARED_TEXT | SPLIT | QUOTES | ROUTE_PLANNER
  title         String                   - user-chosen name ("Italien 2026", "Kassa")
  settings      ExtensionSettings        - polymorphic, per-type config (nullable/None)
  createdBy, createdAt, updatedBy, updatedAt
  deleted       Boolean (soft delete)
  version       Long                     - SyncCollection.GROUP_EXTENSIONS counter

GroupExtensionItem (entry inside a container)
  id            ObjectId / String
  extensionId   ObjectId / String
  groupId       ObjectId / String        - denormalized copy for sync/purge queries, set by server from the container, never trusted from the client
  type          GroupExtensionType       - denormalized copy of the container type
  data          ExtensionItemData        - polymorphic payload (the actual content)
  createdBy, createdAt, updatedBy, updatedAt
  deleted       Boolean
  version       Long                     - same counter as the container
```

Both entities share **one** version counter (`SyncCollection.GROUP_EXTENSIONS`) so a single `since` watermark covers containers and items together.

### 2.2 Payload sealed class - the only thing that grows per extension

```kotlin
enum class GroupExtensionType { SHARED_TEXT, SPLIT, QUOTES, ROUTE_PLANNER }

@Serializable
sealed class ExtensionItemData {
    abstract val extensionType: GroupExtensionType

    /** SHARED_TEXT: exactly one item per container holds the whole document. */
    @Serializable @SerialName("shared_text")
    data class SharedText(val text: String) : ExtensionItemData()

    /** SPLIT: one item per expense or settlement transfer. Amounts in minor units (cents). */
    @Serializable @SerialName("split_expense")
    data class SplitExpense(
        val title: String,
        val amountCents: Long,
        val currency: String,                 // ISO 4217, default from ExtensionSettings.Split
        val paidBy: String,                   // userId
        val participants: List<String>,       // userIds sharing the cost
        val weights: Map<String, Int>? = null,// optional custom split, null = equal
        val date: Long,
        val isSettlement: Boolean = false,    // true = "A paid B back", participants = [B]
        val category: String? = null,
    ) : ExtensionItemData()

    /** QUOTES: one item per quote. */
    @Serializable @SerialName("quote")
    data class Quote(
        val text: String,
        val saidByUserId: String? = null,     // group member, or
        val saidByName: String? = null,       // free text for non-members
        val saidAt: Long? = null,
        val context: String? = null,
    ) : ExtensionItemData()

    /** ROUTE_PLANNER: one item per point; the route is the items ordered by sortIndex. */
    @Serializable @SerialName("route_point")
    data class RoutePoint(
        val coordinates: LatLong,
        val name: String,
        val description: String = "",
        val sortIndex: Int,
        val kind: RoutePointKind = RoutePointKind.STOP,   // STOP | WAYPOINT | ACCOMMODATION | POI
        val plannedAt: Long? = null,          // optional day/time
        val mapEntryId: String? = null,       // link to a schneaggmap entry, if picked from the map
    ) : ExtensionItemData()
}

@Serializable
sealed class ExtensionSettings {
    @Serializable @SerialName("none")  data object None : ExtensionSettings()
    @Serializable @SerialName("split") data class Split(val defaultCurrency: String = "EUR") : ExtensionSettings()
    @Serializable @SerialName("route") data class Route(val transportMode: String? = null, val startDate: Long? = null) : ExtensionSettings()
}
```

Rules:
- Discriminators are snake_case and identical on client (`@SerialName`) and server (`@JsonSubTypes` name + `@TypeAlias`).
- `extensionType` is a property with a fixed value per subclass (like `LocationData.locationtype`); the server rejects an item whose `data.extensionType != container.type`.
- `LatLong` is reused from `schneaggmap.domain` (client) / `schneaggmap.model` (server).
- Derived values (split balances, route distance) are **never stored**; they are pure functions over the item list (§5.3).

### 2.3 Why container + item covers all four extensions

| Extension | Container | Items | Notes |
|---|---|---|---|
| Shared text | title | exactly 1 `SharedText` | server enforces "at most one item" for `SHARED_TEXT`; last write wins, see §3.4 for the optional conflict guard |
| Split | title + `Settings.Split` | n `SplitExpense` | balances + settlement suggestions computed client side |
| Quotes | title | n `Quote` | plain list, newest first |
| Route planner | title + `Settings.Route` | n `RoutePoint` | reorder = batch upsert of changed `sortIndex` values |

---

## 3. Server design (`SchneaggchatV3server`)

Follow `schneaggchat-add-feature` → `schneaggchat-add-realtime-push` → `schneaggchat-add-sync-endpoint`, then run `schneaggchat-security-check`.

### 3.1 Package layout

```
groupextensions/
  GroupExtensionController.kt
  GroupExtensionService.kt          writes + notifications
  GroupExtensionLookupService.kt    reads used by sync / other services
  ExtensionItemData.kt              sealed payload + @JsonSubTypes (+ HOW TO ADD header)
  ExtensionItemValidator.kt         per-type validation (exhaustive when)
  model/
    GroupExtension.kt               @Document("group_extensions"), @TypeAlias("groupextension")
    GroupExtensionItem.kt           @Document("group_extension_items"), @TypeAlias("groupextensionitem")
    ExtensionSettings.kt
    GroupExtensionResponse.kt       + toResponse()
    GroupExtensionItemResponse.kt   + toResponse()
    GroupExtensionSyncResponse.kt
repository/
  GroupExtensionRepository.kt
  GroupExtensionItemRepository.kt
```

Indexes: `group_extensions {groupId:1, version:1}`, `group_extension_items {groupId:1, version:1}`, `{extensionId:1, deleted:1}`.

`core/MongoConfig.kt`: add `ExtensionItemDataWriteConverter/ReadConverter` and `ExtensionSettingsWriteConverter/ReadConverter`, copied from the `LocationData` pair (same `Json.mapper` + `@TypeAlias` → `_class` trick). Do not generalize the existing converter in this change.

`util/SyncCollection.kt`: add `GROUP_EXTENSIONS("groupextensions")`.

`util/LoggingService.kt`: add `LogType.GROUP_EXTENSION_CREATED / _EDITED / _DELETED, GROUP_EXTENSION_ITEM_CREATED / _EDITED / _DELETED`.

### 3.2 Endpoints (fixed forever - new extension types add nothing here)

| Method | Path | Body / params | Returns | Rules |
|---|---|---|---|---|
| `POST` | `/groupextensions/upsert` | `ExtensionRequest(extensionId?, groupId, type, title, settings)` | `GroupExtensionResponse` | create when `extensionId == null`. Requester must be a member of `groupId`. On update the `groupId`/`type` in the request must equal the stored ones (400 otherwise). |
| `DELETE` | `/groupextensions/delete?extensionid=` | - | - | soft delete; allowed for the extension creator or a group admin. Cascades: every non-deleted item of the container is soft-deleted, all stamped with fresh versions (`withVersions(n + 1)`). |
| `POST` | `/groupextensions/items/upsert` | `List<ItemRequest(itemId?, extensionId, data)>` (1..100) | `List<GroupExtensionItemResponse>` | batch on purpose (route reorder, future outbox drain). All items must belong to the same container; requester must be a member of the container's group; each `data.extensionType` must equal the container type; `SHARED_TEXT` rejects a second item (409). Versions via `withVersions(count)`. |
| `DELETE` | `/groupextensions/items/delete?itemid=` | - | - | soft delete; item creator, extension creator or group admin. |
| `GET` | `/groupextensions/sync?since=&page_size=400` | - | `GroupExtensionSyncResponse` | version-watermark sync, §3.3 |

Request DTOs are nested data classes in the controller with `jakarta.validation` annotations; ids validated with `ValidationUtils.validateObjectId`. Item payload limits live in `ExtensionItemValidator` (§3.5).

The service derives `groupId` and `type` for items from the container - the client never sends them for items.

### 3.3 Sync (`GroupExtensionSyncResponse`)

```kotlin
data class GroupExtensionSyncResponse(
    val updatedExtensions: List<GroupExtensionResponse>,
    val deletedExtensions: List<String>,
    val updatedItems: List<GroupExtensionItemResponse>,
    val deletedItems: List<String>,
    val newVersion: Long,
    val moreEntries: Boolean,
)
```

Algorithm (mirrors `MessageService.messageSync` + `MessageLookupService.getMessagesSince`):

1. `watermark = versionCounterService.safeWatermark(GROUP_EXTENSIONS)`.
2. `userGroups = groupLookupService.getUserGroupIds(requester)`; `fullHistoryGroups = groupLookupService.getGroupIdsJoinedAfterExtensionVersion(requester, since)` (new, §3.6).
3. Query both collections with `groupId in userGroups` and (`version > since && version <= watermark`) OR (`groupId in fullHistoryGroups && version <= watermark`), sorted by `version` asc, `limit(pageSize + 1)` each.
4. Merge the two lists by `version` (versions are unique across both collections because they share the counter), take `pageSize`, `moreEntries = merged.size > pageSize`, `newVersion = last taken version ?: since`.
5. Partition each list by `deleted` into updated/deleted.

Deleted rows are reported on every page (they carry versions), so a client can never miss a deletion. Membership is re-evaluated on every call: a removed member simply stops receiving that group's rows; the local purge happens on the client on group removal (§4.5).

### 3.4 Write path details

- Every create/update/delete goes through `withOptimisticRetry {}` + `versionCounterService.withVersion(GROUP_EXTENSIONS) { v -> repo.save(entity.copy(version = v, updatedAt = now, updatedBy = requester)) }`.
- `SHARED_TEXT` conflict guard (v1.1, optional): `ItemRequest.expectedUpdatedAt: Long?`; when set and the stored `updatedAt` differs → `409 Conflict`. The client already maps 409 to `NetworkingError.Conflict`, so it can reload and re-apply the edit. v1 ships last-write-wins.
- After every successful write call `notificationService.notifyGroupExtensionUpdate(...)` (§3.7). The writer is **not** excluded (mirrors `notifyGroupUpdate`, and keeps a second device of the same user in sync); the HTTP response also carries the row, so the local upsert is idempotent.

### 3.5 Validation (`ExtensionItemValidator`)

Exhaustive `when (data)`; new subclasses do not compile until they get a branch:

| Payload | Rules |
|---|---|
| `SharedText` | `text.length <= 20_000` |
| `SplitExpense` | `title` 1..100, `amountCents in 0..100_000_000`, `currency` matches `[A-Z]{3}`, `paidBy` and every `participants` entry must be a current group member, `participants` non-empty and distinct, `weights` keys ⊆ participants and values > 0, `isSettlement` → exactly one participant |
| `Quote` | `text` 1..1_000, `saidByUserId` must be a member when set, `saidByName` ≤ 50, at least one of the two set |
| `RoutePoint` | `ValidationUtils.validateLatLong`, `name` 1..100, `description` ≤ 500, `sortIndex >= 0`, `mapEntryId` must be a valid ObjectId when set |

Container: `title` 1..100, `type` must be a known enum value, `settings` subclass must match `type` (`Split` only for `SPLIT`, `Route` only for `ROUTE_PLANNER`, else `None`).

### 3.6 Group membership joins (`GroupMember.joinedAtExtensionVersion`)

`GroupMember.joinedAtVersion` stores the **MESSAGES** counter and is compared against the messages `since`, so it cannot be reused. Add `joinedAtExtensionVersion: Long = 0` next to it, stamp it in both places where `joinedAtVersion` is stamped (`GroupService.kt:100/110` create, `GroupService.kt:501` add member) with `versionCounterService.current(GROUP_EXTENSIONS)`, add `GroupMemberRepository.findByUseridAndJoinedAtExtensionVersionGreaterThanEqual` and `GroupLookupService.getGroupIdsJoinedAfterExtensionVersion`. Default 0 is correct for pre-existing members: the feature is new, so no client has a `since` above 0 yet.

### 3.7 Realtime push

`SocketConnectionMessage`: add two subtypes (sealed interface **and** `@JsonSubTypes`, gotcha #5):

```kotlin
data class GroupExtensionChange(val extension: GroupExtensionResponse, val deleted: Boolean) : SocketConnectionMessage       // "groupextensionchange"
data class GroupExtensionItemChange(val item: GroupExtensionItemResponse, val deleted: Boolean) : SocketConnectionMessage   // "groupextensionitemchange"
```

`NotificationService.notifyGroupExtensionUpdate(extension, deleted)` and `notifyGroupExtensionItemUpdate(item, deleted)`: targeted shape, one `sendMessage` per current member of `groupId` (same loop as `notifyGroupUpdate`). No FCM/APNs fallback in v1 - offline clients catch up via `/groupextensions/sync` on the next `dataSync`. A push for "new quote about you" / "new expense" is a later opt-in.

### 3.8 Group lifecycle

- `GroupService.deleteGroup` and the expiry cleanup: additionally soft-delete the group's extensions + items with fresh versions so still-connected members receive the deletions before their `GroupChange(deleted)` purge; the client purge (§4.5) makes this belt-and-braces.
- Member removal needs no extension write: the sync filter and the targeted push are membership-based.

---

## 4. Client design (`SchneaggchatV3`)

Follow `android-data-layer`, `android-presentation-mvi`, `android-navigation`, `android-compose-ui`, `android-di-koin`. All new code in English.

### 4.1 Package layout

```
groupextensions/
  domain/
    GroupExtensionType.kt          enum + stringRes() + drawableRes()/icon (exhaustive when)
    ExtensionItemData.kt           sealed payload (+ HOW TO ADD header, mirrors LocationData.kt)
    ExtensionSettings.kt
    GroupExtension.kt              domain container
    GroupExtensionItem.kt          domain item
    split/SplitCalculator.kt       computeBalances(), suggestSettlements()  (pure)
    route/RouteMetrics.kt          totalDistanceMeters() (pure, sums utilities/MapUtils.distanceMeters over consecutive points; formatDistance exists there too)
  data/
    dtos/GroupExtensionDto.kt      @Entity("group_extensions")
    dtos/GroupExtensionItemDto.kt  @Entity("group_extension_items"), indices on extensionId and groupId
    GroupExtensionRepository.kt    Room-only, same shape as EventRepository
  presentation/
    ExtensionListSection.kt        composable used inside ChatDetails (list + "add" button)
    AddExtensionDialog.kt          type picker + title
    GroupExtensionScreenRoot.kt    route entry: loads container, dispatches on type
    sharedtext/  SharedTextScreen + ViewModel + State + Action
    split/       SplitScreen (+ AddExpenseSheet) + ViewModel + State + Action
    quotes/      QuotesScreen + ViewModel + State + Action
    routeplanner/RoutePlannerScreen (maplibre) + ViewModel + State + Action
datasource/network/requestResponseDataClasses/
    GroupExtensionNetworkDtos.kt   ExtensionRequest, ItemRequest, GroupExtensionResponse, GroupExtensionItemResponse, GroupExtensionSyncResponse
    GroupExtensionResponseMapper.kt toGroupExtension()/toDto()/toItem() (same trio as MapResponseMapper)
```

### 4.2 Room

- `Database.kt`: add both entities to `@Database(entities = [...])`, bump `version` 82 → 83 (destructive migration wipes the DB and re-syncs, consistent with current practice; state it in the changelog).
- `RoomTypeConverters`: add `extensionItemDataToString/stringToExtensionItemData` and the `ExtensionSettings` pair, decoding tolerant (`runCatching`) so an unknown future discriminator from a newer server yields `null` and the row is skipped instead of crashing the list. Also `GroupExtensionType` name converter with a safe default.
- `Daos.kt` → `GroupExtensionDao`:
  - `upsertExtension`, `upsertItems(List)`, `deleteExtension(id)` (also deletes its items), `deleteItem(id)`
  - `getExtensionsForGroupFlow(groupId)`, `getExtensionFlow(id)`, `getItemsForExtensionFlow(extensionId)` (ordered by `createdAt`; screens re-sort as needed)
  - `getLastSyncedVersion(): Long` = `MAX(COALESCE(MAX(version) over extensions, MAX(version) over items), 0)`
  - `deleteAllForGroup(groupId)` (both tables), `clearAll` entries in `AllDatabaseDao`
- Sync watermark lives in the rows (`version` column) exactly like messages, so no extra preference key is needed.

### 4.3 Network + sync

- `NetworkUtils`: `groupExtensionSync(since)`, `upsertGroupExtension(request)`, `deleteGroupExtension(id)`, `upsertGroupExtensionItems(list)`, `deleteGroupExtensionItem(id)` - all through the existing `safeGet/safePost/safeDelete`.
- `AppRepository`:
  - `DataSyncJobType.GROUP_EXTENSIONS` + default `DataSyncJobState` entry + a new `async` job in `dataSync` calling `groupExtensionSync()`.
  - `groupExtensionSync()` copies the `messageIdSync` loop: `since = dao.getLastSyncedVersion()`, apply deletions first, then upserts sorted by version, `since = response.newVersion`, loop while `moreEntries`.
  - CRUD wrappers `upsertGroupExtension(...)`, `deleteGroupExtension(id)`, `upsertGroupExtensionItems(...)`, `deleteGroupExtensionItem(id)` following the `upsertEvent` shape: network first, on success upsert/delete locally, on error `sendErrorSuspend(ErrorChannel.ErrorEvent(error))`.
- `SocketConnectionMessage.kt`: add `GroupExtensionChange` / `GroupExtensionItemChange` (`@SerialName` must match the server names) and two `when` branches that upsert/delete locally.

### 4.4 Navigation + DI

- `Route.GroupExtension(extensionId: String)` in `Route.kt`; register in `NavigationState` (`chatRoutes` so the nav bar hides, plus the `subclass(...)` serializer line) and an `entry<Route.GroupExtension>` in `App.kt` next to `ChatDetails` that calls `GroupExtensionScreenRoot(extensionId)`.
- `GroupExtensionScreenRoot` observes the container and switches on `type` to the concrete screen; each concrete screen gets its own ViewModel (`viewModel { (extensionId: String) -> ... }` in `Modules.kt`, explicit lambda like the event ones).
- `Modules.kt`: `singleOf(::GroupExtensionRepository)` + the four ViewModels.

### 4.5 Group details integration

- `ChatDetailsViewmodel`: `val groupExtensions: StateFlow<List<GroupExtension>>` from `groupExtensionRepository.getExtensionsForGroupFlow(chatId)` (only when `isGroup`), `addExtension(type, title)`, `deleteExtension(id)`, `openExtension(id)` → `navigator.navigate(Route.GroupExtension(id))`.
- `ChatDetails.kt`: new section for groups placed after the connected-event block (`ChatDetails.kt:458-506`) and before the expiry block: `HorizontalDivider()`, section header, one `ListItem` per extension (type icon, title, supporting text = item count or "last edited by X"), trailing delete for creator/admin, and an "Add extension" `ListItem` opening `AddExtensionDialog`.
- Purge on group loss: `GroupRepository.deleteGroup` additionally calls `groupExtensionDao.deleteAllForGroup(groupId)`, and the post-`awaitAll` purge loop in `dataSync` (`AppRepository.kt:542`) also purges extensions for `removedGroupIds` (the extension job runs in parallel with the group job, same race as messages).

### 4.6 Presentation per extension (v1 scope)

- **Shared text**: full-screen `TextField`, debounced save (~1 s after last keystroke) via item upsert, "last edited by X at T" footer, incoming socket changes replace the text only when the local field is not dirty (otherwise show a "newer version available" banner).
- **Split**: expense list, add/edit bottom sheet (title, amount, payer, participants defaulting to all members, date), balances card (`SplitCalculator.computeBalances`), "settle up" suggestions (`suggestSettlements`, greedy min-cash-flow) that create `isSettlement = true` items.
- **Quotes**: list newest first, add dialog (text, said-by picker of members or free text, date), long-press delete for allowed users.
- **Route planner**: maplibre map (`MapOptions`, `rememberCameraState`, `SymbolLayer` markers via the existing `MarkerBitmap`, `LineLayer` polyline through points ordered by `sortIndex` - the same layers `SchneaggmapScreen` already uses), tap-to-add point, bottom list with drag reorder (batch upsert of changed `sortIndex`), total distance from `RouteMetrics`. Picking an existing schneaggmap entry (`mapEntryId`) can reuse `Route.Schneaggmap(initialEntryId)` for viewing; a generic "pick location" return flow is a follow-up (today's picker is event-specific via `currentlyEditedEvent`).

### 4.7 Strings

New keys in `values/strings.xml` + `values-de` + `values-it` (never `values-de-rAT`): `group_extensions_title`, `group_extension_add`, `group_extension_type_shared_text/split/quotes/route_planner`, per-screen labels (`split_add_expense`, `split_balances`, `split_settle_up`, `quotes_add`, `route_add_point`, ...), and the confirmation `confirm_delete_extension`. Type names must go through `GroupExtensionType.stringRes()`.

---

## 5. Cross-cutting rules

### 5.1 Permissions (enforced server side, mirrored in UI)

| Action | Allowed |
|---|---|
| create extension / create or edit any item | any current group member |
| delete extension | extension creator or group admin |
| delete item | item creator, extension creator or group admin |
| read / sync | current members only (membership re-checked per request) |

### 5.2 Offline behaviour

Same as map/events today: writes are network-first and fail visibly through `ErrorChannel`; reads are Room-flow driven and fully offline. The batch item endpoint is intentionally shaped so a later outbox (see `OFFLINE_FIRST_ANALYSIS.md` §outbox) can drain queued item upserts in one call. Client-generated item ids (like `clientMessageId`) are the prerequisite for that and are out of scope for v1.

### 5.3 Derived data stays derived

Balances, settlement suggestions, route distance and "item count" are computed from items on the client. Nothing derived is written to the server, so there is nothing to keep consistent.

### 5.4 Security check list (run `schneaggchat-security-check` after implementation)

- Every handler starts with `requireAuth()` and resolves the group through the stored container, never through client-supplied `groupId` for items.
- Membership check on upsert/delete/sync; admin/creator check on deletes.
- Batch size cap (100), payload size caps (§3.5), `page_size` via `validatePaginationPageSize`, `since` via `validateSyncVersion`.
- `saidByUserId`, `paidBy`, `participants` are validated against current membership so a user cannot reference arbitrary user ids.

---

## 6. HOW TO ADD A NEW EXTENSION TYPE (goes verbatim into both `ExtensionItemData.kt` headers)

**Server**
1. Add the enum value to `GroupExtensionType`.
2. Add the `data class` to `ExtensionItemData` with `@TypeAlias("snake_name")` and register it in `@JsonSubTypes` with the same name.
3. If the type needs config, add an `ExtensionSettings` subclass the same way and allow it in the container validator.
4. Add the exhaustive `when` branch in `ExtensionItemValidator` (compiler enforced).
5. If the type is single-item (like shared text), add it to the single-item set checked in `upsertItems`.

**Client**
1. Add the enum value to `GroupExtensionType` and wire `stringRes()` + `drawableRes()` (compiler enforced).
2. Add the `data class` to `ExtensionItemData` with the same `@SerialName` and property names as the server.
3. Add strings for the type name in `values`, `values-de`, `values-it`.
4. Add the screen package (`State`, `Action`, `ViewModel`, `Screen`, `Root`), register the ViewModel in `Modules.kt`, add the `when` branch in `GroupExtensionScreenRoot`.
5. Optionally add a pure domain calculator for derived values.

Nothing else changes: no endpoint, no sync job, no socket message, no Room table, no DB version bump (the payload is JSON in an existing column).

---

## 7. Phases

| Phase | Scope | Done when |
|---|---|---|
| **1 - Server core** | models, converters, `SyncCollection.GROUP_EXTENSIONS`, repositories, validator, service, controller (all 5 endpoints), `joinedAtExtensionVersion`, socket subtypes + `NotificationService` methods, `LogType`s, group delete cascade | endpoints callable with curl, `security-check` table clean |
| **2 - Client data layer** | domain classes, Room entities/DAO/converters + DB 83, network DTOs + mappers, `NetworkUtils` calls, `AppRepository` sync job + CRUD, socket branches, purge hooks, DI | `dataSync` shows a `GROUP_EXTENSIONS` job that succeeds; creating an extension on device A appears in Room on device B |
| **3 - Group details + shared text** | `ChatDetails` section, `AddExtensionDialog`, `Route.GroupExtension`, `GroupExtensionScreenRoot`, shared-text screen | first extension usable end to end |
| **4 - Quotes + Split** | quote screen; split screen with `SplitCalculator` | balances match a hand-computed example |
| **5 - Route planner** | maplibre screen, tap-to-add, reorder, distance | route renders as polyline on both devices |
| **6 - Polish (optional)** | shared-text 409 conflict guard, push notification for new quotes/expenses, location picker return flow, outbox integration | - |

Each phase ends with the `readme-changelog-updater` and `string-resource-extractor` agents.

---

## 8. Decisions taken and open points

Decided in this plan (change here if you disagree):
- Version-watermark sync (messages style), not IdTimeStamp, because items are high-write and per-group.
- Two Mongo collections / two Room tables sharing one counter and one sync envelope, rather than one mixed collection; keeps queries and DAOs obvious.
- Server-side `joinedAtExtensionVersion` for late joiners, mirroring the messages solution, instead of client-side per-group re-fetch bookkeeping.
- Any member can edit any item; deletes restricted to creator/admin.
- No push fallback in v1; socket + sync only.

Open (ask before phase 3):
- Should extensions also be reachable from the chat top bar / an icon row above the message list, or only from group details?
- Split: single currency per container (settings) or per expense? The model allows per expense; the UI in v1 would default to the container currency.
- Route planner: is "pick an existing schneaggmap entry as a point" required for v1, or is tap-to-add enough?
- Should 1:1 chats ever get extensions? The model is keyed by `groupId` only; supporting friend chats would need a second owner key and is not planned here.
