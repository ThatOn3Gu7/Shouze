# Shouze

[![Android CI](https://github.com/ThatOn3Gu7/Shouze/actions/workflows/android-ci.yml/badge.svg)](https://github.com/ThatOn3Gu7/Shouze/actions/workflows/android-ci.yml)

**Shouze** is an offline-first anime & manga tracker for Android, backed by your
[AniList](https://anilist.co) account. Track what you watch and read, sync it
both ways with AniList, and keep everything usable when you're offline.

Built with **Kotlin** and **Jetpack Compose** (Material 3), with **Room** for
local storage and AniList's GraphQL API for the server side.

---

## About

Shouze started as a purely manual, local-only tracker and evolved into a full
AniList client. The design principles that shaped it:

- **Local-first.** Every edit applies instantly on-device; the network is an
  optimization, never a requirement.
- **The server is a peer, not the owner.** Your un-synced local changes are
  sacred: a server refresh can never overwrite them, and a rejected change
  stays on your device with a clear explanation instead of vanishing.
- **One way to add media.** Since AniList knows everything already, adding is
  search-and-pick — no typing titles, descriptions, or episode counts by hand.
- **Honest states.** Loading, offline, error, and "waiting to sync" are always
  visible; nothing fails silently.

## Features

### AniList account
- **Sign in with AniList** — the official OAuth 2 Implicit Grant flow.
  Tokens are stored in Android Keystore–backed encrypted preferences and last
  a year; expired sessions prompt a friendly re-login.
- **Guided manual sign-in** fallback (approval-page link + paste-the-redirect-
  address-or-token) for browsers that block app links.
- Default builds ship with the maintainer's public AniList application id, so
  end users sign in with zero configuration.

### Two-way library sync
- Pulls your AniList anime & manga lists via `MediaListCollection` and pushes
  local edits through `SaveMediaListEntry` / `DeleteMediaListEntry`.
- **Durable outbox** (Room-backed): edits made offline are queued FIFO,
  coalesced per item, and delivered automatically — including the moment
  connectivity returns.
- **Pending-safe reconciliation:** server pulls skip items whose local edits
  haven't been delivered yet, so your changes always win until AniList has
  actually accepted them.
- Permanently rejected changes are reported in the sync status bar with the
  server's reason, and the local edit is kept.
- Score conversion honors your AniList score format (100-point, 10-point,
  5-star, 3-smiley), including WATCHING↔CURRENT and REPEATING round-trips.
- The Profile card shows live sync state: *Syncing…*, *N change(s) waiting*,
  or *Synced N change(s) · just now* (persists across restarts).

### Discovery & details
- **Search & trending** straight from AniList, with infinite scroll, search
  history, anime/manga filter, and cached results for offline viewing.
- **Rich detail page** for any AniList title: banner, cover, genres, synopsis,
  quick facts (format, episodes × duration, season, dates, source), studios,
  ranked tags, staff, trailer link, streaming-site links, next-episode airing
  countdown, and tappable related titles.
- **Airing schedule** with per-day browsing and offline access to cached days.
- Add any title with one tap (default status per media type) or choose from
  all seven statuses.

### The tracker itself
- Manual-style editing for every field that matters: status, progress,
  volume, score, notes, start/end dates, rewatch count, favorite.
- Categories, tags, favorites, sorting, and multi-select bulk actions.
- Statistics screen for your collection.
- Share your lists as text; import shared lists from others.
- Backup & restore the whole library.
- System / light / dark themes with Material You dynamic color.

### Launch experience
- System splash screen (Android 12+ API with pre-12 compatibility) flowing
  into a branded intro: the launcher mark punched out of the brand green,
  warping past the camera into the app.
- Three-step onboarding: welcome → connect AniList (skippable) → theme.

## App structure

```
app/src/main/java/com/app/shouze/
├── MainActivity.kt              # Single-activity Compose host, deep links, shortcuts
├── data/
│   ├── local/                   # Room database (v7): entities, DAOs, migrations
│   │   ├── MediaItemEntity      #   unified library item (LOCAL and ANILIST sources)
│   │   ├── SyncOutboxEntity     #   durable queue of undelivered changes
│   │   ├── RemoteCacheEntity    #   keyed cache (search pages, airing days, sync markers)
│   │   └── AppDatabase          #   + MIGRATION_6_7
│   ├── remote/
│   │   ├── AniListApi           # GraphQL client: typed errors, rate limiting, retries
│   │   ├── AniListModels.kt     # kotlinx.serialization DTOs (tolerant defaults)
│   │   ├── AniListRateLimiter   # client-side throttle honoring AniList limits & Retry-After
│   │   └── NetworkModule        # hardened OkHttp (timeouts, retries)
│   ├── auth/
│   │   ├── AniListAuthRepository# OAuth session: encrypted storage, expiry, fallbacks
│   │   └── ImplicitRedirectParser # parses the #access_token=... deep link
│   ├── sync/
│   │   ├── AniListLibraryRepository # the sync engine: outbox drain, pulls, reconcile
│   │   └── NetworkMonitor       # connectivity flow for auto-flush
│   ├── mapper/AniListMapper     # AniList <-> entity mapping, score formats, ids
│   └── SettingsRepository       # theme, onboarding, update cadence
├── ui/
│   ├── MediaViewModel.kt        # single app-wide ViewModel: library, search, sync, auth
│   ├── screens/                 # Compose screens (Home, Search, Detail, AniList detail,
│   │                            #   Airing, Profile, Statistics, Backup, Settings, ...)
│   ├── components/              # shared composables (cards, images, haptics)
│   └── theme/                   # Material 3 theming + dynamic color
└── res/                         # launcher icon, splash theme, strings
```

## Development notes

### Architecture in short
- **Single-activity, Jetpack Compose**, one shared `MediaViewModel`.
- **Room** is the source of truth for the UI; the sync engine reconciles it
  with AniList. UI state flows come from Room as `Flow`s, so any change —
  local edit or server pull — updates the screen automatically.
- **Sync engine** (`AniListLibraryRepository`): mutex-guarded; drains the
  outbox oldest-first (stopping at the first undeliverable op), then pulls and
  reconciles. Deletes queue a server delete; saves coalesce into one op per
  item. The engine reports delivered counts and errors to the Profile card.
- **API discipline:** every call goes through one execution path with the rate
  limiter, typed `AniListException`s (OFFLINE / RATE_LIMITED / UNAUTHORIZED /
  GRAPHQL / SERVER), and GraphQL-error messages surfaced verbatim to the user.
- **Tolerant DTOs:** remote models use nullable/defaulted fields so AniList
  can never crash a whole screen by omitting one value.

### Building
- Requirements: **JDK 17**, Android SDK (the CI uses the Gradle wrapper).
- `gradle.properties` is machine-local and gitignored — start from the
  committed template:

  ```sh
  cp gradle.properties.example gradle.properties
  ```

  The template's heap (1 GB) is sized for **on-device Termux builds**; on a
  desktop you'll want to raise `org.gradle.jvmargs` (CI appends 3 GB).
  If you configure Gradle globally (`~/.gradle/gradle.properties`), a project
  copy is unnecessary — global properties win.

- **AniList application id:** builds resolve `ANILIST_CLIENT_ID` from
  `local.properties` → `gradle.properties` → the baked-in default (the
  maintainer's public client id). Forks should create their own application at
  <https://anilist.co/settings/developer> with the redirect URL
  `shouze://anilist-auth` and override it — see
  [docs/ANILIST_SETUP.md](docs/ANILIST_SETUP.md). The client id is public by
  design (Implicit Grant); never add a client *secret* anywhere.

### CI
`.github/workflows/android-ci.yml` runs on every push:
- A path filter decides whether app code changed — docs-only pushes skip the
  build entirely (fail-open when in doubt).
- Builds `assembleDebug` + `testDebugUnitTest`; every green run publishes a
  **`Shouze-debug-apk`** artifact.
- On failure it posts a single self-updating PR comment with extracted
  Kotlin/KSP errors, failed unit tests, failed-task output, and Gradle's
  failure summary — sized for humans and AI agents alike — and uploads a
  `ci-failure-diagnostics` artifact with the raw test results.
- Concurrent pushes cancel superseded runs.

### Tests
`app/src/test` covers the pure logic that matters most:
- `AniListMapperTest` — status/score/date round-trips, collection flattening.
- `ImplicitRedirectParserTest` — OAuth fragment parsing, error codes.
- `AniListAuthRepositoryUrlTest` — the authorize URL matches AniList exactly
  (and never carries `redirect_uri`, which AniList rejects).
- `AniListSearchDecodingTest` — partial `mediaListEntry` payloads decode.
- `FuzzyDateTest` — `FuzzyDateInput` conversion for saves.

```sh
./gradlew testDebugUnitTest
```

## Versioning

Current release: **v7.0.0**. Version names live in `app/build.gradle.kts`
(`versionName` / `versionCode`).

## Acknowledgements

- [AniList](https://anilist.co) and its
  [GraphQL API](https://docs.anilist.co) — the entire data backbone.
- The Jetpack Compose, Room, OkHttp, and kotlinx.serialization ecosystems.
