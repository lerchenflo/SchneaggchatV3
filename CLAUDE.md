# Schneaggchat V3

Privacy-focused chat platform, Kotlin Multiplatform + Compose Multiplatform. Targets Android, iOS, Desktop from shared codebase.

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
| Android Min SDK       | 29                    |              |

## Project Structure

```
:composeApp        — Shared KMP code (all features, UI, data, DI)
:androidApp        — Android entry point (Application class, MainActivity)
iosApp/            — iOS Xcode project wrapper
```

Feature-based packages under `composeApp/src/commonMain/kotlin/`:

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

Repository interfaces + their models live in domain/ of feature holding implementation (data/); features may import each other's domain/ only.

Feature data layer: data/ (repositories), data/local/ (local data sources + entity/), data/remote/ (remote data sources + dto/).

## Build Commands

```bash
./gradlew assembleDebug          # Android debug build
./gradlew runDesktop             # Run desktop app
./gradlew build                  # Full build (all targets)
```

## Branch Workflow

- Work on current checked-out branch in place — never create or switch branch on your own
- PRs merge into `main`

## Code Conventions

- **Most important for every change: structure and readability over extended functionality.** Bad structure makes everything else about change useless. Put code in layer and package it belongs to (domain logic in `domain/`, not in UI files), keep functions small and named for what they do, follow architecture skills. Feature only possible with bad structure: fix structure first or ask user. *(user, 2026-11-11)*
- **All new code in English** — variable names, function names, comments, string keys. Never rename existing German code unless actively modifying that file.
- **Always use version catalog** (`gradle/libs.versions.toml`) for all dependencies. No hardcoded versions in build files.

## Wiring checklists

Steps easy to miss that don't fail compile. Missed step found (review, bug, crash): add it here.

- **New route**: `Route` subtype in `app/navigation/Route.kt`; `subclass(...)` in `NavKey` polymorphic module in `app/navigation/NavigationState.kt` (else saving back stack crashes); `entry<Route.X>` in `app/App.kt`; decide whether it hides nav bar (`hiddenNavBarRoutes` in `NavigationState.kt`); ViewModel in `di/Modules.kt`.
- **New socket message**: `SocketConnectionMessage` subtype with `@SerialName`; `subclass(...)` in `datasource/network/AppJson.kt` (else silently dropped); branch in `handleSocketConnectionMessage`; defaults on its fields so missing field doesn't drop it.
- **New push type (`_class`)**: `DecodedNotification` + `PayloadDecoder.decode`; both `resolveLocalizedContent` and `resolveFallbackContent`; never return null content for something user must see (Android shows nothing then); iOS extension `NotificationPayload.swift` (`type`), `NotificationService.swift` and `NotificationStrings.swift` (en/de/it); tap target in `IosPushDelegateBridge.onNotificationTap` and Android launch intent extras (`AndroidNotifier.launchIntent`, `MainActivity`).
- **New stored field**: Room entity, both entity mappers, DTO mapper, `TypeConverter` for new types, `Database.kt` version bump.

## Architecture Skills

Follow architecture skills in `~/.claude/skills/`. They define this project's patterns:

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

Task matches skill: read and follow it.

## Guardrails & Agent Rules

