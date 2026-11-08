# Event Notifications Plan (reminders, change pushes, system messages, map preview, names)

Both repos: client `SchneaggchatV3`, server `SchneaggchatV3server`. Written 2026-11-08. Status: built 2026-11-08 (client and server), waiting for device tests. Server on branch `next-release`.

Covers these `plans/Todo.md` items: "Essential for the next update" (event change push, event system messages), "Event reminders", "Event notifications use one fixed id", "Event join popup: map preview", "Event participants: show names". Server side: `SchneaggchatV3server/Todo.md` section "Event reminders".

## Decisions (user, 2026-11-08)

| Topic | Decision |
|---|---|
| Reminders | Creator picks them on create and edit. Several per event. Presets plus custom lead times (e.g. 2 h 30 min before). New events preselect **1 day** and **1 hour** before. |
| Reminder recipients | Going users + creator (decided 2026-11-05), no opt-out for now. |
| Change pushes | Start time moved, location changed, event cancelled. Not for title or description edits. |
| Change push recipients | Going users + invited users with no answer yet. Not to "Not interested". Never to the user who made the change. |
| System messages | Specific texts: start moved (with the new time), location changed, cancelled. The generic "X changed the event" stays for all other edits. |
| Map preview | Small static map with a pin in the event detail popup, only when a location is set. Gestures off. Tap opens the Schneaggmap at the event. |
| Names | Claude's pick: name list grouped by status (below). Fallback if it does not work well: a popup like the poll details. |
| Name source | The server sends the user name with each invited user and each answer. The client prefers a stored friend, then the server name. Today only accepted friends and the own user get a name (`C/events/presentation/EventsViewModel.kt:92-116`); others would show a raw id. |
| Reminder limits | At most 5 per event, lead time 5 min to 14 days, custom times in 5 min steps. A reminder already past at save time is dropped. |

## Current state (checked 2026-11-08)

| Piece | Where | Today |
|---|---|---|
| Push on change | server `events/EventService.kt:216` `notifyEventUpdate(newEntry = existing == null)` | Push only for a new event; edits are `Live` (socket only) in `docs/UNIFIED_NOTIFICATIONS_PLAN.md:207`. |
| Change detection | server `EventService.kt:204` `contentChanged` | One flag for any field, no per-field info. |
| System message | server `EventService.kt:229` `EVENT_CHANGED`, client `C/chat/presentation/chat/messagecomposables/SystemEventText.kt:45` | One generic text, `text = event.title`. Released clients map unknown types to `SystemEventType.UNKNOWN` (`C/chat/domain/SystemEvent.kt:29`). |
| Delete | server `EventService.detachEvent` :244 | `notifyEventUpdate(deleted = true)`, socket only. |
| Client socket | `C/datasource/network/socket/SocketConnectionMessage.kt:517-522` | Local notification only for `newEntry`. |
| Notification id | `C/utilities/NotificationManager.kt:39`, `C/utilities/notifications/NotificationContentResolver.kt:194, :266` | Fixed id 5: a second event notification replaces the first. |
| Notification tap | `C/app/AppLifecycleManager.kt:76` `notifyNotificationOpened` | Takes a chat only, no event target. |
| Location in popup | `C/events/presentation/uielements/EventJoinPopup.kt` | Only used for "Add to calendar" (:270). No map. |
| Map | `C/schneaggmap/presentation/SchneaggmapScreen.kt` | Only `MaplibreMap` host (maplibre-compose 0.20.0). Location pick in the edit popup jumps to this screen (`EventEditPopup.kt:322`). |
| Names | `EventParticipationOverview.kt:41-64`, `EventUserAvatarRow.kt` | Avatar rows Going / Seen / Not interested, names only in the a11y text. No "no answer yet" group. |

## Part A: Reminders (client + server)

**Client, create and edit popup** (`EventEditPopup.kt`):
- New section "Reminders" under the date fields: one `FilterChip` per picked reminder ("1 day before", "2 h 30 min before") with a remove icon, plus an "Add reminder" chip.
- "Add reminder" opens a small dialog: preset chips (1 h, 6 h, 1 day, 2 days) and a "Custom" row with days / hours / minutes steppers.
- New events start with 1 day and 1 hour. An event without a start date in the future shows the section disabled.
- Duplicates are merged; the list is sorted, longest lead time first.
- Limits: at most 5 reminders, lead time 5 min to 14 days, custom minutes in 5 min steps.
- A reminder whose time is already past when saving is dropped from the list.

**Client, domain and storage**:
- `Event.reminderMinutes: List<Int>` (minutes before `startDate`, empty = none), in the Room entity (DB version bump), DTO and mappers.
- Old servers or old events without the field: empty list.

