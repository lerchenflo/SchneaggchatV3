# Schneaggchat V3

Privacy-focused chat platform built with Kotlin Multiplatform and Compose Multiplatform. Targets Android, iOS, and Desktop from a shared codebase.

## Tech Stack

| Area                  | Technology            | Version      |
|-----------------------|-----------------------|--------------|
| Language              | Kotlin                | 2.3.20       |
| UI                    | Compose Multiplatform | 1.10.3       |
| DI                    | Koin                  | 4.2.0        |
| Database              | Room + SQLite Bundled | 2.8.4        |
| Networking            | Ktor Client           | 3.5.2        |
| Local Storage         | DataStore             | 1.2.1        |
| Navigation            | Jetpack Navigation 3  | 1.1.0-beta01 |
| Annotation Processing | KSP                   | 2.3.5        |
| JVM                   | 21                    |              |
| Android Target SDK    | 37                    |              |
| Android Min SDK       | 26                    |              |

## Project Structure

```
:composeApp        — Shared KMP code (all features, UI, data, DI)
:androidApp        — Android entry point (Application class, MainActivity)
iosApp/            — iOS Xcode project wrapper
```

Feature-based package organization under `composeApp/src/commonMain/kotlin/`:

```
app/               — App setup, navigation, theme, logging
chat/              — Messaging (data/domain/presentation)
login/             — Authentication (data/domain/presentation)
settings/          — User settings
games/             — Games feature
schneaggmap/       — Map feature
events/            — Events (data/domain/presentation)
feedback/          — User feedback (data/domain/presentation)
announcement/      — Server announcements (data/domain)
tools/             — Tools screen (domain/presentation)
sharedUi/          — Shared UI components
datasource/        — Database, network, preferences
di/                — Koin DI modules
utilities/         — Shared helpers
constants.kt       — App-wide constants
```

Repository interfaces + their models live in the domain/ of the feature that holds the implementation (data/); features may import each other's domain/ only.

Feature data layer: data/ (repositories), data/local/ (local data sources + entity/), data/remote/ (remote data sources + dto/).

## Build Commands

```bash
./gradlew assembleDebug          # Android debug build
./gradlew runDesktop             # Run desktop app
./gradlew build                  # Full build (all targets)
```

## Branch Workflow

- Claude always works on the current checked-out branch in place — never create or switch to a new branch on your own
- PRs merge into `main`

## Code Conventions

- **All new code must be in English** — variable names, function names, comments, string keys. Do not rename existing German code unless you are actively modifying that file.
- **Always use the version catalog** (`gradle/libs.versions.toml`) for all dependencies. No hardcoded versions in build files.

## Wiring checklists

Steps that are easy to miss and do not fail the compile. Whenever a missed step is found (in a review, a bug or a crash), add it here.

- **New route**: `Route` subtype in `app/navigation/Route.kt`; `subclass(...)` in the `NavKey` polymorphic module in `app/navigation/NavigationState.kt` (otherwise saving the back stack crashes); `entry<Route.X>` in `app/App.kt`; decide whether it hides the nav bar (`hiddenNavBarRoutes` in `NavigationState.kt`); ViewModel in `di/Modules.kt`.
- **New socket message**: `SocketConnectionMessage` subtype with `@SerialName`; `subclass(...)` in `datasource/network/AppJson.kt` (otherwise it is silently dropped); branch in `handleSocketConnectionMessage`; defaults on its fields so a missing field doesn't drop it.
- **New push type (`_class`)**: `DecodedNotification` + `PayloadDecoder.decode`; both `resolveLocalizedContent` and `resolveFallbackContent`; never return null content for something the user must see (Android shows nothing then); iOS extension `NotificationPayload.swift` (`type`), `NotificationService.swift` and `NotificationStrings.swift` (en/de/it); tap target in `IosPushDelegateBridge.onNotificationTap` and the Android launch intent extras (`AndroidNotifier.launchIntent`, `MainActivity`).
- **New stored field**: Room entity, both entity mappers, DTO mapper, a `TypeConverter` for new types, and a `Database.kt` version bump.

## Architecture Skills

Follow the architecture skills installed in `~/.claude/skills/`. These define the patterns for this project:

| Skill | What it covers |
|---|---|
| `android-dependency-setup` | Version catalog, bundles, KSP wiring, base/optional dependencies |
| `android-module-structure` | Module layout, dependency rules, direct declarations per module |
| `android-di-koin` | Koin modules, scoping, assembly, koinViewModel() |
| `android-data-layer` | Repositories, data sources, DTOs, mappers, Ktor client, Room |
| `android-presentation-mvi` | State, Action, Event, ViewModel, Root/Screen composable split |
| `android-navigation` | Type-safe routes, feature nav graphs, cross-feature callbacks |
| `android-compose-ui` | Stability, recomposition, animations, previews, accessibility |
| `android-error-handling` | Result wrapper, DataError, safe call helpers, UiText mapping |
| `android-testing` | JUnit5, Turbine, AssertK, fakes, Compose UI tests |

When a task matches a skill, read and follow it.

## Guardrails & Agent Rules

- **Always respond in English**, no matter which language the user writes in (German, Vorarlbergerisch, etc.).
- **Never hardcode colors**: Always use `MaterialTheme.colorScheme` colors (or `contentColorFor(...)`). If a specific non-theme color is needed, ask the user first.
- **String resources**: Whenever a string is added OR its value is changed in default `strings.xml` (in `values/`), redo the translation in `values-de/strings.xml` (German) and `values-it/strings.xml` (Italian) to match. Never add or change strings in `values-de-rAT` (Austrian) — that locale is translated manually. The only allowed edits there are deletions: always delete keys that no longer exist in the default `values/strings.xml` (orphaned keys), whenever a key is removed, and always delete a key whose value was changed in `values/strings.xml`, so no outdated Austrian text stays behind (the key then shows up as missing for the `vori-translator` skill). Exception: when the user explicitly invokes the `vori-translator` skill, write its Vori translations straight into the file for the user to review with `git diff`, then follow the skill's learning loop.
- **Gradle — compile tasks only, and only when not on a laptop**: You may run the compile tasks (`:composeApp:compileKotlinJvm`, `:composeApp:compileAndroidMain`, `:composeApp:compileKotlinIosSimulatorArm64`, `:composeApp:compileTestKotlinIosSimulatorArm64`) freely to check your changes, but only if the machine is not a laptop. Check first: macOS `pmset -g batt | grep -q InternalBattery` (match = laptop); Linux `ls /sys/class/power_supply/BAT*` (exists = laptop); Windows `Get-CimInstance Win32_Battery` (any result = laptop). On a laptop, and for every other Gradle task (tests, `assemble*`, `run*`, `install*`, `build`, publish), do not run `./gradlew` unless explicitly requested by the user.
- **Do not assume anything**: If an instruction or requirement is ambiguous or unclear, ask the user for clarification before proceeding.
- **Use available skills**: Always check and apply relevant skills from the skills directory when working on tasks.
- After adding or changing dependencies: remind the user to verify versions are up to date and run a Gradle sync. Do not build or run the project yourself (compile checks per the Gradle rule above are fine).
- Do not create documentation files (README, ARCHITECTURE, etc.) unless explicitly asked.
- Do not refactor or restructure code you were not asked to touch.
- Do not add tests unless explicitly asked.
- **Server repo**: change code only in this repo, never in the server repo `SchneaggchatV3server` (usually `~/Desktop/SchneaggchatV3server`); reading it is fine. When a client change the user approved needs a server change: if a `SchneaggchatV3server` session is live (`ListAgents`), send it with `SendMessage`; otherwise add it to `SchneaggchatV3server/Todo.md`, the only server file agents may write to. Describe the functionality and the exact field names the client sends or reads, not the server's structure; the server agent designs its side and replies with the contract it built. See "Client and server projects" in the global `~/.claude/CLAUDE.md`.
- **Todo list — no thought gets lost**: Whenever you notice something worth doing that is outside the current task (bugs, tech debt, stale comments, deferred refactors, open questions, follow-ups from a plan), add it to `plans/Todo.md` with its source and file:line. Do not fix it unasked; the user reviews the list.
- Run the readme-changelog-updater agent after every fix (Add even small fixes, the user will remove them again if unnecessary.)
- Before implementing new functions, utils, repositories, or mappers, run the code-reuse-scout agent with the planned function list (name + one-line intent per item) so it can research existing code to reuse or extend first. It reports a REUSE/EXTEND/NEW verdict per item and maintains `.claude/code-index.md`; it never edits source code itself.
- **Never look at the `games/` folder for reference** (structure, functions, patterns, reuse candidates, etc.) when working on anything outside of `games/` — the code there is low quality and must not be copied or mirrored elsewhere. Only read it when the task is actually about the games feature.