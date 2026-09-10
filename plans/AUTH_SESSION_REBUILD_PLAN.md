# Auth Session Rebuild Plan (client side)

Rebuild the client-side session layer: token refresh, 401 handling, offline/retry behaviour, and logout. The current implementation grew defect-by-defect and now spreads one piece of state (am I logged in, with which tokens) across three independent caches that drift apart. This plan defines what the replacement has to do, every edge case it has to survive, and how it gets tested.

Analysis date: 2026-09-09. Client repo `SchneaggchatV3` (branch `flolapptop`), server repo `SchneaggchatV3server` (branch `beta`, verified against `origin/beta` = `3aad87d`).

Scope: client only. No server change is required by this plan; the server contract below is what the client must be written against. Anything in this document that would need a server change is called out explicitly in "Server-side follow-ups".

## Status

**Implemented 2026-09-09** on branch `flolapptop` (steps 1-6 of section 9; step 7 deferred per decision 5). 93 tests in `composeApp/src/commonTest/.../datasource/network/auth/` pass via `:composeApp:jvmTest`. Manual verification of step 5 (airplane-mode toggling, laptop sleep, server restart) is still open.

## Revision log

**2026-09-09 (rev 2, pre-implementation triple check).** Every file/line claim in sections 1-2 was re-verified against the working tree. Corrections:

- D6 was wrong: `AppVersion.jvm.getDeviceName()` already appends the first non-virtual MAC address to the hostname. Residual risk is limited to machines where no MAC is readable (`"unknown"`). Reworded; decision 5 stays open, default "no change".
- D15 is now specified, not "unspecified": Ktor's `AuthTokenHolder.setToken` holds a `Mutex` while `refreshTokens` runs; a re-entrant `clearToken()` cannot take it and defers the clear to `GlobalScope`, which nulls the freshly stored tokens right after the callback. Harmless but wasteful. Resolved structurally: the bearer provider is configured with `cacheTokens = false`, so Ktor never holds a copy at all (see R1).
- Two Koin cycles hidden in section 4 are called out and resolved (see 4.2): the non-authenticated `HttpClient` must not depend on the session manager, and the "successful authenticated request" backoff-reset hook must live in the authenticated client, not in `NetworkUtils`.
- `ActionEvent.Login` loses both of its producers (D8, the 5 s loop) and is deleted together with its handler. Hydration of `SessionCache` from storage is `AuthSessionManager.ensureSession()`, called from `AppRepository.loadSavedLoginConfig()` (cold start and the "delete all app data" path, which routes back through `AutoLoginCredChecker`).
- `ensureSession()` never awaits the network (case 36). It only classifies the stored pair and schedules a refresh if the access token is stale.
- Outcome classification (R6), reason gating (R8/R12), refresh-frequency floors (case 23), and the sink API (R1/R13) are now tabulated so there is nothing left to interpret during implementation.
- `Retry-After` is read from the 429 JSON body (`retryAfterSeconds`), which `safeCall` already preserves in `TooManyRequests.message`. No `safeCall` change needed.
- Section 8 decisions are resolved with the recommended options (recorded in 8); the only one deliberately left open is decision 5 (desktop device id), because it changes server-visible behaviour.
- Edge cases 50-58 added. Test stack fixed to `kotlin-test` + `kotlinx-coroutines-test` + `ktor-client-mock` in `commonTest`, executed via `:composeApp:jvmTest`. The `SocketAuthTest` scope is narrowed to a pure guard class because `SocketConnectionManager` reaches into Koin from `setConnectionState`.
- The locally checked-out server (`codecleanup`) is behind `beta` and has a single 10/min `/auth/*` bucket. The chosen backoff (worst case 6 attempts in the first minute) stays under that too, so manual testing against the local server is valid.

---

## 1. Current state (analysis)

### 1.1 The components involved

| Component | File | Role today |
|---|---|---|
| `TokenManager` | `datasource/network/TokenManager.kt` | Single-flight refresh, cooldown backoff, proactive refresh. Pulls `NetworkUtils`, `AppRepository` and the authenticated `HttpClient` out of Koin at call time (`:164`, `:188`, `:193`). |
| Ktor `Auth`/`bearer` | `datasource/network/createHttpClient.kt:48-77` | `loadTokens` -> `TokenManager.loadBearerTokens()`; `refreshTokens` -> `TokenManager.refreshTokens(oldRefreshToken)`; returns `null` on any non-success so Ktor stops retrying. |
| `NetworkUtils` | `datasource/network/NetworkUtils.kt` | `safeCall` (`:98`) maps exceptions + HTTP status to `NetworkingError` (`:170`); `refresh` (`:381`) and `logout` (`:400`) go through `authHttpClient` (no Auth plugin, so they can never recurse). |
| `SessionCache` | `app/SessionCache.kt` | In-memory mirror of auth state (`login:49`, `loginIfValid:65`, `logout:71`, `updateTokens:76`) plus the app-wide online flag (`:104-111`). `requireLoggedIn()` (`:115`) fires a `Login` action as a side effect of a read. |
| `Preferencemanager` | `datasource/preferences/Preferencemanager.kt` | Durable token storage in KSafe (`saveTokens:45`, `getTokens:64`, `clearAll:83`). Source of truth. |
| `App.kt` action handler | `app/App.kt:370-416` | Handles `ActionEvent.Login` (rehydrate from storage, else refresh) and `ActionEvent.AuthInvalidated` (toast + `appRepository.logout()` + navigate to Login). |
| `GlobalViewModel` poll loop | `app/GlobalViewModel.kt:185-216` | Every 5 s: reconnect socket if needed, flush offline messages, and raise `ActionEvent.Login` whenever `SessionCache` says logged out. |
| `SocketConnectionManager` | `datasource/network/socket/SocketConnectionManager.kt` | Own reconnect backoff (`:282-301`), and a second, hand-rolled refresh path in `SocketConnection.connect()` (`:319-359`) because the socket client has no Auth plugin. |
| `AppRepository` | `datasource/AppRepository.kt` | `onNewTokenPair:1207` (persist + mirror), `logout:322`, `endServerSession:306`, `deleteAllAppData:285`, `ActionChannel:230`, `ErrorChannel:193`. |

