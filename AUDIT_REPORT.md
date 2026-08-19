# Shouze Codebase Audit Report
**Date:** 2026-08-19  
**Scope:** Full Android app (`app/src/main/java/com/app/shouze` + build/config)  
**Branch:** `arena/01a01b02-shouze`

---

## Executive Summary

| Severity | Count | Categories |
|----------|-------|------------|
| 🔴 Critical | 6 | Crashes, data loss, security |
| 🟠 Major | 12 | Performance, memory leaks, ANRs, UX bugs |
| 🟡 Minor | 18 | Code quality, dead code, best practices |
| 🟢 Improvements | 10 | Architecture, maintainability, polish |

---

## 🔴 Critical Issues

### 1. Race Condition in `CoverImageStore.init()` — Crash/Double-Init
**File:** `ui/components/CoverImageStore.kt`  
**Line:** 47-58

```kotlin
if (initialized) return  // Thread A passes here
synchronized(this) {
    if (initialized) return  // Thread B already initialized
    // ... init code
}
```
**Problem:** The first `if (initialized) return` is outside the synchronized block. On multi-core devices, two threads can simultaneously see `initialized == false`, enter the synchronized block, and double-initialize, causing `diskDir`/`statusPrefs` to be overwritten and cache corruption.

**Fix:** Move the volatile read inside `synchronized`, or use `by lazy` / `SingletonHolder` pattern.

---

### 2. `MediaItemEntity.tags` Has No Room TypeConverter — Compile/Runtime Failure
**File:** `data/local/MediaItemEntity.kt`, `data/local/Converters.kt`  

`MediaItemEntity` declares `val tags: List<String>`, but `Converters.kt` only defines converters for `genres` (also `List<String>`). Room matches converters by **type**, not field name, so this happens to work by accident. However, if someone later adds a second `List<String>` converter (e.g., for comma-separated values), Room will pick arbitrarily and `tags` will silently corrupt.

**Fix:** Explicitly annotate or rename converters to be unambiguous. Add `@TypeConverters` at the field level or use a dedicated `TagsConverter`.

---

### 3. `OkHttpClient` Created Per `AniListApi` Instance — Memory Leak + No Connection Pool
**File:** `data/remote/AniListApi.kt`  
**Line:** 120

```kotlin
class AniListApi {
    private val client = OkHttpClient()
    ...
}
```

Each `MediaViewModel` creates a new `AniListApi()`, which spawns a new `OkHttpClient` with its own connection pool, thread pool, and dispatcher. These are never shut down. On configuration changes or process restarts, this leaks threads and sockets.

**Fix:** Make `OkHttpClient` a singleton or shared instance.

---

### 4. `BackupScreen` & `DetailScreen` Contain ~700 Lines of Dead Commented Code
**Files:** `ui/screens/BackupScreen.kt`, `ui/screens/DetailScreen.kt`

Large blocks of old implementations are commented out rather than deleted. This bloats the binary (comments still compile into line number tables), confuses reviewers, and makes refactoring hazardous.

**Fix:** Delete the commented code. Git history preserves it.

---

### 5. `android:usesCleartextTraffic="true"` — Security Vulnerability
**File:** `AndroidManifest.xml`  
**Line:** 12

Allows all HTTP (non-TLS) traffic globally. AniList API is HTTPS-only. This exposes the app to man-in-the-middle attacks if any library or WebView falls back to HTTP.

**Fix:** Remove the attribute, or use a `network_security_config.xml` with strict domain pinning if absolutely needed.

---

### 6. `CategoriesScreen` Selected Color Is Never Saved
**File:** `ui/screens/CategoriesScreen.kt`, `ui/MediaViewModel.kt`  

The UI lets users pick a color from `CATEGORY_COLOR_PALETTE`, but `onAddCategory(name)` only passes the name string. `MediaViewModel.addCategory(name)` creates `CategoryEntity(name = name)` ignoring the color entirely. The `colorHex` field is always null for user-created categories.

**Fix:** Pass `colorHex` through the callback chain into `CategoryEntity`.

---

## 🟠 Major Issues

