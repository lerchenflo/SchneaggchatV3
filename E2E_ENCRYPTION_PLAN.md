# End-to-End Encryption Plan

Basic end-to-end encryption for Schneaggchat message text. Not Signal-level: no forward secrecy, no ratchet, no per-device keys. Goal: the server can no longer read message text.

## Current state (analysis)

- The server stores all message content as plaintext in MongoDB (`messages` collection, `content` field).
- Push notification previews are "encrypted" with a single global key (`JWT_SECRET.take(20)`) that is handed to **every** client at login (`AuthService.TokenPair.encryptionKey`). Every user can decrypt every other user's push payloads. This is obfuscation, not E2E.
- The recap feature (`RecapService`) reads plaintext content (char/word stats, longest message, most-reacted message).
- Poll voting is 160 lines of server-side logic over plaintext poll state.
- The client already ships `dev.whyoleg.cryptography` 0.6.0 (AES-GCM in use via `CryptoUtil`); the library also provides ECDH P-256, HKDF, and PBKDF2 — no new dependencies needed.
- Secure key storage already exists: `Preferencemanager` + KSafe (Android Keystore / iOS Keychain, JVM fallback on desktop).
- Room uses destructive migration (version bump = wipe + full re-sync), so no migration code is needed for schema changes.
- A new device login re-syncs the full message history from the server (sync cursor starts at 0).

## Decisions

| Question | Decision |
|---|---|
| What is encrypted | Text messages only (v1). Media, polls, reactions stay plaintext — future work. |
| Key model | One ECDH P-256 key pair per user account. |
| History on new device | Survives: private key encrypted with a password-derived key, backed up on the server. Password reset via email = encrypted history unrecoverable. |
| Existing messages | Stay plaintext. Per-message flag `encryptionVersion` (0 = plaintext, 1 = encrypted). |

## Crypto design (v1)

- **Identity key**: ECDH P-256 key pair per user. Public key distributed to other clients through the existing `/users/sync` mechanism.
- **Pairwise key (1:1 chats)**: `HKDF-SHA256(ECDH(myPrivate, theirPublic), info = "schneaggchat-e2e-v1:<minUserId>:<maxUserId>")` → AES-256 key. User IDs sorted so both sides derive the identical key. Deterministic, cacheable.
- **Message cipher**: AES-256-GCM. Wire `content` = Base64 of the GCM box output (nonce embedded).
- **Group key**: one random 32-byte AES key per group, static (no rotation in v1). Wrapped (AES-GCM under the pairwise key) individually for each member and stored on the server as opaque blobs the server cannot decrypt.
- **Private key backup**: PBKDF2-SHA256(password, random 16-byte salt, 210 000 iterations) → AES-GCM over the DER-encoded private key. Blob + salt + iterations stored on the server.

**Core boundary decision**: the local Room database always stores **plaintext** plus an `encrypted` flag. Encryption/decryption happens in one new class, `E2eEncryptionManager`, at exactly four places in the data layer (send, send-echo, sync, socket receive). The entire UI layer, message search, reply previews, and local notifications stay untouched.

---

## Phase 0 — Server: fields and key endpoints (deployable alone, fully backward compatible)

Repo: `SchneaggchatV3server`.

1. `Message` model + `MessageRequest` / `MessageResponse` / edit DTO: new field `encryptionVersion: Int = 0`, persisted by `MessageService.sendMessage` / `editMessage`.
2. `ValidationUtils`: content limit 56 000 chars when `encryptionVersion > 0` (Base64 + GCM expansion of 10 000 chars), plaintext keeps 10 000.
3. New `keys/` feature package (controller → service → repository → model):
   - `UserKey` (`userkeys` collection): `userId` (unique), `publicKey`, `encryptedPrivateKey`, `kdfSalt`, `kdfIterations`, `backupInvalidatedAt`.
   - `GroupKey` (`groupkeys` collection, unique `(groupId, userId)`): `wrappedKey`, `wrappedByUserId`.
   - Endpoints (all authenticated):
     - `POST /keys/publish` — upsert own key material, clear `backupInvalidatedAt`, bump `user.updatedAt` so the public key propagates via the existing user sync.
     - `GET /keys/backup` — own encrypted private key backup.
     - `POST /keys/group/{groupId}` — upload wrapped group keys `[{userId, wrappedKey}]` (member-gated).
     - `GET /keys/group/{groupId}` — own wrap (member-gated).
     - `GET /keys/group/{groupId}/status` — which members lack a wrap or a public key (drives client self-healing).
