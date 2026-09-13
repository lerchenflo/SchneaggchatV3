# Auth Session Rebuild Plan (client side)

Rebuild the client-side session layer: token refresh, 401 handling, offline/retry behaviour, and logout. The current implementation grew defect-by-defect and now spreads one piece of state (am I logged in, with which tokens) across three independent caches that drift apart. This plan defines what the replacement has to do, every edge case it has to survive, and how it gets tested.

Analysis date: 2026-09-09. Client repo `SchneaggchatV3` (branch `flosmainpc`), server repo `SchneaggchatV3server` (branch `beta`).

Scope: client only. No server change is required by this plan; the server contract below is what the client must be written against. Anything in this document that would need a server change is called out explicitly in "Server-side follow-ups".

---

## 1. Current state (analysis)

### 1.1 The components involved

| Component | File | Role today |
|---|---|---|
| `TokenManager` | `datasource/network/TokenManager.kt` | Single-flight refresh, cooldown backoff, proactive refresh. Pulls `NetworkUtils`, `AppRepository` and the authenticated `HttpClient` out of Koin at call time (`:164`, `:188`, `:193`). |
| Ktor `Auth`/`bearer` | `datasource/network/createHttpClient.kt:48-77` | `loadTokens` → `TokenManager.loadBearerTokens()`; `refreshTokens` → `TokenManager.refreshTokens(oldRefreshToken)`; returns `null` on any non-success so Ktor stops retrying. |
| `NetworkUtils` | `datasource/network/NetworkUtils.kt` | `safeCall` (`:98`) maps exceptions + HTTP status to `NetworkingError` (`:170`); `refresh` (`:381`) and `logout` (`:400`) go through `authHttpClient` (no Auth plugin, so they can never recurse). |
| `SessionCache` | `app/SessionCache.kt` | In-memory mirror of auth state (`login:49`, `loginIfValid:65`, `logout:71`, `updateTokens:76`) plus the app-wide online flag (`:104-111`). `requireLoggedIn()` (`:115`) fires a `Login` action as a side effect of a read. |
| `Preferencemanager` | `datasource/preferences/Preferencemanager.kt` | Durable token storage in KSafe (`saveTokens:45`, `getTokens:64`, `clearAll:83`). Source of truth. |
| `App.kt` action handler | `app/App.kt:375-416` | Handles `ActionEvent.Login` (rehydrate from storage, else refresh) and `ActionEvent.AuthInvalidated` (toast + `appRepository.logout()` + navigate to Login). |
| `GlobalViewModel` poll loop | `app/GlobalViewModel.kt:185-216` | Every 5 s: reconnect socket if needed, flush offline messages, and raise `ActionEvent.Login` whenever `SessionCache` says logged out. |
| `SocketConnectionManager` | `datasource/network/socket/SocketConnectionManager.kt` | Own reconnect backoff (`:282-301`), and a second, hand-rolled refresh path in `SocketConnection.connect()` (`:319-359`) because the socket client has no Auth plugin. |
| `AppRepository` | `datasource/AppRepository.kt` | `onNewTokenPair:1205` (persist + mirror), `logout:322`, `endServerSession:306`, `deleteAllAppData:285`, `ActionChannel:230`, `ErrorChannel:193`. |

### 1.2 Three copies of one piece of state

1. **`Preferencemanager` (KSafe)** — durable, the real source of truth.
2. **`SessionCache.authState`** — in-memory mirror, drives UI and `requireLoggedIn()`.
3. **Ktor `BearerAuthProvider`'s internal `tokens`** — a third copy, only reachable through `loadTokens` / `refreshTokens` / `clearAuthTokens()`.

Every bug in section 2 is, at root, two of these three disagreeing. The rebuild's central idea is to make (1) the only writable copy and derive (2) and (3) from it deterministically.

### 1.3 Server contract (verified, do not re-derive)

