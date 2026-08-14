diff --git a/.opencode/plans/fix-save-crash-data-loss.md b/.opencode/plans/fix-save-crash-data-loss.md
deleted file mode 100644
index 481c058..0000000
--- a/.opencode/plans/fix-save-crash-data-loss.md
+++ /dev/null
@@ -1,75 +0,0 @@
-# Fix: Save-then-crash + data loss + crash loop (Shouze)
-
-## Root cause
-- ViewModel collects Room flows inside `viewModelScope.launch` with no error handler
-  (`MediaViewModel.kt:62-80` init `combine(...).collect()` and `MediaViewModel.kt:53-58`
-  statsUiState upstream). Any DB/query exception (SQLite malformed db / missing column /
-  enum conversion failure) escapes the flow -> uncaught -> whole process crashes, and the
-  UI stays frozen on the initial EMPTY state -> "all data lost, categories gone except All".
-- Most probable trigger for the DB-layer failure: `android:allowBackup="true"`
-  (AndroidManifest.xml). OS auto-restore replaces media_tracker.db WHILE the app is running;
-  the open handle points at a replaced/unlinked file -> next query (save triggers
-  invalidation -> re-query) hits the bogus database -> uncaught crash; on relaunch the
-  half-restored/old empty DB keeps failing -> crash loop + empty UI.
-- Regression: DetailEditDialog Save was `enabled = isTitleValid` only (old code also
-  required categoryId.isNotBlank()), and its `remember` for categoryId is unkeyed so a
-  first-frame empty categories list can produce `categoryId = ""`.
-
-## Changes (exact edits)
-
-### 1. app/src/main/AndroidManifest.xml
-`android:allowBackup="true"` -> `android:allowBackup="false"`
-(stops the OS restore race; app has its own .zip backup via BackupScreen).
-
-### 2. app/src/main/java/com/app/shouze/ui/MediaViewModel.kt
-- Add `import android.util.Log`.
-- init block: wrap `combine(...).collect()` in try/catch; on error log
-  `Log.e("Shouze", "Failed to load library data", e)` and set
-  `_error.value = "Failed to load data: ${e.message}"` +
-  `_uiState.update { it.copy(error = ..., isLoading = false) }`.
-- statsUiState: wrap computeStats in try/catch (return StatsUiState() on failure) and add
-  `.catch { Log.e("Shouze", ...); emit(StatsUiState()) }` before `.stateIn(...)`.
-
-### 3. app/src/main/java/com/app/shouze/data/local/AppDatabase.kt
-- Add `import android.util.Log` and `import java.io.File`.
-- In getInstance builder chain add:
-  `.setQueryCorruptionCallback(object : RoomDatabase.QueryCorruptionCallback() {
-       override fun onCorruption(corruptDbIdentifier: String) {
-           Log.e("Shouze", "Database corruption detected: $corruptDbIdentifier; recreating")
-           INSTANCE?.close()
-           INSTANCE = null
-           runCatching {
-               val file = android.database.DatabaseUtils... // (not used) use:
-               listOf(corruptDbIdentifier, "$corruptDbIdentifier-wal", "$corruptDbIdentifier-shm")
-                   .forEach { File(it).delete() }
-           }
-       }
-   })`
-- On next open Room recreates a fresh DB (onCreate re-seeds the 3 default categories).
-
-### 4. app/src/main/java/com/app/shouze/ui/components/DetailEditDialog.kt
-- `var categoryId by remember(item?.id) { ... }`
-- Add LaunchedEffect to default categoryId to first category for new items when
-  categories arrive async:
-  ```kotlin
-  LaunchedEffect(item?.id, categories.size) {
-      if (item == null && categoryId.isBlank() && categories.isNotEmpty())
-          categoryId = categories.first().id
-  }
-  ```
-  (LaunchedEffect: `androidx.compose.runtime.LaunchedEffect`; already importing
-  `androidx.compose.runtime.*`)
-- Save Button: `enabled = isTitleValid && categoryId.isNotBlank()`.
-
-## Commit/Push (final step, includes pre-existing uncommitted work)
-git add -A
-git commit -m "Fix save crash & data loss: disable auto-backup, harden Room flows, add corruption recovery, require category on save"
-git push origin main
-
-## Build command (run on user's side)
-./gradlew assembleDebug
-APK: app/build/outputs/apk/debug/app-debug.apk
-
-## If a crash still occurs after this build
-App now logs exceptions instead of dying silently:
-adb logcat -s Shouze   (or AndroidIDE crash dialog detail) -> send stack trace back.
\ No newline at end of file
diff --git a/Kimi's-output.md b/Kimi's-output.md
deleted file mode 100644
index b6dcac2..0000000
--- a/Kimi's-output.md
+++ /dev/null
@@ -1,788 +0,0 @@
-Alright, fresh clone is in. Tier 4 — Polish. All five features. Here we go.
-
----
-
-FEATURE 1: Haptic Feedback
-
-NEW FILE: `HapticsHelper.kt`
-
-Create at: `app/src/main/java/com/app/shouze/ui/components/HapticsHelper.kt`
-
-```kotlin
-package com.app.shouze.ui.components
-
-import android.content.Context
-import android.os.Build
-import android.os.VibrationEffect
-import android.os.Vibrator
-import android.view.HapticFeedbackConstants
-import android.view.View
-
-object HapticsHelper {
-    fun performSelectionHaptic(view: View) {
-        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
-            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
-        } else {
-            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
-        }
-    }
-
-    fun performConfirmHaptic(view: View) {
-        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
-            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
-        } else {
-            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
-        }
-    }
-
-    fun performDeleteHaptic(view: View) {
-        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
-            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
-        } else {
-            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
-        }
-    }
-
-    fun performLightClick(context: Context) {
-        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
-        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
-            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
-        }
-    }
-}
-```
-
----
-
-FEATURE 2: Shared Element Transitions (Cover Image)
-
-FILE 1: `MainActivity.kt`
-
-Change 1 — Add import
-After line 14:
-
-```kotlin
-import androidx.compose.animation.SharedTransitionLayout
-```
-
-Change 2 — Wrap NavHost in SharedTransitionLayout
-Find lines 36-37:
-
-```kotlin
-            com.app.shouze.ui.theme.MediaTrackerTheme(settings = settings) {
-                Surface(modifier = Modifier.fillMaxSize()) {
-                    NavHost(
-```
-
-Replace with:
-
-```kotlin
-            com.app.shouze.ui.theme.MediaTrackerTheme(settings = settings) {
-                Surface(modifier = Modifier.fillMaxSize()) {
-                    SharedTransitionLayout {
-                        NavHost(
-```
-
-Change 3 — Close the SharedTransitionLayout brace
-Find line 267:
-
-```kotlin
-                    }
-                }
-            }
-```
-
-Replace with:
-
-```kotlin
-                        }
-                    }
-                }
-            }
-```
-
-Change 4 — Pass animatedVisibilityScope to HomeScreen
-In the `composable("home")` block, change the `HomeScreen` call. After `onTagSelected = viewModel::setTagFilter`, add:
-
-```kotlin
-                                sharedTransitionScope = this@SharedTransitionLayout,
-                                animatedVisibilityScope = this@composable,
-```
-
-Change 5 — Pass animatedVisibilityScope to DetailScreen
-In the `composable("detail/{itemId}")` block, change the `DetailScreen` call. After `onWhereToWatch = { ... }`, add:
-
-```kotlin
-                                    sharedTransitionScope = this@SharedTransitionLayout,
-                                    animatedVisibilityScope = this@composable,
-```
-
----
-
-FILE 2: `HomeScreen.kt`
-
-Change 1 — Add imports
-After line 39:
-
-```kotlin
-import androidx.compose.animation.AnimatedVisibilityScope
-import androidx.compose.animation.SharedTransitionScope
-```
-
-Change 2 — Add parameters to function signature
-After line 68 (`onTagSelected: (String?) -> Unit = {}`), add:
-
-```kotlin
-    sharedTransitionScope: SharedTransitionScope? = null,
-    animatedVisibilityScope: AnimatedVisibilityScope? = null,
-```
-
-Change 3 — Pass shared element keys to MediaCardItem
-In the `MediaCardItem` call inside the LazyColumn (around line 407), after `modifier = Modifier.animateItem()`, add:
-
-```kotlin
-                                sharedTransitionScope = sharedTransitionScope,
-                                animatedVisibilityScope = animatedVisibilityScope,
-```
-
----
-
-FILE 3: `DetailScreen.kt`
-
-Change 1 — Add imports
-After line 34:
-
-```kotlin
-import androidx.compose.animation.AnimatedVisibilityScope
-import androidx.compose.animation.SharedTransitionScope
-```
-
-Change 2 — Add parameters to function signature
-After line 46 (`onWhereToWatch: () -> Unit = {}`), add:
-
-```kotlin
-    sharedTransitionScope: SharedTransitionScope? = null,
-    animatedVisibilityScope: AnimatedVisibilityScope? = null,
-```
-
-Change 3 — Pass shared element keys to CoverBanner
-In the `CoverBanner` call (around line 83), after `status = item.status`, add:
-
-```kotlin
-                sharedTransitionScope = sharedTransitionScope,
-                animatedVisibilityScope = animatedVisibilityScope,
-                sharedElementKey = "cover-${item.id}",
-```
-
----
-
-FILE 4: `MediaCardItem.kt`
-
-Change 1 — Add imports
-After line 24:
-
-```kotlin
-import androidx.compose.animation.AnimatedVisibilityScope
-import androidx.compose.animation.SharedTransitionScope
-```
-
-Change 2 — Add parameters to function signature
-After line 36 (`modifier: Modifier = Modifier`), add:
-
-```kotlin
-    sharedTransitionScope: SharedTransitionScope? = null,
-    animatedVisibilityScope: AnimatedVisibilityScope? = null,
-```
-
-Change 3 — Wrap PosterThumbnail with sharedBounds
-Find the `PosterThumbnail` call inside the Row (around line 60). Replace:
-
-```kotlin
-                PosterThumbnail(
-                    coverUri = item.coverImageUri,
-                    title = item.title,
-                    modifier = Modifier.width(68.dp)
-                )
-```
-
-With:
-
-```kotlin
-                val sharedKey = "cover-${item.id}"
-                val thumbnailModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
-                    with(sharedTransitionScope) {
-                        Modifier.width(68.dp).sharedBounds(
-                            rememberSharedContentState(key = sharedKey),
-                            animatedVisibilityScope = animatedVisibilityScope,
-                            boundsTransform = { _, _ -> androidx.compose.animation.core.tween(400) }
-                        )
-                    }
-                } else {
-                    Modifier.width(68.dp)
-                }
-                PosterThumbnail(
-                    coverUri = item.coverImageUri,
-                    title = item.title,
-                    modifier = thumbnailModifier
-                )
-```
-
-Change 4 — Add haptic on long-press
-Find the `Card` modifier with `combinedClickable` (around line 40). Replace:
-
-```kotlin
-        modifier = modifier
-            .combinedClickable(
-                onClick = onClick,
-                onLongClick = onLongClick
-            )
-```
-
-With:
-
-```kotlin
-        val view = androidx.compose.ui.platform.LocalView.current
-        modifier = modifier
-            .combinedClickable(
-                onClick = onClick,
-                onLongClick = {
-                    com.app.shouze.ui.components.HapticsHelper.performSelectionHaptic(view)
-                    onLongClick?.invoke()
-                }
-            )
-```
-
----
-
-FILE 5: `DetailScreen.kt` — Update CoverBanner
-
-Change 1 — Add parameters to CoverBanner
-Find the `CoverBanner` function signature (around line 272). Change from:
-
-```kotlin
-private fun CoverBanner(
-    coverUri: String?,
-    title: String,
-    categoryName: String,
-    status: Status,
-    modifier: Modifier = Modifier
-) {
-```
-
-To:
-
-```kotlin
-private fun CoverBanner(
-    coverUri: String?,
-    title: String,
-    categoryName: String,
-    status: Status,
-    sharedTransitionScope: SharedTransitionScope? = null,
-    animatedVisibilityScope: AnimatedVisibilityScope? = null,
-    sharedElementKey: String? = null,
-    modifier: Modifier = Modifier
-) {
-```
-
-Change 2 — Wrap SafeRemoteImage with sharedBounds
-Find the `SafeRemoteImage` call inside CoverBanner (around line 286). Replace:
-
-```kotlin
-            SafeRemoteImage(
-                url = coverUri,
-                contentDescription = title,
-                contentScale = ContentScale.Crop,
-                modifier = Modifier.fillMaxSize(),
-                placeholder = { BannerPlaceholder() },
-                errorContent = { BannerPlaceholder(failed = true) }
-            )
-```
-
-With:
-
-```kotlin
-            val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && sharedElementKey != null) {
-                with(sharedTransitionScope) {
-                    Modifier.fillMaxSize().sharedBounds(
-                        rememberSharedContentState(key = sharedElementKey),
-                        animatedVisibilityScope = animatedVisibilityScope,
-                        boundsTransform = { _, _ -> androidx.compose.animation.core.tween(400) }
-                    )
-                }
-            } else {
-                Modifier.fillMaxSize()
-            }
-            SafeRemoteImage(
-                url = coverUri,
-                contentDescription = title,
-                contentScale = ContentScale.Crop,
-                modifier = imageModifier,
-                placeholder = { BannerPlaceholder() },
-                errorContent = { BannerPlaceholder(failed = true) }
-            )
-```
-
-Change 3 — Add haptic on favorite toggle
-Find the `IconButton(onClick = onToggleFavorite)` in the TopAppBar actions (around line 60). Replace:
-
-```kotlin
-                    IconButton(onClick = onToggleFavorite) {
-```
-
-With:
-
-```kotlin
-                    val view = androidx.compose.ui.platform.LocalView.current
-                    IconButton(onClick = {
-                        com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(view)
-                        onToggleFavorite()
-                    }) {
-```
-
-Change 4 — Add haptic on delete
-Find the `IconButton(onClick = { showDeleteConfirm = true })` (around line 70). Replace:
-
-```kotlin
-                    IconButton(onClick = { showDeleteConfirm = true }) {
-```
-
-With:
-
-```kotlin
-                    val view = androidx.compose.ui.platform.LocalView.current
-                    IconButton(onClick = {
-                        com.app.shouze.ui.components.HapticsHelper.performDeleteHaptic(view)
-                        showDeleteConfirm = true
-                    }) {
-```
-
----
-
-FEATURE 3: Home Screen Widget
-
-NEW FILE: `ShouzeWidgetProvider.kt`
-
-Create at: `app/src/main/java/com/app/shouze/ui/widget/ShouzeWidgetProvider.kt`
-
-```kotlin
-package com.app.shouze.ui.widget
-
-import android.app.PendingIntent
-import android.appwidget.AppWidgetManager
-import android.appwidget.AppWidgetProvider
-import android.content.ComponentName
-import android.content.Context
-import android.content.Intent
-import android.net.Uri
-import android.widget.RemoteViews
-import com.app.shouze.MainActivity
-import com.app.shouze.R
-
-class ShouzeWidgetProvider : AppWidgetProvider() {
-    override fun onUpdate(
-        context: Context,
-        appWidgetManager: AppWidgetManager,
-        appWidgetIds: IntArray
-    ) {
-        appWidgetIds.forEach { appWidgetId ->
-            updateAppWidget(context, appWidgetManager, appWidgetId)
-        }
-    }
-
-    companion object {
-        fun updateAppWidget(
-            context: Context,
-            appWidgetManager: AppWidgetManager,
-            appWidgetId: Int
-        ) {
-            val views = RemoteViews(context.packageName, R.layout.widget_layout)
-
-            // Title click opens app
-            val openIntent = Intent(context, MainActivity::class.java).apply {
-                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
-            }
-            val openPending = PendingIntent.getActivity(
-                context, 0, openIntent,
-                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
-            )
-            views.setOnClickPendingIntent(R.id.widget_title, openPending)
-
-            // Refresh button
-            val refreshIntent = Intent(context, ShouzeWidgetProvider::class.java).apply {
-                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
-                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
-            }
-            val refreshPending = PendingIntent.getBroadcast(
-                context, appWidgetId, refreshIntent,
-                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
-            )
-            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending)
-
-            appWidgetManager.updateAppWidget(appWidgetId, views)
-        }
-
-        fun requestUpdate(context: Context) {
-            val appWidgetManager = AppWidgetManager.getInstance(context)
-            val component = ComponentName(context, ShouzeWidgetProvider::class.java)
-            val ids = appWidgetManager.getAppWidgetIds(component)
-            if (ids.isNotEmpty()) {
-                appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
-            }
-        }
-    }
-}
-```
-
-NEW FILE: `widget_layout.xml`
-
-Create at: `app/src/main/res/layout/widget_layout.xml`
-
-```xml
-<?xml version="1.0" encoding="utf-8"?>
-<LinearLayout
-    xmlns:android="http://schemas.android.com/apk/res/android"
-    android:layout_width="match_parent"
-    android:layout_height="wrap_content"
-    android:orientation="vertical"
-    android:background="@android:drawable/dialog_holo_light_frame"
-    android:padding="12dp">
-
-    <LinearLayout
-        android:layout_width="match_parent"
-        android:layout_height="wrap_content"
-        android:orientation="horizontal"
-        android:gravity="center_vertical">
-
-        <TextView
-            android:id="@+id/widget_title"
-            android:layout_width="0dp"
-            android:layout_height="wrap_content"
-            android:layout_weight="1"
-            android:text="Shouze — Up Next"
-            android:textStyle="bold"
-            android:textSize="16sp"
-            android:textColor="@android:color/black" />
-
-        <ImageView
-            android:id="@+id/widget_refresh"
-            android:layout_width="24dp"
-            android:layout_height="24dp"
-            android:src="@android:drawable/ic_menu_refresh"
-            android:contentDescription="Refresh" />
-    </LinearLayout>
-
-    <TextView
-        android:layout_width="match_parent"
-        android:layout_height="wrap_content"
-        android:text="Open app to see your Up Next queue"
-        android:textSize="14sp"
-        android:paddingTop="8dp"
-        android:textColor="@android:color/darker_gray" />
-</LinearLayout>
-```
-
-NEW FILE: `widget_info.xml`
-
-Create at: `app/src/main/res/xml/widget_info.xml`
-
-```xml
-<?xml version="1.0" encoding="utf-8"?>
-<appwidget-provider
-    xmlns:android="http://schemas.android.com/apk/res/android"
-    android:minWidth="180dp"
-    android:minHeight="110dp"
-    android:updatePeriodMillis="86400000"
-    android:previewImage="@mipmap/ic_launcher"
-    android:initialLayout="@layout/widget_layout"
-    android:resizeMode="horizontal|vertical"
-    android:widgetCategory="home_screen" />
-```
-
-FILE: `AndroidManifest.xml`
-
-Change 1 — Add widget receiver
-Before the closing `</application>` tag (line 26), add:
-
-```xml
-        <receiver
-            android:name=".ui.widget.ShouzeWidgetProvider"
-            android:exported="true">
-            <intent-filter>
-                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
-            </intent-filter>
-            <meta-data
-                android:name="android.appwidget.provider"
-                android:resource="@xml/widget_info" />
-        </receiver>
-```
-
----
-
-FEATURE 4: App Shortcuts
-
-NEW FILE: `shortcuts.xml`
-
-Create at: `app/src/main/res/xml/shortcuts.xml`
-
-```xml
-<?xml version="1.0" encoding="utf-8"?>
-<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
-    <shortcut
-        android:shortcutId="add_media"
-        android:enabled="true"
-        android:icon="@mipmap/ic_launcher"
-        android:shortcutShortLabel="@string/shortcut_add_short"
-        android:shortcutLongLabel="@string/shortcut_add_long">
-        <intent
-            android:action="android.intent.action.VIEW"
-            android:targetPackage="com.app.shouze"
-            android:targetClass="com.app.shouze.MainActivity">
-            <extra android:name="shortcut_action" android:value="add" />
-        </intent>
-    </shortcut>
-
-    <shortcut
-        android:shortcutId="search_anilist"
-        android:enabled="true"
-        android:icon="@mipmap/ic_launcher"
-        android:shortcutShortLabel="@string/shortcut_search_short"
-        android:shortcutLongLabel="@string/shortcut_search_long">
-        <intent
-            android:action="android.intent.action.VIEW"
-            android:targetPackage="com.app.shouze"
-            android:targetClass="com.app.shouze.MainActivity">
-            <extra android:name="shortcut_action" android:value="search" />
-        </intent>
-    </shortcut>
-
-    <shortcut
-        android:shortcutId="view_statistics"
-        android:enabled="true"
-        android:icon="@mipmap/ic_launcher"
-        android:shortcutShortLabel="@string/shortcut_stats_short"
-        android:shortcutLongLabel="@string/shortcut_stats_long">
-        <intent
-            android:action="android.intent.action.VIEW"
-            android:targetPackage="com.app.shouze"
-            android:targetClass="com.app.shouze.MainActivity">
-            <extra android:name="shortcut_action" android:value="statistics" />
-        </intent>
-    </shortcut>
-</shortcuts>
-```
-
-FILE: `AndroidManifest.xml`
-
-Change 1 — Add shortcuts metadata to MainActivity
-Inside the `<activity android:name=".MainActivity">` block, after the `</intent-filter>` (line 24), add:
-
-```xml
-            <meta-data
-                android:name="android.app.shortcuts"
-                android:resource="@xml/shortcuts" />
-```
-
-FILE: `MainActivity.kt`
-
-Change 1 — Add shortcut handling in onCreate
-After `enableEdgeToEdge()` (line 24), add:
-
-```kotlin
-        handleShortcutIntent(intent)
-```
-
-Change 2 — Add helper methods in MainActivity
-Before the closing `}` of the class (line 272), add:
-
-```kotlin
-    override fun onNewIntent(intent: Intent?) {
-        super.onNewIntent(intent)
-        intent?.let { handleShortcutIntent(it) }
-    }
-
-    private fun handleShortcutIntent(intent: Intent) {
-        when (intent.getStringExtra("shortcut_action")) {
-            "add" -> {
-                // Navigate to add screen — handled via ViewModel state or deep link
-            }
-            "search" -> {
-                // Navigate to search
-            }
-            "statistics" -> {
-                // Navigate to stats
-            }
-        }
-    }
-```
-
-Actually, handling shortcuts in Compose Navigation requires a different approach. The shortcut launches MainActivity with an extra, and we need to read that extra in the Compose layer. Let me modify the approach:
-
-In `MainActivity.kt`, inside `setContent`, after `val navController = rememberNavController()`, add:
-
-```kotlin
-            val shortcutAction = remember(intent) {
-                intent.getStringExtra("shortcut_action")
-            }
-            LaunchedEffect(shortcutAction) {
-                when (shortcutAction) {
-                    "add" -> navController.navigate("edit?itemId=null")
-                    "search" -> navController.navigate("search")
-                    "statistics" -> navController.navigate("statistics")
-                }
-            }
-```
-
-And also add dynamic shortcuts. Let me add that too.
-
-Change 3 — Add dynamic shortcuts creation
-After `handleShortcutIntent(intent)` (the new line), add:
-
-```kotlin
-        createDynamicShortcuts()
-```
-
-Change 4 — Add the method
-Before the closing `}` of the class, add:
-
-```kotlin
-    private fun createDynamicShortcuts() {
-        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
-            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java) ?: return
-            val shortcuts = listOf(
-                android.content.pm.ShortcutInfo.Builder(this, "dynamic_add")
-                    .setShortLabel("Quick Add")
-                    .setLongLabel("Add new media entry")
-                    .setIcon(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_input_add))
-                    .setIntent(Intent(this, MainActivity::class.java).apply {
-                        action = Intent.ACTION_VIEW
-                        putExtra("shortcut_action", "add")
-                    })
-                    .build()
-            )
-            shortcutManager.dynamicShortcuts = shortcuts
-        }
-    }
-```
-
----
-
-FEATURE 5: Animated Empty States
-
-FILE: `HomeScreen.kt`
-
-Change 1 — Replace the EmptyState composable
-Find the `EmptyState` function (around line 538). Replace the entire function with:
-
-```kotlin
-@Composable
-private fun EmptyState(
-    hasSearchOrFilter: Boolean,
-    modifier: Modifier = Modifier
-) {
-    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
-    val floatAnim by infiniteTransition.animateFloat(
-        initialValue = 0f,
-        targetValue = 1f,
-        animationSpec = infiniteRepeatable(
-            animation = tween(2000, easing = EaseInOutSine),
-            repeatMode = RepeatMode.Reverse
-        ),
-        label = "float"
-    )
-
-    Column(
-        modifier = modifier,
-        horizontalAlignment = Alignment.CenterHorizontally,
-        verticalArrangement = Arrangement.Center
-    ) {
-        Box(
-            modifier = Modifier.size(120.dp),
-            contentAlignment = Alignment.Center
-        ) {
-            // Floating background circle
-            Box(
-                modifier = Modifier
-                    .size(80.dp + (floatAnim * 20).dp)
-                    .background(
-                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
-                        shape = CircleShape
-                    )
-            )
-            // Icon with subtle bounce
-            Icon(
-                imageVector = Icons.Filled.MovieFilter,
-                contentDescription = null,
-                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f + (floatAnim * 0.2f)),
-                modifier = Modifier.size(64.dp)
-            )
-        }
-        Spacer(modifier = Modifier.height(24.dp))
-        Text(
-            text = if (hasSearchOrFilter) "No results found" else "Your library is empty",
-            style = MaterialTheme.typography.titleLarge,
-            color = MaterialTheme.colorScheme.onSurface
-        )
-        Spacer(modifier = Modifier.height(8.dp))
-        Text(
-            text = if (hasSearchOrFilter) {
-                "Try changing your search or filter."
-            } else {
-                "Tap the '+' button to add your first entry"
-            },
-            style = MaterialTheme.typography.bodyLarge,
-            color = MaterialTheme.colorScheme.onSurfaceVariant
-        )
-    }
-}
-```
-
-Change 2 — Add imports
-After line 7:
-
-```kotlin
-import androidx.compose.animation.core.*
-```
-
----
-
-FILE: `strings.xml` — Add shortcut strings
-
-Find or create: `app/src/main/res/values/strings.xml`
-
-If it exists, add these inside `<resources>`:
-
-```xml
-    <string name="shortcut_add_short">Add Media</string>
-    <string name="shortcut_add_long">Add a new media entry</string>
-    <string name="shortcut_search_short">Search AniList</string>
-    <string name="shortcut_search_long">Search AniList database</string>
-    <string name="shortcut_stats_short">Statistics</string>
-    <string name="shortcut_stats_long">View library statistics</string>
-```
-
-If the file doesn't exist, create it with:
-
-```xml
-<?xml version="1.0" encoding="utf-8"?>
-<resources>
-    <string name="app_name">Shouze</string>
-    <string name="shortcut_add_short">Add Media</string>
-    <string name="shortcut_add_long">Add a new media entry</string>
-    <string name="shortcut_search_short">Search AniList</string>
-    <string name="shortcut_search_long">Search AniList database</string>
-    <string name="shortcut_stats_short">Statistics</string>
-    <string name="shortcut_stats_long">View library statistics</string>
-</resources>
-```
-
----
-
-That's all of Tier 4. Five polish features:
-
-1. Haptics — Selection, confirm, delete feedback on supported devices
-2. Shared element transitions — Cover image smoothly animates between list and detail
-3. Home screen widget — Simple widget showing Shouze branding with refresh
-4. App shortcuts — Static shortcuts (Add, Search, Stats) + dynamic "Quick Add"
-5. Animated empty states — Pulsing background circle and floating icon instead of static dead screen
-
-Build, test, and let me know if anything throws a red squiggle.
\ No newline at end of file
diff --git a/app/build.gradle.kts b/app/build.gradle.kts
index 69a3fea..72f68a2 100644
--- a/app/build.gradle.kts
+++ b/app/build.gradle.kts
@@ -15,7 +15,7 @@ android {
         minSdk = 24
         targetSdk = 36
         versionCode = 2
-        versionName = "1.2"
+        versionName = "1.3"
         testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
     }
 
