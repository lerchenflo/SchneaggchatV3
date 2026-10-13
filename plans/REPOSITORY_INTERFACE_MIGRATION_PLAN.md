# Repository Interface Migration Plan

Splits the god object `AppRepository` (`C/datasource/AppRepository.kt`, ~2,990 lines, 14 constructor deps, used by ~30 files) into feature repositories and coordinators. Each one has an **interface in its feature's `domain` package**, so every ViewModel depends only on interfaces and can be unit-tested with in-memory fakes.

**Order**: this plan → `CHAT_BASED_MIGRATION_PLAN.md` → `E2E_ENCRYPTION_PLAN_V2.md`.

**Why first**: the chat migration rewrites the exact code paths this plan moves (send, sync, chat list, push apply). With interfaces and fakes in place first:
- the chat migration changes signatures in one interface instead of in a 3k-line class;
- ViewModel tests written here act as a safety net for that migration;
- E2E gets a clean hook point (`MessageRepository` + `AuthRepository`) instead of a 15th constructor parameter on `AppRepository`.

**Hard rule: purely structural.** No behavior changes, no new features, no API or DB changes. Keep return types as they are (`Boolean` / nullable / `NetworkResult`, see D3) (They need refactoring later on, this will be a seperate step. Write as a todo in a new Todo.md file, and also append the things from step 9 and the follow up plans (they will be implemented before the other plans). Also update the claude.md to always add findings into the todo that no thought is lost) and keep `(chatId, isGroup)` signatures. `chatId` is introduced by the chat migration, not here. Every phase compiles and ships on its own.

