# Event RSVP Plan (seen / accepted / dismissed per user)

Both repos: client `SchneaggchatV3` (KMP) and server `SchneaggchatV3server` (Spring Boot + Mongo). Written 2026-09-05, nothing implemented yet.

**Goal**: every event carries a list of `(userId, status, timestamp)` entries. Status is one of `SEEN`, `ACCEPTED`, `DISMISSED`. The events screen marks events the current user has no entry for as **new**, the join popup lets the user accept or dismiss, and the creator (and everyone else who can see the event) can see who has seen / accepted / dismissed it.

**Core idea**: store the list **embedded in the event** (Mongo sub-document array, Room JSON column), exactly like `Event.invitedUsers` and `Message.readers`/`Message.reactions` already are. A status change bumps `Event.updatedAt`, so the existing IdTimeStamp sync (`/events/sync`) and the existing `EventChange` socket push carry it to every device for free. **No new collection, no new sync job, no new socket message, no new Room table.**

---

## 1. Current state (verified 2026-09-05)

| Piece | Where | Relevant detail |
|---|---|---|
| Server model | `events/eventmodel/Event.kt` | Plain `@Document("events")`, no `@Version`, `updatedAt`/`updatedBy` set on every save |
| Server sync | `EventService.eventIdSync` | Diffs `event.updatedAt > clientTimestamp`; visibility filter via `canAccessEvent` |
| Server push | `NotificationService.notifyEventUpdate` | Socket `EventChange(event, newEntry, deleted)` to invited + creator's friends; FCM/APNs only when `newEntry` |
| Server join | `EventService.joinEvent` | Adds user to the event group, enforces `maxUsers` via group members. Events **without** a group have no join path and `maxUsers` is not enforced for them |
| Server per-user list precedent | `message/messagemodel/Message.kt` | `readers: List<Reader(userId: ObjectId, readAt: Instant)>`, `reactions: List<Reaction>`; atomic edits via `mongoTemplate.findAndModify` (`MessageService.kt:380`) |
| Client domain | `events/domain/Event.kt` | `@Serializable data class Event`, also travels inside `Route.Events(selectedEvent)` |
| Client Room | `events/data/dtos/EventDto.kt` | `invitedUsers: List<String>` stored via `RoomTypeConverters`; DB version **82**, `fallbackToDestructiveMigration(dropAllTables = true)` |
| Client wire | `datasource/network/requestResponseDataClasses/EventNetworkDtos.kt` + `EventResponseMapper.kt` | `EventResponse` mirrors server 1:1, three mappers |
| Client socket | `SocketConnectionMessage.kt:422` | `EventChange` -> `eventRepository.upsertEvent`, local notification when `newEntry` |
| Client screen | `events/presentation/*` | `EventsViewModel` combines Room flows into `EventsState`; `EventsScreen` picks `EventEditPopup` (own) or `EventJoinPopup` (foreign); `isJoined` derived from group membership |
| "Joined" today | `EventsScreen.kt` | `groups.first { id == event.groupId }.members.any { userId == ownId }` - group membership, not an event property |
| Nav bar badge | `app/navigation/NavigationBarItemTemplate.kt` | No badge support exists today |

### 1.1 Consequences

1. Embedding the list in `Event` reuses sync + push unchanged; the only server-side cost is that a status change is a write to the event document that must be **atomic** (two users responding at the same time must not overwrite each other). `Event` has no `@Version`, so use `mongoTemplate` array operators instead of load-modify-save.
2. The client already stores JSON lists in the `events` table; adding one more column is a converter + version bump.
3. `EventJoinPopup` currently receives a **snapshot** `Event` (`_state.selectedEvent` copied at click time). Once statuses change live it must observe the live event row, or the "going" list and own status would go stale inside the open sheet.

---

## 2. Data model

### 2.1 Types (identical names on both sides - they are the JSON keys, Mongo field names, and Room JSON)

```kotlin
enum class EventRsvpStatus { SEEN, ACCEPTED, DISMISSED }

// server: userId ObjectId, updatedAt Instant   |   client + wire: userId String, updatedAt Long (epoch millis)
data class EventRsvp(val userId, val status: EventRsvpStatus, val updatedAt)

// added to Event / EventResponse / EventDto
val rsvps: List<EventRsvp> = emptyList()
```

`rsvps` holds **at most one entry per user**. The server owns the timestamp (`Clock.System.now()`), never the client.

### 2.2 Transition rules (enforced on the server, mirrored in UI)

