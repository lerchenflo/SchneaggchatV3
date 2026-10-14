# Repository Interface Migration Plan

**Status: implemented** (Phases 0–9, branch `macci`, 2026-10-14); the device tests are still open (checklist in `plans/Todo.md`).

Splits the god object `AppRepository` (`C/datasource/AppRepository.kt`, ~2,990 lines, 14 constructor deps, used by ~40 files) into feature repositories and coordinators. Each one has an **interface**, so every ViewModel depends only on interfaces and can be unit-tested with in-memory fakes.

**Order** (decided 2026-10-13): this plan → follow-ups in `plans/Todo.md` (NetworkUtils fix-up, unified NetworkUtils results, `Result<T, DataError>` conversion) → `CHAT_BASED_MIGRATION_PLAN.md` → `E2E_ENCRYPTION_PLAN_V2.md`.

**Why first**: the chat migration rewrites the exact code paths this plan moves (send, sync, chat list, push apply). With interfaces and fakes in place first:
- the follow-ups and the chat migration change signatures in one interface instead of in a 3k-line class;
- ViewModel tests written here act as a safety net for both;
- E2E gets a clean hook point (`MessageRepository` + `AuthRepository`) instead of a 15th constructor parameter on `AppRepository`.

**Hard rule: purely structural.** No behavior changes, no new features, no API or DB changes. Keep return types as they are (`Boolean` / nullable / `NetworkResult`, see D3); converting them is a follow-up in `plans/Todo.md`. Keep `(chatId, isGroup)` signatures; `chatId` is introduced by the chat migration, not here. Every phase compiles and runs on its own.
Wiring changes the user asked for are in scope even though they go beyond moving code: injected channels (D10), interfaces for app-wide classes (D7, D8) and composables reading their data from ViewModels (§2.5). They change how things are wired, not what the app does.
**One deliberate behavior change (D16, revised by R1)**: in Phase 3, the 11 user/group mutations stop calling the full `dataSync()`. Each calls its own repository's `syncUsers()` / `syncGroups()` instead. No cycle, no stale UI, no server dependency. Once `SchneaggchatV3server/Todo.md` confirms that every endpoint pushes over the socket, these own-sync calls can be removed too.

**Android-standards review (2026-10-13, §8)**: an independent review checked this plan against the official Android architecture guidance and Now in Android. Its accepted changes (R1–R12) are worked into the sections below; §8 lists each one, plus the review's open questions and how they were decided while the user was away.

**Findings**: anything noticed during the migration that is out of scope goes into `plans/Todo.md` (see CLAUDE.md), not into the migration commits.

Paths: `C/` = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/`. Line numbers verified 2026-10-13.

---

## 1. Current state (verified 2026-10-13)

### 1.1 `AppRepository`
- Constructor (:171-187): `AppDatabase`, `NetworkUtils`, `Preferencemanager`, `PictureManager`, `AudioManager`, `UserRepository`, `GroupRepository`, `MessageRepository`, `AppVersion` (public `val`), `LoggingRepository`, `MapRepository`, `EventRepository`, `LanguageService`, `AuthSessionManager`. `Notifier` and `SocketConnectionManager` are pulled with `KoinPlatform.getKoin()` inside `logout` / `deleteAllAppData` / `endServerSession`.
- Nested global objects `ErrorChannel` (:189-224) and `ActionChannel` (:227-246). Used by `AppAuthEventSink`, `App.kt`, `EventsViewModel`, `LoginViewModel` and `AppRepository` itself.
- Rough responsibility buckets (function → line):
  - **Auth/session**: `setNotificationToken` :257, `sendEmailVerify` :265, `onNewTokenPair` :1191 (only called internally), `loadSavedLoginConfig` :1212, `login` :1256, `createAccount` :1304, `changePassword` :1595, `getSessions` :1611, `logoutAllDevices` :1633, `endSession` :1650.
  - **Teardown**: `deleteAllAppData` :282, private `endServerSession` :304, `logout` :329. Neither deletes downloaded media (tracked in `plans/Todo.md`).
  - **App info**: `testServer` :357, `getChangeLog` :924, `getLatestGitHubVersionAsString` :940, `appVersion`.
  - **Sync**: `dataSyncLock` :366, `dataSyncState` :423, `dataSync` :430, private `mapEntryIdSync` :632, private `eventIdSync` :710, `userIdSync` :1345, `messageIdSync(fetchMedia)` :2089, `groupIdSync` :2526 (returns removed group ids), `getMissingPics` :872, `getMissingAudios` :904.
    - `dataSync` runs users / messages / groups / map / events in parallel, then purges the messages of removed groups (`messageRepository.deleteMessagesForGroup`, :593), then the MEDIA job (`getMissingPics` + `getMissingAudios`).
    - `getMissingPics` covers **user, group and image-message** pictures, each in its own `launch`. `getMissingAudios` covers message audio only.
    - `executeSync` (:2878) sits inside a commented-out block (:2871-2928) and calls `NetworkUtils` functions that no longer exist. Dead code: it is not moved and disappears with the class in Phase 8.
  - **Map/Event pass-throughs**: `upsertMapEntry` :663, `deleteMapEntry` :695, `upsertEvent` :736, `setEventParticipation` :786, `joinEvent` :803, `detachEvent` :850.
  - **Users/friends**: `getUserByIdFlow` :963, `getUserById` :969, `getFriends`/`getFriendsFlow` :985/:992, `getSnailTrailFlow` :1000, `getProfilePicturesForUserIds` :1498, `getAvailableUsers` :1517, `send/deny/removeFriend…` :1537-1567, `changeUsername` :1581, `changeProfilePic` :1661, `changeUserDetails` :1678, `setLocationSharing` :2686, `disableLocationSharingForAllFriends` :2717, `setWakePermission` :2729, `setWakeGlobal` :2746, `sendWake` :2769.
  - **Chat list / search**: `getPendingFriends` :973, `getUnreadChatCountFlow` :1006, `getChatListFlow` :1018, `getChatSelectorFlow` :1086, `getMessageSearchFlow` :1105, `setPinnedChat` :2841.
  - **Messages**: `MessageContent` :1719, `sendMessageLock` :1728, `sendMessage` :1738, `sendOfflineLock` :1978, `sendOfflineMessages` :1986, `messageSyncLock` :2080, `applyPushMessage` :2198, `getPicturesForMessageIds` :2255, `getAudiosForMessageIds` :2270, `setAllChatMessagesRead` :2305, `editMessage` :2322, `deleteLocalMessage` :2367, `deleteMessage` :2371, `reactToMessage` :2387, `votePoll` :2409, `deletePollOption` :2433.
  - **Groups**: `createGroup` :2471, `getProfilePicturesForGroupIds` :2595, `changeGroup*` :2610-2667.
  - **Settings**: `applyServerSettings` :2789, `setUseMd`…`setMapStyleSetting` :2800-2835, `setQuickReactions` :2855, `resetQuickReactions` :2861, `setLastContributePopupShown` :2866.
- Nested types and file-level helpers that need a new home too:
  - `DataSyncJobStatus` / `DataSyncJobType` / `DataSyncJobState` / `DataSyncState` (:368-424), read by `ChatSelector.kt` and `DataSyncDetailPopup.kt`.
  - `SavedCredsResult` (:1204), returned by `loadSavedLoginConfig`.
  - `MessageContent` (:1719), used by `ChatViewModel` and `ReplyReceiver` (9 uses).
  - private `Message.chatKey` (:2933), private `sortedForChatSelector` (:2944), public `applySearchAndFilter` (:2972, imported by `ChatSelectorViewModel` from `datasource`).
- Calls across buckets that shape the dependency graph:
  - 11 user/group mutations call `dataSync()` after success: `sendFriendRequest` :1544, `denyFriendRequest` :1560, `removeFriend` :1574, `changeUsername` :1588, `changeProfilePic` :1668, `changeGroupProfilePic` :2617, `changeGroupDescription` :2630, `changeGroupName` :2643, `changeGroupMembers` :2660, `changeGroupExpiry` :2674. Moved verbatim this would be a UserRepository/GroupRepository ↔ DataSyncCoordinator cycle; resolved by D16.
  - `logoutAllDevices` calls `logout(endSessionOnServer = false)` (:1644). Moved verbatim this would be an AuthRepository ↔ SessionTerminator cycle; resolved by D17.
  - `userIdSync` calls `applyServerSettings` (:1445), so UserRepository depends on SettingsRepository.
  - `setAllChatMessagesRead` calls `messageIdSync()` (:2317); both end up in MessageRepository.

### 1.2 Existing "repositories" are really local data sources
`UserRepository`, `GroupRepository`, `MessageRepository`, `EventRepository`, `MapRepository` and `LoggingRepository` each wrap only `AppDatabase`. `SettingsRepository` (`C/settings/data/SettingsRepository.kt`: md flag, highlight flag, quick reactions, drafts) wraps only `Preferencemanager`, and `FeedbackRepository` wraps only `NetworkUtils`. None of them has an interface. Under the android-data-layer skill naming, the Room-only ones are **local data sources**.

### 1.3 What ViewModels inject today
| ViewModel | Data-layer / platform deps |
|---|---|
| `ChatViewModel` | AppRepository, MessageRepository, UserRepository, GroupRepository, SettingsRepository, LoggingRepository, PictureManager, AudioManager, PermissionManager, Navigator, ApplicationScope |
| `ChatDetailsViewmodel` | AppRepository, MessageRepository, UserRepository, GroupRepository, EventRepository, PictureManager, Navigator |
| `SharedContentViewModel` | MessageRepository, UserRepository, PictureManager, Navigator |
| `ChatSelectorViewModel` | AppRepository, Preferencemanager, LoggingRepository, PermissionManager, Navigator |
| `EventsViewModel` | AppRepository, EventRepository, GroupRepository, UserRepository, Preferencemanager, PictureManager, Navigator, ApplicationScope |
| `SchneaggmapViewModel` | AppRepository, MapRepository, EventRepository, UserRepository, Preferencemanager, LocationService, Navigator |
| `GlobalViewModel` (Koin `single`) | AppRepository, EventRepository, Preferencemanager, SocketConnectionManager, LocationService, BatteryService, AppVersion, Navigator |
| Settings VMs (Shared, Appearance, Privacy, Notification, User, LoggedInDevices, SchneaggmapSettings, Misc, Dev) | AppRepository (all but Dev), Preferencemanager, LoggingRepository, PermissionManager, PictureManager, AppVersion, LanguageService, Navigator, ApplicationScope |
| Login VMs (Login, SignUp, EmailVerifiedCheck, AutoLoginCredChecker) | AppRepository, Preferencemanager, LoggingRepository, LanguageService, PictureManager, Navigator, ApplicationScope |
| `NewChatViewModel` | AppRepository, Preferencemanager, LoggingRepository, ShareUtils, Navigator |
| `GroupCreatorViewModel` | AppRepository, PictureManager, Navigator |
| `BirthdaysViewModel` | AppRepository, Navigator |
| `FriendCompassViewModel` | AppRepository, LocationService, Navigator |
| `FeedbackViewModel` | FeedbackRepository, Navigator |
| Games: `PlayerSelectorViewModel`, `SchneaggRodeoViewModel` | AppRepository (`getFriends`, `getFriendsFlow`, `getUserByIdFlow` only) |

Notes on these dependencies:
- `Preferencemanager` is injected into 14 ViewModels. They mostly read flows: server URL, theme, map style, merge flags, demo mode, events view mode, onboarding/last-started version.
- `PictureManager`, `AudioManager`, `PermissionManager`, `LocationService`, `BatteryService`, `AppVersion` and `ShareUtils` are **`expect class`es**, so `commonTest`/`jvmTest` can't fake them. `LanguageService` is a plain class but wraps the `expect class LanguageManager`. `SessionTerminator` will need the `expect class Notifier`.
- `Navigator`, `ApplicationScope` (hardcodes `Dispatchers.IO`) and `LanguageService` are plain classes.

### 1.4 Other `AppRepository` callers (not ViewModels)
- `SocketConnectionMessage.kt` (resolves everything via `KoinPlatform.getKoin()`, :168-173): `getPicturesForMessageIds` / `getAudiosForMessageIds` (:240/:244), `getProfilePicturesForUserIds` (:295/:339/:378), `applyServerSettings` (:381), `getProfilePicturesForGroupIds` (:421), `dataSync` (:428).
- `AppAuthEventSink.kt`: the channels.
- `App.kt`: `loadSavedLoginConfig`, `logout` (on `AuthInvalidated`), the channels, `appVersion`. Also injects `Preferencemanager`, `LanguageService`, `LoggingRepository`.
- Composables:
  - `Login.kt`: `appVersion` (`VersionText`), plus `Preferencemanager` and `LanguageService` via `koinInject`.
  - `SettingsScreen.kt`: `appVersion`.
  - `ChatSelector.kt`: `dataSyncState`, `appVersion`, plus `Preferencemanager` via `koinInject`.
  - `UrlChangeDialog.kt`: `testServer`. Hosted by `Login.kt` and `DeveloperSettings.kt`.
  - `DataSyncDetailPopup.kt`: the `DataSync*` types.
- Push entry points (all resolve `AppRepository` via `KoinPlatform.getKoin()`):
  - `androidMain/…/AppFirebaseMessagingService.kt`: `setNotificationToken`, `applyPushMessage`.
  - `androidMain/…/MarkAsReadReceiver.kt`: `setAllChatMessagesRead`.
  - `androidMain/…/ReplyReceiver.kt`: `sendMessage` (`MessageContent.TextContent`), `setAllChatMessagesRead`.
  - `iosMain/…/IosPushDelegateBridge.kt`: `setNotificationToken`, `applyPushMessage`, `messageIdSync`, `dataSync`.
- There is **no** WorkManager job and no iOS background task.
- KDoc references to `AppRepository`: `SafeNetworkCall.kt:25`, `Preferencemanager.kt:398`, `C/chat/data/UserRepository.kt:161`.
- Static `KoinPlatform.getKoin().get<LoggingRepository>()` lookups: `AppFirebaseMessagingService.kt:99/:125`, `koinAndroidUtils.kt:45`, `Notifier.android.kt:439`.

### 1.5 Test setup
- `commonTest` already has `kotlin-test`, `kotlinx-coroutines-test` and `ktor-client-mock`.
- `jvmTest` has one benchmark (`ChatLoadingBenchmark.kt`) using in-memory Room with `BundledSQLiteDriver`.
- Turbine and AssertK are not in the catalog.

---

## 2. Target architecture

### 2.1 Layers and dependency direction
```
ViewModel ──► domain interface (Repository / Coordinator)
                     ▲ implemented by
              data impl ──► LocalDataSource (Room) / NetworkUtils / platform service interfaces
