Alright, fresh clone is in. Tier 4 — Polish. All five features. Here we go.

---

FEATURE 1: Haptic Feedback

NEW FILE: `HapticsHelper.kt`

Create at: `app/src/main/java/com/app/shouze/ui/components/HapticsHelper.kt`

```kotlin
package com.app.shouze.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View

object HapticsHelper {
    fun performSelectionHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun performConfirmHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun performDeleteHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun performLightClick(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }
}
```

---

FEATURE 2: Shared Element Transitions (Cover Image)

FILE 1: `MainActivity.kt`

Change 1 — Add import
After line 14:

```kotlin
import androidx.compose.animation.SharedTransitionLayout
```

Change 2 — Wrap NavHost in SharedTransitionLayout
Find lines 36-37:

```kotlin
            com.app.shouze.ui.theme.MediaTrackerTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
```

Replace with:

```kotlin
            com.app.shouze.ui.theme.MediaTrackerTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SharedTransitionLayout {
                        NavHost(
```

Change 3 — Close the SharedTransitionLayout brace
Find line 267:

```kotlin
                    }
                }
            }
```

Replace with:

```kotlin
                        }
                    }
                }
            }
```

Change 4 — Pass animatedVisibilityScope to HomeScreen
In the `composable("home")` block, change the `HomeScreen` call. After `onTagSelected = viewModel::setTagFilter`, add:

```kotlin
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
```

Change 5 — Pass animatedVisibilityScope to DetailScreen
In the `composable("detail/{itemId}")` block, change the `DetailScreen` call. After `onWhereToWatch = { ... }`, add:

```kotlin
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
```

---

FILE 2: `HomeScreen.kt`

Change 1 — Add imports
After line 39:

```kotlin
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
```

Change 2 — Add parameters to function signature
After line 68 (`onTagSelected: (String?) -> Unit = {}`), add:

```kotlin
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
```

Change 3 — Pass shared element keys to MediaCardItem
In the `MediaCardItem` call inside the LazyColumn (around line 407), after `modifier = Modifier.animateItem()`, add:

```kotlin
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
```

---

FILE 3: `DetailScreen.kt`

Change 1 — Add imports
After line 34:

```kotlin
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
```

Change 2 — Add parameters to function signature
After line 46 (`onWhereToWatch: () -> Unit = {}`), add:

```kotlin
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
```

Change 3 — Pass shared element keys to CoverBanner
In the `CoverBanner` call (around line 83), after `status = item.status`, add:

```kotlin
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedElementKey = "cover-${item.id}",
```

---

FILE 4: `MediaCardItem.kt`

Change 1 — Add imports
After line 24:

```kotlin
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
```

Change 2 — Add parameters to function signature
After line 36 (`modifier: Modifier = Modifier`), add:

```kotlin
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
```

Change 3 — Wrap PosterThumbnail with sharedBounds
Find the `PosterThumbnail` call inside the Row (around line 60). Replace:

```kotlin
                PosterThumbnail(
                    coverUri = item.coverImageUri,
                    title = item.title,
                    modifier = Modifier.width(68.dp)
                )
```

With:

```kotlin
                val sharedKey = "cover-${item.id}"
                val thumbnailModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.width(68.dp).sharedBounds(
                            rememberSharedContentState(key = sharedKey),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> androidx.compose.animation.core.tween(400) }
                        )
                    }
                } else {
                    Modifier.width(68.dp)
                }
                PosterThumbnail(
                    coverUri = item.coverImageUri,
                    title = item.title,
                    modifier = thumbnailModifier
                )
```

Change 4 — Add haptic on long-press
Find the `Card` modifier with `combinedClickable` (around line 40). Replace:

```kotlin
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
```

With:

```kotlin
        val view = androidx.compose.ui.platform.LocalView.current
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    com.app.shouze.ui.components.HapticsHelper.performSelectionHaptic(view)
                    onLongClick?.invoke()
                }
            )
```

---

FILE 5: `DetailScreen.kt` — Update CoverBanner

Change 1 — Add parameters to CoverBanner
Find the `CoverBanner` function signature (around line 272). Change from:

```kotlin
private fun CoverBanner(
    coverUri: String?,
    title: String,
    categoryName: String,
    status: Status,
    modifier: Modifier = Modifier
) {
```

To:

```kotlin
private fun CoverBanner(
    coverUri: String?,
    title: String,
    categoryName: String,
    status: Status,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedElementKey: String? = null,
    modifier: Modifier = Modifier
) {
```

Change 2 — Wrap SafeRemoteImage with sharedBounds
Find the `SafeRemoteImage` call inside CoverBanner (around line 286). Replace:

```kotlin
            SafeRemoteImage(
                url = coverUri,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = { BannerPlaceholder() },
                errorContent = { BannerPlaceholder(failed = true) }
            )
```

With:

```kotlin
            val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && sharedElementKey != null) {
                with(sharedTransitionScope) {
                    Modifier.fillMaxSize().sharedBounds(
                        rememberSharedContentState(key = sharedElementKey),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> androidx.compose.animation.core.tween(400) }
                    )
                }
            } else {
                Modifier.fillMaxSize()
            }
            SafeRemoteImage(
                url = coverUri,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
                placeholder = { BannerPlaceholder() },
                errorContent = { BannerPlaceholder(failed = true) }
            )
```

Change 3 — Add haptic on favorite toggle
Find the `IconButton(onClick = onToggleFavorite)` in the TopAppBar actions (around line 60). Replace:

```kotlin
                    IconButton(onClick = onToggleFavorite) {
```

With:

```kotlin
                    val view = androidx.compose.ui.platform.LocalView.current
                    IconButton(onClick = {
                        com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(view)
                        onToggleFavorite()
                    }) {
```

Change 4 — Add haptic on delete
Find the `IconButton(onClick = { showDeleteConfirm = true })` (around line 70). Replace:

```kotlin
                    IconButton(onClick = { showDeleteConfirm = true }) {
```

With:

```kotlin
                    val view = androidx.compose.ui.platform.LocalView.current
                    IconButton(onClick = {
                        com.app.shouze.ui.components.HapticsHelper.performDeleteHaptic(view)
                        showDeleteConfirm = true
                    }) {
```

---

FEATURE 3: Home Screen Widget

NEW FILE: `ShouzeWidgetProvider.kt`

Create at: `app/src/main/java/com/app/shouze/ui/widget/ShouzeWidgetProvider.kt`

```kotlin
package com.app.shouze.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.app.shouze.MainActivity
import com.app.shouze.R

class ShouzeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Title click opens app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openPending)

            // Refresh button
            val refreshIntent = Intent(context, ShouzeWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val refreshPending = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun requestUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ShouzeWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            }
        }
    }
}
```

NEW FILE: `widget_layout.xml`

Create at: `app/src/main/res/layout/widget_layout.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@android:drawable/dialog_holo_light_frame"
    android:padding="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <TextView
            android:id="@+id/widget_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Shouze — Up Next"
            android:textStyle="bold"
            android:textSize="16sp"
            android:textColor="@android:color/black" />

        <ImageView
            android:id="@+id/widget_refresh"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:src="@android:drawable/ic_menu_refresh"
            android:contentDescription="Refresh" />
    </LinearLayout>

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Open app to see your Up Next queue"
        android:textSize="14sp"
        android:paddingTop="8dp"
        android:textColor="@android:color/darker_gray" />
</LinearLayout>
```

NEW FILE: `widget_info.xml`

Create at: `app/src/main/res/xml/widget_info.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp"
    android:minHeight="110dp"
    android:updatePeriodMillis="86400000"
    android:previewImage="@mipmap/ic_launcher"
    android:initialLayout="@layout/widget_layout"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
```

FILE: `AndroidManifest.xml`

Change 1 — Add widget receiver
Before the closing `</application>` tag (line 26), add:

```xml
        <receiver
            android:name=".ui.widget.ShouzeWidgetProvider"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_info" />
        </receiver>
```

---

FEATURE 4: App Shortcuts

NEW FILE: `shortcuts.xml`

Create at: `app/src/main/res/xml/shortcuts.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="add_media"
        android:enabled="true"
        android:icon="@mipmap/ic_launcher"
        android:shortcutShortLabel="@string/shortcut_add_short"
        android:shortcutLongLabel="@string/shortcut_add_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.app.shouze"
            android:targetClass="com.app.shouze.MainActivity">
            <extra android:name="shortcut_action" android:value="add" />
        </intent>
    </shortcut>

    <shortcut
        android:shortcutId="search_anilist"
        android:enabled="true"
        android:icon="@mipmap/ic_launcher"
        android:shortcutShortLabel="@string/shortcut_search_short"
        android:shortcutLongLabel="@string/shortcut_search_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.app.shouze"
            android:targetClass="com.app.shouze.MainActivity">
            <extra android:name="shortcut_action" android:value="search" />
        </intent>
    </shortcut>

    <shortcut
        android:shortcutId="view_statistics"
        android:enabled="true"
        android:icon="@mipmap/ic_launcher"
        android:shortcutShortLabel="@string/shortcut_stats_short"
        android:shortcutLongLabel="@string/shortcut_stats_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.app.shouze"
            android:targetClass="com.app.shouze.MainActivity">
            <extra android:name="shortcut_action" android:value="statistics" />
        </intent>
    </shortcut>
</shortcuts>
```

