# Games Architecture Plan

**Status: planned** (2026-10-21, branch `macci`). Not started.

Brings `games/` (~47k lines, 20 screens) in line with the rest of the app: repository and data source **interfaces** in `domain/`, implementations in `data/`, **pure game engines** behind a shared contract, a **shared game-session contract** (save / timer / score / pause) instead of the same plumbing copied into every ViewModel, and **MVI with a Root/Screen split** for every screen.

Closes the games items in `plans/Todo.md` (P4 block, lines ~153-159: domain imports DTOs, RecapViewModel injects data sources, presentation imports `games.data` / `datasource.*`).

Paths: `C/` = `composeApp/src/commonMain/kotlin/org/lerchenflo/schneaggchatv3mp/`, `G/` = `C/games/`.

---

## 0. Decisions (user, 2026-10-21)

| # | Decision |
|---|---|
| D1 | Scope: all four parts: data-layer interfaces, MVI everywhere, pure game engines, shared game contract. |
| D2 | Layout: `games/` stays **one feature**. Core code in `G/domain/`, `G/data/`, `G/presentation/`; per-game code in `G/domain/<game>/` and `G/presentation/<game>/` (same as the existing `G/domain/dartcounter/`). No `games/<game>/` sub-features. CoinFlip and FingerPicker stay in `games/`, not `tools/` (user, 2026-10-21). |
| D3 | Behavior: structure first, but **agents may fix bugs they find** while moving code. Every fix is listed in the agent's report and in §7 of this file. Fixes that need a design decision go to `plans/Todo.md` instead. |
| D4 | No new tests. |
| D5 | Schneagg Rodeo: **wiring only** (ViewModel deps, data imports, `koinInject` in the screen). `engine/`, `render/`, `vehicles/` stay as they are. |
| D6 | Result types stay `NetworkResult<T, NetworkingError>` / `EmptyResult` (project-wide D3 of the repository migration; the `Result<T, DataError>` conversion is a separate Todo). Domain interfaces may import `datasource.network.util.NetworkResult` / `NetworkingError` / `EmptyResult` only. |
| D7 | Never change what is persisted or sent: `GameId` names, `GameSaveSlot.saveKey`, DataStore key names, Room table `players`, server endpoints and enum names stay. A snapshot whose layout changes **must** bump its `*_SNAPSHOT_VERSION` (old saves are then discarded, which is fine). |

Global project rules still apply (CLAUDE.md): English for new code, `MaterialTheme.colorScheme` only (existing fixed game palettes such as `WordleColors`, `RodeoFixedColors` stay as they are), new strings go to `values/` + `values-de/` + `values-it/` (never `values-de-rAT`), no docs files, findings outside the task go to `plans/Todo.md`. The games rule ("never use `games/` as reference") is **not** broken here because this task is about games, but agents take patterns from `chat/` and the skills, never from other game files.

Skills to read before starting: `android-data-layer`, `android-presentation-mvi`, `android-di-koin`, `android-error-handling` (for D6 limits), `android-compose-ui`.

---

## 1. Current state (verified 2026-10-21)

### 1.1 Data layer
| File | Problem |
|---|---|
| `G/data/GameHighscoreRepository.kt` | Concrete class, no interface. Used by 14 ViewModels and directly by a composable (`G/presentation/GlobalRanking.kt:69`, `koinInject`). |
| `G/data/GameSaveRepository.kt` | Concrete class, DataStore + `ApplicationScope`. Used through `GameSaveSession`. |
| `G/data/CChallengeStreakRepository.kt` | Concrete class; domain model `CChallengeStreak` defined in the data file. |
| `G/data/PlayerRepository.kt` | Concrete class, takes the whole `AppDatabase`; returns Room `PlayerEntity` to presentation (`PlayerSelectorScreen.kt:59, :280`, `PlayerSelectorState.kt:4`, `PlayerSelectorViewModel.kt:14, :69`). |
| `G/data/PlayerEntity.kt` | Room entity in `data/` root instead of `data/local/entity/`. Referenced by `C/datasource/database/Database.kt:16, :21` and `Daos.kt:24, :587-595` (`PlayerDao`). |
| `G/data/CrosswordRepository.kt` | Concrete class; HTTP call, private DTOs and mapper all in one file. |
| `G/data/WordleRepository.kt` | Same as Crossword (HTTP, private DTO, cache). |
| `G/data/remote/GameRemoteDataSource.kt` | Interface OK, but `getRecap` returns `RecapResponse` from `G/domain/`. |
| `G/domain/Highscore.kt:3-4, :204-236` | Domain imports `HighscoreEntryResponse` / `GlobalRankingEntryResponse` and holds the DTO mappers. |
| `G/domain/RecapDataObjects.kt` | Network DTOs (`@Serializable` mirror of `/recap`) **and** UI models in one domain file. |
| `G/presentation/recap/RecapViewModel.kt:47-62` | Injects `GameRemoteDataSource`, `UserLocalDataSource`, `GroupLocalDataSource` directly. |
| `G/presentation/GameDifficultySelection.kt` | Global `object` with Compose `mutableStateOf`, read by 7 ViewModels and 2 composables. |

