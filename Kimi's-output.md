I have everything I need. Here is the complete Tier 3 implementation — all three features with exact line-by-line instructions.

---

NEW FILE 1: `AiringScheduleScreen.kt`

Create at: `app/src/main/java/com/app/shouze/ui/screens/AiringScheduleScreen.kt`

```kotlin
package com.app.shouze.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.remote.AiringSchedule
import com.app.shouze.ui.components.SafeRemoteImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiringScheduleScreen(
    schedules: List<AiringSchedule>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddToLibrary: (AiringSchedule) -> Unit
) {
    val grouped = remember(schedules) {
        schedules.groupBy { schedule ->
            val date = Date(schedule.airingAt * 1000)
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.DAY_OF_WEEK)
        }.toSortedMap()
    }

    val dayNames = mapOf(
        Calendar.SUNDAY to "Sunday",
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Airing Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoading && schedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null && schedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load schedule", style = MaterialTheme.typography.titleMedium)
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) { Text("Retry") }
                    }
                }
            } else if (grouped.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No airing anime found", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    grouped.forEach { (day, daySchedules) ->
                        item {
                            Text(
                                text = dayNames[day] ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(daySchedules, key = { it.id }) { schedule ->
                            AiringScheduleCard(
                                schedule = schedule,
                                onAdd = { onAddToLibrary(schedule) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiringScheduleCard(
    schedule: AiringSchedule,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val time = remember(schedule.airingAt) {
        timeFormat.format(Date(schedule.airingAt * 1000))
    }
    val title = schedule.media.title.english ?: schedule.media.title.romaji ?: "Unknown"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(2f / 3f)
            ) {
                if (!schedule.media.coverImage?.large.isNullOrBlank()) {
                    SafeRemoteImage(
                        url = schedule.media.coverImage!!.large!!,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ep ${schedule.episode} · $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!schedule.media.format.isNullOrBlank()) {
                    Text(
                        text = schedule.media.format.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add to library")
            }
        }
    }
}
```

---

NEW FILE 2: `StreamingLinksScreen.kt`

Create at: `app/src/main/java/com/app/shouze/ui/screens/StreamingLinksScreen.kt`

```kotlin
package com.app.shouze.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.shouze.data.remote.ExternalLink
import com.app.shouze.data.remote.StreamingEpisode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingLinksScreen(
    title: String,
    streamingEpisodes: List<StreamingEpisode>,
    externalLinks: List<ExternalLink>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onLoad: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Where to Watch") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load links", style = MaterialTheme.typography.titleMedium)
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                val allLinks = remember(streamingEpisodes, externalLinks) {
                    val eps = streamingEpisodes.mapNotNull { ep ->
                        if (!ep.url.isNullOrBlank() && !ep.site.isNullOrBlank()) {
                            LinkItem(ep.site, ep.url, "Episode: ${ep.title ?: "N/A"}")
                        } else null
                    }
                    val ext = externalLinks.map { LinkItem(it.site, it.url, null) }
                    eps + ext
                }

                if (allLinks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No streaming links found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(allLinks, key = { it.url }) { link ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    context.startActivity(intent)
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = link.site,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (link.subtitle != null) {
                                            Text(
                                                text = link.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.OpenInBrowser,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LinkItem(
    val site: String,
    val url: String,
    val subtitle: String?
)
```

---

NEW FILE 3: `ShareListScreen.kt`

Create at: `app/src/main/java/com/app/shouze/ui/screens/ShareListScreen.kt`

```kotlin
package com.app.shouze.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.MediaItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareListScreen(
    items: List<MediaItemEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onImportSharedList: (String) -> Unit
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }

    val shareText = remember(items, categories) {
        buildString {
            appendLine("📚 My Shouze Library")
            appendLine("====================")
            appendLine()

            val grouped = items.groupBy { it.categoryId }
            categories.forEach { cat ->
                val catItems = grouped[cat.id] ?: return@forEach
                if (catItems.isEmpty()) return@forEach
                appendLine("${cat.name} (${catItems.size})")
                appendLine("-".repeat(cat.name.length + 10))
                catItems.forEach { item ->
                    val status = item.status.name.replace("_", " ")
                    val progress = if (item.totalCount > 0) "${item.currentProgress}/${item.totalCount}" else "${item.currentProgress}/?"
                    val rating = if (item.rating > 0) " ★${item.rating}" else ""
                    appendLine("• ${item.title} [$status] $progress$rating")
                }
                appendLine()
            }

            appendLine("Total: ${items.size} items")
            appendLine("Shared via Shouze")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Library Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${items.size} total entries",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${items.count { it.status.name == "COMPLETED" }} completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "My Shouze Library")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share List")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Import Shared List",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Paste a shared Shouze list below to import titles as Plan to Watch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Paste shared list here") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (importText.isNotBlank()) {
                                onImportSharedList(importText)
                                importText = ""
                            }
                        },
                        enabled = importText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Titles")
                    }
                }
            }

            if (shareText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Text(
                        text = shareText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

---

FILE 4: `AniListApi.kt`

Change 1 — Add new data classes after line 57
After the closing `}` of `AniListCoverImage` (line 57), add this entire block:

```kotlin

