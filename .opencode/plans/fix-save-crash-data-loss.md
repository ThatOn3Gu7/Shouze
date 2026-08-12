# Fix: Save-then-crash + data loss + crash loop (Shouze)

## Root cause
- ViewModel collects Room flows inside `viewModelScope.launch` with no error handler
  (`MediaViewModel.kt:62-80` init `combine(...).collect()` and `MediaViewModel.kt:53-58`
  statsUiState upstream). Any DB/query exception (SQLite malformed db / missing column /
  enum conversion failure) escapes the flow -> uncaught -> whole process crashes, and the
  UI stays frozen on the initial EMPTY state -> "all data lost, categories gone except All".
- Most probable trigger for the DB-layer failure: `android:allowBackup="true"`
  (AndroidManifest.xml). OS auto-restore replaces media_tracker.db WHILE the app is running;
  the open handle points at a replaced/unlinked file -> next query (save triggers
  invalidation -> re-query) hits the bogus database -> uncaught crash; on relaunch the
  half-restored/old empty DB keeps failing -> crash loop + empty UI.
- Regression: DetailEditDialog Save was `enabled = isTitleValid` only (old code also
  required categoryId.isNotBlank()), and its `remember` for categoryId is unkeyed so a
  first-frame empty categories list can produce `categoryId = ""`.

## Changes (exact edits)

### 1. app/src/main/AndroidManifest.xml
`android:allowBackup="true"` -> `android:allowBackup="false"`
(stops the OS restore race; app has its own .zip backup via BackupScreen).

### 2. app/src/main/java/com/app/shouze/ui/MediaViewModel.kt
- Add `import android.util.Log`.
- init block: wrap `combine(...).collect()` in try/catch; on error log
  `Log.e("Shouze", "Failed to load library data", e)` and set
  `_error.value = "Failed to load data: ${e.message}"` +
  `_uiState.update { it.copy(error = ..., isLoading = false) }`.
- statsUiState: wrap computeStats in try/catch (return StatsUiState() on failure) and add
  `.catch { Log.e("Shouze", ...); emit(StatsUiState()) }` before `.stateIn(...)`.

### 3. app/src/main/java/com/app/shouze/data/local/AppDatabase.kt
- Add `import android.util.Log` and `import java.io.File`.
- In getInstance builder chain add:
  `.setQueryCorruptionCallback(object : RoomDatabase.QueryCorruptionCallback() {
       override fun onCorruption(corruptDbIdentifier: String) {
           Log.e("Shouze", "Database corruption detected: $corruptDbIdentifier; recreating")
           INSTANCE?.close()
           INSTANCE = null
           runCatching {
               val file = android.database.DatabaseUtils... // (not used) use:
               listOf(corruptDbIdentifier, "$corruptDbIdentifier-wal", "$corruptDbIdentifier-shm")
                   .forEach { File(it).delete() }
           }
       }
   })`
- On next open Room recreates a fresh DB (onCreate re-seeds the 3 default categories).

### 4. app/src/main/java/com/app/shouze/ui/components/DetailEditDialog.kt
- `var categoryId by remember(item?.id) { ... }`
- Add LaunchedEffect to default categoryId to first category for new items when
  categories arrive async:
  ```kotlin
  LaunchedEffect(item?.id, categories.size) {
      if (item == null && categoryId.isBlank() && categories.isNotEmpty())
          categoryId = categories.first().id
  }
  ```
  (LaunchedEffect: `androidx.compose.runtime.LaunchedEffect`; already importing
  `androidx.compose.runtime.*`)
- Save Button: `enabled = isTitleValid && categoryId.isNotBlank()`.

## Commit/Push (final step, includes pre-existing uncommitted work)
git add -A
git commit -m "Fix save crash & data loss: disable auto-backup, harden Room flows, add corruption recovery, require category on save"
git push origin main

## Build command (run on user's side)
./gradlew assembleDebug
APK: app/build/outputs/apk/debug/app-debug.apk

## If a crash still occurs after this build
App now logs exceptions instead of dying silently:
adb logcat -s Shouze   (or AndroidIDE crash dialog detail) -> send stack trace back.