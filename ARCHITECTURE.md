# Arrow Maze — Architecture

## High-level

```
┌──────────────────────────────────────────────────────────────┐
│                       UI Layer (Compose)                      │
│  features/* screens  ←  ViewModels (StateFlow)  ←  UseCases   │
└──────────────────────────────────────────────────────────────┘
                              │
┌──────────────────────────────────────────────────────────────┐
│                     Domain Layer (pure Kotlin)                │
│  models • game logic • rules • use cases (no Android deps)    │
└──────────────────────────────────────────────────────────────┘
                              │
┌──────────────────────────────────────────────────────────────┐
│                       Data Layer                              │
│  Repositories → Room • DataStore • Firestore • Retrofit       │
└──────────────────────────────────────────────────────────────┘
                              │
┌──────────────────────────────────────────────────────────────┐
│           Platform / SDK (Firebase, AdMob, Billing, Media)    │
└──────────────────────────────────────────────────────────────┘
```

## Module / package map

The project is a single Gradle module (`:app`) organized into packages that mirror the requested logical structure. Rationale: multi-module Gradle builds introduce significant compile-time risk for AI-generated code (missing api vs implementation edges, circular deps, Hilt aggregation across modules). A single module with a clean package structure delivers the same architectural clarity with far higher first-pass compile odds.

### `core/`

| Sub-package    | Responsibility                                                              |
| -------------- | --------------------------------------------------------------------------- |
| `common`       | App-wide constants, `Result<T>` / `Either<L,R>` wrappers, `DispatcherProvider` |
| `designsystem` | Material 3 color schemes, typography, shapes, motion tokens; reusable Compose components |
| `domain`       | Pure game logic (see Domain Layer section). No Android imports beyond `kotlinx`. |
| `data`         | Repository implementations, DTOs, mappers to domain                         |
| `database`     | Room entities, DAOs, `ArrowMazeDatabase`                                    |
| `datastore`    | DataStore preference accessors (settings, session)                          |
| `firebase`     | `auth/`, `firestore/`, `messaging/`, `config/` wrappers                     |
| `ads`          | AdMob managers (Banner, Interstitial, Rewarded, Native, AppOpen)            |
| `billing`      | Play Billing connection + purchase flow                                     |
| `audio`        | ExoPlayer-backed music + SFX manager                                        |
| `navigation`   | NavHost, route constants, deep-link definitions                             |
| `ui`           | Shared Compose plumbing (Scaffold state, snackbar host, common collectors)  |
| `di`           | Hilt modules: `DatabaseModule`, `NetworkModule`, `FirebaseModule`, `DatastoreModule`, `RepositoryModule`, `ManagerModule` |
| `utils`        | Extension functions, formatters, validators                                 |

### `features/`

Each feature package contains: `*Screen.kt` (Composable), `*ViewModel.kt`, `*UiState.kt`, and any feature-private components.

| Feature            | Screens                                                        |
| ------------------ | -------------------------------------------------------------- |
| `authentication`   | Login, Signup, ForgotPassword, GuestOnboarding                |
| `home`             | Home dashboard, mode selection, daily-reward teaser           |
| `game`             | Game board, HUD, win/lose dialogs, hint overlay               |
| `profile`          | Profile view + edit, stats summary                             |
| `leaderboard`      | Tabbed leaderboard (global / friends / weekly / monthly / all)|
| `shop`             | Categorized shop, item detail, purchase flow                  |
| `achievements`     | Grid + detail + animated unlock popup                         |
| `statistics`       | Charts (solve time, win rate, streak, level distribution)     |
| `settings`         | Theme picker, audio sliders, accessibility, account actions   |
| `dailychallenge`   | Daily board + streak calendar                                  |

## Domain layer — game design

Arrow Maze is a tap-puzzle. The board is an N×N grid. Each cell holds an arrow (Up/Down/Left/Right) — except for one **Goal** cell and one **Start** cell. The player taps a cell; the arrow rotates 90° clockwise. Win when a continuous path flows from Start → Goal following the arrows.

### Core types

- `ArrowDirection { UP, RIGHT, DOWN, LEFT }` — rotates clockwise via `rotateCW()`
- `Position(row, col)` — value class for grid coords
- `Cell` — sealed: `ArrowCell(direction)`, `StartCell`, `GoalCell`, `EmptyCell`
- `Board` — immutable grid of cells + size + start/goal positions
- `DifficultyTier { EASY, NORMAL, HARD, EXPERT, MASTER, LEGEND }`
- `LevelConfig(boardSize, arrowCount, pathComplexity, thinkingDifficulty)` — derived from level number via `LevelProgression`
- `PuzzleGenerator` — generates **guaranteed-solvable** boards by: (1) lay a random Hamiltonian-ish path Start→Goal, (2) place arrows along path with random offset, (3) fill remaining cells with decoy arrows, (4) verify solver still finds unique-ish solution
- `WinDetector` — BFS/DFS following arrows from Start; win if reaches Goal
- `HintSystem` — finds a cell whose rotation brings the player closer to a solved state (greedy)
- `GameSession` — holds current `Board`, move count, hint count, elapsed time; emits `GameEvent`s

### Level progression

| Level range | Tier     | Board size | Arrow density |
| ----------- | -------- | ---------- | ------------- |
| 1–20        | Easy     | 4×4        | 40–55%        |
| 21–50       | Normal   | 5×5        | 50–60%        |
| 51–100      | Hard     | 6×6        | 55–65%        |
| 101–200     | Expert   | 7×7        | 60–70%        |
| 201–400     | Master   | 8×8        | 65–72%        |
| 401+        | Legend   | 9×9        | 70–78%        |

## State management

- **ViewModel** exposes `StateFlow<UiState>` — single source of truth.
- **UiState** is a sealed/`data class` with loading / success / error variants.
- **One-shot events** (navigation, snackbars) flow via `SharedFlow<UiEvent>`.
- **Repository** is the write barrier — all writes go through it so Room + DataStore + Firestore stay consistent.

## Offline-first sync

Local Room/DataStore is the source of truth for reads. Firestore writes happen in the background via WorkManager + `Transactions.batch()`. Guest progress is tagged with a local UUID; on sign-in, a `GuestMerger` reconciles local → cloud and clears the guest tag.

## Theming

Material 3 with a custom `ArrowMazeTheme` composable that swaps `ColorScheme` per selected theme (Light, Dark, Cyberpunk, Minimal, Glass, Neon, Ocean, Sunset, Forest, Space, Galaxy, Golden, Wood). Each theme provides: surface, background, primary/secondary/tertiary, plus custom `arrowColor`, `trailColor`, `goalColor` extension colors held in a `CompositionLocalProvider`.

## Build variants

- `debug` — uses AdMob **test ad units**, `.debug` applicationId suffix, verbose Timber.
- `release` — uses **release ad units** (placeholders until you fill real IDs in `app/build.gradle.kts`), R8 minify + resource shrink, signed with `keystore.properties` if present.

## Security

- `network_security_config.xml` — cleartext only for localhost, system anchors for release.
- Play Integrity ready (integration hook in Phase 10).
- Firestore Security Rules documented in `docs/firestore-rules.rules` (Phase 7).
- R8 obfuscation on release.

## Testing strategy (not auto-generated)

Domain layer is pure Kotlin and trivially unit-testable (`PuzzleGenerator`, `WinDetector`, `HintSystem`). Per project policy no tests are committed, but the domain package is structured to make adding tests a 1-file-per-class exercise.