- **Always respond in English**, whatever language user writes (German, Vorarlbergerisch, etc.).
- **Never hardcode colors**: always `MaterialTheme.colorScheme` colors (or `contentColorFor(...)`). Specific non-theme color needed: ask user first.
- **String resources**: string added OR value changed in default `strings.xml` (in `values/`): redo translation in `values-de/strings.xml` (German) and `values-it/strings.xml` (Italian) to match. Never add or change strings in `values-de-rAT` (Austrian) — translated manually. Only allowed edits there = deletions: always delete keys no longer in default `values/strings.xml` (orphaned keys), whenever key removed, and always delete key whose value changed in `values/strings.xml`, so no outdated Austrian text stays (key then shows as missing for `vori-translator` skill). Exception: user explicitly invokes `vori-translator` skill: write its Vori translations straight into file for user to review with `git diff`, then follow skill's learning loop. **iOS notification texts always go with it** (user, 2026-11-17): push texts also live in `iosApp/SchneaggchatNotificationServiceExtension/NotificationStrings.swift` (English, German, Italian, Austrian tables); every translation change updates them in same change (Austrian table under same rule as `values-de-rAT`, so every `vori-translator` run fills it too), because missing key there shows English on iOS.
- **Gradle — compile tasks only, only when not on laptop**: run compile tasks (`:composeApp:compileKotlinJvm`, `:composeApp:compileAndroidMain`, `:composeApp:compileKotlinIosSimulatorArm64`, `:composeApp:compileTestKotlinIosSimulatorArm64`) freely to check changes, only if machine not laptop (laptop check: global `~/.claude/CLAUDE.md` "Builds"). On laptop, and for every other Gradle task (tests, `assemble*`, `run*`, `install*`, `build`, publish), never run `./gradlew` unless user explicitly requests.
- **Do not assume anything**: instruction or requirement ambiguous or unclear: ask user before proceeding.
- **Use available skills**: always check and apply relevant skills from skills directory.
- After adding or changing dependencies: remind user to verify versions up to date and run Gradle sync. Never build or run project yourself (compile checks per Gradle rule above fine).
- No documentation files (README, ARCHITECTURE, etc.) unless explicitly asked.
- No refactor or restructure of code you were not asked to touch.
- No tests unless explicitly asked.
- **Server repo**: change code only in this repo, never in server repo `SchneaggchatV3server` (usually `~/Desktop/SchneaggchatV3server`); reading fine. Approved client change needs server change: `SchneaggchatV3server` session live (`ListAgents`), send with `SendMessage`; else add to `SchneaggchatV3server/Todo.md`, only server file agents may write. Only exception: `bump-version` skill sets `version = "..."` line in server's `build.gradle.kts`, nothing else. Describe functionality and exact field names client sends or reads, not server's structure; server agent designs its side, replies with contract it built. See "Client and server projects" in global `~/.claude/CLAUDE.md`.
- **Tour step for new features** *(user, 2026-11-19)*: building new feature or new button: interview user (AskUserQuestion) whether it needs tap target step in onboarding tour (`app/onboarding/onboardingtour.kt`), so users see what is new. Yes: build step, ask how it shows: only once to users who haven't seen it (own tag, launched first time its screen opens) or only in this version. Full tour (retake) always shows every step. Tag-based steps need kmp-taptarget 1.0.5 (see onboarding item in `plans/Todo.md`).
- **Todo list — no thought gets lost**: noticed something worth doing outside current task (bugs, tech debt, stale comments, deferred refactors, open questions, follow-ups from plan): add to `plans/Todo.md` with source and file:line. Never fix unasked; user reviews list. Item finished: tick it, move to top of `## Done` section in same change, never leave ticked in its priority section; open device test of it goes into `## Device tests` *(user, 2026-11-20)*. `## Side quests`: done only when tokens left over, never picked as next todo unless user asks *(user, 2026-11-20)*.
- Run the `readme-changelog` skill inline after every fix or feature (add even small fixes, user removes unnecessary ones); never as a subagent.
- Before implementing new functions, utils, repositories or mappers: run code-reuse-scout agent with planned function list (name + one-line intent per item) to research existing code to reuse or extend first. Reports REUSE/EXTEND/NEW verdict per item, maintains `.claude/code-index.md`; never edits source code.
- **Releases**: before creating or pushing GitHub release tag (`v*`), run `pre-release-check` skill; open blocker stops tag unless user explicitly says go ahead. *(user, 2026-11-17)*
- **Never look at `games/` folder for reference** (structure, functions, patterns, reuse candidates, etc.) when working outside `games/` — code there low quality, must not be copied or mirrored elsewhere. Read it only when task is about games feature.
