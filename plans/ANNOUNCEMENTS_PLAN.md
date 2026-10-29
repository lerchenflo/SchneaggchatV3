# Admin Announcements Plan

Admins (server role `ADMIN`) create global announcements from the developer settings. Every user except the Play Store / App Store test account sees the newest active announcement as a dismissible banner at the top of the chat list, in their own language. A dismissed announcement stays hidden after a restart and on all other devices of the same account. Every announcement has an expiry date after which it disappears on its own.

Design in one line: like the changelog and the contribute popup. Announcements carry a running number; the synced settings store the number of the last dismissed one; on start the app asks the server for the newest active announcement above that number. No local table, no sync job, no new socket messages.

Analysis date: 2026-10-16. Client repo `SchneaggchatV3` (branch `macci`), server repo `SchneaggchatV3server` (branch `macm4`). Both repos change.

## Decisions (2026-10-16)

| # | Question | Decision |
|---|---|---|
| 1 | Translations | German is required, English / Vori / Italian are optional. Fallbacks: Vori uses German; Italian uses English, then German; English uses German. Entered by hand, no auto-translation. |
| 2 | Push notification on create | None. Banner only. |
| 3 | Managing announcements | Create, list (active and expired), edit, delete. |
| 4 | Edit and dismissals | An edit never shows the announcement again to users who already dismissed it (the number stays the same). |
| 5 | Recognising the test account | By username `testaccount`, the name `UserService.ensureTestaccount()` creates it with. |
| 6 | Storage of the read state | No sync. A running number per announcement plus `lastDismissedAnnouncementId: Long` in the synced settings, like `lastContributePopupShown`. |
| 7 | How many at once | Only one: the newest active announcement. Dismissing it also hides every older one. |
| 8 | When the app checks | On app start, like the changelog. |
| 9 | Admin flag in the app | Never. The role stays in the server database and is never sent to or synced with the app. |
| 10 | Where admins create announcements | An option inside the developer settings that opens a popup, no new screen or route. Everyone with developer settings enabled can open it and see the form; the server rejects every request from a non-admin (404) and the app shows an error. Normal users don't have developer settings enabled anyway. |
| 11 | Dismissal while offline | No retry logic. The setting is sent once like every other setting; if that fails, the next sync restores the server's number and the announcement simply shows once more. |

Defaults taken without a question (change them before implementation if needed):

- Plain text only. Title required (max 80 characters), text optional (max 500 characters) per language. A language counts as filled when its title is filled. No link, no button.
- Every user except the test account sees it. No targeting by platform or app version.
- Any admin may edit or delete any announcement.
- Expiry is required: in the future and at most 365 days ahead, on create and on edit. To end one early, delete it.
- Delete is a hard delete. Numbers are never reused.
- New accounts start at 0 and see the announcement that is currently active.
- No automatic cleanup of expired announcements (tiny data). Noted in `plans/Todo.md`.
- Optional, not planned: also checking when the app returns to the foreground (one small request), so a new announcement shows without a restart.

## Accepted limits

- Only the newest announcement is ever shown. If two are created while a user is away, that user only sees the newer one; the older one is covered by the dismissal of the newer one.
- A new announcement appears at the next app start, not live.
- Offline at start: no banner (nothing is cached). It shows at the next start with a connection.
- Dismissed while offline: the dismissal is not retried. The next settings sync restores the server's number and the announcement shows once more (decision 11).

## Current state

- Server: `core/security/AdminGuard.kt:22` `requireAdmin()` loads the user and checks `User.role == UserRole.ADMIN` (`user/usermodel/User.kt:51`), answering 404 to non-admins. The role is not part of any user response, and stays that way (decision 9).
- Server: the test account is seeded in `user/UserService.kt:58` with the literal `"testaccount"`, only when `apns.debug` is true (`core/MainController.kt:105`).
- Server: per-language texts already exist for the FAQ (`website/faq/model/FaqEntry.kt`: `german` required, `austrian` / `english` optional). Announcements follow the same shape plus Italian.
- Server: `POST /users/settings` (`user/UserController.kt:242`) is a partial update (null = unchanged) and ends with `notificationService.notifyUserUpdate(...)` (`user/UserService.kt:~502`), so every other online device of the account gets the new settings over the socket (`UserChange`).
- Client: synced settings are `settings/domain/SyncedSettings.kt`, applied by `DataStoreSettingsRepository.applyServerSettings` (`settings/data/DataStoreSettingsRepository.kt:51`) from `/users/sync` and the `UserChange` socket push. Server always wins there.
- Client: a setting is written locally, then sent once; a failure is ignored (`DataStoreSettingsRepository.kt:40-43`, e.g. `setLastContributePopupShown` at `:158`). An offline change is therefore overwritten by the next sync. The announcement number needs protection against that (see S4, C2).
- Client: the start check for changelog and contribute popup lives in `ChatSelectorViewModel` (`chat/presentation/chatselector/ChatSelectorViewModel.kt:270-300`).
- Client: developer settings are unlocked by anyone with 8 taps on the version text (`settings/presentation/SettingsScreen.kt:249-300`). The app does not hide the form from non-admins; the server's `requireAdmin()` is the only check (decision 10).
- Client: the chat list shows info banners with `sharedUi/ChatSelectorDismissableInfo.kt` (`chat/presentation/chatselector/ChatSelector.kt:692-735`). Its button (`activateText`, `onClick`) is required today.
- Client: languages are `settings/domain/LanguageSetting.kt` (SYSTEM, GERMAN, ENGLISH, VORI, ITALIAN); `LanguageService.getSystemLanguageSetting()` resolves SYSTEM.

