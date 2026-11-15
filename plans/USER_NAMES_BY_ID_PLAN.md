# User Names by Id Plan

Written 2026-11-15. Both repos: client `SchneaggchatV3` (KMP), server `SchneaggchatV3server` (Spring Boot + Mongo). Todo: `plans/Todo.md`, "User names by id instead of names sent as strings"; server `SchneaggchatV3server/Todo.md`, "MOST IMPORTANT (2 of 2)". Builds on `plans/PROFILE_PICTURE_PRIVACY_PLAN.md` (nullable `profilePicUpdatedAt`, D15 there).

Paths: `C/` = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/`.

**Status:** approved 2026-11-15. Contract agreed with the server session (section 3a). Server built and committed on `beta` (`ff49760`). Client steps 1-6 built 2026-11-15, not committed (user review); JVM, Android and iOS compile clean. Open: device tests (section 7), follow-ups in `plans/Todo.md` ("User names by id: follow-ups").

---

## 1. Goal

The server stops sending other users' names as text in in-app data. It sends user ids, and the client resolves every shown user id to a name (and picture) through **one path**: the users table first (friends, requests, the own user; the nickname wins over the name), otherwise a separate cache table filled by a lookup endpoint. Pushes keep their names.

---

## 2. Decisions (user)

| # | Decision | Date |
|---|---|---|
| N1 | Pushes keep sending names as text (shown before the app can look anything up). Only in-app data switches to ids. | 2026-11-12 |
| N2 | The old name-as-text fields are removed with this release; breaking changes are fine (old app versions are forced to update). | 2026-11-12, 2026-11-15 |
| N3 | Any logged-in user may look up any user id's name (names are searchable already). Pictures follow the privacy setting (`profilePicUpdatedAt` null = hidden). | 2026-11-12 |
| N4 | **Client lookup only.** Group co-members are *not* synced like friends. The client finds the ids it needs and fetches them. | 2026-11-15 |
| N5 | Separate table for looked-up users: id, name, picture timestamp, last fetch time (plus the stored picture file). Not the users table. | 2026-11-15 |
| N6 | One path for every shown user id: users table first (own nickname for them, else their name), then the cache table, then a lookup. Applies to every user that is not in the users table. | 2026-11-15 |
| N7 | When to fetch: after a group sync, all group members that are missing in both tables or whose cache entry is older than 1 day (in batches). Opening an event fetches its missing participants. Any other unknown id is looked up when it is shown. | 2026-11-15 |
| N8 | A shown name whose cache entry is older than 1 day is fetched again in the background; the old name shows meanwhile. | 2026-11-12 |
| N9 | Cleanup: every cache entry (and its picture file) is deleted 5 days after its last fetch, group members included; it is fetched again when needed. | 2026-11-15 |
| N10 | Until a name is known (first appearance, offline): "Unknown user". | 2026-11-15 |
| N11 | A deleted account shows "Deleted user" everywhere, history included. The cached name is dropped at the next lookup. | 2026-11-15 |
| N12 | Pictures of cached users load only when shown, and only when visible (`profilePicUpdatedAt` not null); a newer timestamp loads them again. No viewer setting to turn this off. | 2026-11-08 |
| N13 | No "add friend" highlighting of user ids everywhere; adding friends stays in groups and the unknown-user popup as today. | 2026-11-12 |

Accepted consequence of N4 / N8 / N9: a co-member's new name or newly hidden picture reaches this device within about a day (friends' rows update at once). A deleted co-member shows "Deleted user" after the next lookup of that id.

---

## 3. What the server needs to do (request; the server designs its side and replies with the contract)

1. **Lookup endpoint:** takes a list of user ids, returns per id: the current username and `profilePicUpdatedAt` (nullable Long, null = hidden for the requester, same rule as `/users/profilepic/{id}`). Deleted or unknown ids are marked as such (not just left out), so the client can show "Deleted user" and stop asking. The client sends batches; please name the maximum batch size.
2. **Remove every in-app name-as-text field and send ids instead** (N2). Known on the client:
   - System events: `actorName`, `targets[].userName` (ids `actorId`, `targets[].userId` exist).
   - Group members: `memberName` (id `userid` exists).
   - Events: `invitedUserNames` (`EventUserName` list) and `creatorName` (id `creatorId` exists).
   - Socket `friendrequest`: `requestingUserName` (id `requestingUser` exists).
   - Socket `eventupdatealert`: `actorName` (no id today, needs `actorId`).
   - Feedback: entry `creatorName` (no id today, needs the creator's id; only where the client may know who wrote it) and comment `authorName` (`authorId` exists).
   - Announcements: `creatorName` (admin list only; needs the creator's id).
   - Please check the server for more.
3. Push payloads (FCM / APNs) keep their names (N1).
4. Breaking changes are fine (N2); both sides ship in the same release.

## 3a. Agreed contract (server session, 2026-11-15)

- `POST /users/lookup`, body `{"userIds": [...]}`, 1 to 100 ids (duplicates ignored). Reply `{"users": [{"id", "username", "profilePicUpdatedAt"}], "deletedUserIds": [...]}`; every id is in exactly one list. `deletedUserIds` also holds ids that never existed. `username` is the plain name, never a nickname. Errors 400, 401, 429; no 403/404.
- Removed: system event `actorName`, `targets[].userName` (targets are `[{userId}]`); `members[].memberName`; event `invitedUserNames`, `creatorName`, `participations[].userName`; socket `friendrequest` `requestingUserName`; socket `eventupdatealert` `actorName`; feedback entry `creatorName`; feedback comment `authorName`; announcement `creatorName`; map entry `updatedByName`.
- Added: socket `eventupdatealert` `actorId`; feedback entry `creatorId` (nullable, null for seeded entries); announcement `creatorId` (nullable, admin list only).
- Unchanged: leaderboard and recap names, quote `saidByName`, every push payload.
- Server behaviour: a username change no longer bumps groups or sends `groupchange`; lookups send no socket message.
- Client: notifications wait at most 3 s for a name lookup, then use the stored name even if outdated (or "Unknown user"); name and picture load in parallel.

---

## 4. Client design

### 4.1 Data

- New Room table for looked-up users (name to be chosen in the build, e.g. `looked_up_users`): `id`, `name`, `profilePicUpdatedAt: Long?` (null = hidden), `deleted: Boolean`, `fetchedAt` (epoch ms), `profilePictureUrl` (local file, empty when none). Entity, DAO (one-shot and `Flow` reads), mappers, `Database.kt` version bump.
- Remote: the lookup call in the user remote data source, plus its DTOs.

### 4.2 One path for names (N6)

- A repository behind a domain interface (in `C/chat/domain/`, next to `UserRepository`) with:
  - `observeUser(userId): Flow<…>`: emits a small domain model with the display name state (known name / unknown / deleted) and the picture file path (empty when hidden or not loaded). Reads the users table first (nickname > name, picture as stored), otherwise the cache table. A missing or older-than-1-day entry triggers a background lookup (N8); the flow emits again when it lands.
  - `observeUsers(userIds)`: the same for lists (member lists, readers, voters), so a list makes one batch request instead of one per row.
  - `prefetch(userIds)`: fetches the missing and older-than-1-day entries in batches, for group sync and event open (N7).
  - `deleteStale()`: deletes entries (and picture files) last fetched more than 5 days ago (N9).
- Lookups from many flows at once are collected for a short moment and sent as one batch; an id already being fetched is not requested twice.
- The UI maps "unknown" to "Unknown user" (existing `unknown_user`) and "deleted" to a new "Deleted user" string (en / de / it).

### 4.3 Where it hooks in

- After the group sync and on a socket `GroupChange`: `prefetch` all member ids of the changed groups that are not in the users table.
- Event detail open: `prefetch` the event's creator, invitees and participants.
- `DataSyncCoordinator.dataSync`: `deleteStale()` (with the media job).
- Pictures: loaded on first show when visible, the same way as for friends today (download to a file named by user id, stored in the cache row).

### 4.4 Places that switch to the one path

From the known fields and today's "Unknown user" fallbacks (to be completed while building):

- Group members: `GroupMember.memberName` and its entity column go (`C/chat/domain/GroupMember.kt`, `chat/data/local/entity/GroupMemberEntity.kt`, `GroupEntityMapper.kt`, `chat/data/remote/dto/GroupResponseMapper.kt`); users of it: `C/chat/presentation/chat/MessageDisplayMapper.kt:56`, `chat/presentation/chatdetails/Composables.kt:136`, `groupextensions/presentation/split/SplitMembers.kt:33`, the socket notification sender and reactor names (`datasource/network/socket/SocketConnectionMessage.kt:277, :542`).
- System messages: `actorName` / `userName` in `SystemEventNetworkDtos.kt`, rendered in `chat/presentation/chat/messagecomposables/SystemEventText.kt`.
- Events: `invitedUserNames`, `creatorName` (`events/data/…`, `events/domain/Event.kt` `EventUserName`, `EventDetailScreen.kt`, `EventEditPopup.kt`, `EventParticipationOverview.kt:127`).
- Chat list last message sender (`chat/data/OfflineFirstChatListRepository.kt:104, :124`, `sharedUi/buttons/UserButton.kt:268`).
- Mentions (`sharedUi/text/ComboAnnotationSources.kt:60`), poll voters (`PollMessageContentView.kt:728`), message readers, reactions.
- Unknown-user popup: name and picture through the one path (replaces the in-memory load for stored-nowhere users from the picture privacy build, `UnknownUserViewModel.loadUnstoredPicture`).
- In-app socket alerts: friend request (`requestingUserName`), event update (`actorName`).
- Feedback authors (`feedback/presentation/uielements/FeedbackDetailDialog.kt:295, :400`), announcement creators (admin list).

---

## 5. Client steps

1. Data layer: table, DAO, local data source, remote call, DTOs, DB bump.
2. Repository with the one path, batching, prefetch, cleanup; Koin.
3. Hooks: group sync, `GroupChange`, event open, cleanup in `dataSync`.
4. Switch the places in 4.4 one feature at a time (groups and chat, system messages, events, feedback / announcements, socket alerts, popup).
5. Remove the old name fields from DTOs, domain models and entities (after the server reply names the final fields).
6. Strings: "Deleted user" (en / de / it); nothing in `values-de-rAT`.
7. Todo: close this item and the `UnknownUserViewModel` follow-up; device tests.

Each step compiles on its own. The server reply with the contract is needed before steps 1 (DTOs) and 5.

---

## 6. Known gaps (accepted)

- A co-member's name change or newly hidden picture reaches this device within about a day (N8), not at once.
- Offline, a user never seen before shows "Unknown user" until the next lookup.
- A Room version bump wipes queued unsent messages like every bump (see the Todo item on destructive migrations).

---

## 7. Device tests (after both sides are built)

1. Group with a non-friend: names show after the group sync, also offline afterwards.
2. Non-friend renames: the new name shows within a day (or after a cache entry older than 1 day is shown again).
3. Deleted account: "Deleted user" in the member list, system messages and old messages.
4. Event with strangers: names show after opening the event.
5. Mention of a stranger: "Unknown user" first, then the name; tap opens the popup with name and picture (if visible).
6. Cache entries and their pictures are gone 5 days after the last fetch.
7. Friend with a nickname: the nickname shows everywhere, not the looked-up name.
