# Shouze — Agent Workflow Notes

Android app (Jetpack Compose + Room + OkHttp + AniList GraphQL). Package: `com.app.shouze`.

## Environment (IMPORTANT)
- Source lives at `/data/data/com.termux/files/home/Shouze` (Termux home on the device).
- There is **no Android SDK / Gradle build available** inside this proot/Ubuntu shell. You CANNOT run `./gradlew assembleDebug` or lint here.
- The human runs the build/assemble commands on the device (Termux) and reports back errors.
- Workflow: edit Kotlin/XML files in the path above, tell the user exactly which screens to open to see each change, user builds, user pastes errors, fix, repeat.

## Verification rule
- After every edit, re-read the changed file to confirm the change is correct and does not break surrounding code. You cannot rely on the compiler here.

## Architecture map
- `MainActivity.kt` — single `NavHost`, bottom nav (Home/Search/Profile), wires ViewModel to screens.
- `ui/MediaViewModel.kt` — single source of truth (Room + Settings + AniList). UI states: `HomeUiState`, `AniListSearchUiState`, `AiringScheduleUiState`, `StreamingUiState`, `StatsUiState`.
- `data/remote/AniListApi.kt` — OkHttp GraphQL to `https://graphql.anilist.co` (URL is correct; "host not resolving" is environmental network, not a code bug).
- Screens in `ui/screens/*`, reusable UI in `ui/components/*`.
- Theme in `ui/theme/MediaTrackerTheme.kt`.

## Conventions
- Material 3, Compose BOM. No XML layouts for screens (Compose only).
- `Status` enum: WATCHING, READING, COMPLETED, DROPPED, PLAN_TO_WATCH.
- Status colors: chips/cards use `statusContainerColor`/`statusContentColor` helpers.
- Friendly API errors: map network failures to user-facing text via `MediaViewModel.friendlyError`.

## Known limitations / out of scope (skip, don't force)
- Offline caching of AniList, reminders/notifications (WorkManager), QR share, category color *editing* UI.
- Backup zip is already JSON; CSV + MAL XML import exist.
- Widget exists (`ui/widget/ShouzeWidgetProvider`).
