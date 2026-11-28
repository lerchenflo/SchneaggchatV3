# Privacy Setup Plan

Written 2026-11-26. Client only (`SchneaggchatV3`); no server change (every card writes a setting that already exists). Todo: `plans/Todo.md`, "Privacy setup".

Paths: `C/` = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/`.

**Status:** plan only. Waiting for the notification areas feature (not built yet); build this together with or after it (user, 2026-11-26).

---

## 1. Goal

Sign-up stays as small as it is today (username, email, birthday, profile picture, password, terms). The basic privacy and feature settings move into an optional **privacy setup**: a card flow like the register screen (`SwipeableCardView`), one setting per card. Users who never saw some settings (older accounts, new settings added later) find it as a large card at the top of the settings screen, which is highlighted while the setup has cards they have not seen yet.

---

## 2. Decisions (user, 2026-11-26)

| # | Decision |
|---|---|
| D1 | Cards: location sharing (map), police/radar alerts, profile picture visibility, notifications + wake, save received images to the gallery (added 2026-11-28). More cards are added later; notification areas are next, once that feature exists. |
| D2 | Optional only: never started on its own (not after sign-up), only from the settings screen. |
| D3 | Each change is saved right away, like in the normal settings. Leaving halfway keeps what was set. |
| D4 | Highlight: a hardcoded list of app versions that added cards. The settings card is highlighted while the user has not opened the setup since the newest listed version. Opening the setup clears it. |
| D5 | Look: large card at the top of the settings screen (above user settings). Highlighted = `primaryContainer` background + "New" chip; normal look otherwise. No nav bar badge. |
| D6 | Onboarding tour: a tag-based step (own tag) on the settings card, shown once to users who have not seen it when the settings screen opens; also in the full tour. |
| D7 | Flow has an intro card (what comes, privacy first) and a finish card ("All set" + finish button). |
| D8 | Own ViewModel for the setup. |

---

## 3. Design

### 3.1 Domain (`C/settings/domain/`)

- `PrivacySetupCard` enum: `INTRO`, `LOCATION_SHARING`, `MAP_SIGHTING_PUSH`, `PROFILE_PIC_VISIBILITY`, `NOTIFICATIONS`, `SAVE_RECEIVED_IMAGES`, `FINISH` (later `NOTIFICATION_AREAS` as the first step). The order of the enum is the order of the cards.
- `PrivacySetupRevision(version: String, addedCards: Set<PrivacySetupCard>)` and the hardcoded list `PRIVACY_SETUP_REVISIONS` (first entry: the version this ships in, all setting cards). Adding a card later = append an entry with the new version.
- `isPrivacySetupHighlighted(seenVersion: String?): Boolean` = `seenVersion != PRIVACY_SETUP_REVISIONS.last().version`. Plain string equality, no version compare needed (no compare helper exists in the code), because the list only holds versions up to the running build. Never opened (`null`) = highlighted.
- Optional: `newCardsSince(seenVersion)` so the setup can mark cards added since the last visit with a "New" chip (see open question Q3).

### 3.2 Data

- `SettingsRepository` + `DataStoreSettingsRepository`: `getPrivacySetupSeenVersionFlow(): Flow<String?>`, `setPrivacySetupSeenVersion(version: String)`; key `privacy_setup_seen_version` in `C/datasource/preferences/Preferencemanager.kt`.
- Local per device (like `onboarding_seen_tour_tags`), see Q1.
- "Reset onboarding" in the misc settings does not clear it (see Q2).

### 3.3 Structure first: shared setting content and save logic

The setup must not copy the settings screens. Before the setup is built:

- Pull the dialog bodies out into content composables that both the dialog and the setup card use:
  - `LocationSharingDialog` (`C/settings/presentation/schneaggmapsettings/LocationSharingDialog.kt:49`) -> `LocationSharingContent` (global switch, advanced per-friend sharing).
  - `ProfilePicVisibilityDialog` (`C/settings/presentation/privacyandsecurity/ProfilePicVisibilityDialog.kt:47`) -> `ProfilePicVisibilityOptions`.
  - Notifications on/off + permission and wake (`C/settings/presentation/notificationsettings/NotificationSettings.kt`) -> content composable(s).
  - Save received images to the gallery: the `SettingsSwitch` in `C/settings/presentation/miscSettings/MiscSettings.kt:92` with its title / info strings (`save_received_images_to_gallery`, `save_received_images_to_gallery_info`), saved over `SettingsRepository.setSaveReceivedImagesToGallery` (no ViewModel logic to move; the card reads `getSaveReceivedImagesToGalleryFlow`). Per device, default on; the gallery start time is not touched by the card.
- Move the save logic that sits in the ViewModels today into the domain, so both ViewModels call the same function: location sharing writes (`SchneaggmapSettingsViewModel.kt:175`, global + per-friend drafts over `userRepository`), wake writes (`NotificationSettingsViewModel.kt:122`), permission request (`NotificationSettingsViewModel.kt:102`). Run code-reuse-scout (or Explore, the agent is missing) with this list first.

### 3.4 Presentation (`C/settings/presentation/privacysetup/`)

- `PrivacySetupState`, `PrivacySetupAction`, `PrivacySetupEvent`, `PrivacySetupViewModel`, `PrivacySetupScreenRoot` / `PrivacySetupScreen` (MVI skill).
- ViewModel: loads the current values (user repository, settings repository, permission manager), saves each change right away (D3), sets the seen version to `PRIVACY_SETUP_REVISIONS.last().version` once on open (D4), errors through `ErrorChannel`.
- Screen: `ActivityTitle` + `SwipeableCardView` (`C/sharedUi/SwipeableCardView.kt:53`), one `CardItem` per `PrivacySetupCard`, `canContinue = { true }`, `finishEnabled = true`, finish and back navigate back.
- The setting cards reuse the content composables from 3.3.

### 3.5 Settings screen

- `SettingsViewModel` exposes `isPrivacySetupHighlighted` (from the seen version flow).
- New `PrivacySetupEntryCard` composable at the top of `C/settings/presentation/SettingsScreen.kt`: title + short text, `primaryContainer` + "New" chip when highlighted, theme colors only.

### 3.6 Wiring (CLAUDE.md "New route")

- `Route.PrivacySetup` in `C/app/navigation/Route.kt`; `subclass(...)` in `C/app/navigation/NavigationState.kt`; nav bar hidden (`hiddenNavBarRoutes`); `entry<Route.PrivacySetup>` in `C/app/App.kt`; ViewModel in `C/di/Modules.kt`.

### 3.7 Tour step (D6)

- New tag `TourTags.PRIVACY_SETUP` plus the settings screen tag, highlight step on `PrivacySetupEntryCard` in `C/app/onboarding/onboardingtour.kt`, also part of the settings section of the full tour.

### 3.8 Strings

- New keys in `values/strings.xml` (card titles, texts, intro, finish, settings card, "New" chip, tour step), German and Italian in `values-de/` and `values-it/`. Nothing in `values-de-rAT` (vori-translator lists them as missing).

---

## 4. Steps

1. Structure: content composables + domain save functions (3.3); settings screens keep working the same (device check).
2. Domain + data (3.1, 3.2).
3. ViewModel + screen + route wiring (3.4, 3.6).
4. Settings entry card + highlight (3.5).
5. Tour step (3.7), strings (3.8), readme-changelog, `plans/Todo.md`.
6. Later: notification areas card (new `PrivacySetupCard`, new `PRIVACY_SETUP_REVISIONS` entry).

---

## 5. Open questions (ask before building)

- **Q1** Seen version per device (DataStore) or per account (server, same on every device)? Plan assumes per device.
- **Q2** Should "Reset onboarding" (misc settings) also clear the seen version, so the setup is highlighted again?
- **Q3** Inside the setup: mark cards added since the last visit with a "New" chip, or start at the first new card?
- **Q4** Location sharing per-friend toggles with "save right away": one server call per toggle, or save when the user leaves the card?
- **Q5** Notifications card on desktop (no permission, no wake): hide the card there?
- **Q6** Profile picture itself (change picture) as a card too, or only its visibility?
- **Q7** Gallery card on desktop (copies go to `~/Pictures/Schneaggchat`): show it there too, or mobile only? And does the card need the photo permission flow on Android / iOS (saving to the gallery), or is that asked on the first save?
