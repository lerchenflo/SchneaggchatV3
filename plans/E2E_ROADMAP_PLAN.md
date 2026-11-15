# E2E Roadmap Plan (client)

One file for everything that leads to end-to-end encryption. Written 2026-11-12, reviewed and restructured 2026-11-13 (section 5 lists what the review changed and why). All code references re-verified 2026-11-12 (client `main` at `ca7d0ee7`, server branch `beta` at `3b7b349`).

Replaces and deletes: `E2E_ENCRYPTION_PLAN.md` (v1), `E2E_ENCRYPTION_PLAN_V2.md`, `CHAT_BASED_MIGRATION_PLAN.md`, `MESSAGE_SEND_SYNC_REBUILD_PLAN.md`, `OFFLINE_FIRST_ANALYSIS.md`. Their text is in git history (last commit that has them: `ca7d0ee7`).

Server side: `SchneaggchatV3server/docs/E2E_ROADMAP_SERVER_PLAN.md`. This file is the client side plus the shared design.

Paths: `C/` = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/`, `A/` = `composeApp/src/androidMain/kotlin/org/lerchenflo/schneaggchatv3mp/`, `I/` = `composeApp/src/iosMain/kotlin/org/lerchenflo/schneaggchatv3mp/`, `T/` = `composeApp/src/jvmTest/kotlin/org/lerchenflo/schneaggchatv3mp/`, `S/` = `SchneaggchatV3server/src/main/kotlin/com/lerchenflo/schneaggchatv3server/`.

---

## 0. Order (fixed, do not reorder)

| Stage | What | Repos | Ships alone | Depends on |
|---|---|---|---|---|
| **1a** | The message sync cursor becomes a stored value of its own (today it is `MAX(version)` over the message table) | client | yes | - |
| **1b** | One inbound message handler: every message from the server is decoded, merged and stored in one class, with one merge rule. One codec interface is the seam for the message text | client | yes | 1a |
| **1c** | One message notifier: socket and push decide in one place whether a message shows a notification | client | yes | 1b |
| **2a** | The client uses one `chatId` everywhere inside the app. Server and wire format stay as they are today; one small file translates | client | yes | 1b, 1c |
| **2b** | Wire switch: `chatId` on the wire, `chats` collection, DIRECT chats become real chats, backfill | client + server, one release | no (both at once) | 2a |
| **2c** | Chat features that need 2b: history visibility toggle, read-only chat after leaving | client | yes | 2b |
| **3** | End-to-end encryption (Phase 0 server, Phase 1 login key and keys, Phase 2R receive, Phase 2S send, Phase 3 verification) | client + server | each phase | 2b |

Why this order (user, 2026-11-12): after stage 1 the migration and the encryption each touch one place per direction instead of ten. Stage 2 must be done before stage 3 because the crypto layer needs one member list and one `membershipVersion` per chat (section 3.9).

Why the steps are cut like this (review 2026-11-13, details in section 5): every step that can be built and released without the other repo is its own step, so each one can be compiled, started and tested against today's server. Only 2b needs both repos at once, and after 2a it is small.

Rules for the agent that executes a stage:

1. Do one stage (or one step of it) per session. Do not start a stage before the one above it is merged.
2. Before writing any new class, function or mapper: run the `code-reuse-scout` agent with the names listed in that stage (project rule in `CLAUDE.md`).
3. Follow the architecture skills named in `CLAUDE.md` (`android-data-layer`, `android-di-koin`, `android-navigation`, `android-presentation-mvi`).
4. After every step: run the compile check `./gradlew :composeApp:compileKotlinJvm` (only if the machine is not a laptop, see `CLAUDE.md`). At the end of every stage also run `:composeApp:compileAndroidMain` and `:composeApp:compileKotlinIosSimulatorArm64`. After every step the app must compile and work; if a step cannot be finished that way, stop and ask the user.
5. Tests: the project rule is "no tests and no test runs unless the user asks". At the start of each stage ask the user once (AskUserQuestion) whether the test classes listed for that stage may be run before the first step (baseline) and after the last step. Stage 3 Phase 1 is the exception: it is not done without its tests (section 4.5).
6. Line numbers in this file are from 2026-11-12. If a line number does not match, find the place by the function name given next to it. Never guess.
7. If a step is unclear or the code does not look like this file says: stop and ask the user. Do not pick a default.
8. Server work is never done from this repo. The server agent works from the server plan file; the wire contract in section 2.2 is what both sides follow.
9. Every finished step is a good commit point. Tell the user; commit only when the user asks.
10. Every database version bump is destructive (`C/datasource/database/CreateDatabase.kt:12`): the client loads everything again and messages that are still queued unsent are lost. This is true for every bump today. The bumps of this plan are in 1a, 2a (two), 2b and stage 3 Phase 1; the open decision about the unsent queue is in `plans/Todo.md`.

---

## 1. Stage 1: stored sync cursor, one inbound handler, one notifier (client only)

No server change. One database version bump (1a). Nothing changes that the user sees, except the fixes marked **behaviour change**.

### 1.1 Where a server message enters the client today (verified 2026-11-12)

A `MessageResponse` (`C/datasource/network/dto/MessageNetworkDtos.kt:7`) or a push payload is read at these places. Each one copies `content` on its own. With E2E each one would need its own decrypt call.

| # | Entry | File and line | What it does today |
|---|---|---|---|
| 1 | Sync page | `C/chat/data/OfflineFirstMessageRepository.kt:770-801` (`syncMessages`) | Batch lookup by id, fallback by `clientMessageId`, `toDomainMessage(version = response.version)`, one `upsertMessages` |
| 2 | Socket `MessageChange` | `C/datasource/network/socket/SocketConnectionMessage.kt:241-267` | Single lookup by id, fallback `clientMessageId`, `toDomainMessage(version = existing?.version ?: 0)`, delete / skip older poll / upsert |
| 3 | Send response | `OfflineFirstMessageRepository.kt:553-594` (`sendMessageForResult`) | Builds a `Message` by hand from the response (no reactions, no systemEvent), forces `localPK` and `clientMessageId` |
| 4 | Edit response | `OfflineFirstMessageRepository.kt:234-243` (`editMessage`) | Copies `content`, `changeDate`, `edited` onto the stored row |
| 5 | Stuck-edit retry | `OfflineFirstMessageRepository.kt:261-266` (`retryStuckEdit`) | Same as 4 plus `sent = true` |
| 6 | Reaction response | `OfflineFirstMessageRepository.kt:286-297` (`reactToMessage`) | Copies `reactions`, `changeDate` |
| 7 | Poll responses (vote, delete option, edit poll) | `OfflineFirstMessageRepository.kt:324-335` (`storePollResponse`) | Copies `poll`, `changeDate`, `edited`, skips if stored row is newer |
| 8 | Push, provisional row | `OfflineFirstMessageRepository.kt:848-889` (`applyPushMessage`), called from `A/utilities/notifications/AppFirebaseMessagingService.kt:95` and `I/utilities/notifications/IosPushDelegateBridge.kt:37, :58` | Builds a TEXT row from `DecodedNotification.Message.content` |
| 9 | Push, notification text | `C/utilities/notifications/NotificationContentResolver.kt:55-91` (`resolveMessage`), fallback `:241-260` (`resolveFallbackContent`) | Uses `decoded.content` as the notification body |
| 10 | Socket, notification text | `C/utilities/notifications/MessageNotificationMapper.kt:15-49` (`Message.toNotificationContent`), fallback `SocketConnectionMessage.kt:701-721` | Uses `message.content` after entry 2 stored it |
| 11 | iOS Notification Service Extension | `iosApp/SchneaggchatNotificationServiceExtension/NotificationService.swift` | Swift, own process, reads the payload itself. Not part of stage 1; under E2E it shows a generic text (section 4.6) |

The only mapper is `MessageResponse.toDomainMessage` (`C/chat/data/remote/dto/MessageResponseMapper.kt:9-47`); entries 1 and 2 use it, entries 3-7 do not.

Text leaves the client at three places (needed for step 1b.8): send TEXT `OfflineFirstMessageRepository.kt:488-495`, edit `:226-229`, stuck-edit retry `:256`. The image caption (`:499-506`) stays plaintext in E2E v2.

### 1.2 Stage 1a: the sync cursor becomes a stored value

Today the cursor of `/messages/sync` is not stored. It is computed as `MAX(version)` over the message table (`C/datasource/database/Daos.kt:357-358`, read at `OfflineFirstMessageRepository.kt:745`). Three problems follow from that:

- Every writer outside the sync must store a wrong `version` on purpose so it does not move the cursor (`OfflineFirstMessageRepository.kt:589-591`, `:886`, `SocketConnectionMessage.kt:255-258`). So `version` cannot be used to see which of two copies of a message is newer.
- A sync page that holds only deletions moves nothing: the envelope's `newVersion` is used inside the loop (`:811-814`) but never stored, so the next sync fetches the same deletions again.
- Stage 2b loads older messages of a chat (backfill). An old message can carry a high `version` (somebody reacted to it yesterday). Stored as it is, it would move `MAX(version)` past messages of other chats that were never fetched: silent message loss.

After 1a the cursor is one row in its own table, written in the same transaction as the page it belongs to.

Run `code-reuse-scout` first with: `SyncCursorEntity` (one stored sync cursor per stream), `MessageLocalDataSource.getMessageSyncCursor` (read the stored cursor), `MessageLocalDataSource.applySyncPage` (store one sync page and its cursor in one transaction).

- [ ] **1a.1** Run `code-reuse-scout` (list above).
- [ ] **1a.2** New entity `C/chat/data/local/entity/SyncCursorEntity.kt`: `@Entity(tableName = "sync_cursors") data class SyncCursorEntity(@PrimaryKey val name: String, val version: Long)`. One row is used: `name = "messages"` (constant `MESSAGE_SYNC_CURSOR` in the same file).
- [ ] **1a.3** `Daos.kt`, in `MessageDao`: `@Query("SELECT version FROM sync_cursors WHERE name = :name") suspend fun getSyncCursor(name: String): Long?` and `@Upsert suspend fun upsertSyncCursor(cursor: SyncCursorEntity)`. In `AllDatabaseDao`: `@Query("DELETE FROM sync_cursors") suspend fun clearSyncCursors()` and call it in `clearAll()` (`Daos.kt:554`). **This call is mandatory**: a cursor that survives a cleared message table makes the client skip its whole history. Register the entity in `Database.kt:24`, version 93 -> 94.
- [ ] **1a.4** `MessageLocalDataSource` + `RoomMessageDataSource`: add `suspend fun getMessageSyncCursor(): Long` (0 when there is no row) and `suspend fun applySyncPage(deletedIds: List<String>, messages: List<Message>, newCursor: Long)`. `applySyncPage` runs in **one** transaction (`database.useWriterConnection { it.immediateTransaction { ... } }`, same pattern as `RoomMessageDataSource.kt:143-156`): delete the ids, upsert the messages, upsert the cursor. Remove `getLastSyncedMessageVersion` from the DAO, the interface and the implementation.
- [ ] **1a.5** `syncMessages` (`OfflineFirstMessageRepository.kt:743-819`): `since = messageLocalDataSource.getMessageSyncCursor()`; per page one `applySyncPage(deletedMessages, <mapped messages>, messageSyncResponse.data.newVersion)` instead of `deleteMessages` (`:766`) plus `upsertMessages` (`:788`). The loop line `since = newVersion` (`:814`) stays.
- [ ] **1a.6** Remove `Index(value = ["version"])` from `MessageEntity` (it only served `MAX(version)`). Do not touch the writers that store `existing?.version ?: 0L`; stage 1b changes them.
- [ ] **1a.7** Fix the comments that explain `MAX(version)`: `OfflineFirstMessageRepository.kt:589-590`, `:721-724`, `:763-765`, `:843-846`, `SocketConnectionMessage.kt:255-257`.
- [ ] **1a.8** Check: `grep -rn "getLastSyncedMessageVersion\|MAX(version)" composeApp/src` prints nothing. Tests when the user allows it: `MessageSendTest`, `MessageSendEchoDedupTest`, `OfflineMessageQueueTest`, `MessagePushApplyTest`, `DefaultDataSyncCoordinatorTest`.
- [ ] **1a.9** Manual check by the user: fresh login loads all chats; kill the app in the middle of the first sync and start it again, the sync continues and no chat is missing; logout and login as another user loads that user's messages.

User-facing: one full reload of all data after the update (as with every database version bump).

### 1.3 Stage 1b: `MessageContentCodec` and `InboundMessageProcessor`

Two new files in `C/chat/data/`.

**`MessageContentCodec.kt`**: the seam between message transport and message text. Stage 1 binds the pass-through implementation; stage 3 binds the encrypting one. Nothing else in the client reads `MessageResponse.content` or writes a text to the server.

```kotlin
/** The only place message text is turned into what the server sees, and back. E2E replaces the binding (plans/E2E_ROADMAP_PLAN.md section 4). */
interface MessageContentCodec {
    /** Text to store for a server message. [stored] is the row the client has now, null for a new message. */
    suspend fun decode(response: MessageResponse, stored: Message?): String