| Fact | Where | Value |
|---|---|---|
| Access token TTL | `core/security/JwtService.kt:21` | 15 minutes |
| Refresh token TTL | `core/security/JwtService.kt:24` | 30 days, and the DB row's `expiresAt` slides forward on every rotation (`AuthService.kt:396`) |
| Refresh rotates | `AuthService.refresh:320` → `rotateTokenRow:380` | Atomic `findAndModify` on (userId, hashedToken). Exactly one concurrent caller wins. |
| One-hop replay recovery | `AuthService.kt:356-367` | A token that was already rotated away is still accepted **once more**: the row that holds it as `previousHashedToken` returns its *current* raw token. Valid until the next rotation replaces `previousHashedToken`. |
| Genuinely dead token | `AuthService.kt:372` | HTTP 401 |
| Login device dedup | `AuthService.kt:149-164` | Keyed on (userId, deviceName, deviceType). Blank device names are never dedup'd. |
| Logout | `AuthService.logout:203` | Deletes the session row. Idempotent, answers 200 even when nothing matched. |
| Rate limit `/auth/refresh` | `RateLimitProperties.kt:19` + `RateLimitFilter.kt:38-41` | `AUTH_REFRESH`, **60 requests / minute per IP**, separate bucket from login |
| Rate limit other `/auth/*` | `RateLimitProperties.kt:14` | `AUTH`, 20 / minute per IP |
| Rate limit global | `RateLimitProperties.kt:12-13` | IP 300/min, USER 300/min |
| 429 response | `RateLimitFilter.kt:109-116` | Status 429, `Retry-After` header in seconds, JSON body `{"error":"rate_limited","retryAfterSeconds":N}` |

**Consequence the rebuild depends on:** because of one-hop replay recovery, *losing a refresh response* (crash, storage write failure, killed process) is recoverable — the old token still works one more time. The client therefore does **not** need write-ahead journalling of token pairs. Losing **two** rotations in a row is not recoverable.

---

## 2. Known defects to fix

Each defect gets a test in section 6. Numbering is stable so it can be referenced from commits.

### D1 — Refresh blocks every request and cannot be cancelled (HIGH)
`TokenManager.kt:138` awaits the in-flight refresh inside `withContext(NonCancellable)`. That await happens inside Ktor's `loadTokens`/`refreshTokens`, which Ktor serialises behind its own lock. A refresh stalled on a bad network runs to the 30 s request timeout (`createHttpClient.kt:83`), every other request queues behind it, and leaving the screen cannot abort it. The app reads as frozen for 30 s.

`NonCancellable` is only genuinely needed around the *persist* step, not the await.

### D2 — Backoff never resets when the network comes back (MEDIUM)
`currentRetryCooldown()` (`TokenManager.kt:157`) doubles 5 s → 5 min per consecutive failure and is only reset by a successful refresh (`:181-184`). Six failures on a flaky link arm a 5-minute window; when the network returns 10 seconds later the session stays unusable for the rest of it, because `refreshTokens` short-circuits to the cached `Retryable` (`:113`) without going near the network.

### D3 — Permanent non-401 failures retry forever and are invisible (MEDIUM)
Only `NetworkingError.Unauthorized` maps to `Invalidated` (`TokenManager.kt:169`). A misconfigured server URL (404), a blocking gateway (403), or a permanent 400 is classified `Retryable` forever, with no user-visible signal. The user sees "offline" indefinitely on a working network.

### D4 — Socket refresh guard trusts the device clock (MEDIUM)
`SocketConnection.connect()` (`SocketConnectionManager.kt:330`) only refreshes when the client believes the access token is expired. A token the *server* rejects but the client considers valid (clock skew, JWT secret rotation, token revoked) never triggers a socket-side refresh. Only an unrelated HTTP 401 heals it.

### D5 — A silently failed token write reports success (MEDIUM)
`AppRepository.onNewTokenPair:1205` catches and rethrows; `TokenManager.doRefresh` (`:187-189`) runs it inside `NonCancellable` and the throw is caught by the outer `catch` (`:197`) → the call is classified `Retryable`, so the *new* token pair is dropped while the server has already rotated. Recoverable exactly once via server replay. Two in a row is a forced logout.

Per the user: acceptable risk, server-handled. Still worth an explicit test that the *second* consecutive loss is what actually invalidates.

