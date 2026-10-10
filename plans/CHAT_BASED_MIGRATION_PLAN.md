# Chat-Based Addressing Migration Plan

Migrates messaging in both repos from **user-based addressing** (`Message.receiverId` = user id *or* group id + `groupMessage` flag) to **chat-based addressing** (`Message.chatId` + a `chats` collection with type `DIRECT | GROUP` and an explicit member list).

**Why**: prerequisite for `E2E_ENCRYPTION_PLAN_V2.md`. With one membership source per chat, E2E key distribution, rotation, healing, user/key reachability and server-side authorization become *one* code path for DMs and groups instead of two (§8). It also removes the `(senderId, receiverId, groupMessage)` CASE logic that is duplicated across ~10 places in the client.

**Release model**: client and server migrate in **one release**, no backwards compatibility, no dual-format endpoints. Old clients break against the new server; no min-version gate is added (D5), users must update.

Paths: server = `SchneaggchatV3server/src/main/kotlin/com/lerchenflo/schneaggchatv3server/` (`S/`), client = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/` (`C/`). Line numbers verified 2026-10-10 (two independent sweeps + one adversarial review).

---

## 1. Current state (verified 2026-10-10)

### 1.1 Server
- `S/message/messagemodel/Message.kt`: `senderId` (`@Indexed`, :43; group id for group SYSTEM messages), `receiverId` (`@Indexed`, :45), `groupMessage` (:47), `version` (:70, bumped by **every** change: edit, reaction, poll vote, read receipt — `MessageService.kt:743-751`). Compound indexes `senderId_version_idx`, `receiverId_version_idx`, unique partial `sender_client_message_idx` (:16-31). `MessageRepository` is empty; all queries via `MongoTemplate`. `spring.data.mongodb.auto-index-creation=true` (`application.properties:20`).
- Requests (`messagemodel/MessageRequest.kt`) carry `receiverId` + `groupMessage`; `MessageResponse` carries both (:6-7).
- Authz `MessageService.canUserAccessMessage` (:823-864): send = group membership / `areFriends`; per-message = current group membership / sender-or-receiver. Self-DMs forbidden at send (:856).
- Sync `MessageLookupService.getMessagesSince` (:89-123) + `MessageService.messageSync` (:797-818): visibility `senderId == u OR receiverId == u OR (groupMessage AND receiverId IN myGroups)`, plus a `fullHistoryGroups` clause (groups with `joinedAtVersion >= since`, `GroupLookupService.kt:42-45`) that replays the whole group history with no lower bound. Page size 400 (client `NetworkUtils.kt:1143`), `newVersion = max(page) ?: since`.
- Groups: `Group` (:14-27), `GroupMember` (`groupmembers`, field `userid`, unique `userid_groupId_idx`, `joinedAtVersion`, :16-39). `/groups/sync` timestamp diff; live via `SocketConnectionMessage.GroupChange`.
- Friendship: `Friendship` (sorted `userId1/userId2`, status). **removeFriend, decline and unblock hard-delete the document** (`FriendsService.kt:147,157,191,196-200`). `FriendshipSetting` per user (unused `muted`).
- `/users/sync` only covers users with a Friendship document (`FriendsLookupService.getAllInteractions :68-69`) — event-group co-members and ex-friends are never synced.
- Pinned chats: `User.settings.pinnedChats: List<PinnedChat(chatId, group, pinTimePoint)>` (`user/usermodel/PersonalUserSettings.kt:20,37-41`), validated in `UserController.kt:230-256`.
- Push: `MessageNotificationResponse` (`notifications/firebase/model/NotificationResponse.kt:23-41`) has `receiverId`, `groupMessage`, `groupId`, **plaintext `content`** (global-key wrap removed 2026-09-05, `SECURITY_AUDIT.md` M-2). Data map carries both `_class` (`@JsonTypeInfo`, :7-11) and `type` (`FirebaseService.kt:413-422`, `ApnsService.kt:326-336`).
- Media file names depend on `groupMessage`: `util/ImageManager.kt:72-94`, `util/AudioManager.kt:22-49`.
- Startup migrations: idempotent functions in `S/core/MainController.kt:87-118`, run on `ApplicationReadyEvent` — i.e. **after Tomcat already serves requests**. No Mongock, no API versioning, no min-version gate.
- Realtime fan-out to all connections per user is already fixed (`SocketConnectionHandler.kt:117` uses `filter`).

### 1.2 Client
- Room `C/datasource/database/Database.kt`, **version 87**, destructive migration (`CreateDatabase.kt:12`). Persisted sync cursor = `MAX(version)` in Room (`Daos.kt:354`).
- `MessageDto` (`C/chat/data/dtos/MessageDto.kt`): `senderId`, `receiverId`, `groupMessage`; two OR-arm indexes + one covering index for aggregates.
- Chat identity is derived in many places with the same CASE: `Daos.kt:166-207`, `Daos.kt:223-341` (`(senderId = :id OR receiverId = :id) AND groupMessage = :gruppe`), `Daos.kt:385-401` (unread badge; DM arm requires `frienshipStatus = ACCEPTED`), `AppRepository.chatKey` (:2933), `SocketConnectionMessage.kt:224`, `MessageNotificationMapper.kt:30`, search (`AppRepository.kt:1133-1143`), `Message.isThisChatMessage` (`chat/domain/Message.kt:85`), `MessageWithReadersDto.isThisChatMessage` (:39).
- `GroupDto` + `GroupMemberDto` exist only for groups; DMs have no chat row.
- Pinned chats + drafts in DataStore keyed by `(chatId, group)` (`Preferencemanager.kt:124-126, 306-357, 425-479`; drafts wrapped by `settings/data/SettingsRepository.kt:27-36`).
- Navigation `Route.Chat(chatId, isGroup, …)`, `ChatDetails(chatId, isGroup)`, `ChatSharedContent(chatId, isGroup, …)` (`app/navigation/Route.kt:16,33,38`); Koin passes `(chatId, isGroup)` to ViewModels (`di/Modules.kt:209-243`).
- Notifications: `PayloadDecoder.kt` (`chatTargetId` = groupId or senderId, decodes on `_class`), Android extras `EXTRA_CHAT_ID` + `EXTRA_GROUP_CHAT`, notification id = `chatId.hashCode()`. iOS NSE (`iosApp/SchneaggchatNotificationServiceExtension/NotificationPayload.swift`) decodes on `type`, parses `groupMessage`.

---

## 2. Target data model

### 2.1 Server collections

```
chats
  _id                ObjectId    GROUP: == old group id (unchanged). DIRECT: new ObjectId.
  type               DIRECT | GROUP
  pairKey            String?     DIRECT only: "<smallerHex>:<largerHex>" (lowercase hex; ordering
                                 == ObjectId.compareTo, consistent with Friendship.userId1/2).
                                 Unique partial index.
  name, description, profilePicUpdatedAt, expiresAt, creatorId     GROUP only (nullable)
  createdAt, updatedAt, deleted
  membershipVersion  Long        +1 on every member-set change (join, leave, remove, account
                                 deletion) and on DIRECT send-permission change (§2.2).
  historyVisibility  FULL | SINCE_JOIN    (§8.4, D2): new groups SINCE_JOIN, optional group-settings
                                 toggle to FULL; migrated groups FULL

