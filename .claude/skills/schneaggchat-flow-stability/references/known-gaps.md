# Schneaggchat anchors per lens

The project-specific half of the five lenses: the real classes, locks and reference shapes to
compare against, plus known gaps with `file:line` anchors. The generic reasoning for every item is
in the global `flow-stability` skill (`references/lens-checklists.md`).

Client paths are relative to the client's main package in SchneaggchatV3; server paths are relative
to `com.lerchenflo.schneaggchatv3server` in SchneaggchatV3server (sibling folder). Line numbers
drift — re-check them before quoting.

## 1. Crash / unhandled exception

- `AppRepository.sendOfflineMessages` does `val poll = m.poll!!` (`datasource/AppRepository.kt:1981`)
  — safe only because the message type was switched on `MessageType.POLL` two lines above.
- Server id validation: `require(ValidationUtils.validateObjectId(id))`
  (`util/ValidationUtils.kt:241`) before every `ObjectId(id)`; mapping in
  `core/GlobalExceptionHandler.kt`.
- Correct cancellation pattern (`ensureActive()` in `catch`): `TokenManager.doRefresh` and every job
  inside `AppRepository.dataSync`.
- `handleSocketConnectionMessage` (`datasource/network/socket/SocketConnectionMessage.kt:155`)
  wraps the whole `when` in one try/catch.
- **Known gap:** `ChatViewModel.onImagesSelected` (`chat/presentation/chat/ChatViewModel.kt:299`)
  runs `pictureManager.downscaleImage(...)` in a bare `CoroutineScope(...).launch { }` — a corrupt
  or undecodable picked image crashes the app.

## 2. Races & concurrency

- Client locks: `sendMessageLock` / `sendOfflineLock` in `AppRepository` (the offline sender waits
  via `while (sendMessageLock.isLocked) delay(...)`), `dataSyncLock` (`AppRepository.dataSync` — a
  second caller awaits the first run), `TokenManager.refreshMutex` + the `inFlight` `Deferred`,
  `SocketConnectionManager.connectMutex`.
- Server optimistic locking: `withOptimisticRetry` (`util/OptimisticRetry.kt`) wraps load-then-save
  in `MessageService.kt:211,384,481` and `SchneaggmapService.kt:108`.
- Parallel sync jobs: `AppRepository.dataSync` launches `userJob`/`messageJob`/`groupJob`/`mapJob`/
  `eventJob` via `async`, then re-purges `removedGroupIds` after `awaitAll` because `messageJob` can
  re-insert rows a concurrent `groupIdSync` just deleted. A new job added to this list needs the
  same check.
- Socket vs sync: the `MessageChange` handler in `handleSocketConnectionMessage` keeps
  `version = existing?.version ?: 0L` so a push never advances the sync cursor.
- Version watermark: `VersionCounterService` (`util/VersionCounterService.kt`) — `next()`/`reserve()`
  allocate via `findAndModify`, `withVersion { }`/`withVersions` track in-flight versions,
  `safeWatermark()` (not `current()`) bounds sync queries. `MessageService.messageSync`
  (`message/MessageService.kt:717`) does it correctly.
- **Known gap (harmless):** `dataSync`'s check-then-act around `dataSyncLock`
  (`datasource/AppRepository.kt:394-400`) can run two full syncs back-to-back. Don't copy the shape.

## 3. Network failure / offline / retry

- Error subtypes: `NetworkError` in `datasource/network/util/RequestError.kt` (`NoInternet`,
  `RequestTimeout`, `Unauthorized`, `Forbidden`, `Conflict`, `TooManyRequests`, `PayloadTooLarge`,
  `Serialization`, `Unknown`); callers receive `DataResult.Error`.
- Durable retry: `sendOfflineMessages` picks up `sent = false` rows in Room.
- Token refresh: `RefreshResult.Retryable` vs `RefreshResult.Invalidated` in `TokenManager`; check
  every new caller of `refreshTokens()`.
- Reconnect: every exit of `SocketConnectionManager.connect` (`onError`/`onClose`/catch) must reach
  `scheduleReconnectIfPossible()`.
- Push fallback: `NotificationService` tries `socketConnectionHandler.sendMessage(...)` and falls back
  to `firebaseMessagingService`/`apnsService` when it returns `false` — see `notifyMessageUpdate`
  (`notifications/NotificationService.kt:56`).
- **Known gap — head-of-line blocking:** `sendOfflineMessages` (`datasource/AppRepository.kt:1937`)
  always retries `messages.first()` from `messageRepository.getUnsentMessages()`, a query with no
  `ORDER BY` (`datasource/database/Daos.kt:137`), and returns when that head row is unchanged — one
  permanently failing message blocks all others forever.
- **Known gap — dead fallback:** the `AUDIO` case of the same function
  (`datasource/AppRepository.kt:2002-2004`) builds `TextContent("Offline audio sending failed")` but
  never returns it, then builds `AudioContent` from a possibly missing file.
- **Known gap — sequential fetches:** `getMissingPics`/`getMissingAudios`
  (`datasource/AppRepository.kt:798-847`) run categories concurrently, but
  `getPicturesForMessageIds`/`getAudiosForMessageIds` (`datasource/AppRepository.kt:2118,2133`) fetch
  one id at a time with `ids.forEach`.
- **Known gap — lost resume event:** auto-resync on reconnect (`app/GlobalViewModel.kt:45-91`) is
  driven by `AppLifecycleManager.appResumedEvent`, a `MutableSharedFlow<Unit>(extraBufferCapacity = 1)`
  with `replay = 0` (`app/AppLifecycleManager.kt`); a resume before `init` subscribes is dropped.

## 4. Data consistency / sync drift

- Tombstones: `mapEntryIdSync` applies `response.data.deletedEntries` as well as `updatedEntries` —
  the reference for new synced resources.
- Local id reconciliation: `existingLocalPK = existing?.localPK ?: 0L` in the `MessageChange`
  handler.
- Pagination: `MessageService.messageSync` (`message/MessageService.kt:717`) pages by version
  watermark. **Known drift window:** `EventService.eventIdSync` (`events/EventService.kt:44`) and
  `SchneaggmapService.mapSync` (`schneaggmap/SchneaggmapService.kt:175`) recompute
  `sortedByDescending { updatedAt }.drop(start).take(pageSize)` on every page.

## 5. Stuck UI states

- Reference shape: `AppRepository.dataSync` clears `DataSyncState.isSyncing` in `finally`; its
  `SessionCache.requireLoggedIn() ?: return` early return comes before the flag is set (safe).
- User-visible errors: `ErrorChannel` popup only (ViewModels send a returned `DataError` with `reportErrorTo`); `SnackbarManager.showMessage` is for success and info — wired examples
  in `app/App.kt:390` and `login/.../LoginViewModel.kt:75`.
- Check that `DataSyncState.exitedWithErrors` is actually surfaced, and that the forced logout still
  ends on login: `AppAuthEventSink.onSessionInvalidated` (`datasource/network/auth/AppAuthEventSink.kt:40`)
  shows the popup, wipes in the app scope and sends `ActionEvent.AuthInvalidated` in a `finally`;
  `app/App.kt:435` navigates to login on it.