Paths: `C/` = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/`. Line numbers verified 2026-10-11.

---

## 1. Current state (verified 2026-10-11)

### 1.1 `AppRepository`
- Constructor (:171-187): `AppDatabase`, `NetworkUtils`, `Preferencemanager`, `PictureManager`, `AudioManager`, `UserRepository`, `GroupRepository`, `MessageRepository`, `AppVersion` (public `val`), `LoggingRepository`, `MapRepository`, `EventRepository`, `LanguageService`, `AuthSessionManager`.
- Nested global objects `ErrorChannel` (:189-230) and `ActionChannel` (:233-252). They are used by `App.kt`, `LoginViewModel`, `AppAuthEventSink` and others.
- Rough responsibility buckets (function → line):
  - **Auth/session**: `setNotificationToken` :257, `sendEmailVerify` :265, `onNewTokenPair` :1191, `loadSavedLoginConfig` :1212, `login` :1256, `createAccount` :1304, `changePassword` :1595, `getSessions` :1611, `logoutAllDevices` :1633, `endSession` :1650.
  - **Teardown**: `deleteAllAppData` :282, `logout` :329.
  - **App info**: `testServer` :357, `getChangeLog` :924, `getLatestGitHubVersionAsString` :940, `appVersion`.
  - **Sync**: `dataSyncLock` :366, `dataSyncState` :423, `dataSync` :430, `userIdSync` :1345, `messageIdSync` :2089, `groupIdSync` :2526, `getMissingPics` :872, `getMissingAudios` :904, `executeSync` :2878.
  - **Map/Event pass-throughs**: `upsertMapEntry` :663, `deleteMapEntry` :695, `upsertEvent` :736, `setEventParticipation` :786, `joinEvent` :803, `detachEvent` :850.
  - **Users/friends**: `getUserByIdFlow` :963, `getUserById` :969, `getFriends`/`getFriendsFlow` :985/:992, `getSnailTrailFlow` :1000, `getProfilePicturesForUserIds` :1498, `getAvailableUsers` :1517, `send/deny/removeFriend…` :1537-1567, `changeUsername` :1581, `changeProfilePic` :1661, `changeUserDetails` :1678, `setLocationSharing` :2686, `disableLocationSharingForAllFriends` :2717, `setWakePermission` :2729, `setWakeGlobal` :2746, `sendWake` :2769.
  - **Chat list / search**: `getPendingFriends` :973, `getUnreadChatCountFlow` :1006, `getChatListFlow` :1018 (+ `chatKey` :2933), `getChatSelectorFlow` :1086, `getMessageSearchFlow` :1105, `setPinnedChat` :2841.
  - **Messages**: `sendMessageLock` :1728, `sendMessage` :1738, `sendOfflineMessages` :1986, `applyPushMessage` :2198, `getPicturesForMessageIds` :2255, `getAudiosForMessageIds` :2270, `setAllChatMessagesRead` :2305, `editMessage` :2322, `deleteLocalMessage` :2367, `deleteMessage` :2371, `reactToMessage` :2387, `votePoll` :2409, `deletePollOption` :2433.
  - **Groups**: `createGroup` :2471, `getProfilePicturesForGroupIds` :2595, `changeGroup*` :2610-2667.
  - **Settings**: `applyServerSettings` :2789, `setUseMd`…`setMapStyleSetting` :2800-2835, `setQuickReactions` :2855, `resetQuickReactions` :2861, `setLastContributePopupShown` :2866.

### 1.2 Existing "repositories" are really local data sources
`UserRepository`, `GroupRepository`, `MessageRepository`, `EventRepository`, `MapRepository` and `LoggingRepository` each wrap only `AppDatabase`. `SettingsRepository` wraps only `Preferencemanager`, and `FeedbackRepository` wraps only `NetworkUtils`. None of them has an interface. Under the android-data-layer skill naming, the Room-only ones are **local data sources**.

### 1.3 What ViewModels inject today (non-games)
| ViewModel | Concrete data-layer deps |
|---|---|
| `ChatViewModel` | AppRepository, MessageRepository, UserRepository, GroupRepository, SettingsRepository, LoggingRepository, PictureManager, AudioManager, PermissionManager |
| `ChatDetailsViewmodel` | AppRepository, MessageRepository, UserRepository, GroupRepository, EventRepository, PictureManager |
| `SharedContentViewModel` | MessageRepository, UserRepository, PictureManager |
| `ChatSelectorViewModel` | AppRepository, Preferencemanager, LoggingRepository, PermissionManager |
| `EventsViewModel` | AppRepository, EventRepository, GroupRepository, UserRepository, Preferencemanager, PictureManager |
| `SchneaggmapViewModel` | AppRepository, MapRepository, EventRepository, UserRepository, Preferencemanager, LocationService |
| `GlobalViewModel` | AppRepository, EventRepository, Preferencemanager, SocketConnectionManager, LocationService, BatteryService, AppVersion |
| Settings VMs (8) | AppRepository, Preferencemanager, LoggingRepository, PermissionManager, PictureManager, AppVersion |
| Login VMs (4) | AppRepository, Preferencemanager, LoggingRepository, LanguageService, PictureManager |
| `NewChatViewModel`, `GroupCreatorViewModel`, `BirthdaysViewModel`, `FriendCompassViewModel` | AppRepository (+ LocationService) |
| `FeedbackViewModel` | FeedbackRepository |

Notes on these dependencies:
- `Preferencemanager` is injected into 13 ViewModels. They mostly read flows: server URL, theme, map style, merge flags, demo mode, events view mode, onboarding/last-started version.
- `PictureManager`, `AudioManager`, `PermissionManager`, `LocationService`, `BatteryService` and `AppVersion` are **`expect class`es**, so `commonTest`/`jvmTest` can't fake them.
- `Navigator` and `ApplicationScope` are plain classes. They can be used for real in tests (see D8).

### 1.4 Other `AppRepository` callers (not ViewModels)
- `SocketConnectionMessage.kt`: calls `dataSync`, `applyServerSettings`, and the media fetches for message, user and group ids.
- `Preferencemanager.kt`: calls `applyServerSettings`.
- `AppAuthEventSink.kt`: uses the channels.
- `App.kt`: `loadSavedLoginConfig`, `logout`, the channels, `appVersion`.
- UI composables reading `appVersion` or `testServer`: `Login.kt`, `SettingsScreen.kt`, `ChatSelector.kt` (`dataSyncState`), `UrlChangeDialog.kt`.

### 1.5 Test setup
- `commonTest` has `kotlin-test` + `kotlinx-coroutines-test`.
- `jvmTest` has one benchmark using in-memory Room with `BundledSQLiteDriver`.
- `ktor-client-mock` is in the catalog. Turbine and AssertK are not.

---

## 2. Target architecture

### 2.1 Layers and dependency direction
```
ViewModel ──► domain interface (Repository / Coordinator)
                     ▲ implemented by
              data impl ──► LocalDataSource (Room) / NetworkUtils / Preferencemanager / platform services