chatmembers                      (replaces groupmembers; unique {chatId, userId}, index {userId})
  chatId, userId, joinedAt, joinedAtVersion
  role               MEMBER | ADMIN       DIRECT: always MEMBER
  color              Int                  GROUP only
  muted              Boolean              private: only returned to the member itself (D6)

messages
  _id, chatId (ObjectId), senderId (ObjectId?, null = SYSTEM message of the chat),
  createdVersion (Long, immutable, = version at insert),
  msgType, answerId, content, poll, systemEvent, sendDate, lastChanged, deleted, edited,
  version, readers, reactions, clientMessageId
  -- removed: receiverId, groupMessage
```

Indexes on `messages`:
- `{chatId: 1, version: 1}` — sync range scan.
- `{chatId: 1, createdVersion: 1}` — backfill + SINCE_JOIN filtering.
- `sender_client_message_idx` — **keep name and definition byte-identical** (changing the partial filter for nullable `senderId` is unnecessary — SYSTEM rows have no `clientMessageId` — and a changed definition under the same name aborts startup with IndexOptionsConflict 86).

**Decisions baked in**:
- **GROUP chat id = old group id.** Events, group profile pictures, `joinedAtVersion` and future group extensions keep their keys. Only DMs need new ids.
- **DIRECT chats are created by the server only**, idempotently via `pairKey` upsert, at friendship accept (and by the migration). Client never creates a DM ⇒ no client id scheme, no duplicate-DM race.
- `Group` collection is **folded into `chats`**. One `ChatResponse` replaces `GroupResponse`.
- **All** SYSTEM messages (group *and* direct) get `senderId = null`; the actor stays in `systemEvent.actorId`. One rule, no asymmetry; recap stats ignore SYSTEM uniformly.
- `chatId` is an `ObjectId` everywhere (server model, raw-Document code, events). Never write hex strings into Mongo.

### 2.2 DIRECT chats and friendship
Friendship stays a separate concept (location sharing, wake, phone number). The DIRECT chat outlives it:

| Friendship state | DIRECT chat | Send / mutate | Visible in chat list |
|---|---|---|---|
| ACCEPTED | exists, both members | yes | yes |
| removed / declined / blocked (doc hard-deleted or BLOCKED) | stays, both members | **no** (`canSend`) | **hidden** (D1, as today; local messages kept, reappear on re-friend) |
| re-accepted | same chat reused via `pairKey` | yes | yes |

The member set of a DIRECT chat never changes. `membershipVersion` is still bumped on send-permission changes as **hygiene** (forces a fresh session after re-friending, keeps rotation logic uniform) — the actual protection against an ex-friend is `canSend`, not rotation: the partner legitimately receives post-refriend sessions anyway.

### 2.3 Client Room (DB 87 → 88)

```
ChatDto        (replaces GroupDto)
  id, type, partnerUserId?   (DIRECT: the other member, denormalized for user→chat lookup)
  name?, profilePictureUrl?, description?, expiresAt?, createDate, updatedAt, profilePicUpdatedAt
  membershipVersion, historyVisibility, muted (own flag)
  membershipState    ACTIVE | LEFT   (D8: LEFT = removed/left, read-only, history kept)
  backfillDoneBeforeVersion Long?   per-chat backfill progress (§3.3)
ChatMemberDto  (replaces GroupMemberDto)   unique {chatId, userId}, index {userId}
  chatId, userId, joinDate, admin, color, memberName
MessageDto
  + chatId (indexed), + createdVersion, senderId nullable; - receiverId, - groupMessage
