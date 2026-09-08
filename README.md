# Shouze

A media tracker for Android — track the anime, manga and shows you consume, and
(optionally) use it as a full client for your [AniList](https://anilist.co) library.

Built with Jetpack Compose + Room, speaking directly to the
[AniList GraphQL API](https://docs.anilist.co).

## Highlights

- **Your library, your way** — local categories, statuses, favorites, tags,
  notes, stats and backups all work with no account required.
- **AniList account sync** — sign in once and your anime/manga lists stay in
  step with AniList: progress, scores, statuses, notes, start/completion dates
  and rewatch counts. Edits made offline queue up and sync when you're back
  online.
- **API-driven discovery** — search with pagination, trending, airing schedule
  and streaming links, all cached so they still work offline (read-only).
- **Resilient by design** — typed network errors, client-side rate limiting
  (AniList is currently limited to ~30 requests/min), optimistic updates with
  rollback/queueing, and clear loading/empty/error/offline states everywhere.

## AniList integration

Shouze uses AniList's **OAuth2 Implicit Grant** — the flow AniList itself
recommends for mobile apps. No client secret ships in the APK; the access token
is stored in Android Keystore-encrypted preferences.

To enable sign-in you need your own AniList API client id (free, takes a
minute): see **[docs/ANILIST_SETUP.md](docs/ANILIST_SETUP.md)**.

### How syncing works

| Aspect | Behavior |
| --- | --- |
| Source of truth | AniList for signed-in (AniList-backed) entries; local entries remain device-only |
| Reads | Library is cached in Room; the app works offline and auto-refreshes when stale |
| Writes | Applied locally immediately (optimistic), then synced via a durable outbox queue |
| Offline edits | Queued in the outbox, flushed oldest-first when connectivity returns |
| Bursts | Multiple quick edits to the same entry coalesce into one API call |
| Conflicts | Your unsent local edits always push before a pull (last-writer-wins on server) |
| Session | 1-year tokens; expiry is detected and you're asked to sign in again |

Entries you track on AniList appear in the library under the "Anime" / "Manga"
categories, and the `Paused` / `Rewatching` statuses are now available locally
too (they map to AniList's `PAUSED` / `REPEATING`).

## Architecture

```
app/src/main/java/com/app/shouze/
├── MainActivity.kt              # single-activity NavHost, deep links (OAuth redirect)
├── data/
│   ├── auth/                    # AniList OAuth (implicit grant), encrypted token store
│   ├── local/                   # Room DB (media, categories, sync outbox, response cache)
│   ├── mapper/                  # pure AniList <-> app model conversions (unit tested)
│   ├── remote/                  # AniList GraphQL client + rate limiter + DTOs
│   ├── sync/                    # library sync engine, outbox flusher, network monitor
│   └── SettingsRepository.kt
├── ui/
│   ├── MediaViewModel.kt        # single source of truth for UI state
│   ├── screens/                 # Compose screens (Home, Search, Airing, Profile, ...)
│   ├── components/              # shared composables
│   └── theme/
```

Key ideas:

- **Room drives the UI.** Everything on screen renders from Room flows; network
  results are written into Room, so the app feels instant and survives process
  death with data intact.
- **One write path.** `MediaViewModel.persist(item)` routes by `item.source`:
  local items go straight to Room, AniList items go through the optimistic sync
  engine (`data/sync/AniListLibraryRepository.kt`).
- **One rate limiter.** All AniList traffic funnels through
  `AniListRateLimiter`, which paces requests and honors `Retry-After` on 429s.

## Building

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew assembleRelease        # signed release (needs signing config)
```

Android Studio (or `local.properties` with `sdk.dir`) is required. CI builds
and tests every push that touches app code — see
`.github/workflows/android-ci.yml`.

Releases are cut by pushing a `v*` tag (`.github/workflows/build-release.yml`).

## License

All rights reserved by the author.
