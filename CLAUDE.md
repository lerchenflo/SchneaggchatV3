# Schneaggchat V3

Privacy-focused chat platform built with Kotlin Multiplatform and Compose Multiplatform. Targets Android, iOS, and Desktop from a shared codebase.

## Tech Stack

| Area | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.3.20 |
| UI | Compose Multiplatform | 1.10.3 |
| DI | Koin | 4.2.0 |
| Database | Room + SQLite Bundled | 2.8.4 |
| Networking | Ktor Client | 3.4.2 |
| Local Storage | DataStore | 1.2.1 |
| Navigation | Jetpack Navigation 3 | 1.1.0-beta01 |
| Annotation Processing | KSP | 2.3.5 |
| JVM | 21 |  |
| Android Target SDK | 36 |  |
| Android Min SDK | 26 |  |

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
todolist/          — To-do list feature
sharedUi/          — Shared UI components
datasource/        — Database, network, preferences
di/                — Koin DI modules
utilities/         — Shared helpers
```

## Build Commands

```bash
./gradlew assembleDebug          # Android debug build
./gradlew runDesktop             # Run desktop app
./gradlew build                  # Full build (all targets)
```

## Branch Workflow

- Create a new branch for each feature
- PRs merge into `main`
- Multiple contributors work on separate branches

## Code Conventions

- **All new code must be in English** — variable names, function names, comments, string keys. Do not rename existing German code unless you are actively modifying that file.
- **Always use the version catalog** (`gradle/libs.versions.toml`) for all dependencies. No hardcoded versions in build files.

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

## Guardrails

- After adding or changing dependencies: remind the user to verify versions are up to date and run a Gradle sync. Do not attempt to build or run the project yourself.
- Do not create documentation files (README, ARCHITECTURE, etc.) unless explicitly asked.
- Do not refactor or restructure code you were not asked to touch.
- Do not add tests unless explicitly asked.