Coordinator impl ──► Repository interfaces   (never the reverse)
Composable ──► its ViewModel only            (exception: App.kt, see §2.5)
```
Rules:
- A ViewModel never imports anything from a `data` package or from `datasource` (except `datasource/domain`): no `AppDatabase`, `NetworkUtils`, `Preferencemanager`, DAOs or DTOs.
- A composable never `koinInject`s a data-layer class. It reads ViewModel state and sends actions (§2.5).
- Repositories don't depend on each other in a cycle. Shared logic goes up into a coordinator.
- Coordinators only call repository interfaces: `sync*()`, `fetchMissing*()`, and narrow cleanup methods such as `MessageRepository.deleteMessagesForGroups(ids)`.
- Coordinators own the cross-repo locks: `dataSyncLock` → `DataSyncCoordinator`; `sendMessageLock` / `sendOfflineLock` / `messageSyncLock` → `MessageRepository`.
- One interface + one implementation per concept. Bind in Koin as `singleOf(::OfflineFirstMessageRepository) bind MessageRepository::class`.
- **Work that must outlive a screen runs in the data layer (R2).** Repositories and coordinators take an `externalScope: CoroutineScope` (Koin binds the existing `ApplicationScope` instance) and run such work as `externalScope.async { … }.await()`. ViewModels call them from `viewModelScope` and never inject `ApplicationScope`. Tests pass `backgroundScope`.
- **Interfaces in `D/` never expose network DTOs (R3).** `NetworkUtils.*` types used by ViewModels (`FriendshipStatus`, `GroupMemberAction`, `WakeOutcome`, `SessionResponse`, `NewFriendsUserResponse`, `PollCreateRequest`, `PollVoteOptionCreateRequest`, `PollVoteRequest`) get domain models + mappers in the phase that creates the interface exposing them.
- **No hardcoded dispatchers in classes under test (R4).** `OfflineFirstChatListRepository` and `OfflineFirstMessageRepository` take `CoroutineDispatcher` constructor parameters whose defaults keep today's behavior. Koin relies on the defaults (explicit lambda with named arguments, no dispatcher binding; decided in Phase 4); tests pass their own dispatcher.

### 2.2 Target classes
Interface packages follow **D4** (decided 2026-10-13), matching the module-structure skill rule "features never import from each other":
- **Cross-feature interfaces** live in the shared package `datasource/domain/` (shortened to `D/` below), together with the types they expose.
- **Feature-only interfaces** stay in their feature's `domain` package: `ChatListRepository`, `FeedbackRepository`.
- `MapRepository` counts as shared, because `DataSyncCoordinator` (shared) calls it.
- Implementations stay in the `data` packages listed below; Koin binds them. A feature `data` package importing `D/` is fine (feature → shared).
- **Models move with their interface (R7).** A domain model that a `D/` interface exposes (`User`, `Group`, `Message`, `Event`, map entry models, …) moves to `D/model/` in the phase that creates that interface, as a mechanical package move. Otherwise the shared package would import a feature. Feature-only models (`ChatListItem`, `MessageDisplayItem`, …) stay in their feature.

| Interface | Impl | Takes over |
|---|---|---|
| `D/AuthRepository` | `login/data/RemoteAuthRepository` | login, createAccount, loadSavedLoginConfig (+ `SavedCredsResult` → `D/`), onNewTokenPair (internal), sendEmailVerify, changePassword, sessions, endSession, setNotificationToken, `endServerSession` (with the existing 5 s timeout), `logoutAllDevicesOnServer()` (the server half of `logoutAllDevices`, D17) |
| `D/SessionTerminator` | `app/data/LocalSessionTerminator` | logout, deleteAllAppData, `logoutAllDevices()` (calls `AuthRepository.logoutAllDevicesOnServer()`, then its own `logout(endSessionOnServer = false)`; D17). Moved verbatim: clears the session, DB, prefs, push token, socket and `SessionCache`, exactly like today. **No media wipe** (gap tracked in `plans/Todo.md`). Gets `AuthSessionManager` (`currentTokens`, `clearSession`), `AuthRepository`, `Notifier` and `SocketConnectionManager` by constructor instead of `KoinPlatform` |
| `D/AppInfoRepository` | `app/data/RemoteAppInfoRepository` | testServer, getChangeLog, getLatestGitHubVersionAsString, app version string |
| `D/UserRepository` | `chat/data/OfflineFirstUserRepository` | user/friend flows, friend requests, available users, username/pic/details changes, location sharing, wake permission/global/send, snail trail, online state (from the current local class), `syncUsers()` (= `userIdSync`, uses `SettingsRepository.applyServerSettings`), `getProfilePicturesForUserIds`, `fetchMissingProfilePictures()` (user part of `getMissingPics`). The mutations call `syncUsers()` instead of `dataSync()` (D16/R1) |
| `D/GroupRepository` | `chat/data/OfflineFirstGroupRepository` | createGroup, changeGroup*, group flows/getters, `syncGroups(): List<String>` (= `groupIdSync`, returns the removed group ids), `getProfilePicturesForGroupIds`, `fetchMissingGroupPictures()` (group part of `getMissingPics`). The mutations call `syncGroups()` instead of `dataSync()` (D16/R1) |
| `D/MessageRepository` | `chat/data/OfflineFirstMessageRepository` | sendMessage (+ `MessageContent` → `D/`), sendOfflineMessages, edit/delete/react/poll, markRead, applyPushMessage, message flows/getters (from the current local class), `syncMessages(fetchMedia: Boolean = true)` (= `messageIdSync`), message media fetch, `fetchMissingMessageMedia()` (image part of `getMissingPics` + `getMissingAudios`), `deleteMessagesForGroups(ids)`. Send and mark-read run in `externalScope` (R2), which replaces the `applicationScope.launch` in `ChatViewModel` |
| `chat/domain/ChatListRepository` | `chat/data/OfflineFirstChatListRepository` | getChatListFlow (+ `chatKey`, `sortedForChatSelector`), getChatSelectorFlow, getMessageSearchFlow, getPendingFriends, getUnreadChatCountFlow, setPinnedChat. `applySearchAndFilter` moves to `chat/domain` (pure function, used by the ViewModel) |
| `D/EventRepository` | `events/data/OfflineFirstEventRepository` | upsertEvent, setEventParticipation, joinEvent, detachEvent, event flows, `syncEvents()` (= `eventIdSync`) |
| `D/MapRepository` | `schneaggmap/data/OfflineFirstMapRepository` | upsertMapEntry, deleteMapEntry, map flows, `syncMapEntries()` (= `mapEntryIdSync`) |
| `D/SettingsRepository` | `settings/data/DataStoreSettingsRepository` | **absorbs the existing `settings/data/SettingsRepository`** (drafts, md flag, highlight flag, quick reactions) + every `set*` from AppRepository + applyServerSettings + `setLastContributePopupShown` + **every preference flow a ViewModel reads today** (replaces direct `Preferencemanager` injection) |
| `D/DataSyncCoordinator` | `app/data/DefaultDataSyncCoordinator` | dataSync (order, lock, `dataSyncState`, purge of removed groups' messages, MEDIA job), `DataSync*` types → `D/`. The sync body runs in `externalScope` (R2), so callers in `viewModelScope` no longer need `applicationScope.launch` to keep it alive. A caller that leaves its screen no longer cancels a running sync |
| `feedback/domain/FeedbackRepository` | `feedback/data/RemoteFeedbackRepository` | unchanged API, just gets an interface |
| `D/Logger` (D9) | `app/logging/RoomLoggingRepository` | log*, getLogs, clearLogs |

The existing Room-only classes get renamed to local data sources (D1). They stay internal to `data`:
- `UserRepository` → `UserLocalDataSource` (interface) / `RoomUserDataSource`
- `GroupRepository` → `GroupLocalDataSource` / `RoomGroupDataSource`
- `MessageRepository` → `MessageLocalDataSource` / `RoomMessageDataSource`
- `EventRepository` → `EventLocalDataSource` / `RoomEventDataSource`
- `MapRepository` → `MapLocalDataSource` / `RoomMapDataSource`

`NetworkUtils` (1,442 lines) stays one concrete remote data source in this plan (D2). Repository impls are tested against it through Ktor `MockEngine`. Splitting it is the first follow-up after this plan.

### 2.3 Platform services → interfaces (D7, D8 revised by R9)
- Every `expect class` that a ViewModel or repository injects becomes a common `interface` plus per-platform implementations: `PictureManager`, `AudioManager`, `PermissionManager`, `LocationService`, `BatteryService`, `AppVersion`, `ShareUtils`, `LanguageManager`, `Notifier` (9). KMP forces this; it matches NiA's `Notifier` / `NetworkMonitor` interfaces.
  - Pattern: `interface PictureManager { … }` in `commonMain`, `class AndroidPictureManager(context) : PictureManager` in `androidMain`, and the same for iOS/JVM. Koin platform modules bind them.
  - Signatures stay identical, so call sites only change in Koin.
  - The `AppVersion` interface moves from `settings/data/` to `utilities/`, next to the other platform services (R11).
- **No interfaces for `Navigator`, `ApplicationScope`, `LanguageService` (R9).** Google has no such rule, and NiA and the house navigation skill keep `Navigator` concrete. Tests use a real `Navigator`, a real `LanguageService` with a fake `LanguageManager`, and pass `backgroundScope` where a repository wants an `externalScope`. `ApplicationScope` stays the existing class, but ViewModels stop injecting it (R2).

### 2.4 Global channels → injected (D10)
- `AppRepository.ErrorChannel` / `ActionChannel` become interfaces in `D/` (`ErrorChannel`, `ActionChannel`, with `ErrorEvent` / `ActionEvent`) with one impl each, bound as Koin `single`s.
- Same buffered `Channel`, same `send` / `trySend` semantics, so behavior doesn't change.
- Callers get them injected: the new repositories, `AppAuthEventSink`, `App.kt`, `EventsViewModel`, `LoginViewModel`. Tests use fakes that record events.
- `ErrorEvent` becomes free of Compose (R6). `toStringComposable()` turns into an extension function in `app/`, so `D/` has no Compose dependency.
- Repositories writing into `ErrorChannel` is temporary (R6). The data layer should return errors to its caller; that goes away with the `Result<T, DataError>` follow-up.
- Channels stay Channels (review question Q1). The house MVI skill uses a `Channel` for one-off events; turning the global error/action events into app-level state is in `plans/Todo.md`.

### 2.5 Composables read from ViewModels (decided 2026-10-13, done in Phase 7)
All of it happens in Phase 7, once every repository exists, so each composable is rewired only once, straight to its final interface (D21). Until then the composables keep using the `AppRepository` delegating shell.
- `Login.kt` → `LoginViewModel`: version text, server URL, language.
- `ChatSelector.kt` → `ChatSelectorViewModel`: `dataSyncState`, `isMobile` / `isDesktop`, preference flows.
- `SettingsScreen.kt` → `SettingsViewModel`: version name and code.
- `UrlChangeDialog.kt` takes state + callbacks. `testServer` runs in its two hosts, `LoginViewModel` and `DevSettingsViewModel`.
- `DataSyncDetailPopup.kt`: only the type imports change.
- **Exception `App.kt`**: as the root composable it keeps `koinInject`, but only of interfaces (`AuthRepository`, `SessionTerminator`, `AppVersion`, `Logger`, `SettingsRepository`, `LanguageService`, the channels). `GlobalViewModel` gets nothing new; splitting it is in `plans/Todo.md`.

---

## 3. Phases

All phases go on **one long branch** (D11). Each phase still leaves the app compiling and behaving the same (except D16), and lands as its own commit(s), so a regression can be bisected to one phase. During the move, `AppRepository` keeps thin delegating methods (`suspend fun login(...) = authRepository.login(...)`) so callers can be switched one by one. It is deleted in Phase 8.

**Verification per phase (D20)**: for this migration Claude may run the compile tasks (`:composeApp:compileKotlinJvm`, `:composeApp:compileAndroidMain`, `:composeApp:compileKotlinIosSimulatorArm64`) and `:composeApp:jvmTest`. No assemble, run or install tasks. The user still launches the app on Android, iOS and Desktop after each phase.

### Phase 0: Groundwork
1. Channels → injected interfaces in `D/` (§2.4). Switch all 5 callers.
2. The 9 `expect class`es → interfaces (§2.3); the `AppVersion` interface lives in `utilities/` (R11). Update the Koin platform modules. `Navigator`, `ApplicationScope`, `LanguageService` stay concrete (R9).
3. `LoggingRepository` → `D/Logger` + `RoomLoggingRepository` (D9), including the 4 static `KoinPlatform` lookups (§1.4).
4. Test deps: add `turbine` + `assertk` to the version catalog and `commonTest` (D5). Add `commonTest/…/testutil/` with `Dispatchers.setMain(UnconfinedTestDispatcher())` setup, a helper that resets the global objects `SessionCache`, `SnackbarManager`, `AppLifecycleManager` and `IncomingDataManager` before each test (D19; tests therefore don't run in parallel), and an in-memory Room builder for `jvmTest`. Concurrency tests (locks, dedup, sync order) use `StandardTestDispatcher` with one shared scheduler per test instead of `UnconfinedTestDispatcher` (R12).
6. `ErrorEvent` loses its `@Composable` member; `toStringComposable()` becomes an extension in `app/` (R6).
5. Rename the Room-only classes to `*LocalDataSource` / `Room*DataSource` (§2.2) with interfaces. This is mechanical, and the IDE rename covers it.

### Phase 1: Low-risk pass-throughs
- `SettingsRepository` interface + `DataStoreSettingsRepository`, absorbing the existing `settings/data/SettingsRepository`. Move all `set*`, `applyServerSettings`, quick reactions, `setLastContributePopupShown`, plus the preference flows from §1.3.
- Switch the 14 ViewModels from `Preferencemanager` to `SettingsRepository` (incl. `DevSettingsViewModel` and `ChatViewModel`'s old `SettingsRepository`). `SocketConnectionMessage` (`applyServerSettings`) points at the new class. Update the KDoc at `Preferencemanager.kt:398`.
- `MapRepository`, `EventRepository` (absorb the pass-throughs from :663-850) and `AppInfoRepository`.
- `FeedbackRepository` interface (rename only).
- Models exposed by the new `D/` interfaces move to `D/model/` (R7), e.g. the event and map entry models.
- `EventsViewModel`'s `applicationScope.launch { dataSync(…) }` (:285) stays until Phase 6 (R2).
- Callers switched: Map, Events, Appearance/Misc/Map/Notification/Dev settings, `SharedSettingsViewmodel`, `LoggedInDevicesViewModel` (AppVersion), `GlobalViewModel` (`testServer`). Composables and `App.kt` wait for Phase 7 (§2.5).

### Phase 2: Auth + teardown
- `AuthRepository` + `SessionTerminator` (§2.2). `logout` and `deleteAllAppData` move verbatim; no media wipe.
- `logoutAllDevices` is split (D17): the server call becomes `AuthRepository.logoutAllDevicesOnServer()`, and `SessionTerminator.logoutAllDevices()` calls it and then `logout(endSessionOnServer = false)`. Same order as today, one-way dependency.
- `AuthSessionManager` stays as-is and is injected into `AuthRepository` and `SessionTerminator`.
- `SessionResponse` gets a domain model + mapper (R3).
- Callers switched: the 4 login VMs, `UserSettings`/`PrivacyAndSecurity`/`LoggedInDevices`/`MiscSettings` VMs, `AppFirebaseMessagingService` and `IosPushDelegateBridge` (`setNotificationToken`). `App.kt` waits for Phase 7.
- **Test on device**: login, logout, logout all devices, push token registration on Android and iOS.

### Phase 3: Users and groups
- `UserRepository` (offline-first, wraps `UserLocalDataSource` + `NetworkUtils` + `PictureManager`) and `GroupRepository`.
- `syncUsers()` / `syncGroups()` are moved from `userIdSync` / `groupIdSync` unchanged. `DataSyncCoordinator` doesn't exist yet, so `AppRepository.dataSync` calls them for now.
- **D16 / R1**: the 11 mutations (§1.1) call their own repository's `syncUsers()` (user mutations) or `syncGroups()` (group mutations) instead of the full `dataSync()`. Because the own-sync no longer goes through `dataSyncLock`, `syncUsers()` and `syncGroups()` each guard their body with a repository-level `Mutex` (like `messageSyncLock`), so a mutation's sync and a running full sync never run the same sync concurrently. Lock order is always `dataSyncLock` → repository lock, so no deadlock (found in Phase 3a). Once `SchneaggchatV3server/Todo.md` confirms the socket pushes, these calls can go too.
- `User`, `Group`, `GroupMember`, `SnailTrailPoint`, `UserLocation` and the other models these interfaces expose move to `D/model/` (R7). `FriendshipStatus`, `GroupMemberAction`, `WakeOutcome`, `NewFriendsUserResponse` get domain models + mappers (R3).
- `getMissingPics` is split: its user part becomes `UserRepository.fetchMissingProfilePictures()`, its group part `GroupRepository.fetchMissingGroupPictures()`. `AppRepository.getMissingPics` calls both until Phase 6.
- Callers switched: NewChat, GroupCreator, Birthdays, FriendCompass, ChatDetails (user/group parts), Notification/Map/Privacy settings, `GlobalViewModel` (friends, location sharing), `SocketConnectionMessage` (user and group pics), and the games `PlayerSelectorViewModel` / `SchneaggRodeoViewModel` (mechanical swap only; nothing else in `games/` is touched or used as reference).

### Phase 4: Chat list
- `ChatListRepository` with `getChatListFlow` (+ `chatKey`, `sortedForChatSelector`), `getChatSelectorFlow`, `getMessageSearchFlow`, `getPendingFriends`, `getUnreadChatCountFlow`, `setPinnedChat`. It depends on the User/Group/Message **local data sources** for reads and on `SettingsRepository` for pins.
- `applySearchAndFilter` moves to `chat/domain`.
- Callers: `ChatSelectorViewModel`, `GroupCreatorViewModel`, `NewChatViewModel`, `GlobalViewModel` (unread count).
- This is the class the chat migration later rewrites around `chatId`.
- Takes a `CoroutineDispatcher` for its `flowOn(Dispatchers.Default)` calls (R4).

### Phase 4.5: Send-path characterization tests (review Q2, decided 2026-10-13)
Nobody can device-test during an unattended run, and message sending is due for a rebuild anyway (`plans/Todo.md`). So before Phase 5, write `jvmTest` tests (in-memory Room + Ktor `MockEngine`, `StandardTestDispatcher`) against the **current** `AppRepository` send path:
- send success (local insert, server call, local row updated with the server id);
- send-echo dedup by `clientMessageId` (the socket/sync copy doesn't create a second row);
- offline queue retry (`sendOfflineMessages` sends unsent rows in order and stops on error).
They must stay green after Phase 5 (c); then they point at `OfflineFirstMessageRepository`. They don't replace the device test after Phase 5, which stays on the user's checklist.

**Done (2026-10-14).** `jvmTest/…/chat/data/`: `MessageSendTest` (5), `MessageSendEchoDedupTest` (3), `OfflineMessageQueueTest` (5). Notes for Phase 5:
- Only `jvmTest/…/testutil/SendPathFixture.kt` builds the object under test and calls `sendMessage` / `sendOfflineMessages` / `messageIdSync` (it also holds the only `MessageContent` import). Retarget it there; the test classes stay unchanged.
- The server is `testutil/FakeMessageServer` (MockEngine for `POST /messages/send/text` and `GET /messages/sync`). Any other request fails the test, and every `unusedDependency()` stub throws when called. If the moved code starts using one of them on the text path, give the fixture a fake.
- Two tests run a send concurrently (sync before the send response; drain waits for a send in flight). They rely on the send suspending inside the MockEngine handler while holding `sendMessageLock`, and on the drain polling that lock with `delay`. With R2 (`externalScope`) the fixture is built in `@BeforeTest`, outside `runTest`, so `backgroundScope` is not available there; build the fixture inside the test or pass a scope of its own.

### Phase 5: Messages (highest risk)
- `MessageRepository` takes over send (:1738-1976 incl. send-echo handling), offline queue, edit, delete, react, poll, mark-read, `applyPushMessage` and message media fetches, together with `sendMessageLock`, `sendOfflineLock` and `messageSyncLock`. `MessageContent` moves to `D/`.
- `syncMessages(fetchMedia)` = `messageIdSync(fetchMedia)`.
- The image-message part of `getMissingPics` and all of `getMissingAudios` become `fetchMissingMessageMedia()`.
- New `deleteMessagesForGroups(ids)` wraps the purge `dataSync` does today (:593), so the coordinator doesn't need a local data source.
- R2: send and mark-read run inside `externalScope` in the repository; `ChatViewModel` drops `ApplicationScope` and calls them from `viewModelScope` (today: `ChatViewModel.kt:200, 289, 303, 320, 362`). Started work must still finish after the chat screen closes.
- R4: the repository takes `CoroutineDispatcher` parameters for its `withContext(Dispatchers.IO)` (:1920).
- R3: `PollCreateRequest`, `PollVoteOptionCreateRequest`, `PollVoteRequest` get domain models + mappers; `MessageContent` and `Message` live in `D/model/`.
- The Phase 4.5 tests run after each sub-step.
- Callers: `ChatViewModel`, `ChatDetailsViewmodel`, `SharedContentViewModel`, `GlobalViewModel` (`sendOfflineMessages`), `SocketConnectionMessage`, `AppFirebaseMessagingService` + `IosPushDelegateBridge` (`applyPushMessage`, `messageIdSync`), `MarkAsReadReceiver`, `ReplyReceiver`. Also update the KDoc reference in `SafeNetworkCall.kt:25`.
- Move order inside the phase: (a) reads + simple actions, (b) edit/delete/react/poll, (c) send + offline queue, (d) push apply. **Test send/offline/dedup manually on device after (c)** (`MESSAGE_SEND_SYNC_REBUILD_PLAN.md` invariants), and push receive / reply / mark-as-read after (d).

**(a) + (b) done (2026-10-14, commit "Phase 5a").** Notes for the rest of Phase 5:
- `D/MessageRepository` / `chat/data/OfflineFirstMessageRepository` hold the reads, media fetches (`getPicturesForMessageIds`, `getAudiosForMessageIds`, `fetchMissingMessageMedia()`), `deleteMessagesForGroups(ids)` and edit/delete/react/poll. The impl has no dispatcher or `externalScope` yet (none of the moved code hardcodes one); Koin binds it with `singleOf`. When (c) adds `ioDispatcher` (for the `withContext(Dispatchers.IO)` in `sendMessage`) and `externalScope`, switch to an explicit lambda like `OfflineFirstChatListRepository`.
- `getMissingAudios` is folded into `fetchMissingMessageMedia()` (image check + downloads, then audio check + downloads, each in its own `coroutineScope`); `AppRepository.getMissingPics` launches the three `fetchMissing*()` calls and `dataSync`'s MEDIA job only calls `getMissingPics()`. Only difference: the audio downloads now overlap with the user/group picture downloads instead of starting after them.
- Still on `AppRepository` and still using `MessageLocalDataSource`/`AppDatabase`/`PictureManager`/`AudioManager`/`ErrorChannel` there: `sendMessage`, `sendOfflineMessages`, `messageIdSync`, the three locks, `applyPushMessage`/`applyPushReaction`, `setAllChatMessagesRead`. They call the media fetches through the `AppRepository` delegates; once moved, call the impl's own methods. When the last of them leaves, drop those constructor params from `AppRepository`.
- `Message`, `MessageReader`, `Reaction`, `MessageType`, `MessageMinimal`, `isNewerThan`, `PollMessage`/`PollVoteOption`/`PollVoter`/`PollVisibility`, `SystemEvent*` and `MessageContent` are in `D/model/`; Room mappers in `chat/data/dtos/MessageDtoMapper.kt`. `ReaderUi` stays in `chat/domain`.
- R3: `NewPoll` / `NewPollOption` / `PollVote` (`D/model/`), mapped in `requestResponseDataClasses/PollRequestMapper.kt`. `MessageContent.PollContent` carries a `NewPoll`; `sendMessage` maps it with `toPollCreateRequest()` for the network call, `toLocalPollMessage()` now takes a `NewPoll`, and the offline queue rebuilds it with `PollMessage.toNewPoll()` (was `toCreateRequest()`).
- `SendPathFixture` builds a real `OfflineFirstMessageRepository` and passes it to `AppRepository`; for (c) point `repository` at it.

**(c) done (2026-10-14, commit "Phase 5b").** Notes for the rest of Phase 5:
- `sendMessage`, `sendOfflineMessages`, `messageIdSync` (now `syncMessages(fetchMedia: Boolean = true)`) and the three locks are in `OfflineFirstMessageRepository`, moved verbatim; they call the impl's own media fetches. The impl now also takes `AppDatabase` (the send path uses `messageDao()` directly, like `OfflineFirstUserRepository`), `externalScope: CoroutineScope` and `ioDispatcher: CoroutineDispatcher = Dispatchers.IO`. Koin binds it with an explicit lambda, `externalScope = get<ApplicationScope>()`.
- R2: `sendMessage`'s body runs as `externalScope.async { … }.await()`, created before any suspension point. `ChatViewModel`'s four send sites are `viewModelScope.launch { messageRepository.sendMessage(…) }`; `viewModelScope` is `Main.immediate`, so the `async` exists before the ViewModel can be cleared. `ChatViewModel` still injects `ApplicationScope` for `setAllMessagesRead` only.
- `sendOfflineMessages` and `syncMessages` do **not** use `externalScope`: the drain's callers are `GlobalViewModel` (app-wide `single`, never cleared) and `ChatSelectorViewModel.refresh` (screen-scoped, cancelled with the screen today), so wrapping it would change when a drain stops. Each resend goes through `sendMessage`, so a resend in flight finishes even if the drain is cancelled. `syncMessages` gets its scope with the `DataSyncCoordinator` (Phase 6) or mark-read (5c).
- Still on `AppRepository`: `applyPushMessage`/`applyPushReaction` (use `messageLocalDataSource`) and `setAllChatMessagesRead` (uses `networkUtils`, then `messageRepository.syncMessages()`). `AppRepository` dropped `AppDatabase`, `PictureManager`, `AudioManager` and `ErrorChannel` (no user left); `dataSync` calls `messageRepository.syncMessages(fetchMedia = false)`. Delegates `sendMessage` / `sendOfflineMessages` / `messageIdSync` stay until Phase 8.
- Callers switched: `ChatViewModel` (send), `GlobalViewModel` and `ChatSelectorViewModel` (`sendOfflineMessages`), `ReplyReceiver` (`sendMessage`), `IosPushDelegateBridge` (`syncMessages()`). Still on `AppRepository`: `ReplyReceiver` + `MarkAsReadReceiver` + `ChatViewModel` (`setAllChatMessagesRead`), `AppFirebaseMessagingService` + `IosPushDelegateBridge` (`applyPushMessage`).
- Tests: `SendPathFixture(testScope)` builds `OfflineFirstMessageRepository` alone (no `AppRepository`), with `externalScope = backgroundScope` and `ioDispatcher` on the test scheduler. The test classes build it inside `runTest` via a private `runSendPathTest { }` (still `StandardTestDispatcher`); assertions unchanged.

**(d) done (2026-10-14, commit "Phase 5c"). Phase 5 is complete.** Notes for Phase 6:
- `applyPushMessage` (+ private `applyPushReaction`) and `setAllChatMessagesRead` are in `OfflineFirstMessageRepository`, moved verbatim. `setAllChatMessagesRead`'s body runs as `externalScope.async { … }.await()` (R2) and calls the impl's own `syncMessages()`.
- Mark-read in `ChatViewModel` is only triggered from its own `viewModelScope` collectors (app resumed, chat back on screen, messages list changed), never from `onCleared`. A plain `viewModelScope.launch` would still drop a request that is queued behind the mutex while an earlier one is in flight, once the screen closes. So the mutex + `lastMarkedReadMessageId` moved, unchanged, into a per-screen `D/model/MarkReadGuard`, and the guarded block moved into the non-suspending `MessageRepository.markChatReadInBackground(chatid, gruppe, guard, readNewestMessageId)`, which runs it in `externalScope.launch`. The ViewModel owns the guard, so the dedup state lives exactly as long as before. `ChatViewModel` no longer injects `ApplicationScope`.
- `AppRepository` dropped `NetworkUtils` and `MessageLocalDataSource` (12 positional `get()`s). No file outside `AppRepository.kt` calls a message method on it any more (the only hit is the commented-out resume handler in `GlobalViewModel`, Todo).
- Callers switched: `ChatViewModel`, `MarkAsReadReceiver`, `ReplyReceiver` (mark-read), `AppFirebaseMessagingService`, `IosPushDelegateBridge` (`applyPushMessage`).
- Still on `AppRepository` for Phase 6: `dataSync`, `dataSyncLock`, `dataSyncState` + `updateSyncJob`, the `DataSync*` types and `getMissingPics` (it only launches the three `fetchMissing*()` calls). `dataSync` callers: `GlobalViewModel` (2, plus one in the commented-out resume handler), `ChatSelectorViewModel`, `GroupCreatorViewModel`, `EventsViewModel`, `EmailVerifiedCheckViewModel` (2), `LoginViewModel`, `SignUpViewModel`, `SchneaggmapViewModel`, `MiscSettingsViewModel`, `PrivacyAndSecurityViewModel`, `UserSettingsViewModel` (4), `SocketConnectionMessage`, `IosPushDelegateBridge.onNotificationTap`. `dataSyncState` / `DataSync*` types: `ChatSelector.kt`, `DataSyncDetailPopup.kt` (Phase 7). `applicationScope.launch` around a sync: `LoginViewModel`, `SignUpViewModel`, `EmailVerifiedCheckViewModel` (2), `EventsViewModel`, `MiscSettingsViewModel`.

### Phase 6: Sync coordinator
- `DataSyncCoordinator` takes over `dataSync`: order, lock, `DataSyncState` / `DataSyncJob*`, logging, the purge via `deleteMessagesForGroups`, and the MEDIA job via the three `fetchMissing*()` calls. Keep today's parallelism (`coroutineScope` + `launch` per kind).
- The `DataSync*` types move to `D/`.
- `executeSync` is dead code and is not moved.
- R2: the sync body runs in `externalScope`. The `applicationScope.launch { dataSync(…) }` wrappers in `LoginViewModel`, `SignUpViewModel`, `EmailVerifiedCheckViewModel` (2×), `EventsViewModel` and `MiscSettingsViewModel` become plain calls from `viewModelScope`, and those ViewModels drop `ApplicationScope`. `EmailVerifiedCheckViewModel` keeps waiting for the sync result as today.
- Callers: every VM that calls `dataSync` (Login, SignUp, Events, Map, UserSettings, EmailVerifiedCheck, Misc, Privacy, ChatSelector, GroupCreator, GlobalViewModel), `SocketConnectionMessage` and `IosPushDelegateBridge`. `ChatSelector.kt` (`dataSyncState`) and `DataSyncDetailPopup.kt` move in Phase 7. Update the KDoc on `updateUserLastSeenNow` in `C/chat/data/UserLocalDataSource.kt` (was `UserRepository.kt:161`, renamed in Phase 0).

**Done (2026-10-14, commit "Phase 6").** Notes for Phase 7:
- `D/DataSyncCoordinator` (`dataSyncState`, `dataSync(reason)`) / `app/data/DefaultDataSyncCoordinator` hold `dataSync`, `dataSyncLock` (now private), `dataSyncState` + `updateSyncJob` and `getMissingPics` (private, the MEDIA job), moved verbatim. The body runs as `externalScope.async { … }.await()` (R2), so it now always runs on `ApplicationScope`'s `Dispatchers.IO`. Koin binds it with an explicit lambda, `externalScope = get<ApplicationScope>()`; it depends on the five repositories + `Logger` only.
- `DataSyncJobStatus` / `DataSyncJobType` / `DataSyncJobState` / `DataSyncState` are in `D/model/DataSyncState.kt`. `DataSyncJobType.toUiText()` is an extension in `app/DataSyncJobTypeText.kt` (keeps Compose resources out of `D/`); `DataSyncDetailPopup.kt` imports it.
- The former `applicationScope.launch { dataSync(…) }` wrappers (Login, SignUp, EmailVerifiedCheck ×2, Events, Misc) and GroupCreator's `CoroutineScope(Dispatchers.IO).launch` are `viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { dataSyncCoordinator.dataSync(…) }`: the `async` is created synchronously inside the `launch` call, whatever thread calls it and even if the ViewModel is cleared right after. No ViewModel injects `ApplicationScope` any more.
- `AppRepository` keeps `dataSyncState` / `dataSync` delegates; it dropped `Logger` (last user moved). Still on it: `ChatSelector.kt` (`dataSyncState`, `appVersion`), `Login.kt` / `SettingsScreen.kt` (`appVersion`), `UrlChangeDialog.kt` (`testServer`), `App.kt` (`loadSavedLoginConfig`, `logout`, `appVersion`), and five ViewModels for `appVersion` only: `ChatViewModel`, `ChatSelectorViewModel`, `NewChatViewModel`, `EventsViewModel`, `SharedSettingsViewmodel`.

### Phase 7: Composables + clean ViewModel boundary
- Do §2.5 in one go: `Login.kt`, `ChatSelector.kt`, `SettingsScreen.kt`, `UrlChangeDialog.kt` (+ its hosts), `DataSyncDetailPopup.kt`, plus `MessageChatSelector.kt`, `EmailVerifiedCheckScreen.kt`, `SignUp.kt`, `UserSettings.kt` (they also `koinInject` `Preferencemanager`; found in Phase 1a), `ComboAnnotationSources.kt` (pulls local data sources from Koin; found in Phase 1b) read from their ViewModels; `App.kt` swaps to interfaces. Grep for any other composable that `koinInject`s a data-layer class.
- ViewModels that still read `appRepository.appVersion` for platform checks inject the `AppVersion` interface directly (six of them, found in Phase 1b).
- Grep check: no file under `*/presentation/` imports `datasource.` (except `datasource.domain`), `.data.`, `AppDatabase`, `NetworkUtils` or `Preferencemanager`, and no composable `koinInject`s a data-layer class (except the `App.kt` interfaces, §2.5). Fix whatever is left. Also: no ViewModel injects `ApplicationScope` (R2), and no presentation file uses a `NetworkUtils.*` type (R3). The grep doesn't catch ViewModels that still inject `LocationService` / `PictureManager` / `AudioManager` / `BatteryService`; those are a Todo item (R10).

**Done (2026-10-14, commit "Phase 7").** Notes for Phase 8:
- Composables now read from their ViewModels:
  - `Login.kt` → `LoginViewModel`: `versionName` / `versionCode` (from `AppVersion`), `savedServerUrl` (live `StateFlow` for the website and password-reset links), the German-system-language dialog (`showVoriLanguageDialog`, `onLanguageSelected`, check moved from the `LaunchedEffect` into `init`; `LanguageService` injected, R9). `VersionText` takes the two strings.
  - `ChatSelector.kt` / `MessageChatSelector.kt` → `ChatSelectorViewModel`: `dataSyncState` (from `DataSyncCoordinator`), `isDesktop`, `onboardingCompleted`, `highlightTodaysTimestamp`, `serverUrl`. `isMobile` is only used by a commented-out block, so it was not added (Todo). `SettingsRepository` got `getOnboardingSeenFlow()` (pass-through).
  - `SettingsScreen.kt` → `SettingsViewModel.versionName` / `versionCode`.
  - `EmailVerifiedCheckScreen.kt` / `SignUp.kt` → `serverUrl` `StateFlow`s in their ViewModels (`SignUpScreen` takes it as a parameter, so the hand-written `SignupState.equals` stays untouched). `UserSettings.kt` only dropped its unused `Preferencemanager` injection.
  - `UrlChangeDialog` takes `checkStatus` + `onTestServer` / `onUrlEdited`; `CheckStatus` became public. `LoginViewModel` and `DevSettingsViewModel` run the test (`AppInfoRepository.testServer`) in a child scope of `viewModelScope`; the hosts call `resetServerCheck()` when opening and closing the dialog, which cancels running tests and resets to idle, as the dialog's own `remember` state and composition scope did. The text field buffer and the `https://` prefixing stay in the dialog.
  - `DataSyncDetailPopup.kt` needed nothing (Phase 6 already switched its imports).
