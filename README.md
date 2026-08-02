# Arrow Maze — Native Android (Kotlin + Jetpack Compose)

A 100% native rebuild of the HTML5 "Arrow Maze" tap puzzle, rewritten from scratch in Kotlin with Jetpack Compose. **No WebView, no HTML rendering, no JavaScript engine.** Every screen, animation, and gameplay mechanic is rebuilt natively.

## Tech stack

| Layer            | Tech                                                            |
| ---------------- | -------------------------------------------------------------- |
| Language         | Kotlin 2.0.21                                                  |
| UI               | Jetpack Compose + Material 3                                   |
| Architecture     | MVVM + Repository + Use Cases, StateFlow, Coroutines           |
| DI               | Hilt                                                           |
| Navigation       | Navigation Compose                                             |
| Persistence      | Room + DataStore Preferences                                   |
| Networking       | Retrofit + OkHttp + Kotlinx Serialization                     |
| Auth             | Firebase Auth (Email, Google, Guest) via Credentials API       |
| Backend          | Firestore, Storage, Remote Config, Analytics, Crashlytics, FCM |
| Audio            | Media3 ExoPlayer                                               |
| Images / Anim    | Coil + Lottie                                                  |
| Ads              | Google AdMob (Banner, Interstitial, Rewarded, Native, AppOpen) |
| Billing          | Google Play Billing v7                                         |
| Background       | WorkManager + Hilt                                             |

## Requirements

- **minSdk**: 26 (Android 8.0)
- **targetSdk / compileSdk**: 35 (Android 15)
- **Java**: 17
- **Gradle**: 8.9
- **AGP**: 8.7.2
- **Orientation**: Portrait only

## Project structure

Single Gradle module, package-organized to mirror the requested multi-folder layout (keeps the build simple while preserving the conceptual separation). See [ARCHITECTURE.md](ARCHITECTURE.md) for the full map.

```
com.zenox.arrowmaze/
├── ArrowMazeApplication.kt        # @HiltAndroidApp, WorkManager config
├── MainActivity.kt                # Single-activity host
├── core/
│   ├── common/                    # Constants, result wrappers, dispatchers
│   ├── data/                      # DTOs, repositories, mappers
│   ├── domain/                    # Pure game logic + models + use cases
│   ├── ui/                        # Shared Compose UI plumbing
│   ├── navigation/                # Nav graph + routes
│   ├── designsystem/              # Material 3 theme, components, tokens
│   ├── database/                  # Room entities, DAOs, database
│   ├── datastore/                 # DataStore preferences
│   ├── firebase/                  # Auth, Firestore, Messaging, Config
│   ├── ads/                       # AdMob managers
│   ├── billing/                   # Play Billing manager
│   ├── audio/                     # ExoPlayer + SFX manager
│   ├── di/                        # Hilt modules
│   └── utils/                     # Extensions, formatters
└── features/
    ├── authentication/
    ├── home/
    ├── game/
    ├── profile/
    ├── leaderboard/
    ├── shop/
    ├── achievements/
    ├── statistics/
    ├── settings/
    └── dailychallenge/
```

## Build locally

### Prerequisites

1. **Android Studio Koala/Ladybug+** (or just JDK 17 + Gradle 8.9)
2. Generate the Gradle wrapper once (the wrapper jar is intentionally not committed):
   ```bash
   gradle wrapper --gradle-version 8.9
   ```
3. Copy `local.properties.template` → `local.properties` and point `sdk.dir` at your Android SDK.

### Debug build

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Signed release build

1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore arrow-maze.jks -keyalg RSA -keysize 2048 \
     -validity 10000 -alias arrow-maze
   ```
2. Copy `keystore.properties.template` → `keystore.properties`, fill in the values.
3. ```bash
   ./gradlew assembleRelease
   ```

## CI / GitHub Actions

The workflow at `.github/workflows/android-ci.yml`:
- Builds a **debug APK** on every push / PR — uploads it as an artifact (`arrow-maze-debug-apk`).
- Builds a **release APK** if the debug job passes — uploads it as `arrow-maze-release-apk`.
- For signed releases, set these repository **secrets** (never paste tokens in chat!):
  - `ARROW_MAZE_KEYSTORE_BASE64` — base64 of your `.jks`
  - `ARROW_MAZE_KEYSTORE_PASSWORD`
  - `ARROW_MAZE_KEY_ALIAS`
  - `ARROW_MAZE_KEY_PASSWORD`

```bash
# base64-encode your keystore for the GitHub secret:
base64 -i arrow-maze.jks | pbcopy   # macOS
base64 -w 0 arrow-maze.jks          # Linux
```

## Firebase setup

> ⚠️ See `app/README.google-services.md` — the current `google-services.json` belongs to a different Firebase project. Replace it with your own Arrow Maze project's config before going to production.

## Development phases

This project is built incrementally across 10 phases:

| Phase | Scope                                                              |
| ----- | ----------------------------------------------------------------- |
| 1     | Gradle, Version Catalog, Manifest, resources, CI, signing         |
| 2     | Hilt Application, Material 3 theme, navigation, splash            |
| 3     | Design system, adaptive icons, reusable components                |
| 4     | Domain layer — pure Kotlin game logic                            |
| 5     | Data layer — Room, DataStore, repositories                       |
| 6     | Game UI — Compose Canvas board, animations, HUD                  |
| 7     | Authentication & profile (Firebase Auth + Firestore)             |
| 8     | Shop, 13 themes, achievements, daily challenge                   |
| 9     | Leaderboards, friends, statistics, settings                      |
| 10    | AdMob, Play Billing, FCM, Remote Config, polish, release         |

See `worklog.md` at the repo root for per-phase progress.

## License

Proprietary — © Zenox. All rights reserved.
