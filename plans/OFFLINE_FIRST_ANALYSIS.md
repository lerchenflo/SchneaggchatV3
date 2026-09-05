# Offline-First Analysis — Message Flows

Analysis date: 2026-09-01. Traced end-to-end through `composeApp/src/commonMain` (send, offline send, receive, sync, read receipts, edits/reactions, media, connectivity, startup). Line numbers refer to the current working tree. Companion doc: `E2E_ENCRYPTION_PLAN_V2.md` (its four data-layer boundary sites are the same sites named here).

## TL;DR

The app is already *half* offline-first: the UI is 100 % Room-flow-driven, text/image/audio sends are optimistic-local-first with a `sent = 0` queue, and startup works fully offline. What breaks the "true offline-first" claim:

1. **Every non-send mutation is network-first and fire-and-forget** (reactions, poll votes, edits, deletes, read receipts, group/profile changes) — done offline they are silently lost or (edits) even corrupted into duplicates.
2. **Destructive Room migration** (`fallbackToDestructiveMigration(dropAllTables = true)`) wipes the unsent queue and all history on every schema bump.
3. **No connectivity observer** — online state defaults to `true`, offline is only discovered by a failed request, recovery is a 5-second polling loop.
4. **No idempotency** — retries can duplicate messages server-side.
5. Send failures are invisible except a tick glyph (`println` only, no error event).

---

## 1. Current flow inventory

### 1.1 Send (TEXT / IMAGE / AUDIO / POLL) — local-first ✅ (with gaps)

- UI: `ChatAction.OnSendClick` → `ChatViewModel.sendMessage` (`chat/presentation/chat/ChatViewModel.kt:204-296`). Launched on `applicationScope` (survives leaving the screen), fire-and-forget, input cleared before the network resolves.
- Repository: `AppRepository.sendMessage` (`datasource/AppRepository.kt:1692`):
  - **Optimistic insert first**: `MessageDto(id = null, sent = false, sendDate = now)` written to Room *before* the network call (`:1804-1807`). The row's `localPK` is the local identity; server `id` stays `null` until success.
  - Network: `POST /messages/send/{text|image|audio|poll}` via `safeCall` (never throws; maps transport errors to `NetworkResult.Error` + `setOffline()`).
  - **Success** (`:1870-1921`): row replaced in place (same `localPK`), server `id` + server `sendDate` assigned, `sent = true`, `version` deliberately kept so the sync cursor is not advanced. Image/audio: server copy re-downloaded, `unsent_*` staging file deleted.
  - **Failure** (`:1849-1869`): row simply left with `sent = false` (that *is* the queue); image/audio bytes spilled to disk as `unsent_<localPK>…`. **No user feedback beyond the "not sent" tick** (`MessageContent.kt:127-134`), no `ErrorChannel` event, only `println`.
- No encryption on the send path today; `CryptoUtil.encrypt` has zero call sites. (E2E plan V2 keeps the queue plaintext-in-Room and encrypts at real send time — compatible with everything below.)

### 1.2 Offline queue & retry — exists, primitive

- Queue = `SELECT * FROM messages WHERE sent = 0` (`Daos.kt:136-138`).
- Drain: `AppRepository.sendOfflineMessages` (`:1937-2031`) — serial, head-of-line: re-enters `sendMessage(localpk = …)`; stops as soon as the head message fails again (`:2026`). **One permanently failing message blocks the whole queue forever.**
- Triggers: app resume, notification-tap, pull-to-refresh, and a **5-second infinite polling loop** in `GlobalViewModel.kt:138-169`. No backoff, no WorkManager/BGTaskScheduler (only a commented-out WorkManager block at `AppRepository.kt:2671-2717`) — nothing drains the queue while the app is backgrounded/killed.
- Idempotency hole: `sendMessage`'s `messageId` parameter is passed as `null` at every call site. If the server persists a message but the response is lost (timeout, process death mid-send), the row stays `sent = 0` and the retry **creates a server-side duplicate**.
- Locking: `sendMessageLock` is held across the whole HTTP round-trip (`:1694`) — a slow image upload serializes every outgoing message app-wide; `sendOfflineMessages` busy-waits on it in a 15 ms poll loop (`:1942-1946`).

