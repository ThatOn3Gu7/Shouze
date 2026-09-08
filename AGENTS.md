# Shouze — Agent Workflow Notes

Android app (Jetpack Compose + Room + OkHttp + AniList GraphQL). Package: `com.app.shouze`.

## Environment (IMPORTANT)
- There is **no Android SDK / JDK / Gradle build available** in the local sandbox. You CANNOT run `./gradlew assembleDebug` locally.
- **The GitHub Actions CI is the compiler.** Workflow: edit code → commit → push this branch → CI builds (`Android CI` workflow) → if it fails, it posts a single self-updating comment on the PR with extracted compile errors → read it with `gh pr view <n> --comments` (or `gh api repos/ThatOn3Gu7/Shouze/issues/<n>/comments`) → fix → push again. Success turns the same comment green.
- Docs-only pushes skip the build (path filter in `.github/scripts/detect_app_changes.py`).

## Architecture map
- `MainActivity.kt` — single `NavHost`, bottom nav (Home/Airing/Search/Settings), handles the `shouze://anilist-auth` OAuth deep link.
- `ui/MediaViewModel.kt` — single source of truth (Room + Settings + AniList). UI states: `HomeUiState`, `AniListSearchUiState`, `AiringScheduleUiState`, `StreamingUiState`, `StatsUiState`. Exposes `authState`, `syncStatus`, `isOnline`.
- `data/remote/AniListApi.kt` — all GraphQL. One hardened `execute()` path: rate limiting (`AniListRateLimiter`, live limit 30 req/min, honor Retry-After), optional Bearer token, typed `AniListException{NETWORK,HTTP,RATE_LIMITED,UNAUTHORIZED,GRAPHQL}`.
- `data/auth/` — OAuth2 **Implicit Grant** (AniList's recommended mobile flow; no PKCE, 1-year token). `AniListAuthRepository` stores the token in EncryptedSharedPreferences. Client id comes from `ANILIST_CLIENT_ID` gradle property (see docs/ANILIST_SETUP.md).
- `data/sync/AniListLibraryRepository.kt` — sync engine: optimistic local apply → durable outbox (`sync_outbox` table) → FIFO drain when online; pull = `MediaListCollection` (ANIME+MANGA) → upsert + prune stale; pending local edits always flush before a pull.
- `data/mapper/AniListMapper.kt` — pure conversions (status/score/date); unit tested in `app/src/test`.
- `data/local/` — Room v7: `media_items` (now has `source`,`anilistId`,`listEntryId`,`mediaType`,`pendingSync`), `categories`, `sync_outbox`, `remote_cache` (offline read-only cache for trending/airing/search).
- `ui/MediaViewModel.persist(item)` is THE write entry point: routes by `item.source` — LOCAL → Room, ANILIST → optimistic sync engine. Bulk ops skip AniList items.

## Conventions
- Material 3, Compose BOM. Compose only, no XML layouts.
- `Status` enum: WATCHING, READING, COMPLETED, DROPPED, PLAN_TO_WATCH, PAUSED, REPEATING. PAUSED/REPEATING mirror AniList; WATCHING/READING both map to AniList `CURRENT` (split by media type). Adding enum values requires updating every exhaustive `when` (compiler will catch them).
- Status colors: `statusContainerColor`/`statusContentColor` helpers (per-file).
- Friendly API errors: `MediaViewModel.friendlyError` switches on `AniListException.kind` first.
- Favorites/tags are local-only (AniList favorites live on Media, not entries); notes/score/progress/dates/repeat sync.
- Keep new pure logic in `data/mapper`, `data/auth/ImplicitRedirectParser`, etc. and add JVM unit tests — CI runs `testDebugUnitTest`.

## Known limitations / out of scope (skip, don't force)
- No incremental refresh (full MediaListCollection pull per type; fine for realistic library sizes).
- AniList-backed items are read-only while signed out (edits require a session).
- Favorites/tags/custom-list mapping to AniList is not implemented.