- `App.kt` injects only `SettingsRepository` (was `Preferencemanager`), `AuthRepository`, `SessionTerminator`, `AppVersion`, `Logger`, `LanguageService`, the channels (+ `Navigator`, `GlobalViewModel`, the settings ViewModels as before). `GlobalViewModel` is unchanged.
- `ChatViewModel`, `ChatSelectorViewModel`, `NewChatViewModel`, `EventsViewModel`, `SharedSettingsViewmodel` inject `AppVersion` instead of `AppRepository` (Koin lambdas of `ChatViewModel` / `EventsViewModel` updated).
- `ThemeSetting`, `LanguageSetting`, `MapStyleSetting` moved verbatim (UI members included) from `datasource/preferences/` to `D/model/`, so no presentation file imports `datasource.preferences` any more. Three games files got the mechanical import change. `PinnedChat` stays in `Preferencemanager.kt` (not a presentation import; Todo).
- Deviations:
  - The `runBlocking { getServerUrl() }` reads in `Login.kt`, `ChatSelector.kt` and `SignUp.kt` became reads of an eagerly started `StateFlow` (initial `BASE_SERVER_URL`, the same fallback `getServerUrl()` uses). Only difference: a tap in the first milliseconds after the ViewModel is created would see the fallback; the UI thread is no longer blocked.
  - The preference flows the composables collected with an initial value are now `stateIn(WhileSubscribed(5_000))` with the same initial value, so returning to the screen within 5 s keeps the last value instead of flashing the initial one.
  - `ComboAnnotationSources.kt` (shared composable behind every `ComboText` / `ComboInputField`) still resolves from Koin, now the `MapRepository` / `UserRepository` / `EventRepository` interfaces instead of the local data sources. Feeding it from every host ViewModel would touch every caller; Todo.