    /** Text of a pushed message. Null when it cannot be read yet; the sync brings the message then. */
    suspend fun decodePush(push: DecodedNotification.Message): String?

    /** What is sent to the server as `content` for [text]. Called by send, edit and the stuck-edit retry. */
    suspend fun encode(text: String, receiverId: String, isGroup: Boolean): String
}

/** Plain text in, plain text out. */
class PlainMessageContentCodec : MessageContentCodec {
    override suspend fun decode(response: MessageResponse, stored: Message?): String = response.content
    override suspend fun decodePush(push: DecodedNotification.Message): String? = push.content.ifBlank { null }
    override suspend fun encode(text: String, receiverId: String, isGroup: Boolean): String = text
}
```

Stage 2a changes `encode` to take `chatId`. Stage 3 widens the return types (section 4.5, P1.2).

**`InboundMessageProcessor.kt`**: the only class that turns a `MessageResponse` or a message push into a stored `Message`. No interface (one implementation; tests use the real class on the in-memory database, as `T/testutil/SendPathFixture.kt` does for the repository).

```kotlin
/**
 * @param message the message as it is stored now (or as it would be, when [applied] is false)
 * @param previous the stored message before this change, null when there was none
 * @param applied false when nothing was written (the stored copy is as new or newer, or there was no row to update)
 */
data class InboundResult(val message: Message, val previous: Message?, val applied: Boolean)

class InboundMessageProcessor(
    private val messageLocalDataSource: MessageLocalDataSource,
    private val codec: MessageContentCodec,
    private val logger: Logger,
) {
    /** One page of /messages/sync: deletions, updates and the new cursor in one transaction. */
    suspend fun applySyncPage(page: MessageSyncResponse, ownId: String): List<InboundResult>

    /** A message that arrived on the socket. */
    suspend fun applySocket(response: MessageResponse, ownId: String): InboundResult

    /** The server's answer to a send. [pendingLocalPK] is the row that was sent. */
    suspend fun applySendResponse(response: MessageResponse, ownId: String, pendingLocalPK: Long): InboundResult

    /** The server's answer to an edit, a reaction or a poll change. Never creates a row. */
    suspend fun applyWriteResponse(response: MessageResponse, ownId: String): InboundResult

    suspend fun delete(messageIds: List<String>)

    /** Stores a provisional row for a pushed TEXT message, or applies a pushed reaction. */
    suspend fun applyPush(push: DecodedNotification.Message)
}
```

**One merge rule for every caller** (private function, all four `apply*` functions use it):

1. Hold `writeLock` (a `Mutex` in the processor) from the lookup to the write. No network call inside the lock. This makes "read the stored row, decide, write" one step for sync, socket and HTTP responses; today they interleave.
2. Find the stored row: by `messageId`; if there is none, by `response.clientMessageId` (only own messages carry one); `applySendResponse` then falls back to the row with `pendingLocalPK`. `applySyncPage` does the first two lookups for the whole page with the two batch queries (`getMessageEntitiesByIds`, `getMessageEntitiesByClientMessageIds`).
3. **Stale check**: a stored row with `stored.version >= response.version` already holds this server state or a newer one. Write nothing, return `applied = false`. The server gives every change of a message a new, higher `version` (`S/message/MessageService.kt`, every write runs in `versionCounterService.withVersion`), and every `MessageResponse` carries it (`S/message/messagemodel/MessageMapper.kt:23`). Rows that never came from the server (pending send, provisional push row) have `version = 0` and always lose.
4. No stored row: `applyWriteResponse` writes nothing (`applied = false`): the row was deleted in the meantime and an answer to an own edit must not bring it back. The other three insert.
5. Build the message with `MessageResponse.toDomainMessage`: `existingLocalPK`, `existingPictureUrl`, `existingAudioPath`, `existingClientMessageId` from the stored row (as today), `version = response.version` (**the real one, for every caller**), `content = codec.decode(response, stored)`.
6. Write: `applySyncPage` calls `messageLocalDataSource.applySyncPage` (stage 1a) once for the page; the others call `upsertMessage`.

`toDomainMessage` gets one new parameter `content: String = this.content`, so the processor passes the decoded text and nothing else builds a `Message` from a `MessageResponse`.

**Behaviour changes** (all are fixes; tell the user in the summary):
- The stale check uses `version` for every caller. Before, only poll updates were protected, by `lastChanged`; a late socket frame or HTTP answer could put an older reaction or edit state back, and no later sync repaired it.
- Socket, send and write answers store the real `version` (possible since 1a).
- Send, edit, reaction and poll answers store every field of the response, not only the two or three fields they copied before.
- Lookup and write are one locked step.

Run `code-reuse-scout` first with: `MessageContentCodec.decode` / `decodePush` / `encode` (seam for message text), `InboundMessageProcessor.applySyncPage` / `applySocket` / `applySendResponse` / `applyWriteResponse` (merge server messages into Room), `InboundMessageProcessor.applyPush` (provisional row from a push), `InboundMessageProcessor.delete`, `SocketMessageChangeHandler.handle` (socket branch for a changed message).

Steps:

- [ ] **1b.1** Run `code-reuse-scout` (list above).
- [ ] **1b.2** Add parameter `content: String = this.content` to `toDomainMessage` (`MessageResponseMapper.kt:9`) and use it at `:20`.
- [ ] **1b.3** Create `MessageContentCodec.kt` and `InboundMessageProcessor.kt` as specified. `previous` in the result is the stored row mapped with `toMessage()` (`C/chat/data/local/entity/MessageEntityMapper.kt`).
- [ ] **1b.4** Koin (`C/di/Modules.kt`, next to `RoomMessageDataSource` `:244`): `single<MessageContentCodec> { PlainMessageContentCodec() }` and `singleOf(::InboundMessageProcessor)`. Add both as constructor parameters of `OfflineFirstMessageRepository` (`Modules.kt:247-248`, class at `OfflineFirstMessageRepository.kt:85-94`).
- [ ] **1b.5** Replace the six repository entries of section 1.1. After each one the compile check must pass.
  - Entry 1, `syncMessages` `:759-814`: per page `val results = inbound.applySyncPage(messageSyncResponse.data, ownId)` (it sorts the updates by `version`, as today's `:770`). Keep the media id collection (`:803-809`), now over `results.filter { it.applied }`, and the loop line `since = newVersion`. Remove the lookup and mapping code `:766-801`.
  - Entry 3, send response `:558-594`: `inbound.applySendResponse(serverrequest.data, ownId, localpkintern)`. Keep the media download and the `unsent_` file cleanup `:596-603` (they run whether or not `applied` is true). Keep `withContext(ioDispatcher)`.
  - Entry 4, `editMessage` `:234-243`: `inbound.applyWriteResponse(response, ownId)`. `ownId` comes from `SessionCache.requireLoggedIn()?.userId` as in `syncMessages:738`.
  - Entry 5, `retryStuckEdit` `:260-271`: on success call `applyWriteResponse`. If the result is `applied = false`, still write `existing.copy(sent = true)`, otherwise the row blocks the offline queue forever.
  - Entry 6, `reactToMessage` `:286-297`: `applyWriteResponse`.
  - Entry 7, `storePollResponse` `:324-335`: body becomes `inbound.applyWriteResponse(response, ownId)`.
- [ ] **1b.6** Push row (entry 8): move the body of `applyPushMessage` (`:848-889`) and `applyPushReaction` (`:892-903`) into `InboundMessageProcessor.applyPush`. The text of the row comes from `codec.decodePush(push)`; null means no row. `MessageRepository.applyPushMessage` stays in the interface and only delegates, so the three callers and `T/chat/data/MessagePushApplyTest.kt` stay unchanged.
- [ ] **1b.7** Socket (entry 2): new file `C/chat/data/SocketMessageChangeHandler.kt`, class `SocketMessageChangeHandler(inbound: InboundMessageProcessor, messageRepository: MessageRepository, socketConnectionManager: SocketConnectionManager, ...)` with `suspend fun handle(change: SocketConnectionMessage.MessageChange, ownId: String)`. Move the whole branch `SocketConnectionMessage.kt:241-350` into it, in this order (the order matters, see the comments at `:273` and `:317`):
  1. `deleted` -> `inbound.delete(listOf(id))`, else `inbound.applySocket(change.message, ownId)`.
  2. New message, not SYSTEM: notification (today's code `:272-315`; replaced by the notifier in 1c).
  3. New message, not deleted: send `MessageAck` (`:320-324`). Must run even if step 2 threw.
  4. New message, not SYSTEM: media download (`:327-336`).
  5. Not new, not deleted, `applied` and `previous != null`: reaction notification (`notifyNewReactions`, `:646-698`, moves along).
  The branch in `handleSocketConnectionMessage` becomes one line: `KoinPlatform.getKoin().get<SocketMessageChangeHandler>().handle(socketMessage, ownId)` (this function resolves all its dependencies that way, `:223-233`). Register the handler in `Modules.kt`. `handleSocketConnectionMessage` no longer needs `MessageLocalDataSource` (`:225`).
- [ ] **1b.8** Outbound: use `codec.encode(text, empfaenger, gruppe)` at the three places where text leaves the client: send TEXT (`:492`), edit (`:228`), stuck-edit retry (`:256`; it needs the row's `receiverId` and `groupMessage`). The image caption (`:503`) does not go through it.
- [ ] **1b.9** Fixtures (edit only): `T/testutil/SendPathFixture.kt` builds the repository; pass a `PlainMessageContentCodec` and an `InboundMessageProcessor` on the same local data source.
- [ ] **1b.10** Check: `grep -rn "toDomainMessage" composeApp/src` shows only the mapper file and `InboundMessageProcessor.kt`. `grep -rn "\.content" C/chat/data/OfflineFirstMessageRepository.kt` shows no `response.content` / `serverrequest.data.content` / `result.data.content` any more. `grep -rn "existing?.version" composeApp/src` prints nothing.
- [ ] **1b.11** Tests when the user allows it: the classes of 1a.8 must pass unchanged. If the user allows new tests: one test class that binds a marking codec (`decode` returns `"dec(" + content + ")"`, `encode` returns `"enc(" + text + ")"`) and asserts for each of the six entries and the push row that the stored text went through `decode`, and for send, edit and stuck-edit retry that the request text went through `encode`. This test proves the seam that stage 3 depends on.
- [ ] **1b.12** Manual check by the user, on two devices of one account plus one other account: send, edit, react, vote in a poll, delete; each change shows on all three devices without restart. Send while offline, then go online. Receive with the app closed (push), open the chat.

### 1.4 Stage 1c: `MessageNotifier`

Today two code paths decide about a message notification and they differ:

| | Socket path (`SocketConnectionMessage.kt:272-315`, `:646-721`) | Android push path (`AppFirebaseMessagingService.kt:125-166`) |
|---|---|---|
| Sender and group name | from Room (`:278-282`) | from the payload (`senderName`, `groupName`) |
| Open-chat check | `OpenChatTracker.isSeen`, then a launched `isSeenAfterConfirmDelay` (`:290-305`) | blocking `isSeenAfterConfirmDelay` (`:152-153`) |
| Already handled check | none | "message is already stored" (`:136-141`) |
| Fallback | `showFallbackMessageNotification` (`:701-721`) | `resolveFallbackContent` (`NotificationContentResolver.kt:241`) |
| Reactions | diff of stored vs new (`notifyNewReactions`) | `resolveReaction` (`NotificationContentResolver.kt:93`) |

**New file** `C/chat/data/MessageNotifier.kt` (it reads the user and group local data sources, so it belongs to the chat data layer). Run `code-reuse-scout` first with: `MessageNotifier.onSocketMessage`, `MessageNotifier.onSocketReactions`, `MessageNotifier.onPush`, `IncomingMessageNotification` (one model both paths map into).

```kotlin
/** What both paths know about a message that may need a notification. */
data class IncomingMessageNotification(
    val msgId: String, val chatId: String, val isGroup: Boolean,
    val senderId: String, val senderName: String, val groupName: String?,
    val messageType: MessageType, val text: String, val sendDate: Long, val reaction: Boolean,
)