### 1.2 Presentation
| Game | VM lines | MVI today | Main problems |
|---|---|---|---|
| Tetris | 458 | no Action, VM functions called from UI | engine (`Tetromino`, board rules) inside VM file; Compose `Color` in state + snapshot; no Root (`App.kt` passes VM) |
| Morse | 458 | no Action | `mutableStateOf` in VM; `AudioManager` `koinInject` in `MorseScreen.kt:323`; no Root (`App.kt` passes VM) |
| Towerstack | 367 | `sealed class GameAction`, state named `GameState` | physics inside VM; `onCleared` logic also in `Towerstack.kt` |
| 2048 | 365 | Action in VM file | merge/spawn rules inside VM |
| SchneaggaHus | 466 | Action | Compose types in `SchneaggaHusState.kt`, `SchneaggaHusSnapshot.kt`, `SchneaggaHusMapGenerator.kt`, VM |
| GridRush | 283 | Action | `GridRushLogic.kt` already separate, but in `presentation/` |
| OddOneOut | 277 | Action | `OddOneOutLogic.kt` in `presentation/` |
| CChallenge | 277 | Action | generator, templates, snippets (~880 lines) in `presentation/` |
| Crossword | 312 | Action | `GermanCrosswordGenerator.kt`, `GermanWordList.kt` in `presentation/` |
| Wordle | 288 | Action | `GermanWordleWordList.kt` in `presentation/` |
| Yatzi | 307 | Action | rules in VM, `YatziModels.kt` in presentation |
| DartCounter | 253 | Action | already uses `G/domain/dartcounter/` (good reference for engine shape) |
| Undercover | 665 | Action | rules + word dealing in VM, `UndercoverWordList.kt` in presentation |
| CoinFlip | 52 | sealed class Action | VM via `koinInject` (not `koinViewModel`) in `CoinFlip.kt:78`; no Root |
| FingerPicker | 104 | Action | VM via `koinInject` in `FingerPicker.kt:50`; Compose `PointerId` in VM; no Root |
| GameSelector | 215 | none | Compose `ImageVector` list in VM; screen gets VM from `App.kt` |
| PlayerSelector | 90 | Action | `PlayerEntity` in presentation |
| Highscores | 110 | Action | concrete repo |
| GlobalRanking dialog | — | none | composable calls repository itself |
| Recap | 285 | Action | see 1.1 |
| Schneagg Rodeo | 339 | Action + Event | concrete repos; `AppVersion` `koinInject` in `SchneaggRodeoScreen.kt:92`; `RodeoTest.kt` dev toggles (leave) |

Duplicated plumbing in nearly every arcade VM: `gameStartTime` + `awaitResume` drift loop + `delay(32)` timer (9 VMs), a private `submitScore` that wraps `gameHighscoreRepository.submitScore` (11 VMs), `snapshotOrNull` / `restore` / `pauseAndPersist` / `onCleared` persist (15 VMs), reading `GameDifficultySelection.selected` at start (7 VMs).

---

## 2. Target architecture

### 2.1 Packages
```
G/domain/                 core models (GameId, GameDifficulty, LeaderboardSpec, HighscoreEntry, GameSave, GameSaveSlot, GamePlayer, RecapAvailability)
G/domain/                 core interfaces: GameHighscoreRepository, GameSaveRepository, PlayerRepository, GameDifficultyRepository, RecapRepository
G/domain/engine/          GameRules contracts (§2.3)
G/domain/<game>/          engine, domain state, snapshot, word lists / generators, game-specific repository interfaces
G/data/                   repository implementations
G/data/local/             PlayerLocalDataSource + RoomPlayerDataSource, entity/PlayerEntity.kt (+ mapper)
G/data/remote/            GameRemoteDataSource (+Ktor), CrosswordRemoteDataSource (+Ktor), WordleRemoteDataSource (+Ktor)
G/data/remote/dto/        GamesNetworkDtos, RecapDtos, NytCrosswordDto, NytWordleDto + mappers
G/presentation/           shared game UI (GameHud, overlays, HighscoreUpload*) + shared session helpers (§2.4)
G/presentation/<game>/    <Game>State, <Game>Action, <Game>Event, <Game>ViewModel, <Game>Root + <Game>Screen, UI-only files
```
Word lists and generators are pure Kotlin data/logic, so they move to `G/domain/<game>/`.