```

Message indexes: `{chatId, sendDate}` (chat window and all `…ForChatFlow` queries), covering `{readByMe, myMessage, chatId, msgType, sent, sendDate}` (aggregates, unread badge); `id`, `sent`, `version`, `clientMessageId` unchanged.

`UserDto` keeps user/friendship fields; `notisMuted` moves to `ChatDto.muted`. `UserDto` must also hold **non-friend co-members** (§3.1 user reachability).

---

## 3. Server changes (`SchneaggchatV3server`)

Follow the existing package style (`spring-boot-architecture` skill + project skills): controller with nested DTOs → service → LookupService owning the repository; `requireAuth()` first line.

### 3.1 New `chat/` package (absorbs `group/`)
- `model/Chat.kt`, `model/ChatMember.kt`, `model/ChatResponse.kt`: `ChatResponse(id, type, members, name?, …, membershipVersion, historyVisibility, ownMuted)`; members carry `userId, role, color, joinedAt`; other members' `muted` is never serialized. `/chats/sync` response = `updatedChats`, `leftChats: List<chatId>` (D8: client keeps read-only) and `deletedChats: List<chatId>` (client wipes).
- `ChatLookupService`: `isMember(userId, chatId)` (**checks `chat.deleted`** — `GroupLookupService.isUserInGroup :83-86` does not), `getChatIdsForUser`, `getMembers`, `getMember(chatId, userId)`, `getOrCreateDirectChat(a, b)` (upsert by `pairKey`, rejects `a == b`), `getDirectChat(a, b)`, `canSend(userId, chatId)` = member AND (GROUP OR friendship ACCEPTED).
- `ChatService`: everything from `GroupService` (create, metadata, `performUserAction`, expiry, delete, `deleteExpiredGroups` → `deleteExpiredChats`, called from `core/DatabaseCleanupTask.kt:14,37`); every member-set change bumps `membershipVersion` + `updatedAt`.
- `ChatController` under `/chats`: `/chats/sync` (replaces `/groups/sync`; same `IdTimeStamp` diff but over *all* chats incl. DIRECT), `/chats/create` (GROUP only), `/chats/{id}/profilepic` (GET, from `GroupService.getGroupProfilePic :192`), `/chats/{id}/setname|setdescription|setprofilepic|setexpiry|setmuted|sethistoryvisibility` (the last one admin-only, D10), `/chats/changemembers`, `/chats/{id}/messages` (backfill, §3.3). `/groups/**` deleted.
- **User reachability by chat membership**: `/users/sync` (`UserService.userIdSync :92-157`) covers *friendships ∪ users sharing any chat*. Non-friend co-members get a `SimpleUserResponse` (name, picture; later the V2 key fields). Without this, event-group co-members and ex-friends have no name/picture in the client and — under E2E — no public keys (asymmetry between DIRECT and GROUP).
- `FriendsService.acceptFriendRequest` (:91-123): `getOrCreateDirectChat` → `notifyChatUpdate` (**ChatChange first**) → FRIENDSHIP_ACCEPTED system message into that chat. `removeFriend`/`blockUser`/`unblockUser` → bump `membershipVersion` + `notifyChatUpdate` (today `removeFriend` notifies nobody).

### 3.2 Messages
- Requests (`MessageController.kt:26,40-41,58-68,84-94,109-119`): `receiverId` + `groupMessage` → `chatId` (`validateObjectId`).
- `canUserAccessMessage` (:823-864) becomes:
  - send: `canSend(sender, chatId)`;
  - read-type access (media fetch, sync, backfill): `isMember` AND visible-to-member;
  - mutations (react, vote, edit, delete, setread): `canSend` AND visible-to-member.
  - **visible-to-member** = `historyVisibility == FULL` OR `message.createdVersion > member.joinedAtVersion`. Uses the **immutable** `createdVersion`, never `version` (a reaction or read receipt on a pre-join message would otherwise leak it).
- Send (:113-250): set `createdVersion = version`; validate `answerId` is in the same `chatId` and visible to the sender.
- Edit (:527-556): **add `senderId == userId` and `!deleted` checks** (F1).
- `setMessagesRead` (:682-777): `/messages/setread?chatId=&timestamp=`, criteria `chatId == c AND readers.userId != reader AND visible-to-member`. One batched socket frame instead of N `notifyMessageUpdate` calls (F16).
- `SystemMessageService`: every event → `chatId`, `senderId = null`, actor in `systemEvent`. Callers: `GroupService` (→ ChatService), `EventService.kt:227-238,310`, `FriendsService.kt:117`, `WakeService.kt:111,176`.
- Account deletion (`MessageLookupService.deleteAllUserMessages :125-140`): soft-delete + version bump (tombstones) instead of hard delete (F8).

### 3.3 Sync and backfill
`getMessagesSince`:

```
chatId IN myChatIds AND version > since AND version <= watermark
  AND (historyVisibility == FULL OR createdVersion > member.joinedAtVersion)   -- per chat
```

`fullHistoryGroups` is **removed from the version stream**. Confirmed bug today (F10): for a group joined after the cursor with > 400 messages below the cursor, page 1 is the 400 lowest replayed rows, `newVersion < since`, `moreMessages = true`, and every following page returns the same 400 rows ⇒ the client's `while` loop never terminates (also on a fresh install for such groups). Smaller history: no loop, but duplicate re-downloads until the cursor passes `joinedAtVersion`.

Backfill replaces it: `GET /chats/{id}/messages?beforeVersion=&limit=` ordered by `createdVersion` desc, filtered by visible-to-member.
- Trigger: a chat appears in `/chats/sync` (new chat, or `joinedAtVersion` newer than the stored one ⇒ rejoin).
- Bound: the client records `upperBound = current sync cursor` at the moment the membership first appears, backfills `createdVersion <= upperBound` page by page, and persists progress in `ChatDto.backfillDoneBeforeVersion`. Everything above `upperBound` arrives via the normal version stream. Survives app kills and partial backfills; rejoin resets progress.
- Also gives "load older messages" for free later.

Removed members: no membership ⇒ no sync, no backfill, no access by id. (Today they keep seeing their own sent group messages via `senderId == u`.) Locally the chat becomes `membershipState = LEFT`: history stays readable, composer disabled, no new messages (D8). Re-adding resets it to ACTIVE and triggers a backfill.

### 3.4 Notifications
- `MessageNotificationResponse`: drop `receiverId`, `groupMessage`, `groupId`; add `chatId`, `chatType`, `chatName` (group name, null for DIRECT). Same for reaction and wake notifications.
- **Truncate `content`** to a preview (e.g. 500 chars) — FCM *and* APNs data payloads are limited to 4 KB, content is up to 10 000 chars today (F19).
- `NotificationService.notifyMessageUpdate` (:60-163): one path over `getMembers(chatId)`, `toMessageResponse(member)` per recipient (fixes F11 at :125). Also deliver to the changer's *other* connections (F12). `notifySystemMessage` (:176-193), `notifyReactionAdded` (:426-464), `notifyGroupUpdate` (:345-357) → chat-based.
- Socket: `GroupChange` → `ChatChange(chat: ChatResponse)`; `EventChange.event.groupId` → `chatId`.
- `FirebaseService.notificationResponseToDataMap` (:400-426), `ApnsService.buildPayload` (:315-380): new field set; keep both `_class` and `type`, client standardises on `type`. Fix `requesterId` (`FirebaseService.kt:234`, `ApnsService.kt:223`, F14).
- Respect `chatmembers.muted` before sending FCM/APNs (D6).

### 3.5 Other server touchpoints
| Area | Change |
|---|---|
| Events | `Event.groupId` → `chatId` (`eventmodel/Event.kt:22`, ObjectId, same value), `EventResponse.kt:11,41`, `EventJoinResponse.kt:6` (`GroupResponse` → `ChatResponse`), `repository/EventRepository.kt:9` (`findByGroupId` → `findByChatId`), `EventsLookupService.kt:55`, `EventService.kt:41-42,127-183,225-275,290-373` via `ChatService`. Event join adds **non-friends** to a GROUP chat — keep explicit (exception to the friendship rule for adds). |
| Wake | `WakeController.kt:21-48` `WakeRequest(chatId)`; `WakeService.kt:60-233` — DIRECT target = partner. |
| Recap | `RecapService.kt:87-107, 219-232, 288-301, 357-362, 385-396, 425-428` + `RecapResponse.kt:60,94,116` on `chatId`/chat type. Content stats must exclude encrypted messages in E2E Phase 0. |
| Media | Keep files where they are. New writes use `message_image_{msgId}.png` / `message_audio_{msgId}.m4a`; reads try the new name, then fall back to both legacy names (`ImageManager.kt:88-90`, `AudioManager.kt:42-45`). No rename migration ⇒ no collisions, no partial-rename state. |
| Pinned chats | `PinnedChat(chatId, pinTimePoint)`; `UserController.kt:230-256` rejects entries that are not chats the user is a member of (protects against an old client overwriting the migrated list with user ids during the iOS review window). |
| Account deletion | `UserService.kt:578-607`: leave every chat through `ChatService` (admin promotion, empty-group deletion, notifications, `membershipVersion`) instead of `GroupLookupService.leaveAllGroups :111-115`; delete *all* friendships of the user, not only ACCEPTED (`UserService.kt:588`), with the correct settings id (F13). |
| `core/MainController.kt` | Remove/rewrite group-based code: injections (:58-65), `migrateGroupDeletedFlag` (:372), `migrateDetachEventsFromDeletedGroups` (:456-484 — would detach every event once `groups` is gone), `migrateTypeAliases` group entries (:516), `printAllGroups` (~:930). Older message migrations that read `Message::class` (e.g. `migrateMessageVersions :339`) must run **before** the chat migration or be deleted — they cannot map documents in the new shape. |

### 3.6 Min-version gate — dropped (D5)
No server-enforced gate in this release. Old clients fail with errors until updated; the existing optional update hints stay (desktop GitHub check, `iosApp/iosApp/AppUpdateChecker.swift`). Revisit before E2E Phase 2b.

### 3.7 Stale server docs/skills (update after implementation)
`.claude/skills/schneaggchatv3server/SKILL.md` + `references/persistence-and-entities.md`, `references/routes.md`, `.claude/skills/schneaggchat-add-sync-endpoint/SKILL.md`, `.claude/skills/schneaggchat-flow-stability/references/known-flows.md`, `docs/domain-model.html`, `README.md`, `SECURITY_AUDIT.md`. Dangling reference `MessageController.kt:166` → `docs/CLIENT_SYNC_MIGRATION.md` (does not exist).

---

## 4. Client changes (`SchneaggchatV3`)

### 4.1 Data layer
- **Room 87 → 88** (§2.3). `GroupDto`/`GroupMemberDto` → `ChatDto`/`ChatMemberDto`; `GroupWithMembersDto` → `ChatWithMembersDto`; `ChatAggregateDto` (:9) keyed on `chatId` only.
- **Daos** (`C/datasource/database/Daos.kt`) — every chat query `WHERE chatId = :chatId`:
  - `getChatAggregatesFlow` (:166-182), `getLastMessagePerChatFlow` (:192-207): `GROUP BY chatId`, no CASE.
  - `getMessagesByUserIdFlow` (:223-241) → `getMessagesByChatIdFlow`, single arm.
  - :247-341 (`getMessageCountSince`, `getOldestUnreadSendDate`, `getMessagesByUserId`, image/type/count/link flows).
  - `getUnreadChatCountFlow` (:385-401): join `chats`; DIRECT arm keeps the "partner is ACCEPTED friend" rule (D1 hidden); LEFT chats excluded.
  - `deleteMessagesForGroup` (:357), `deleteReadersForGroupMessages` (:423), `getCommonGroupsWithMembers` (:476-485) → chat-based.
- **Repositories**: `chat/data/GroupRepository.kt:27-72` → `ChatRepository` (incl. `getDirectChatIdForUser(userId): String?` via `partnerUserId`); `MessageRepository.kt:79-125` chat-scoped wrappers take `chatId` only.
- **AppRepository** (`C/datasource/AppRepository.kt`):
  - `sendMessage(chatId, …)` (:1738-1974); send-echo (:1919-1969) maps `chatId`.
  - `sendOfflineMessages` (:1986-2070) uses `m.chatId`.
  - `messageIdSync` (:2089-2188): plain version stream; new `chatBackfill(chatId)` per §3.3.
  - `applyPushMessage` (:2198-2239), `applyPushReaction` (:2242): `chatId` from payload.
  - `setAllChatMessagesRead(chatId, ts)` (:2305).
  - `groupIdSync` (:2526-2593) → `chatIdSync` (`/chats/sync`); `leftChats` → `membershipState = LEFT` (keep messages, D8); `deletedChats` → purge (replaces `dataSync` :593 and `GroupRepository.deleteGroup :50-57`).
  - `getChatListFlow`: DIRECT chats only while friendship ACCEPTED (D1); LEFT GROUP chats shown read-only.
  - `getChatListFlow` (:1018-1081): chats + aggregates; DIRECT rows join partner `UserDto`.
  - `getMessageSearchFlow` (:1105-1159): chat from `message.chatId`.
  - `chatKey()` (:2933-2937) deleted.
  - `createGroup` (:2471) → `/chats/create`; `sendWake(chatId, reason)` (:2769); `setPinnedChat` (:2841-2848).
  - **Unknown chatId** in a socket frame / push / sync row → trigger `chatIdSync` before handling (covers the accept-ordering race, §3.1).
- `MessageResponseMapper.toDomainMessage` (`C/datasource/network/requestResponseDataClasses/MessageResponseMapper.kt:9-45`): `chatId`, `createdVersion`, nullable `senderId`.
- **Domain**: `Message` loses `receiverId`, `groupMessage`, `isGroupMessage()`, `isThisChatMessage()`; gains `chatId`. UI reads "is group" from the chat (`RepliedMessagePreview.kt:71`, `MessageContent.kt:63`, `UserButton.kt:265`, `MessageDisplayMapper.kt:48`). `Group` → `Chat`; `ChatListItem.isGroup` → `type` + `partnerUserId`; `MessageSearchResult.kt:12`.
- **NetworkUtils**: message DTOs (:856-996), `/messages/setread` (:1147), `/groups/*` (:723-838) → `/chats/*`, `WakeRequest` (:1353), `PersonalUserSettings.pinnedChats` (:529, :556). Remove content `println`s (:1022, :1121).
- **Socket** (`C/datasource/network/socket/SocketConnectionMessage.kt`): `MessageChange` handler (:181-256) uses `message.chatId`; `GroupChange` (:390-424) → `ChatChange`.
- **Preferences**: `PinnedChat(chatId, pinTimePoint)`, `Draft(chatId, text)` (`Preferencemanager.kt`, `SettingsRepository.kt:27-36`); local drafts dropped on upgrade.
- **Events**: `events/data/dtos/EventDto.kt:14,22` (Room index on `groupId`), `EventNetworkDtos.kt:19`, `EventResponseMapper.kt`, `events/data/EventRepository.kt:29-30`, `events/domain/Event.kt` → `chatId`.
- **Profile pictures**: `PictureManager.kt:33` + android/ios/jvm actuals keep the `gruppe` path for GROUP chats; DIRECT chats render the partner's user picture.

### 4.2 Presentation & navigation
- `Route.Chat(chatId, highlightMessageId)`, `Route.ChatDetails(chatId)`, `Route.ChatSharedContent(chatId, …)`; `App.kt:610-636`; Koin params `di/Modules.kt:209-243`.
- Chat screen: `ChatViewModel` (:87-88, 196, 208, 231-235, 290-363, 723, 788-799, 826-830, 861-880), `ChatScreen.kt:28-59`, `ChatState.kt`, `uielements/ChatTopBar.kt:54,68`. Reply targets (:728-735) scoped to `chatId`.
- Details: `ChatDetailsViewmodel` (:118-508), `ChatDetails.kt` (`isGroup` branches → `chat.type`), `chatdetails/Composables.kt:115-119`, `SharedContentViewModel`, `SharedContentScreen.kt`.
- Chat list: `ChatSelectorViewModel` (:151-386), `ChatSelector.kt:761`, `MessageChatSelector.kt:218` (list keys → `chatId`).
- New chat / group creator: `NewChatViewModel.kt:131-145`, `newchat/GroupCreator.kt`, `GroupCreatorViewModel.kt:147,258`.
- App shell: `OpenChatTracker.OpenChat(chatId)`, `AppLifecycleManager.NotificationOpenData(chatId)` (:88-109), `GlobalViewModel` (:269-271).
- User-centric entry points resolve `userId → chatId` via `getDirectChatIdForUser` (null ⇒ "not friends" hint): `SchneaggmapViewModel.kt:349`, `ComboAnnotationSources.kt:135`, `BirthdaysViewModel.kt:74`, `EventsViewModel.kt:356`.
- Events UI: `EventsViewModel.kt:289,300`, `EventEditPopup.kt:822-847`, `EventsScreen.kt`, `EventsState.kt`, `EventJoinPopup.kt:215-224,324`.
- Preview fixtures with `receiverId`: `MessageViewWithActions.kt:174-375`, `PollPreviewData.kt`.

### 4.3 Notifications
- `PayloadDecoder.kt`: decode on `type`; `DecodedNotification.Message(chatId, chatType, chatName, …)`; drop `receiverId`, `groupId`, `groupMessage`, `chatTargetId`.
- `MessageNotificationMapper.kt:15-47`, `NotificationContentResolver.kt:38-76`: `chatId` direct; notification id = `chatId.hashCode()` (no user/group id collision anymore).
- Android: `ReplyReceiver`, `MarkAsReadReceiver` (drop `EXTRA_GROUP_CHAT`), `Notifier.android.kt:70,281-348`, `AppFirebaseMessagingService.kt:37-126`, `androidApp/.../MainActivity.kt:100-102`. **Notifications posted before the upgrade** carry old user ids: on open, if the id is not a known chat, try `getDirectChatIdForUser(id)`, else open the chat list. Cancel all message notifications once on first start after upgrade.
- iOS: `NotificationPayload.swift` (`chatType`, `chatName`; add SYSTEM to `MessageType`), `NotificationService.swift:85-157` (fallback title uses `groupMessage` at :90-105), `IosPushDelegateBridge.kt:25-80`.
- Local mute check (D6). Desktop `Notifier.jvm.kt:53`: stop logging bodies.

### 4.4 Tests to touch
`composeApp/src/jvmTest/.../chat/ChatLoadingBenchmark.kt`. No new tests unless requested.

---

## 5. Server data migration (one shot, before serving)

### 5.1 Execution rules
- **Runs before the web server accepts requests.** `ApplicationReadyEvent` (today's hook) fires after Tomcat starts ⇒ a new client could sync mid-migration, advance its cursor past messages that do not have a `chatId` yet and never receive them (no version bump). Run in a `SmartInitializingSingleton` / `@PostConstruct` migration bean, or block all non-health requests with 503 until the marker document exists.
- **Raw `Document`s only** (`mongoTemplate.getCollection(...)`), never `Message::class`/`Group::class` — the Kotlin models are already in the new shape.
- **Index pre-step**: `auto-index-creation=true` builds indexes during context init, i.e. before the migration. Drop `senderId_version_idx`, `receiverId_version_idx`, `receiverId` single-field index in the same pre-context step (or name new indexes so no key/name conflict arises). Keep `sender_client_message_idx` identical.
- **Idempotent + resumable**: marker document `migrations{_id: "chat_based_v1", step}`; every step re-runnable (upserts, `chatId: {$exists: false}` guards). A crash mid-run resumes on restart.
- **Dry-run on a mongodump copy** first; log counts per step (orphans, self-DMs, deleted-user pairs); backup before deploy.

### 5.2 Steps
1. **Groups → chats**: upsert `chats{_id: group._id, type: GROUP, name, description, profilePicUpdatedAt, expiresAt, creatorId, deleted, createdAt, updatedAt, membershipVersion: 0, historyVisibility: FULL}` (FULL preserves today's behaviour for existing groups).
2. **groupmembers → chatmembers**: upsert by `{chatId, userId}`; `userid` → `userId`, `admin` → `role`, keep `joinedAt`, `joinedAtVersion`, `color`.
3. **DIRECT chats**: candidate pairs = ACCEPTED (and BLOCKED, so the chat survives an unblock) friendships ∪ aggregate `$group` over `messages{groupMessage: false}` on `{$min: [senderId, receiverId]}, {$max: …}` (Mongo `distinct` cannot do pairs). Skip pairs where `a == b` (self-DM; count + log, leave those messages unmigrated → step 7), and pairs where either user no longer exists. Upsert `chats{type: DIRECT, pairKey}` + two `chatmembers`. Keep `pairKey → chatId` map. PENDING/DECLINED pairs get no chat (created lazily at accept).
4. **Group messages** (one pipeline update): `updateMany({groupMessage: true, chatId: {$exists: false}}, [{$set: {chatId: "$receiverId", createdVersion: "$version"}}, {$unset: ["receiverId", "groupMessage"]}])`.
5. **Group SYSTEM sender**: `updateMany({msgType: "SYSTEM", chatId: {$exists: true}, senderId: <is the chatId>}, {$set: {senderId: null}})` — keyed on `msgType == SYSTEM`; older rows that carry an actor id get `senderId = null` too (actor is in `systemEvent.actorId`). Same for direct SYSTEM messages after step 6.
6. **Direct messages**: per pair, `updateMany({senderId: {$in: [a, b]}, receiverId: {$in: [a, b]}, groupMessage: false, chatId: {$exists: false}}, …$set chatId, createdVersion: $version, $unset…)` — uses the `{senderId}` index; no in-memory loading of messages.
7. **Orphans**: group messages whose group doc is missing, direct messages of skipped pairs → count, log, then delete (or move to `messages_orphaned`). Messages + media of **already-deleted groups** (`deleted = true`) are purged too (D7 applied retroactively).
8. **Pinned chats**: per user (raw docs), convert only entries that still have a `group` field: `group == false` → `pairKey(self, chatId)` lookup, drop if no DIRECT chat; `group == true` → keep id if the chat exists and the user is a member, else drop. Bump `user.updatedAt`.
9. **Events**: rename `groupId` → `chatId` (`$rename`); events whose group was deleted keep `chatId = null`.
10. **Done**: write marker `done`. Drop `groups` / `groupmembers` in a **later** release (rollback source).

Client: nothing to migrate — Room bump wipes and re-syncs; drafts dropped; pinned chats come back from server settings.

---

## 6. Release & rollout

1. Server backup → deploy (migration runs before serving) → smoke test with the new client build.
2. Publish Android / iOS / desktop at the same time. iOS review delay = window where old iOS clients cannot sync/send; submit iOS first and deploy the server on approval.
3. Changelog (user-facing): chats re-sync once after the update; drafts are cleared.

Effort: ~60 client files / ~25 server files (incl. events/recap/wake), mostly mechanical; roughly ¼–⅓ of the E2E V2 effort. Riskiest parts: migration §5 (pairing, ordering before serving, index pre-step), sync/backfill redesign (§3.3), notification payload on three platforms.

---

## 7. Design flaws found during analysis (fix in this release unless noted)

Security / authorization:
- **F1** Edit has no sender check and no deleted check (`S/message/MessageService.kt:527-556`; also via `/send/text` with `messageId`, `MessageController.kt:33-35`). Any chat member can rewrite anyone's message. → §3.2.
- **F2** New group members can fetch pre-join messages/media by id. → §3.2 visible-to-member (immutable `createdVersion`).
- **F3** Unfriended/blocked users keep full DM mutation access (react, vote, edit, delete). → `canSend` for mutations.
- **F4** `answerId` not validated against the chat. → §3.2.
- **F5** Group ADD_USER has no friendship and no user-exists check (`GroupService.kt:379-411`). → require friendship with the adder (event joins explicitly excepted).
- **F6** Any member may change group name/description/picture (`GroupService.kt:205-289`) — **kept by decision D3**. Exception: the `historyVisibility` toggle is admin-only (D10).
- **F7** `UserService.changeUserProfile` lets a friend overwrite another user's global `userDescription` (`UserService.kt:334-383`). Separate fix.

Consistency:
- **F8** Account deletion hard-deletes messages (no tombstones), leaves groups without admin promotion/notifications, and only deletes ACCEPTED friendships (`UserService.kt:578-607, 588`). → §3.5.
- **F9** Group deletion keeps messages/media on the server (`GroupService.deleteGroup :589-625`) while clients wipe locally. → D7: `ChatService.deleteChat` soft-deletes all messages (version bump ⇒ tombstones; or rely on `deletedChats` and hard-delete after a grace period) and purges message images/audio (new + legacy file names).
- **F10** Sync infinite loop (confirmed by query analysis): group joined after the cursor with > 400 pre-cursor messages ⇒ the same page forever; smaller histories re-download repeatedly. In-loop cursor regression is transient (persisted cursor = Room `MAX(version)`). → §3.3.
- **F11** DM socket push builds the response for `message.receiverId` instead of the recipient (`NotificationService.kt:125`); bites when the receiver reacts/votes/reads (sender sees wrong poll state, missing `clientMessageId`). → §3.4.
- **F12** The changer's own other devices get no socket update for their own changes. → §3.4.
- **F13** `deleteFriendship` deletes `FriendshipSetting` by the friendship id (wrong id) — settings leak (`FriendsService.kt:197`, `UserService.kt:592`).
- **F14** Friend-request push sets `requesterId` to the receiving user (`FirebaseService.kt:234`, `ApnsService.kt:223`). Harmless today (client never reads it), fix anyway.
- **F15** `isUserInGroup` ignores `Group.deleted` (`GroupLookupService.kt:83-86`). → `isMember`.
- **F16** Read receipts emit one socket frame per message (`MessageService.kt:682-777`) and bump the version of every read message.
- **F17** `notisMuted` / `FriendshipSetting.muted` are dead fields — **D6**.
- **F18** ~~Push discriminator mismatch~~ — verified fine: payload carries both `_class` and `type`. Standardise on `type` (§4.3).
- **F19** FCM and APNs payloads copy full content (up to 10 000 chars) — both have a 4 KB limit. → truncate (§3.4).
- **F20** `VersionCounterService.safeWatermark` relies on an in-memory in-flight set — single server instance only. Note, no fix now.
- **F24** Startup migrations run after the web server accepts traffic (`ApplicationReadyEvent`). Fine for past migrations, unsafe for this one. → §5.1.
- **F25** `/users/sync` only syncs users with a Friendship doc; event-group co-members and ex-friends are invisible (no name/picture) and would have no reachable E2E keys. → §3.1.

Privacy (trivial, fix now):
- **F21** Content logged: client `NetworkUtils.kt:1022, 1121` (`println`), `Message.toString` (`C/chat/domain/Message.kt:102-115`), desktop `Notifier.jvm.kt:53`; server `WakeService.kt:227-233` (wake reasons in DB log).
- **F22** Push content is plaintext today. Until E2E lands Google/Apple see message text; consider generic-text pushes in the meantime. E2E V2 Phase 2 must put the envelope (or nothing) into push.
- **F23** Server-side content processing E2E will break: recap content stats, poll rendering, image re-encoding (`ImageManager.kt:72-111`). Text-only E2E hits only recap.

---

## 8. Impact on `E2E_ENCRYPTION_PLAN_V2.md` — one mechanism for every chat

After this migration V2 has **no DM-specific code path**.

### 8.1 Unified primitives
- Session scope: `(chatId, senderId, sessionId)`. Session export binds `chatId` + `membershipVersion`, IK-signed.
- Share recipients = `members(chatId)` incl. self (own other devices). DIRECT = 2 members, GROUP = N; same code.
- Send gate: `canSend(chatId)` AND all members have published keys.
- Self-healing (V2 Phase 3.4) runs for every chat: session's recorded recipient set vs. current members.
- User/key reachability: every co-member's public keys arrive via `/users/sync` scoped by shared chat membership (§3.1) — no friend-only assumption.

### 8.2 One rotation trigger + server guard
- Client rotates its outbound session when `chat.membershipVersion` ≠ the version stored on the session (plus V2's 500 messages / 7 days).
- **Server guard**: encrypted sends carry `membershipVersion`; the server rejects a stale value with `409`, the client syncs the chat, rotates, re-sends. Closes the race "message encrypted for a member removed a second ago" for both chat types.

### 8.3 One server authz rule for keys
`POST /keys/shares` items `{chatId, recipientId, sessionId, blob}`; server checks `isMember(sender, chatId) && isMember(recipient, chatId)`. Replaces V2 Phase 0.3's "friends or co-members".

### 8.4 One history rule (`chats.historyVisibility`)
- `FULL`: senders share sessions at index 0 to new members (requires the initial ratchet value per session in the sender's key backup — V2 "simpler alternative").
- `SINCE_JOIN`: share at the current index + rotate; server enforces via `createdVersion > joinedAtVersion` on sync, backfill and by-id access (§3.2/§3.3).
- DIRECT chats never gain members ⇒ the flag only matters for GROUP, but it is still one code path. **D2** sets the default.

### 8.5 Stays per user
Identity key, signed prekey, key backup, safety numbers, key-change warnings — per **account**. Safety numbers reachable from DIRECT chat details and from each member row in GROUP chat details (event groups contain non-friends; TOFU applies).

### 8.6 Edits V2 needed (applied to `E2E_ENCRYPTION_PLAN_V2.md` on 2026-10-10)
- Merge Phase 2 (1:1) and Phase 3 (groups) into one chat-generic phase; only the rollout gate (decrypt-only release, then sending) stays two-step.
- Phase 3.2 assumes new members get the full backlog — align with D2/§8.4 (done: SINCE_JOIN default, FULL toggle).
- Phase 0.3 authz → §8.3; Phase 0.4 key fields on `UserResponse` must also reach non-friend co-members (§3.1).
- DB is v87 (V2 says 79); after this migration 88, V2 bumps to 89.
- Socket fan-out bug already fixed (`SocketConnectionHandler.kt:117`); remove from V2 future work.
- Global push key already removed (F22) — Phase 0.6 becomes "envelope (if ≤ 4 KB) into `content`, else generic + fetch".
- `AppRepository` line references shifted (send :1738, send-echo :1919-1969, sync :2089-2188, socket handler :181-256). `MessageResponseMapper.toDomainMessage` still must become `suspend`. Send-echo, edit, mapper take `content` from the server (`AppRepository.kt:1933, 2355`, mapper :19) — keep local plaintext under E2E.

---

## 9. Decisions (answered 2026-10-10)

| # | Question | Decision |
|---|---|---|
| D1 | DIRECT chat with a non-friend (removed/blocked) | **Hidden, as today.** Messages stay locally and on the server; chat reappears on re-friend. Sending blocked server-side (`canSend`). |
| D2 | Default `historyVisibility` for new groups | **`SINCE_JOIN`**, with an optional group-settings toggle to `FULL`. Migrated groups start as `FULL`. Toggle only affects future joiners; under E2E, `FULL` only covers sessions created while the toggle was on (earlier sessions already discarded `R_0`). |
| D3 | Group metadata edits | **Any member, as today.** |
| D4 | DM → group conversion | **Not supported**; "add person" from a DM creates a new GROUP chat. |
| D5 | Min-version gate in this release | **No.** |
| D6 | Mute | **Implement per chat member** (`chatmembers.muted`, private; server skips push, client skips local notification). |
| D7 | Group deletion | **Delete messages + media on the server** (tombstones via `deletedChats`, purge media). |
| D8 | Removed/left group member | **Keep history read-only** on the device (`membershipState = LEFT`); wipe only on group deletion. |
| D9 | Self-DM legacy messages (if the dry-run finds any) | **Delete as orphans** (§5.2 step 7). |
| D10 | Who may flip the `historyVisibility` toggle? | **Admins only** (`/chats/{id}/sethistoryvisibility`, admin check), although other metadata stays any-member (D3). |