### 7. `preloadTrendingCovers()` Race Condition — Wasted Work
**File:** `ui/MediaViewModel.kt`  
**Line:** 295-310

```kotlin
init {
    viewModelScope.launch { fetchTrendingNow() }
    viewModelScope.launch { preloadTrendingCovers() }  // Runs concurrently!
}
```

`preloadTrendingCovers()` reads `_searchUiState.value.trending` immediately, but `fetchTrendingNow()` hasn't completed yet. The trending list is empty, so preloading does nothing. A second call happens after trending loads (triggered by `loadTrending()`), but it's redundant.

**Fix:** Move preloading into `fetchTrendingNow()` after success, or `await` the fetch.

---

### 8. `UpdateScheduler.apply()` Called on Every Configuration Change
**File:** `MainActivity.kt`  
**Line:** 95

```kotlin
LaunchedEffect(Unit) {
    UpdateScheduler.apply(applicationContext, ...)
}
```

While `LaunchedEffect(Unit)` survives recompositions, it re-runs if the composable leaves and re-enters composition (e.g., process death restoration, theme change). WorkManager `enqueueUniqueWork` with `REPLACE` cancels and restarts the worker unnecessarily.

**Fix:** Schedule updates in `Application.onCreate()` or check if already scheduled.

---

### 9. `HomeScreen` `deletePending` Dialog Is Dead Code
**File:** `ui/screens/HomeScreen.kt`  
**Line:** ~520-560

`var deletePending by remember { mutableStateOf<MediaItemEntity?>(null) }` is declared and an AlertDialog is conditionally shown, but `deletePending` is **never assigned**. The single-item delete flow in `HomeScreen` doesn't work; deletion only works via `DetailScreen`.

**Fix:** Either wire up `onDeleteItem` to set `deletePending`, or remove the dead dialog.

---

### 10. `CoverImageStore.fetchBytes()` Uses `HttpURLConnection` Instead of OkHttp
**File:** `ui/components/CoverImageStore.kt`  
**Line:** 160-185

The app already depends on OkHttp, but image fetching uses raw `HttpURLConnection`. This loses connection pooling, HTTP/2, and response caching that OkHttp provides.

**Fix:** Inject the shared `OkHttpClient` and use `okhttp3.Request`.

---

### 11. `StreamingLinksScreen` Re-fetches on Every Recomposition
**File:** `ui/screens/StreamingLinksScreen.kt`  
**Line:** 42

```kotlin
LaunchedEffect(Unit) { onLoad() }
```

If the screen recomposes for any reason (theme change, animation, etc.), `onLoad()` re-triggers the AniList search + streaming API calls.

**Fix:** Use `LaunchedEffect(title)` so it only reloads when the actual parameter changes.

---

### 12. `ProfileScreen` Gallery URI Permission Not Persisted Correctly
**File:** `ui/screens/ProfileScreen.kt`  
**Line:** ~55-65

`ActivityResultContracts.GetContent()` does **not** grant persistable URI permissions by default. `takePersistableUriPermission()` will usually fail silently. The profile picture will disappear after app restart.

**Fix:** Use `ActivityResultContracts.OpenDocument()` with `Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION`, or copy the image to app-private storage.

---

### 13. `CSV Export` Vulnerable to Formula Injection
**File:** `ui/MediaViewModel.kt`  
**Line:** ~680-720

Titles, notes, and genres are wrapped in quotes but not sanitized for CSV formula injection. A malicious title like `=cmd|'!C'!'calc'` will execute when opened in Excel.

**Fix:** Prefix risky fields with a single quote (`'`) or strip leading formula characters (`=`, `+`, `-`, `@`, `\t`, `\r`).

---

### 14. `AboutScreen` Hardcodes App Version
**File:** `ui/screens/AboutScreen.kt`  
**Line:** 36

```kotlin
private const val APP_VERSION = "6.5.0"
```

This is already out of sync with `build.gradle.kts` (`versionName = "6.5.0"`) and will drift. `currentVersionName()` exists in the same file's scope.

**Fix:** Use `currentVersionName(context) ?: "Unknown"`.

---