### D6 — Desktop device identity is not unique (MEDIUM)
`AppVersion.getDeviceName()` on desktop returns the machine hostname. Server login dedup keys on (userId, deviceName, deviceType) (`AuthService.kt:149-155`), so two desktops with the same hostname on one account share a single refresh-token row and rotate each other out. Two rotations apart defeats the one-hop replay → forced logout. Android embeds `ANDROID_ID`, iOS the vendor id; desktop needs the same.

### D7 — Logout clears credentials last (LOW)
`AppRepository.logout:322` runs `endServerSession()` then `deleteAllAppData()` then `preferencemanager.clearAll()`. A throw in `deleteAllAppData()` leaves the tokens on disk; the 5 s poll loop then rehydrates `SessionCache` straight from them and the user is silently logged back in.

### D8 — `requireLoggedIn()` has a side effect (LOW)
`SessionCache.requireLoggedIn()` (`:115`) sends `ActionEvent.Login` from a synchronous read. Combined with the 5 s loop (`GlobalViewModel.kt:206`) this is what turned a logged-out cache into a refresh storm. A read must not drive the session state machine.

### D9 — No jitter in either backoff (LOW)
`TokenManager.currentRetryCooldown()` and `SocketConnectionManager.scheduleReconnectIfPossible():294` both use pure exponential doubling. Many devices dropped by the same server restart retry in lockstep.

### D10 — `Retry-After` from a 429 is ignored (LOW)
The server sends `Retry-After` and `retryAfterSeconds` (`RateLimitFilter.kt:110-116`). The client throws it away and applies its own guess.

### D11 — Cooldown is keyed on the token string, not on the session (LOW)
`Cooldown(refreshToken, until, error)` (`TokenManager.kt:67`). A fresh login after a failure storm produces a different token, so the cooldown is bypassed correctly — but the *failure counter* (`consecutiveRetryableFailures`) is not reset by login, so the first failure after a fresh login can immediately arm a multi-minute window.

### D12 — Two independent refresh paths (LOW)
HTTP refresh lives in `TokenManager`; the socket has its own hand-rolled variant (`SocketConnectionManager.kt:319-359`) because the socket client has no Auth plugin. Two code paths, two sets of edge cases, one of which (D4) already diverged.

### D13 — Untestable by construction (blocking for this plan)
`TokenManager` reaches into Koin at call time (`:164`, `:188`, `:193`) and talks to global singletons (`SessionCache`, `AppRepository.ActionChannel`). There is no `commonTest` source set in the repo at all. Nothing in section 2 can be regression-tested before the dependencies are inverted.

### D14 — `loadTokens` can perform network I/O (design smell behind D1)
`loadBearerTokens()` (`TokenManager.kt:75-91`) triggers a full network refresh when the access token has under 2 minutes left. Ktor calls this on the request path, holding its token lock. Proactive refresh is the right behaviour; doing it *inside* `loadTokens` is not.

### D15 — Ktor's cached tokens and storage can disagree
`doRefresh` calls `clearAuthTokens()` (`TokenManager.kt:193`) on the authenticated client from inside a callback that Ktor itself invoked — mutating the provider's state re-entrantly. It works today but is unspecified behaviour and there is no test pinning it.

---

## 3. What the rebuilt system has to do (requirements)

### R1 — Single source of truth
Durable storage (`Preferencemanager`) is the only writable copy of the token pair. `SessionCache` and Ktor's `BearerAuthProvider` are derived views, refreshed from storage after every successful write. No code path may write one without the others.

### R2 — Single refresh path
Exactly one component performs `POST /auth/refresh`. HTTP (Ktor Auth plugin), WebSocket handshake, the `Login` action, and push/background entry points all call into it. No second implementation.

### R3 — Single-flight
Concurrent callers share one in-flight refresh. A caller arriving while a refresh runs waits for that result; it never starts a second request against the same refresh token. This is load-bearing: the server rotates the token, so two parallel refreshes with the same input burn the one-hop replay budget.

### R4 — Cancellable waiting, uncancellable committing
Awaiting a refresh must be cancellable (leaving a screen aborts the wait). Persisting a successful token pair must not be cancellable (a half-written session is worse than a slow one). Cancelling the *waiter* must not cancel the *refresh* — a second waiter may still need it.