Coordinator impl ──► Repository interfaces   (never the reverse)
```
Rules:
- A ViewModel never imports anything from `data` or `datasource`: no `AppDatabase`, `NetworkUtils`, `Preferencemanager`, DAOs or DTOs.
- Repositories don't depend on each other in a cycle. Shared logic goes up into a coordinator.
- Coordinators own the cross-repo locks: `dataSyncLock` → `DataSyncCoordinator`; `sendMessageLock`/`sendOfflineLock` → `MessageRepository`.
- One interface + one implementation per concept. Bind in Koin as `singleOf(::OfflineFirstMessageRepository) bind MessageRepository::class`.

### 2.2 Target classes

| Interface (`domain`) | Impl (`data`) | Takes over |
|---|---|---|
| `login/domain/AuthRepository` | `login/data/RemoteAuthRepository` | login, createAccount, loadSavedLoginConfig, onNewTokenPair, sendEmailVerify, changePassword, sessions, logoutAllDevices, endSession, setNotificationToken |
| `app/domain/SessionTerminator` | `app/data/LocalSessionTerminator` | logout, deleteAllAppData (wipes DB, prefs, media, socket; calls `AuthRepository` for the server-side end) |
| `app/domain/AppInfoRepository` | `app/data/RemoteAppInfoRepository` | testServer, getChangeLog, getLatestGitHubVersionAsString, app version string |
| `chat/domain/UserRepository` | `chat/data/OfflineFirstUserRepository` | user/friend flows, friend requests, available users, username/pic/details changes, location sharing, wake permission/global/send, snail trail, online state (from the current local class), `syncUsers()` (= `userIdSync`), `fetchMissingProfilePictures(ids)` |
| `chat/domain/GroupRepository` | `chat/data/OfflineFirstGroupRepository` | createGroup, changeGroup*, group flows/getters, `syncGroups()` (= `groupIdSync`), group pics |
| `chat/domain/MessageRepository` | `chat/data/OfflineFirstMessageRepository` | sendMessage, sendOfflineMessages, edit/delete/react/poll, markRead, applyPushMessage, message flows/getters (from the current local class), `syncMessages()` (= `messageIdSync`), message media fetch, missing pics/audios |
| `chat/domain/ChatListRepository` | `chat/data/OfflineFirstChatListRepository` | getChatListFlow (+ `chatKey`), getChatSelectorFlow, getMessageSearchFlow, getPendingFriends, getUnreadChatCountFlow, setPinnedChat |
| `events/domain/EventRepository` | `events/data/OfflineFirstEventRepository` | upsertEvent, setEventParticipation, joinEvent, detachEvent, event flows, `syncEvents()` |
| `schneaggmap/domain/MapRepository` | `schneaggmap/data/OfflineFirstMapRepository` | upsertMapEntry, deleteMapEntry, map flows, `syncMapEntries()` |
| `settings/domain/SettingsRepository` | `settings/data/DataStoreSettingsRepository` | every `set*` from AppRepository + applyServerSettings + quick reactions + drafts + **every preference flow a ViewModel reads today** (replaces direct `Preferencemanager` injection) |
| `app/domain/DataSyncCoordinator` | `app/data/DefaultDataSyncCoordinator` | dataSync (order, lock, `dataSyncState`), executeSync. Calls `sync*()` on the repos above. |
| `feedback/domain/FeedbackRepository` | `feedback/data/RemoteFeedbackRepository` | unchanged API, just gets an interface |
| `app/logging/…/Logger` (see D9) | `RoomLoggingRepository` | log*, getLogs, clearLogs |

The existing Room-only classes get renamed to local data sources (D1). They stay internal to `data`:
- `UserRepository` → `UserLocalDataSource` (interface) / `RoomUserDataSource`
- `GroupRepository` → `GroupLocalDataSource` / `RoomGroupDataSource`
- `MessageRepository` → `MessageLocalDataSource` / `RoomMessageDataSource`
- `EventRepository` → `EventLocalDataSource` / `RoomEventDataSource`
- `MapRepository` → `MapLocalDataSource` / `RoomMapDataSource`

`NetworkUtils` (1,442 lines) stays one concrete remote data source in this plan (D2). Repository impls are tested against it through Ktor `MockEngine`.

### 2.3 Platform services → interfaces
Every `expect class` a ViewModel injects becomes a common `interface` plus per-platform implementations: `PictureManager`, `AudioManager`, `PermissionManager`, `LocationService`, `BatteryService`, `AppVersion`.
- Pattern: `interface PictureManager { … }` in `commonMain`, `class AndroidPictureManager(context) : PictureManager` in `androidMain`, and the same for iOS/JVM. Koin platform modules bind them.
- Signatures stay identical, so call sites only change in Koin.

### 2.4 Global channels
`AppRepository.ErrorChannel` / `ActionChannel` move unchanged to `app/events/ErrorChannel.kt` / `ActionChannel.kt`. Only the import changes. Turning them into injected buses is out of scope (D10).

---

## 3. Phases

Each phase leaves the app compiling and behaving the same. During the move, `AppRepository` keeps thin delegating methods (`suspend fun login(...) = authRepository.login(...)`) so callers can be switched one by one. It is deleted in Phase 8.

### Phase 0: Groundwork
1. Move `ErrorChannel` / `ActionChannel` out (§2.4).
2. Convert the six `expect class`es to interfaces (§2.3) and update the Koin platform modules.
3. Test deps: add `turbine` + `assertk` to the version catalog and `commonTest`, and `ktor-client-mock` to `jvmTest` (D5). Add `commonTest/…/testutil/` with `MainDispatcherExtension`-style setup (`Dispatchers.setMain(UnconfinedTestDispatcher())`) and an in-memory Room builder for `jvmTest`.
4. Rename the Room-only classes to `*LocalDataSource` / `Room*DataSource` (§2.2) with interfaces. This is mechanical, and the IDE rename covers it.

### Phase 1: Low-risk pass-throughs
- `SettingsRepository` interface + `DataStoreSettingsRepository`. Move all `set*`, `applyServerSettings`, quick reactions, `setLastContributePopupShown`, plus the preference flows from §1.3.
- Switch the 13 ViewModels from `Preferencemanager` to `SettingsRepository`. `SocketConnectionMessage` and `Preferencemanager.applyServerSettings` point at the new class.
- `MapRepository`, `EventRepository` (absorb the pass-throughs from :663-850) and `AppInfoRepository`.
- `FeedbackRepository` interface (rename only).
- Callers switched: Map, Events, Appearance/Misc/Map/Notification settings, `SharedSettingsViewmodel`, `LoggedInDevicesViewModel` (AppVersion), `ChatSelector`/`Login`/`SettingsScreen`/`UrlChangeDialog` (appVersion, testServer).

### Phase 2: Auth + teardown
- `AuthRepository` + `SessionTerminator`.
- `logout` and `deleteAllAppData` need DB, prefs, socket and media cleanup, so they live in `SessionTerminator`. `AuthRepository.endServerSession()` is called inside it with the existing 5 s timeout.
- `App.kt`, the 4 login VMs, `UserSettings`/`PrivacyAndSecurity`/`LoggedInDevices`/`MiscSettings` VMs and `EmailVerifiedCheckViewModel` are switched.
- `AuthSessionManager` stays as-is; it's injected only into `AuthRepository`.

### Phase 3: Users and groups
- `UserRepository` (offline-first, wraps `UserLocalDataSource` + `NetworkUtils` + `PictureManager`) and `GroupRepository`.
- `syncUsers()` / `syncGroups()` are moved from `userIdSync` / `groupIdSync` unchanged. `DataSyncCoordinator` doesn't exist yet, so `AppRepository.dataSync` calls them for now.
- Callers switched: NewChat, GroupCreator, Birthdays, FriendCompass, ChatDetails (user/group parts), Notification/Map/Privacy settings, `GlobalViewModel` (friends), `SocketConnectionMessage` (profile pics).

### Phase 4: Chat list
- `ChatListRepository` with `getChatListFlow` (+ `chatKey`), `getChatSelectorFlow`, `getMessageSearchFlow`, `getPendingFriends`, `getUnreadChatCountFlow`, `setPinnedChat`. It depends on the User/Group/Message **local data sources** for reads and on `SettingsRepository` for pins.
- Callers: `ChatSelectorViewModel`, `GroupCreatorViewModel`, `NewChatViewModel`, `GlobalViewModel`.
- This is the class the chat migration later rewrites around `chatId`.

### Phase 5: Messages (highest risk)
- `MessageRepository` takes over send (:1738-1976 incl. send-echo handling), offline queue, edit, delete, react, poll, mark-read, `applyPushMessage` and message media fetches, together with `sendMessageLock`, `sendOfflineLock` and `messageSyncLock`.
- `syncMessages()` = `messageIdSync`.
- `getMissingPics` / `getMissingAudios` move here too: they are message-media only, while profile pics already moved in Phase 3.
- Callers: `ChatViewModel`, `ChatDetailsViewmodel`, `SharedContentViewModel`, `GlobalViewModel` (`sendOfflineMessages`), `SocketConnectionMessage`, push handlers. Also update the KDoc reference in `SafeNetworkCall.kt:25`.
- Move order inside the phase: (a) reads + simple actions, (b) edit/delete/react/poll, (c) send + offline queue, (d) push apply. **Test send/offline/dedup manually on device after (c)** (`MESSAGE_SEND_SYNC_REBUILD_PLAN.md` invariants).

### Phase 6: Sync coordinator
- `DataSyncCoordinator` takes over `dataSync` (order, lock, `DataSyncState`/`DataSyncJobStatus`, logging) and `executeSync`. It only calls `sync*()` / `fetchMissing*()` on the repository interfaces.
- `DataSyncState` moves to `app/domain` (it's read by `DataSyncDetailPopup`).
- Callers: every VM that calls `dataSync` (Login, SignUp, Events, Map, UserSettings, EmailVerifiedCheck, Misc, Privacy, ChatSelector, GroupCreator, GlobalViewModel), `SocketConnectionMessage`, the Android WorkManager/iOS background entry points and `ChatSelector.kt` (`dataSyncState`).

### Phase 7: Clean ViewModel boundary
- Grep check: no file under `*/presentation/` imports `datasource.`, `.data.`, `AppDatabase`, `NetworkUtils` or `Preferencemanager`. Fix whatever is left. Known leftover: `ChatViewModel` imports `NetworkUtils.PollVoteRequest`, which moves to a domain model + mapper.
- ViewModels stay on the existing `Navigator`, `ApplicationScope` and `LanguageService` (D8).

### Phase 8: Delete `AppRepository`
- Remove the delegating shells and the class, and update `di/Modules.kt`.
- Update the cross-plan references (§5).

### Phase 9: Safety-net tests before the chat migration (D6)
Write the tests the chat migration needs to be safe:
- **ViewModel tests with fakes**: `ChatSelectorViewModel` (list, search, pins, unread badge), `ChatViewModel` (send, edit, delete, react, mark read, offline state), `ChatDetailsViewmodel` (group edits, friend actions), `NewChatViewModel`, `GroupCreatorViewModel`.
- **Repository tests** (`jvmTest`, in-memory Room + `MockEngine`):
  - `OfflineFirstChatListRepository`: DM vs group grouping, last message, unread count with friendship gating.
  - `OfflineFirstMessageRepository`: send success, send echo dedup by `clientMessageId`, offline queue retry, `applyPushMessage`.
  - `DefaultDataSyncCoordinator`: order, lock re-entry, state transitions.
- Fakes live in `commonTest/…/fakes/` (`FakeMessageRepository`, …) and are reused by the chat migration.

---

## 4. Risks
- **Send/sync regressions (Phase 5/6).** These are the paths that just got fixed (duplicate sends, event overwrite on read). Mitigation: move the code verbatim, no clean-ups in the same commit, device test after each sub-step, Phase 9 tests.
- **Koin graph cycles.** Example: `SessionTerminator` → `AuthRepository` → … → `SessionTerminator`. Keep the dependency direction from §2.1. Koin fails at first resolve, so every phase needs a launch on all 3 platforms.
- **iOS/Android background sync entry points** call `AppRepository` outside of Compose. Grep `androidMain`/`iosMain` in Phase 6.
- **Merge conflicts** with other branches touching `AppRepository`. Land each phase as its own PR into `main` quickly.
- **Plan drift**: line numbers in the chat and E2E plans go stale (§5).

## 5. Cross-plan edits after Phase 8
- `CHAT_BASED_MIGRATION_PLAN.md`:
  - §4.1 "AppRepository" bullet (:196) and §8.6 note (:343): re-point to `OfflineFirstMessageRepository` (send, send-echo, sync, push apply), `OfflineFirstChatListRepository` (`chatKey`, chat list, search) and `SocketConnectionMessage`.
  - Add §4.4: "update fakes + Phase 9 tests to `chatId`".
- `E2E_ENCRYPTION_PLAN_V2.md`:
  - :31 boundary sites → `MessageRepository` impl.
  - :114 `E2eEncryptionManager` goes into `OfflineFirstMessageRepository` + `RemoteAuthRepository` constructors (not AppRepository), behind a `domain` interface so ViewModel/repository tests use `FakeE2eEncryptionManager`.
  - :117 `AppRepository.login` → `RemoteAuthRepository.login`.

## 6. Effort
- Phases 0–8: ~4–6 sessions. Phase 5 alone is ~1–2.
- Phase 9: ~2 sessions.
- Each phase is one PR. Verification needs a Gradle build plus a launch on Android, iOS and Desktop, which you do (no gradle runs from me unless asked).

---

## 7. Decisions (open, please answer)

| # | Question | Recommendation |
|---|---|---|
| D1 | Rename the existing Room-only `*Repository` classes to `*LocalDataSource` / `Room*DataSource`, freeing the `*Repository` names for the real multi-source repos? | **Yes.** Matches the data-layer skill and avoids two `UserRepository` concepts. |
| D2 | Split `NetworkUtils` into per-feature remote data sources now? | **No.** The chat migration rewrites the message/group endpoints anyway. Test repos via `MockEngine`. Revisit after E2E. |
| D3 | Convert `Boolean` / nullable returns to `Result<T, DataError>` now? | **No.** Keeps this plan behavior-neutral. Convert per feature while the chat migration touches those methods anyway. |
| D4 | Interfaces in each feature's `domain` package (`chat/domain`, `login/domain`, `settings/domain`, `events/domain`, `schneaggmap/domain`, `app/domain`)? | **Yes.** |
| D5 | Test stack: the skill says JUnit5 + Turbine + AssertK, but JUnit5 doesn't run in KMP `commonTest`. Use `kotlin-test` as runner + Turbine + AssertK in `commonTest`, with Room/MockEngine tests in `jvmTest`? | **Yes.** |
| D6 | Is Phase 9 (safety-net tests) part of this migration, before the chat migration starts? | **Yes.** It's the reason for doing this first. |
| D7 | Convert the six `expect class` platform services to interfaces (§2.3)? | **Yes.** Without it, `ChatViewModel`, `ChatDetailsViewmodel` and the settings VMs can't be faked. |
| D8 | Keep `Navigator`, `ApplicationScope` and `LanguageService` as concrete classes (used for real in tests)? | **Yes.** `ApplicationScope` gets a constructor parameter for the dispatcher/scope so tests can pass a `TestScope`. |
| D9 | `LoggingRepository` → `Logger` interface (injected almost everywhere)? | **Yes**, done in Phase 0. |
| D10 | Keep `ErrorChannel` / `ActionChannel` as global objects (just moved)? | **Yes for now.** Tests can collect from them. Injected buses are a later clean-up. |
| D11 | One PR per phase into `main`, or one long-running branch? | **One PR per phase.** `AppRepository` is a conflict magnet. |


## 8. Todos to ask the user about if this plan has been implemented
- Fix up networkutils
- unify the passed results from networkutils back to the callers
- Ask the user about all the user things that have been deferred in this plan for later implementation