- Grep results: no presentation file imports `Preferencemanager`, `AppDatabase`, `NetworkUtils`, `datasource.preferences` or a `.data.` class, except `games/` (left alone, Todo) and `ChatDetailsViewmodel` / `FeedbackViewModel` importing `datasource.network.util` result types the interfaces still return (D3, Todo). No composable `koinInject`s a data-layer class outside `App.kt`; composables still resolving platform services (`AppVersion`, `ShareUtils`, `BatteryService`, `PictureManager`, `AudioManager`) are listed in Todo. No ViewModel injects `ApplicationScope`; no presentation file uses a `NetworkUtils.*` type.
- Remaining `AppRepository` references: `di/Modules.kt` (import + the `single`), and only comments elsewhere: the commented-out resume handler in `C/app/GlobalViewModel.kt:173-174`, the commented-out map button in `C/chat/presentation/chatselector/ChatSelector.kt:370`, and KDoc/history notes in `DataSyncJobTypeText.kt`, `DefaultDataSyncCoordinator.kt`, the `OfflineFirst*Repository` impls and the `jvmTest` send-path tests. Nothing resolves `AppRepository` at runtime any more, so Phase 8 can delete the class and its `single` without touching a caller; decide what to do with the two commented-out blocks.

### Phase 8: Delete `AppRepository`
- Remove the delegating shells and the class (incl. the commented-out `executeSync` block), and update `di/Modules.kt`.
- Update the cross-plan references (§5).