### 1.2 Three copies of one piece of state

1. **`Preferencemanager` (KSafe)** - durable, the real source of truth.
2. **`SessionCache.authState`** - in-memory mirror, drives UI and `requireLoggedIn()`.
3. **Ktor `BearerAuthProvider`'s internal `tokens`** - a third copy, only reachable through `loadTokens` / `refreshTokens` / `clearAuthTokens()`.

Every bug in section 2 is, at root, two of these three disagreeing. The rebuild's central idea is to make (1) the only writable copy and derive (2) from it deterministically, and to remove (3) entirely (`cacheTokens = false`).

### 1.3 Server contract (verified against `origin/beta`, do not re-derive)

| Fact | Where | Value |
|---|---|---|
| Access token TTL | `core/security/JwtService.kt:21` | 15 minutes |
| Refresh token TTL | `core/security/JwtService.kt:24` | 30 days, and the DB row's `expiresAt` slides forward on every rotation (`AuthService.kt:396`) |
| Refresh rotates | `AuthService.refresh:320` -> `rotateTokenRow:380` | Atomic `findAndModify` on (userId, hashedToken). Exactly one concurrent caller wins. |
| One-hop replay recovery | `AuthService.kt:356-367` | A token that was already rotated away is still accepted **once more**: the row that holds it as `previousHashedToken` returns its *current* raw token. Valid until the next rotation replaces `previousHashedToken`. |
| Genuinely dead token | `AuthService.kt:372` | HTTP 401 |
| 401 body / headers | `ResponseStatusException` | Plain 401, no `WWW-Authenticate` header. Ktor's Auth plugin still refreshes: with a single provider it does not require the header. |
| Login device dedup | `AuthService.kt:149-164` | Keyed on (userId, deviceName, deviceType). Blank device names are never dedup'd. |
| Logout | `AuthService.logout:203` | Deletes the session row. Idempotent, answers 200 even when nothing matched. |
| Rate limit `/auth/refresh` | `RateLimitProperties.kt:19` + `RateLimitFilter.kt:38-41` | `AUTH_REFRESH`, **60 requests / minute per IP**, separate bucket from login |
| Rate limit other `/auth/*` | `RateLimitProperties.kt:14` | `AUTH`, 20 / minute per IP |
| Rate limit global | `RateLimitProperties.kt:12-13` | IP 300/min, USER 300/min |
| 429 response | `RateLimitFilter.kt:109-116` | Status 429, `Retry-After` header in seconds, JSON body `{"error":"rate_limited","retryAfterSeconds":N}` |
| Local dev server (`codecleanup`) | `RateLimitProperties.kt:11` | Single `/auth/*` bucket, **10 / minute per IP**. Backoff must survive this too. |

**Consequence the rebuild depends on:** because of one-hop replay recovery, *losing a refresh response* (crash, storage write failure, killed process) is recoverable - the old token still works one more time. The client therefore does **not** need write-ahead journalling of token pairs. Losing **two** rotations in a row is not recoverable.

---

## 2. Known defects to fix

Each defect gets a test in section 6. Numbering is stable so it can be referenced from commits.

### D1 - Refresh blocks every request and cannot be cancelled (HIGH)
`TokenManager.kt:138` awaits the in-flight refresh inside `withContext(NonCancellable)`. That await happens inside Ktor's `loadTokens`/`refreshTokens`, which Ktor serialises behind its own lock. A refresh stalled on a bad network runs to the 30 s request timeout (`createHttpClient.kt:83`), every other request queues behind it, and leaving the screen cannot abort it. The app reads as frozen for 30 s.

`NonCancellable` is only genuinely needed around the *persist* step, not the await.

### D2 - Backoff never resets when the network comes back (MEDIUM)
`currentRetryCooldown()` (`TokenManager.kt:157`) doubles 5 s -> 5 min per consecutive failure and is only reset by a successful refresh (`:181-184`). Six failures on a flaky link arm a 5-minute window; when the network returns 10 seconds later the session stays unusable for the rest of it, because `refreshTokens` short-circuits to the cached `Retryable` (`:113`) without going near the network.

### D3 - Permanent non-401 failures retry forever and are invisible (MEDIUM)
Only `NetworkingError.Unauthorized` maps to `Invalidated` (`TokenManager.kt:169`). A misconfigured server URL (404), a blocking gateway (403), or a permanent 400 is classified `Retryable` forever, with no user-visible signal. The user sees "offline" indefinitely on a working network.

### D4 - Socket refresh guard trusts the device clock (MEDIUM)
`SocketConnection.connect()` (`SocketConnectionManager.kt:330`) only refreshes when the client believes the access token is expired. A token the *server* rejects but the client considers valid (clock skew, JWT secret rotation, token revoked) never triggers a socket-side refresh. Only an unrelated HTTP 401 heals it.

### D5 - A silently failed token write reports success (MEDIUM)
`AppRepository.onNewTokenPair:1207` catches and rethrows; `TokenManager.doRefresh` (`:187-189`) runs it inside `NonCancellable` and the throw is caught by the outer `catch` (`:197`) -> the call is classified `Retryable`, so the *new* token pair is dropped while the server has already rotated. Recoverable exactly once via server replay. Two in a row is a forced logout.

Per the user: acceptable risk, server-handled. Still worth an explicit test that the *second* consecutive loss is what actually invalidates.