// --- Airing Schedule ---

@Serializable
data class AniListAiringScheduleResponse(
    val data: AiringScheduleData? = null
)

@Serializable
data class AiringScheduleData(
    val Page: AiringSchedulePage? = null
)

@Serializable
data class AiringSchedulePage(
    val airingSchedules: List<AiringSchedule> = emptyList()
)

@Serializable
data class AiringSchedule(
    val id: Int,
    val episode: Int,
    val airingAt: Long,
    val media: AiringScheduleMedia
)

@Serializable
data class AiringScheduleMedia(
    val id: Int,
    val title: AniListTitle,
    val coverImage: AniListCoverImage? = null,
    val format: String? = null
)

// --- Streaming Episodes ---

@Serializable
data class AniListStreamingResponse(
    val data: StreamingData? = null
)

@Serializable
data class StreamingData(
    val Media: StreamingMedia? = null
)

@Serializable
data class StreamingMedia(
    val streamingEpisodes: List<StreamingEpisode>? = null,
    val externalLinks: List<ExternalLink>? = null
)

@Serializable
data class StreamingEpisode(
    val title: String? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    val site: String? = null
)

@Serializable
data class ExternalLink(
    val url: String,
    val site: String
)
```

Change 2 — Add new API methods after line 115
After the closing `}` of `searchMedia()` (line 115), add this entire block:

```kotlin

    suspend fun getAiringSchedule(): Result<List<AiringSchedule>> = withContext(Dispatchers.IO) {
        try {
            val graphqlQuery = """
                query {
                    Page(page: 1, perPage: 50) {
                        airingSchedules(notYetAired: true, sort: TIME) {
                            id
                            episode
                            airingAt
                            media {
                                id
                                title { romaji english }
                                coverImage { large }
                                format
                            }
                        }
                    }
                }
            """.trimIndent()

            val requestBody = buildJsonObject {
                put("query", graphqlQuery)
            }.toString()

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))
                val result = json.decodeFromString<AniListAiringScheduleResponse>(body)
                val schedules = result.data?.Page?.airingSchedules ?: emptyList()
                Result.success(schedules)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStreamingEpisodes(mediaId: Int): Result<Pair<List<StreamingEpisode>, List<ExternalLink>>> = withContext(Dispatchers.IO) {
        try {
            val graphqlQuery = """
                query(${"$"}id: Int) {
                    Media(id: ${"$"}id) {
                        streamingEpisodes {
                            title
                            thumbnail
                            url
                            site
                        }
                        externalLinks {
                            url
                            site
                        }
                    }
                }
            """.trimIndent()

            val requestBody = buildJsonObject {
                put("query", graphqlQuery)
                putJsonObject("variables") {
                    put("id", mediaId)
                }
            }.toString()

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))
                val result = json.decodeFromString<AniListStreamingResponse>(body)
                val media = result.data?.Media
                val episodes = media?.streamingEpisodes ?: emptyList()
                val links = media?.externalLinks ?: emptyList()
                Result.success(episodes to links)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
```

---

FILE 5: `MediaViewModel.kt`

Change 1 — Add new UI state data classes after line 47
After the closing `}` of `AniListSearchUiState` (line 47), add:

```kotlin