class MessageNotifier(userLocalDataSource, groupLocalDataSource, codec: MessageContentCodec, applicationScope, logger) {
    /** Socket: returns at once, the open-chat confirm delay runs in the app scope. */
    suspend fun onSocketMessage(message: Message)
    suspend fun onSocketReactions(previous: Message, current: Message, ownId: String)
    /** Push: returns after the notification is posted or skipped (the push service blocks on it). */
    suspend fun onPush(decoded: DecodedNotification.Message)
}
```

One private function does the work for both: resolve names (payload value first, Room second), open-chat check (`awaitSeenCheck` true for push, false for socket), profile picture (`ensureProfilePictureForNotification`), build `NotificationContent` (reuse `Message.toNotificationContent` and `reactionNotificationContent`), show, and on any exception show the plain fallback. The push text comes from `codec.decodePush(decoded)`; when it is null the notification shows the generic "new message" text.

Steps:

- [ ] **1c.1** Run `code-reuse-scout`.
- [ ] **1c.2** Create `MessageNotifier.kt`, move the socket code (`SocketMessageChangeHandler` steps 2 and 5 from 1b.7, the old `:272-315`, `:646-721`) into `onSocketMessage` / `onSocketReactions`. Register in `Modules.kt`.
- [ ] **1c.3** Push: `AppFirebaseMessagingService.showNotification` (`:125-166`): for `DecodedNotification.Message` call `messageNotifier.onPush(decoded)`; every other type keeps today's code. `resolveMessage` and `resolveReaction` in `NotificationContentResolver.kt` are then only called from the notifier; move them or make them internal to it. Keep `resolveLocalizedContent` for the other types. Keep the `handled` flag logic (`:79-85`): `onPush` must not throw after it posted.
- [ ] **1c.4** Fallback: one fallback builder for both paths (keep `resolveFallbackContent`, delete `showFallbackMessageNotification`).
- [ ] **1c.5** **Ask the user before building this step.** Replace the "already stored" skip (`AppFirebaseMessagingService.kt:136-141`) by an in-memory registry of message ids this process already notified, claimed by both paths inside `MessageNotifier`. This is the open item in `plans/Todo.md` ("A push is skipped when its message is already stored"); the user wanted it done together with the server notification refactor (server `docs/UNIFIED_NOTIFICATIONS_PLAN.md`, built on `beta`). If the user says no, keep the stored-check inside `onPush` unchanged.
- [ ] **1c.6** Check: `grep -rn "showLocalNotification\|NotificationManager.showNotification(message" composeApp/src` shows no message notification outside `MessageNotifier.kt` (other types still use it). iOS foreground and drain (`IosPushDelegateBridge.kt:28-64`) only store rows; iOS notifications are shown by the system, no change.

Result of stage 1: message text passes through `MessageContentCodec` in both directions (`decode`, `decodePush`, `encode`), every server message is stored by `InboundMessageProcessor`, every message notification is decided in `MessageNotifier`, and the sync cursor is a stored value. Stages 2 and 3 name these places.

---

## 2. Shared design for stage 2 (chat-based addressing)

Migrates both repos from `Message.receiverId` (user id or group id) + `groupMessage` to `Message.chatId` + a `chats` collection with type `DIRECT | GROUP` and an explicit member list. The wire switch is one release for client and server (phase 2b), no backwards compatibility, no dual-format endpoints. Before it, phase 2a moves the client's own code to `chatId` on today's wire format (section 3.0).

Old versions: not supported on any release (user, 2026-11-05, server `Todo.md`): Play forces the update on Android, iOS shows an update popup. No server min-version gate is added (D5).

### 2.1 Target data model

Server collections (the server plan has the full list with indexes):

```
chats        _id (GROUP: old group id unchanged. DIRECT: new id), type DIRECT|GROUP,
             pairKey (DIRECT only, "<smallerHex>:<largerHex>", unique),
             name, description, profilePicUpdatedAt, expiresAt, creatorId (GROUP only),
             createdAt, updatedAt, deleted, membershipVersion (Long), historyVisibility FULL|SINCE_JOIN
chatmembers  chatId, userId, joinedAt, joinedAtVersion, role MEMBER|ADMIN, color, muted (private)
messages     chatId, senderId (null = SYSTEM message), createdVersion (immutable), ... ; receiverId and groupMessage removed
```

Rules baked in:
- GROUP chat id = old group id. Events, group pictures and group extensions keep their keys.
- DIRECT chats are created only by the server (at friendship accept and by the migration), found by `pairKey`. The client never creates one.
- All SYSTEM messages have `senderId = null`; the actor is in `systemEvent.actorId`.
- DIRECT chat and friendship: the chat outlives the friendship. Removed / declined / blocked: chat stays, sending is refused by the server (`canSend`), the chat is hidden in the list (D1) and comes back on re-friend with the same id. `membershipVersion` is bumped on these changes too.

Client Room (database `C/datasource/database/Database.kt:26`, version 93 before this plan; destructive migration, the client re-syncs):

```
ChatEntity (replaces GroupEntity, table "chats")
  id, type, partnerUserId? (DIRECT: the other member),
  name?, profilePictureUrl?, description?, expiresAt?, createDate, updatedAt, profilePicUpdatedAt,
  membershipVersion, historyVisibility, muted (own flag),
  membershipState ACTIVE|LEFT   (LEFT = removed or left: read-only, history kept, D8)
  backfillDoneBeforeVersion Long?
ChatMemberEntity (replaces GroupMemberEntity, table "chat_members")  unique {chatId, userId}, index {userId}
  chatId, userId, joinDate, admin, color, memberName
MessageEntity
  + chatId (indexed), + createdVersion, senderId nullable; - receiverId, - groupMessage
  indexes: {chatId, sendDate}; {readByMe, myMessage, chatId, msgType, sent, sendDate}; id, sent, clientMessageId unchanged (the `version` index is removed in stage 1a)