### 15. `StatsUiState` Recomputed on Every DB Change — O(n²) Risk
**File:** `ui/MediaViewModel.kt`  
**Line:** 145-150

```kotlin
val statsUiState: StateFlow<StatsUiState> = combine(
    dao.getAllItems(),
    categoryDao.getAll()
) { items, categories -> computeStats(items, categories) }
```

`computeStats` iterates the full list 10+ times (`count`, `sumOf`, `groupingBy`, etc.). For large libraries (1000+ items), this runs on every small DB update (progress increment, favorite toggle).

**Fix:** Cache intermediate computations or move heavy stats to a background coroutine with `flowOn(Dispatchers.Default)`.

---

### 16. `WorkManager` `EVERY_LAUNCH` Uses `REPLACE` Policy — Cancels Running Work
**File:** `data/UpdateScheduler.kt`  
**Line:** 25-30

```kotlin
ExistingWorkPolicy.REPLACE,
OneTimeWorkRequestBuilder<UpdateCheckWorker>()...
```

If the user opens the app while an update check is already running, `REPLACE` cancels it and restarts. This wastes bandwidth and delays the check.

**Fix:** Use `KEEP` for `EVERY_LAUNCH`.

---

### 17. `WidgetProvider` Is Non-Functional
**File:** `ui/widget/ShouzeWidgetProvider.kt`

The widget only sets click listeners but never displays any actual data (item counts, titles, etc.). Users see a blank or static widget.

**Fix:** Either implement real widget data binding or remove it to avoid Play Store rejection for non-functional widgets.

---

### 18. `AniListDetailScreen` Duplicate Fallback Logic
**File:** `ui/screens/AniListDetailScreen.kt`  
**Line:** ~155

```kotlin
val coverUrl = media.coverImage?.large
    ?: media.coverImage?.large   // Duplicate!
    ?: media.coverImage?.medium
```

The second `large` fallback is redundant and suggests a copy-paste error. Should fall back to `medium` then a placeholder.

---

## 🟡 Minor Issues

### 19. `Status.values()` Deprecated — Use `entries`
**File:** `ui/screens/HomeScreen.kt`  
**Line:** ~680

```kotlin
Status.values().forEach { status -> ... }
```

In Kotlin 1.9+, `Enum.values()` is deprecated in favor of `Enum.entries`.

---

### 20. `SettingsRepository` Writes Preferences One-by-One
**File:** `data/SettingsRepository.kt`

Every setter calls `prefs.edit().putX().apply()` individually. Setting multiple values (e.g., during onboarding) causes multiple disk writes.

**Fix:** Offer a `bulkUpdate { }` API or use `edit { }` block.

---

### 21. `MediaViewModel` `selectedAniListMedia` Never Cleared
**File:** `ui/MediaViewModel.kt`  
**Line:** ~830

```kotlin
var selectedAniListMedia: AniListMedia? = null
```

After navigating back from `AniListDetailScreen`, the media object stays in memory. With large images/descriptions, this leaks memory.

**Fix:** Expose a `clearSelectedAniListMedia()` method and call it on back navigation.

---

### 22. `consumePendingPreFill()` Is Dead Code
**File:** `ui/MediaViewModel.kt`  
**Line:** ~810-820

`pendingPreFill` and `consumePendingPreFill()` exist but are never referenced in any UI code. Incomplete feature.

**Fix:** Implement the pre-fill flow or remove the dead code.

---

### 23. `DataSyncController` `json` PrettyPrint Disabled But Comment Says Otherwise
**File:** `data/local/DataSyncController.kt`  
**Line:** 48

```kotlin
private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false   // Backups are minified — hard to debug
}
```

Backups are single-line JSON inside a ZIP. For a user-facing backup format, `prettyPrint = true` would help debugging.

---

### 24. `isMinifyEnabled = false` In Release Builds
**File:** `app/build.gradle.kts`  
**Line:** 56

ProGuard/R8 is disabled. The APK is significantly larger than necessary and reverse-engineering is trivial.

**Fix:** Enable `isMinifyEnabled = true` and add ProGuard rules for Room, kotlinx.serialization, and OkHttp.

---