**Server, functionality** (server agent designs it):
- Store the reminder lead times per event; set on create and edit; validate the limits above.
- An edit from a released client (sends no field) must keep the stored reminders, not clear them.
- A scheduler sends each due reminder once per event, lead time and start date: reset when the start moves, skip ended events, skip lead times already past when the event is created or edited.
- Recipients: Going users + creator.
- Payload field names the client will read: `eventId`, `eventTitle`, `startDate` (epoch ms), `leadMinutes`.

**Client, showing it**: title "Reminder: <eventTitle>", body "Starts in <lead time>" / "Starts at <time>" in the viewer's time zone. Tap opens the event (Part D).

## Part B: Change pushes (server + client)

**Server, functionality**:
- On an edit that moves `startDate` or changes `location`, and on cancel (delete of the event), send an alert to the Going users + invited users without an answer, never to the actor.
- One push per save, listing all changed parts (start and location in one save = one push).
- Payload field names the client will read: `eventId`, `eventTitle`, `changes` (list: `START`, `LOCATION`, `CANCELLED`), `startDate` (new start, epoch ms), `actorName`.
- Moving the start also resets the sent reminders (Part A).
- Released clients show an unknown push type as the generic "You have new messages" (see P2 item "ship the client before the server"). So either ship the client first or skip old app versions for this push. Server agent decides.

**Client**:
- Show the push and the socket alert: "Grillabend moved to Sat 14 Nov, 18:00", "Grillabend has a new location", "Grillabend was cancelled"; combined text when both start and location changed.
- Texts in values, values-de, values-it; iOS Notification Service Extension strings (`iosApp/SchneaggchatNotificationServiceExtension/NotificationStrings.swift`) for en/de/it.
- Tap opens the event; a cancelled event opens the events screen.

## Part C: System messages (server + client)