### R5 — Bounded refresh
A refresh has its own timeout, shorter than the 30 s request timeout it is nested inside (proposed: 10-12 s). It must never be the reason a UI request takes 30 s.

### R6 — Three-way outcome classification
Every attempt resolves to exactly one of:
- **Success** — new pair persisted, derived views updated.
- **Retryable(error, retryAfter)** — the refresh token may still be good; try later. Offline, timeout, 5xx, 429.
- **Invalidated(reason)** — the session is dead. 401 from `/auth/refresh`, blank/absent refresh token, refresh token expired by its own `exp` claim.

Plus a fourth, currently missing, category:
- **Broken(error)** — permanently failing but *not* proof the session is dead: 400, 403, 404, serialization failure. Must not silently log the user out, must not retry forever, and must surface to the user (D3). Proposed handling: retry with the maximum backoff, and after N consecutive `Broken` outcomes raise a distinct user-visible error ("cannot reach this server — check the server URL in settings") without clearing credentials.

### R7 — Backoff with reset triggers
Exponential with jitter, capped. Reset to zero on **all** of:
- a successful refresh,
- a successful *any* authenticated request (proof the network works),
- connectivity transitioning offline → online (`SessionCache.updateOnline(true)`),
- app returning to foreground,
- a fresh login,
- an explicit user action ("retry" / pull-to-refresh).

Honour `Retry-After` from a 429 when it is larger than the computed backoff (D10).

### R8 — Proactive refresh off the request path
Refresh before expiry, but from a background task, not from inside `loadTokens` (D14). `loadTokens` returns whatever is in storage, synchronously, and at most kicks off (does not await) a proactive refresh.

Corollary: a request may still go out with an access token that expires in flight. That is fine — it comes back 401 and the reactive path handles it. The proactive path is an optimisation, not a correctness requirement.

### R9 — 401 handling is context-dependent
A 401 from `/auth/refresh` invalidates the session. A 401 from any *other* endpoint means "access token stale" and triggers exactly one refresh-and-retry. A 401 on the retry (with a token the server just issued) is a server-side authorisation problem, not a session problem — surface it, do not log out.

### R10 — Socket handshake uses the same path
The WebSocket handshake asks the shared component for a token, and on failure asks it to refresh. The guard must not be "does the client think the token is expired" (D4) but "have consecutive handshakes failed", so clock skew and secret rotation are recoverable.

### R11 — Logout is atomic and irreversible from the client's view
Order: clear derived views and durable credentials **first** (in a `finally`, so a throw anywhere else cannot leave them), then best-effort server-side session kill, then data wipe. The current order (`AppRepository.logout:322`) is the reverse.

Open trade-off, needs a decision (see section 8): killing the server session requires the refresh token, which the "clear first" order has already discarded. Proposed resolution: read the refresh token into a local before clearing, clear, then use the local for the best-effort `POST /auth/logout`.

### R12 — Offline is a first-class state, not an error
`SessionCache.isOnline()` must be driven by observed request outcomes (as `trackConnectivity()` already does) and must gate the refresh scheduler: while offline, no refresh is attempted at all, and the moment connectivity returns exactly one refresh fires (not one per queued caller).

### R13 — Every failure path is observable
Each outcome writes one line to `LoggingRepository` with: reason, error code, attempt number, backoff applied. The current implementation logs some paths and not others, which is why the nightly storm took a log-archaeology session to diagnose.

### R14 — Testable without Koin, without a real network, without wall-clock time
All collaborators injected. All time from an injected clock / virtual time. All network through an injectable seam. Section 6 covers this.

---

## 4. Proposed architecture

New package: `datasource/network/auth/`.

```
AuthSessionStore      (interface)  read / write / clear the durable token pair
AuthRefreshApi        (interface)  suspend refresh(refreshToken): NetworkResult<TokenPair, NetworkingError>
AuthEventSink         (interface)  onSessionInvalidated(reason), onSessionRefreshed(), onServerUnreachable(error)
AuthClock             (interface)  now(): Instant                      -- test seam for expiry + backoff
RefreshBackoff        (class)      pure, no I/O: nextDelay(attempt, retryAfter?) + reset triggers
AuthSessionManager    (class)      the state machine; the only caller of AuthRefreshApi
```