### 2.2 Repository and data source interfaces (Phase 0a)
Same function signatures as the current classes unless noted. Implementations get a prefix naming the source, like `chat/` (`OfflineFirstUserRepository`).

| Interface (`G/domain/…`) | Implementation (`G/data/…`) | Notes |
|---|---|---|
| `GameHighscoreRepository` | `RemoteGameHighscoreRepository` | 5 methods unchanged. |
| `GameSaveRepository` | `DataStoreGameSaveRepository` | `load`, `saveAsync`, `clearAsync` unchanged. |
| `cchallenge/CChallengeStreakRepository` + `cchallenge/CChallengeStreak` | `DataStoreCChallengeStreakRepository` | Key names unchanged. |
| `PlayerRepository` + `SavedPlayer(id, name)` | `LocalPlayerRepository` over `data/local/PlayerLocalDataSource` → `RoomPlayerDataSource` (uses `PlayerDao`, not `AppDatabase`) | `PlayerEntity` → `data/local/entity/`, table name unchanged, `@Serializable` dropped if unused. Update the imports in `Database.kt` / `Daos.kt`; schema version unchanged (package moves do not change the schema). Add `addPlayer(name)` so the VM no longer builds the entity. |
| `crossword/CrosswordRepository` | `NytCrosswordRepository` over `data/remote/CrosswordRemoteDataSource` → `KtorNytCrosswordDataSource` | Retry/random-date logic in repository, HTTP in data source, DTO + `toPuzzle()` + HTML-entity decoding in `dto/`. |
| `wordle/WordleRepository` | `NytWordleRepository` over `data/remote/WordleRemoteDataSource` → `KtorNytWordleDataSource` | Guess-list cache + mutex stay in the repository. |
| `RecapRepository` | `RemoteRecapRepository` | **Phase 1 WP-K**, not Phase 0. |
| `GameDifficultyRepository` | `InMemoryGameDifficultyRepository` | **Phase 0b**. `val selected: StateFlow<GameDifficulty>`, `fun select(difficulty)`. In memory, default `MEDIUM` (same as today). |

`HighscoreEntryResponse.toHighscoreEntry()` / `GlobalRankingEntryResponse.toGlobalRankingEntry()` move from `G/domain/Highscore.kt` to `G/data/remote/dto/HighscoreMappers.kt`.
Koin (`C/di/Modules.kt:203, :266-272`): `singleOf(::RemoteGameHighscoreRepository) bind GameHighscoreRepository::class` etc.; the NYT data sources keep `named(HTTPCLIENTTYPE.NOT_AUTHENTICATED)`.

### 2.3 Engine contract (Phase 0b, `G/domain/engine/GameRules.kt`)
```kotlin
/**
 * Pure rules of one game. No Compose, coroutines, Koin, Clock or platform types:
 * randomness comes in through the constructor (kotlin.random.Random), time as millis.
 */
interface GameRules<State, Move> {
    /** The state after [move]; returns [state] unchanged when the move is not allowed. */
    fun apply(state: State, move: Move): State
    fun isOver(state: State): Boolean
}

/** Single-player game with a leaderboard score. */
interface ScoredGameRules<State, Move> : GameRules<State, Move> {
    /** Score submitted to the leaderboard once [isOver]. */
    fun score(state: State): Long
}

/** Game that also advances on its own (falling pieces, timers, swinging blocks). */
interface RealtimeGameRules<State, Move> : ScoredGameRules<State, Move> {
    fun tick(state: State, deltaMillis: Long): State
}

/** Shared-device game: every player gets a result at the end. */
interface SharedDeviceGameRules<State, Move> : GameRules<State, Move> {
    fun results(state: State): List<Pair<GamePlayer, Long>>
}
```
Each engine also has its own `newGame(...)` factory (inputs differ per game: difficulty, puzzle, players). Domain state is an immutable `data class` in `G/domain/<game>/`. Exempt from the engine contract: CoinFlip, FingerPicker (tools, no rules worth extracting), Recap, selectors.