```

`UserEntity.notisMuted` moves to `ChatEntity.muted`. `UserEntity` also holds non-friend co-members (the server sends them through `/users/sync`, section 2.2).

### 2.2 Wire contract (names the client sends and reads)

These names are the proposal. The server agent builds its side and replies with the final contract; the client follows the reply.

| Call | Change |
|---|---|
| `MessageRequest`, `ImageMessageRequest`, `AudioMessageRequest`, `PollMessageRequest` (`C/chat/data/remote/dto/MessageNetworkDtos.kt:9-52`) | `receiverId` + `groupMessage` -> `chatId` |
| `MessageResponse` (`C/datasource/network/dto/MessageNetworkDtos.kt:7-30`) | `receiverId` + `groupMessage` -> `chatId`; `senderId` nullable; new `createdVersion: Long` |
| `POST /messages/setread` | `chatId`, `timestamp` (no `group`) |
| `POST /chats/sync` (replaces `/groups/sync`) | request: list of `{id, timeStamp}` as today; response: `updatedChats: List<ChatResponse>`, `leftChats: List<String>`, `deletedChats: List<String>` |
| `ChatResponse` (replaces `GroupResponse`, `C/datasource/network/dto/GroupNetworkDtos.kt`) | `id, type, name?, description?, updatedAt, profilePicUpdatedAt, createdAt, creatorId?, expiresAt?, membershipVersion, historyVisibility, ownMuted, members: [{userId, joinedAt, role, color, memberName}]` |
| `/chats/create`, `/chats/{id}/profilepic`, `/chats/{id}/setname`, `setdescription`, `setprofilepic`, `setexpiry`, `setmuted`, `sethistoryvisibility`, `/chats/changemembers`, `/chats/delete` | replace `/groups/*` |
| `GET /chats/{id}/messages?beforeVersion=&limit=` | new: backfill of older messages of one chat, newest first by `createdVersion` |
| `/users/sync` | also returns users who share a chat with me (as the simple user shape) |
| Wake request | `chatId` instead of user id / group id |
| `PinnedChat` in user settings (`C/datasource/network/dto/SettingsNetworkDtos.kt`) | `{chatId, pinTimePoint}` (no `group`) |
| Socket `groupchange` | -> `chatchange` `{chat: ChatResponse, deleted}` |
| `EventResponse.groupId` | -> `chatId` |
| Push `_class = message` (`C/utilities/notifications/PayloadDecoder.kt:99-113`) | drop `receiverId`, `groupMessage`, `groupId`, `groupName`; add `chatId`, `chatType`, `chatName` (empty for DIRECT). Same for the wake push (`groupId`, `groupName` -> `chatId`, `chatName`) |

Group extensions keep `ownerType = GROUP` and `ownerId` (D11); `ownerId` is the chat id.

### 2.3 Sync and backfill

- `/messages/sync` becomes a plain version stream over my chats. The server no longer replays the whole history of a newly joined group inside this stream (today's cause of an endless sync loop for groups with more than 400 older messages, server flaw F10).
- A chat that appears in `/chats/sync` for the first time (new chat, or rejoined) is backfilled: the client reads `upperBound` = the stored message sync cursor while it holds `messageSyncLock` (so no sync page is in flight), stores it, then pages `GET /chats/{id}/messages` for `createdVersion <= upperBound` and saves progress in `ChatEntity.backfillDoneBeforeVersion`. Survives app kills. Everything newer arrives through the normal stream. Backfilled messages are stored with their real `version`; this is safe only because the sync cursor is a stored value of its own since stage 1a. Why nothing is missed: a message with `createdVersion <= upperBound` comes from the backfill; every other message has `version >= createdVersion > upperBound` and comes from the stream.
- No backfill after an empty database: if the message sync cursor was 0 when this data sync started, the stream itself delivers the whole history of every chat. Chats first seen in such a run get `backfillDoneBeforeVersion = 0` (nothing to do).
- Removed or left member: no sync, no backfill, no access by id. Locally the chat becomes `membershipState = LEFT`.
- Visibility for a member: `historyVisibility == FULL` or `message.createdVersion > member.joinedAtVersion` (server rule).
- A message whose `chatId` is not known locally (socket, push or sync row): run the chat sync first, then handle the message. This check lives in the merge rule of `InboundMessageProcessor` (one place, thanks to stage 1).

### 2.4 Decisions

| # | Question | Decision |
|---|---|---|
| D1 | DIRECT chat with a non-friend (removed/blocked) | Hidden, as today. Messages stay; chat reappears on re-friend. Sending blocked by the server. (2026-10-10) |
| D2 | Default `historyVisibility` for new groups | `SINCE_JOIN`, optional group-settings toggle to `FULL`. Migrated groups start as `FULL`. (2026-10-10) |
| D3 | Group metadata edits | Any member, as today. (2026-10-10) |
| D4 | DM to group conversion | Not supported; "add person" from a DM creates a new GROUP chat. (2026-10-10) |
| D5 | Server min-version gate | No. Old versions are unsupported anyway (2026-11-05). |
| D6 | Mute | Per chat member (`chatmembers.muted`, private). Server skips push, client skips the local notification. (2026-10-10) |
| D7 | Group deletion | Server deletes messages and media. (2026-10-10) |
| D8 | Removed/left group member | History stays read-only on the device (`LEFT`); wiped only on group deletion. (2026-10-10) |
| D9 | Self-DM legacy messages | Deleted as orphans by the server migration. (2026-10-10) |
| D10 | Who may flip `historyVisibility` | Admins only. (2026-10-10) |
| D11 | Group extensions | Keep `ownerType GROUP` + `ownerId`; `ownerId` equals the chat id, no data change. The reserved `FRIENDSHIP` type later means the DIRECT chat id. (2026-11-12) |
| D12 | E2E key granularity | Per account, although the server has a device registry now. (2026-11-12) |
| D13 | Order | Pipeline first, then chat-based addressing, then E2E (user, 2026-11-12). Cut into 1a-1c and 2a-2c by the review of 2026-11-13, section 0. |

---

## 3. Stage 2: client steps

Stage 2 runs in three phases: 2a, 2b, 2c. Sections 3.1-3.7 ("Block A-G") are the inventory of every file that changes and what it looks like at the end of 2b. Section 3.0 says which part of each block is done in which phase.

How to find every place: after stage 2b these commands must print nothing outside `games/` (the counts are from 2026-11-12, 91 files):

```bash
grep -rEn "receiverId|groupMessage|isGroupMessage|isThisChatMessage|chatTargetId|EXTRA_GROUP_CHAT" composeApp/src androidApp/src iosApp --include="*.kt" --include="*.swift"
grep -rEn "\bgruppe\b|\bempfaenger\b|isGroup\b|groupChat\b" composeApp/src androidApp/src --include="*.kt"
grep -rEn "GroupEntity|GroupMemberEntity|GroupWithMembers|GroupResponse|GroupLocalDataSource|RoomGroupDataSource|/groups/" composeApp/src --include="*.kt"
```

### 3.0 Phases

Why two phases for one migration (review 2026-11-13): done in one go, the client does not compile from the first edit to the last (about 90 files), and it cannot be started until the new server exists. In 2a the whole client moves to `chatId` while server and wire stay as they are, so every step compiles, runs against today's server and can be released. 2b is then a small change at the edge of the client.

#### Phase 2a: `chatId` inside the client (client only, today's server)

Target of 2a: `receiverId`, `groupMessage`, `gruppe`, `empfaenger` and `isGroup` exist only in the wire DTOs, the DTO mappers, the `Ktor*RemoteDataSource` implementations, `PayloadDecoder`, the Room group tables and the new file below. Everything else uses `chatId` and `Chat.type`.

New file `C/chat/domain/LegacyChatAddressing.kt` (pure functions, deleted in 2b):

```kotlin
/** Chat ids while the server still addresses by receiver id + group flag. Deleted in stage 2b. */
const val LEGACY_DIRECT_PREFIX = "direct-"

/** A GROUP chat id is the group id. A DIRECT chat id is "direct-<partner user id>". */
fun legacyDirectChatId(partnerUserId: String): String = LEGACY_DIRECT_PREFIX + partnerUserId

fun legacyChatId(senderId: String, receiverId: String, groupMessage: Boolean, ownId: String): String =
    if (groupMessage) receiverId else legacyDirectChatId(if (senderId == ownId) receiverId else senderId)

data class LegacyTarget(val receiverId: String, val groupMessage: Boolean)

fun legacyTarget(chatId: String): LegacyTarget =
    if (chatId.startsWith(LEGACY_DIRECT_PREFIX)) LegacyTarget(chatId.removePrefix(LEGACY_DIRECT_PREFIX), groupMessage = false)
    else LegacyTarget(chatId, groupMessage = true)