### 1.3 Receive — socket + push + delta sync ✅ (solid design)

- WebSocket: `SocketConnectionManager` — foreground-only, exponential backoff capped 30 s, plus the parallel 5 s reconnect poll in `GlobalViewModel` (backoff effectively neutralized on mobile). All incoming events (`messagechange`, `userchange`, `groupchange`, map/events/presence/locations) are written **to Room**, never to UI state (`SocketConnectionMessage.kt:155-473`). Socket messages are stored with `version = existing ?: 0` so pushes can never advance the sync watermark — correct.
- Message sync = true versioned delta: `messageIdSync` (`AppRepository.kt:2042-2116`) pages `GET /messages/sync?since=MAX(version)` — cursor derived from the DB itself, no separate stored timestamp. Deletions applied before updates. Error → break without advancing. Correct offline-first shape.
- Users/groups/map/events sync = **id + updatedAt diff where the client uploads its entire local index each time** — O(n) payload growth, no paging for users/groups.
- Push (Android FCM data-message / iOS): payload carries content; body rendered locally; push additionally triggers `messageIdSync()` from a possibly cold process. Desktop: permanent socket, no push.
- `dataSync` runs all jobs concurrently, each failure isolated, never blocks UI — status only surfaces as a small icon (`ChatSelector.kt:322-350`).

### 1.4 Startup offline ✅

`AutoLoginCredChecker → loadSavedLoginConfig` (`AppRepository.kt:1245-1284`) is 100 % local (KSafe tokens + JWT expiry check + Room user row). Chat list and chat screen render straight from Room flows. Fully usable offline from cold start.

### 1.5 Read receipts — local-first but lossy ❌

`setAllChatMessagesRead` (`AppRepository.kt:2153-2162`): Room updated first (good), then `POST /messages/setread` whose result is **discarded**. Offline: local `readByMe` is already 1, so nothing will ever re-send the receipt — **read state silently diverges from the server permanently** (peers keep seeing "unread").

### 1.6 Edit / delete / reaction / poll vote — network-first ❌

| Flow | Site | Offline behavior |
|---|---|---|
| Edit (sent msg) | `AppRepository.kt:2165-2207` | Network-first. **Failure branch corrupts data**: flips `sent = false` on a row that has a server `id`; `sendOfflineMessages` later re-sends it as a *new* message → server-side duplicate, original stays unedited (`:2183-2193`, error event commented out). |
| Edit (unsent msg) | `:2167-2176` | Purely local — fine. |
| Delete | `:2213-2227` | Network-first, no optimistic removal; offline → error snackbar, nothing queued. |
| Reaction | `:2229-2247` | Network-first, no optimistic update; offline → error snackbar, lost. |
| Poll vote / delete option | `:2251-2292` | Same, lost. |
| Group/profile mutations, event join, map upserts | various | Fire-and-forget network-only, lost offline. |

### 1.7 Connectivity detection — reactive only ❌

- No `ConnectivityManager` / `NWPathMonitor` / any connectivity abstraction anywhere.
- `SessionCache._onlineFlow` **defaults to `true`** (`app/SessionCache.kt:104`); set `false` only inside `safeCall` exception branches (with string-matching heuristics for iOS `NSURLError`); set `true` on any successful call.
- Consequence: first action after a cold offline start eats the full 10 s connect timeout; recovery detection is `testServer()` polled every 5 s.

### 1.8 Media ✅ (mostly)