### 2.4 Session contract (Phase 0b, `G/presentation/`)
Composition, not a base ViewModel.
```kotlin
/** What a ViewModel implements so GameSaveSession can save and restore it. */
interface PersistableGame<Snapshot> {
    /** null = nothing worth keeping; the save is then removed. */
    fun snapshotOrNull(): Snapshot?
    fun restore(save: GameSave<Snapshot>)
    /** App went to background: pause the run. The session persists right after. */
    fun onAppBackgrounded()
}
```
- `GameSaveSession` (existing, `G/presentation/GameSaveSession.kt`): takes the `GameSaveRepository` **interface**; new `fun bind(game: PersistableGame<T>, difficulty: () -> GameDifficulty)` that does restore-once, `restoreChecked`, background → `onAppBackgrounded()` + persist, and `persistNow()` for `onCleared()`. Keep the old `start` / `persist` until Phase 2 so Phase 1 can migrate game by game.
- `PausableGameTimer(scope, tickMs = 32, isPaused: () -> Boolean)`: `elapsedMillis: StateFlow<Long>`, `start(fromElapsedMillis = 0)`, `stop()`, `elapsedNow()`. Replaces the `gameStartTime` + `awaitResume` + `delay` loops; keeps `awaitResume` semantics (pause does not count). `awaitResume` stays for loops that need it (Tetris gravity, Towerstack).
- `GameScoreSubmitter(game: GameId, repository: GameHighscoreRepository, scope)`: `submit(difficulty, score, timeMillis)`, `submitFriend(...)`. Replaces the private `submitScore` copies.
- `HighscoreUploadController` (existing): only swap to the interface.
- `CommonGameAction` (`Start`, `TogglePause`, `Restart`, `LeaveGame`): every arcade game's Action has `data class Common(val action: CommonGameAction) : <Game>Action`, so `GameStartOverlay` / `GamePauseOverlay` / `GameOverOverlay` wire the same way everywhere. Shared-device and puzzle games use it where it fits.
- Events: one-off UI effects (navigate back, snackbar, haptics, sound) go through `<Game>Event` on a `Channel(Channel.BUFFERED)` collected in the Root. Use the project's existing event-observer composable if one exists (code-reuse-scout); otherwise add one to `C/sharedUi/` in Phase 0b.

### 2.5 Per-game definition of done (every WP in Phase 1)
1. Rules live in `G/domain/<game>/<Game>Engine.kt` implementing the right contract from §2.3; no Compose / coroutine / Clock / Koin import in `G/domain/`.
2. Snapshot (`@Serializable`) lives in `G/domain/<game>/`, without Compose types. Layout changed → bump its version (D7).
3. ViewModel injects **only** domain interfaces (`GameHighscoreRepository`, `GameSaveRepository`, `GameDifficultyRepository`, …) and app services that already have interfaces; no `games.data.*` or `datasource.*` import (except D6 result types). It holds `MutableStateFlow<<Game>State>`, `fun onAction(<Game>Action)`, `events`, and uses `GameSaveSession.bind` + `PausableGameTimer` + `GameScoreSubmitter`. No rules inline.
4. `<Game>State` / `<Game>Action` / `<Game>Event` in their own file `<Game>State.kt` (Action and Event may share it, as the existing games do).
5. `<Game>Root(onBackClick)` gets the VM with `koinViewModel()` and collects events; `<Game>Screen(state, onAction)` is stateless. No `koinInject` in composables (AudioManager, AppVersion move into the VM or a VM-provided state).
6. Compose types (`Color`, `ImageVector`, `PointerId`, `Offset`) only in presentation; map from domain enums in the composable or a UI mapper.
7. `GameDifficultySelection` reads replaced by `GameDifficultyRepository`.
8. Bugs fixed on the way are listed in the report (D3).

---

## 3. Phases and work packages

Order: **Phase 0a ∥ Phase 0b → Phase 1 (12 WPs in parallel) → Phase 2.**
The tree must compile at the end of Phase 0 and Phase 2. During Phase 1 it may be broken in between.