| Incoming | Existing entry | Result |
|---|---|---|
| `SEEN` | none | insert `SEEN` |
| `SEEN` | `SEEN` / `ACCEPTED` / `DISMISSED` | **no-op** (never downgrade, no write, no push) |
| `ACCEPTED` | any / none | upsert `ACCEPTED` (respect `maxUsers`, see 3.2) |
| `DISMISSED` | any / none | upsert `DISMISSED` |

- Creator gets an `ACCEPTED` entry when the event is created (in `upsertEvent`, `existing == null`).
- `joinEvent` (group path) also upserts `ACCEPTED`, so "joined" and "accepted" never disagree.
- Leaving the event group does **not** touch the entry in v1 (see §7).
- "Unseen" on the client means: `rsvps.none { it.userId == ownId } && creatorId != ownId`.

---

## 3. Server design (`SchneaggchatV3server`)

### 3.1 Files

| File | Change |
|---|---|
| `events/eventmodel/EventRsvp.kt` (new) | `EventRsvpStatus` enum, `EventRsvp(userId: ObjectId, status, updatedAt: Instant)`, `EventRsvpResponse(userId: String, status, updatedAt: Long)`, `toResponse()` |
| `events/eventmodel/Event.kt` | `val rsvps: List<EventRsvp> = emptyList()` (default keeps old documents loadable, same trick as `groupDeleteDelay`) |
| `events/eventmodel/EventResponse.kt` | `val rsvps: List<EventRsvpResponse> = emptyList()`, map in `Event.toResponse` |
| `events/eventmodel/EventRsvpRequest.kt` (new) | `EventRsvpRequest(eventId: String, status: EventRsvpStatus)` |
| `events/EventsLookupService.kt` | `upsertRsvp(eventId, userId, status): Event?` and `findRsvp(event, userId)` - the only place that writes the array (needs `MongoTemplate`) |
| `events/EventService.kt` | `setRsvp(requestingUser, request): EventResponse`; call `upsertRsvp(ACCEPTED)` from `upsertEvent` (new event, creator) and from `joinEvent` |
| `events/EventsController.kt` | `POST /events/rsvp` |
| `notifications/NotificationService.kt` | nothing new - `notifyEventUpdate(newEntry = false, deleted = false)` is reused |

### 3.2 `POST /events/rsvp`

```
body   EventRsvpRequest(eventId, status)
auth   requireAuth()
checks validateObjectId(eventId); event exists (404); canAccessEvent(requester, event) (else requireOrLog -> 403 + log, same wording as joinEvent)
       status == ACCEPTED && event.groupId == null && event.maxUsers != null
           -> require(acceptedCount(excluding requester) < maxUsers) { "Event is full" }
       status == ACCEPTED && event.groupId != null
           -> allowed, but the client should call /events/join instead (join already sets ACCEPTED); accept it anyway so a status-only accept without joining the chat works
write  EventsLookupService.upsertRsvp(...) (3.3), returns null when the SEEN no-op rule applied
push   if written: notifyEventUpdate(saved.toResponse(creatorName), newEntry = false, deleted = false)
return EventResponse (current state, written or not) so the caller can upsert locally right away
```

`updatedBy` stays untouched (it means "last content editor"); only `updatedAt` is bumped so sync picks the change up. No `EVENT_CHANGED` system message for status writes (that message is only sent by `upsertEvent`).

### 3.3 Atomic array write (`EventsLookupService.upsertRsvp`)

Two `mongoTemplate` calls, both filtered on `_id`; no read-modify-write of the whole document:

```kotlin
// 1. SEEN must not downgrade: only match when the user has no entry yet
val seenGuard = if (status == SEEN) Criteria.where("rsvps.userId").ne(userId) else null
// 2. remove the user's old entry (no-op when absent)
mongoTemplate.updateFirst(query(_id [+ seenGuard]), Update().pull("rsvps", Query(where("userId").is(userId))), Event::class.java)
// 3. push the new one + bump updatedAt, return the new document
mongoTemplate.findAndModify(query(_id [+ seenGuard]), Update().push("rsvps", EventRsvp(userId, status, now)).set("updatedAt", now), returnNew, Event::class.java)
```

A race between two requests of the **same** user is harmless (last writer wins, still one entry). Two **different** users never touch each other's entries because `$pull`/`$push` are per-element operators. Wrap in `withOptimisticRetry` is not needed (no `@Version`), but keep the pattern of `MessageService` for the `findAndModify` call shape.

### 3.4 Security checklist (run `schneaggchat-security-check` after implementation)

- `userId` comes from `requireAuth()` only; the request body never carries a user id.
- `canAccessEvent` reused unchanged, so visibility rules are identical to sync/join.
- Invalid enum -> Jackson 400; invalid ObjectId -> 400 via `ValidationUtils.validateObjectId`.
- Global `RateLimitFilter` (USER tier) covers the endpoint; no extra limiter needed.
- `rsvps` are visible to everyone who can see the event - same audience as `invitedUsers` today (see §7, decision 3).