## Server (`SchneaggchatV3server`)

### S1 Test account constant
`UserService`: move `"testaccount"` into a constant (e.g. `TESTACCOUNT_USERNAME`) and use it in `ensureTestaccount()` and in the announcement service.

### S2 Model, numbering and repository
New package `announcement/`, repository in `repository/` as usual.

- `Announcement` in collection `announcements`: `id: Long` (the running number), `german: AnnouncementText` (required), `austrian`, `english`, `italian: AnnouncementText?`, `expiresAt: Instant`, `createdBy: ObjectId`, `createdAt`, `updatedAt`.
- `AnnouncementText(title: String, body: String)`.
- Running number: one counter document (e.g. collection `counters`, `_id = "announcements"`), incremented atomically with `findAndModify` + `$inc` + upsert. Never decremented, so a deleted number is never handed out again.
- Index on `expiresAt` (or `id` descending + `expiresAt`) for the "newest active" query.

### S3 Endpoints
`AnnouncementController` under `/announcements`, with `AnnouncementService` + `AnnouncementLookupService`.

| Method | Route | Guard | Does |
|---|---|---|---|
| GET | `/announcements/latest` | `requireAuth()` | `{ "announcement": AnnouncementResponse \| null }`: the newest announcement with `expiresAt > now` and `id > caller's settings.lastDismissedAnnouncementId`, all languages included. `null` for none and always for the test account. |
| GET | `/announcements/admin/list` | `requireAdmin()` | `List<AnnouncementResponse>` including expired ones, newest first, with `creatorName`. |
| POST | `/announcements/admin/create` | `requireAdmin()` | Body `AnnouncementRequest` (`id` null). Create with the next number. Validates texts and expiry (see defaults). |
| POST | `/announcements/admin/update` | `requireAdmin()` | Body `AnnouncementRequest` with `id`. Edit texts and expiry, bumps `updatedAt`, keeps the number. |
| DELETE | `/announcements/admin/delete?id=` | `requireAdmin()` | Hard delete. |

Shapes (as implemented in the app's `announcement/data/remote/dto/`; JSON `austrian` is the app's Vori, times are epoch millis): `AnnouncementResponse(id: Long, german: {title, body}, austrian?, english?, italian?, expiresAt, createdAt, updatedAt, creatorName?)`, `AnnouncementRequest(id: Long?, german, austrian?, english?, italian?, expiresAt)`.

The server comparing against the stored number (not only the app) avoids a race: a second device that starts before its settings sync arrived would otherwise briefly show an announcement already dismissed elsewhere.

Validation goes into `util/ValidationUtils.kt` like the FAQ texts. Text is trimmed; blank optional languages are stored as null.

### S4 Settings field
- `PersonalUserSettings.lastDismissedAnnouncementId: Long = 0`, in `UserSettingsRequest` (nullable) and in the settings response.
- `UserService.updateSettings`: stores `max(current, new)`, so an outdated device can never lower it (cheap safety; the app itself does no retries, decision 11).
- No migration needed: missing field = 0.

### S5 No admin flag for the app
`User.role` is not added to `SelfUserResponse` or any other response (decision 9). Admin rights are granted by hand in MongoDB as today. Every admin route goes through `requireAdmin()`, which answers 404 to everyone else.

## Client (`SchneaggchatV3`)

Run the code-reuse-scout agent with the function list below before implementing.

Status 2026-10-16: C1-C5 implemented (uncommitted), compiled for JVM, Android, iOS and the iOS tests. Not run on a device; tests not run.

