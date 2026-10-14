# Todo

Collected follow-ups, deferred work, open decisions and findings, so no thought gets lost. Each item names where it came from. Tick an item off (or delete it) once it is done or rejected.

Paths: `C/` = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/`.

---

## Cross-repo

- [ ] **Server: socket push after every user/group mutation** (plan D16/R1). After Phase 3 the 11 user/group mutations call their own `syncUsers()` / `syncGroups()` instead of the full `dataSync()`. The per-endpoint check lives in `SchneaggchatV3server/Todo.md`. Once it confirms every push, remove those own-sync calls as well.

## Follow-ups after `REPOSITORY_INTERFACE_MIGRATION_PLAN.md`

Deferred on purpose, because that plan is purely structural. Run **after** that plan and **before** `CHAT_BASED_MIGRATION_PLAN.md` / `E2E_ENCRYPTION_PLAN_V2.md` (decided 2026-10-13).

- [ ] Fix up `NetworkUtils` (1,442 lines): split it into per-feature remote data sources (plan D2).
- [ ] Unify the results `NetworkUtils` passes back to its callers (one error shape instead of a mix of `NetworkResult`, `Boolean` and `null`).
- [ ] Return types: convert the `Boolean` / nullable / raw `NetworkResult` returns of the new repositories to `Result<T, DataError>` (plan D3). With it, stop repositories from writing into `ErrorChannel`; they return the error and the ViewModel maps it to UI (plan R6).
- [ ] Ask the user about every user-facing behavior the plan deferred for later.
- [ ] Inject the global objects `SessionCache`, `SnackbarManager`, `AppLifecycleManager`, `IncomingDataManager` instead of using them statically (plan D19). Until then tests reset them and can't run in parallel.
- [ ] **Rebuild message sending** (user, 2026-10-13: "message sending is trashed anyway"). The migration moves it verbatim and only guards it with the Phase 4.5 characterization tests.
- [ ] Split `OfflineFirstMessageRepository` after the move. It inherits ~1,100 lines (send, offline queue, sync, push apply, actions, media) and becomes the next god class. Candidates: a sender, a sync part, a push/action part.

## Findings from the plan review (2026-10-13)

Outside the structural scope of the migration. Not fixed, listed for review.

- [ ] **Logout leaves downloaded media on the device.** `logout` / `deleteAllAppData` (since Phase 2 in `C/app/data/LocalSessionTerminator.kt`) wipe DB, prefs, push token and socket, but never delete profile pictures, image messages or voice messages saved by `PictureManager` / `AudioManager`. The next account on the same device can still find the previous account's files on disk. Decided: not part of the migration (plan D12), `SessionTerminator` moves logout verbatim.
- [ ] **"Delete all app data" in Misc settings deletes less than its name says.** `MiscSettingsViewModel.deleteAllAppData` calls `SessionTerminator.deleteAllAppData` (was `AppRepository.deleteAllAppData` before Phase 2), which clears the DB but keeps preferences (except last started version) and all media.
- [ ] **Split `GlobalViewModel`** (414 lines, 8 deps: sync triggers, socket, location, battery, unread count, offline send). It must not grow during the migration (plan D14). It is registered as an app-wide Koin `single`; per the Android review (plan R8) the target is data-layer managers in the app scope, not more ViewModels.
- [ ] Stale comment: map sync is described as "triggered only from map screen, not part of global dataSync", but `dataSync` runs it as its MAP job. Since Phase 1b the comment sits in the moved copy, `C/schneaggmap/data/OfflineFirstMapRepository.kt:31` (`syncMapEntries`).
- [ ] `setLastContributePopupShown` (`C/datasource/AppRepository.kt:2866`) is the only settings setter without `.trackConnectivity()` on its `updateSettings` call.
- [ ] **Two writers per table** (plan R5, deferred). `SocketConnectionMessage` (`C/datasource/network/socket/SocketConnectionMessage.kt:168-173`, ~26 calls) writes socket updates straight into the Room classes, bypassing `AppRepository` today and the new repositories after the migration. Route them through repository `apply*` methods (like `applyPushMessage`), which also brings them under the repositories' locks. Relevant for the chat migration, which rewrites both paths.
- [ ] Service locator calls: `SocketConnectionMessage.kt:168-173` and the push handlers (`AppFirebaseMessagingService`, `MarkAsReadReceiver`, `ReplyReceiver`, `IosPushDelegateBridge`) resolve repositories via `KoinPlatform.getKoin().get<…>()`. The Android/iOS entry points need a lookup; `SocketConnectionMessage` could get its dependencies injected if that creates no Koin cycle.

## Findings from migration Phase 0a (2026-10-14)

Noticed while doing Phase 0a. Not fixed.

- [ ] **Log timestamps print the wrong month.** `C/app/logging/RoomLoggingRepository.kt:26` formats the console timestamp with `timestamp.month.ordinal`, which is 0-based, so October prints as `09`. Only the console line is affected; `LogEntry.timeStamp` is stored as epoch millis.
- [ ] `D/Logger` exposes `LogEntry` (a Room `@Entity`) and `LogType` from `C/app/logging/`, so the shared `datasource/domain` package imports from `app`. Move them to `D/model/` (or map `LogEntry` to a domain model) next time logging is touched.
- [ ] `D/ErrorEvent` still carries `NetworkingError` (`C/datasource/network/util/`) and `UiText`. Both are free of Compose types themselves, but the event is UI-shaped. Goes away with the `Result<T, DataError>` follow-up (plan R6).
- [ ] `SnackbarManager.showMessage` (`C/utilities/SnackbarManager.kt:32-33`) wraps the send in two nested `scope?.launch` calls; one is enough.
- [ ] `AppLifecycleManager.setCarSessionActive(false)` (`C/app/AppLifecycleManager.kt:42-54`) emits `appBackgroundedEvent` every time it is called while the app is in the background, also when the car session was already off.
- [ ] `composeApp/src/jvmTest/.../chat/ChatLoadingBenchmark.kt` calls itself a "temporary benchmark" but runs on every `jvmTest` (3 DB sizes up to 20k messages, 15 runs each). Remove it or move it out of the default test run.

## Findings from migration Phase 0b (2026-10-14)

Noticed while doing Phase 0b. Not fixed.

- [ ] **`@Transaction` outside a DAO does nothing.** Room only honours it on `@Dao` methods, but it sits on data-source methods: `C/chat/data/RoomUserDataSource.kt:26, :51, :90`, `C/chat/data/RoomGroupDataSource.kt:29, :34, :66, :81`, `C/chat/data/RoomMessageDataSource.kt:141, :193, :210`. So multi-statement writes are not atomic: `updateFriendLocations` (delete trail + insert points), `upsertMessageWithReaders` (upsert message + delete/insert readers), and without any annotation `deleteGroup` (4 deletes) and `deleteUser` (2 deletes). A crash or a concurrent read in between sees half-written data. Move them into `@Transaction` DAO methods or wrap them in a Room transaction.
- [ ] `AndroidLocationService` (`composeApp/src/androidMain/.../utilities/location/AndroidLocationService.kt:33, :104, :108`) builds its own `AndroidPermissionManager(context)` three times instead of getting the Koin `PermissionManager` injected.
- [ ] Stale KDoc examples in `C/chat/data/GroupLocalDataSource.kt:30-31, :41-42`: `chat.toGroup()?.groupMembers` and `getCommonGroups(chat.id)` (the function takes two ids) don't match the current code.
- [ ] Other plans still name the platform files renamed in Phase 0b: `Notifier.android.kt` / `Notifier.jvm.kt` (`CHAT_BASED_MIGRATION_PLAN.md:231, :233, :304`, `ANDROID_AUTO_MAP_PLAN.md:28, :230`), `LocationService.android.kt` (`ANDROID_AUTO_MAP_PLAN.md:15`), `AppVersion.jvm` (`AUTH_SESSION_REBUILD_PLAN.md:27, :110`). They are now `AndroidNotifier.kt`, `DesktopNotifier.kt`, `AndroidLocationService.kt`, `DesktopAppVersion.kt`.

## Findings from migration Phase 1a (2026-10-14)

Noticed while doing Phase 1a (SettingsRepository). Not fixed.

- [ ] **Phase 7 gap: more composables `koinInject<Preferencemanager>()` than plan §2.5 lists.** Besides `Login.kt`, `ChatSelector.kt` and `App.kt`: `C/chat/presentation/chatselector/MessageChatSelector.kt:65` (highlight flag), `C/login/presentation/emailverifiedcheck/EmailVerifiedCheckScreen.kt:77` (server URL), `C/login/presentation/signup/SignUp.kt:118` (server URL), `C/settings/presentation/usersettings/UserSettings.kt:105` (injected, never used). Phase 7 has to cover them too.
- [ ] `C/login/presentation/signup/SignUp.kt:376` reads the server URL with `runBlocking { preferencemanager.getServerUrl() }` inside a composable, which blocks the UI thread.
- [ ] The settings enums `ThemeSetting`, `LanguageSetting`, `MapStyleSetting` (`C/datasource/preferences/`) carry UI code (`toUiText()`, `getIcon(): ImageVector`). `D/SettingsRepository` exposes them, so `D/` reaches Compose icons through them, and 7 presentation files import them from `datasource.preferences`, which the Phase 7 grep flags. Move the UI mapping into presentation extensions and the enums (plus `PinnedChat`, used by `D/model/SyncedSettings`) to `D/model/`, in Phase 7 at the latest.
- [ ] `C/settings/presentation/appearancesettings/AppearanceSettingsViewModel.kt:139` prints the system language with `println` on every language change (debug leftover).

## Findings from migration Phase 1b (2026-10-14)

Noticed while doing Phase 1b (Event, Map, AppInfo, Feedback repositories). Not fixed.

- [ ] **Phase 7 gap: `C/sharedUi/text/ComboAnnotationSources.kt:48-50`** (composable `rememberComboAnnotationSources`) gets `MapLocalDataSource`, `UserLocalDataSource` and `EventLocalDataSource` straight from Koin to resolve annotation names. Plan §2.5 doesn't list it. Phase 7 has to move it onto `MapRepository` / `EventRepository` / `UserRepository` (or a ViewModel).
- [ ] **The event and map models moved to `D/model/` carry UI code**, same as the settings enums above: `EventType.icon()` / `@Composable color()` / `labelRes()` (`C/datasource/domain/model/EventType.kt:63-104`), `EventVisibility.icon()` / `labelRes()`, `EventParticipationStatus.icon()` / `labelRes()` (`EventParticipation.kt:60-70`), `GroupDeleteDelay.labelRes()`, `AttributeOption.labelRes` (`AttributeOptions.kt`), and in `LocationData.kt:646-833` `AttributeKey.labelRes()`, `LocationType.stringRes()` / `drawableRes()`, `@Composable LocationGroup.stringRes()` / `sortedTypes()`. So `D/` pulls in Compose icons, colors and resources. Move the UI mappings into presentation extensions, in Phase 7 at the latest.
- [ ] **ViewModels still read `appRepository.appVersion`** for platform checks: `C/chat/presentation/chat/ChatViewModel.kt:929`, `C/chat/presentation/newchat/NewChatViewModel.kt:168`, `C/chat/presentation/chatselector/ChatSelectorViewModel.kt:118`, `C/events/presentation/EventsViewModel.kt:80, :141`, `C/settings/presentation/SharedSettingsViewmodel.kt:63`. No phase of the plan names them. They need the `AppVersion` interface (`C/utilities/`) injected directly before Phase 8 deletes `AppRepository`. (The version name moved to `AppInfoRepository.getVersionName()` in Phase 1b.)
- [ ] `syncEvents()` / `syncMapEntries()` (`C/events/data/OfflineFirstEventRepository.kt:48`, `C/schneaggmap/data/OfflineFirstMapRepository.kt:35`) keep an unused `val userId = SessionCache.requireLoggedIn() ?: return`; only the early return matters.
- [ ] `feedback/domain/FeedbackRepository` returns `NetworkResult` / `EmptyResult` / `NetworkingError` from `C/datasource/network/util/`, so the feature's domain package depends on network types. Goes away with the `Result<T, DataError>` follow-up.
- [ ] Other plans still name the old model paths (now `C/datasource/domain/model/`): `EVENT_RSVP_PLAN.md:20, :131, :132` (`events/domain/…`), `MAP_LOCATION_RATINGS_PLAN.md:39, :157` (`schneaggmap/domain/…`), `GROUP_EXTENSIONS_PLAN.md:318` (`schneaggmap/domain/`).

## Findings from migration Phase 2 (2026-10-14)

Noticed while doing Phase 2 (AuthRepository, SessionTerminator). Not fixed.

- [ ] `C/chat/presentation/chatselector/ChatSelectorViewModel.kt:94` pulls `Notifier` from `KoinPlatform.getKoin()` inside a ViewModel to read the push token, then hands it to `AuthRepository.setNotificationToken`. Inject it, or give `AuthRepository` a method that reads the token itself, so the ViewModel only triggers the registration.
- [ ] `D/AuthRepository.login` / `createAccount` (`C/datasource/domain/AuthRepository.kt:16-32`) report the result through an `onResult: (Boolean) -> Unit` callback instead of a return value; `SignUpViewModel` copies it into a local `var` (`C/login/presentation/signup/SignUpViewModel.kt:220`). Return the result directly with the `Result<T, DataError>` follow-up.

## Findings from migration Phase 3a (2026-10-14)

Noticed while doing Phase 3a (UserRepository). Not fixed.

- [ ] **The own `syncUsers()` after a user mutation (D16/R1) runs outside `dataSyncLock`** (`C/chat/data/OfflineFirstUserRepository.kt:305, :322, :337, :352, :366`). The old `dataSync()` call waited for a running sync; now a friend request sent during a full sync runs a second `/users/sync` in parallel with the USERS job (double requests, maybe double profile picture downloads, last write wins). Goes away when the calls are removed after `SchneaggchatV3server/Todo.md`; otherwise give `syncUsers()` its own mutex.
- [ ] `syncUsers()` writes through `AppDatabase.userDao()` directly (`C/chat/data/OfflineFirstUserRepository.kt:88-231`, moved verbatim from `userIdSync`) instead of `UserLocalDataSource`. Its delete path (`:227`, `userDao().delete`) leaves the deleted user's snail-trail points behind, while `UserLocalDataSource.deleteUser` (used by the socket `UserChange`) deletes them first. Route the sync through the local data source (needs a `UserDto` getter there) and the orphaned trail points go away too.
- [ ] `UserRepository.getFriends(searchTerm)` filters with the case-sensitive `name.contains(searchTerm)` (`C/chat/data/OfflineFirstUserRepository.kt:63`), `getFriendsFlow` with SQL `LIKE` (case-insensitive for ASCII). Same search, different results. All current callers pass `""`, so nothing visible yet.
- [ ] Non-ViewModel classes still read users from `UserLocalDataSource` via `KoinPlatform`: `composeApp/src/androidMain/.../utilities/notifications/AndroidNotifier.kt:364, :387` and `C/datasource/network/socket/SocketConnectionManager.kt:147` (`clearOnlineFriends`). The games `C/games/presentation/recap/RecapViewModel.kt:61` injects it too; the Phase 7 presentation grep will flag it (`chat.data` import).
- [ ] `C/chat/presentation/chatdetails/ChatDetailsViewmodel.kt:194` shows a hardcoded English snackbar text ("You are not Friends with this User or other error (401)") for every non-403 wake error. Needs a string resource (and the text is wrong for e.g. offline).
- [ ] `C/chat/presentation/newchat/NewChatViewModel.kt:102` and `C/chat/presentation/chatdetails/ChatDetailsViewmodel.kt:514` send friend requests in an unscoped `CoroutineScope(Dispatchers.IO).launch` ("to not access the db on main thread"). The repository is main-safe; use `viewModelScope`, or the repository's `externalScope` (R2) if the request must outlive the screen.

## Deferred from the Android-standards review (plan §8, 2026-10-13)

- [ ] **Auth invalidation in the data layer** (R8). Today `App.kt` reacts to `ActionChannel.AuthInvalidated` with error popup + `logout()` + navigate to Login. Target: `SessionTerminator` listens to `AuthEventSink` in the app scope, and `App.kt` only navigates when the auth state changes to logged out.
- [ ] **Location and media repositories** (R10). ViewModels still inject `LocationService`, `PictureManager`, `AudioManager`, `BatteryService` directly; Android recommends going through repositories.
- [ ] **Global error/action events as app-level state** (review Q1). Replace the `ErrorChannel` / `ActionChannel` streams with app-level `StateFlow` messages and an `onShown(id)` acknowledgement, as Android's UI-events guidance recommends.
- [ ] **Work that must survive process death** (review Q3). Sends and sync run in the in-process app scope. Add a platform background scheduler (WorkManager on Android, BGTaskScheduler on iOS) for them.

## Device-test checklist (unattended migration run)

Steps the plan asks to test on a real device, which nobody could do during the night run. Test on Android and iOS (Desktop where it applies):

- [ ] Phase 2: login, logout, logout all devices, delete all app data, push token registration.
- [ ] Phase 3: each of the 11 user/group mutations updates the UI right away (friend request send/deny/remove, username, profile pic, group pic/description/name/members/expiry).
- [ ] Phase 5: send text/image/audio/poll, offline send + reconnect (no duplicates), edit/delete/react/vote, push receive, notification reply, notification mark-as-read.
- [ ] Phase 6: full sync on login, app resume, pull-to-refresh; sync status popup.
- [ ] Phase 7: login screen (version, server URL dialog), settings screen version, chat selector sync indicator.