diff --git a/app/src/main/java/com/app/shouze/MainActivity.kt b/app/src/main/java/com/app/shouze/MainActivity.kt
index 2156f32..883695e 100644
--- a/app/src/main/java/com/app/shouze/MainActivity.kt
+++ b/app/src/main/java/com/app/shouze/MainActivity.kt
@@ -129,6 +129,21 @@ class MainActivity : ComponentActivity() {
                                 }
                             )
                         }
+                        composable("anidetail") {
+                            val media = viewModel.selectedAniListMedia
+                            if (media != null) {
+                                AniListDetailScreen(
+                                    media = media,
+                                    onBack = { navController.popBackStack() },
+                                    onAdd = { m, status ->
+                                        viewModel.addOrUpdate(
+                                            viewModel.createItemFromAniList(m, status)
+                                        )
+                                    }
+                                )
+                            }
+                        }
+
                         composable("search") {
                             SearchScreen(
                                 uiState = searchUiState,
@@ -140,11 +155,8 @@ class MainActivity : ComponentActivity() {
                                 onTypeChange = viewModel::setSearchType,
                                 onLoadTrending = viewModel::loadTrending,
                                 onSelect = { media ->
-                                    val item = viewModel.createItemFromAniList(media)
-                                    viewModel.setPendingPreFill(item)
-                                    navController.navigate("edit?itemId=null") {
-                                        popUpTo("search") { inclusive = true }
-                                    }
+                                    viewModel.selectAniListMedia(media)
+                                    navController.navigate("anidetail")
                                 }
                             )
                         }
@@ -357,8 +369,6 @@ class MainActivity : ComponentActivity() {
         }
         }
 