---

## 4. Client design (`SchneaggchatV3`)

### 4.1 Data layer

| File | Change |
|---|---|
| `events/domain/EventRsvp.kt` (new) | `@Serializable enum class EventRsvpStatus`, `@Serializable data class EventRsvp(userId: String, status, updatedAt: Long)`, plus `fun Event.rsvpFor(userId): EventRsvp?`, `fun Event.isUnseenBy(userId): Boolean`, `fun Event.acceptedUserIds(): List<String>`, `EventRsvpStatus.labelRes()/icon()` |
| `events/domain/Event.kt` | `val rsvps: List<EventRsvp> = emptyList()` (default keeps `newEvent()` and previews compiling) |
| `events/data/dtos/EventDto.kt` | `val rsvps: List<EventRsvp>` |
| `datasource/database/RoomTypeConverters.kt` | `fromEventRsvpList` / `toEventRsvpList` (JSON, `ListSerializer(EventRsvp.serializer())`, tolerant decode like `toReactionList`) |
| `datasource/database/Database.kt` | version **82 -> 83**; new `schemas/.../83.json` gets generated by the next build (destructive migration + full re-sync, as always) |
| `EventNetworkDtos.kt` | `EventResponse.rsvps: List<EventRsvp> = emptyList()` (default = works against an older server); `EventRsvpRequest(eventId, status)` |
| `EventResponseMapper.kt` | map `rsvps` in all three mappers |
| `NetworkUtils.kt` | `suspend fun setEventRsvp(eventId, status): NetworkResult<EventResponse, NetworkingError>` = `safePost("/events/rsvp", EventRsvpRequest(...))` |
| `AppRepository.kt` | `suspend fun setEventRsvp(eventId, status): Boolean` - network, on success `eventRepository.upsertEvent(result.data.toEvent())`, on error `sendErrorSuspend` (same shape as `upsertEvent`) |
| `EventRepository.kt` | nothing new (upsert covers it) |
| `SocketConnectionMessage.kt` | nothing new (`EventChange` upsert already carries `rsvps`); the `newEntry` notification stays as is |

No optimistic local write in v1: the server answers with the full event and the socket echo arrives at the same time, so the UI updates within one round trip. See §7 for the offline decision.

### 4.2 ViewModel (`EventsViewModel`)

- `EventsState`: add `ownUserId: String?` (from `ownUserFlow`, so composables stop reading `SessionCache` for it) and `unseenEventCount: Int` (derived in the combine, used by the optional tab badge).
- `OnEventClick`: after setting `selectedEvent`, if `event.isUnseenBy(ownId)` -> `appRepository.setEventRsvp(id, SEEN)` fire-and-forget in `viewModelScope`. The server no-op rule makes a second call harmless, so no client-side guard is needed. Opening from a push notification (`initialEntryId`) goes through the same action and therefore also marks seen.
- New actions:
  - `OnAcceptEvent(eventId)` - for events **without** a group: `setEventRsvp(ACCEPTED)`, keep the sheet open so the user sees the "going" list update. Events **with** a group keep using `OnJoinEvent` (server sets `ACCEPTED` there).
  - `OnDismissEvent(eventId)` - `setEventRsvp(DISMISSED)`, then close the sheet.
- Live selected event: in the combine, when `selectedEvent.creatorId != ownId` and the events list contains the same id, replace `selectedEvent` with the live row. Own events keep the draft copy (the edit popup holds unsaved edits and the map-pick flow passes a modified draft in).

### 4.3 UI

| Composable | Change |
|---|---|
| `EventItem` | new params `ownStatus: EventRsvpStatus?`, `isUnseen: Boolean`, `acceptedCount: Int`. Unseen: small **New** `Badge` (`colorScheme.primary`/`onPrimary`) next to the title. `DISMISSED`: whole card at reduced alpha. `ACCEPTED`: check icon in the meta row. Meta row also shows "N going" when `acceptedCount > 0`. Callers: `EventsScreen` list, `EventsWeekView`, `EventsCalendarDayDetailSheet` |
| `EventRsvpOverview` (new, `events/presentation/uielements/`) | Three collapsible avatar rows **Going / Seen / Not interested** built from `rsvps` + `friendsById` (reuse `EventUserAvatar` like the invited row). Each avatar's `contentDescription` = name + `millisToString(updatedAt)`; long-press/tooltip is not needed in v1. Used by both popups |
| `EventJoinPopup` | Replace the single join button block with: `Join` (group) / `Accept` (no group) when own status != ACCEPTED; `Open chat` (group) or an "You're going" label when ACCEPTED; `Not interested` text button unless already DISMISSED; `EventRsvpOverview` above the divider. Needs `ownStatus`, `onAccept`, `onDismissEvent` params; `isJoined` stays (group membership still decides Open chat) |
| `EventEditPopup` | Read-only `EventRsvpOverview` section for the creator (below invited users). No new callbacks |
| `EventsScreen` | pass the new `EventItem`/popup params; `ownId` from `state.ownUserId` |
| Month view | unchanged (dots only) |

