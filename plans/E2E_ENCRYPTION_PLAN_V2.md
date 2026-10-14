# End-to-End Encryption Plan V2 — "Matrix-level"

Supersedes `E2E_ENCRYPTION_PLAN.md` (v1). Same repos, same boundary philosophy, much stronger cryptography. Design is modeled on Matrix (Megolm sender sessions + encrypted key backup + signatures + verification), not on pure Signal — because Signal's model (per-device keys, Double Ratchet only, no server history) is fundamentally incompatible with Schneaggchat's "new device re-syncs full history from the server" architecture. Matrix was built for exactly that architecture.

**Prerequisite**: `CHAT_BASED_MIGRATION_PLAN.md` ships first. This plan assumes chat-based addressing everywhere: every message has a `chatId`, every chat (`DIRECT` or `GROUP`) has an explicit member list and a `membershipVersion`, and co-members are reachable via `/users/sync` regardless of friendship. **There is no DM-specific code path in the crypto layer** — a DM is a chat with two members.

**Goal**: the server cannot read message text, cannot silently substitute keys without detection, removed members cannot read future messages, and a leaked identity key alone does not decrypt past traffic picked up after rotation windows. History still survives a new-device login via password-protected key backup.

## What v2 adds over v1

| Property | v1 | v2 |
|---|---|---|
| Server can't read text | ✅ | ✅ |
| Key rotation on member removal | ❌ | ✅ (per-sender session rotation, driven by `membershipVersion`) |
| Sender authenticity in groups (member can't forge another member) | ❌ (shared AES key) | ✅ (per-session signatures) |
| Key-substitution detection | ❌ (silent MITM possible) | ✅ (signed prekeys + safety numbers + key-change warnings) |
| Forward secrecy | ❌ | Partial: in-session hash ratchet + session rotation + prekey expiry. Deliberately bounded by key backup (see honesty section) |
| Post-compromise security | ❌ | Partial (session rotation re-keys via fresh ephemeral ECDH) |
| DMs and groups share one mechanism | ❌ (two schemes) | ✅ (one chat-generic mechanism, no DM branch) |
| History on new device | ✅ | ✅ (encrypted key backup, Matrix-SSSS-style) |
| Per-device keys, deniability, sealed sender | ❌ | ❌ (out of scope, see future work) |

## Current state (re-verified 2026-10-10)

- **Nothing of v1 or v2 is implemented.** No `keys` package, no `encryptionVersion`, no `E2eEncryptionManager`, no `publicKey` field in either repo. `encryptionVersion = 1` stays reserved/unused; this plan uses `2`.
- **Push is plaintext.** The old global `jwtSecret.take(20)` wrap was removed 2026-09-05 (server `SECURITY_AUDIT.md` M-2); `MessageNotificationResponse.content` carries the plain text through FCM/APNs today. After the migration it is truncated to a preview (4 KB payload limit).
- Client crypto: `dev.whyoleg.cryptography` 0.6.0 `provider-optimal` in commonMain (`gradle/libs.versions.toml:8,94-95`, `composeApp/build.gradle.kts:193-194`), not yet imported anywhere — provides ECDH P-256, ECDSA P-256, HKDF-SHA256, HMAC-SHA256, AES-256-GCM, PBKDF2, SecureRandom. **No X25519/Ed25519 in 0.6.0** → all DH/signatures run on P-256 (protocol-equivalent; Matrix/Signal use Curve25519 but nothing in the constructions requires it).
- Secure storage: `Preferencemanager.SecureKey` enum (`Preferencemanager.kt:34-42`) + KSafe 3.3.0; `clearSessionCredentials()` (:90-95) wipes every enum entry on logout, so new entries are auto-wiped.
- Room DB: v87 today, **v88 after the chat migration**, destructive migration, no Migration objects — schema changes are one version bump + full re-sync. This plan bumps to **v89**.
- Realtime fan-out goes to **all** connections of a user (`SocketConnectionHandler.kt:117` uses `filter`). Socket events are still treated as hints only; pull endpoints are the source of truth for key distribution.
- The four data-layer boundary sites. `AppRepository` is gone (`REPOSITORY_INTERFACE_MIGRATION_PLAN.md`): send, send-echo, sync and edit now live in the `MessageRepository` impl, `chat/data/OfflineFirstMessageRepository.kt`. Line numbers as of that plan's Phase 8; they will shift with the chat migration, re-locate by function name:
  - send: `sendMessage` (:349-589), local insert :456;
  - send-echo: server-response upsert (:533-584, takes `content` from the response at :547);
  - sync: `syncMessages` (was `messageIdSync`, :703-802), mapper call :759;
  - socket: `SocketConnectionMessage.kt` `MessageChange` handler (:190-266), mapper call :198, notification :217-246.
  Sync + socket funnel through `MessageResponseMapper.toDomainMessage` (`datasource/network/requestResponseDataClasses/MessageResponseMapper.kt:9-45`, non-suspend, takes server `content` at :19) — must become `suspend`. Edit (`editMessage` :205-248) also writes back server content (:238).
- Content currently logged, removed by the migration release: `NetworkUtils.kt:1022,1121` (`println`), `Message.toString`, desktop `Notifier.jvm.kt:53`.

## Decisions

| Question | Decision |
|---|---|
| What is encrypted | Text messages (v2). Media/captions, polls, reactions plaintext — future work. |
| Protocol model | Matrix-style: per-sender **message sessions** (hash ratchet) per chat, ECIES against **signed prekeys** for session distribution, **encrypted key backup** for history. |
| Chat model | One mechanism for every chat. Recipients = `members(chatId)`; rotation trigger = `chat.membershipVersion`; key-share authz = chat membership. DIRECT vs GROUP never branches in the crypto layer. |
| Key granularity | Per **account**, not per device (no device registry server-side; all devices restore the same key bundle from backup). Documented deviation from Matrix/Signal — see future work. |
| Trust model | Signed prekeys chained to an identity signing key; trust-on-first-use + safety-number verification UI + key-change warnings (Signal-style). Applies equally to friends and non-friend co-members (event groups). |
| History for new members | Follows `chat.historyVisibility` (migration plan §8.4, decision D2: new groups `SINCE_JOIN`, optional toggle to `FULL`): `FULL` = share sessions at index 0, `SINCE_JOIN` = share at current index. DIRECT chats never gain members. |
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
   - Ratchet: `R_{i+1} = HMAC-SHA256(R_i, "advance")`; per-message key: `HKDF-SHA256(R_i, info = "...:msgkey:" + sessionId + ":" + i)` → AES-256-GCM key. Old `R` values are discarded after advancing ⇒ within a session, a stolen current state cannot decrypt earlier messages. (For `historyVisibility = FULL` chats the sender additionally keeps `R_0` in its own key backup so it can share at index 0 to later joiners.)
   - Each session carries its own ECDSA P-256 **session signing pair**; its public half is bound into the session export, which is itself signed by the sender's `IK` ⇒ receivers can prove which member sent a message (no in-chat forgery).
   - Each session records the `membershipVersion` it was created for and the set of recipients it was shared with.
   - **Rotation**: whenever `chat.membershipVersion` differs from the session's (join, leave, remove, account deletion; for DIRECT also unfriend/block/re-accept), otherwise after 500 messages or 7 days. Rotation = new random session, distributed fresh.
4. **Backup key `BK`** — random 32 bytes. Wrapped with AES-256-GCM under `PBKDF2-SHA256(password, random 16-byte salt, 600 000 iterations)`. Every session key the client learns (own outbound at creation, inbound at import) plus `IK`/current `SPK` privates are encrypted under `BK` and appended to the server-side backup. Password change client-side = re-wrap `BK` only (one small blob), atomic with the change request.

### Wire formats (opaque strings to the server)

- **Message content** (`encryptionVersion = 2`): Base64 of JSON `{ "sid": sessionId, "idx": i, "ct": b64(AES-GCM box), "sig": b64(ECDSA(session key, over sid‖idx‖ct)) }`. Fits a 56 000-char limit for 10 000-char plaintext. AES-GCM associated data = `chatId` (a ciphertext cannot be replayed into another chat).
- **Session export** (what gets shared): `{ sessionId, chatId, senderId, membershipVersion, ratchet: R_i, idx: i, sessionSigPub }` + `IK` signature over the whole export. A receiver importing at index `i` can decrypt messages `≥ i` only.
- **Key share** (ECIES): sender generates ephemeral ECDH pair `e`; `k = HKDF-SHA256(ECDH(e, SPK_recipient), info = "...:share:" + chatId + ":" + recipientUserId)`; blob = `{ ePub, spkId, ct: AES-GCM(k, sessionExport) }`. Authenticity comes from the `IK` signature *inside* the export, checked against the sender's published identity key.
- **Safety number**: SHA-256 over sorted `(userId‖IK_pub)` of both parties → rendered as 12 groups of 5 digits, Signal-style. Comparison screen; `verified` flag stored locally; any change of a peer's `IK` flips every chat containing that peer into a "key changed" warning state.

### Decrypt-miss healing (the Matrix lesson)

A message can arrive before its session share (offline devices, lost socket frames). Never block sync: store the raw envelope in `MessageDto.pendingContent`, show a "waiting for key / could not decrypt" placeholder in `content`, and run a re-decrypt pass over all `pendingContent` rows after every share pickup and after backup restore. Designed in from day one — it is the single biggest UX failure mode in real Matrix clients.

---

## Phase 0 — Server: fields, key endpoints, share store, backup store (deployable alone, inert)

Repo: `SchneaggchatV3server`. Follow the project package style (controller with nested DTOs → service → LookupService, `requireAuth()` first line, `/keys/**` authenticated by default via `SecurityConfig`). Authorization everywhere through `ChatLookupService` from the migration.

1. `Message` + message requests + `MessageResponse` + `EditMessageRequest`: `encryptionVersion: Int = 0`, plus `membershipVersion: Long?` on send requests. `ValidationUtils.validateStringMessage` gains an encrypted variant (56 000 limit).
2. **Stale-membership guard**: an encrypted send whose `membershipVersion` ≠ `chat.membershipVersion` → `409 Conflict`. Client syncs the chat, rotates, re-sends. Closes the race "encrypted for a member removed a second ago" for every chat type.
3. New `keys/` package, collections:
   - `userkeys` (unique `userId`): `identityPub`, `spkPub`, `spkSig`, `spkId`, `updatedAt`, `backupInvalidatedAt`.
   - `keyshares` (indexed `recipientId`; unique `(recipientId, sessionId, senderId)`): `chatId`, `senderId`, `sessionId`, `blob`, `createdAt`. TTL index ~90 days.
   - `keybackups` (unique `userId`): `wrappedBackupKey`, `kdfSalt`, `kdfIterations`.
   - `backupentries` (unique `(userId, entryId)`): `blob`, `createdAt`. `entryId` = sessionId (or `identity`) so re-uploads upsert.
4. Endpoints (all authenticated, own-data-gated):
   - `POST /keys/publish` — upsert identity + signed prekey; clears `backupInvalidatedAt`; bumps `user.updatedAt` so keys propagate through `/users/sync` (precedent: `UserService.setWakeGlobal`).
   - `POST /keys/shares` — batch upload `[{chatId, recipientId, sessionId, blob}]`; **single rule**: `isMember(sender, chatId) && isMember(recipient, chatId)`. No friendship check.
   - `GET /keys/shares` — own pending shares; `POST /keys/shares/ack` — delete picked-up shares (deletion narrows the FS window).
   - `POST /keys/backup` (wrapped `BK` + KDF params), `GET /keys/backup`, `POST /keys/backup/entries` (batch upsert), `GET /keys/backup/entries` (paged).
5. `UserResponse` (all variants, **including `SimpleUserResponse` for non-friend co-members**): `identityPub: String?`, `spkPub: String?`, `spkSig: String?`, `spkId: Int?` — touch `UserService.serializeSyncUser` and the `NotificationService` construction sites. Since the migration, `/users/sync` covers friendships ∪ users sharing any chat, so every co-member's keys are reachable.
6. New outbound `SocketConnectionMessage.KeyShareWaiting` (hint only, no payload; add to the Jackson sealed types **and** to `FirebaseService.notificationResponseToDataMap`'s `when` if it should also ride push).
7. Push: for `encryptionVersion == 2`, put the E2E envelope into `content` **only if the payload stays under 4 KB**, otherwise send `content = ""` + a `needsFetch` flag (client fetches via sync). Never send a plaintext preview for encrypted messages. Add `encryptionVersion` to the payload map.
8. `RecapService`: exclude `encryptionVersion != 0` from content stats (longest message, word/char counts, most-reacted content) — `RecapService.kt:219-232, 288-301` (pre-migration lines).
9. `EmailService.resetPassword` → set `backupInvalidatedAt = now`; publishing after invalidation also deletes the user's `keyshares` and `backupentries` (stale, unusable).

## Phase 1 — Client: crypto core + key lifecycle (silent release)

Repo: `SchneaggchatV3`, `composeApp/src/commonMain`, new package `utilities/crypto/`.

1. Protocol core, pure Kotlin, no Android/iOS code, built only on cryptography-kotlin primitives (unit-testable on JVM):
   - `MessageSession` (outbound/inbound hash-ratchet state, encrypt/decrypt, export/import at index)
   - `KeyShareCodec` (ECIES wrap/unwrap against SPK, signature verify against IK)
   - `IdentityStore` (IK/SPK generation, SPK rotation & 30-day pruning)
   - `KeyBackup` (BK wrap/unwrap, entry encrypt/decrypt)
   - `SafetyNumber`
2. `E2eEncryptionManager` (interface + one Koin singleton impl in `di/Modules.kt`, injected into the `OfflineFirstMessageRepository` and `RemoteAuthRepository` constructors; behind the interface so ViewModel and repository tests use a `FakeE2eEncryptionManager`): the only class the data layer talks to. Chat-generic API: `ensureKeysOnLogin(password)`, `encryptFor(chatId): Envelope`, `decrypt(envelope, chatId, senderId): String?`, `onChatChanged(chat)` (membershipVersion / member diff → rotate + heal), `pickUpShares()`, `healOutboundShares(chatId)`, `redecryptPending()`.
3. Persistence: Room table `E2eSessionDto` (sessionId, chatId, senderId, membershipVersion, ratchet, idx, initialRatchet?-if-own-and-FULL, sessionSigPub/priv-if-own, sharedWith: List<userId>, createdAt, msgCount); `UserDto` + `identityPub/spkPub/spkSig/spkId/verified/keyChangedAt`; `MessageDto` + `encrypted: Boolean` + `pendingContent: String?`. **One** DB bump 88 → 89. IK/SPK privates + BK in KSafe: new `SecureKey` entries `E2E_IDENTITY_KEY`, `E2E_SPK_KEYS`, `E2E_BACKUP_KEY` (auto-wiped by `clearSessionCredentials()`; note `SessionTerminator.deleteAllAppData()` (`app/data/LocalSessionTerminator.kt:37-44`) does *not* clear KSafe — sessions die with the Room wipe, keys survive, backup restore covers the gap).
4. `NetworkUtils`: the Phase-0 endpoints; `encryptionVersion` + `membershipVersion` on message DTOs (sent as 0 / null for now); new `UserResponse` fields mapped into `UserDto`.
5. Login (`RemoteAuthRepository.login`, `login/data/RemoteAuthRepository.kt:174-219`, password in scope) and signup (`SignUpViewModel.kt:219-235` chains login): after token save, before `dataSync` → `ensureKeysOnLogin(password)`: try backup restore (fetch + PBKDF2 + unwrap + import entries), else generate IK/SPK/BK, publish keys, upload backup. Autologin (`RemoteAuthRepository.loadSavedLoginConfig` :130-171, no password): keys already in KSafe, nothing to do. Password change (`RemoteAuthRepository.changePassword` :252-262, `PrivacyAndSecurityViewModel.kt:75-89`): re-wrap BK, send with the request (new optional fields on the server DTO).
6. Smoke tests: ratchet vectors, ECIES round-trip, backup round-trip on Android/iOS/desktop (P-256 + GCM via provider-optimal on all three).

Release. Users silently accumulate identities, prekeys, backups — compatibility gate exactly like v1 (no server-enforced min version exists, migration plan D5): wait until active clients have adopted Phase 1 before Phase 2b.

## Phase 2 — Encrypted text in every chat (2a decrypt-only flag-gated, 2b sending on)

One chat-generic phase for DIRECT and GROUP. Two releases only to protect a user's own stale devices: 2a can decrypt, 2b starts sending (ship 2b only once 2a adoption is high — no enforced min version, so stragglers see placeholders until they update).

**Send gate** (one rule): `canSend(chatId)` AND every member has published keys → encrypt; otherwise plaintext until healed. Cached per chat, invalidated by `onChatChanged`.

The four boundary sites:

1. **Send** (`sendMessage`, TEXT branch): get/create outbound session for the chat (on create: wrap export for **every member incl. self** — own other devices need it — upload shares, append to backup), encrypt, send `encryptionVersion = 2` + `membershipVersion`. `409` → `chatIdSync`, rotate, retry once. Room row keeps plaintext.
2. **Send-echo**: keep the local plaintext; never write the server's ciphertext echo back.
3. **Sync** (`toDomainMessage`, now suspend): `encryptionVersion == 2` → look up inbound session `(chatId, senderId, sid)`, verify signature, decrypt, store plaintext; miss → placeholder + `pendingContent`.
4. **Socket**: same funnel; decryption happens before the notification is shown.

**Membership changes** (handled identically for every chat type via `onChatChanged`):
- **Member added** (GROUP only in practice): rotate; share the new session with all members. Additionally share existing sessions to the newcomer per `historyVisibility`: `FULL` → export at index 0 from the stored `initialRatchet`; `SINCE_JOIN` → nothing older (server also withholds pre-join messages, migration plan §3.2/§3.3). New groups default to `SINCE_JOIN`; the group-settings toggle to `FULL` only covers sessions created while it is on.
- **Member removed / left** keeps already-decrypted history locally (read-only chat, migration plan D8) — expected: E2E protects future messages, not what a member already saw.
- **Member removed / left / account deleted**: every remaining member rotates on next send. Removed member cannot decrypt anything after rotation.
- **DIRECT unfriend / block / re-accept**: `membershipVersion` bump → rotation on next send (hygiene; `canSend` is what actually blocks sending).

**Self-healing**: on chat open / `dataSync`, compare the own outbound session's `sharedWith` set against current members; wrap + upload for anyone missing (covers server-side joins, lost shares, new co-member keys).

**Sender authenticity**: signature check on every message; failure ⇒ placeholder, never silent acceptance.

Additional:
- Sticky encryption on edit (`editMessage`): encrypted messages get encrypted edits; the edit write-back must not clobber local plaintext. Only the sender may edit (fixed server-side in the migration).
- Offline queue unchanged (plaintext in Room, encrypt at real send time — `sendOfflineMessages` re-enters `sendMessage`).
- Share pickup on app resume, socket `KeyShareWaiting`, chat open, and before declaring a decrypt-miss; `redecryptPending()` after every pickup.
- Push: Android `NotificationContentResolver` decrypts the envelope in-process via `E2eEncryptionManager` (generic "new message" on failure or `needsFetch`); iOS NSE shows generic text (unchanged from v1 scope); desktop is socket-only.
- Lock icon on encrypted chats optional.

## Phase 3 — Verification & reset UX

1. Safety-number screen per peer: reachable from DIRECT chat details and from every member row in GROUP chat details (event groups contain non-friends — TOFU applies to them too). Verified flag on `UserDto`.
2. Key-change detection: peer's `identityPub` changed → banner in every chat containing that peer ("Encryption keys changed — verify again"), verified flag reset. This is what makes server key-substitution *detectable*.
3. Backup unrecoverable on login (wrong-password decrypt failure or `backupInvalidatedAt`): dialog "Encrypted history can't be recovered. Generate new keys?" → new IK/SPK/BK, publish (server wipes stale shares/backup entries), peers get key-change warnings, chats re-share current sessions to the new SPK via self-healing. Old encrypted messages show placeholders.

---

## Example: Alice → Bob "Hallo" (DIRECT chat `c1`; a group works identically with N recipients)

1. First encrypted message in `c1` (`membershipVersion = 3`, members Alice + Bob): Alice creates session `S` (random ratchet `R_0`, session signing pair, `membershipVersion = 3`), signs the export with her `IK`, wraps it via ECIES for every member of `c1` — Bob's `SPK` and her own — `POST /keys/shares` with `chatId = c1`, appends `S` to her backup.
2. She encrypts: key = `HKDF(R_0)`, AES-GCM("Hallo", AD = `c1`), signs, sends `{"sid","idx":0,"ct","sig"}` as `content`, `encryptionVersion = 2`, `membershipVersion = 3`, advances to `R_1` and discards `R_0`. Local Room keeps "Hallo".
3. Server checks `membershipVersion` matches, stores/relays the envelope; push carries it (≤ 4 KB) unchanged; the recap never sees plaintext.
4. Bob (possibly hinted by `KeyShareWaiting`) pulls his shares, unwraps with his SPK private, verifies Alice's `IK` signature (TOFU on first contact), imports `S` at index 0, acks the share (server deletes it), appends `S` to *his* backup, decrypts, stores plaintext, notification shows "Hallo".
5. Bob's new laptop, three weeks later: login with password → backup restore returns `S` and every other session → full history re-sync decrypts completely.
6. Alice unfriends Bob: `c1.membershipVersion` → 4, Bob can no longer send (`canSend`), Alice's next session after a re-friend is fresh.
7. If the server had swapped Bob's keys to MITM the chat, Alice's client would show the key-change banner, and a safety-number comparison would fail.

## What stays visible to the server (honesty section)

All metadata: who talks to whom, timestamps, message types, chat membership, read receipts, reaction content, polls, media bytes and image captions, ciphertext length, push tokens. **Forward secrecy is deliberately bounded**: session keys live in the password-encrypted backup, so an adversary with the server's ciphertext archive *and* the user's password reads everything — identical trade-off to Matrix key backup / Signal PIN-less backups; it is the price of "history survives a new device". `historyVisibility = FULL` widens this further (initial ratchet values kept in backup). Within that bound: server-only compromise reads nothing; identity-key-only compromise reads nothing by itself (shares are ECIES to SPKs and deleted after pickup; SPK privates expire after 30 days); a removed member reads nothing post-rotation. No deniability (messages are ECDSA-signed). Account-level keys: any logged-in device equals the account, key-change warnings fire per account, not per device. The server controls chat membership — it could add a member and thereby receive future sessions; this is visible to users as a SYSTEM "member added" message and a member-list change (same limitation as Matrix without cross-signed membership).

## Future work (out of scope for v2)

- Per-device keys + device list + cross-signing (true Matrix parity; requires a server device registry — none exists today)
- One-time prekeys / full X3DH and a Double Ratchet transport channel (needs per-device state)
- Media, caption, poll, reaction encryption (requires moving image processing from the server — `ImageManager` re-encodes — to the client)
- Sealed sender / metadata reduction
- iOS notification extension decryption (App Group key mirror)
- QR-code safety-number scanning
- Signed membership changes (admin-signed add/remove) so the server cannot silently add a member

## Rollout order

Chat-based migration (separate plan) → Phase 0 (server, inert) → Phase 1 (client keys + backup, silent) → Phase 2a (decrypt-ready) → Phase 2b (sending on, after 2a adoption) → Phase 3 (verification UX). Each independently shippable; no data migration; mixed history renders per message via `encryptionVersion`.

## Effort estimate (relative to v1)

v1 ≈ baseline. v2 ≈ **3× v1** (down from 3–4×: the chat-based migration removes the separate DM/group distribution, rotation and authz paths). The ratchet/session core and backup are new machinery (~half the total), share distribution + healing + pending-redecrypt is the fiddly part, verification UI is small. The protocol core (Phase 1 item 1) is security-critical hand-rolled code — there is no vetted KMP library (libsignal and vodozemac are Rust/JNI/Swift, no Kotlin Multiplatform bindings); mitigate with the pure-Kotlin isolated core, published test vectors, and exhaustive unit tests before any UI work. Full Signal/Matrix parity (per-device everything) would roughly double v2 again and is not worth it before the server has a device concept.