```

Why the prefix: in 2b a DIRECT chat gets a real id that is not the partner's user id. With the prefix, code that uses a chat id as a user id fails already in 2a (the user is not found), while today's server is still there to test against. The partner of a DIRECT chat is always read from `Chat.partnerUserId`, never from the id. `legacyTarget` is called only in the files named in the target of 2a.

In 2a a DIRECT chat is not stored. `ChatRepository` builds it from the partner's `User` (id `legacyDirectChatId(user.id)`, `type = DIRECT`, `partnerUserId = user.id`, name and picture of the user); a GROUP chat is built from the group tables as today. In 2b the same repository functions read the new `chats` table; their callers do not change.

Steps (each one compiles and runs against today's server; ask the user to try the named flows after each step):

- [ ] **2a.1 Renames only, no behaviour change**: `Group` -> `Chat` (new fields `type: ChatType = GROUP`, `partnerUserId: String? = null`), `GroupMember` -> `ChatMember`, `GroupMemberAction` -> `ChatMemberAction`, `GroupRepository` -> `ChatRepository` with the function names of Block A, `OfflineFirstGroupRepository` -> `OfflineFirstChatRepository`, `FakeGroupRepository` -> `FakeChatRepository`. Room entities, DAOs and DTOs keep their `Group*` names until 2b. Use the IDE-style rename (every caller changes with it).
- [ ] **2a.2 Add the new key next to the old one**: create `LegacyChatAddressing.kt`. `Message.chatId` and `MessageEntity.chatId` (index `{chatId, sendDate}`), filled in `MessageResponse.toDomainMessage`, in the four `MessageEntity(...)` builders of the send (`OfflineFirstMessageRepository.kt:387-466`) and in `InboundMessageProcessor.applyPush`. New functions next to the old ones, with the names of Blocks A and B: `getMessagesByChatIdFlow(chatId, limit)` and the other `chatId` queries in `MessageDao`, `MessageLocalDataSource`, `MessageRepository`; `ChatRepository.getChatByIdFlow` / `getChatById` for DIRECT ids (built from the user); `ChatRepository.getDirectChatIdForUser(userId)` (returns `legacyDirectChatId(userId)` when the user is stored); `ChatListItem.chatId`, `type`, `partnerUserId`. `MessageRepository.sendMessage(chatId, ...)` and the other write functions with `chatId` call `legacyTarget(chatId)` at the remote data source call. `MessageContentCodec.encode(text, chatId)`. Database version +1.
- [ ] **2a.3 Navigation and chat screen** (Block E, first two items): `Route.Chat(chatId, highlightMessageId)`, `OpenChatTracker`, `AppLifecycleManager`, `GlobalViewModel`, `ChatViewModel` and its screen files. All 17 `Route.Chat(` call sites change in this step; a caller that still holds `(id, isGroup)` builds the id with `if (isGroup) id else legacyDirectChatId(id)` for now. Try: open a DIRECT and a GROUP chat, send, receive, mark read, jump to a message.
- [ ] **2a.4 Details, shared content, chat list, new chat** (Block E, items three and four). Try: chat list order, unread counts, pinning, search, details of both chat types, creating a group.
- [ ] **2a.5 Other features that open or name a chat** (Block E, item five; `C/events/` and `C/groupextensions/` uses of the renamed classes).
- [ ] **2a.6 Notifications** (Block F without the payload change): `DecodedNotification.Message.chatId` is computed in `PayloadDecoder` with `legacyChatId` from the old payload fields and replaces `chatTargetId`; `MessageNotifier`, `MessageNotificationMapper`, `NotificationContent`, Android notifier and receivers, `MainActivity`, `IosPushDelegateBridge` use it. Notification id = `chatId.hashCode().absoluteValue`; cancel all message notifications once on the first start after the update. Try: push with the app closed, reply and mark-as-read from the notification, tap opens the right chat.
- [ ] **2a.7 Preferences**: `Draft(chatId, text)` (convert stored drafts once with `if (group) chatId else legacyDirectChatId(chatId)`); pinned chats are mapped at the settings DTO boundary (the server keeps `{chatId, group, pinTimePoint}` until 2b).
- [ ] **2a.8 Remove the old key**: delete `Message.receiverId`, `Message.groupMessage`, `isGroupMessage()`, `isThisChatMessage()`, the old DAO queries and repository functions with `gruppe`, the two old message indexes, every temporary `if (isGroup) ... legacyDirectChatId(...)` of 2a.3, and `content` from `Message.toString` (privacy flaw F21). `MessageEntity` drops `receiverId` and `groupMessage`; the offline queue gets its target from `legacyTarget(chatId)`. Tests and fakes (Block G). Database version +1.
- [ ] **2a.9 Check**: the first two greps above print matches only in the files named in the target of 2a. `grep -rn "legacyTarget(\|legacyDirectChatId(\|legacyChatId(" composeApp/src androidApp/src` prints only those files. All three compile checks pass.

2a is released on its own. User-facing: one full reload of all data.

#### Phase 2b: wire switch (client and server, one release)

Precondition: 2a is released. The server with its stage 2 runs on the test server (`BASE_SERVER_URL_TEST`, `C/constants.kt:5`) with a migrated copy of the production data, and the server agent has replied with the final contract for section 2.2. The client is built against that server, never against a server that does not exist yet.

- [ ] **2b.1 Network** (Block C): DTOs, `MessageResponseMapper` (`chatId`, `createdVersion`, nullable `senderId` come from the wire), remote data sources on `/chats/*`, socket `ChatChange`.
- [ ] **2b.2 Room** (Block B): `ChatEntity`, `ChatMemberEntity` replace the group tables; `MessageEntity.createdVersion`; `senderId` nullable. Database version +1.
- [ ] **2b.3 Repositories** (Block D): `syncChats` on `/chats/sync`, DIRECT chats come from the server, `backfillChat`, the unknown-chat rule in `InboundMessageProcessor`, mute through `/chats/{id}/setmuted` (the mute switch exists today as a local flag `notisMuted` on user and group). A chat I left is stored with `membershipState = LEFT` and is **not** shown in the chat list yet (same as today, where it disappears); 2c shows it.
- [ ] **2b.4 Notifications** (Block F, payload part): new push fields, iOS extension.
- [ ] **2b.5** Delete `LegacyChatAddressing.kt`. Drafts keyed by a `direct-` id are dropped. Cancel all message notifications once on the first start after the update.
- [ ] **2b.6 Check and release**: section 3.8.

#### Phase 2c: features on top (client only, after 2b is live)

- [ ] **2c.1** `historyVisibility` toggle for admins in the chat details (D10).
- [ ] **2c.2** A `LEFT` chat is shown in the chat list, read-only, with a disabled composer (D8).

New strings go to `values/`, `values-de/`, `values-it/` (never `values-de-rAT`).

### 3.1 Block A: domain models

- [ ] `C/chat/domain/Message.kt`: remove `receiverId` (`:43`), `groupMessage` (`:49`), `isGroupMessage()` (`:76`), `isThisChatMessage()` (`:83-98`); add `chatId: String`, `createdVersion: Long = 0`; `senderId: String?`. Remove `content` from `toString` (`:100-131`, privacy flaw F21).
- [ ] `C/chat/domain/Group.kt` -> `Chat.kt`: `Chat(id, type: ChatType, partnerUserId?, name?, ..., membershipVersion, historyVisibility, muted, membershipState, members)`. New enums `ChatType { DIRECT, GROUP }`, `HistoryVisibility { FULL, SINCE_JOIN }`, `MembershipState { ACTIVE, LEFT }`. `GroupMember` -> `ChatMember`, `GroupMemberAction` -> `ChatMemberAction`.
- [ ] `C/chat/domain/ChatListItem.kt`: `isGroup` -> `type` + `partnerUserId`. `ChatListSearch.kt`, `MessageSearchResult.kt`, `FriendRequestState.kt`: same.
- [ ] `C/chat/domain/GroupRepository.kt` (`:17-67`) -> `ChatRepository`: same functions renamed (`getAllChatsFlow`, `getChatByIdFlow`, `getChatById`, `getChatMembers`, `getCommonChats`, `syncChats`, `getProfilePicturesForChatIds`, `fetchMissingChatPictures`, `createGroup`, the `change*` functions, `deleteChat`), plus `suspend fun getDirectChatIdForUser(userId: String): String?`, `suspend fun setMuted(chatId, muted)`, `suspend fun setHistoryVisibility(chatId, visibility)`, `suspend fun backfillChat(chatId: String)`.
- [ ] `C/chat/domain/MessageRepository.kt`: every function with `(…, gruppe: Boolean)` loses that parameter and takes `chatId` (`:18-41`, `:104-125`, `:163`, `:174-179`); `getMessagesByUserIdFlow` -> `getMessagesByChatIdFlow`; `empfaenger` -> `chatId`; `deleteMessagesForGroups` -> `deleteMessagesForChats`.

### 3.2 Block B: Room

- [ ] `C/chat/data/local/entity/`: `GroupEntity.kt` -> `ChatEntity.kt`, `GroupMemberEntity.kt` -> `ChatMemberEntity.kt`, `relations/GroupWithMembers.kt` -> `ChatWithMembers.kt`, `GroupEntityMapper.kt` -> `ChatEntityMapper.kt` (fields in section 2.1). `MessageEntity.kt`: fields and the index list (`:15-37`) per section 2.1; rewrite the index comments. `MessageEntityMapper.kt`, `relations/MessageWithReaders.kt`, `ChatAggregate.kt`: `chatId`.
- [ ] `C/datasource/database/Database.kt:24-26`: entity list, version + 1. `C/datasource/database/Daos.kt`:
  - `getChatAggregatesFlow` (`:169-182`), `getLastMessagePerChatFlow` (`:195-210`): `GROUP BY chatId`, no CASE, no `ownId` parameter if it is no longer needed.
  - `getMessagesByUserIdFlow` (`:227-244`) -> `getMessagesByChatIdFlow(chatId, limit)`; `getMessageCountSince` (`:250`), `getOldestUnreadSendDate` (`:262`), `getMessagesByUserId` (`:273`), image/type/count/link flows (`:281-341`): `WHERE chatId = :chatId`.
  - `deleteMessagesForGroup` (`:360`) -> `deleteMessagesForChat`; `deleteReadersForGroupMessages` (`:431`) -> `deleteReadersForChatMessages`.
  - `getImageMessages` / `getAudioMessages` (`:373`, `:377`): `chatId IN (SELECT id FROM chats)`.
  - `getUnreadChatCountFlow` (`:393-406`): join `chats`; DIRECT chats count only while the partner is an ACCEPTED friend (D1); `LEFT` chats do not count.
  - `GroupDao` (`:437-493`) -> `ChatDao` on `chats` / `chat_members`; add `getDirectChatIdForUser(partnerUserId)`. `AllDatabaseDao` `clearGroups` / `clearGroupMembers` (`:531-535`) and `clearAll` (`:554`).
  - `EventDao.getEventFlowByGroupId` (`:632`) -> `getEventFlowByChatId`.
- [ ] `C/chat/data/local/`: `GroupLocalDataSource.kt` + `RoomGroupDataSource.kt` -> `ChatLocalDataSource` + `RoomChatDataSource`; `MessageLocalDataSource.kt` (`:40-61`, `:83-86`) + `RoomMessageDataSource.kt`: `chatId` signatures.
- [ ] Wiring checklist "New stored field" in `CLAUDE.md`: entity, both entity mappers, DTO mapper, a `TypeConverter` for the three new enums (`C/datasource/database/RoomTypeConverters.kt`), version bump.

### 3.3 Block C: network

- [ ] DTOs: `C/chat/data/remote/dto/MessageNetworkDtos.kt:9-52`, `C/datasource/network/dto/MessageNetworkDtos.kt:7-30`, `C/datasource/network/dto/GroupNetworkDtos.kt` -> `ChatNetworkDtos.kt` (`ChatResponse`, `ChatMemberResponse`), `C/chat/data/remote/dto/GroupNetworkDtos.kt` + `GroupResponseMapper.kt` -> chat versions, `C/chat/data/remote/dto/UserNetworkDtos.kt` (wake request), `C/datasource/network/dto/SettingsNetworkDtos.kt` (`PinnedChat`), `C/datasource/network/dto/EventNetworkDtos.kt` (`chatId`), `C/datasource/network/dto/NotificationNetworkDtos.kt`.
- [ ] `C/chat/data/remote/dto/MessageResponseMapper.kt:25-31`: `chatId`, `createdVersion`, nullable `senderId` (`myMessage = senderId == ownId`).
- [ ] `C/chat/data/remote/MessageRemoteDataSource.kt:21-44` + `KtorMessageRemoteDataSource.kt`: `chatId` instead of `empfaenger` + `gruppe`; `setMessagesRead(chatId, timeStamp)`; new `getChatMessages(chatId, beforeVersion, limit)`. Remove the two `println` of requests (`KtorMessageRemoteDataSource.kt:66, :138`, flaw F21).
- [ ] `C/chat/data/remote/GroupRemoteDataSource.kt` + `KtorGroupRemoteDataSource.kt` -> `ChatRemoteDataSource` + `KtorChatRemoteDataSource` on `/chats/*`. `UserRemoteDataSource.kt` + `KtorUserRemoteDataSource.kt`: wake by `chatId`.
- [ ] Socket: `SocketConnectionMessage.GroupChange` (`SocketConnectionMessage.kt:80-82`) -> `ChatChange` with `@SerialName("chatchange")`; handler branch (`:483-497`) upserts the chat, and for a chat that is new locally starts `chatRepository.backfillChat(id)`. Wiring checklist "New socket message" in `CLAUDE.md`: `subclass(...)` in `C/datasource/network/AppJson.kt`.

### 3.4 Block D: repositories

- [ ] `C/chat/data/InboundMessageProcessor.kt`: map `chatId`; before storing, if a `chatId` is unknown locally call `chatRepository.syncChats()` once per batch (section 2.3). `applyPush`: `chatId` from the payload (replaces `OfflineFirstMessageRepository.kt:876-877`).
- [ ] `C/chat/data/OfflineFirstMessageRepository.kt`: `sendMessage` / `sendVoiceMessage` / `sendMessageForResult` (`:349-611`) take `chatId`; the four `MessageEntity(...)` builders (`:387-466`) set `chatId`; `sendOfflineMessages` chat key (`:638`) = `queued.chatId`; `setAllChatMessagesRead(chatId, timestamp)` (`:922`), `markChatReadInBackground` (`:948`); read wrappers (`:98-123`); `deleteMessagesForChats` (`:206`).
- [ ] `C/chat/data/OfflineFirstGroupRepository.kt` -> `OfflineFirstChatRepository.kt`: `syncGroups` -> `syncChats` on `/chats/sync`: `updatedChats` upsert, `leftChats` -> `membershipState = LEFT` (keep messages), `deletedChats` -> delete chat, its messages, readers and its group extensions (`GroupExtensionDao.deleteForOwners`, `Daos.kt:652`). New `backfillChat` per section 2.3. `getDirectChatIdForUser`.
- [ ] `C/app/data/DefaultDataSyncCoordinator.kt`: group job (`:164-171`) -> chat job; it must finish before the message job starts using chat ids, or the unknown-chat rule above covers it. The purge after the jobs (`:293`) uses `deletedChats` only.
- [ ] `C/chat/data/OfflineFirstChatListRepository.kt`: `getChatListFlow` (`:76`) = chats + aggregates, DIRECT rows join the partner `UserEntity` and show only while ACCEPTED (D1), `LEFT` GROUP chats shown read-only (phase 2c; hidden in 2b); `getMessageSearchFlow` (`:164`) reads `message.chatId`; delete `chatKey()` (`:254`); `setPinnedChat(chatId, pinned)` (`:240`).
- [ ] `C/chat/data/OfflineFirstUserRepository.kt`: `sendWake(chatId, reason)`. `C/events/`: `groupId` -> `chatId` in `data/local/entity/EventEntity.kt`, `EventEntityMapper.kt`, `data/local/EventLocalDataSource.kt`, `RoomEventDataSource.kt`, `data/OfflineFirstEventRepository.kt`, `data/remote/dto/EventNetworkDtos.kt`, `EventResponseMapper.kt`, `domain/Event.kt`, `domain/EventRepository.kt`.
- [ ] `C/groupextensions/`: no key change (D11). Only replace uses of `Group` / `GroupRepository` by `Chat` / `ChatRepository` in `data/OfflineFirstGroupExtensionRepository.kt`, `presentation/GroupExtensionViewModel.kt`, `presentation/split/SplitViewModel.kt`, `SplitMembers.kt`, `editor/SplitExpenseEditorViewModel.kt`.
- [ ] `C/datasource/preferences/Preferencemanager.kt`: `PinnedChat(chatId, pinTimePoint)` (`:478`), `Draft(chatId, text)` (`:495-519`); drafts are converted in 2a.7, the direct ones are dropped in 2b.5. `C/settings/` wrappers follow.
- [ ] `C/utilities/PictureManager.kt` + `AndroidPictureManager.kt`, `IosPictureManager.kt`, `DesktopPictureManager.kt`: keep the group picture path for GROUP chats; a DIRECT chat shows the partner's user picture.
- [ ] `C/di/Modules.kt`: renamed classes (`:241`, `:244-248`), ViewModel parameters (`ChatViewModel` `:388`, `ChatDetailsViewmodel` `:408`, `SharedContentViewModel` `:471`).

### 3.5 Block E: navigation and presentation

- [ ] `C/app/navigation/Route.kt`: `Chat(chatId, highlightMessageId)` (`:18`), `ChatDetails(chatId)` (`:43`), `ChatSharedContent(chatId, …)` (`:47`). `C/app/App.kt` entries, `C/app/OpenChatTracker.kt` (`OpenChat(chatId)`, all four functions lose `isGroup`), `C/app/AppLifecycleManager.kt` (`notifyNotificationOpened(chatId, …)`), `C/app/GlobalViewModel.kt`.
- [ ] Chat screen: `C/chat/presentation/chat/ChatViewModel.kt`, `ChatScreen.kt`, `ChatState.kt`, `MessageDisplayMapper.kt`, `uielements/ChatTopBar.kt`, `messagecomposables/content/MessageContent.kt`, `messagecomposables/options/RepliedMessagePreview.kt`, `options/ReactionView.kt`, `C/sharedUi/buttons/UserButton.kt`: "is group" comes from `chat.type`. A `LEFT` chat disables the composer (phase 2c).
- [ ] Details: `chatdetails/ChatDetailsViewmodel.kt`, `ChatDetails.kt`, `Composables.kt`, `sharedcontent/SharedContentViewModel.kt`, `SharedContentScreen.kt`, `birthdays/BirthdaysViewModel.kt`. Mute goes through the server (D6, phase 2b). The `historyVisibility` toggle for admins (D10) is phase 2c. New strings go to `values/`, `values-de/`, `values-it/` (never `values-de-rAT`).
- [ ] Chat list and new chat: `chatselector/ChatSelectorViewModel.kt`, `ChatSelector.kt`, `MessageChatSelector.kt`, `newchat/NewChatViewModel.kt`, `GroupCreatorViewModel.kt`, `GroupCreator.kt`.
- [ ] Places that start from a user and need a chat: resolve with `getDirectChatIdForUser` (null = show the "not friends" hint): `C/schneaggmap/presentation/SchneaggmapViewModel.kt`, `C/events/presentation/EventsViewModel.kt`, `events/presentation/detail/EventDetailViewModel.kt`, `EventDetailScreen.kt`, `events/presentation/uielements/EventEditPopup.kt`, `EventsState.kt`, `birthdays/BirthdaysViewModel.kt`, and the mention / combo text sources (find with the grep above).
- [ ] Preview fixtures: `messagecomposables/MessageViewWithActions.kt`, `content/poll/PollPreviewData.kt`.

### 3.6 Block F: notifications

- [ ] `C/utilities/notifications/PayloadDecoder.kt`: `DecodedNotification.Message(msgId, senderName, messageType, chatId, chatType, chatName, content, senderId, reaction, sendDate, answerId)`; delete `chatTargetId` (`:30`); wake payload `chatId`, `chatName` (`:55-61`). Wiring checklist "New push type" in `CLAUDE.md` applies to the changed fields.
- [ ] `C/chat/data/MessageNotifier.kt`, `C/utilities/notifications/MessageNotificationMapper.kt` (`:16`, `:30`), `NotificationContentResolver.kt`, `NotificationContent.kt` (`groupChat` field), `NotificationPictures.kt`: `chatId` directly; notification id = `chatId.hashCode().absoluteValue`. Skip the notification when `ChatEntity.muted` (D6).
- [ ] Android: `A/utilities/notifications/AndroidNotifier.kt` (35 places), `ReplyReceiver.kt`, `MarkAsReadReceiver.kt` (drop `EXTRA_GROUP_CHAT`), `AppFirebaseMessagingService.kt`, `androidApp/src/main/kotlin/org/lerchenflo/androidApp/MainActivity.kt`. Notifications posted before the update carry old user ids: on open, if the id is no known chat try `getDirectChatIdForUser(id)`, else open the chat list. Cancel all message notifications once on the first start after the update.
- [ ] iOS: `iosApp/SchneaggchatNotificationServiceExtension/NotificationPayload.swift`, `NotificationService.swift` (title fallback uses `groupMessage`), `NotificationStrings.swift` if texts change; `I/utilities/notifications/IosPushDelegateBridge.kt:66-80`.

### 3.7 Block G: tests and fakes (edit only)

`composeApp/src/commonTest/.../fakes/FakeMessageRepository.kt` (34 places), `FakeChatListRepository.kt`, `FakeGroupRepository.kt` -> `FakeChatRepository.kt`, `FakeUserRepository.kt`, `FakeSettingsRepository.kt`, `FakePictureManager.kt`, `testutil/TestModels.kt`, `testutil/GlobalStateReset.kt`; ViewModel tests under `commonTest/.../chat/presentation/`; `T/testutil/SendPathFixture.kt`, `FakeMessageServer.kt`, `T/chat/data/*Test.kt`, `T/chat/ChatLoadingBenchmark.kt`, `T/chat/presentation/newchat/GroupCreatorViewModelTest.kt`.

### 3.8 Done criteria and release of stage 2b

- The three greps at the top of section 3 print nothing outside `games/`.
- Compile checks pass: `:composeApp:compileKotlinJvm`, `:composeApp:compileAndroidMain`, `:composeApp:compileKotlinIosSimulatorArm64`.
- Smoke test against the test server with migrated data: sync on a fresh install; send in a DIRECT and a GROUP chat; join a group with a long history (backfill, kill the app in the middle and start again); unfriend and re-friend (the same chat comes back); leave a group; push with the app closed on Android and iOS.
- Old client check: start the last released version (2a) against the migrated test server. It may show errors, but it must not crash in a loop, must not lose its login, and its update prompt must still appear. Old versions are unsupported (D5), but they must fail in a safe way.
- User-facing changelog line (the user adds it): chats reload once after the update; drafts of direct chats are cleared.
- Open before the build: the unsent-queue decision in `plans/Todo.md`.

Release order (one maintenance window, as short as possible):

1. Submit Android and iOS and wait until **both** are approved and held: Play "managed publishing", App Store "manually release this version". Do not let either store release on its own.
2. Server: backup, then deploy. Its migration runs before it serves and can take minutes (the server plan measures this in the dry run).
3. Release both store versions and the desktop build at once.
4. Run the smoke test against production with the released builds.

There is no way back after step 3: new messages exist only in the new format. The way back before step 3 is in the server plan (section 1.4).

### 3.9 What stage 2 gives stage 3

One mechanism for every chat, no DM branch in the crypto layer:
- Session scope `(chatId, senderId, sessionId)`; recipients of a key share = `members(chatId)` including myself.
- One rotation trigger: `chat.membershipVersion` differs from the one stored on the session.
- One server rule for key shares: sender and recipient are both members of `chatId`.
- One history rule: `historyVisibility` (`FULL` = share sessions at index 0, `SINCE_JOIN` = share at the current index).
- Keys of every co-member reach the client through `/users/sync`.

---

## 4. Stage 3: end-to-end encryption

Design modeled on Matrix (per-sender message sessions, encrypted key backup, signatures, verification), not on pure Signal: Signal's per-device Double Ratchet has no server history, and Schneaggchat re-syncs the full history on a new device.

**Goal**: the server cannot read message text, cannot swap keys without the users seeing it, removed members cannot read later messages, and history survives a new-device login through a password-protected key backup.

### 4.0 What is protected, and against whom

| Adversary | Protected | Not protected |
|---|---|---|
| Somebody who reads the server database or its backups | Text of encrypted messages, the key backup (as strong as the password) | Everything in 4.9 |
| The server itself, acting against its users | Text of encrypted messages, as long as users notice key-change warnings; no silent fall back to plaintext (send gate, 4.6) | It can add a member to a chat (visible as a system message), hold back messages, and guess weak passwords offline |
| Somebody with an unlocked device | Nothing | Room holds plaintext, the device holds all keys |
| A removed chat member | Messages after the removal | Messages before it |

### 4.1 Current state (verified 2026-11-12)

- Nothing is implemented: no `keys` package, no `encryptionVersion`, no key fields in either repo. `encryptionVersion = 1` stays unused; this plan uses `2`.
- Crypto library is in the catalog but imported nowhere: `dev.whyoleg.cryptography` 0.6.0 (`gradle/libs.versions.toml:8, :89-90`, `composeApp/build.gradle.kts:183-184`). It has ECDH P-256, ECDSA P-256, HKDF-SHA256, HMAC-SHA256, AES-256-GCM, PBKDF2, SecureRandom. Version 0.6.0 (2026-04-02, the newest on 2026-11-13) also has Ed25519 and X25519, but on Android they come from the platform, and old Android versions (minimum SDK 26) may not have them. So everything runs on P-256, which every target has. Step P1.0 proves this before any protocol code is written.
- Secure storage: `Preferencemanager.SecureKey` (`C/datasource/preferences/Preferencemanager.kt:38-46`) on KSafe 3.3.0; `clearSessionCredentials()` (`:94-99`) deletes every enum entry, so new entries are wiped on logout without extra code.
- Push content is plaintext today (`S/notifications/NotificationService.kt:99`). The server already keeps FCM payloads under 4 KB (`S/notifications/firebase/FirebaseService.kt:65, :191`).
- Login: `RemoteAuthRepository.login` (`C/login/data/RemoteAuthRepository.kt:154`, password in scope), `createAccount` (`:174`), `changePassword` (`:188`), saved login without password `loadSavedLoginConfig` (`:120`).
- After stage 1 all message text passes through `MessageContentCodec` (`decode`, `decodePush`, `encode`). Stage 3 adds one implementation of it and changes one Koin binding.
- The login sends the password itself to the server (`RemoteAuthRepository.login`, server `AuthService.login` `:121-142`, bcrypt compare). A key backup wrapped with a key made from that password could be opened by the server at every login. Section 4.3 "Login secret" changes this first.

### 4.2 Decisions

| Question | Decision |
|---|---|
| What is encrypted | Text messages. Media, captions, polls, reactions stay plaintext (future work). |
| Protocol model | Per-sender message sessions (hash ratchet) per chat, ECIES against signed prekeys for session distribution, encrypted key backup for history. |
| Chat model | One mechanism for every chat (section 3.9). |
| Key granularity | Per account (D12). Every device restores the same key bundle from the backup. |
| Trust model | Signed prekeys chained to an identity key; trust on first use + safety-number screen + key-change warnings. Same for friends and non-friend co-members. |
| History for new members | Follows `chat.historyVisibility`. |
| History on a new device | Key backup encrypted under a password-derived key. Email password reset = backup lost = new identity, old encrypted messages show placeholders. |
| Existing messages | Stay plaintext. `encryptionVersion` per message: 0 = plaintext, 2 = this envelope. |
| Local Room storage | Plaintext + flag. All crypto behind `MessageContentCodec`. UI, search, reply previews untouched. |
| Login | The client sends a login key derived from the password, never the password (4.3 "Login secret"). Open alternative for the user: a separate recovery code (`plans/Todo.md`). (review 2026-11-13) |
| Downgrade | A chat that was encrypted once stays encrypted; no silent plaintext (4.6 send gate). (review 2026-11-13) |
| Rollout | Receive first (Phase 2R), then send behind a developer switch, then send for everybody (P2.9). (review 2026-11-13) |
| iOS notification text | Generic "new message" for encrypted text in this version; open decision in `plans/Todo.md`. |

### 4.3 Crypto design

All primitives from `dev.whyoleg.cryptography` 0.6.0. Every HKDF/HMAC use gets a distinct `info`/domain-separation string prefixed `schneaggchat-e2e-v2:`.

#### Key hierarchy

1. **Identity key `IK`** — ECDSA P-256 signing pair, one per account. Root of trust; its public half is what safety numbers commit to. Private half lives in KSafe + key backup.
2. **Signed prekey `SPK`** — ECDH P-256 pair, public half signed by `IK`. Not rotated in this version: its private half lies next to the backup key on the device and inside the backup, so a rotation would protect nothing and would add races between the devices of one account. `spkId` stays in every format so rotation can be added later.
3. **Message session** (Megolm-analog) — per chat, per sender, per device instance:
   - `sessionId` = random 128-bit id; state = 32-byte ratchet value `R_i` + index `i`.
   - Ratchet: `R_{i+1} = HMAC-SHA256(R_i, "advance")`; per-message key: `HKDF-SHA256(R_i, info = "...:msgkey:" + sessionId + ":" + i)` → AES-256-GCM key. The OUTBOUND state discards old `R` values after advancing. This does not give forward secrecy on a device: every device also keeps an INBOUND copy from the first index it knows, to read history again after a reload. The job of the ratchet is `SINCE_JOIN`: a session shared at index `i` cannot open messages before `i`.
   - The sender imports its own session as an INBOUND session at index 0 when it creates it, so own messages decrypt like everybody else's (from sync, socket and on its other devices), and later joiners of a `FULL` chat can get the session from index 0.
   - **Never restore an outbound session**: OUTBOUND state exists only in Room on the device that created it and is never written to the backup. After a restore or a database reload the next send creates a new session. Continuing an old one would use an index, and so a key, twice.
   - AES-GCM nonce: 12 random bytes per message, carried in the envelope, so even a repeated index cannot repeat a key and nonce pair. An edit takes a new index.
   - Each session carries its own ECDSA P-256 **session signing pair**; its public half is bound into the session export, which is itself signed by the sender's `IK` ⇒ receivers can prove which member sent a message (no in-chat forgery).
   - Each session records the `membershipVersion` it was created for and the set of recipients it was shared with.
   - **Rotation**: whenever `chat.membershipVersion` differs from the session's (join, leave, remove, account deletion; for DIRECT also unfriend/block/re-accept), otherwise after 500 messages or 7 days. Rotation = new random session, distributed fresh.
4. **Backup key `BK`** — random 32 bytes. Wrapped with AES-256-GCM under the backup wrap key, which the client derives from the password and never sends ("Login secret" below). Every session the client learns (its own at creation, others at import) is appended to the server-side backup as a session export (ratchet value, index, public signing key; never the private signing key), encrypted under `BK`, together with the `IK` and `SPK` privates. Password change client-side = re-wrap `BK` only (one small blob), atomic with the change request.

#### Login secret (the server never gets the password)

Built first, in Phase 0 (server) and Phase 1 (client), because the backup is worth nothing without it.

- `masterKey = PBKDF2-SHA256(password, kdfSalt, 600 000 rounds, 32 bytes)`; `kdfSalt` = 16 random bytes per account, stored by the server, not secret.
- `loginKey = HKDF-SHA256(masterKey, info = "schneaggchat-login-v1")`: sent instead of the password at login, signup and password change. The server stores bcrypt of it.
- `backupWrapKey = HKDF-SHA256(masterKey, info = "schneaggchat-e2e-v2:backup-wrap")`: wraps `BK`, never leaves the device.
- Before login the client asks for the account's `kdfSalt` (new call; for an unknown name the server answers with a stable fake salt, so the call does not reveal which names exist).
- Old accounts switch at their next login with the password: the client sends the password once more together with a new `kdfSalt` and `loginKey`; the server checks the password, stores the new values and deletes the old hash. Keys and backup of an account are created only after this switch.
- Password reset by email: the web form (`reset_password_form.html` on the server) computes the same keys in the browser (WebCrypto has PBKDF2 and HKDF). The backup is lost (decision in 4.2).
- Open product decision for the user (noted in `plans/Todo.md`): a separate recovery code instead of the password would be stronger (no offline guessing) but users lose it.

#### Wire formats (opaque strings to the server)

- **Message content** (`encryptionVersion = 2`): Base64 of JSON `{ "v": 2, "sid": sessionId, "idx": i, "n": b64(nonce), "ct": b64(AES-GCM box), "sig": b64(signature) }`. Fits a 56 000-char limit for 10 000-char plaintext. Header string `h = "2|" + chatId + "|" + senderId + "|" + sid + "|" + idx` (UTF-8; the ids never contain `|`). AES-GCM associated data = `h`, so a ciphertext cannot be moved to another chat, sender or position. `sig` = ECDSA with the session signing key over `h + "|" + n + "|" + ct` (the Base64 strings). The receiver stores which message id used which `(sid, idx)` (`MessageEntity.e2eSessionId`, `e2eIndex`); the same pair under another message id is shown as a placeholder, never as a second message.
- **Session export** (what gets shared): `{ sessionId, chatId, senderId, membershipVersion, ratchet: R_i, idx: i, sessionSigPub }` + `IK` signature over the whole export. A receiver importing at index `i` can decrypt messages `≥ i` only.
- **Key share** (ECIES): sender generates ephemeral ECDH pair `e`; `k = HKDF-SHA256(ECDH(e, SPK_recipient), info = "...:share:" + chatId + ":" + recipientUserId)`; blob = `{ ePub, spkId, ct: AES-GCM(k, sessionExport) }`. Authenticity comes from the `IK` signature *inside* the export, checked against the sender's published identity key.
- **Safety number**: SHA-256 over sorted `(userId‖IK_pub)` of both parties → rendered as 12 groups of 5 digits, Signal-style. Comparison screen; `verified` flag stored locally; any change of a peer's `IK` flips every chat containing that peer into a "key changed" warning state.

#### Decrypt-miss healing (the Matrix lesson)

A message can arrive before its session share (offline devices, lost socket frames). Never block sync: store the raw envelope in `MessageEntity.pendingContent`, show a "waiting for key / could not decrypt" placeholder in `content`, and run a re-decrypt pass over all `pendingContent` rows after every share pickup and after backup restore. Designed in from day one — it is the single biggest UX failure mode in real Matrix clients.

### 4.4 Phase 0: server (deployable alone, inert)

In the server plan file. Added by the review: the login key (`kdfSalt` call, `loginKey` at login, signup, password change, reset form), "publish only if nothing is stored" for the first key upload (two devices that log in at the same time must not both create an identity), a chat flag that makes the server refuse plaintext text in an encrypted chat (catches client bugs), and size limits for all `/keys/*` bodies. Summary of what the client gets: `encryptionVersion: Int` on message requests and `MessageResponse`; `membershipVersion: Long?` on send requests, a stale value is answered with `409`; endpoints `POST /keys/publish`, `POST /keys/shares`, `GET /keys/shares`, `POST /keys/shares/ack`, `POST /keys/backup`, `GET /keys/backup`, `POST /keys/backup/entries`, `GET /keys/backup/entries`; `identityPub`, `spkPub`, `spkSig`, `spkId` on every user response shape; socket hint `keysharewaiting`; push carries the envelope when it fits into 4 KB, else empty `content` + `needsFetch`, plus `encryptionVersion`.

### 4.5 Phase 1: client crypto core and key lifecycle (silent release)

New package `C/utilities/crypto/`. Run `code-reuse-scout` first.

- [ ] **P1.0 Spike, before any protocol code**: (1) a throwaway test that runs ECDH P-256, ECDSA P-256, HKDF-SHA256, HMAC-SHA256, AES-256-GCM and PBKDF2-SHA256 (600 000 rounds, measure the time) on an Android API 26 device or emulator, on iOS and on desktop, and compares the bytes between the platforms (signature and key encodings differ per provider; fix one encoding). (2) Look again for an audited library that covers the message session part on all three targets (candidates: vodozemac bindings of the Trixnity project, `mls-rs` UniFFI bindings). Use one only if it supports Android, iOS and JVM, has a licence the app can use, and is maintained; report the result to the user. If none: continue with P1.1 as written.
- [ ] **P1.1** Protocol core, pure Kotlin on cryptography-kotlin only, testable on JVM: `MessageSession` (outbound/inbound ratchet state, encrypt, decrypt, export/import at an index), `KeyShareCodec` (ECIES wrap/unwrap against a signed prekey, signature check against the identity key), `IdentityStore` (identity key and signed prekey generation), `LoginKeys` (master key, login key and backup wrap key from password and salt), `KeyBackup` (backup key wrap/unwrap, entry encrypt/decrypt), `SafetyNumber`.
- [ ] **P1.2** `E2eEncryptionManager` interface + one Koin singleton implementation (`C/di/Modules.kt`): `ensureKeysOnLogin(backupWrapKey)`, `encryptFor(chatId): Envelope`, `decrypt(envelope, chatId, senderId): String?`, `onChatChanged(chat)`, `pickUpShares()`, `healOutboundShares(chatId)`, `redecryptPending()`. Used by a new `E2eMessageContentCodec` (the stage 3 implementation of `MessageContentCodec`; the return types of `decode` and `encode` grow by `encryptionVersion`, `membershipVersion` and the pending envelope) and by `RemoteAuthRepository`. `InboundMessageProcessor` and `OfflineFirstMessageRepository` do not change. Tests get a `FakeE2eEncryptionManager` in `commonTest/.../fakes/`.
- [ ] **P1.3** Persistence, one database version bump: new `E2eSessionEntity`, primary key `(sessionId, direction)`: sessionId, direction `OUTBOUND|INBOUND`, chatId, senderId, membershipVersion, ratchet, idx, sessionSigPub, sessionSigPriv (OUTBOUND only), sharedWith (OUTBOUND only), createdAt, msgCount. An INBOUND row keeps the ratchet value of the first index it knows and is never advanced in place (the key for a later index is computed from a copy), so history can be decrypted again after a reload. An own session has two rows: OUTBOUND (current state) and INBOUND (index 0). `UserEntity` + `identityPub`, `spkPub`, `spkSig`, `spkId`, `verified`, `keyChangedAt`; `ChatEntity` + `encrypted: Boolean` (set from the chat response or by the first encrypted message, never set back to false); `MessageEntity` + `encrypted: Boolean`, `pendingContent: String?`, `e2eSessionId: String?`, `e2eIndex: Int?`. Private keys in KSafe: new `SecureKey` entries `E2E_IDENTITY_KEY`, `E2E_SPK_KEY`, `E2E_BACKUP_KEY`. `SessionTerminator.resetLocalData()` (`C/app/data/LocalSessionTerminator.kt`) and every destructive database bump wipe Room but not KSafe: the keys survive, the sessions come back from the backup as INBOUND rows, and the next send creates a new OUTBOUND session.
- [ ] **P1.4** Remote: new `KeyRemoteDataSource` + Ktor implementation for the `/keys/*` endpoints; `encryptionVersion` + `membershipVersion` on the message DTOs (sent as 0 / null for now); key fields on the user DTOs (`C/datasource/network/dto/UserNetworkDtos.kt`) mapped into `UserEntity` (both the sync path and the socket `UserChange` branch, `SocketConnectionMessage.kt:352-480`).
- [ ] **P1.5 Login key, its own release before any key is created** (4.3 "Login secret"): `RemoteAuthRepository.login` (`:154`) first asks the server for the `kdfSalt` of the user name, derives `masterKey`, `loginKey` and `backupWrapKey` (PBKDF2 runs off the main thread and the login button shows progress; the spike P1.0 measured the time), and logs in with `loginKey`. For an account that is still on the old scheme it sends the password as today plus a new `kdfSalt` and `loginKey`, and the server switches the account. `createAccount` (`:174`) and `changePassword` (`:188`) send login keys only; a password change uses a new `kdfSalt`. After an account has switched, app versions older than this release cannot log in to it (old versions are unsupported, D5). Manual check by the user on Android, iOS and desktop: login, wrong password, signup, password change, email reset, the store test account.
- [ ] **P1.6 Keys at login and signup**: after the tokens are saved and before the first `dataSync`, call `ensureKeysOnLogin(backupWrapKey)`: try the backup restore (fetch, unwrap, import the entries as INBOUND sessions), else generate keys, publish (the server answers `409` when another device of the account was faster: restore instead), upload the backup. Saved login: the keys are in KSafe, nothing to do. After a database reload the sessions are restored with the backup key from KSafe, no password needed. Password change: re-wrap the backup key with the new `backupWrapKey` and send it in the same request.
- [ ] **P1.7** Tests are part of this phase (ask the user for the go-ahead when the phase starts; without them the phase is not done): fixed test vectors for ratchet, key share, backup and login keys that give the same bytes on Android, iOS and desktop; negative tests (wrong key, changed ciphertext, changed associated data, wrong signature must all fail).
- [ ] **P1.8** Before Phase 2S: an independent review of section 4.3 and of the protocol core (a second reviewer, not the agent that wrote it). Crypto that only its author has read is not released.

### 4.6 Phase 2: encrypted text in every chat (Phase 2R receive, Phase 2S send)

Two releases, so that a user's own devices that have not updated yet can already read. Phase 2R can decrypt: P2.1, P2.2, P2.6, P2.7, P2.8 and the pickup part of P2.5. Phase 2S sends: P2.3, P2.4, the share part of P2.5, P2.9. (Named 2R and 2S so they are not mixed up with stages 2a and 2b.)

**Send gate**: a chat becomes encrypted when the server lets me send in it and every member has published keys. From then on it stays encrypted (`ChatEntity.encrypted`, set by the first encrypted message sent or received): text is never sent in plaintext into such a chat. If a member has no keys later, the message is still sent encrypted and that member sees a placeholder. Without this rule the server could remove a member's keys from `/users/sync` and receive plaintext.

- [ ] **P2.1 Inbound** (`E2eMessageContentCodec.decode`): `encryptionVersion == 2` -> find the inbound session `(chatId, senderId, sid)`, check the signature, decrypt, return plaintext, set `encrypted = true`. Miss -> placeholder text in `content`, raw envelope in `pendingContent`; never block the sync. If decryption fails and the stored row has readable text with the same `lastChanged`, keep the stored text.
- [ ] **P2.2 Push** (`E2eMessageContentCodec.decodePush`): decrypt the envelope from the payload; on a miss or `needsFetch` return the generic "new message" text and let the sync bring the message. `applyPush` stores no provisional row for a message it could not decrypt. iOS Notification Service Extension shows the generic text (no key access from the extension).
- [ ] **P2.3 Outbound** (`E2eMessageContentCodec.encode`, used by send, edit and stuck-edit retry): get or create the outbound session of the chat (on create: wrap the export for every member including myself, upload the shares, append to the backup), encrypt, send `encryptionVersion = 2` + `membershipVersion`. `409` -> `syncChats`, rotate, retry once. The Room row keeps plaintext. The offline queue is unchanged (it re-enters the send, so it encrypts at real send time). An encrypted message is edited encrypted.
- [ ] **P2.4 Membership changes** through `onChatChanged` (called from the `ChatChange` socket branch and from `syncChats`): member added -> rotate, share the new session with all members, and share older sessions to the newcomer only when `historyVisibility == FULL` (from the own INBOUND copy at index 0); member removed / left / account deleted -> every remaining member rotates on the next send; DIRECT unfriend / block / re-accept -> `membershipVersion` changed, rotate on the next send.
- [ ] **P2.5 Healing**: on chat open and on `dataSync`, compare the own outbound session's `sharedWith` with the current members and upload shares for anyone missing. Pick up shares on app resume, on the socket hint, on chat open and before declaring a decrypt miss; run `redecryptPending()` after every pickup and after a backup restore.
- [ ] **P2.6** Signature check failure -> placeholder, never silent acceptance.
- [ ] **P2.7 Key-change warning** (must exist before sending is on; without it the server can swap a key unseen): a peer's `identityPub` changed -> banner in every chat that contains the peer.
- [ ] **P2.8 Lock indicator**: an encrypted chat shows it in the top bar and in the chat details; the details name the member without keys when a chat is not encrypted yet.
- [ ] **P2.9 Rollout of Phase 2S in two releases**: first, sending encrypted is behind a switch in the developer settings (`C/settings/presentation/devsettings/`), default off, so the user and testers run it in real chats; second, the switch is removed and sending is on for everybody. Decrypting is always on since Phase 2R.

### 4.7 Phase 3: verification and reset

- [ ] **P3.1** Safety-number screen per peer, reachable from DIRECT chat details and from every member row in GROUP chat details. `verified` flag on `UserEntity`. New route: follow the "New route" wiring checklist in `CLAUDE.md`.
- [ ] **P3.2** The key-change banner of P2.7 also resets `verified`.
- [ ] **P3.3** Backup not recoverable at login (wrong-password decrypt failure, or the server marks the backup invalid after an email password reset): dialog "Encrypted history can't be recovered. Generate new keys?" -> new keys, publish, peers get key-change warnings, healing re-shares current sessions. Old encrypted messages show placeholders.

### 4.8 Example: Alice → Bob "Hallo" (DIRECT chat `c1`; a group works identically with N recipients)

1. First encrypted message in `c1` (`membershipVersion = 3`, members Alice + Bob): Alice creates session `S` (random ratchet `R_0`, session signing pair, `membershipVersion = 3`), signs the export with her `IK`, wraps it via ECIES for every member of `c1` — Bob's `SPK` and her own — `POST /keys/shares` with `chatId = c1`, imports `S` as her own inbound session and appends the export to her backup.
2. She encrypts: key = `HKDF(R_0)`, random nonce, AES-GCM("Hallo", associated data = the header of `c1`, Alice, `S`, index 0), signs, sends the envelope as `content` with `encryptionVersion = 2` and `membershipVersion = 3`, and advances her outbound state to `R_1`. Her inbound copy of `S` stays at index 0. Local Room keeps "Hallo".
3. Server checks `membershipVersion` matches, stores/relays the envelope; push carries it (≤ 4 KB) unchanged; the recap never sees plaintext.
4. Bob (possibly hinted by `KeyShareWaiting`) pulls his shares, unwraps with his SPK private, verifies Alice's `IK` signature (TOFU on first contact), imports `S` at index 0, acks the share (server deletes it), appends `S` to *his* backup, decrypts, stores plaintext, notification shows "Hallo".
5. Bob's new laptop, three weeks later: login with password → backup restore returns `S` and every other session → full history re-sync decrypts completely.
6. Alice unfriends Bob: `c1.membershipVersion` → 4, Bob can no longer send (`canSend`), Alice's next session after a re-friend is fresh.
7. If the server had swapped Bob's keys to MITM the chat, Alice's client would show the key-change banner, and a safety-number comparison would fail.

### 4.9 What stays visible to the server

All metadata: who talks to whom, timestamps, message types, chat membership, read receipts, reaction content, polls, media bytes and image captions, ciphertext length, push tokens. **Forward secrecy is deliberately bounded**: every session lies in the password-protected backup from the first index the account knows, so an adversary with the server's ciphertext archive *and* the user's password reads everything. This is the same trade-off as Matrix key backup; it is the price of "history survives a new device". The server never receives the password ("Login secret"), but it stores a hash of the login key and can try passwords against it offline, so a weak password is a weak backup. A server that recorded a password before the account switched to the login key can use it until the user changes the password. Within that bound: a server-only compromise reads nothing; an identity-key-only compromise reads nothing by itself (shares are ECIES to the signed prekey and deleted after pickup); a removed member reads nothing after the rotation. No deniability (messages are ECDSA-signed). Account-level keys: any logged-in device equals the account, key-change warnings fire per account, not per device. The server controls chat membership: it could add a member and so receive future sessions; users see this as a SYSTEM "member added" message and a changed member list (same limitation as Matrix without cross-signed membership).

### 4.10 Future work (not in this plan)

- Per-device keys with device list and cross-signing (the server has a device registry since the unified notifications work; decided against for now, D12).
- One-time prekeys / full X3DH and a Double Ratchet channel.
- Media, caption, poll and reaction encryption (needs image processing moved from the server to the client).
- Sealed sender / metadata reduction.
- Decryption inside the iOS notification extension (App Group key mirror).
- QR-code safety-number scanning.
- Signed membership changes, so the server cannot add a member unseen.

### 4.11 Effort

Stage 1: small (about 15 files). Stage 2a: about 85 client files, mechanical, in steps that each compile. Stage 2b: about 30 client files and 25 server files; riskiest parts are the server data migration, the sync/backfill redesign and the push payload on three platforms. Stage 3: about three times stage 2 on the client; the protocol core is security-critical hand-written code (no vetted Kotlin Multiplatform library was found; step P1.0 checks again), so it is isolated, pure Kotlin and tested with vectors before any UI work.

---

## 5. Review 2026-11-13: what changed and why

The first version of this plan (2026-11-12) was reviewed against the code with the question "would a professional do it this way". Decisions the user made (section 2.4, the order of the stages, one release for the wire switch, no support for old versions) were kept.

| # | Change | Why |
|---|---|---|
| 1 | New stage 1a: stored sync cursor | The cursor was `MAX(version)` of the message table. Every other writer had to store a wrong version, and the backfill of stage 2 would have lost messages. |
| 2 | Stage 1b: one merge rule by `version` instead of a table of rules per source; one lock | Simpler to build, and it closes the case where a late answer puts an older state back. |
| 3 | Stage 1b: `MessageContentCodec` interface instead of three private functions in two classes | Stage 3 swaps one binding and does not edit the handler or the repository. A marking codec can prove in a test that every path uses it. |
| 4 | Stage 2 cut into 2a (client only, today's server), 2b (wire switch), 2c (features) | In one go the client would not compile over about 90 files and could not be started before the new server exists. |
| 5 | 2b is built against the test server with migrated data; release with both stores held; old-client check | The first real test must not be the release. |
| 6 | Stage 3: login key instead of the password | The password goes to the server at every login, so the server could open a backup that is wrapped with it. |
| 7 | Stage 3: an encrypted chat never falls back to plaintext; key-change warning before sending is on; lock indicator | Without them the server can get plaintext or swap keys unseen, which the goal of stage 3 excludes. |
| 8 | Stage 3: no signed-prekey rotation; own inbound session copy; outbound sessions are never restored; random nonce, associated data and replay rule spelled out | Less protocol to get wrong; the first plan left these open. |
| 9 | Stage 3: spike P1.0, tests and an independent review as part of the work; sending first behind a developer switch | Hand-written crypto must be proven on all three platforms and read by a second reviewer before all users depend on it. |
| 10 | The claim "no X25519 / Ed25519 in the library" corrected | Version 0.6.0 has them; P-256 stays because of old Android versions. |