Files on disk, path/filename in Room, no base64 in DB. Self-healing re-download (`getMissingPics`/`getMissingAudios`) as a MEDIA sync job. Two gaps:
- A just-sent image renders **empty** until the server round-trip completes — the locally chosen bytes are staged to `unsent_*` but never used as the displayed image (`ImageMessageContentView.kt:50`).
- `Message.toDto()` drops `audioPath` (see bugs).

### 1.9 Not persisted at all (blank offline)

- Non-friend user search (`NewChatViewModel` holds raw network response in memory).
- Roadmap screen (GitHub-direct), changelog popup.
- Online presence (in-memory by design — correct).

---

## 2. Concrete bugs found while tracing

1. **Failed edit → server-side duplicate message.** `AppRepository.kt:2183-2193` sets `sent = false` on a row carrying a server `id`; `sendOfflineMessages` (`:2020`) re-sends it with `messageId = null` as a brand-new message. Original never edited.
2. **`Message.toDto()` silently drops `audioPath`** (`Message.kt:165-190` — no `audioPath =` line, unlike `toMessage()`). Every `upsertMessage` from sync/socket/reaction/edit nulls the stored audio path; only survives via direct-DAO `updateAudioPath`.
3. **Read receipts lost offline permanently** (§1.5) — local flag flipped before network, result discarded, never retried.
4. **Dead null-guard in offline audio retry**: `AppRepository.kt:2003-2006` — `if (m.audioPath == null) { TextContent(...) }` value discarded, always falls through; `m.audioPath` is always null anyway because the AUDIO DTO built at `:1783-1799` never sets it.
5. **Reply to unsent message loses the link**: `answerid = replyTo?.id` is `null` while the target is queued; never back-filled.
6. **Send failure invisible**: only a `println` (`:1851`) + tick glyph. `deleteMessage`/`reactToMessage` use `ErrorChannel`; send does not.
7. **Draft restore condition looks inverted**: `ChatViewModel.kt:681` requires current text *non*-empty before restoring a draft.
8. **Schema export drift**: Room version 79 but latest committed schema JSON is 70; no `Migration` objects exist; `fallbackToDestructiveMigration(dropAllTables = true)` (`CreateDatabase.kt:12`).
9. Dead code: `MessageDao.markMessageAsSent` (`Daos.kt:148-150`) — no callers.
10. `handleSocketConnectionMessage` swallows every exception with `printStackTrace()` (`SocketConnectionMessage.kt:471-473`) — malformed pushes invisible in production.

---

## 3. What must change for true offline-first

### P0 — data loss / correctness

1. **Stop destructive migrations wiping the outbox.** A version bump today deletes queued unsent messages and all cached history, forcing full re-sync. Options (ascending effort): (a) commit exported schemas again + Room auto-migrations for additive changes; (b) hand-written `Migration` objects for the rare destructive change; (c) at absolute minimum, snapshot `sent = 0` rows + `unsent_*` files before a destructive wipe and replay them after re-sync.
2. **Introduce a generic outbox (operation queue) table.** One Room table: `{opId (client UUID), type (SEND/EDIT/DELETE/REACT/VOTE/READ/…), targetLocalPK/serverId, payload JSON, createdAt, attempts, lastError, state}`. All mutations write locally first (optimistic), enqueue an op, and a single drain worker replays in order. This subsumes and fixes §1.5, §1.6, and the edit-duplication bug in one mechanism. The current `sent = 0` message queue can stay as-is initially and migrate into the outbox later.
3. **Client-generated message ID for idempotency.** Generate a UUID at insert time, send it with every send/retry (the `messageId` parameter and server-side plumbing already exist — currently always `null`), server dedupes on it. Eliminates duplicate-on-lost-response and fixes the edit-retry duplication (edits carry the server id instead of re-sending as new).
4. **Fix the failed-edit branch** (`AppRepository.kt:2183-2193`): never flip `sent = false` on a row with a server `id`. Until the outbox exists: revert content and surface the error (`sendErrorSuspend` is right there, commented out).
5. **Make read receipts retryable**: either an outbox op, or a `readSyncedToServer` flag per chat/message retried by the drain loop.
6. **Fix `Message.toDto()` dropping `audioPath`** — one-line mapper fix.

