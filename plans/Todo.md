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

- [ ] **Logout leaves downloaded media on the device.** `logout` / `deleteAllAppData` (`C/datasource/AppRepository.kt:282`, `:329`) wipe DB, prefs, push token and socket, but never delete profile pictures, image messages or voice messages saved by `PictureManager` / `AudioManager`. The next account on the same device can still find the previous account's files on disk. Decided: not part of the migration (plan D12), `SessionTerminator` moves logout verbatim.
- [ ] **"Delete all app data" in Misc settings deletes less than its name says.** `MiscSettingsViewModel.deleteAllAppData` calls `AppRepository.deleteAllAppData`, which clears the DB but keeps preferences (except last started version) and all media.
- [ ] **Split `GlobalViewModel`** (414 lines, 8 deps: sync triggers, socket, location, battery, unread count, offline send). It must not grow during the migration (plan D14). It is registered as an app-wide Koin `single`; per the Android review (plan R8) the target is data-layer managers in the app scope, not more ViewModels.
- [ ] Stale comment: `C/datasource/AppRepository.kt:630` says map sync is "triggered only from map screen, not part of global dataSync", but `dataSync` runs `mapEntryIdSync` as its MAP job. Check the moved copy in `OfflineFirstMapRepository` too.
- [ ] `setLastContributePopupShown` (`C/datasource/AppRepository.kt:2866`) is the only settings setter without `.trackConnectivity()` on its `updateSettings` call.
- [ ] **Two writers per table** (plan R5, deferred). `SocketConnectionMessage` (`C/datasource/network/socket/SocketConnectionMessage.kt:168-173`, ~26 calls) writes socket updates straight into the Room classes, bypassing `AppRepository` today and the new repositories after the migration. Route them through repository `apply*` methods (like `applyPushMessage`), which also brings them under the repositories' locks. Relevant for the chat migration, which rewrites both paths.
- [ ] Service locator calls: `SocketConnectionMessage.kt:168-173` and the push handlers (`AppFirebaseMessagingService`, `MarkAsReadReceiver`, `ReplyReceiver`, `IosPushDelegateBridge`) resolve repositories via `KoinPlatform.getKoin().get<…>()`. The Android/iOS entry points need a lookup; `SocketConnectionMessage` could get its dependencies injected if that creates no Koin cycle.

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

