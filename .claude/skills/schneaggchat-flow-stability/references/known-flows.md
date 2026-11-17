# Known flow hop-maps

Pre-traced hop lists for the flows that come up most often, so a trace starts from real anchors
instead of rediscovering the plumbing every time. Each map is a starting point — a real trace may
branch differently for a specific message type, error path, or platform (Android/iOS/Desktop).
Apply the five lenses from the global `flow-stability` skill to every hop listed here, with the anchors in `known-gaps.md`.

## 1. Send a message (text/image/audio/poll, online or offline)

1. UI: `ChatInputBar` → `ChatAction.OnSendClick` (`chat/presentation/chat/ChatState.kt`); for images,
   picking runs through `ChatViewModel.onImagesSelected` (`chat/presentation/chat/ChatViewModel.kt:299`)
   first, which downscales inside a **bare** `CoroutineScope(Dispatchers.Default).launch { }` — not
   `viewModelScope`/`applicationScope`, no attached `SupervisorJob`/exception handler. A corrupt or
   undecodable picked image throws unhandled here and crashes the app (lens 1).
2. `ChatViewModel` builds `MessageContent`, calls `AppRepository.sendMessage(...)`
3. `AppRepository.sendMessage` (`datasource/AppRepository.kt:1692`) — under `sendMessageLock`;
   builds a type-specific `MessageDto` with `sent = false`, inserts it locally first (optimistic UI),
   then attempts the network call.
4. `NetworkUtils` POST via `safeCall` (`datasource/network/NetworkUtils.kt`) — on success flips
   `sent = true` and applies the server-assigned `id`; on failure the local `sent = false` row is
   left in place as the durable retry record.
5. **Offline retry path**: `AppRepository.sendOfflineMessages` (`datasource/AppRepository.kt:1937`)
   — under `sendOfflineLock`, polls `messageRepository.getUnsentMessages()` (no `ORDER BY`,
   `datasource/database/Daos.kt:137`) and replays them one at a time through the same `sendMessage`,
   waiting out `sendMessageLock` first, stopping once the head of the queue is unchanged after an
   attempt (`AppRepository.kt:2023-2024`). Type-specific unwraps happen here (e.g. `m.poll!!` at line
   1981 — see lens 1). Two gotchas worth checking before trusting a retry queue shaped like this
   elsewhere: (a) a permanently-failing head message head-of-line-blocks every message queued after
   it, forever (lens 3); (b) the `AUDIO` branch's `if (m.audioPath == null) { TextContent(...) }`
   (`AppRepository.kt:2002-2004`) never returns that fallback — it's dead code, and execution falls
   through to build `AudioContent` from a possibly-missing file regardless (lens 3).
6. Server: `MessageController` → `MessageService` (auth/ownership checks — see
   `spring-endpoint-security-check` for that half) → Mongo insert.
7. `NotificationService.notifyMessageUpdate` (`notifications/NotificationService.kt:56`) — socket
   push to each recipient, FCM/APNs fallback per-recipient if `socketConnectionHandler.sendMessage`
   returns false.
8. Client: `handleSocketConnectionMessage`'s `MessageChange` branch
   (`datasource/network/socket/SocketConnectionMessage.kt:155`) — upserts into Room, reconciles
   `existingLocalPK`, preserves the sync `version` cursor, triggers a notification banner unless the
   chat is already open in foreground.
9. Room `Flow` → `ChatViewModel` state → Compose recomposition.

Edit/delete/react/vote branch off step 6 through `MessageService` — see
`spring-endpoint-security-check`'s section 2a for the known asymmetry between `editMessage` and
`deleteMessage`'s ownership checks; that same file's `withOptimisticRetry` calls
(`message/MessageService.kt:211,384,481`) are the concurrency-safety hop for those paths.

## 2. Global data sync (`AppRepository.dataSync`)

