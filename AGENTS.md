# Shouze — Agent Workflow Notes

Android app (Jetpack Compose + Room + OkHttp + AniList GraphQL). Package: `com.app.shouze`.

## Environment (IMPORTANT)
- Source lives at `/data/data/com.termux/files/home/Shouze` (Termux home on the device).
- There is **no Android SDK / Gradle build available** inside this proot/Ubuntu shell. You CANNOT run `./gradlew assembleDebug` or lint here.
- The human runs the build/assemble commands on the device (Termux) and reports back errors.
- Workflow: edit Kotlin/XML files in the path above, tell the user exactly which screens to open to see each change, user builds, user pastes errors, fix, repeat.

## Verification rule
- After every edit, re-read the changed file to confirm the change is correct and does not break surrounding code. You cannot rely on the compiler here.

## Architecture map (online-first)
- `MainActivity.kt` — single `NavHost`, bottom nav (Discover/Library/Search/Account), wires `ShouzeViewModel` to screens. Also handles the AniList OAuth2 deep link (`com.app.shouze://oauth?code=…`) and static shortcuts.
- `ui/ShouzeViewModel.kt` — single source of truth. UI states: `LibraryUiState`, `DiscoverUiState`, `SearchUiState`, `DetailUiState`, `StatsUiState`; exposes `account` (`AccountUiState`), `categories`, `credentials`, `settingsFlow`.
- `data/MediaRepository.kt` — online-first two-way sync against AniList; Room is a local cache of `library_entries`, AniList is canonical. Optimistic mutations push via `SaveMediaListEntry`.
- `data/AniListAuth.kt` — AniList OAuth2 (client credentials in SharedPreferences, authorization-code token exchange, persisted access/refresh token).
- `data/remote/AniListApi.kt` — OkHttp GraphQL to `https://graphql.anilist.co` (URL is correct; "host not resolving" is environmental network, not a code bug).
- `data/local/*` — Room DB v7: `library_entries` (`LibraryEntryEntity`) + `categories` (`CategoryEntity`).
- Screens in `ui/screens/*`, reusable UI in `ui/components/*`.
- Theme in `ui/theme/MediaTrackerTheme.kt`.

## Conventions
- Material 3, Compose BOM. No XML layouts for screens (Compose only).
- `MediaStatus` enum (AniList vocabulary): CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING.
- Status colors: chips/cards use the `MediaStatus.containerColor()` / `contentColor()` composable extensions in `ui/components/StatusUi.kt`.
- Friendly API errors: map network failures to user-facing text via `friendlyError` in `data/MediaRepository.kt`.

## Known limitations / out of scope (skip, don't force)
- Reminders/notifications (WorkManager), category color *editing* UI.
- Pruned (removed): MAL XML import, CSV export, backup zip, avatar pickers, share list. Manual add (offline fallback) and categories are kept.
- Widget exists (`ui/widget/ShouzeWidgetProvider`).
