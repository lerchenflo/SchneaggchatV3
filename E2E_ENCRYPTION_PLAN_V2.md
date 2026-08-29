# End-to-End Encryption Plan V2 — "Matrix-level"

Supersedes `E2E_ENCRYPTION_PLAN.md` (v1). Same repos, same boundary philosophy, much stronger cryptography. Design is modeled on Matrix (Megolm sender sessions + encrypted key backup + signatures + verification), not on pure Signal — because Signal's model (per-device keys, Double Ratchet only, no server history) is fundamentally incompatible with Schneaggchat's "new device re-syncs full history from the server" architecture. Matrix was built for exactly that architecture.

**Goal**: the server cannot read message text, cannot silently substitute keys without detection, removed group members cannot read future messages, and a leaked identity key alone does not decrypt past traffic picked up after rotation windows. History still survives a new-device login via password-protected key backup.

## What v2 adds over v1

| Property | v1 | v2 |
|---|---|---|
| Server can't read text | ✅ | ✅ |
| Group key rotation on member removal | ❌ | ✅ (per-sender session rotation) |
| Sender authenticity in groups (member can't forge another member) | ❌ (shared AES key) | ✅ (per-session signatures) |
| Key-substitution detection | ❌ (silent MITM possible) | ✅ (signed prekeys + safety numbers + key-change warnings) |
| Forward secrecy | ❌ | Partial: in-session hash ratchet + session rotation + prekey expiry. Deliberately bounded by key backup (see honesty section) |
| Post-compromise security | ❌ | Partial (session rotation re-keys via fresh ephemeral ECDH) |
| 1:1 and groups share one mechanism | ❌ (two schemes) | ✅ (sender sessions everywhere) |
| History on new device | ✅ | ✅ (encrypted key backup, Matrix-SSSS-style) |
| Per-device keys, deniability, sealed sender | ❌ | ❌ (out of scope, see future work) |

## Current state (verified 2026-08-29)

- **Nothing of v1 is implemented.** No `keys` package, no `encryptionVersion`, no `E2eEncryptionManager`, no `publicKey` field anywhere in either repo. v2 therefore replaces v1 outright — no v1 compatibility layer needed. `encryptionVersion = 1` stays reserved/unused; this plan uses `2`.
- Server still pushes plaintext wrapped with the global `jwtSecret.take(20)` key (`JwtService.kt:20`, `FirebaseService.kt:121/170`, `ApnsService.kt:122/165`).
- Client crypto: `dev.whyoleg.cryptography` 0.6.0 `provider-optimal` in commonMain — provides ECDH P-256, ECDSA P-256, HKDF-SHA256, HMAC-SHA256, AES-256-GCM, PBKDF2, SecureRandom. **No X25519/Ed25519 in 0.6.0** → all DH/signatures run on P-256 (protocol-equivalent; Matrix/Signal use Curve25519 but nothing in the constructions requires it).
- Secure storage: `Preferencemanager` `SecureKey` enum + KSafe 3.0.0 (`Preferencemanager.kt:33-42`); `clearAll()` auto-wipes new enum entries on logout.
- Room DB version 79, destructive migration, no Migration objects — schema changes are one version bump + full re-sync.
- The four data-layer boundary sites from v1 are confirmed and unchanged: send (`AppRepository.kt:1819`), send-echo (`AppRepository.kt:1870-1911`), sync (`AppRepository.kt:2074`), socket (`SocketConnectionMessage.kt:172-189`). Sync + socket funnel through `MessageResponseMapper.toDomainMessage` (must become `suspend`).
- Server socket delivers realtime events to only the **first** connection per user (`SocketConnectionHandler.kt:57` uses `find`, not `filter`) → socket events are hints only; pull endpoints are the source of truth for key distribution.

## Decisions

| Question | Decision |
|---|---|
| What is encrypted | Text messages (v2). Media/captions, polls, reactions plaintext — future work. |
| Protocol model | Matrix-style: per-sender **message sessions** (hash ratchet) for content, ECIES against **signed prekeys** for session distribution, **encrypted key backup** for history. |
| Key granularity | Per **account**, not per device (no device registry exists server-side; all devices restore the same key bundle from backup). Documented deviation from Matrix/Signal — see future work. |
| Trust model | Signed prekeys chained to an identity signing key; trust-on-first-use + safety-number verification UI + key-change warnings (Signal-style). |
| History on new device | Survives via key backup encrypted under a password-derived key. Email password reset ⇒ backup unrecoverable ⇒ new identity, history placeholders. |
| Existing messages | Stay plaintext. `encryptionVersion` per message: 0 = plaintext, 2 = v2 envelope. |
| Local Room storage | **Plaintext + flag**, exactly as v1. All crypto in the data layer at the four boundary sites. UI, search, reply previews, local notifications untouched. |

## Crypto design

All primitives from `dev.whyoleg.cryptography` 0.6.0. Every HKDF/HMAC use gets a distinct `info`/domain-separation string prefixed `schneaggchat-e2e-v2:`.

### Key hierarchy

1. **Identity key `IK`** — ECDSA P-256 signing pair, one per account. Root of trust; its public half is what safety numbers commit to. Private half lives in KSafe + key backup.
2. **Signed prekey `SPK`** — ECDH P-256 pair, public half signed by `IK`. Rotated every 14 days (lazy, on app start); old private halves kept 30 days for in-flight shares, then deleted from device and backup (this deletion is what creates the forward-secrecy window). Identified by an incrementing `spkId`.
3. **Message session** (Megolm-analog) — per chat, per sender, per device instance:
   - `sessionId` = random 128-bit id; state = 32-byte ratchet value `R_i` + index `i`.
   - Ratchet: `R_{i+1} = HMAC-SHA256(R_i, "advance")`; per-message key: `HKDF-SHA256(R_i, info = "...:msgkey:" + sessionId + ":" + i)` → AES-256-GCM key. Old `R` values are discarded after advancing ⇒ within a session, a stolen current state cannot decrypt earlier messages.
   - Each session carries its own ECDSA P-256 **session signing pair**; its public half is bound into the session export, which is itself signed by the sender's `IK` ⇒ receivers can prove which member sent a message (no in-group forgery).
   - **Rotation**: immediately on any membership change of the chat (both add — see Phase 3 — and remove), otherwise after 500 messages or 7 days. Rotation = new random session, distributed fresh.
4. **Backup key `BK`** — random 32 bytes. Wrapped with AES-256-GCM under `PBKDF2-SHA256(password, random 16-byte salt, 600 000 iterations)`. Every session key the client learns (own outbound at creation, inbound at import) plus `IK`/current `SPK` privates are encrypted under `BK` and appended to the server-side backup. Password change client-side = re-wrap `BK` only (one small blob), atomic with the change request.

### Wire formats (opaque strings to the server)

- **Message content** (`encryptionVersion = 2`): Base64 of JSON `{ "sid": sessionId, "idx": i, "ct": b64(AES-GCM box), "sig": b64(ECDSA(session key, over sid‖idx‖ct)) }`. Fits v1's 56 000-char limit for 10 000-char plaintext.
- **Session export** (what gets shared): `{ sessionId, chatId, senderId, ratchet: R_i, idx: i, sessionSigPub }` + `IK` signature over the whole export. A receiver importing at index `i` can decrypt messages `≥ i` only.
- **Key share** (ECIES): sender generates ephemeral ECDH pair `e`; `k = HKDF-SHA256(ECDH(e, SPK_recipient), info = "...:share:" + recipientUserId)`; blob = `{ ePub, spkId, ct: AES-GCM(k, sessionExport) }`. Authenticity comes from the `IK` signature *inside* the export, checked against the sender's published identity key.
- **Safety number**: SHA-256 over sorted `(userId‖IK_pub)` of both parties → rendered as 12 groups of 5 digits, Signal-style. Comparison screen; `verified` flag stored locally; any change of a peer's `IK` flips the chat into a "key changed" warning state.

### Decrypt-miss healing (the Matrix lesson)

A message can arrive before its session share (offline devices, socket single-connection bug). Never block sync: store the raw envelope in a new `MessageDto.pendingContent` column, show a "waiting for key / could not decrypt" placeholder in `content`, and run a re-decrypt pass over all `pendingContent` rows after every share pickup and after backup restore. This path is designed in from day one — it is the single biggest UX failure mode in real Matrix clients.

---

## Phase 0 — Server: fields, key endpoints, share store, backup store (deployable alone, inert)

Repo: `SchneaggchatV3server`. Follow the wake-feature package style (controller with nested DTOs → service → repository, `requireAuth()` first line, `/keys/**` is authenticated by default via `SecurityConfig`).

1. `Message` + `MessageRequest` + `MessageResponse` + `EditMessageRequest`: `encryptionVersion: Int = 0`. `ValidationUtils.validateStringMessage` gains an encrypted variant (56 000 limit).
2. New `keys/` package, collections:
   - `userkeys` (unique `userId`): `identityPub`, `spkPub`, `spkSig`, `spkId`, `updatedAt`, `backupInvalidatedAt`.
   - `keyshares` (indexed `recipientId`; unique `(recipientId, sessionId, senderId)`): `senderId`, `sessionId`, `blob`, `createdAt`. TTL index ~90 days.
   - `keybackups` (unique `userId`): `wrappedBackupKey`, `kdfSalt`, `kdfIterations`.
   - `backupentries` (unique `(userId, entryId)`): `blob`, `createdAt`. `entryId` = sessionId (or `identity`) so re-uploads upsert.
3. Endpoints (all authenticated, own-data-gated; membership-gated where a `groupId` appears):
   - `POST /keys/publish` — upsert identity + signed prekey; clears `backupInvalidatedAt`; bumps `user.updatedAt` so keys propagate through existing `/users/sync` (precedent: `UserService.setWakeGlobal`).
   - `POST /keys/shares` — batch upload `[{recipientId, sessionId, blob}]`; recipients must be friends or co-members.
   - `GET /keys/shares` — own pending shares; `POST /keys/shares/ack` — delete picked-up shares (their deletion narrows the FS window).
   - `POST /keys/backup` (wrapped `BK` + KDF params), `GET /keys/backup`, `POST /keys/backup/entries` (batch upsert), `GET /keys/backup/entries` (paged).
4. `UserResponse` (all variants): `identityPub: String?`, `spkPub: String?`, `spkSig: String?`, `spkId: Int?` — touch `UserService.serializeSyncUser` and the four `NotificationService` construction sites.
5. New outbound `SocketConnectionMessage.KeyShareWaiting` (hint only, no payload; add to the Jackson sealed types **and** to `FirebaseService.notificationResponseToDataMap`'s `when` if it should also ride push).
6. Push: for `encryptionVersion == 2`, put the E2E envelope directly into `encodedContent` (skip the global-key wrap) and add `encryptionVersion` to the payload map.
7. `RecapService`: filter `encryptionVersion != 0` out of `buildMessagingRecap` content stats (`:155-162`) and `buildReactionsRecap` (`:236`) — both currently ship raw content to clients.
8. `EmailService.resetPassword` → set `backupInvalidatedAt = now`; publishing after invalidation also deletes the user's `keyshares` and `backupentries` (stale, unusable).

## Phase 1 — Client: crypto core + key lifecycle (silent release)

Repo: `SchneaggchatV3`, `composeApp/src/commonMain`, new package `utilities/crypto/`.

1. Protocol core, pure Kotlin, no Android/iOS code, built only on cryptography-kotlin primitives (unit-testable on JVM):
   - `MessageSession` (outbound/inbound hash-ratchet state, encrypt/decrypt, export/import at index)
   - `KeyShareCodec` (ECIES wrap/unwrap against SPK, signature verify against IK)
   - `IdentityStore` (IK/SPK generation, SPK rotation & 30-day pruning)
   - `KeyBackup` (BK wrap/unwrap, entry encrypt/decrypt)
   - `SafetyNumber`
2. `E2eEncryptionManager` (Koin singleton, added to `AppRepository`'s constructor + `di/Modules.kt`): the only class the data layer talks to. API: `ensureKeysOnLogin(password)`, `encryptFor(chatId): Envelope`, `decrypt(envelope, senderId): String?`, `onMembershipChanged(chatId)`, `pickUpShares()`, `healOutboundShares(chatId, memberIds)`, `redecryptPending()`.
3. Persistence: Room table `E2eSessionDto` (sessionId, chatId, senderId, ratchet, idx, sessionSigPub/priv-if-own, createdAt, msgCount); `UserDto` + `identityPub/spkPub/spkSig/spkId/verified/keyChangedAt`; `MessageDto` + `encrypted: Boolean` + `pendingContent: String?`. **One** DB bump 79 → 80. IK/SPK privates + BK in KSafe: new `SecureKey` entries `E2E_IDENTITY_KEY`, `E2E_SPK_KEYS`, `E2E_BACKUP_KEY` (auto-wiped by `clearAll()`; note `deleteAllAppData()` from `MiscSettingsViewModel` does *not* clear KSafe — sessions die with the Room wipe, keys survive, backup restore covers the gap).
4. `NetworkUtils`: the Phase-0 endpoints; `encryptionVersion` on message DTOs (sent as 0 for now); new `UserResponse` fields mapped into `UserDto`.
5. Login (`AppRepository.login:1287`, password in scope) and signup (`SignUpViewModel` chains login): after token save, before `dataSync` → `ensureKeysOnLogin(password)`: try backup restore (fetch + PBKDF2 + unwrap + import entries), else generate IK/SPK/BK, publish keys, upload backup. Autologin path (`loadSavedLoginConfig`): keys already in KSafe, nothing to do. Password change (`changePassword:1615`): re-wrap BK, send with the request (new optional fields on the server DTO).
6. Smoke tests: ratchet vectors, ECIES round-trip, backup round-trip on Android/iOS/desktop (P-256 + GCM via provider-optimal on all three).

Release. Users silently accumulate identities, prekeys, backups — compatibility gate exactly like v1.

## Phase 2 — 1:1 text over sender sessions (2a decrypt-only flag-gated, 2b sending on)

Same two-release trick as v1 (protects a user's own stale devices).

The four boundary sites:

1. **Send** (`AppRepository.sendMessage`, TEXT branch `:1819`): peer has published keys → get/create outbound session for the chat (on create: wrap export for peer **and for self** — own other devices need it — upload shares, append to backup), encrypt, send `encryptionVersion = 2`. Room row (written at `:1747`) keeps plaintext.
2. **Send-echo** (`:1870-1911`): keep the local plaintext; never write the server's ciphertext echo back.
3. **Sync** (`toDomainMessage`, becomes suspend): `encryptionVersion == 2` → look up inbound session, verify signature, decrypt, store plaintext; miss → placeholder + `pendingContent`.
4. **Socket** (`SocketConnectionMessage.kt:172`): same funnel; decryption happens before `showNotification` at `:206`.

Additional: sticky encryption on edit (`editMessage:2165` — encrypted message gets encrypted edits, and the edit write-back at `:2198` must not clobber plaintext); offline queue unchanged (plaintext in Room, encrypt at real send time — `sendOfflineMessages:1961` re-enters `sendMessage`); share pickup triggered on app resume, socket `KeyShareWaiting`, chat open, and before declaring a decrypt-miss; `redecryptPending()` after every pickup. Push: Android `NotificationContentResolver` decrypts the envelope in-process via `E2eEncryptionManager` (generic fallback on any failure); iOS NSE shows generic "new message" (unchanged from v1 scope). Lock icon on encrypted chats optional.

## Phase 3 — Groups (same machinery)

1. Group send gate: all members have published keys → encrypt with own outbound session for that group; otherwise plaintext until healed. Cached, invalidated on membership change.
2. **Member added**: existing behavior gives new members full history backlog → each sender shares their *current* sessions with the new member (import at current index; the encrypted backlog before that index stays placeholder for them — accepted v2 trade-off, strictly better than v1's "new member reads everything ever") **and** rotates. Simpler alternative if history-for-new-members must hold: share at index 0 by keeping the initial ratchet value in the sender's backup — decide at implementation time, both fit the format.
3. **Member removed**: every remaining member rotates their outbound session on next send. Removed member cannot decrypt anything after rotation — the v1 headline gap, closed.
4. Self-healing: on group open / `dataSync`, compare own outbound session's shared-recipient set (tracked locally) against current membership; wrap + upload for anyone missing. Covers server-side joins and lost shares.
5. Sender authenticity: signature check on every group message; signature failure ⇒ placeholder, never silent acceptance.

## Phase 4 — Verification & reset UX

1. Safety-number screen per 1:1 chat (both fingerprints, digit blocks, "mark as verified"). Verified flag on `UserDto`.
2. Key-change detection: peer's `identityPub` changed → banner in chat ("Encryption keys changed — verify again"), verified flag reset. This is what makes server key-substitution *detectable*.
3. Backup unrecoverable on login (wrong-password decrypt failure or `backupInvalidatedAt`): dialog "Encrypted history can't be recovered. Generate new keys?" → new IK/SPK/BK, publish (server wipes stale shares/backup entries), peers get key-change warnings, groups re-share current sessions to the new SPK via Phase-3 healing. Old encrypted messages show placeholders.

---

## Example: Alice → Bob "Hallo"

1. First encrypted message of the chat: Alice creates session `S` (random ratchet `R_0`, session signing pair), signs the export with her `IK`, wraps it via ECIES for Bob's `SPK` and for her own `SPK`, `POST /keys/shares`, appends `S` to her backup.
2. She encrypts: key = `HKDF(R_0)`, AES-GCM("Hallo"), signs, sends `{"sid","idx":0,"ct","sig"}` as `content`, `encryptionVersion = 2`, advances to `R_1` and discards `R_0`. Local Room keeps "Hallo".
3. Server stores/relays the envelope; push carries it unchanged; the recap and the global push key never see plaintext.
4. Bob (possibly hinted by `KeyShareWaiting`) pulls his shares, unwraps with his SPK private, verifies Alice's `IK` signature (TOFU on first contact), imports `S` at index 0, acks the share (server deletes it), appends `S` to *his* backup, decrypts, stores plaintext, notification shows "Hallo".
5. Bob's new laptop, three weeks later: login with password → backup restore returns `S` and every other session → full history re-sync decrypts completely.
6. If the server had swapped Bob's keys to MITM the chat, Alice's client would show the key-change banner, and a safety-number comparison would fail.

## What stays visible to the server (honesty section)

All metadata: who talks to whom, timestamps, message types, group membership, read receipts, reaction content, polls, media bytes and image captions, ciphertext length, push tokens. **Forward secrecy is deliberately bounded**: session keys live in the password-encrypted backup, so an adversary with the server's ciphertext archive *and* the user's password reads everything — identical trade-off to Matrix key backup / Signal PIN-less backups; it is the price of "history survives a new device". Within that bound: server-only compromise reads nothing; identity-key-only compromise reads nothing by itself (shares are ECIES to SPKs and deleted after pickup; SPK privates expire after 30 days); a removed member reads nothing post-rotation. No deniability (messages are ECDSA-signed — stronger authenticity, weaker deniability than Signal; acceptable here). Account-level keys: any logged-in device equals the account, key-change warnings fire per account, not per device.

## Future work (out of scope for v2)

- Per-device keys + device list + cross-signing (true Matrix parity; requires a server device registry — none exists today)
- One-time prekeys / full X3DH and a Double Ratchet transport channel (needs per-device state)
- Media, caption, poll, reaction encryption
- Sealed sender / metadata reduction
- iOS notification extension decryption (App Group key mirror)
- QR-code safety-number scanning
- Fix the server's first-connection-only socket delivery (`SocketConnectionHandler.kt:57`) — v2 works around it via pull, but multi-device realtime wants `filter`, not `find`

## Rollout order

Phase 0 (server, inert) → Phase 1 (client keys + backup, silent) → Phase 2a (decrypt-ready) → Phase 2b (sending on) → Phase 3 (groups + rotation) → Phase 4 (verification UX). Each independently shippable; no data migration; mixed history renders per message via `encryptionVersion`.

## Effort estimate (relative to v1)

v1 ≈ baseline. v2 ≈ **3–4× v1**: the ratchet/session core and backup are new machinery (~half the total), share distribution + healing + pending-redecrypt is the fiddly part, verification UI is small. The protocol core (Phase 1 item 1) is security-critical hand-rolled code — there is no vetted KMP library (libsignal and vodozemac are Rust/JNI/Swift, no Kotlin Multiplatform bindings); mitigate with the pure-Kotlin isolated core, published test vectors, and exhaustive unit tests before any UI work. Full Signal/Matrix parity (per-device everything) would roughly double v2 again and is not worth it before the server has a device concept.