### Phase 0a — data layer (1 agent, ~first 40 min)
Owns: all of `G/data/`, `G/domain/Highscore.kt`, `G/domain/Crossword.kt` + `Wordle.kt` (move into `domain/crossword/`, `domain/wordle/`), `C/datasource/database/Database.kt` + `Daos.kt` (import only), `C/di/Modules.kt` (games lines), import swaps in **every** file that imports a moved class.
1. Run `code-reuse-scout` with the planned list (§2.2 classes).
2. Create the interfaces and implementations of §2.2 (without Recap).
3. Swap imports across `G/` (mechanical; no other edits in game files).
4. Wire Koin; compile `:composeApp:compileKotlinJvm` (desktop, allowed).
5. Report: moved files, new class names.

### Phase 0b — shared contracts (1 agent, in parallel with 0a)
Owns: `G/domain/engine/`, `G/domain/GameDifficultyRepository.kt`, `G/data/InMemoryGameDifficultyRepository.kt`, `G/presentation/GameSaveSession.kt`, new `PausableGameTimer.kt`, `GameScoreSubmitter.kt`, `PersistableGame.kt`, `CommonGameAction.kt`, `G/presentation/GameDifficultySelection.kt` (keep the object for now; keep `DifficultySelector` / `stringRes`), the shared overlays (`GameStartOverlay`, `GamePauseOverlay`, `GameOverOverlay`, `GameHud`) if their signatures need the `CommonGameAction` callbacks.
1. Run `code-reuse-scout` with the §2.3 / §2.4 list.
2. Write §2.3 and §2.4. `GameSaveSession` and `GameScoreSubmitter` reference the 0a interfaces by name; agree on the names in §2.2 (do not rename).
3. Add one Koin line for `GameDifficultyRepository` (coordinate with 0a: 0b sends the line, 0a adds it, or 0b adds it after 0a is done).
4. Compile after 0a is done.

### Phase 1 — per game (12 agents in parallel)
Each WP owns **only** its listed files plus its own `entry<Route.X>` block in `C/app/App.kt` (only if the Root name changes). Do **not** edit `Modules.kt` (constructor changes are picked up by `viewModelOf`; new bindings go into the report), the shared overlays, or another WP's files. Do **not** run Gradle (parallel Gradle runs in one tree fight over the lock); Phase 2 compiles. New strings: append own keys (`games_<game>_…`) to en/de/it.

| WP | Scope (files under `G/presentation/` unless noted) | Engine contract | Notes |
|---|---|---|---|
| A | `tetris/*` | Realtime | Extract `Tetromino`, `canPlace`, `landingRow`, line clear, gravity into `domain/tetris/TetrisEngine.kt`. Board cells become `TetrominoType?`, colors mapped in UI. Snapshot loses Compose `Color` → bump version. Add `TetrisRoot`; App.kt entry. |
| B | `morse/*` | Realtime (challenge) | `MorseCode`, `MorseWordLists` → `domain/morse/`. Replace `mutableStateOf` in VM. Add `MorseAction`. `AudioManager` from composable into VM. Add `MorseRoot`; App.kt entry. |
| C | `towerstack/*` | Realtime | Rename `GameState` → `TowerstackState`, `GameAction` → `TowerstackAction` (sealed interface). Physics → `domain/towerstack/`. Remove VM-like logic from `Towerstack.kt`. Rename screen to `TowerstackRoot` + `TowerstackScreen`; App.kt entry. |
| D | `game2048/*` | Scored | Move/merge/spawn → `domain/game2048/`. Move Action/State out of VM file. |
| E | `gridrush/*`, `oddoneout/*` | Realtime / Scored | `GridRushLogic.kt`, `OddOneOutLogic.kt` → `domain/<game>/` as engines. |
| F | `schneaggahus/*` | Realtime | Remove Compose types from state, snapshot, map generator; generator + geometry → `domain/schneaggahus/`. `SchneaggaHusPreviewState.kt` stays UI. |
| G | `wordle/*`, `crossword/*` | Scored | Word lists + `GermanCrosswordGenerator` → `domain/<game>/`. Use the 0a repository interfaces. `evaluateWordleGuess` / `wordleScoreFor` move into `WordleEngine`. |
| H | `cchallenge/*` | Scored | `CChallengeGenerator`, `CChallengeOutputTemplates`, `CChallengeSnippets`, `CChallenge.kt` → `domain/cchallenge/`. `CCodeBlock.kt` stays UI. Streak via the 0a interface. |
| I | `yatzi/*`, `dartcounter/*`, `undercover/*`, `HighscoreUpload*.kt`, `GameResetButton.kt`, `PlayerSelector/*` | SharedDevice | Yatzi and Undercover rules + word list → `domain/<game>/`; Dart already has `domain/dartcounter/`, make `DartGame` implement the contract. PlayerSelector uses `SavedPlayer`. |
| J | `coinflip/*`, `fingerpicker/*`, `GameSelectorScreen.kt`, `GameSelectorViewModel.kt`, `GameScreenElement.kt`, `GameHighscores.kt`, `HighscoresViewModel.kt`, `GlobalRanking.kt`, `GameOverOverlay.kt` (difficulty read only) | — | CoinFlip/FingerPicker: `koinViewModel` + Root. GameSelector: MVI, VM exposes plain data (route, `GameId`, string res), icons chosen in the composable; `GameSelectorRoot`, App.kt entry. GlobalRanking: new `GlobalRankingViewModel` (+ DI line in report). Switch `GameSelectorScreen` / `GameOverOverlay` to `GameDifficultyRepository` (via VM / parameter). |
| K | `recap/*`, `G/domain/RecapDataObjects.kt`, `G/data/remote/GameRemoteDataSource.kt` + `Ktor…` (recap part) | — | DTOs → `data/remote/dto/RecapDtos.kt`; domain `Recap*` models + mapper; `RecapRepository` / `RemoteRecapRepository`; VM uses `RecapRepository` + `chat.domain.UserRepository.getUserById` / `GroupRepository.getGroupById` (closes Todo P4 items). UI models stay in presentation. `ChatSelector.kt:78` imports `RecapPalette` from games: leave, already in Todo. |
| L | `schneaggrodeo/SchneaggRodeoViewModel.kt`, `SchneaggRodeoScreen.kt` (wiring only, D5) | — | Interfaces only, `AppVersion` into the VM, `GameScoreSubmitter` + `GameSaveSession.bind`. No engine / render / vehicle changes. |