4. `UserResponse` (all variants): new `publicKey: String?`.
5. `NotificationService` / `FirebaseService` / `ApnsService`: for `encryptionVersion > 0`, put the E2E ciphertext directly into `encodedContent` (skip the global-key wrap) and add `encryptionVersion` to the push payload.
6. `RecapService`: exclude encrypted messages from content-derived stats; never ship encrypted content back.
7. Email password reset (`EmailService.resetPassword`): set `backupInvalidatedAt = now` (backup is unrecoverable without the old password).
8. Security check on all new endpoints (only own key publishable, group endpoints membership-gated).

## Phase 1 — Client: key lifecycle (silent release, nothing encrypted yet)

Repo: `SchneaggchatV3`, `composeApp/src/commonMain`.

1. New `utilities/crypto/E2eEncryptionManager.kt` (Koin singleton): `ensureKeysOnLogin(password)` (restore backup or generate + publish), `encryptForUser` / `decryptFromUser`, `encryptForGroup` / `decryptForGroup`, `wrapGroupKeyFor` / `unwrapGroupKey`, in-memory pairwise-key cache.
2. `Preferencemanager`: new secure keys `E2E_PRIVATE_KEY`, `E2E_PUBLIC_KEY` (KSafe).
3. `NetworkUtils`: the new key endpoints; `publicKey` on `UserResponse`; `encryptionVersion` on message DTOs (still sent as 0).
4. Room: `UserDto.publicKey`, `MessageDto.encrypted`, `GroupDto.groupKey` — one database bump 79 → 80 (destructive, full re-sync; batch all fields into a single bump).
5. Hook `AppRepository.login` and signup (password is in memory there): `ensureKeysOnLogin(password)` after token save, before `dataSync`. Logout wipes the E2E keys.
6. Password change: client re-encrypts the private key with the new password and sends the new backup atomically with the password change request (new optional fields on the server's `PasswordChangeRequest`).
7. Smoke test ECDH/HKDF/PBKDF2 on all three targets (Android, iOS, desktop).

After this release users silently accumulate published keys — this is the compatibility gate for Phase 2.

## Phase 2 — 1:1 text encryption (the visible feature)

Two releases: **2a** ships decryption support with sending disabled (compile-time flag), **2b** enables sending once 2a is widely installed. This protects a user's own not-yet-updated devices (the key is account-level, so an old desktop client would otherwise show Base64 garbage).

The four boundary sites (all in the data layer):

1. **Send** (`AppRepository.sendMessage`, TEXT branch): if enabled, direct message, and the peer has a public key → encrypt, send `encryptionVersion = 1`. The Room row (written before the network call) keeps plaintext.
2. **Send echo**: after a successful send, write the local plaintext back to Room, not the server's ciphertext echo.
3. **Sync** (`messageIdSync`): decrypt before storing. Decryption peer = `if (myMessage) receiverId else senderId` (own messages sent from another device!). On failure store a "message could not be decrypted" placeholder — never block sync.
4. **Socket** (`MessageChange` handler): same decrypt-before-upsert.

Additional:

- **Edit**: encryption state is sticky per message (an encrypted message gets encrypted edits).
- **Offline queue**: unsent messages hold plaintext in Room; encryption is decided at actual send time — no change needed.
- **Push, Android** (`NotificationContentResolver`): decrypt the E2E ciphertext in-process (private key in KSafe, sender public key in Room); generic "new message" fallback on any failure.
- **Push, iOS** (Swift Notification Service Extension): show a generic "new message" for encrypted payloads. Real NSE decryption (mirroring keys into the App Group) is deferred future work.
- Optional: small lock icon on encrypted messages / chat header.

## Phase 3 — Group text encryption

1. **Group creation**: if all members have public keys, the creator generates the group key, wraps it for every member (including self), uploads via `POST /keys/group/{id}`. Otherwise the group stays plaintext until healed.
2. **Key acquisition**: a member without the group key fetches `GET /keys/group/{id}` and unwraps using the pairwise key with `wrappedByUserId`.
3. **Self-healing**: on opening a group chat, a member holding the key checks `/keys/group/{id}/status` and wraps for any member missing one. Covers member adds, server-side event joins, and key resets. Because the group key is static and new members get the full history backlog (existing behavior), healed members can decrypt the entire encrypted backlog.
4. **Send/receive**: same four boundary sites; encrypt only when no member is missing a wrap (cached, re-checked on membership changes).
5. Documented v1 limitation: no key rotation on member removal. The server's access control still blocks removed members at the API layer.

## Phase 4 — Key-reset UX

- On login, if the backup cannot be decrypted or was invalidated (email password reset): dialog — "Your encrypted message history can't be recovered. Generate new encryption keys?" → generate + republish. Old encrypted direct messages show the placeholder; groups heal via Phase 3 self-healing.
- Server: publishing after invalidation also deletes the user's stale `groupkeys` wraps so healing kicks in.

---

## Example: how it works

### Alice sends "Hallo" to Bob

1. Alice's app writes the plaintext to its local Room DB (`encrypted = true`, `sent = false`).
2. `E2eEncryptionManager` takes Bob's public key (synced earlier), computes `ECDH(alicePrivate, bobPublic)` → HKDF → pairwise AES key (cached), encrypts with AES-GCM: `"m8QxT2lY…Zg=="`.
3. Wire request:
   ```json
   { "receiverId": "…bob", "groupMessage": false, "msgType": "TEXT",
     "content": "m8QxT2lY…Zg==", "encryptionVersion": 1 }
   ```
4. The server stores exactly this ciphertext. It never sees "Hallo". WebSocket push and FCM/APNs fallback carry the ciphertext unchanged.
5. Bob's client derives the identical pairwise key (sorted-user-ID derivation), decrypts, stores plaintext locally, notification shows "Hallo". On iOS with the app killed, the notification extension shows a generic "New message"; the content appears on next app open via sync.
6. Alice's send-echo writes her own local plaintext back, not the server's ciphertext.

### Group

Alice creates "Skiclub" with Bob and Carol: she generates group key `GK`, uploads three wraps (one per member, each encrypted with the respective pairwise key). Bob fetches his wrap, unwraps with `pairwise(alice, bob)`, stores `GK`. Messages are AES-GCM under `GK`. Bob later adds Dave: Bob wraps `GK` under `pairwise(bob, dave)` and uploads; Dave syncs the full history and can decrypt all of it.

---

## What stays visible to the server (honesty section)

Sender/receiver IDs, timestamps, message types, group membership, read receipts, reaction content, polls (all fields), media bytes (images/audio; image captions too in v1), system messages, profile data, ciphertext length (≈ text length), and all who-talks-to-whom metadata. There is no forward secrecy, no signatures (the server could substitute public keys — trust-on-first-use), and no safety-number verification in v1. Deliberate scope: the server can no longer **read text content** — nothing more.

## Future work (out of scope for v1)

- Media and caption encryption
- Poll encryption (requires moving vote logic client-side)
- Group key rotation on member removal
- iOS notification extension decryption (App Group key mirror)
- Key fingerprint verification UI (defeat key substitution)
- Fix pre-existing multi-device socket bug (server delivers realtime updates to only the first connected session per user)

## Rollout order

Phase 0 (server, inert) → Phase 1 (client keys, silent) → Phase 2a (decrypt-ready) → Phase 2b (sending on) → Phase 3 (groups) → Phase 4 (reset UX). Each phase is independently shippable. No data migration anywhere; mixed history renders per message via the `encryptionVersion` flag.