### 25. `DetailEditDialog` Cover URL Regex Is Too Strict
**File:** `ui/components/DetailEditDialog.kt`  
**Line:** ~95

```kotlin
Regex("(?i).+\\.(jpg|jpeg|png|webp|gif)(\\?.*)?\$")
```

Rejects valid image URLs with fragments (`#`), paths without extensions (some CDNs use `?format=webp`), and modern formats like `.avif`.

**Fix:** Allow any URL when using gallery picker; only warn for URLs without known image extensions.

---

### 26. `NotificationChannel` Recreated on Every Worker Run
**File:** `data/UpdateCheckWorker.kt`  
**Line:** 55-58

```kotlin
nm.createNotificationChannel(
    NotificationChannel(CHANNEL_ID, "App updates", ...)
)
```

Creating a channel with the same ID is a no-op, but it's wasteful to do it on every background check.

**Fix:** Create the channel once in `Application.onCreate()`.

---

### 27. `ShareListScreen` Uses String Comparison for Status
**File:** `ui/screens/ShareListScreen.kt`  
**Line:** ~40

```kotlin
items.count { it.status.name == "COMPLETED" }
```

Should use `it.status == Status.COMPLETED` for type safety.

---

### 28. `AiringScheduleScreen` `grouped` `remember` Key Is Unstable
**File:** `ui/screens/AiringScheduleScreen.kt`  
**Line:** 35

```kotlin
val grouped = remember(schedules) { ... }
```

`schedules` is a `List<AiringSchedule>`. If the ViewModel emits a new list instance with the same contents, `remember` recomputes unnecessarily. Use `remember(schedules.size)` or derive from a stable ID set.

---

### 29. `SearchScreen` `trendingLoadedOnce` Is Saveable But Shouldn't Be
**File:** `ui/screens/SearchScreen.kt`  
**Line:** ~95

```kotlin
var trendingLoadedOnce by rememberSaveable { mutableStateOf(false) }
```

Survives process death, meaning trending won't reload after the app is killed and restored. Should be `remember`.

---

### 30. `HomeScreen` `ContinueWatchingCarousel` Auto-Advance Interrupts User
**File:** `ui/screens/HomeScreen.kt`  
**Line:** ~780-800

```kotlin
while (isActive) {
    delay(CAROUSEL_AUTO_ADVANCE_MS)
    if (!pagerState.isScrollInProgress) {
        pagerState.animateScrollToPage(pagerState.currentPage + 1)
    }
}
```

If the user is hovering/focused on the carousel but not actively dragging, it still auto-advances. Should also check `isFocused` or pause when the user interacts.

---

### 31. `DownloadReceiver` Missing Null Check for `cursor`
**File:** `data/UpdateDownloader.kt`  
**Line:** 75

```kotlin
dm.query(...).use { cursor ->
    if (cursor != null && cursor.moveToFirst()) { ... }
}
```

`query()` returns nullable `Cursor`. The `use` extension handles null, but the logic is slightly convoluted.

---

### 32. `UpdateCheckWorker` Returns `Result.retry()` On Network Failure
**File:** `data/UpdateCheckWorker.kt`  
**Line:** 19

```kotlin
val latest = fetchLatestRelease() ?: return Result.retry()
```

If GitHub is down or the device is offline, WorkManager will immediately retry with exponential backoff. For a non-critical update check, `Result.failure()` is more appropriate to avoid battery drain.

---

### 33. `AboutScreen` `SimpleDateFormat` Without Locale
**File:** `ui/screens/AboutScreen.kt`  
**Line:** ~115

```kotlin
SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
```

Using default locale for formatting can produce inconsistent results across devices. Fine for display, but the install date should ideally use `DateTimeFormatter`.

---

### 34. `build.gradle.kts` Missing `isShrinkResources`
**File:** `app/build.gradle.kts`

With `isMinifyEnabled = false`, resources aren't shrunk either. Enabling both would significantly reduce APK size.

---

### 35. `MediaViewModel` `importFromMalXml` Parses XML on Main Thread
**File:** `ui/MediaViewModel.kt`  
**Line:** ~750-850

