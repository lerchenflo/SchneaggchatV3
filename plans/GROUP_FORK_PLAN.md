# Group Fork Plan

Companion docs: `CHAT_BASED_MIGRATION_PLAN.md`, `E2E_ENCRYPTION_PLAN_V2.md`, `GROUP_EXTENSIONS_PLAN.md` (same folder). Both repos: client `SchneaggchatV3` (KMP) and server `SchneaggchatV3server` (Spring Boot + Mongo).

Paths: server = `SchneaggchatV3server/src/main/kotlin/com/lerchenflo/schneaggchatv3server/` (`S/`), client = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/` (`C/`). Line numbers verified 2026-10-26.

**Goal**: a member can fork an existing group, like forking a repo on GitHub. The fork is a new, independent group that starts with the parent's members and keeps a permanent reference to the group it was forked from. Forks are never merged back. A fork can be deleted, and its details screen shows where it came from.

**Status**: proposal. Nothing is decided yet; see §8 for the open decisions. The plan names a recommended default for each one so implementation can start once they are confirmed.

---

## 1. Is this a good feature?

### 1.1 Use cases it covers

- **Split off a sub-topic**: a big group plans a trip, four people want to organise the car. Today they create a new group by hand and pick the members again.
- **Fresh start, same people**: a group has a long, noisy history (or an old name/picture that no longer fits). A fork gives a clean chat with the same people and keeps the old one as an archive.
- **Keep an event group alive**: event groups expire (`Group.expiresAt`, `GroupService.deleteExpiredGroups`, `S/group/GroupService.kt:658`). A fork before expiry keeps the people together without touching the event's timer.
- **Subgroup without some members**: fork with all members preselected, then deselect the people who should not be in the new chat.

### 1.2 Risks

- **Group clutter**: every fork is a full group in the chat list. Mitigation: the fork reference makes it visible which groups belong together (§5.3), and forks can be deleted (§4.3).
- **Consent / spam**: a fork pulls every selected member into a new group without asking. Today only admins can add people and the creator must be friends with every member (`S/group/GroupService.kt:72-78`). A fork by a normal member who is not friends with everyone would bypass both rules. See decisions D1 and D2.
- **Privacy of excluded members**: if the parent gets a "X forked this group" system message, members who were left out learn that a group without them exists. See D6.

### 1.3 Alternatives

- **Topics / threads inside one group** (Telegram topics, Slack threads): solves "sub-topic" without a new group, but is a much bigger feature (message model, UI, notifications) and does not solve "fresh start" or "subgroup without some members".
- **"Create group from members" shortcut only**: the group creator opens prefilled with the parent's members, no reference is stored. Very cheap, but loses the "where did this come from" link the user explicitly wants.

### 1.4 Verdict

Worth doing as a small feature. The useful core (new group + same members + permanent parent link + delete) is cheap because it reuses group creation almost completely. Copying message history is the expensive and risky part (§6); it should not be in v1. The fork reference is also a good base for later ideas (show related groups, fork extensions from `GROUP_EXTENSIONS_PLAN.md`).

Rough effort for v1 without history: server ~4 files, client ~12 files plus strings, about the size of the group expiry feature.

---

## 2. Current state (verified 2026-10-26)

### 2.1 Server

- `S/group/model/Group.kt`: `id, name, description, updatedAt, profilePicUpdatedAt, expiresAt, deleted, createdAt, creatorId`. No parent/fork field. Soft delete via `deleted`.
- `S/group/model/GroupMember.kt`: `userid, groupId, joinedAt, admin, color, joinedAtVersion`; unique `{userid, groupId}`.
- `S/group/model/GroupResponse.kt`: response sent over `/groups/sync` and the `GroupChange` socket message.
- `GroupService.createGroup` (`S/group/GroupService.kt:55`): at least 3 members (:61, skipped for event groups), creator must be friends with every member (:72-78), saves the picture through `ImageManager.saveProfilePic` (`S/util/ImageManager.kt:34`), generates unique member colors, sets `joinedAtVersion` to the current `SyncCollection.MESSAGES` counter (:115), notifies all members (`notifyGroupUpdate`) and posts `SystemEventType.GROUP_CREATED` (:144).
- `GroupController` (`S/group/GroupController.kt`): `/groups/create` (multipart), `/groups/sync`, `/groups/profilepic/{id}`, `setprofilepic`, `setdescription`, `setGroupName`, `setexpiry` (admin only), `changemembers`, `DELETE /groups/delete` (admin only, added 2026-10-26, see §4.3). Before that endpoint a group was only deleted when the last member left (`performUserAction`, :360), its expiry passed (`deleteExpiredGroups`), or the event creator deleted it through `DELETE /events/delete?deletegroup=true`. All paths go through `GroupService.deleteGroup`, which soft-deletes, removes members, deletes the picture, notifies, and detaches or deletes a connected event.
- `GroupLookupService.isUserInGroup` (`S/group/GroupLookupService.kt:84`) does not check `group.deleted` (already noted as F-item in `CHAT_BASED_MIGRATION_PLAN.md` §3.1).
- Server JSON ignores unknown properties (`S/util/JsonMapper.kt:9`).

### 2.2 Client

- `C/chat/domain/Group.kt`, `C/chat/data/local/entity/GroupEntity.kt` (Room table `groups`), DTO `C/datasource/network/dto/GroupNetworkDtos.kt` (`GroupResponse`), mapper `C/chat/data/remote/dto/GroupResponseMapper.kt`.
- `GroupRepository` (`C/chat/domain/GroupRepository.kt`) + `OfflineFirstGroupRepository`: server-first writes, each write runs its own `syncGroups()` afterwards (D16/R1 comment).
- Group creation UI: `Route.GroupCreator` (data object, `C/app/navigation/Route.kt:20`), `GroupCreatorViewModel` (`C/chat/presentation/newchat/GroupCreatorViewModel.kt`). Selectable users come from `ChatListRepository.getChatSelectorFlow` filtered to non-groups, so **only friends can be selected**. A profile picture is required (`GroupCreatorState.valid()`).
- Group details: `ChatDetailsViewmodel` (`C/chat/presentation/chatdetails/ChatDetailsViewmodel.kt`) with `ChatDetailsState.GroupDetails`, screen in `ChatDetails.kt` / `Composables.kt`. Has member management, expiry, connected event, shared content, birthdays.
- System messages: `SystemEventType` (`C/chat/domain/SystemEvent.kt`) with an `UNKNOWN` fallback, rendered in `SystemEventText.kt`. Old clients degrade new event types to a generic line.
- Client JSON ignores unknown keys (`C/datasource/network/AppJson.kt:19`), so new response fields do not break old clients.
- Room DB version 87 (`C/datasource/database/Database.kt:23`), destructive migration: a schema change is a version bump + full re-sync.
- Removing a group locally (`RoomGroupDataSource.deleteGroup`, `C/chat/data/local/RoomGroupDataSource.kt:46`) also deletes its messages.

### 2.3 Planned changes that touch this feature

- `CHAT_BASED_MIGRATION_PLAN.md` folds `groups` into a `chats` collection, adds `historyVisibility` (`FULL | SINCE_JOIN`), `membershipState = LEFT` (left chats stay readable, D8 there), a per-chat backfill endpoint, and moves `/groups/**` to `/chats/**`. Planned order: repository migration, its Todo follow-ups, chat migration, then E2E.
- `E2E_ENCRYPTION_PLAN_V2.md`: once shipped, the server cannot read message content, so it cannot copy or rewrite messages for a fork.
- `GROUP_EXTENSIONS_PLAN.md`: extensions owned by a group (`ownerType` + `ownerId`). A fork could copy them (D10).

---

## 3. Core design (v1)

A fork is created **server-side in one request** from the parent group id. The server reads the parent's members itself, so the client cannot invent membership, and the parent link is written by the server, never trusted from the client.

```
Group (new fields, all nullable, null = not a fork)
  forkedFromGroupId   ObjectId?   direct parent only (like GitHub)
  forkedFromName      String?     snapshot of the parent name at fork time
  forkedAt            Instant?    time of the fork
  forkedAtVersion     Long?       MESSAGES counter at fork time; only needed for history option C (§6)
```

- **Name snapshot**: the parent can be renamed, deleted, or the viewer may not be a member of it. The snapshot always lets the fork show "Forked from <name>". If the viewer is a member of the parent, the client shows the parent's current name and links to it.
- **Direct parent only**: a fork of a fork points to its own parent. A `rootGroupId` is not needed for v1 (D8).
- **Parent deletion**: the fork keeps `forkedFromGroupId`; the client shows the snapshot name with a "(deleted)" hint because the parent is no longer in Room.
- **No merge**: there is no endpoint and no UI to merge.

---

## 4. Server changes (`SchneaggchatV3server`)

### 4.1 Model

- `S/group/model/Group.kt`: add `forkedFromGroupId: ObjectId? = null`, `forkedFromName: String? = null`, `forkedAt: Instant? = null` (+ `forkedAtVersion: Long? = null` if history option C is chosen). Defaults keep existing documents valid, no migration needed.
- `S/group/model/GroupResponse.kt`: add `forkedFromGroupId: String?`, `forkedFromName: String?`, `forkedAt: Long?`; fill them in `GroupLookupService.getGroupAsGroupResponse` (:61).
- Optional index on `forkedFromGroupId` only if the parent should list its forks (D7).

### 4.2 Fork endpoint

`POST /groups/fork`, JSON body:

```
ForkGroupRequest(
  parentGroupId: String,         // validated ObjectId
  name: String,                  // default on the client: "<parent name> (Fork)"
  description: String,
  memberIds: List<String>,       // must be a subset of the parent's current members (D3)
  copyProfilePicture: Boolean,   // true = server copies the parent picture file (D9)
)
```

`GroupService.forkGroup(requestingUser, request)`:

1. Parent exists and is not deleted; requester is a member (`isUserInGroup`). Admin check if D1 says admins only.
2. Every id in `memberIds` is a current member of the parent; the requester is always added. Members who left the parent between opening the screen and sending are dropped silently or rejected (D3).
3. Friendship rule per D2: either keep the `createGroup` check (requester friends with everyone) or skip it, because all members already share the parent group.
4. Minimum member count: reuse the `> 2` rule from `createGroup` (:61) or allow 2 (D4).
5. Create the group through the same code path as `createGroup` (extract the shared part into a private function instead of copying it). Differences: picture may be copied from the parent file (`ImageManager.getProfilePicFileName` + file copy, new helper in `ImageManager`), fork fields set, member colors copied from the parent where possible (keeps everyone's color, D9), admin flags per D5.
6. No `expiresAt` and no event link on the fork (D11).
7. Notify all fork members (`notifyGroupUpdate`), post system messages (§4.4), log `LogType.GROUP_FORKED` (new entry).
8. Rate limit: reuse the same limit as `/groups/create` if one exists, otherwise add one; a fork is a cheap way to create many groups.

### 4.3 Delete endpoint - implemented 2026-10-26

Shipped ahead of the fork as a standalone feature (D12 decided: admins, any group; a connected event is detached and survives).

- Server: `DELETE /groups/delete?groupid=` (`GroupController.deleteGroup`) -> `GroupService.deleteGroupAsAdmin` (404 for a missing/deleted group, member + admin check via `requireOrLog`) -> `deleteGroup(groupId, deletedBy, deleteConnectedEvent = false)`.
- Client: `GroupRemoteDataSource.deleteGroup` / `GroupRepository.deleteGroup` (local delete as optimistic fast path, the `GroupChange` push converges), `ChatDetailsViewmodel.deleteGroup` (navigates to the chat selector on success), admin-only "delete group" button with confirmation under "leave group" in `ChatDetails.kt`.

A fork therefore needs no extra delete code. When the chat migration lands, this becomes `/chats/{id}/delete` together with the other group endpoints.

### 4.4 System messages

New `SystemEventType` values on both sides (server `message/messagemodel/SystemEventType`, client `C/chat/domain/SystemEvent.kt`):

- `GROUP_FORKED_FROM` in the **fork** as its first line: "X forked this group from <parent name>" (replaces `GROUP_CREATED` for forks).
- `GROUP_FORKED` in the **parent**: "X created the fork <fork name>". Only if D6 says yes.

Old clients show the `UNKNOWN` fallback line, which is acceptable.

### 4.5 Security checklist (run `spring-endpoint-security-check` on both endpoints)

- Requester must be a parent member (no forking of groups you are not in, otherwise the member list leaks).
- Member ids are checked against the parent on the server, never trusted.
- Fork fields are set by the server only.
- Delete is admin-only and checks the group is not already deleted.

---

## 5. Client changes (`SchneaggchatV3`)

Run `code-reuse-scout` before implementing (project rule). Candidates: `GroupCreatorViewModel`, `MemberSelector` (`C/sharedUi/popups/MemberSelector.kt`), `ChatDetailsViewmodel.navigateToChat`.

### 5.1 Data layer

- DTO `GroupResponse` (`C/datasource/network/dto/GroupNetworkDtos.kt`): add the three nullable fields with `= null` defaults.
- `Group` domain + `GroupEntity`: add `forkedFromGroupId`, `forkedFromName`, `forkedAt`. Update `GroupEntityMapper.kt` and `GroupResponseMapper.kt`. Bump Room version 87 → 88 (destructive, full re-sync). If the chat migration lands first, these fields go on `ChatEntity` instead (§7.1).
- New DAO query: `getForksOfGroupFlow(groupId)` (`SELECT * FROM groups WHERE forkedFromGroupId = :groupId`), only if D7 says the parent lists its forks. It only sees forks the user is a member of, which is correct for privacy.
- `GroupRemoteDataSource` + `KtorGroupRemoteDataSource`: `forkGroup(request)` and `deleteGroup(groupId)`.
- `GroupRepository` + `OfflineFirstGroupRepository`: `forkGroup(...): String?` (returns the new group id, same shape as `createGroup`), `deleteGroup(groupId): Boolean` (server delete, then `syncGroups()` like the other writes).

### 5.2 Fork flow (UI)

- Entry point: a "Fork group" action in the group details screen (`ChatDetails.kt`), visible per D1.
- Screen: reuse the group creator. Change `Route.GroupCreator` from a data object to `data class GroupCreator(val forkFromGroupId: String? = null)` and keep the `NavigationState.kt:194,212` serializer registration in sync. In fork mode `GroupCreatorViewModel`:
  - prefills name `"<parent> (Fork)"`, description, and picture (shows the parent picture, "keep picture" = `copyProfilePicture = true`, so the "picture required" rule is satisfied without a new upload);
  - shows the parent's members, all preselected, instead of the friend list. Non-friend members are shown too if D2 allows them;
  - calls `groupRepository.forkGroup(...)` instead of `createGroup(...)` and opens the new chat like today.
- Follow `mobile-ux-conventions` for the confirm button placement.

### 5.3 Showing the reference

- Fork details screen (`ChatDetailsState.GroupDetails`): a row "Forked from <name>" with the fork date.
  - Parent in Room (user is a member): tappable, opens the parent chat via `navigateToChat(parentId, isGroup = true)`.
  - Parent not in Room (user is not a member, or it was deleted): plain text with the snapshot name, not tappable.
- Optional (D7): parent details screen lists "Forks" the user is a member of.
- Optional: small fork icon next to the name in the chat list or top bar. Not in v1 unless asked.

### 5.4 Delete

Done, see §4.3.

### 5.5 Strings

New strings in `values/strings.xml` with German and Italian translations; nothing added to `values-de-rAT` (project rule). Needed: fork action, fork screen title, "Forked from %1$s", "(deleted)", delete action + confirmation, the two system event lines.

---

## 6. Message history: options

The user has not decided whether messages go to the fork (D13). Options:

| Option | What the fork shows | Server cost | Client cost | Works with chat migration | Works with E2E |
|---|---|---|---|---|---|
| **A. No history** | Empty chat, "Forked from X" row and first system line link to the parent | none | none | yes | yes |
| **B. Copy messages** | Real copies of all (or the last N) messages | High: copy `Message` documents with new ids and versions, remap reply ids, copy or re-link media files (file names depend on the group, `S/util/ImageManager.kt:72-94`), drop or copy reactions, readers, polls; every member downloads the copies again | low (normal sync) | possible, but `createdVersion`/`SINCE_JOIN` rules need care | **no**: the server cannot read or re-key ciphertext; client-side copy would mean re-sending every message |
| **C. Virtual prefix** | Parent messages up to `forkedAtVersion` shown read-only above the fork's own messages, read from the local Room DB, no copy | none (store `forkedAtVersion`) | Medium: chat paging merges two sources, search and shared content need the same rule | yes; with D8 there (left chats stay readable) the prefix even survives leaving the parent | yes: the client already has the parent's keys |
| **D. Link only** | Same as A, plus a "View history in <parent>" button that opens the parent chat scrolled to the fork point | none | low | yes | yes |

Notes:
- With C, a member added to the fork later does not see the prefix (they were never in the parent), and the prefix disappears today when the parent is deleted or the user leaves it (local purge in `RoomGroupDataSource.deleteGroup`). After the chat migration only deletion purges.
- B duplicates storage and creates two editable copies of the same message, which is confusing (edit in parent does not reach the fork).

**Recommendation**: A + D in v1. Store `forkedAtVersion` from the start anyway (one field), so C can be added later without a data migration. Avoid B.

---

## 7. Interaction with other plans

### 7.1 Chat-based migration

- If the fork ships **before** the chat migration: the new `Group` fields must be added to the migration's field mapping (`CHAT_BASED_MIGRATION_PLAN.md` §5.2 step 1) and to `ChatEntity` / `ChatResponse`, and `/groups/fork` + `/groups/delete` move to `/chats/{id}/fork` and `/chats/{id}/delete`.
- If it ships **after**: implement directly on `chats` (GROUP type only; a DIRECT chat cannot be forked). Fork fields live on the chat document. The fork gets `historyVisibility` per D13 (`SINCE_JOIN` fits option A/D).
- Recommendation (D14): ship after the chat migration, because the migration rewrites every group file this plan touches. If the feature is wanted sooner, the v1 scope is small enough to port.

### 7.2 E2E

Options A, C and D are compatible. Option B is not (server cannot copy ciphertext meaningfully, and Megolm-style sessions are bound to the chat).

### 7.3 Group extensions

When `GROUP_EXTENSIONS_PLAN.md` ships, a fork could copy extensions (shared text, split) as a snapshot. Out of scope for v1 (D10).

---

## 8. Open decisions

| # | Question | Options | Recommendation |
|---|---|---|---|
| D1 | Who may fork? | any member / admins only | Any member (a fork does not change the parent), combined with D2 |
| D2 | Must the forker be friends with every selected member? | keep the `createGroup` friendship rule / skip it because everyone already shares the parent / non-friends allowed but not preselected | Skip the rule only for members of the parent, members can leave the fork. Respect blocks: never add a user who blocked the forker or is blocked by them |
| D3 | Member selection | always all members / all preselected, deselect allowed / pick freely incl. friends outside the parent | All preselected, deselect allowed, no outsiders in the fork screen (add them later the normal way) |
| D4 | Minimum members | 3 like normal groups / 2 | 3, same as normal groups |
| D5 | Admins in the fork | forker is the only admin / copy parent admin flags | Forker only admin |
| D6 | System message in the parent ("X created fork Y")? | yes / no / only when all members were taken over | No (excluded members would learn about the fork) |
| D7 | Parent shows its forks? | no / yes, only forks the viewer is a member of | Yes, viewer's forks only (local query, no server change) |
| D8 | Nested forks (fork of a fork) | allowed, direct parent only / allowed + store root / forbidden | Allowed, direct parent only |
| D9 | What is copied from the parent | name, description, picture, member colors, expiry, pinned/mute state | Name (+ " (Fork)"), description, picture, member colors; not expiry, not mute/pin |
| D10 | Copy group extensions (after `GROUP_EXTENSIONS_PLAN.md`) | no / snapshot copy | No in v1 |
| D11 | Forking an event group | fork is a plain group (no event link, no expiry) / forbidden | Plain group |
| D12 | Delete endpoint scope | admins can delete any group / only forks / creator only | **Decided 2026-10-26: admins, any group; connected event is detached. Implemented (§4.3)** |
| D13 | Message history | A none / B copy / C virtual prefix / D link to parent (§6) | A + D now, store `forkedAtVersion` for C later |
| D14 | Timing vs. chat migration | before / after | After |
| D15 | Notify members on fork? | normal push like "added to group" / silent | Same push as being added to a new group |
| D16 | What happens to the fork when the parent is deleted? | nothing, show snapshot + "(deleted)" / delete forks too | Nothing |

---

## 9. Phases

| Phase | Scope | Done when |
|---|---|---|
| 0 | Confirm decisions D1-D16 | table above has a "Decided" column filled in |
| 1 | Server: model fields, `getGroupAsGroupResponse`, `/groups/fork`, system event types, log type, security check (`/groups/delete` already done) | endpoints pass `spring-endpoint-security-check`, fork appears in `/groups/sync` for every selected member with the parent fields |
| 2 | Client data: DTO, domain, entity, mappers, Room bump, remote + repository methods | fork and delete callable from the repository, fork fields stored |
| 3 | Client UI: fork entry in group details, fork mode in `GroupCreator`, "Forked from" row, delete action, system event texts, strings (EN/DE/IT) | fork created from the details screen opens the new chat; reference row links to the parent or shows the snapshot |
| 4 (optional) | Parent lists its forks (D7), history option C (D13) | as decided |

Post-implementation per project rules: `readme-changelog-updater`, `string-resource-extractor`, and `flow-stability` over fork + delete (socket `GroupChange`, parallel `/groups/sync`, parent deleted while forking).