`AuthSessionManager` replaces `TokenManager`. Public surface (draft):

```kotlin
suspend fun currentTokens(): TokenPair?              // storage read, no I/O, may kick off proactive refresh
suspend fun refresh(reason: RefreshReason, presentedToken: String?): RefreshOutcome
fun onAuthenticatedRequestSucceeded()                // backoff reset trigger (R7)
fun onConnectivityRestored()                         // backoff reset trigger (R7)
fun onLoggedIn(tokens: TokenPair)                    // seeds state after a password login
suspend fun clearSession()                           // used by logout, R11
val state: StateFlow<AuthSessionState>               // Unknown / Active / Refreshing / Degraded / Invalidated
```

`RefreshReason` exists purely for logging and for the "should this even be attempted" gate (proactive refreshes are skipped while offline; a reactive 401 refresh is attempted even when the online flag is stale).

### Why the interfaces

- `AuthRefreshApi` breaks the Koin cycle that forced the service-locator lookups (D13). The real implementation only needs the **non-authenticated** HTTP client, `Preferencemanager` (for the server URL) and `AppVersion` (device name/type) — none of which depend on `AuthSessionManager`. There is no cycle, so it can be constructor-injected normally.
- `AuthEventSink` replaces the direct `AppRepository.ActionChannel` / `SessionCache` calls, so tests can assert "the session was invalidated exactly once" without a running app.

### Wiring changes

- `Modules.kt:85` — `singleOf(::TokenManager)` becomes the new manager plus its three interface bindings.
- `createHttpClient.kt:51-74` — `loadTokens` becomes a pure storage read; `refreshTokens` calls `AuthSessionManager.refresh(RefreshReason.Reactive401, oldTokens?.refreshToken)`.
- `SocketConnectionManager.kt:319-359` — the hand-rolled block collapses to: get tokens, handshake, on failure count it and ask the manager to refresh (R10).
- `App.kt:375-395` — the `Login` action stops calling refresh directly; it asks the manager to ensure a session and reacts to the returned outcome.
- `GlobalViewModel.kt:206` — stops raising `Login` on a 5 s timer; the manager's own scheduler owns retry timing (D8).
- `SessionCache.requireLoggedIn():115` — drops the side effect (D8) and becomes a pure read.

---

## 5. Edge case catalogue

Everything below has to be either handled or explicitly declared out of scope. Grouped by trigger.

### 5.1 Concurrency
1. Two screens fire authenticated requests simultaneously, both get 401 → exactly one `/auth/refresh` (R3).
2. A refresh is in flight when a second caller arrives with the *same* old token → joins the in-flight one.
3. A refresh is in flight when a second caller arrives with a *different* (older) token → must not start a second refresh; joins, then re-reads storage.
4. The waiter is cancelled (screen left) while the refresh continues → refresh completes and persists; no orphaned state (R4).
5. **All** waiters are cancelled → refresh must still complete and persist, because the tokens are already rotated server-side.
6. HTTP refresh and WebSocket handshake refresh race → same single-flight (R2, R10).
7. Password login completes while a refresh for the *old* session is in flight → the in-flight result must not overwrite the newer login tokens. Needs a session generation counter, not just a token comparison.
8. Logout runs while a refresh is in flight → refresh result is discarded, not persisted (else logout is undone).
9. Two processes (Android app + FCM service) refresh concurrently → single-flight is per-process and cannot cover this. One will lose and be rescued by server replay. Document as accepted; the only real fix is a cross-process lock.

### 5.2 Bad / flaky network
10. Refresh times out at the client → `Retryable`, backoff, no user-visible logout.
11. Refresh request *is* delivered, response is lost → server rotated, client keeps the old token → next attempt is rescued by one-hop replay.
12. Two consecutive lost responses → server-side `previousHashedToken` has moved on → 401 → genuine forced logout. Unavoidable client-side; verify the user gets a clear message, not a silent failure.
13. Connectivity flaps during backoff → the online→offline→online transition resets the backoff (R7); flapping must not cause a request storm, so the reset itself is debounced (proposal: at most one reset-triggered refresh per 5 s).
14. Captive portal returns HTTP 200 with an HTML body → `SerializationException` → currently `SerializationError`. Must classify as `Broken`, not `Retryable`-forever and not `Invalidated`.
15. DNS failure / no route → `NoInternetConnection` → `Retryable`, and must **not** flip the app into a state that hides the login screen.
16. Server returns 502/503 during a deploy → `Retryable` with backoff; must recover without user action when the server comes back.
17. 429 with `Retry-After: 47` → wait at least 47 s (D10).
18. Very slow network where the refresh takes 25 s → capped by the refresh timeout (R5) at ~10-12 s, reported `Retryable`, not left blocking the request path.