Load balance: A, B, F, I, K are the biggest; give them the strongest agents. If fewer agents are available, merge E+D, G+H, J+L.

### Phase 2 — integration (1 agent)
1. Add Koin lines from the WP reports; remove unused ones.
2. Delete `GameDifficultySelection` object, old `GameSaveSession.start` / `persist` overloads, `awaitResume` if unused.
3. Grep checks (all must be empty):
   - `grep -rn "import .*games\.data\." C/games/presentation C/games/domain`
   - `grep -rn "import .*datasource\." C/games/presentation C/games/domain | grep -v "network.util.\(NetworkResult\|NetworkingError\|EmptyResult\)"`
   - `grep -rln "import androidx.compose" C/games/domain`
   - `grep -rn "koinInject" C/games/presentation`
   - `grep -rn "GameDifficultySelection" C/`
4. Compile all four compile tasks from CLAUDE.md (desktop: allowed).
5. Write all WP findings into `plans/Todo.md`, tick the games P4 items, fill §7 below.
6. Run `readme-changelog-updater`, then `string-resource-extractor` (new strings en/de/it only).
7. Device smoke test list for the user: every game start / pause / background / kill-restore / game over / leaderboard upload.

---

## 4. Agent briefing (paste into every Phase 1 agent prompt)
> Work in place on branch `macci` (no new branch, no worktree). Read `plans/GAMES_ARCHITECTURE_PLAN.md` §0, §2 and your WP row. Read the skills `android-presentation-mvi`, `android-data-layer`, `android-compose-ui`. Edit only your WP files and your own App.kt entry. Do not run Gradle. Do not edit `Modules.kt`. Run `code-reuse-scout` before writing new helpers. You may fix bugs you find (D3), list each one. Finish with a report: files moved/created/deleted, Koin bindings needed, App.kt changes, bugs fixed (file:line, what, why), Todo items (file:line), snapshot versions bumped.

---

## 5. Risks
- **Save compatibility**: a moved snapshot class keeps its JSON shape unless its fields change; the class package is not in the JSON. Field changes → version bump (D7).
- **Room**: moving `PlayerEntity` changes no schema; double-check `Database.kt` exported schema stays at version 87.
- **Parallel edits**: shared files (`App.kt`, strings.xml) only get small, distinct hunks; Edit calls apply on the latest file content, so they do not overwrite each other. Never rewrite whole shared files.
- **Timing behavior**: `PausableGameTimer` must keep "paused time does not count" exactly; arcade agents compare the old loop line by line.

## 6. Open after this plan
- `Result<T, DataError>` conversion (project-wide Todo).
- Rodeo engine/vehicles structure (D5, later).

## 7. Bug fixes done during the refactor
_(filled in by Phase 2 from the WP reports)_