`parseMalXml()` is called inside `viewModelScope.launch` without `withContext(Dispatchers.Default)`. XML parsing is CPU-intensive and can jank the UI for large MAL exports.

**Fix:** Wrap `parseMalXml()` in `withContext(Dispatchers.Default)`.

---

### 36. `SafeRemoteImage` Caches Content URIs Indefinitely
**File:** `ui/components/SafeRemoteImage.kt`  
**Line:** 35-45

`contentUriCache` is an `LruCache` but never evicted when URI permissions expire. If the user revokes storage permissions, the app shows stale cached images instead of reloading.

---

## 🟢 Suggested Improvements

### 37. Use `by lazy` for `AppDatabase` Singleton
Current manual double-checked locking is verbose and error-prone. `by lazy(LazyThreadSafetyMode.SYNCHRONIZED)` is idiomatic Kotlin.

### 38. Extract Magic Numbers to Constants
- `MAX_DOWNLOAD_BYTES = 24L * 1024 * 1024` (CoverImageStore)
- `CAROUSEL_AUTO_ADVANCE_MS = 4000L` (HomeScreen)
- `MAX_SEARCH_HISTORY = 10` (MediaViewModel)

### 39. Add `flowOn(Dispatchers.IO)` to DAO Flows
Room DAO flows emit on the thread they were collected on. If collected on Main, DB queries run on Main. While Room switches internally for the query, the mapping/filtering doesn't.

### 40. Use `derivedStateOf` for Expensive UI Computations
`HomeScreen` computes `hasSearchOrFilter` and `onClearFilters` lambda on every recomposition. `derivedStateOf` would prevent redundant work.

### 41. Consolidate `friendlyError()` Into a Utility
The same network error mapping logic is duplicated between `MediaViewModel` and could be reused by `UpdateCheckWorker` or other API callers.

### 42. Add `StrictMode` Detection in Debug Builds
Enable `StrictMode` for disk reads/writes and network operations on the main thread to catch regressions during development.

### 43. Version Catalog Could Use BOM for OkHttp
OkHttp 4.12.0 is pinned manually. A BOM would keep OkHttp artifacts in sync if more are added (e.g., logging-interceptor).

### 44. Add `lintOptions` / `detekt` for Code Quality
No static analysis is configured. Adding Detekt or Android Lint would catch many of the issues above automatically.

### 45. Use `ImmutableList` from kotlinx.collections.immutable
Passing `List` through `@Composable` functions breaks Compose's stability inference. Using `ImmutableList` or `@Stable` marker classes would reduce unnecessary recompositions.

### 46. Add `android:enableOnBackInvokedCallback="true"` for Predictive Back
The manifest explicitly disables the modern predictive back gesture (`false`). Enabling it improves UX on Android 13+.

---

## Files Requiring Attention (Priority Order)

| Priority | File | Reason |
|----------|------|--------|
| P0 | `ui/components/CoverImageStore.kt` | Race condition, crashes, HttpURLConnection |
| P0 | `data/remote/AniListApi.kt` | OkHttp leak per instance |
| P0 | `AndroidManifest.xml` | Cleartext traffic security hole |
| P1 | `ui/MediaViewModel.kt` | Performance, race conditions, dead code |
| P1 | `ui/screens/CategoriesScreen.kt` | Color selection ignored |
| P1 | `ui/screens/HomeScreen.kt` | Dead delete dialog, deprecated API |
| P1 | `ui/screens/StreamingLinksScreen.kt` | Recomposition re-fetch |
| P1 | `ui/screens/ProfileScreen.kt` | URI permission bug |
| P2 | `ui/screens/AboutScreen.kt` | Hardcoded version |
| P2 | `ui/screens/BackupScreen.kt` | Dead commented code |
| P2 | `ui/screens/DetailScreen.kt` | Dead commented code |
| P2 | `app/build.gradle.kts` | Minify disabled |
| P3 | `ui/widget/ShouzeWidgetProvider.kt` | Non-functional widget |
| P3 | `data/UpdateScheduler.kt` | WorkManager policy |
| P3 | `data/UpdateCheckWorker.kt` | Notification channel recreation |

---

*End of Audit Report*