**Trigger**: `GlobalViewModel.init` (`app/GlobalViewModel.kt:45-91`) chains
`AppLifecycleManager.appResumedEvent.combine(SessionCache.onlineFlow)` →
`.combine(SessionCache.authState)` → on each qualifying tick, connects the socket, replays offline
messages, then calls `dataSync("appResumed")`. `appResumedEvent` is a
`MutableSharedFlow<Unit>(extraBufferCapacity = 1)` with `replay = 0`
(`app/AppLifecycleManager.kt`) — an emission with no active collector is dropped, not queued for the
next subscriber. A resume firing before this `init` block's `collect` has attached (plausible at
cold start) is lost outright, with no automatic re-trigger until the *next* resume. This is a
silent-skip, not a stuck spinner — nothing ever shows loading, sync just doesn't happen.

Entry point: `AppRepository.dataSync(reason)` (`datasource/AppRepository.kt:390`).

1. `SessionCache.requireLoggedIn() ?: return` — bails before `isSyncing` is ever set (safe early
   exit, not a stuck spinner).
2. `dataSyncLock` — a concurrent caller awaits the in-flight run instead of starting a second one,
   via a check-then-act pair (`isLocked` check, then a separate `withLock`) that can let two
   near-simultaneous callers both run a full sync back-to-back rather than coalescing — see lens 2.