### 5.3 Token / clock
19. Access token expired, refresh token valid → normal reactive refresh.
20. Both expired (app unused for > 30 days) → `Invalidated` on the local `exp` check, without spending a network request.
21. Refresh token valid by `exp` but deleted server-side (logged out from another device / "log out everywhere") → 401 → `Invalidated`.
22. Device clock is hours *behind* → client thinks tokens are still valid, server rejects them → must recover via the 401 path, never loop (D4).
23. Device clock is hours *ahead* → client thinks a good token is expired → refreshes early. Harmless but must not loop: a `Success` that returns a token the client also considers expired must not immediately trigger another refresh. Needs a floor on refresh frequency independent of expiry maths.
24. Server JWT secret rotated → every token invalid → 401 → `Invalidated` → forced logout with a clear message.
25. Malformed / truncated token in storage (partial write, storage corruption) → `JwtUtils` returns defaults; must be treated as `Invalidated`, not crash.
26. `exp` claim missing entirely → `isTokenDateValid` returns false → treated as expired. Correct; pin with a test.

### 5.4 Storage
27. Token write throws → D5. Server already rotated. Must return an outcome that makes the *next* attempt use the old token (so replay recovery applies) — i.e. do **not** report `Success`, and do **not** arm a long backoff that delays the recovery.
28. Token read throws → treat as no session, do not crash the app.
29. Storage returns empty after an OS-level keystore reset → `Invalidated`, clean navigation to Login.
30. Two writers (refresh + password login) → last write wins; the generation counter (case 7) decides which is last, not wall-clock ordering.

### 5.5 Lifecycle
31. App killed mid-refresh → next start reads the old token, replay recovery applies.
32. App backgrounded mid-refresh → refresh must complete (`NonCancellable` persist), not be cancelled by scope teardown.
33. Android FCM entry point rehydrates a session from storage while the main process is refreshing → accepted divergence (case 9).
34. Cold start with valid stored tokens → no `/auth/refresh` at all; hydrate `SessionCache` from storage. (This is the fix already applied at `App.kt:385`; keep it.)
35. Cold start with expired access + valid refresh → exactly one refresh, then proceed.
36. Cold start offline → no refresh attempt; app opens in offline mode with the cached session, and refreshes when connectivity returns.
37. Desktop app minimised for 8 hours → socket reaped by the server after 75 s of no pong; reconnect backoff must not spend a refresh per attempt (this is the fixed nightly storm — keep a regression test).

### 5.6 Logout
38. Logout while offline → local logout completes, server session dies only at expiry. Must not hang (bounded by `LOGOUT_SERVER_CALL_TIMEOUT`, `AppRepository.kt:306`).
39. Logout while a refresh is in flight → case 8.
40. Logout throws mid-way → credentials still gone (R11, D7).
41. Server-forced logout (401 on refresh) → toast + navigate to Login + credentials cleared, exactly once even if several callers observed the same 401.
42. `AuthInvalidated` fired twice concurrently → one toast, one navigation, one wipe. Needs idempotence, currently unguarded.
43. Logout, then immediately log in as a different user → all derived state (`SessionCache`, Ktor cached bearer tokens, backoff counters, single-flight deferred) reset. D11 covers the counter.

### 5.7 Rate limiting
44. Client hits the 60/min `AUTH_REFRESH` bucket → 429 → back off per `Retry-After`; must never turn a 429 into a logout.
45. Several devices behind one NAT share the per-IP bucket → one client's misbehaviour rate-limits the others. Client-side mitigation is jitter + hard floor between refreshes; the real fix is server-side (section 7).
46. A 429 on a *normal* endpoint must not be mistaken for an auth problem.