**Done (2026-10-14, commit "Phase 8").** Notes for Phase 9:
- `datasource/AppRepository.kt` is deleted (delegating shells, the class, the commented-out `executeSync` block). Nothing had to be moved first: every member was a pure delegate. The only one with logic, `applyServerSettings(PersonalUserSettings)` (mapped with `toSyncedSettings()`), had no caller left; `OfflineFirstUserRepository.syncUsers` and `SocketConnectionMessage` already call `settingsRepository.applyServerSettings(….toSyncedSettings())` themselves.
- `di/Modules.kt` lost the import and the `single`. No other Koin definition (common module, platform modules, `androidApp`, `iosApp`) names `AppRepository`, and no constructor takes it, so no positional `get()` could have resolved it.
- Comments:
  - Commented-out resume handler in `C/app/GlobalViewModel.kt:173-174`: rewritten 1:1 to the injected `messageRepository.sendOfflineMessages(ownId)` / `dataSyncCoordinator.dataSync(reason = "appResumed")`. The block stays (dead code, Todo).
  - Commented-out map button in `C/chat/presentation/chatselector/ChatSelector.kt:370`: left as is, because `appRepository.appVersion.isMobile()` has no 1:1 replacement (`ChatSelectorViewModel` only has `isDesktop`). Todo.
  - No KDoc named `AppRepository` as the current location. The remaining mentions are history notes ("moved verbatim from AppRepository …", "was AppRepository.getMissingPics", "on `AppRepository` until Phase 5b") in `DataSyncJobTypeText.kt`, `DefaultDataSyncCoordinator.kt`, the four `OfflineFirst*Repository` impls (chat list, group, message, user) and the four `jvmTest` send-path files. They stay.