### P1 — offline-first architecture

7. **Real connectivity observer** — `expect/actual ConnectivityObserver` (Android `ConnectivityManager.NetworkCallback`, iOS `NWPathMonitor`, desktop keep polling or java.net check) feeding `SessionCache.onlineFlow`. Kills the "default true, discover offline via 10 s timeout" behavior and turns the 5 s poll loops into event-driven reactions: connectivity regained → drain outbox + reconnect socket + `dataSync`.
8. **Optimistic UI for all mutations**: reactions, poll votes, deletes, edits apply to Room immediately (with a pending marker), reconciled by the outbox drain / next sync. Delete keeps a tombstone until acked.
9. **Per-op retry state with backoff instead of head-of-line blocking.** Today one failing message freezes the entire queue (`:2026`). Outbox drain: skip-after-N-attempts, exponential backoff per op, terminal-failure state surfaced to the user (tap-to-retry on the message).
10. **Surface send failures**: emit `ErrorChannel` event (or at least mark the message row with a failed state distinct from "queued") + tap-to-retry / delete affordance in the chat UI.
11. **Show the local image immediately after sending** — use the `unsent_*` staged file (or the pre-downscale bytes) as `pictureUrl` until the server copy arrives.
12. **Reply linking via `localPK`**: store `answerLocalPK` alongside `answerId`; back-fill `answerId` when the target message gets its server id.
13. **Stop holding `sendMessageLock` across network I/O** — the outbox drain worker (single coroutine, ordered) replaces both `sendMessageLock` and `sendOfflineLock`, and removes the 15 ms busy-wait.
14. **Background drain**: WorkManager (Android) / `BGProcessingTask` (iOS) job that drains the outbox when connectivity returns while the app is backgrounded. The commented-out WorkManager block at `AppRepository.kt:2671` was the right instinct.

### P2 — robustness & scale

15. **Paging for message queries** — `getMessagesByUserIdFlow` and the chat-list flow load entire tables with no `LIMIT`; fine at current size, will not stay fine. Room `PagingSource` or windowed queries.
16. **Move users/groups sync to a version cursor** like messages — the current design uploads the full local id+timestamp index every sync (O(n) forever, single unpaged request).
17. **Log socket handler failures** to `loggingRepository` instead of `printStackTrace`.
18. **Persist friend-search-adjacent data where sensible** (pending friends already are; non-friend search legitimately needs network — just show an explicit offline state instead of blank).
19. Fix the inverted draft-restore condition (`ChatViewModel.kt:681`); delete dead `markMessageAsSent`; remove the dead audio null-guard (`:2003-2006`).
20. Cache roadmap/changelog responses (nice-to-have; explicit offline placeholder is enough).

### Interaction with E2E plan V2

The outbox design is compatible with `E2E_ENCRYPTION_PLAN_V2.md` as written: queue stores plaintext in Room, encryption happens at real send time inside `sendMessage` (plan §Phase 2, "offline queue unchanged"). The client-generated UUID (item 3) should land **before** E2E, since encrypted retries make server-side content-based dedup impossible. The decrypt-miss `pendingContent` healing loop from the plan slots naturally into the same drain/sync machinery.

### Suggested order

1. Quick fixes, independently shippable: items 4, 5, 6, 10, 19 (one PR).
2. Connectivity observer (7) — removes polling, prerequisite for event-driven drain.
3. Client message UUID + server dedupe (3) — small server change, kills duplicates.
4. Outbox table + drain worker (2, 8, 9, 13) — the core architectural change.
5. Migration strategy (1) — before the next schema bump if possible.
6. Background drain (14), paging (15), sync cursors (16) — afterwards.