**Server, functionality**: post into the event's group chat, instead of the generic text:
- start moved: carries the new start as epoch ms (the client formats it in the viewer's time zone),
- location changed,
- cancelled (only when the group survives the delete),
- the generic `EVENT_CHANGED` for any other change. One save with start and location changed: one message per changed part.
- Released clients show new types as `UNKNOWN`; server agent checks what that looks like and whether old clients should get `EVENT_CHANGED` instead.

**Client**: new `SystemEventType` entries and texts in `SystemEventText.kt` ("X moved the start to Sat 14 Nov, 18:00", "X changed the location", "X cancelled the event"), strings in values, values-de, values-it.

## Part D: Notification plumbing (client)

- Per-event notification id (hash of the event id in the `EVENT` range) so two events don't replace each other; a reminder and a change push of the same event replace each other (newest wins).
- Tap target: extend `AppLifecycleManager.notifyNotificationOpened` with an event target; open `Route.Events(selectedEvent)` with the stored event (falls back to the events screen if it is not stored yet).
- Android (`AndroidNotifier` + FCM service), iOS (NSE + tap handling), desktop (`DesktopNotifier`) all route the same way.
- Existing new-event notification uses the same id and tap path.

## Part E: Map preview (client)

- New reusable `sharedUi` composable `StaticMapPreview(location, modifier, onClick)`: a `MaplibreMap` about 140 dp high, rounded corners, gestures off, one pin, zoom about 15, same map style as the Schneaggmap.
- Shown in `EventJoinPopup` and in the creator's `EventEditPopup` when `Event.location != null`.
- Tap closes the popup and opens the Schneaggmap centered on the event (needs a camera target on the map route).
- Risks to check on all three platforms: a native map view inside a scrolling `Dialog` (Android `AndroidView` clipping, iOS UIKit view in a dialog, desktop runtime). If a live map inside a dialog is unstable on a platform, fall back to the "Show on map" button there.

## Part F: Participant names (client)

- Replace the avatar rows in `EventParticipationOverview` with grouped rows: avatar + name (+ time of the answer). Groups: Going, No answer yet (new: invited users without a participation), Seen, Not interested.
- Each group shows its count in the header and the first 5 rows; "Show all (N)" expands it.
- Used in the join popup and the edit popup. Strings in values, values-de, values-it.
- Names: a stored friend's name first, else the name the server sends with the invited user or answer (`userName`), else "Unknown user". Needs the server field (step 2b).
- Fallback if it gets too long or busy: keep the avatar rows and open a popup like the poll details on tap.

## Order and effort

| Step | Content | Size | Needs server |
|---|---|---|---|
| 1 | Part D plumbing (id, tap target). **Done 2026-11-08** (Android, iOS push taps; desktop has no tap routing at all) | S-M | no |
| 2 | Part F names (**done 2026-11-08**, local names only), Part E map preview (open) | M | no |
| 2b | Server names in the name list | S | yes |
| 3 | Part C system messages (client texts) | S | yes |
| 4 | Part B change pushes (client) | M | yes |
| 5 | Part A reminders (picker, field, display) | M | yes |
| 6 | Device tests on Android, iOS, desktop | - | - |

Steps 1 and 2 can start at once. Steps 3-5 start when the server agent confirms its contract (field names, push types). The server work goes to the live server session by message, or into `SchneaggchatV3server/Todo.md` if none is live.

## Server contract (from the server session, 2026-11-08)

Built on the server, not committed or deployed yet. Old app versions get no special handling: the client release ships first, then the server. Accepted by the client unchanged.

**Server-side decisions:** change alerts go to Going users plus invited users without any participation (not SEEN, not DISMISSED, never the actor). Missed reminders (server down): only the due one with the shortest lead time is sent. A save that moves the start and also changes the title posts the specific message(s) plus `EVENT_CHANGED`.

**Reminders**
- `EventRequest.reminderMinutes: List<Int>?`: missing or null keeps the stored list, `[]` clears it.
- Validation (HTTP 400): at most 20 raw entries and 5 after merging duplicates; each entry 5..20160 and a multiple of 5. The server merges duplicates and sorts longest first.
- `EventResponse.reminderMinutes: List<Int>`, always present, empty = none.
- A reminder already past when set is skipped, never sent late. A moved start resets all reminders. Scheduler every 60 s, only before the start. Recipients: Going + creator, all devices.
- Socket `_class: "eventreminder"`: `eventId: String`, `eventTitle: String`, `startDate: Long` (epoch ms), `leadMinutes: Int`.
- Push `_class: "event_reminder"`: same 4 fields as strings in the data map; TTL ends at the start. APNs fallback (English): title "Reminder: <title>", body "Starts in 1 day" / "Starts in 2 h 30 min".

**Change alert**
- `changes`: `START` (startDate changed), `LOCATION` (set, changed or removed), `CANCELLED` (DELETE /events/delete with deleteevent=true). Only for events not ended yet. One alert per save.
- Socket `_class: "eventupdatealert"`: `eventId`, `eventTitle`, `changes: List<String>`, `startDate: Long` (new start; for CANCELLED the event's start), `actorName: String`. The normal `eventchange` with the event data still comes too.
- Push `_class: "event_update"`: same fields; `changes` is a JSON array string in the data map, e.g. `["START","LOCATION"]`. TTL ends at the event end. APNs fallback body: "The event was cancelled" / "New start time and location" / "The start time changed" / "The location changed"; title = event title.

**System messages** (`SystemEventResponse`)
- New `eventType`: `EVENT_START_CHANGED` (new field `timestamp: Long?` = new start, epoch ms), `EVENT_LOCATION_CHANGED`, `EVENT_CANCELLED`. `text` = event title on all; `actorId` / `actorName` as before.
- `timestamp` is new on every SystemEventResponse, null except on `EVENT_START_CHANGED`.
- `EVENT_CANCELLED` only when the group survives the delete and the event has not ended. `EVENT_CHANGED` stays for every other field. A reminder-only edit posts no system message.
- Example: start + location + title in one save = 3 messages.

**Names**
- `EventParticipationResponse.userName: String`.
- `EventResponse.invitedUserNames: List<{userId: String, userName: String}>`, same users and order as `invitedUsers`.
- Deleted or unknown user: `userName = ""`, show "Unknown user". Sent everywhere an EventResponse goes (sync, upsert, join, participation, socket `eventchange`).

## Build status (2026-11-08)

- Done: Part D (per-event id, tap target), Part F (name list, local names), Part E (map preview in join and edit popup; join popup tap opens the Schneaggmap at the event via `Route.Schneaggmap.focusLocation`, edit popup tap opens the location picker so unsaved edits survive).
- Done too: Part C system message texts (`EVENT_START_CHANGED` with `timestamp`, `EVENT_LOCATION_CHANGED`, `EVENT_CANCELLED`); server names in the name list (`EventParticipation.userName`, `Event.invitedUserNames`, DB version 92); no new-event notification on the creator's own devices (server request).
- Done too: Part B alerts (socket `eventupdatealert`, push `event_update`, Android via the shared resolver, iOS NSE texts), Part A (reminder section in the edit popup with presets + custom days/hours/minutes, `reminderMinutes` in request/response/Room with DB version 93, past reminders dropped on save, socket `eventreminder` + push `event_reminder`). Every new alert falls back to a plain notification when its text can't be built, and an unknown `changes` value shows the generic "updated the event".
- 2026-11-08, user: the event join popup is replaced by a full screen `Route.EventDetail(eventId)` (`C/events/presentation/detail/`) for guests and the creator; the creator gets an Edit button that opens the edit popup on the events screen. Add to calendar sits next to the date, only Not interested and Join / Open chat / Accept stay at the bottom. Every way into an event (calendar, map, chat details, chat links, notification taps) opens this screen.
- Open: device tests on Android, iOS and desktop.