3. `isSyncing = true`, all job statuses reset to `IDLE`.
4. Five jobs launched concurrently via `async`: `userJob`, `messageJob`, `groupJob`, `mapJob`,
   `eventJob` — each wraps its sync call in its own try/catch that calls `ensureActive()` before
   marking the job `FAILED` (so cancellation isn't mistaken for a sync failure).
5. `awaitAll(...)`, then `removedGroupIds.forEach { messageRepository.deleteMessagesForGroup(it) }`
   — the deliberate re-purge that undoes whatever `messageJob` re-inserted into a group `groupJob`
   deleted mid-run. This is the reference shape for "concurrent job interference" (lens 2).
6. `MEDIA` job runs after the concurrent batch: `getMissingPics()` + `getMissingAudios()`
   (`datasource/AppRepository.kt:798-847`) — their three categories (profile pics, group pics,
   message media) run concurrently, but *within* each category the actual downloads
   (`getPicturesForMessageIds`/`getAudiosForMessageIds`, lines 2118/2133) go through a plain
   `ids.forEach { ... }`, one HTTP round trip at a time. This is the most likely real contributor to
   a long-lived (not permanently stuck, but visibly slow) sync spinner after a long offline stretch:
   a large backlog plus still-weak signal at reconnect means many sequential near-timeout requests
   stack up before the job finishes (lens 3).
7. `finally { isSyncing = false }` — guarantees the spinner clears even if something above threw
   past its own try/catch (lens 5's positive example).

Each per-type sync (`userIdSync`, `messageIdSync`, `groupIdSync`, `mapEntryIdSync`, `eventIdSync`)
loops a sync endpoint until `moreEntries`/`moreMessages` is false, applying updated and deleted
entries from each page. They are **not all paginated the same way** server-side — see lens 4's
message-vs-event/map comparison (`MessageService.messageSync:717` uses a `VersionCounterService`
watermark; `EventService.eventIdSync:44` and `SchneaggmapService.mapSync:175` re-sort and re-slice
the full eligible list on every page call instead). `mapEntryIdSync`
(`datasource/AppRepository.kt` just after `dataSync`) is the clearest client-side example to compare
a new sync type against.

## 3. Login / token refresh

1. UI: login form → `LoginViewModel` → `AppRepository.login(...)` (`datasource/AppRepository.kt:1287`)
2. Server: `AuthController.login` (`authentication/AuthController.kt:96`) →
   `AuthService.login` (`authentication/AuthService.kt:91`) — issues a `TokenPair`.
3. `AppRepository.onNewTokenPair` (`datasource/AppRepository.kt:1209`) persists tokens,
   `SessionCache.login(...)` flips auth state.
4. **Ongoing refresh**: `createHttpClient`'s `install(Auth)` block (`datasource/network/createHttpClient.kt:48`)
   wires `tokenManager.loadBearerTokens()` for outgoing requests and `tokenManager.refreshTokens(oldRefreshToken)`
   on a 401.
5. `TokenManager.refreshTokens` (`datasource/network/TokenManager.kt`) — `refreshMutex` +
   `inFlight` `Deferred` dedupe concurrent refresh attempts from multiple in-flight requests hitting
   401 at once; a 5-second `Cooldown` suppresses hammering the endpoint after a retryable failure.
6. Server: `AuthController.refresh` (`authentication/AuthController.kt:114`) →
   `AuthService.refresh` (`authentication/AuthService.kt:126`) — rotates the refresh token.
7. Result maps to `RefreshResult.Success` / `Retryable` / `Invalidated` — only `Invalidated` should
   ever trigger `AppAuthEventSink.onSessionInvalidated` (`datasource/network/auth/AppAuthEventSink.kt:40`):
   popup, wipe in the app scope, then `ActionEvent.AuthInvalidated` → `app/App.kt:435` navigates to
   login. Confirm `App.kt` still collects `ActionChannel` and acts on that event (lens 5).
8. On success, `HttpClient(named(HTTPCLIENTTYPE.AUTHENTICATED)).clearAuthTokens()` drops the Auth
   plugin's cache so a refresh triggered *outside* the plugin (explicit login, socket reconnect)
   doesn't leave the client sending a stale access token until its next 401.

## 4. WebSocket connect / reconnect

1. `SocketConnectionManager.connect(serverUrl, onError, onClose)` — requires
   `SessionCache.requireLoggedIn()`, guarded by `connectMutex` so overlapping connect calls collapse
   to one attempt.
2. `SocketConnection` opens the Ktor `webSocket` with a bearer token from `TokenManager`.
3. Every failure branch (`onError`, `onClose`, the `connectJob`'s catch, the outer catch) calls
   `setConnectionState(Disconnected)` then `scheduleReconnectIfPossible()` — tracing a change here
   means checking *every* exit still reaches that call (lens 3).
4. `setConnectionState(Disconnected)` also clears known-online friends
   (`UserRepository.clearOnlineFriends()`) since presence is only valid while connected; repopulated
   by the server's `FriendOnlineStatusSnapshot` on reconnect
   (`notifications/NotificationService.kt:315`, `sendInitialFriendOnlineSnapshot`).
5. Incoming frames dispatch to `handleSocketConnectionMessage` (see flow 1, step 8) inside
   `scope.launch { ... }` — an exception in one frame's handling must not take down the receive loop
   (the top-level try/catch in that function is what prevents that).

## 5. Push fallback (recipient offline)

1. Any `NotificationService.notifyX` method (`notifyMessageUpdate`, `notifyGroupUpdate`,
   `notifyReactionAdded`, `notifyEventUpdate`, `notifyFriendRequest`, ...) first tries
   `socketConnectionHandler.sendMessage(message, receiverId)`.
2. If that returns `false` (recipient not connected), falls back to
   `firebaseMessagingService.sendXNotificationToUser(...)` (Android/web) and
   `apnsService.sendXNotificationToUser(...)` (iOS) — both called, not either/or, since a user may
   have devices on both platforms.
3. Client-side, a push arrives outside the socket/session lifecycle (app killed, backgrounded) and is
   decrypted/handled by the platform-specific push receiver before reaching the same
   `handleSocketConnectionMessage`-adjacent upsert logic, or a dedicated push payload path — check
   `utilities/notifications/PayloadDecoder.kt` and `NotificationContentResolver.kt` for that branch
   when tracing a push-only path (no live socket) rather than assuming it shares every step of flow 1.
4. A method that skips the socket-first check and always sends push wastes a round trip for online
   recipients; a method that skips the fallback silently drops the update for offline ones — both are
   findings-worthy deviations from the pattern above.
