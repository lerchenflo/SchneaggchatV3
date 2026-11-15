# Profile Picture Privacy Plan

Written 2026-11-15. Both repos: client `SchneaggchatV3` (KMP), server `SchneaggchatV3server` (Spring Boot + Mongo). Todo: `plans/Todo.md`, "Profile picture privacy setting"; server `SchneaggchatV3server/Todo.md`, "MOST IMPORTANT (1 of 2)".

Paths: `C/` = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/`.

**Status:** approved by the user 2026-11-15 (with the nullable timestamp, D15). Contract confirmed to the server session the same day. Client steps 0-4 built 2026-11-15 (JVM and Android compile clean), not committed: waiting for the user's review. Server built on `beta` (commit `5e46473`, 2026-11-15), contract as in section 3 without changes; a refused picture now answers 403 (was 400), a lost file or unknown id 404.

---

## 1. Goal

Each user chooses who can see their profile picture: **public**, **friends and groups**, or **friends only**. The server applies the choice on every way to the picture. The client offers the choice in the privacy settings, never asks for pictures it may not see, and deletes stored pictures that become hidden.

---

## 2. Decisions (user)

| # | Decision | Date |
|---|---|---|
| D1 | Three values: public / friends and groups / friends only. | 2026-11-08 |
| D2 | Default **friends and groups** for everyone, existing users included (today's server rule). No step at registration; the choice lives in the privacy settings only. "Public preselected" was considered and rejected (privacy by default). | 2026-11-12, confirmed 2026-11-15 |
| D3 | Pending friend request: the receiver sees the sender's picture even if the sender's setting would hide it. The sender sees the receiver's picture only if the receiver's setting allows it. | 2026-11-12 |
| D4 | Declined friend request: no unlock. It counts like no friendship (only public or a shared group shows the picture). | 2026-11-15 |
| D5 | Blocking works as today: a blocked friendship only removes the friendship unlock. A public picture or a shared group still shows it to the blocked person. | 2026-11-15 |
| D6 | Event groups count as groups for "friends and groups" (today's rule). | 2026-11-15 |
| D7 | "Public" unlocks the picture for strangers: the new chat search results and the unknown-user popup load it from the server. The "User names by id" lookup uses the same rule later. | 2026-11-15 |
| D8 | Strangers' pictures (search results, unknown-user popup) are only kept while shown, never saved on the device. | 2026-11-15 |
| D9 | A picture that becomes hidden for the viewer is deleted from the viewer's device on the next sync; the placeholder shows instead. | 2026-11-15 |
| D10 | The server marks hidden pictures in the user data, so the client never asks for them (no refused call at every sync, no server log noise). | 2026-11-15 |
| D11 | Changing the own setting needs a connection: the server saves it first. On failure the error popup shows and the old value stays. | 2026-11-15 |
| D12 | No viewer setting to turn pictures off. Owners cannot remove their picture; it stays required at registration. | 2026-11-12, 2026-11-15 |
| D13 | A picture "not found" (404) means the server lost the file. No special client handling: the server logs it (as today), the client asks again on the next sync (as today). | 2026-11-15 |
| D14 | The next update is breaking; breaking changes are not a problem (old app versions are forced to update). | 2026-11-15 |
| D15 | Hidden is signalled by a nullable `profilePicUpdatedAt` (`null` = hidden for this viewer), not by an extra Boolean. `null` has no other meaning, since every user has a picture (D12). Group pictures keep a non-null timestamp. | 2026-11-15 |

---

## 3. Wire contract

Proposed by the server session on 2026-11-15; changed by the user to a nullable timestamp (D15). Points 7 and 8 are the client's additions. Confirmed to the server on 2026-11-15.

1. **Own setting:** `settings.profilePicVisibility` in the own user (`_class: "self"`, its `settings` object). A String: `PUBLIC`, `FRIENDS_AND_GROUPS` or `FRIENDS`. Always present; existing users get `FRIENDS_AND_GROUPS`.
2. **Change it:** `POST /users/settings` with `{"profilePicVisibility": "FRIENDS"}` (the existing partial update; other fields may be left out). Reply: 200 with an empty body; an unknown value gives 400. The new value comes back through the own user in `/users/sync` and the socket `userchange`.
3. **Other users' rows** (`_class` `simple` and `friend`, in `/users/sync` `updatedUsers` and the socket `userchange`): `profilePicUpdatedAt` becomes nullable. `null` = the requester may not see the picture. Otherwise it is the real timestamp as today. On `self` it is never null. No extra Boolean.
4. **Visibility change:** the affected row comes again through `/users/sync` (its `updatedAt` moves) and as a socket `userchange`. Client reaction:
   - timestamp to `null`: delete the stored picture, show the placeholder (D9).
   - `null` to a timestamp, or a new row with a timestamp: load it once through `/users/profilepic/{id}`.
   - a newer timestamp: load it again (as today).
5. **`GET /users/profilepic/{id}`:** 200 image when allowed; 403 when the setting hides it (expected, not logged); 404 when the file is missing (logged, D13).
6. **Group co-members without a friendship row:** not part of this item. `/users/sync` only carries users with a friendship row (friends, pending, declined, blocked). Co-members' names and picture visibility come with the "User names by id" lookup.
7. **Client addition, search results (D7):** `GET /users/availableusers?searchterm=` returns `NewFriendsUserResponse` (`id`, `username`, `commonFriendCount`). Add `profilePicUpdatedAt` (nullable Long, same meaning as point 3) per result, so the client only asks for pictures it may see.
8. **Client confirmation of the rules:** D3 (pending, one-sided), D4 (declined: no unlock), D5 (block as today), D6 (event groups count).

---

## 4. Client steps

In this order. Each step compiles on its own.

### Step 0 — one place that applies a user row (structure, needs the user's OK)

Today the mapping from `UserResponse` to `UserEntity` (including the "load the picture?" check) exists six times: three branches in `OfflineFirstUserRepository.syncUsers` (`C/chat/data/OfflineFirstUserRepository.kt:99-250`) and three in the socket `UserChange` handler (`C/datasource/network/socket/SocketConnectionMessage.kt:352-490`). The hidden-picture handling would have to be added to all six.

Move it into one repository function used by both paths, for the users table only. This is a small part of the Todo item "Two writers per table" (R5). The other tables stay as they are.

### Step 1 — the setting

- Domain enum `ProfilePicVisibility` (`PUBLIC`, `FRIENDS_AND_GROUPS`, `FRIENDS`) in `C/settings/domain/`, like `ThemeSetting`.
- `PersonalUserSettings` (`C/datasource/network/dto/SettingsNetworkDtos.kt`): `profilePicVisibility`, default `FRIENDS_AND_GROUPS`. `UserSettingsRequest`: `profilePicVisibility: String? = null`.
- `SyncedSettings` field and its mapper; stored by `applyServerSettings` through `Preferencemanager`.
- `SettingsRepository`: a flow for the current value and a setter that calls the server first and returns an `EmptyDataResult<DataError>`; the local value changes only on success (D11). This differs on purpose from the other synced settings, which write locally first.

### Step 2 — hidden pictures of users with a row

- `profilePicUpdatedAt` nullable in the `simple` and `friend` `UserResponse` subtypes (`C/datasource/network/dto/UserNetworkDtos.kt`), `UserEntity`, both entity mappers and the domain `User`, plus a `Database.kt` version bump (93 to 94) for the changed column. Per the wiring checklist "New stored field". The bump is destructive like every bump: full re-sync, and queued unsent messages are lost (see the Todo item about stage 2 of the E2E plan).
- In the function from step 0: a hidden row deletes the stored file and clears `profilePictureUrl`, and is never added to the download list. A visible row is downloaded when it is new, was hidden before, or has a newer timestamp.
- Deleting a user row (sync `deletedUsers`, socket `deleted`) also deletes the stored picture file. Today the file stays behind, which would break D9 after an unfriend.
- `fetchMissingProfilePictures` (`OfflineFirstUserRepository.kt:272`) skips hidden users.
- Notification pictures (`C/utilities/notifications/NotificationPictures.kt`) skip users whose row is hidden.

### Step 3 — the option in the privacy settings

- `C/settings/presentation/privacyandsecurity/`: the three values, current value from the flow, the setter on change. Failure: `ErrorChannel` popup, the old value stays selected. No success snackbar needed (the selection itself shows the result).
- Strings in `values/`, `values-de/`, `values-it/` (title, the three values, one short explanation line). Nothing in `values-de-rAT`.

### Step 4 — strangers' pictures, only while shown (D7, D8)

- Repository: load a user's picture as bytes without saving it (returns 403 / 404 as an error; the caller shows the placeholder).
- `ProfilePictureView` (`C/sharedUi/picture/ProfilePictureView.kt`): also accept bytes (Coil's `AsyncImage` takes a `ByteArray` model).
- New chat search: `AvailableUser` gets the nullable `profilePicUpdatedAt`. A visible result loads its picture when its row is shown; the ViewModel keeps the bytes in memory for the screen's life.
- Unknown-user popup (`C/chat/presentation/unknownuser/UnknownUserViewModel.kt`): a user with a local row uses the stored file or the placeholder (row flag). A user without a row loads the picture once when the popup opens; 403 or 404 shows the placeholder.

### Step 5 — wrap up

- Todo: close "Profile picture privacy setting" and the P1 item "a pending friend request unlocks the picture both ways" once both sides are built; add device tests.
- Changelog: per the user's rule for small fixes (the user adds lines).

---

## 5. Server side (behaviour; the server session designs its own code)

- Contract points 1 to 8 above.
- The rules D3 to D6 on every way to the picture: `/users/profilepic/{id}`, the nullable `profilePicUpdatedAt` on user rows and search results, later the name lookup.
- Every visibility change reaches the viewer through `/users/sync` and the socket: owner setting changes, friendship changes (request, accept, decline, remove, block), and shared groups joined or left. The server plans to bump the friendship row between two users when they start or stop sharing a group (not yet confirmed by the user on the server side).
- A 403 caused by the setting is not logged as an attack.

---

## 6. Release

Both sides ship together in the next release (D14: breaking is fine, old versions are forced to update). Branch `next-release` on both repos.

---

## 7. Known gaps (accepted, handled later)

- Group co-members without a friendship row: their visibility and the deletion of their stored pictures come with "User names by id". Until then a picture that a notification downloaded for such a co-member (`NotificationPictures`) stays on the device.
- Unfriended but still in a shared group: the row is deleted, so the picture file is deleted too (step 2). Group views show the placeholder until "User names by id" or a notification loads it again, although "friends and groups" allows it.
- A picture that a viewer already copied (screenshot, export) can't be taken back.

---

## 8. Device tests (after both sides are built)

1. Change the setting online: the new value shows, a second device of the same account shows it after sync.
2. Change it offline: error popup, old value stays.
3. Friend sees the picture with every value.
4. Pending request: receiver sees the sender's picture with "friends only"; the sender does not see the receiver's with "friends only".
5. Declined request: no picture unless public or shared group.
6. Switch from public to friends only: a non-friend's device deletes the stored picture on the next sync and shows the placeholder; no further picture requests for that user (check the server log).
7. Switch back: the picture loads again once.
8. Public: the picture shows in the new chat search and in the unknown-user popup; after leaving the screen it is not stored.
9. Blocked person with public setting: still sees the picture (D5).