### 5.8 Server-behaviour edge cases
47. Refresh succeeds but returns the *same* refresh token (replay-recovery path, `AuthService.kt:361-367`) → client must accept it and not treat "token unchanged" as a failure.
48. Refresh returns a token for a different user (should be impossible) → detect via the `sub` claim and treat as `Invalidated` rather than silently switching accounts.
49. Server sends 200 with an empty body → `SerializationError` → `Broken` (case 14).

---

## 6. Test plan

There is no test source set today (D13). This is the first deliverable.

### 6.1 Infrastructure
- Add `composeApp/src/commonTest/kotlin/...`. `commonTest.dependencies { implementation(libs.kotlin.test) }` already exists in `composeApp/build.gradle.kts`.
- Add to `gradle/libs.versions.toml` (per the repo rule: version catalog only, no hardcoded versions):
  - `kotlinx-coroutines-test` — virtual time, `runTest`, `TestScope`, `advanceTimeBy`. Mandatory: every backoff assertion depends on it.
  - `ktor-client-mock` — for the handful of tests that must exercise the real Ktor `Auth` plugin rather than a fake `AuthRefreshApi`.
  - Optionally `turbine` for `StateFlow` assertions (the `android-testing` skill already names it).
- Run target: `:composeApp:jvmTest`. Fast, no emulator. (Note: the repo rule is "never run Gradle unless asked" — running the tests is explicitly in scope for the implementation phase, nothing else is.)

### 6.2 Fakes
- `FakeAuthSessionStore` — in-memory, with switches to throw on read and on write (cases 27, 28).
- `FakeAuthRefreshApi` — scriptable queue of outcomes, records every call with the token presented and the virtual timestamp. This is what proves single-flight and backoff timing.
- `FakeAuthClock` — settable instant, so token expiry can be simulated without minting real JWTs. Requires the expiry check to go through the injected clock rather than `Clock.System` inside `JwtUtils`.
- `RecordingAuthEventSink` — collects invalidation / unreachable events.
- Test JWT builder — mints signature-less JWTs with arbitrary `sub` and `exp` so `JwtUtils` can parse them (cases 19-26, 48).

### 6.3 Test suites (one per requirement/defect, minimum)

| Suite | Covers | Key assertions |
|---|---|---|
| `SingleFlightTest` | R3, cases 1-3, 6 | N concurrent callers → exactly 1 API call; all N get the same outcome |
| `CancellationTest` | R4, D1, cases 4, 5, 32 | Cancelling waiters does not cancel the refresh; the pair is still persisted; a cancelled waiter's parent scope is not blocked |
| `BackoffTest` | R7, D2, D9, D11, cases 13, 17, 44 | Doubling with jitter within bounds; cap respected; reset on each of the five triggers; `Retry-After` honoured when larger; counter reset on login |
| `ClassificationTest` | R6, D3, cases 14, 15, 16, 21, 24, 46, 49 | Every `NetworkingError` subtype maps to exactly the intended outcome; 401-on-refresh is the *only* thing that invalidates |
| `TimeoutTest` | R5, case 18 | A refresh that never returns resolves `Retryable` within the refresh timeout, well under 30 s |
| `ProactiveRefreshTest` | R8, D14, cases 19, 23 | `currentTokens()` never awaits the network; near-expiry schedules at most one background refresh; a refresh floor prevents a loop under clock skew |
| `TokenIntegrityTest` | cases 20, 25, 26, 47, 48 | Expired/malformed/missing-`exp`/foreign-`sub` tokens each produce the right outcome without a crash |
| `StorageFailureTest` | D5, cases 27-30 | A write failure does not report `Success`; the next attempt presents the *old* token; the generation counter picks the right winner |
| `LogoutTest` | R11, D7, cases 8, 38-43 | Credentials gone even when a later step throws; refresh-in-flight result discarded; invalidation is idempotent |
| `ConnectivityTest` | R12, cases 13, 15, 36 | No refresh while offline; exactly one on restore; flapping is debounced |
| `KtorIntegrationTest` | R9, D15, cases 1, 46 | Using `MockEngine` + the real `Auth` plugin: one 401 → one refresh → one retry; a second 401 does not loop; a 429 on a normal endpoint does not touch auth state |
| `SocketAuthTest` | R10, D4, cases 22, 37 | Consecutive handshake failures eventually force a refresh even when the client thinks the token is valid; a reconnect storm spends at most one refresh per backoff window |