- Remaining `AppRepository` / `appRepository` mentions in `composeApp`, `androidApp`, `iosApp`: those history notes, the commented-out map button, and the git-ignored `androidApp/release/mapping.txt` (R8 output of an older release build, regenerated by the next one).
- Cross-plan edits (§5):
  - `CHAT_BASED_MIGRATION_PLAN.md`: order note at the top (this plan → `plans/Todo.md` follow-ups → chat migration → E2E V2). §1.2 (`chatKey`, search) and the §4.1 bullet now point to `OfflineFirstMessageRepository` (send, send-echo, sync, push apply, mark-read), `OfflineFirstChatListRepository` (chat list, search, `chatKey`, pins), `OfflineFirstGroupRepository` (`syncGroups`, `createGroup`), `OfflineFirstUserRepository` (`sendWake`), `DefaultDataSyncCoordinator` (purge) and `SocketConnectionMessage`, with current line numbers. The target name `chatIdSync` was kept. §4.4 now lists the Phase 4.5 send-path tests, the `commonTest/…/fakes/` and the Phase 9 tests as "update to `chatId`". §8.6 note re-pointed. Other class names that the migration renamed in that plan are in Todo.
  - `E2E_ENCRYPTION_PLAN_V2.md`: the boundary sites point to `OfflineFirstMessageRepository` (and current `SocketConnectionMessage` lines). `E2eEncryptionManager` is an interface + one Koin `single`, injected into the `OfflineFirstMessageRepository` and `RemoteAuthRepository` constructors, so tests use a `FakeE2eEncryptionManager`. Login, autologin and password change point to `RemoteAuthRepository`, and `deleteAllAppData` to `LocalSessionTerminator`.
  - `GROUP_EXTENSIONS_PLAN.md`, `MAP_LOCATION_RATINGS_PLAN.md` (not implemented yet) were written for the old split (Room-only `*Repository` + `AppRepository`). Their passages now name the local data source, the offline-first repository and `DefaultDataSyncCoordinator` instead. This is a rename only, with one consequence: the group extensions purge after the sync goes through a narrow repository cleanup method, the same pattern as `deleteMessagesForGroups` (§2.1). Re-check both before implementing (Todo).
  - Left as history (Todo lists the lines): `AUTH_SESSION_REBUILD_PLAN.md` (has the user's uncommitted edits, not touched), `E2E_ENCRYPTION_PLAN.md` (v1, superseded), `MESSAGE_SEND_SYNC_REBUILD_PLAN.md` (implemented), `OFFLINE_FIRST_ANALYSIS.md` (dated analysis), `EVENT_RSVP_PLAN.md` (looks implemented as `setEventParticipation`).
- Verified: `compileKotlinJvm`, `compileAndroidMain`, `compileKotlinIosSimulatorArm64`, `jvmTest` (14 tests: the 13 send-path tests + the benchmark) all green.
- For Phase 9:
  - The send-path tests and their fixture (`jvmTest/…/testutil/SendPathFixture.kt`, `FakeMessageServer.kt`) already build a real `OfflineFirstMessageRepository`. Reuse them for the `OfflineFirstMessageRepository` tests instead of writing a second fixture.
  - The chat migration (§4.4 there) will move every fake and Phase 9 test to `chatId`. So keep the fakes thin: one per interface in `commonTest/…/fakes/`, mirroring the interface signatures.
  - Device test for this phase: Todo checklist, "Phase 8".

### Phase 9: Safety-net tests before the follow-ups and the chat migration (D6)
- **ViewModel tests with fakes**: `ChatSelectorViewModel` (list, search, pins, unread badge), `ChatViewModel` (send, edit, delete, react, mark read, offline state), `ChatDetailsViewmodel` (group edits, friend actions), `NewChatViewModel`, `GroupCreatorViewModel`.
- **Repository tests** (`jvmTest`, in-memory Room + `MockEngine`):
  - `OfflineFirstChatListRepository`: DM vs group grouping, last message, unread count with friendship gating.
  - `OfflineFirstMessageRepository`: send success, send echo dedup by `clientMessageId`, offline queue retry, `applyPushMessage`.
  - `DefaultDataSyncCoordinator`: order, lock re-entry, state transitions, group purge after the parallel jobs.
- Fakes live in `commonTest/…/fakes/` (`FakeMessageRepository`, `FakeErrorChannel`, the platform service fakes, …) and are reused by the follow-ups and the chat migration. `Navigator` is used for real (R9). Concurrency tests use `StandardTestDispatcher` (R12).
- The Phase 4.5 send-path tests are already there; Phase 9 adds the rest.

**Done (2026-10-14, commit "Phase 9").** `jvmTest` runs 80 tests (was 14).
- Fakes in `commonTest/…/fakes/`, one per interface the tested classes need, mirroring its signatures: `FakeMessageRepository`, `FakeUserRepository`, `FakeGroupRepository`, `FakeChatListRepository`, `FakeSettingsRepository`, `FakeDataSyncCoordinator`, `FakeAuthRepository`, `FakeAppInfoRepository`, `FakeEventRepository`, `FakeMapRepository`, `FakeErrorChannel`, `FakeActionChannel`, and for the platform services `FakeAppVersion`, `FakeNotifier`, `FakePermissionManager`, `FakePictureManager`, `FakeAudioManager`, `FakeShareUtils`. Reads serve public `MutableStateFlow`s. Writes are recorded in a thread-safe `CallLog`, which can also wait for a call made on another thread. The sync/send entry points run replaceable `on…` hooks, so a test can hold a call on a gate or make it fail. No fakes for `SessionTerminator` or `LanguageManager`: nothing tested here needs them.
- `commonTest/…/testutil/`:
  - `TestViewModelStore` + `runViewModelTest`: clear the ViewModels the way a screen does, then wait for their coroutines. Needed because a cancelled `flowOn(Dispatchers.Default)` producer otherwise resumed on the next test's `Dispatchers.Main`.
  - `awaitMatching`: Turbine with a wall-clock timeout, for the ViewModel flows that hop to `Dispatchers.Default`.
  - `startTestKoin`: a global Koin with only a `Notifier`, for the `KoinPlatform` lookups in `ChatSelectorViewModel` and `NotificationManager`.
  - `TestModels.kt`: model builders.
  - `resetGlobalState()` now also resets `OpenChatTracker`.
- `jvmTest/…/testutil/TestNetworkUtils.kt` builds the real `NetworkUtils` on a `MockEngine` client; `SendPathFixture` uses it now.
- ViewModel tests (`commonTest`, `MainDispatcherRule` + `resetGlobalState()`):
  - `ChatSelectorViewModelTest` (10): list, login switch, in-memory search, unread filter, debounced message search, pin toggle, pending-request badge, refresh order and re-entry, push token.
  - `ChatViewModelTest` (12): send (reply, blank, logged out), edit, delete (sent vs. queued), reaction, message order, mark-read on resume (guard, notification cancel), partner online state, draft.
  - `ChatDetailsViewmodelTest` (10): group details, group edits, member actions and picker, user details, friend removal, nickname/description, friend request, wake.
  - `NewChatViewModelTest` (8).
  - `GroupCreatorViewModelTest` (6) is in `jvmTest`, because the group picture goes through the platform `GalleryPhotoResult.loadBytes()`.
- `DefaultDataSyncCoordinatorTest` (7) is in `commonTest`, not `jvmTest`. The coordinator depends only on interfaces, so fakes replace Room and `MockEngine`. It covers:
  - the five jobs, then the purge, then the MEDIA job, and the message sync without media;
  - the purge waiting for the parallel message sync;
  - state transitions, and a failed job plus the reset on the next run;
  - lock re-entry;
  - a cancelled caller (R2);
  - logged out.
  It runs on one `StandardTestDispatcher` with `externalScope = backgroundScope` and is driven with `runCurrent()`, because `advanceUntilIdle()` stops once only background work is left.
- `jvmTest` repository tests:
  - `OfflineFirstChatListRepositoryTest` (6): DM vs. group, last message and counters (incl. unsent), unread badge gating (accepted friends and stored groups only, no SYSTEM lines), pin order, `setPinnedChat` push, pending requests.
  - `MessagePushApplyTest` (7): `applyPushMessage`.
  - The Phase 4.5 tests already built `OfflineFirstMessageRepository` directly (since Phase 5b) and stay unchanged.
- Verified: the three compile tasks, plus `compileTestKotlinIosSimulatorArm64`, which compiles the shared tests for iOS. Kotlin/Native rejects `,` in backtick test names, so the names were changed. The iOS test task itself was not run. Three full `jvmTest` runs and five more runs of the new tests were all green; no flaky test.
- Not covered:
  - `ChatViewModel`: image/audio/poll sends, recording, paging, and the auto mark-read on a list change (a 1.5 s delay after an emission from a `Default` thread; racy without a dispatcher seam).
  - Chat selector startup popups and the desktop update check.
  - Chat details: group picture, event detach, shared-content counts.
  - Chat list message search.
  - Every other ViewModel and repository (Todo "Add tests to everything").
- Production code: unchanged, no test seams added.
- Bugs: no tested path misbehaved; the unread-badge friendship gating is correct. What the tests ran into is in Todo "Findings from migration Phase 9", e.g. a 25-character group name that can't be created but shows no error (pinned by a test), a lost update in `GroupCreatorViewModel`'s name validation, `runBlocking` in `ChatViewModel`, and a check-then-act in the sync lock.

---

## 4. Risks
- **Send/sync regressions (Phase 5/6).** These are the paths that just got fixed (duplicate sends, event overwrite on read). Mitigation: move the code verbatim, no clean-ups in the same commit, device test after each sub-step, Phase 9 tests.
- **Unattended implementation.** Phases may be implemented while nobody can launch the app. Compile + `jvmTest` (D20) catch build errors and the tested paths only. Every "test on device" step is collected in the device-test checklist in `plans/Todo.md` for the user.
- **Koin graph cycles.** The two known ones are resolved by D16 and D17. `SessionTerminator` now takes `Notifier` and `SocketConnectionManager` by constructor instead of `KoinPlatform`; `SocketConnectionManager` only needs the socket client and `AuthSessionManager` today, so no cycle, but recheck. Keep the dependency direction from §2.1. Koin fails at first resolve, so every phase needs a launch on all 3 platforms.
- **Push entry points resolve classes at runtime** via `KoinPlatform.getKoin()` (§1.4). A missing binding fails only when a push, reply or mark-as-read arrives. Test those on Android and iOS after Phases 2 and 5.
- **Merge conflicts on the long branch (D11).** `AppRepository` is a conflict magnet. Merge `main` into the branch after every phase and keep other work off `AppRepository` while the branch is open.
- **Plan drift**: line numbers in the chat and E2E plans go stale (§5).

## 5. Cross-plan edits after Phase 8
- `CHAT_BASED_MIGRATION_PLAN.md`:
  - §4.1 "AppRepository" bullet (:196) and §8.6 note (:343): re-point to `OfflineFirstMessageRepository` (send, send-echo, sync, push apply), `OfflineFirstChatListRepository` (`chatKey`, chat list, search) and `SocketConnectionMessage`.
  - §4.4 "Tests to touch" already exists and says "No new tests unless requested". Extend it: update the fakes + Phase 9 tests to `chatId`.
  - Note the new order (Todo follow-ups run before it).
- `E2E_ENCRYPTION_PLAN_V2.md`:
  - :31 boundary sites → `MessageRepository` impl.
  - :114 `E2eEncryptionManager` goes into `OfflineFirstMessageRepository` + `RemoteAuthRepository` constructors (not AppRepository), behind an interface so ViewModel/repository tests use `FakeE2eEncryptionManager`.
  - :117 `AppRepository.login` → `RemoteAuthRepository.login`.

## 6. Effort
- Phases 0–8: ~8–10 sessions (Phase 0 grew with D7/D8/D10/D19, Phase 7 with §2.5). Phase 5 alone is ~1–2.
- Phase 9: ~2 sessions.
- One long branch, one or more commits per phase. After each phase Claude runs the compile tasks + `jvmTest` (D20); the user launches on Android, iOS and Desktop.

---

## 7. Decisions (all answered 2026-10-13)

| # | Question | Answer |
|---|---|---|
| D1 | Rename the existing Room-only `*Repository` classes to `*LocalDataSource` / `Room*DataSource`? | **Yes.** |
| D2 | Split `NetworkUtils` into per-feature remote data sources now? | **Not in this plan.** First follow-up after it, before the chat migration (`plans/Todo.md`). Repositories are tested via `MockEngine` meanwhile. |
| D3 | Convert `Boolean` / nullable returns to `Result<T, DataError>` now? | **Not in this plan.** Follow-up after it, before the chat migration (`plans/Todo.md`). |
| D4 | Where do the interfaces live? | **Shared + feature split**: cross-feature interfaces in `datasource/domain/`, feature-only ones (`ChatListRepository`, `FeedbackRepository`) in their feature `domain`. Follows the skill rule "features never import from each other" (§2.2). |
| D5 | Test stack: `kotlin-test` runner + Turbine + AssertK in `commonTest`, Room/MockEngine tests in `jvmTest`? | **Yes.** |
| D6 | Phase 9 (safety-net tests) part of this migration? | **Yes.** |
| D7 | Convert the platform `expect class`es to interfaces? | **Yes, 9 of them**: the original six + `ShareUtils`, `LanguageManager`, `Notifier`. |
| D8 | Keep `Navigator`, `ApplicationScope` and `LanguageService` concrete? | ~~Interfaces for all three~~ → **revised by R9 (user-confirmed 2026-10-13): all three stay concrete**; ViewModels stop injecting `ApplicationScope` (R2). |
| D9 | `LoggingRepository` → `Logger` interface? | **Yes**, in Phase 0. |
| D10 | Keep `ErrorChannel` / `ActionChannel` as global objects? | **No: injected in this plan** (Phase 0). |
| D11 | One PR per phase, or one long-running branch? | **One long branch.** |
| D12 | Should the migration add the missing media wipe on logout? | **No.** Logout moves verbatim; the wipe is a Todo item. |
| D13 | Composables that `koinInject` data-layer classes? | **Move their data into their ViewModels** (§2.5). `App.kt` keeps `koinInject`, interfaces only. |
| D14 | Split `GlobalViewModel` in this plan? | **No.** Todo item; it must not grow in this plan. |
| D15 | Where do the deferred refactors run? | **Before** the chat and E2E plans (see Order). |
| D16 | 11 user/group mutations call `dataSync()` after success (repo ↔ coordinator cycle). How to break it? | **Revised by R1 (user-confirmed 2026-10-13): call the repository's own `syncUsers()` / `syncGroups()` instead.** Remove those calls too once `SchneaggchatV3server/Todo.md` confirms the socket pushes. |
| D17 | `logoutAllDevices` → `logout` (AuthRepository ↔ SessionTerminator cycle)? | **`SessionTerminator` owns `logoutAllDevices()`**; `AuthRepository` only keeps the server call. |
| D18 | Move safety-net tests before Phase 5? | **Partly (user-confirmed 2026-10-13):** send-path characterization tests in Phase 4.5, run again after the change. Everything else stays in Phase 9. |
| D19 | Global objects `SessionCache`, `SnackbarManager`, `AppLifecycleManager`, `IncomingDataManager`? | **Stay global**; Phase 0 adds a reset helper for tests. Injecting them is a Todo item. |
| D20 | May Claude run Gradle during this migration? | **Yes: compile tasks + `jvmTest`** after each phase. No assemble/run/install. |
| D21 | When do the composables move (§2.5)? | **Phase 7**, all at once, straight to the final interfaces. |

Follow-ups and open items formerly listed here live in `plans/Todo.md`.

---

## 8. Android-standards review (2026-10-13)

An independent agent checked the plan against developer.android.com (architecture guide, recommendations, data/domain/UI layer, offline-first, coroutines best practices and testing, modularization) and Now in Android. Sources: DL = …/topic/architecture/data-layer, OF = …/data-layer/offline-first, REC = …/topic/architecture/recommendations, SH = …/ui-layer/stateholders, EV = …/ui-layer/events, CBP = developer.android.com/kotlin/coroutines/coroutines-best-practices, CT = …/kotlin/coroutines/test.

| # | Finding | Decision |
|---|---|---|
| R1 | D16 accepted stale UI; for online-only writes OF says "if successful, update the local data source". | **Applied** (user-confirmed): own `syncUsers()` / `syncGroups()` after each mutation. |
| R2 | Work that must outlive a screen is launched from ViewModels via `applicationScope`; DL/CBP put it in the data layer with an injected `externalScope`. | **Applied** (user-confirmed): repositories/coordinator take `externalScope`; ViewModels drop `ApplicationScope` (Phases 5, 6). |
| R3 | Network DTOs (`NetworkUtils.*`, 8 types in 9 presentation files) would leak into `D/` interfaces; OF and the house data-layer skill forbid that. | **Applied**: domain models + mappers in the phase that creates each interface. |
| R4 | Hardcoded `Dispatchers.*` in classes Phase 9 tests (CBP: don't hardcode dispatchers). | **Applied** for ChatList and Message repositories, with defaults that keep today's behavior. |
| R5 | `SocketConnectionMessage` writes to local data sources directly (DL: only the source of truth changes data). | **Deferred** (user-confirmed): routing through repositories adds locking; the chat migration rewrites the handler. `plans/Todo.md`. |
| R6 | `ErrorEvent` has a `@Composable` member; repositories push UI errors (DL/EV: return errors to the caller). | **Applied**: `ErrorEvent` Compose-free; repository → `ErrorChannel` marked temporary until the `Result` follow-up. |
| R7 | Shared `D/` interfaces would import models from the chat feature (NiA: `core:model`, core never depends on features). | **Applied**: models move with their interface to `D/model/`. |
| R8 | `App.kt` handles `AuthInvalidated` with logout + navigation; SH/EV prefer a data-layer component and state-driven navigation; `GlobalViewModel` as an app-wide `single`. | **Deferred** (user-confirmed): navigation/behavior risk. `plans/Todo.md`; the `GlobalViewModel` split targets data-layer managers. |
| R9 | Interfaces for `Navigator`, `ApplicationScope`, `LanguageService` go beyond Google/NiA practice. | **Applied** (user-confirmed): all three stay concrete. |
| R10 | ViewModels inject `LocationService`, `PictureManager`, `AudioManager`, `BatteryService` (REC: ViewModels shouldn't talk to data sources like GPS). | **Deferred**: `plans/Todo.md` (location and media repositories). |
| R11 | `AppVersion` interface package unspecified; it sits in `settings/data`. | **Applied**: `utilities/`. |
| R12 | `UnconfinedTestDispatcher` everywhere (CT: `StandardTestDispatcher` for concurrency). | **Applied** for lock/dedup/sync-order tests. |

Review questions:
- **Q1 one-off events** (REC: don't send events from ViewModel to UI; house MVI skill uses `Channel`): **keep Channels**; app-level state for the global error/action events is a Todo item.
- **Q2 test timing**: **send-path tests before Phase 5** (Phase 4.5), user-confirmed; message sending gets a rebuild later (Todo).
- **Q3 work that must survive process death** (Google: WorkManager, not usable in KMP common code): **keep in-process app-scope work**; a platform background scheduler (WorkManager / BGTaskScheduler) is a Todo item.
- **Q4 D8 interfaces**: see R9.

What the review confirmed as right: `OfflineFirst*` / `Default*` repository interfaces bound in DI (as NiA's `DataModule`), `XLocalDataSource` + per-repository `syncX()` run by one orchestrator (as NiA's `Syncable` / `SyncManager`), `DataSyncCoordinator` in the data layer (it holds a lock and state, so not a use case), composables reading only from ViewModels, fakes over mocks with Turbine + AssertK.