### D6 - Desktop device identity can collide (LOW, downgraded in rev 2)
`AppVersion.jvm.getDeviceName()` returns `"<hostname> - macAddress: <first non-virtual MAC or "unknown">"`. Server login dedup keys on (userId, deviceName, deviceType) (`AuthService.kt:149-155`). Two desktops on one account collide only if they share a hostname **and** neither exposes a MAC (VMs, containers, locked-down NICs). When they do collide they share one refresh-token row and rotate each other out; two rotations apart defeats the one-hop replay -> forced logout. A per-install random id would close the gap; see decision 5.

### D7 - Logout clears credentials last (LOW)
`AppRepository.logout:322` runs `endServerSession()` then `deleteAllAppData()` then `preferencemanager.clearAll()`. A throw in `deleteAllAppData()` leaves the tokens on disk; the 5 s poll loop then rehydrates `SessionCache` straight from them and the user is silently logged back in.

### D8 - `requireLoggedIn()` has a side effect (LOW)
`SessionCache.requireLoggedIn()` (`:115`) sends `ActionEvent.Login` from a synchronous read. Combined with the 5 s loop (`GlobalViewModel.kt:206`) this is what turned a logged-out cache into a refresh storm. A read must not drive the session state machine. 53 call sites read it; all of them only want the user id / developer flag.

### D9 - No jitter in either backoff (LOW)
`TokenManager.currentRetryCooldown()` and `SocketConnectionManager.scheduleReconnectIfPossible():294` both use pure exponential doubling. Many devices dropped by the same server restart retry in lockstep. (Only the refresh backoff is in scope; the socket backoff is left as is.)

### D10 - `Retry-After` from a 429 is ignored (LOW)
The server sends `Retry-After` and `retryAfterSeconds` (`RateLimitFilter.kt:110-116`). The client throws it away and applies its own guess.

### D11 - Cooldown is keyed on the token string, not on the session (LOW)
`Cooldown(refreshToken, until, error)` (`TokenManager.kt:67`). A fresh login after a failure storm produces a different token, so the cooldown is bypassed correctly - but the *failure counter* (`consecutiveRetryableFailures`) is not reset by login, so the first failure after a fresh login can immediately arm a multi-minute window.

### D12 - Two independent refresh paths (LOW)
HTTP refresh lives in `TokenManager`; the socket has its own hand-rolled variant (`SocketConnectionManager.kt:319-359`) because the socket client has no Auth plugin. Two code paths, two sets of edge cases, one of which (D4) already diverged.

### D13 - Untestable by construction (blocking for this plan)
`TokenManager` reaches into Koin at call time (`:164`, `:188`, `:193`) and talks to global singletons (`SessionCache`, `AppRepository.ActionChannel`). There is no `commonTest` source set in the repo at all. Nothing in section 2 can be regression-tested before the dependencies are inverted.

### D14 - `loadTokens` can perform network I/O (design smell behind D1)
`loadBearerTokens()` (`TokenManager.kt:75-91`) triggers a full network refresh when the access token has under 2 minutes left. Ktor calls this on the request path, holding its token lock. Proactive refresh is the right behaviour; doing it *inside* `loadTokens` is not.

### D15 - Ktor's cached tokens and storage can disagree (LOW, now specified)
`doRefresh` calls `clearAuthTokens()` (`TokenManager.kt:193`) from inside the `refreshTokens` callback. Ktor 3.5.2 (`AuthTokenHolder.setToken`) holds a `Mutex` around that callback; `clearToken()` therefore fails `tryLock()` and launches the clear on `GlobalScope`, which runs *after* `setToken` stored the new pair and nulls it again. Net effect: the next request reloads from storage. Correct by accident. More importantly, any refresh that happens *outside* Ktor (socket path, scheduler) leaves Ktor's cache stale until it either gets cleared or the next 401. Removing the cache (`cacheTokens = false`) makes both problems disappear.

---

## 3. What the rebuilt system has to do (requirements)

### R1 - Single source of truth
Durable storage (`Preferencemanager`, behind `AuthSessionStore`) is the only writable copy of the token pair. `AuthSessionManager` keeps one in-memory mirror that is written only together with storage (under the same lock, after the write succeeded). `SessionCache` is updated through `AuthEventSink.onSessionActive(tokens)` on every successful write. Ktor reads the mirror on every request (`loadTokens` with `cacheTokens = false`) and holds no copy. No code path may write storage without going through the manager.

### R2 - Single refresh path
Exactly one component performs `POST /auth/refresh`. HTTP (Ktor Auth plugin), WebSocket handshake, cold-start hydration, the manager's own scheduler, and connectivity/foreground triggers all call `AuthSessionManager.refresh(reason, presentedRefreshToken)`. No second implementation. `NetworkUtils.refresh` is deleted.

### R3 - Single-flight
Concurrent callers share one in-flight refresh. A caller arriving while a refresh runs waits for that result; it never starts a second request against the same refresh token. This is load-bearing: the server rotates the token, so two parallel refreshes with the same input burn the one-hop replay budget. A caller that presents a refresh token that is *not* the current one (someone already rotated) gets `Success` back immediately without touching the network and re-reads the tokens.

### R4 - Cancellable waiting, uncancellable committing
Awaiting a refresh must be cancellable (leaving a screen aborts the wait). Persisting a successful token pair must not be cancellable (a half-written session is worse than a slow one). Cancelling the *waiter* must not cancel the *refresh* - a second waiter may still need it. Implementation: the refresh runs as a `Deferred` in the manager's own scope (`ApplicationScope`, never cancelled); waiters `await()` it plainly; the commit is wrapped in `withContext(NonCancellable)`.

### R5 - Bounded refresh
A refresh has its own timeout of **12 s** (`withTimeoutOrNull` around the API call), shorter than the 30 s request timeout it is nested inside. It must never be the reason a UI request takes 30 s. A timeout is classified `Retryable(NetworkTimeout)`.

### R6 - Four-way outcome classification
Every attempt resolves to exactly one `RefreshOutcome`:

| Outcome | Meaning | Triggered by |
|---|---|---|
| `Success` | new pair persisted, mirror + `SessionCache` updated | 200 with a parseable pair; also returned without network when the presented token is already stale or the session was superseded by a newer login (caller re-reads) |
| `Retryable(error, retryAfter)` | refresh token may still be good; try later | `NoInternetConnection`, `NetworkTimeout` (incl. the 12 s timeout), `ServerError` (5xx), `TooManyRequests` (429), storage write failure (case 27, short retry, backoff counter **not** incremented), offline gate skip (no counter change) |
| `Broken(error)` | permanently failing, not proof the session is dead | `BadRequest`, `Forbidden`, `NotFound`, `Conflict`, `PayloadTooLarge`, `SerializationError` (captive portal, empty body, unparseable token pair), `Unknown` |
| `Invalidated(reason)` | session is dead; storage and mirror are cleared | `Unauthorized` from `/auth/refresh` (`RejectedByServer`), no stored refresh token (`NoSession`), refresh token expired by its own `exp` or unparseable (`RefreshTokenExpired` / `MalformedToken`), returned pair belongs to a different `sub` (`ForeignSubject`), logout during the in-flight refresh (`LoggedOut`) |

`Broken` handling: backoff at the cap (2 min), and after **3 consecutive** `Broken` outcomes `AuthEventSink.onServerUnreachable(error)` fires once per streak. Credentials are never cleared by `Broken`.

### R7 - Backoff with reset triggers
Exponential with jitter, capped: base 2 s, factor 2, cap 2 min, jitter +/-25 %. Reset to zero on **all** of:
- a successful refresh,
- a successful *any* authenticated request (proof the network works) - hooked as a client plugin in the authenticated `HttpClient`,
- connectivity transitioning offline -> online (`SessionCache.onlineFlow`),
- app returning to foreground (`AppLifecycleManager.appResumedEvent`),
- a fresh login (`onLoggedIn`),
- an explicit user action (`refresh(Manual)`).

Honour `retryAfterSeconds` from a 429 body when it is larger than the computed backoff (D10). A reset that also triggers a refresh (connectivity, foreground, manual) is debounced to at most one refresh per 5 s (case 13).

### R8 - Proactive refresh off the request path
`currentTokens()` returns the in-memory mirror (storage on first call), never awaits the network, and at most *launches* a proactive refresh when the access token has < 2 min left. The scheduler wakes up at `accessExp - 2 min` after every success (floor: 60 s after the success). If a freshly issued access token already looks expired by the local clock, the clock is untrusted for that token: no proactive schedule, reactive path only (case 23).

Corollary: a request may still go out with an access token that expires in flight. That is fine - it comes back 401 and the reactive path handles it. The proactive path is an optimisation, not a correctness requirement.

Reason gating:

| `RefreshReason` | Attempted while `onlineFlow == false`? | Subject to 60 s post-success floor? |
|---|---|---|
| `Reactive401` | yes (a 401 proves connectivity) | no (1 s hard floor only) |
| `SocketHandshake` | no | yes |
| `Proactive`, `Scheduled` | no | yes |
| `ConnectivityRestored`, `AppResumed`, `Manual` | yes | no (1 s hard floor only) |

A gated-out attempt returns `Retryable(NoInternetConnection, 0)` without counting as a failure and without rescheduling; the connectivity collector re-triggers it.