### 6.4 Non-goals for the test suite
- Real network I/O, real server, real KSafe/DataStore.
- Compose UI tests. The navigation on `AuthInvalidated` is asserted through the event sink, not through the UI.
- Cross-process behaviour (case 9, 33). Documented, not tested.

---

## 7. Server-side follow-ups (not required by this plan)

Listed so they are not lost; each is independent and can be done later.

- **S1** — `/auth/refresh` is rate limited per IP (`RateLimitFilter.kt:41`). Keying it on the token's subject instead would stop one looping client behind a NAT from starving everyone else (case 45).
- **S2** — the replay grace window never expires: `previousHashedToken` stays mintable until the next rotation, and `rawToken` is stored in plaintext next to the hash (`AuthService.kt:381-396`). Timestamping the rotation and rejecting old replays would bound the exposure.
- **S3** — `rotateTokenRow` overwrites `deviceName`/`deviceType` from the request body unverified (`AuthService.kt:393-394`), so any token holder can relabel a session and corrupt the device list.
- **S4** — no access-token revocation: "log out everywhere" leaves already-issued access tokens usable for up to 15 minutes.

---

## 8. Decisions needed before implementation

1. **Logout ordering vs. server session kill** (R11). Read the refresh token into a local, clear storage, then best-effort `POST /auth/logout` with the local? Or keep the current order and accept D7? Recommended: the former.
2. **`Broken` outcome UX** (R6, D3). What does the user see after N consecutive 400/403/404/serialization failures? Proposed: a persistent banner "cannot reach this server" with a link to the server-URL setting, no logout.
3. **Backoff constants.** Proposed: base 2 s, factor 2, cap 2 min (not the current 5 min), jitter ±25 %, hard floor of 1 s between any two refresh attempts. The current 5-minute cap is what makes D2 painful.
4. **Refresh timeout** (R5). Proposed 12 s (well under the 30 s request timeout, above a realistic slow-3G round trip).
5. **Desktop device name** (D6). Add a random per-install id to the hostname? That changes the server-side dedup key once, on the next login, and will leave one stale row per desktop to age out via `expiresAt`. Confirm that is acceptable.
6. **Does the 5 s `GlobalViewModel` loop stay?** (D8). It also drives socket reconnect and offline-message flushing, so it cannot simply be deleted — only its `ActionEvent.Login` branch (`GlobalViewModel.kt:206`) is in scope here.
7. **Do the push/background entry points** (`AppFirebaseMessagingService.kt:59`, `MarkAsReadReceiver.kt:45`, `IosPushDelegateBridge.kt:30/52/76`) get the full manager, or keep using `SessionCache.loginIfValid` and stay read-only? Recommended: keep them read-only, because a background process refreshing is exactly case 9.

---

## 9. Implementation order

1. Version catalog + `commonTest` source set + fakes + test-JWT builder. No production code yet.
2. The interfaces (`AuthSessionStore`, `AuthRefreshApi`, `AuthEventSink`, `AuthClock`) with real implementations that delegate to today's classes, wired in Koin. `TokenManager` unchanged and still live.
3. `RefreshBackoff` (pure) + its tests. This is where D2, D9, D10, D11 die.
4. `AuthSessionManager` + the full suite from 6.3, running against fakes, with `TokenManager` still the one wired into the app.
5. Cut over: Ktor Auth plugin, `App.kt` Login action, `SocketConnectionManager`, `GlobalViewModel`, `SessionCache.requireLoggedIn`. Delete `TokenManager`.
6. Logout rework (R11) + its tests.
7. Desktop device name (D6) — separate commit, it changes server-visible behaviour.

Steps 1-4 are additive and cannot regress the running app. Step 5 is the only risky one and is where manual verification (airplane-mode toggling, laptop sleep, server restart) belongs.