data class AiringScheduleUiState(
    val schedules: List<AiringSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class StreamingUiState(
    val title: String = "",
    val streamingEpisodes: List<StreamingEpisode> = emptyList(),
    val externalLinks: List<ExternalLink> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

Change 2 — Add new StateFlows after line 79
After line 79 (`val searchUiState: StateFlow<AniListSearchUiState> = _searchUiState.asStateFlow()`), add:

```kotlin

    private val _airingScheduleUiState = MutableStateFlow(AiringScheduleUiState())
    val airingScheduleUiState: StateFlow<AiringScheduleUiState> = _airingScheduleUiState.asStateFlow()

    private val _streamingUiState = MutableStateFlow(StreamingUiState())
    val streamingUiState: StateFlow<StreamingUiState> = _streamingUiState.asStateFlow()
```

Change 3 — Add new methods after line 326
After the closing `}` of `clearSearchResults()` (line 326), add this entire block:

```kotlin

    // --- Airing Schedule ---

    fun fetchAiringSchedule() {
        viewModelScope.launch {
            _airingScheduleUiState.update { it.copy(isLoading = true, error = null) }
            val result = aniListApi.getAiringSchedule()
            result.fold(
                onSuccess = { schedules ->
                    _airingScheduleUiState.update { it.copy(schedules = schedules, isLoading = false) }
                },
                onFailure = { e ->
                    _airingScheduleUiState.update { it.copy(error = e.message ?: "Failed to load", isLoading = false) }
                }
            )
        }
    }

    fun createItemFromAiringSchedule(schedule: AiringSchedule): MediaItemEntity {
        val title = schedule.media.title.english ?: schedule.media.title.romaji ?: "Unknown"
        val categories = uiState.value.categories
        val categoryId = categories.find { it.name.equals("Anime", ignoreCase = true) }?.id
            ?: categories.find { it.name.equals("TV Series", ignoreCase = true) }?.id
            ?: categories.firstOrNull()?.id ?: ""

        return MediaItemEntity(
            title = title,
            categoryId = categoryId,
            status = Status.PLAN_TO_WATCH,
            currentProgress = 0,
            totalCount = 0,
            coverImageUri = schedule.media.coverImage?.large ?: schedule.media.coverImage?.medium
        )
    }

    // --- Where to Watch / Streaming ---

    fun loadStreamingForTitle(title: String) {
        viewModelScope.launch {
            _streamingUiState.update { it.copy(isLoading = true, error = null, title = title) }
            val searchResult = aniListApi.searchMedia(title)
            searchResult.fold(
                onSuccess = { mediaList ->
                    val match = mediaList.firstOrNull()
                    if (match != null) {
                        val streamResult = aniListApi.getStreamingEpisodes(match.id)
                        streamResult.fold(
                            onSuccess = { (episodes, links) ->
                                _streamingUiState.update {
                                    it.copy(streamingEpisodes = episodes, externalLinks = links, isLoading = false)
                                }
                            },
                            onFailure = { e ->
                                _streamingUiState.update { it.copy(error = e.message, isLoading = false) }
                            }
                        )
                    } else {
                        _streamingUiState.update { it.copy(error = "Not found on AniList", isLoading = false) }
                    }
                },
                onFailure = { e ->
                    _streamingUiState.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun clearStreamingState() {
        _streamingUiState.value = StreamingUiState()
    }

    // --- Social / Shared Lists ---

    fun importSharedList(text: String) {
        viewModelScope.launch {
            val lines = text.lines()
            val titles = lines.mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("•")) {
                    trimmed.removePrefix("•").trim().substringBefore("[").trim()
                } else null
            }

            if (titles.isEmpty()) {
                showMessage("No titles found in shared list", isError = true)
                return@launch
            }

            val categories = uiState.value.categories
            val defaultCategory = categories.find { it.name.equals("Anime", ignoreCase = true) }?.id
                ?: categories.firstOrNull()?.id ?: ""

            var imported = 0
            titles.forEach { title ->
                val exists = uiState.value.allItems.any { it.title.equals(title, ignoreCase = true) }
                if (!exists) {
                    dao.insertOrUpdate(
                        MediaItemEntity(
                            title = title,
                            categoryId = defaultCategory,
                            status = Status.PLAN_TO_WATCH,
                            currentProgress = 0,
                            totalCount = 0
                        )
                    )
                    imported++
                }
            }
            showMessage("Imported $imported new titles from shared list")
        }
    }
```

---

FILE 6: `DetailScreen.kt`

Change 1 — Add import
After line 15:

```kotlin
import androidx.compose.material.icons.filled.PlayArrow
```

Change 2 — Add parameter to function signature
Change lines 38-46 from:

```kotlin
fun DetailScreen(
    item: MediaItemEntity,
    category: CategoryEntity?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onIncrementRewatch: () -> Unit = {}
) {
```

To:

```kotlin
fun DetailScreen(
    item: MediaItemEntity,
    category: CategoryEntity?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onIncrementRewatch: () -> Unit = {},
    onWhereToWatch: () -> Unit = {}
) {
```

Change 3 — Add button in TopAppBar actions
After line 72 (the `IconButton` for Delete), add:

```kotlin
                    IconButton(onClick = onWhereToWatch) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Where to Watch",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
```

---

FILE 7: `HomeScreen.kt`

Change 1 — Add import
After line 27:

```kotlin
import androidx.compose.material.icons.filled.CalendarToday
```

Change 2 — Add parameter to function signature
After line 68 (`onTagSelected: (String?) -> Unit = {}`), add:

```kotlin
    onAiringScheduleClick: () -> Unit = {},
```

Change 3 — Add button in TopAppBar actions
After line 229 (`IconButton(onClick = onSearchAniListClick)` block), add:

```kotlin
                        IconButton(onClick = onAiringScheduleClick) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Airing Schedule")
                        }
```

---

FILE 8: `SettingsScreen.kt`

Change 1 — Add import
After line 15:

```kotlin
import androidx.compose.material.icons.filled.Share
```

Change 2 — Add parameter to function signature
After line 29 (`onNavigateToStatistics: () -> Unit`), add:

```kotlin
    onNavigateToShareList: () -> Unit = {},
```

Change 3 — Add SettingsItem in General section
After line 74 (the `SettingsItem` for "Backup & Restore"), add:

```kotlin
                SettingsItem(
                    title = "Share List",
                    subtitle = "Share or import your library",
                    icon = Icons.Default.Share,
                    onClick = onNavigateToShareList
                )
```

---

FILE 9: `MainActivity.kt`

Change 1 — Add new routes inside NavHost
Find the `composable("about")` block. After its closing `}`, add these new routes:

```kotlin
                        composable("airing") {
                            val airingState by viewModel.airingScheduleUiState.collectAsState()
                            LaunchedEffect(Unit) {
                                if (airingState.schedules.isEmpty()) {
                                    viewModel.fetchAiringSchedule()
                                }
                            }
                            AiringScheduleScreen(
                                schedules = airingState.schedules,
                                isLoading = airingState.isLoading,
                                error = airingState.error,
                                onBack = { navController.popBackStack() },
                                onRefresh = { viewModel.fetchAiringSchedule() },
                                onAddToLibrary = { schedule ->
                                    val item = viewModel.createItemFromAiringSchedule(schedule)
                                    viewModel.addOrUpdate(item)
                                }
                            )
                        }

                        composable("streaming/{title}") { backStackEntry ->
                            val title = backStackEntry.arguments?.getString("title")?.let {
                                java.net.URLDecoder.decode(it, "UTF-8")
                            } ?: ""
                            val streamingState by viewModel.streamingUiState.collectAsState()

                            StreamingLinksScreen(
                                title = streamingState.title,
                                streamingEpisodes = streamingState.streamingEpisodes,
                                externalLinks = streamingState.externalLinks,
                                isLoading = streamingState.isLoading,
                                error = streamingState.error,
                                onBack = { navController.popBackStack() },
                                onLoad = { viewModel.loadStreamingForTitle(title) }
                            )
                        }

                        composable("share") {
                            ShareListScreen(
                                items = uiState.allItems,
                                categories = uiState.categories,
                                onBack = { navController.popBackStack() },
                                onImportSharedList = { text ->
                                    viewModel.importSharedList(text)
                                }
                            )
                        }
```

Change 2 — Wire up HomeScreen callback
In the `HomeScreen` call inside `composable("home")`, add:

```kotlin
                                onAiringScheduleClick = { navController.navigate("airing") },
```

Change 3 — Wire up DetailScreen callback
In the `DetailScreen` call inside `composable("detail/{itemId}")`, add:

```kotlin
                                    onWhereToWatch = {
                                        val encodedTitle = java.net.URLEncoder.encode(item.title, "UTF-8")
                                        navController.navigate("streaming/$encodedTitle")
                                    },
```

Change 4 — Wire up SettingsScreen callback
In the `SettingsScreen` call inside `composable("settings")`, add:

```kotlin
                            onNavigateToShareList = { navController.navigate("share") },
```

---

That's all of Tier 3. Build, test, and let me know if anything throws a red squiggle.