-    }
-
     override fun onNewIntent(intent: android.content.Intent) {
         super.onNewIntent(intent)
         setIntent(intent)
@@ -385,29 +395,4 @@ class MainActivity : ComponentActivity() {
         }
     }
 
-    override fun onNewIntent(intent: android.content.Intent) {
-        super.onNewIntent(intent)
-        setIntent(intent)
-        intent.getStringExtra("shortcut_action")?.let {
-            shortcutActions.tryEmit(it)
-        }
-    }
-
-    private fun createDynamicShortcuts() {
-        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
-            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java) ?: return
-            val shortcuts = listOf(
-                android.content.pm.ShortcutInfo.Builder(this, "dynamic_add")
-                    .setShortLabel("Quick Add")
-                    .setLongLabel("Add new media entry")
-                    .setIcon(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_input_add))
-                    .setIntent(android.content.Intent(this, MainActivity::class.java).apply {
-                        action = android.content.Intent.ACTION_VIEW
-                        putExtra("shortcut_action", "add")
-                    })
-                    .build()
-            )
-            shortcutManager.dynamicShortcuts = shortcuts
-        }
-    }
 }
diff --git a/app/src/main/java/com/app/shouze/ui/MediaViewModel.kt b/app/src/main/java/com/app/shouze/ui/MediaViewModel.kt
index e3bafe6..6cef66e 100644
--- a/app/src/main/java/com/app/shouze/ui/MediaViewModel.kt
+++ b/app/src/main/java/com/app/shouze/ui/MediaViewModel.kt
@@ -529,7 +529,17 @@ class MediaViewModel(application: Application) : AndroidViewModel(application) {
         return item
     }
 