### 4.4 Strings (add to `values/`, then `values-de/` and `values-it/`; never `values-de-rAT`)

`event_new_badge` ("New"), `event_accept`, `event_not_interested`, `event_you_are_going`, `event_going_count` ("%1$d going"), `event_rsvp_going`, `event_rsvp_seen`, `event_rsvp_dismissed`, `event_rsvp_at` ("%1$s · %2$s" name + time), `event_rsvp_failed`.

### 4.5 DI / navigation

Nothing new: `EventsViewModel` already has `AppRepository`; no new routes.

---

## 5. Sync and lifecycle walk-through

| Scenario | What happens |
|---|---|
| A creates event | Server inserts `rsvps = [A ACCEPTED]`, pushes `EventChange(newEntry = true)` to invited + friends; B's client shows the card with **New** |
| B opens the card | `OnEventClick` -> `SEEN` -> server pull/push -> `updatedAt` bump -> `EventChange` to everyone -> A's edit popup lists B under "Seen"; B's other devices drop the New badge |
| B taps Join (group event) | `/events/join` adds group member **and** upserts `ACCEPTED`; existing `GroupChange` + new `EventChange` fan-out |
| B taps Not interested | `DISMISSED`; card dims on all of B's devices; A sees B under "Not interested" |
| B offline for a day | `/events/sync` diff on `updatedAt` pulls the event with the full `rsvps` list on next sync - nothing new to sync |
| A deletes event | Whole document (incl. `rsvps`) goes away; unchanged |
| Server old / client new | `rsvps` default `emptyList()` -> everything reads as unseen; harmless |
| Client old / server new | Old `EventResponse` decoders ignore the unknown key (`ignoreUnknownKeys`); harmless |

---

## 6. Phases

1. **Server** - `EventRsvp` model, `Event.rsvps`, `EventResponse.rsvps`, `EventsLookupService.upsertRsvp`, `EventService.setRsvp`, creator + join auto-accept, `POST /events/rsvp`. Run `schneaggchat-security-check`. Deploy first: the client tolerates the new field before it uses it.
2. **Client data** - domain type, DTO + converter + DB 83, wire + mappers, `NetworkUtils.setEventRsvp`, `AppRepository.setEventRsvp`.
3. **Client presentation** - ViewModel actions + live selected event, `EventItem` badge/dim/going count, `EventRsvpOverview`, popup buttons, strings (3 locales), README changelog.
4. **Optional follow-ups** - unseen count badge on the Events tab (nav bar has no badge support today, needs a `BadgedBox` in the bar composable and a state source outside `EventsViewModel`), "hide dismissed" filter, offline queue for ACCEPTED/DISMISSED, reset the entry when a user leaves the event group.

---

## 7. Decisions (defaults chosen; say so if any should change)

1. **Where "seen" is set**: opening the event sheet (`OnEventClick`), not scrolling past the card. The New badge stays until the user actually looks at the event, which is what makes it useful.
2. **Storage**: embedded in the event, not a separate collection - reuses sync + push, matches `readers`/`reactions`.
3. **Visibility of the list**: everyone who can see the event sees all entries (same audience as `invitedUsers`). If only the creator should see who dismissed, `Event.toResponse` gets a `viewerId` and filters `DISMISSED` entries for non-creators - one line, decide before phase 1.
4. **Fan-out of SEEN**: same audience as any event change. Events are low-volume; one small socket message per first open is acceptable and keeps every device consistent.
5. **Offline**: v1 has no retry queue. A failed `SEEN` is retried the next time the sheet opens; a failed accept/dismiss shows the `event_rsvp_failed` snackbar. Messages have an unsent queue, events do not, and adding one is out of scope here.
6. **Dismissed events** stay in the list, dimmed. Hiding them is a later filter.
7. **Leaving the event group** leaves the `ACCEPTED` entry alone in v1; `GroupService` has no event hook for member removal and adding one touches group deletion cascades.
8. **`maxUsers` for groupless events** becomes enforced through `ACCEPTED` count (today it is silently ignored for them).