### C1 Domain and remote (done)
New feature package `announcement/`, built like `feedback/`:

- `announcement/domain/Announcement.kt`: `Announcement` (`id: Long`, `german`, `vori?`, `english?`, `italian?`, `expiresAt`, `createdAt`, `updatedAt`, `creatorName?`), `AnnouncementText`, `AnnouncementDraft`, `AnnouncementLimits` (80 / 500 / 365 days).
- `Announcement.textFor(language)`: the fallbacks from decision 1.
- `announcement/domain/AnnouncementRepository.kt`: `getLatest()`, `getAll()`, `create()`, `update()`, `delete()`, returning `NetworkResult` like `FeedbackRepository`.
- `announcement/data/RemoteAnnouncementRepository.kt`, `data/remote/` (`AnnouncementRemoteDataSource`, `KtorAnnouncementRemoteDataSource`), `data/remote/dto/` (DTOs + mapper; the mapper trims texts and drops translations without a title). Registered in `di/Modules.kt`.

### C2 Settings field (done)
- `SyncedSettings`, `PersonalUserSettings`, `UserSettingsRequest`, the mapper, `Preferencemanager`: `lastDismissedAnnouncementId: Long`.
- Server wins in `applySyncedSettings`, like every other synced setting. `setLastDismissedAnnouncementId(id)` writes locally, then sends once (decision 11).

### C3 Chat list banner (done)
- `ChatSelectorViewModel.loadAnnouncement()`: after login, once per ViewModel, calls `getLatest()`. Non-network failures are logged.
- `ChatSelectorViewModel.announcement`: `combine(latest, lastDismissedAnnouncementId flow)`, shown while `id > lastDismissed`; a `delay` until `expiresAt` hides it while the app stays open. A dismissal on another device (arriving as `UserChange`) hides it at once.
- `chat/presentation/chatselector/AnnouncementBanner.kt` (with preview), placed below the notification permission banner. Language: the locale the string resources use (`Locale.current` through `LanguageSetting.fromIsoCode`); an untranslated system language counts as English, like the string resources. No existing "current app language" helper existed to reuse.
- `ChatSelectorDismissableInfo`: `activateText` / `onClick` optional, new `icon` parameter.

### C4 Announcements popup in the developer settings (done)
- No new screen and no new route (decision 10). `DeveloperSettings` gets a `SettingsOption` "Announcements" that opens `AnnouncementAdminPopup`, with its own `AnnouncementAdminViewModel` (MVI: `AnnouncementAdminState`, `AnnouncementAdminAction`), in `settings/presentation/devsettings/announcements/`.
- Shown to everyone who has developer settings enabled. No admin check in the app.
- List: all announcements (expired ones marked), number, title, expiry, creator, edit and delete (with confirmation). "New" opens the form.
- Form: German (required), English, Vori, Italian; title + text with counters. Expiry presets 1 / 7 / 30 days plus a date-time wheel picker (max 365 days). Preview per language with the banner's look and fallbacks.
- Errors show inside the popup (a snackbar would be hidden behind it). NotFound from an admin route shows "Only admins can manage announcements".

### C5 Finish
- New UI strings in `values/`, `values-de/`, `values-it/` (not `values-de-rAT`). Done.
- README changelog (readme-changelog-updater), string-resource-extractor.
- `plans/Todo.md`: cleanup of expired announcements; optional foreground check.

## Order

1. S1-S5 on the server.
2. C1-C2 data layer and settings field.
3. C3 banner.
4. C4 popup in the developer settings.
5. C5.

Server tasks are also tracked in the server repo's `Todo.md` (section "Admin announcements").

## Manual test checklist

- Admin creates an announcement: other accounts see it at their next app start.
- Each language: German, English, Vori, Italian, system language; missing translations fall back as in decision 1.
- Dismiss on device A while device B is open: banner on B disappears at once (settings over the socket).
- Dismiss on device A, then start device B: no banner, not even briefly.
- Dismiss while offline: the announcement shows once more after the next sync; dismissing it again online keeps it hidden everywhere.
- Two announcements created: only the newer one shows; after dismissing it, the older one does not appear.
- Edit: users who dismissed it don't see it again; others see the new text at their next start.
- Delete and expiry: banner disappears at the next start (delete) and at `expiresAt` while open (expiry).
- Non-admin with developer settings unlocked: sees the popup and the form, but list, create, edit and delete all fail with the "Only admins" message; nothing is stored on the server.
- No user response (`/users/sync`, `UserChange`) contains the role.
- Test account: never sees an announcement.