-    fun createItemFromAniList(media: AniListMedia): MediaItemEntity {
+    var selectedAniListMedia: AniListMedia? = null
+        private set
+
+    fun selectAniListMedia(media: AniListMedia) {
+        selectedAniListMedia = media
+    }
+
+    fun createItemFromAniList(
+        media: AniListMedia,
+        defaultStatus: Status = Status.PLAN_TO_WATCH
+    ): MediaItemEntity {
         val title = media.title.english ?: media.title.romaji ?: "Unknown"
         
         // For anime: use episodes. For manga: use chapters, fall back to volumes.
@@ -576,7 +586,7 @@ class MediaViewModel(application: Application) : AndroidViewModel(application) {
         return MediaItemEntity(
             title = title,
             categoryId = categoryId,
-            status = Status.PLAN_TO_WATCH,
+            status = defaultStatus,
             currentProgress = 0,
             totalCount = totalCount,
             rating = 0.0,
diff --git a/app/src/main/java/com/app/shouze/ui/components/DetailEditDialog.kt b/app/src/main/java/com/app/shouze/ui/components/DetailEditDialog.kt
index a792325..be76df9 100644
--- a/app/src/main/java/com/app/shouze/ui/components/DetailEditDialog.kt
+++ b/app/src/main/java/com/app/shouze/ui/components/DetailEditDialog.kt
@@ -154,6 +154,8 @@ fun DetailEditDialog(
                 modifier = Modifier
                     .padding(20.dp)
                     .verticalScroll(rememberScrollState())
+                    .navigationBarsPadding()
+                    .imePadding()
             ) {
                 Text(
                     text = if (item == null) "Add New Item" else "Edit Item",
diff --git a/app/src/main/java/com/app/shouze/ui/screens/ProfileScreen.kt b/app/src/main/java/com/app/shouze/ui/screens/ProfileScreen.kt
index 6cb0126..cf4b216 100644
--- a/app/src/main/java/com/app/shouze/ui/screens/ProfileScreen.kt
+++ b/app/src/main/java/com/app/shouze/ui/screens/ProfileScreen.kt
@@ -25,7 +25,7 @@ import androidx.compose.ui.text.style.TextOverflow
 import androidx.compose.ui.unit.dp
 import com.app.shouze.ui.components.SafeRemoteImage
 
-@OptIn(ExperimentalMaterial3Api::class)
+@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
 @Composable
 fun ProfileScreen(
     username: String,
@@ -79,14 +79,29 @@ fun ProfileScreen(
                         .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                 ) {
                     if (!profilePictureUri.isNullOrBlank()) {
-                        SafeRemoteImage(
-                            url = profilePictureUri,
-                            contentDescription = "Profile picture",
-                            modifier = Modifier.fillMaxSize(),
-                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
-                            placeholder = { ProfileInitials(username) },
-                            errorContent = { ProfileInitials(username) }
-                        )
+                        if (profilePictureUri.startsWith("emoji:")) {
+                            Box(
+                                modifier = Modifier
+                                    .fillMaxSize()
+                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
+                                contentAlignment = Alignment.Center
+                            ) {
+                                Text(
+                                    text = profilePictureUri.removePrefix("emoji:"),
+                                    style = MaterialTheme.typography.displayMedium,
+                                    color = MaterialTheme.colorScheme.onPrimaryContainer
+                                )
+                            }
+                        } else {
+                            SafeRemoteImage(
+                                url = profilePictureUri,
+                                contentDescription = "Profile picture",
+                                modifier = Modifier.fillMaxSize(),
+                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
+                                placeholder = { ProfileInitials(username) },
+                                errorContent = { ProfileInitials(username) }
+                            )
+                        }
                     } else {
                         ProfileInitials(username)
                     }
@@ -155,7 +170,15 @@ fun ProfileScreen(
                     value = text,
                     onValueChange = { text = it },
                     singleLine = true,
-                    placeholder = { Text("Enter a username") }
+                    placeholder = { Text("Enter a username") },
+                    trailingIcon = {
+                        IconButton(onClick = { text = randomAnimeUsername() }) {
+                            Icon(
+                                Icons.Filled.Refresh,
+                                contentDescription = "Generate random username"
+                            )
+                        }
+                    }
                 )
             },
             confirmButton = {
@@ -171,12 +194,58 @@ fun ProfileScreen(
     }
 
     if (showPictureDialog) {
+        var avatarTab by remember { mutableStateOf(0) }
         AlertDialog(
             onDismissRequest = { showPictureDialog = false },
             title = { Text("Change profile picture") },
             text = {
-                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
-                    Text("Choose how to set your profile picture.")
+                Column(
+                    modifier = Modifier
+                        .fillMaxWidth()
+                        .verticalScroll(rememberScrollState())
+                ) {
+                    Text(
+                        "Pick a default avatar or upload your own — your call!",
+                        style = MaterialTheme.typography.bodyMedium,
+                        color = MaterialTheme.colorScheme.onSurfaceVariant
+                    )
+                    Spacer(Modifier.height(12.dp))
+                    TabRow(selectedTabIndex = avatarTab) {
+                        AVATAR_PRESETS.forEachIndexed { index, (name, _) ->
+                            Tab(
+                                selected = avatarTab == index,
+                                onClick = { avatarTab = index },
+                                text = { Text(name) }
+                            )
+                        }
+                    }
+                    Spacer(Modifier.height(12.dp))
+                    FlowRow(
+                        horizontalArrangement = Arrangement.spacedBy(12.dp),
+                        verticalArrangement = Arrangement.spacedBy(12.dp)
+                    ) {
+                        AVATAR_PRESETS[avatarTab].second.forEach { emoji ->
+                            Surface(
+                                modifier = Modifier
+                                    .size(56.dp)
+                                    .clip(CircleShape)
+                                    .clickable {
+                                        onProfilePictureChange("emoji:$emoji")
+                                        showPictureDialog = false
+                                    },
+                                color = MaterialTheme.colorScheme.primaryContainer,
+                                shape = CircleShape
+                            ) {
+                                Box(contentAlignment = Alignment.Center) {
+                                    Text(
+                                        text = emoji,
+                                        style = MaterialTheme.typography.headlineMedium,
+                                        color = MaterialTheme.colorScheme.onPrimaryContainer
+                                    )
+                                }
+                            }
+                        }
+                    }
                 }
             },
             confirmButton = {
@@ -281,3 +350,26 @@ private fun ProfileMenuItem(
         )
     }
 }
+
+private val AVATAR_PRESETS = listOf(
+    "Chibi" to listOf("🐱", "🐶", "🐰", "🐼", "🦊", "🐲", "🐥", "🐙"),
+    "Shonen" to listOf("⚔️", "🔥", "💥", "⚡", "🥷", "🦸", "🚀", "🌪️"),
+    "Minimalist" to listOf("⭐", "🌸", "🌟", "🌙", "💎", "🍃", "☀️", "🌈")
+)
+
+private val USERNAME_ADJECTIVES = listOf(
+    "Shadow", "Neon", "Crimson", "Silent", "Lunar",
+    "Electric", "Hidden", "Mystic", "Rapid", "Frozen"
+)
+
+private val USERNAME_NOUNS = listOf(
+    "Shinobi", "Otaku", "Samurai", "Ronin", "Kitsune",
+    "Titan", "Reaper", "Phantom", "Saiyan", "Wolf"
+)
+
+private fun randomAnimeUsername(): String {
+    val adj = USERNAME_ADJECTIVES.random()
+    val noun = USERNAME_NOUNS.random()
+    val num = (10..99).random()
+    return "$adj$noun$num"
+}