FILE: `AndroidManifest.xml`

Change 1 — Add shortcuts metadata to MainActivity
Inside the `<activity android:name=".MainActivity">` block, after the `</intent-filter>` (line 24), add:

```xml
            <meta-data
                android:name="android.app.shortcuts"
                android:resource="@xml/shortcuts" />
```

FILE: `MainActivity.kt`

Change 1 — Add shortcut handling in onCreate
After `enableEdgeToEdge()` (line 24), add:

```kotlin
        handleShortcutIntent(intent)
```

Change 2 — Add helper methods in MainActivity
Before the closing `}` of the class (line 272), add:

```kotlin
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleShortcutIntent(it) }
    }

    private fun handleShortcutIntent(intent: Intent) {
        when (intent.getStringExtra("shortcut_action")) {
            "add" -> {
                // Navigate to add screen — handled via ViewModel state or deep link
            }
            "search" -> {
                // Navigate to search
            }
            "statistics" -> {
                // Navigate to stats
            }
        }
    }
```

Actually, handling shortcuts in Compose Navigation requires a different approach. The shortcut launches MainActivity with an extra, and we need to read that extra in the Compose layer. Let me modify the approach:

In `MainActivity.kt`, inside `setContent`, after `val navController = rememberNavController()`, add:

```kotlin
            val shortcutAction = remember(intent) {
                intent.getStringExtra("shortcut_action")
            }
            LaunchedEffect(shortcutAction) {
                when (shortcutAction) {
                    "add" -> navController.navigate("edit?itemId=null")
                    "search" -> navController.navigate("search")
                    "statistics" -> navController.navigate("statistics")
                }
            }
```

And also add dynamic shortcuts. Let me add that too.

Change 3 — Add dynamic shortcuts creation
After `handleShortcutIntent(intent)` (the new line), add:

```kotlin
        createDynamicShortcuts()
```

Change 4 — Add the method
Before the closing `}` of the class, add:

```kotlin
    private fun createDynamicShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java) ?: return
            val shortcuts = listOf(
                android.content.pm.ShortcutInfo.Builder(this, "dynamic_add")
                    .setShortLabel("Quick Add")
                    .setLongLabel("Add new media entry")
                    .setIcon(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_input_add))
                    .setIntent(Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("shortcut_action", "add")
                    })
                    .build()
            )
            shortcutManager.dynamicShortcuts = shortcuts
        }
    }
```

---

FEATURE 5: Animated Empty States

FILE: `HomeScreen.kt`

Change 1 — Replace the EmptyState composable
Find the `EmptyState` function (around line 538). Replace the entire function with:

```kotlin
@Composable
private fun EmptyState(
    hasSearchOrFilter: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Floating background circle
            Box(
                modifier = Modifier
                    .size(80.dp + (floatAnim * 20).dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
            // Icon with subtle bounce
            Icon(
                imageVector = Icons.Filled.MovieFilter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f + (floatAnim * 0.2f)),
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (hasSearchOrFilter) "No results found" else "Your library is empty",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasSearchOrFilter) {
                "Try changing your search or filter."
            } else {
                "Tap the '+' button to add your first entry"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

Change 2 — Add imports
After line 7:

```kotlin
import androidx.compose.animation.core.*
```

---

FILE: `strings.xml` — Add shortcut strings

Find or create: `app/src/main/res/values/strings.xml`

If it exists, add these inside `<resources>`:

```xml
    <string name="shortcut_add_short">Add Media</string>
    <string name="shortcut_add_long">Add a new media entry</string>
    <string name="shortcut_search_short">Search AniList</string>
    <string name="shortcut_search_long">Search AniList database</string>
    <string name="shortcut_stats_short">Statistics</string>
    <string name="shortcut_stats_long">View library statistics</string>
```

If the file doesn't exist, create it with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Shouze</string>
    <string name="shortcut_add_short">Add Media</string>
    <string name="shortcut_add_long">Add a new media entry</string>
    <string name="shortcut_search_short">Search AniList</string>
    <string name="shortcut_search_long">Search AniList database</string>
    <string name="shortcut_stats_short">Statistics</string>
    <string name="shortcut_stats_long">View library statistics</string>
</resources>
```

---

That's all of Tier 4. Five polish features:

1. Haptics — Selection, confirm, delete feedback on supported devices
2. Shared element transitions — Cover image smoothly animates between list and detail
3. Home screen widget — Simple widget showing Shouze branding with refresh
4. App shortcuts — Static shortcuts (Add, Search, Stats) + dynamic "Quick Add"
5. Animated empty states — Pulsing background circle and floating icon instead of static dead screen

Build, test, and let me know if anything throws a red squiggle.