### R9 - 401 handling is context-dependent
A 401 from `/auth/refresh` invalidates the session. A 401 from any *other* endpoint means "access token stale" and triggers exactly one refresh-and-retry (Ktor's `AuthCircuitBreaker` attribute guarantees the retried request is never refreshed again). A 401 on the retry (with a token the server just issued) is a server-side authorisation problem, not a session problem - it surfaces as `NetworkingError.Unauthorized` to the caller, no logout.

### R10 - Socket handshake uses the same path
`SocketConnection.connect()` asks the manager for `currentTokens()`, attempts the handshake, and on failure consults a pure `SocketAuthGuard`: refresh when the access token is expired by the local clock **or** when this is the 2nd consecutive handshake failure (D4: clock skew and secret rotation become recoverable). Only a `Success` outcome triggers an immediate reconnect; every other outcome leaves the retry to the socket's backoff loop. Because the manager's backoff gate answers from cache while armed, a reconnect storm spends at most one network refresh per backoff window.

### R11 - Logout is atomic and irreversible from the client's view
Order in `AppRepository.logout()`:
1. read the current refresh token into a local (`authSessionManager.currentTokens()`),
2. `authSessionManager.clearSession()` in `try`, `SessionCache.logout()` in `finally` - mirror, durable credentials and the session generation are gone before anything else can fail,
3. best-effort, bounded `POST /auth/logout` with the local token (unchanged 5 s timeout),
4. data wipe (`deleteAllAppData()`, `preferencemanager.clearAll()`), socket close, notifier token removal, snackbar.

A throw in step 2 is logged and the remaining steps still run (`clearAll()` retries the delete).

### R12 - Offline is a first-class state, not an error
`SessionCache.onlineFlow` (driven by `trackConnectivity()`) gates the scheduler: while offline, no optional refresh is attempted at all, and the moment connectivity returns exactly one refresh fires if the session is degraded or the access token is stale (not one per queued caller).

### R13 - Every failure path is observable
Each outcome writes one line to `LoggingRepository` (behind a tiny `AuthLog` seam) with: reason, outcome, error code, attempt number, backoff applied.

### R14 - Testable without Koin, without a real network, without wall-clock time
All collaborators injected. All time from an injected `AuthClock` and the injected scope's `delay`. All network through `AuthRefreshApi`. Section 6 covers this.

---

## 4. Architecture

### 4.1 New package `datasource/network/auth/`

```
AuthSessionStore      (interface)  read(): TokenPair? / write(pair) / clear()
AuthRefreshApi        (interface)  suspend refresh(refreshToken): NetworkResult<TokenPair, NetworkingError>
AuthEventSink         (interface)  onSessionActive(tokens), onSessionInvalidated(reason), onServerUnreachable(error)
AuthClock             (fun interface) now(): Instant
AuthLog               (interface)  info/warn/error(message)            -- LoggingRepository seam
RefreshBackoff        (class)      pure: nextDelay(retryAfter?), capDelay(), reset(), attempt
RetryAfter            (object)     pure: parse retryAfterSeconds out of a TooManyRequests body
SocketAuthGuard       (class)      pure: handshake failure counting -> shouldRefresh
AuthSessionManager    (class)      the state machine; the only caller of AuthRefreshApi
RefreshOutcome / RefreshReason / InvalidationReason / AuthSessionState / SessionCheck   (types)

PreferenceAuthSessionStore   real store  (Preferencemanager: saveTokens + saveOWNID / getTokens / clearTokens)
KtorAuthRefreshApi           real api    (NOT_AUTHENTICATED HttpClient + Preferencemanager.buildServerUrl + AppVersion, via safeNetworkCall)
AppAuthEventSink             real sink   (SessionCache.updateTokens / SessionCache.logout + ActionChannel.AuthInvalidated / ErrorChannel toast)
SystemAuthClock              real clock  (Clock.System)
LoggingAuthLog               real log    (LoggingRepository)
```

`AuthSessionManager` public surface:

```kotlin
suspend fun currentTokens(): TokenPair?                       // mirror read, no I/O after first load; may launch a proactive refresh
suspend fun ensureSession(): SessionCheck                     // Active / NoSession / Expired; never awaits the network
suspend fun refresh(reason: RefreshReason, presentedRefreshToken: String? = null): RefreshOutcome
suspend fun onLoggedIn(tokens: TokenPair)                     // password login / registration; bumps the generation
suspend fun clearSession()                                    // logout, R11; bumps the generation; silent (no sink event)
fun onAuthenticatedRequestSucceeded()                         // backoff reset trigger (R7)
val state: StateFlow<AuthSessionState>                        // Unknown / Active(userId) / Refreshing / Degraded(error, nextAttemptAt) / Invalidated(reason)
```

Session generation counter: incremented by `onLoggedIn` and `clearSession`. A refresh commits only if the generation it started under is still current; otherwise the result is discarded (case 7, 8, 30). Superseded by a login -> waiters get `Success` (they re-read the newer tokens). Cleared by logout -> waiters get `Invalidated(LoggedOut)` with no sink event.

`onSessionInvalidated` fires only on a transition **from** `Active`/`Refreshing`/`Degraded`. A session that was never active in this process (cold start with an expired or malformed pair) is invalidated silently; `loadSavedLoginConfig` already shows its own "session expired" message there (case 20).

### 4.2 Dependency graph and the two cycles

```
NOT_AUTHENTICATED HttpClient   <- nothing (createHttpClient(engine))
KtorAuthRefreshApi             <- NOT_AUTHENTICATED client, Preferencemanager, AppVersion, LoggingRepository
PreferenceAuthSessionStore     <- Preferencemanager
AppAuthEventSink               <- (static SessionCache / ActionChannel / ErrorChannel)
AuthSessionManager             <- store, api, sink, clock, log, ApplicationScope, SessionCache.onlineFlow, AppLifecycleManager.appResumedEvent
AUTHENTICATED HttpClient       <- AuthSessionManager (createAuthenticatedHttpClient(engine, manager))
NetworkUtils                   <- both clients, Preferencemanager, LoggingRepository, AppVersion   (unchanged)
AppRepository                  <- ... + AuthSessionManager
SocketConnectionManager        <- SOCKET client + AuthSessionManager
```

Cycle 1 (would exist if the plain client kept taking the manager, as `createHttpClient(engine, get(), false)` does today): manager -> api -> NOT_AUTHENTICATED client -> manager. Broken by giving the plain client no auth argument at all.

Cycle 2 (would exist if the R7 "successful request" hook lived in `NetworkUtils`): manager -> api -> NetworkUtils -> AUTHENTICATED client -> manager. Broken by (a) the api doing its own POST through `safeNetworkCall` (the `safeCall` body extracted from `NetworkUtils` into `datasource/network/util/SafeNetworkCall.kt`, `NetworkUtils.safeCall` delegating to it) and (b) the hook being a `createClientPlugin` `onResponse` in the authenticated client.

### 4.3 Wiring changes

- `gradle/libs.versions.toml` + `composeApp/build.gradle.kts` - `kotlinx-coroutines-test`, `ktor-client-mock` in `commonTest`.
- `Modules.kt:85` - `singleOf(::TokenManager)` becomes the manager plus its five interface bindings.
- `createHttpClient.kt` - split into `createHttpClient(engine)` and `createAuthenticatedHttpClient(engine, manager)`; bearer with `cacheTokens = false`, `loadTokens = manager.currentTokens()`, `refreshTokens = manager.refresh(Reactive401, oldTokens?.refreshToken)` returning the mirror on `Success`, `null` otherwise; plus the success-hook plugin. Platform modules (`AndroidModules.kt`, `DesktopModules.kt`, `IosModules.kt`) updated.
- `NetworkUtils.kt` - `safeCall` delegates to `safeNetworkCall`; `refresh` + `RefreshRequest` removed (moved to `KtorAuthRefreshApi`).
- `SocketConnectionManager.kt:319-359` - the hand-rolled block collapses to: `currentTokens()`, handshake, on failure `SocketAuthGuard` -> `refresh(SocketHandshake)` (R10).
- `App.kt` - `TokenManager` inject and the `Login` branch removed; `AuthInvalidated` branch unchanged.
- `AppRepository.kt` - `ActionEvent.Login` deleted; `onNewTokenPair` -> `authSessionManager.onLoggedIn`; `loadSavedLoginConfig` -> `ensureSession()`; `logout` reordered (R11); `endServerSession(refreshToken)`.
- `GlobalViewModel.kt:206` - the `Login` branch is removed; the loop keeps socket reconnect + offline-message flushing.
- `SessionCache.requireLoggedIn():115` - pure read.
- `Preferencemanager` - `clearTokens()` (secure keys only).
- `JwtUtils` - `expiresAtEpochMillis(token)` and `isValidAt(token, nowEpochMillis)` so expiry goes through the injected clock.
- `TokenManager.kt` deleted.

---

## 5. Edge case catalogue

Everything below has to be either handled or explicitly declared out of scope. Grouped by trigger.

### 5.1 Concurrency
1. Two screens fire authenticated requests simultaneously, both get 401 -> exactly one `/auth/refresh` (R3).
2. A refresh is in flight when a second caller arrives with the *same* old token -> joins the in-flight one.
3. A refresh is in flight when a second caller arrives with a *different* (older) token -> must not start a second refresh; gets `Success` and re-reads the mirror.
4. The waiter is cancelled (screen left) while the refresh continues -> refresh completes and persists; no orphaned state (R4).
5. **All** waiters are cancelled -> refresh must still complete and persist, because the tokens are already rotated server-side.
6. HTTP refresh and WebSocket handshake refresh race -> same single-flight (R2, R10).
7. Password login completes while a refresh for the *old* session is in flight -> the in-flight result must not overwrite the newer login tokens. Session generation counter, not a token comparison.
8. Logout runs while a refresh is in flight -> refresh result is discarded, not persisted (else logout is undone).
9. Two processes (Android app + FCM service) refresh concurrently -> single-flight is per-process and cannot cover this. One will lose and be rescued by server replay. Accepted; the only real fix is a cross-process lock.

### 5.2 Bad / flaky network
10. Refresh times out at the client -> `Retryable`, backoff, no user-visible logout.
11. Refresh request *is* delivered, response is lost -> server rotated, client keeps the old token -> next attempt is rescued by one-hop replay.
12. Two consecutive lost responses -> server-side `previousHashedToken` has moved on -> 401 -> genuine forced logout. Unavoidable client-side; the user gets the existing "session invalidated" toast, not a silent failure.
13. Connectivity flaps during backoff -> the online->offline->online transition resets the backoff (R7); flapping must not cause a request storm, so the reset-triggered refresh is debounced to one per 5 s.
14. Captive portal returns HTTP 200 with an HTML body -> `SerializationException` -> `SerializationError` -> `Broken`, not `Retryable`-forever and not `Invalidated`.
15. DNS failure / no route -> `NoInternetConnection` -> `Retryable`, and must **not** flip the app into a state that hides the login screen (only the online flag moves).
16. Server returns 502/503 during a deploy -> `Retryable` with backoff; recovers without user action when the server comes back (scheduler).
17. 429 with `retryAfterSeconds: 47` -> wait at least 47 s (D10).
18. Very slow network where the refresh takes 25 s -> capped by the 12 s refresh timeout (R5), reported `Retryable`, not left blocking the request path.

### 5.3 Token / clock
19. Access token expired, refresh token valid -> normal reactive refresh.
20. Both expired (app unused for > 30 days) -> `Invalidated(RefreshTokenExpired)` on the local `exp` check, without spending a network request, silently (never active in this process).
21. Refresh token valid by `exp` but deleted server-side (logged out from another device / "log out everywhere") -> 401 -> `Invalidated(RejectedByServer)`.
22. Device clock is hours *behind* -> client thinks tokens are still valid, server rejects them -> recovers via the 401 path (`Reactive401` ignores local expiry), never loops.
23. Device clock is hours *ahead* -> client thinks a good token is expired -> refreshes early. Must not loop: a `Success` whose access token already looks expired disables proactive scheduling for that token; the 60 s post-success floor covers the socket/proactive reasons.
24. Server JWT secret rotated -> every token invalid -> 401 -> `Invalidated` -> forced logout with the existing toast.
25. Malformed / truncated token in storage (partial write, storage corruption) -> `JwtUtils` returns null/false; a malformed *refresh* token is `Invalidated(MalformedToken)`, a malformed *access* token with a good refresh token is simply refreshed. No crash.
26. `exp` claim missing entirely -> treated as expired. Pinned with a test.

### 5.4 Storage
27. Token write throws -> D5. Server already rotated. Outcome is `Retryable` with a short retry and **no** backoff growth, mirror unchanged, so the *next* attempt presents the old token and replay recovery applies. Not `Success`.
28. Token read throws -> treated as no session for this call, not cached as "no session" (retried on the next read), no crash.
29. Storage returns empty after an OS-level keystore reset -> `Invalidated(NoSession)` / `SessionCheck.NoSession`, clean navigation to Login.
30. Two writers (refresh + password login) -> last write wins; the generation counter (case 7) decides which is last, not wall-clock ordering.

### 5.5 Lifecycle
31. App killed mid-refresh -> next start reads the old token, replay recovery applies.
32. App backgrounded mid-refresh -> refresh must complete (`ApplicationScope` + `NonCancellable` persist), not be cancelled by scope teardown.
33. Android FCM entry point rehydrates a session from storage while the main process is refreshing -> accepted divergence (case 9). Push entry points stay read-only (`SessionCache.loginIfValid`).
34. Cold start with valid stored tokens -> no `/auth/refresh` at all; `ensureSession()` hydrates `SessionCache` from storage.
35. Cold start with expired access + valid refresh -> `ensureSession()` reports `Active` and schedules exactly one refresh; the first authenticated request may also trigger it reactively - single-flight makes that one refresh.
36. Cold start offline -> no refresh attempt; app opens in offline mode with the cached session, and refreshes when connectivity returns.
37. Desktop app minimised for 8 hours -> socket reaped by the server after 75 s of no pong; reconnect backoff must not spend a refresh per attempt (the fixed nightly storm - regression test on the guard + manager gate).

### 5.6 Logout
38. Logout while offline -> local logout completes, server session dies only at expiry. Must not hang (bounded by `LOGOUT_SERVER_CALL_TIMEOUT`).
39. Logout while a refresh is in flight -> case 8.
40. Logout throws mid-way -> credentials still gone (R11, D7).
41. Server-forced logout (401 on refresh) -> toast + navigate to Login + credentials cleared, exactly once even if several callers observed the same 401.
42. `AuthInvalidated` fired twice concurrently -> one toast, one navigation, one wipe. The manager emits it once per generation; `clearSession()` is idempotent.
43. Logout, then immediately log in as a different user -> all derived state (`SessionCache`, mirror, backoff counters, single-flight deferred, generation) reset. D11 covers the counter.

### 5.7 Rate limiting
44. Client hits the 60/min `AUTH_REFRESH` bucket -> 429 -> back off per `retryAfterSeconds`; never turns a 429 into a logout.
45. Several devices behind one NAT share the per-IP bucket -> one client's misbehaviour rate-limits the others. Client-side mitigation is jitter + hard floor between refreshes; the real fix is server-side (section 7).
46. A 429 on a *normal* endpoint must not be mistaken for an auth problem (no refresh, no state change).

### 5.8 Server-behaviour edge cases
47. Refresh succeeds but returns the *same* refresh token (replay-recovery path, `AuthService.kt:361-367`) -> accepted and persisted; "token unchanged" is not a failure.
48. Refresh returns a token for a different user (should be impossible) -> detected via the `sub` claim -> `Invalidated(ForeignSubject)` rather than silently switching accounts.
49. Server sends 200 with an empty body -> `SerializationError` -> `Broken` (case 14).

### 5.9 Added in rev 2
50. "Delete all app data" from settings (`MiscSettingsViewModel.deleteAllAppData`) wipes `SessionCache` but keeps the tokens on purpose and routes back through `AutoLoginCredChecker` -> `ensureSession()` re-hydrates. No dependency on the 5 s loop.
51. Refresh returns 200 with a pair whose tokens do not parse (no `exp`, no `sub`) -> `Broken(SerializationError)`; garbage is never persisted.
52. `ensureSession()` on a store that throws transiently at cold start -> `NoSession` for this call, next call reads again (case 28).
53. `refresh()` with a blank presented token (Ktor `oldTokens == null`) -> treated as "no preference": refresh the current token.
54. Partial write (access token written, refresh token not) -> next read still carries the old refresh token -> replay recovery. Nothing to do beyond case 27.
55. `onLoggedIn` write throws -> propagates to the login flow (login reports failure), mirror and generation unchanged.
56. Waiter cancelled after the deferred was created but before `await()` -> nothing to clean up; the deferred completes on its own (case 5).
57. 401 on a normal endpoint while the online flag is stale (`false`) -> `Reactive401` is attempted regardless of the flag (R8 table).
58. Connectivity restored while the session is `Active` with a fresh access token -> backoff reset only, no refresh (R12).

---

## 6. Test plan

### 6.1 Infrastructure
- `composeApp/src/commonTest/kotlin/org/lerchenflo/schneaggchatv3mp/datasource/network/auth/`. `commonTest.dependencies { implementation(libs.kotlin.test) }` already exists.
- Version catalog additions: `kotlinx-coroutines-test` (version ref `kotlinx-coroutines`), `ktor-client-mock` (version ref `ktor`). No Turbine (state is asserted through `StateFlow.value` after `advanceUntilIdle()`), no JUnit5/AssertK (`commonTest` is multiplatform; `kotlin.test` is the framework).
- Run target: `:composeApp:jvmTest`. Gradle 9.7.1 needs JDK 17+ to launch; the shell default is JDK 11, so run with `JAVA_HOME=/usr/lib/jvm/java-25-microsoft-openjdk`. Running the tests is explicitly in scope; nothing else Gradle-related is.

### 6.2 Fakes (`commonTest/.../auth/fakes/`)
- `FakeAuthSessionStore` - in-memory, switches to throw on read and on write, write counter (cases 27, 28, 55).
- `FakeAuthRefreshApi` - scriptable queue of results (or a suspending gate to hold a refresh open), records every call with the presented token and the virtual timestamp.
- `TestAuthClock` - reads the `TestCoroutineScheduler`'s virtual time so `delay` and `now()` agree.
- `RecordingAuthEventSink` - collects `onSessionActive` / `onSessionInvalidated` / `onServerUnreachable`.
- `RecordingAuthLog`.
- `TestJwt` - mints unsigned 3-part JWTs (`{"alg":"HS256","typ":"JWT"}` header, arbitrary `sub`/`exp`, fake signature) that `JWT.from` parses.
- `AuthTestHarness` - builds a manager on `TestScope.backgroundScope` with all of the above plus a `MutableStateFlow<Boolean>` for online and a `MutableSharedFlow<Unit>` for resume.

### 6.3 Test suites

| Suite | Covers | Key assertions |
|---|---|---|
| `SingleFlightTest` | R3, cases 1-3, 6, 53 | N concurrent callers -> exactly 1 API call; all N get the same outcome; stale presented token -> `Success` with 0 calls |
| `CancellationTest` | R4, D1, cases 4, 5, 32, 56 | Cancelling waiters does not cancel the refresh; the pair is still persisted; a cancelled waiter completes promptly |
| `BackoffTest` | R7, D2, D9, D10, D11, cases 13, 17, 44, 58 | Doubling with jitter within bounds; cap respected; reset on each of the six triggers; `retryAfterSeconds` honoured when larger; counter reset on login; flapping debounced; restore with fresh token -> no refresh |
| `ClassificationTest` | R6, D3, cases 14, 15, 16, 21, 24, 46, 49, 51 | Every `NetworkingError` subtype maps to exactly the intended outcome; 401-on-refresh is the *only* network outcome that invalidates; 3 consecutive `Broken` -> one `onServerUnreachable` |
| `TimeoutTest` | R5, case 18 | A refresh that never returns resolves `Retryable(NetworkTimeout)` at 12 s virtual time |
| `ProactiveRefreshTest` | R8, D14, cases 19, 23, 35 | `currentTokens()` never awaits the network; near-expiry launches at most one background refresh; scheduler fires at `exp - 2 min`; skewed clock -> no proactive loop |
| `TokenIntegrityTest` | cases 20, 25, 26, 47, 48 | Expired/malformed/missing-`exp`/foreign-`sub` tokens each produce the right outcome without a crash; same refresh token accepted |
| `StorageFailureTest` | D5, cases 27-30, 52, 55 | A write failure does not report `Success`, does not grow the backoff, and the next attempt presents the *old* token; second consecutive loss -> 401 -> `Invalidated`; read failure is not cached |
| `SessionLifecycleTest` | R11, D7, D11, cases 7, 8, 34, 36, 38-43, 50 | `ensureSession` hydrates without network; login during in-flight -> newer tokens win; logout during in-flight -> discarded; invalidation fires once; `clearSession` resets everything |
| `ConnectivityTest` | R12, cases 13, 15, 36, 57 | No optional refresh while offline; exactly one on restore; `Reactive401` attempted while offline; flapping debounced |
| `KtorIntegrationTest` | R9, D15, cases 1, 46 | `MockEngine` + the real `Auth` plugin: one 401 -> one refresh -> one retry -> 200; a second 401 does not loop; N parallel 401s -> 1 refresh; 429 on a normal endpoint touches nothing; tokens refreshed outside Ktor are used on the next request without a 401 |
| `SocketAuthGuardTest` | R10, D4, cases 22, 37 | 2nd consecutive failure forces a refresh even when the client thinks the token is valid; expired token refreshes on the 1st; a 20-attempt reconnect storm spends at most one network refresh per backoff window |
| `RetryAfterTest` | D10 | body parsing, header-less, garbage |

### 6.4 Non-goals for the test suite
- Real network I/O, real server, real KSafe/DataStore.
- Compose UI tests. The navigation on `AuthInvalidated` is asserted through the event sink, not through the UI.
- Cross-process behaviour (cases 9, 33). Documented, not tested.
- `SocketConnectionManager`'s reconnect loop itself (it pulls `UserRepository` from Koin in `setConnectionState`). Only the extracted guard is tested.

---

## 7. Server-side follow-ups (not required by this plan)

Listed so they are not lost; each is independent and can be done later.

- **S1** - `/auth/refresh` is rate limited per IP (`RateLimitFilter.kt:41`). Keying it on the token's subject instead would stop one looping client behind a NAT from starving everyone else (case 45).
- **S2** - the replay grace window never expires: `previousHashedToken` stays mintable until the next rotation, and `rawToken` is stored in plaintext next to the hash (`AuthService.kt:381-396`). Timestamping the rotation and rejecting old replays would bound the exposure.
- **S3** - `rotateTokenRow` overwrites `deviceName`/`deviceType` from the request body unverified (`AuthService.kt:393-394`), so any token holder can relabel a session and corrupt the device list.
- **S4** - no access-token revocation: "log out everywhere" leaves already-issued access tokens usable for up to 15 minutes.

---

## 8. Decisions (resolved in rev 2 unless marked open)

1. **Logout ordering vs. server session kill** (R11) - **adopted**: read the refresh token into a local, clear session + storage, then best-effort `POST /auth/logout` with the local.
2. **`Broken` outcome UX** (R6, D3) - **adopted (minimal)**: after 3 consecutive `Broken` outcomes the sink raises an `ErrorChannel` toast ("Cannot reach this server. Check the server URL in settings.", new string resource in en/de/it) without logging out. A persistent banner with a deep link into the server-URL setting is a UI follow-up, not part of this plan.
3. **Backoff constants** - **adopted**: base 2 s, factor 2, cap 2 min, jitter +/-25 %, hard floor 1 s between any two attempts, 60 s post-success floor for non-401 reasons, `Broken` sits at the cap.
4. **Refresh timeout** (R5) - **adopted**: 12 s.
5. **Desktop device name** (D6) - **OPEN**, default no change. The MAC suffix already covers the common case; a per-install random id changes the server-side dedup key once on the next login and leaves one stale row per desktop to age out via `expiresAt`. Needs the user's confirmation before touching it.
6. **The 5 s `GlobalViewModel` loop stays** (D8) - **adopted**: only its `ActionEvent.Login` branch goes.
7. **Push/background entry points stay read-only** (`SessionCache.loginIfValid`) - **adopted**, because a background process refreshing is exactly case 9.

---

## 9. Implementation order

1. Version catalog + `commonTest` source set + fakes + test-JWT builder.
2. `JwtUtils` clock-injectable helpers, `Preferencemanager.clearTokens()`, `safeNetworkCall` extraction, the interfaces and pure classes (`RefreshBackoff`, `RetryAfter`, `SocketAuthGuard`) with their tests.
3. `AuthSessionManager` + the full suite from 6.3 against fakes.
4. Real implementations (`PreferenceAuthSessionStore`, `KtorAuthRefreshApi`, `AppAuthEventSink`, `SystemAuthClock`, `LoggingAuthLog`), Koin wiring, `createAuthenticatedHttpClient`, `KtorIntegrationTest`.
5. Cut over: `App.kt`, `AppRepository` (login/register/hydrate/logout), `SocketConnectionManager`, `GlobalViewModel`, `SessionCache.requireLoggedIn`, platform HTTP modules. Delete `TokenManager` and `ActionEvent.Login`.
6. String resources (en/de/it), changelog.
7. (Deferred, decision 5) Desktop device id.

Steps 1-4 are additive and cannot regress the running app. Step 5 is the only risky one and is where manual verification (airplane-mode toggling, laptop sleep, server restart) belongs.
