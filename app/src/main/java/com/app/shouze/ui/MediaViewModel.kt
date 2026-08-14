package com.app.shouze.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.shouze.data.SettingsRepository
import com.app.shouze.data.ThemeMode
import com.app.shouze.data.local.*
import com.app.shouze.data.remote.AniListApi
import com.app.shouze.data.remote.AniListMedia
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import androidx.room.withTransaction

enum class SortMode {
    LAST_UPDATED, TITLE, RATING_HIGH, PROGRESS
}

data class HomeUiState(
    val allItems: List<MediaItemEntity> = emptyList(),
    val items: List<MediaItemEntity> = emptyList(),
    val upNextItems: List<MediaItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val sortMode: SortMode = SortMode.LAST_UPDATED,
    val showFavoritesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val syncMessage: String? = null,
    val error: String? = null,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val allTags: List<String> = emptyList(),
    val selectedTag: String? = null
)

data class AniListSearchUiState(
    val results: List<AniListMedia> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchType: String = "ANIME"
)

data class AiringScheduleUiState(
    val schedules: List<com.app.shouze.data.remote.AiringSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class StreamingUiState(
    val title: String = "",
    val streamingEpisodes: List<com.app.shouze.data.remote.StreamingEpisode> = emptyList(),
    val externalLinks: List<com.app.shouze.data.remote.ExternalLink> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.mediaDao()
    private val categoryDao = db.categoryDao()
    private val syncController = DataSyncController(db)
    private val settingsRepo = SettingsRepository(application)
    private val aniListApi = AniListApi()
    private val json = Json { ignoreUnknownKeys = true }

    val settings = settingsRepo.settings

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _sortMode = MutableStateFlow(SortMode.LAST_UPDATED)
    private val _showFavoritesOnly = MutableStateFlow(false)
    private val _selectedTag = MutableStateFlow<String?>(null)

    private val _filterConfig = combine(_sortMode, _showFavoritesOnly) { sort, favOnly ->
        sort to favOnly
    }

    private val _isLoading = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchUiState = MutableStateFlow(AniListSearchUiState())
    val searchUiState: StateFlow<AniListSearchUiState> = _searchUiState.asStateFlow()

    private val _airingScheduleUiState = MutableStateFlow(AiringScheduleUiState())
    val airingScheduleUiState: StateFlow<AiringScheduleUiState> = _airingScheduleUiState.asStateFlow()

    private val _streamingUiState = MutableStateFlow(StreamingUiState())
    val streamingUiState: StateFlow<StreamingUiState> = _streamingUiState.asStateFlow()

    private var pendingPreFill: MediaItemEntity? = null

    val statsUiState: StateFlow<StatsUiState> = combine(
        dao.getAllItems(),
        categoryDao.getAll()
    ) { items, categories ->
        computeStats(items, categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    init {
        viewModelScope.launch {
            startLibraryCollection()
        }
    }

    private suspend fun startLibraryCollection() {
        try {
            combine(
                dao.getAllItems(),
                categoryDao.getAll(),
                _selectedCategoryId,
                _searchQuery,
                _filterConfig,
                _selectedTag
            ) { args: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val allItems = args[0] as List<MediaItemEntity>
                @Suppress("UNCHECKED_CAST")
                val allCategories = args[1] as List<CategoryEntity>
                val catId = args[2] as String?
                val query = args[3] as String
                @Suppress("UNCHECKED_CAST")
                val filterConfig = args[4] as Pair<SortMode, Boolean>
                val tag = args[5] as String?

                val (sort, favOnly) = filterConfig
                val allTags = allItems.flatMap { it.tags }.distinct().sorted()
                val filtered = filterItems(allItems, catId, query, sort, favOnly, tag)
                val upNext = allItems
                    .filter { it.status == Status.WATCHING || it.status == Status.READING }
                    .sortedByDescending { it.lastUpdated }
                    .take(10)
                 _uiState.update { current ->
                    current.copy(
                        allItems = allItems,
                        items = filtered,
                        upNextItems = upNext,
                        categories = allCategories,
                        selectedCategoryId = catId,
                        searchQuery = query,
                        sortMode = sort,
                        showFavoritesOnly = favOnly,
                        allTags = allTags,
                        selectedTag = tag
                    )
                }
            }.collect()
        } catch (e: Exception) {
            showMessage("Failed to load library: ${e.message}", isError = true)
        }
    }

    fun addOrUpdate(item: MediaItemEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var updated = item.copy(lastUpdated = now)

            if ((item.status == Status.WATCHING || item.status == Status.READING) && item.startDate == null) {
                updated = updated.copy(startDate = now)
            }

            if (item.status == Status.COMPLETED && item.endDate == null) {
                updated = updated.copy(endDate = now)
            }

            dao.insertOrUpdate(updated)
        }
    }

    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            val item = uiState.value.allItems.find { it.id == itemId } ?: return@launch
            dao.insertOrUpdate(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun incrementRewatch(itemId: String) {
        viewModelScope.launch {
            val item = uiState.value.allItems.find { it.id == itemId } ?: return@launch
            dao.insertOrUpdate(item.copy(rewatchCount = item.rewatchCount + 1, lastUpdated = System.currentTimeMillis()))
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            dao.deleteById(itemId)
        }
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun toggleShowFavorites() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }
   
    fun setTagFilter(tag: String?) {
        _selectedTag.value = tag
    }

    
    // --- Selection / Multi-select ---

    fun toggleSelection(itemId: String) {
        val current = _uiState.value.selectedIds
        val updated = if (current.contains(itemId)) current - itemId else current + itemId
        _uiState.update { it.copy(selectedIds = updated, isSelectionMode = updated.isNotEmpty()) }
    }

    fun selectAllVisible() {
        val visibleIds = _uiState.value.items.map { it.id }.toSet()
        _uiState.update { it.copy(selectedIds = visibleIds, isSelectionMode = true) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryDao.insert(CategoryEntity(name = name.trim()))
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryDao.delete(categoryId)
        }
    }

    // --- Bulk Edit ---

    fun bulkDelete() {
        viewModelScope.launch {
            _uiState.value.selectedIds.forEach { dao.deleteById(it) }
            clearSelection()
        }
    }

    fun bulkUpdateCategory(categoryId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _uiState.value.allItems
                .filter { it.id in _uiState.value.selectedIds }
                .forEach { dao.insertOrUpdate(it.copy(categoryId = categoryId, lastUpdated = now)) }
            clearSelection()
        }
    }

    fun bulkUpdateStatus(status: Status) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _uiState.value.allItems
                .filter { it.id in _uiState.value.selectedIds }
                .forEach { item ->
                    var updated = item.copy(status = status, lastUpdated = now)
                    if ((status == Status.WATCHING || status == Status.READING) && item.startDate == null) {
                        updated = updated.copy(startDate = now)
                    }
                    if (status == Status.COMPLETED && item.endDate == null) {
                        updated = updated.copy(endDate = now)
                    }
                    dao.insertOrUpdate(updated)
                }
            clearSelection()
        }
    }

    fun bulkToggleFavorite() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _uiState.value.allItems
                .filter { it.id in _uiState.value.selectedIds }
                .forEach { dao.insertOrUpdate(it.copy(isFavorite = !it.isFavorite, lastUpdated = now)) }
            clearSelection()
        }
    }

    private fun filterItems(
        all: List<MediaItemEntity>,
        categoryId: String?,
        query: String,
        sort: SortMode,
        favoritesOnly: Boolean,
        tag: String?
    ): List<MediaItemEntity> {
        val filtered = all
            .filter { categoryId == null || it.categoryId == categoryId }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            .filter { !favoritesOnly || it.isFavorite }
            .filter { tag == null || tag in it.tags }

        return when (sort) {
            SortMode.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortMode.RATING_HIGH -> filtered.sortedByDescending { it.rating }
            SortMode.PROGRESS -> filtered.sortedByDescending {
                if (it.totalCount > 0) it.currentProgress.toFloat() / it.totalCount else 0f
            }
            SortMode.LAST_UPDATED -> filtered.sortedByDescending { it.lastUpdated }
        }
    }

    // --- AniList Search ---

    fun searchAniList(query: String) {
        viewModelScope.launch {
            _searchUiState.update { it.copy(isLoading = true, error = null) }
            val type = _searchUiState.value.searchType
            val result = aniListApi.searchMedia(query, type)
            result.fold(
                onSuccess = { media ->
                    _searchUiState.update { it.copy(results = media, isLoading = false) }
                },
                onFailure = { e ->
                    _searchUiState.update { it.copy(error = e.message ?: "Search failed", isLoading = false) }
                }
            )
        }
    }

    fun setSearchType(type: String) {
        _searchUiState.update { it.copy(searchType = type, results = emptyList()) }
    }

    fun clearSearchResults() {
        _searchUiState.update { AniListSearchUiState() }
    }

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

    fun createItemFromAiringSchedule(schedule: com.app.shouze.data.remote.AiringSchedule): MediaItemEntity {
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
            showMessage("Imported ${'$'}imported new titles from shared list")
        }
    }

    fun setPendingPreFill(item: MediaItemEntity?) {
        pendingPreFill = item
    }

    fun consumePendingPreFill(): MediaItemEntity? {
        val item = pendingPreFill
        pendingPreFill = null
        return item
    }

    fun createItemFromAniList(media: AniListMedia): MediaItemEntity {
        val title = media.title.english ?: media.title.romaji ?: "Unknown"
        
        // For anime: use episodes. For manga: use chapters, fall back to volumes.
        val totalCount = when (_searchUiState.value.searchType) {
            "ANIME" -> media.episodes
            else -> media.chapters ?: media.volumes
        } ?: 0

        val coverImage = media.coverImage?.large ?: media.coverImage?.medium
        val genres = media.genres ?: emptyList()
        val notes = media.description?.let { desc ->
            desc.replace(Regex("<.*?>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        } ?: ""

        val categories = uiState.value.categories
        
        // Smart category matching using exact names first, then partial
        val categoryId = when (_searchUiState.value.searchType) {
            "ANIME" -> {
                categories.find { it.name.equals("Anime", ignoreCase = true) }?.id
                    ?: categories.find { it.name.equals("TV Series", ignoreCase = true) }?.id
                    ?: categories.find { it.name.equals("OVA", ignoreCase = true) }?.id
                    ?: categories.find { it.name.equals("Movie", ignoreCase = true) }?.id
                    ?: categories.find { it.name.contains("anime", ignoreCase = true) }?.id
                    ?: categories.find { it.name.contains("tv", ignoreCase = true) }?.id
            }
            "MANGA" -> {
                categories.find { it.name.equals("Manga", ignoreCase = true) }?.id
                    ?: categories.find { it.name.equals("Light Novel", ignoreCase = true) }?.id
                    ?: categories.find { it.name.equals("Novel", ignoreCase = true) }?.id
                    ?: categories.find { it.name.equals("Webtoon", ignoreCase = true) }?.id
                    ?: categories.find { 
                        it.name.contains("manga", ignoreCase = true) 
                        || it.name.contains("novel", ignoreCase = true) 
                        || it.name.contains("book", ignoreCase = true)
                        || it.name.contains("webtoon", ignoreCase = true)
                    }?.id
            }
            else -> null
        } ?: categories.firstOrNull()?.id ?: ""

        return MediaItemEntity(
            title = title,
            categoryId = categoryId,
            status = Status.PLAN_TO_WATCH,
            currentProgress = 0,
            totalCount = totalCount,
            rating = 0.0,
            coverImageUri = coverImage,
            genres = genres,
            notes = notes
        )
    }

    // --- Settings ---

    fun setThemeMode(mode: ThemeMode) = settingsRepo.setThemeMode(mode)
    fun setDynamicColor(enabled: Boolean) = settingsRepo.setDynamicColor(enabled)
    fun setAmoledBlack(enabled: Boolean) = settingsRepo.setAmoledBlack(enabled)
    fun setHasSeenOnboarding(seen: Boolean) = settingsRepo.setHasSeenOnboarding(seen)

    // --- Backup / Restore ---

    fun backupToLocalZip(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _syncMessage.value = null
            _error.value = null
            _uiState.update { it.copy(isLoading = true, syncMessage = null, error = null) }
            try {
                val result = syncController.exportToJson()
                result.fold(
                    onSuccess = { jsonString ->
                        val itemCount = try {
                            json.decodeFromString<BackupPayload>(jsonString).itemCount
                        } catch (_: Exception) { 0 }
                        val output = getApplication<Application>().contentResolver.openOutputStream(uri)
                        if (output == null) {
                            showMessage("Failed to open file for writing", isError = true)
                            return@launch
                        }
                        output.use { os ->
                            ZipOutputStream(os).use { zos ->
                                val entry = ZipEntry("backup.json")
                                zos.putNextEntry(entry)
                                zos.write(jsonString.toByteArray(Charsets.UTF_8))
                                zos.closeEntry()
                            }
                        }
                        showMessage("Backup saved successfully ($itemCount items)")
                    },
                    onFailure = { e ->
                        showMessage("Export failed: ${e.message}", isError = true)
                    }
                )
            } catch (e: Exception) {
                showMessage("Unexpected error: ${e.message}", isError = true)
            }
        }
    }

    fun restoreFromLocalZip(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _syncMessage.value = null
            _error.value = null
            _uiState.update { it.copy(isLoading = true, syncMessage = null, error = null) }
            try {
                val input = getApplication<Application>().contentResolver.openInputStream(uri)
                if (input == null) {
                    showMessage("Failed to open backup file", isError = true)
                    return@launch
                }
                val backupJson = input.use { stream ->
                    var content = ""
                    ZipInputStream(stream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "backup.json") {
                                content = zis.bufferedReader().readText()
                                break
                            }
                            entry = zis.nextEntry
                        }
                    }
                    content
                }
                if (backupJson.isBlank()) {
                    showMessage("Invalid backup file: backup.json not found", isError = true)
                    return@launch
                }
                val importResult = syncController.importFromJson(backupJson)
                importResult.fold(
                    onSuccess = { count ->
                        showMessage("Restore successful (imported $count items)")
                    },
                    onFailure = { e ->
                        showMessage("Import failed: ${e.message}", isError = true)
                    }
                )
            } catch (e: Exception) {
                showMessage("Unexpected error: ${e.message}", isError = true)
            }
        }
    }

     // --- CSV Export ---

    fun exportToCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val items = dao.getAllItemsSnapshot()
                val csv = StringBuilder()
                csv.appendLine("id,title,categoryId,status,currentProgress,totalCount,currentVolume,rating,coverImageUri,genres,tags,notes,rewatchCount,startDate,endDate,lastUpdated")
                items.forEach { item ->
                    val row = listOf(
                        item.id,
                        "\"${item.title.replace("\"", "\"\"")}\"",
                        item.categoryId,
                        item.status.name,
                        item.currentProgress,
                        item.totalCount,
                        item.currentVolume ?: "",
                        item.rating,
                        item.coverImageUri ?: "",
                        "\"${item.genres.joinToString(", ")}\"",
                        "\"${item.tags.joinToString(", ")}\"",
                        "\"${item.notes.replace("\"", "\"\"").replace("\n", " ")}\"",
                        item.rewatchCount,
                        item.startDate ?: "",
                        item.endDate ?: "",
                        item.lastUpdated
                    ).joinToString(",")
                    csv.appendLine(row)
                }
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(csv.toString().toByteArray(Charsets.UTF_8))
                }
                showMessage("CSV exported successfully (${items.size} items)")
            } catch (e: Exception) {
                showMessage("CSV export failed: ${e.message}", isError = true)
            }
        }
    }

    // --- MAL XML Import ---

    fun importFromMalXml(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val xml = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: return@launch showMessage("Failed to read XML file", isError = true)

                val items = parseMalXml(xml)
                if (items.isEmpty()) {
                    showMessage("No valid entries found in XML", isError = true)
                    return@launch
                }
                db.withTransaction {
                    items.forEach { dao.insertOrUpdate(it) }
                }
                showMessage("Imported ${items.size} items from MAL XML")
            } catch (e: Exception) {
                showMessage("MAL import failed: ${e.message}", isError = true)
            }
        }
    }

    private fun parseMalXml(xml: String): List<MediaItemEntity> {
        val items = mutableListOf<MediaItemEntity>()
        val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        var eventType = parser.eventType
        var inEntry = false
        val currentData = mutableMapOf<String, String>()
        var currentTag = ""

        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "anime" || currentTag == "manga") {
                        inEntry = true
                        currentData.clear()
                    }
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> {
                    if (inEntry) {
                        val text = parser.text.trim()
                        if (text.isNotBlank()) {
                            currentData[currentTag] = text
                        }
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    if ((parser.name == "anime" || parser.name == "manga") && inEntry) {
                        val title = currentData["series_title"] ?: ""
                        if (title.isNotBlank()) {
                            val isManga = parser.name == "manga"
                            val malStatus = currentData["my_status"] ?: "Plan to Watch"
                            val status = when (malStatus.lowercase()) {
                                "watching", "reading" -> Status.WATCHING
                                "completed" -> Status.COMPLETED
                                "on-hold" -> Status.PLAN_TO_WATCH
                                "dropped" -> Status.DROPPED
                                "plan to watch", "plan to read" -> Status.PLAN_TO_WATCH
                                else -> Status.PLAN_TO_WATCH
                            }

                            val progress = if (isManga) {
                                currentData["my_read_chapters"]?.toIntOrNull() ?: 0
                            } else {
                                currentData["my_watched_episodes"]?.toIntOrNull() ?: 0
                            }
                            val total = if (isManga) {
                                currentData["series_chapters"]?.toIntOrNull() ?: 0
                            } else {
                                currentData["series_episodes"]?.toIntOrNull() ?: 0
                            }
                            val volume = if (isManga) {
                                currentData["my_read_volumes"]?.toIntOrNull()
                            } else null

                            val score = currentData["my_score"]?.toDoubleOrNull() ?: 0.0
                            val rewatches = if (isManga) {
                                currentData["my_times_read"]?.toIntOrNull() ?: 0
                            } else {
                                currentData["my_times_watched"]?.toIntOrNull() ?: 0
                            }

                            val categoryId = if (isManga) {
                                uiState.value.categories.find { it.name.equals("Manga", ignoreCase = true) }?.id
                                    ?: uiState.value.categories.find { it.name.equals("Novel", ignoreCase = true) }?.id
                                    ?: uiState.value.categories.firstOrNull()?.id ?: ""
                            } else {
                                uiState.value.categories.find { it.name.equals("Anime", ignoreCase = true) }?.id
                                    ?: uiState.value.categories.find { it.name.equals("TV Series", ignoreCase = true) }?.id
                                    ?: uiState.value.categories.firstOrNull()?.id ?: ""
                            }

                            val tags = currentData["my_tags"]?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                            val notes = currentData["my_comments"] ?: ""

                            items.add(
                                MediaItemEntity(
                                    title = title,
                                    categoryId = categoryId,
                                    status = status,
                                    currentProgress = progress,
                                    totalCount = total,
                                    currentVolume = volume,
                                    rating = score,
                                    genres = emptyList(),
                                    tags = tags,
                                    notes = notes,
                                    rewatchCount = rewatches
                                )
                            )
                        }
                        inEntry = false
                    }
                }
            }
            eventType = parser.next()
        }
        return items
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
        _error.value = null
        _uiState.update { it.copy(syncMessage = null, error = null) }
    }

    private fun showMessage(message: String, isError: Boolean = false) {
        _isLoading.value = false
        if (isError) {
            _error.value = message
            _syncMessage.value = null
        } else {
            _syncMessage.value = message
            _error.value = null
        }
        _uiState.update { it.copy(isLoading = false, syncMessage = if (isError) null else message, error = if (isError) message else null) }
    }

    private fun computeStats(
        items: List<MediaItemEntity>,
        categories: List<CategoryEntity>
    ): StatsUiState {
        if (items.isEmpty()) return StatsUiState()

        val total = items.size
        val completed = items.count { it.status == Status.COMPLETED }
        val watching = items.count { it.status == Status.WATCHING }
        val reading = items.count { it.status == Status.READING }
        val dropped = items.count { it.status == Status.DROPPED }
        val planToWatch = items.count { it.status == Status.PLAN_TO_WATCH }
        val favorites = items.count { it.isFavorite }

        val ratedItems = items.filter { it.rating > 0.0 }
        val avgRating = if (ratedItems.isNotEmpty()) ratedItems.map { it.rating }.average() else 0.0
        val totalProgress = items.sumOf { it.currentProgress }

        val genreCounts = mutableMapOf<String, Int>()
        items.forEach { item ->
            item.genres.forEach { genre ->
                genreCounts[genre] = genreCounts.getOrDefault(genre, 0) + 1
            }
        }
        val genreDistribution = genreCounts
            .map { (genre, count) -> GenreStat(genre, count, count.toFloat() / total) }
            .sortedByDescending { it.count }
            .take(8)

        val categoryCounts = items.groupingBy { it.categoryId }.eachCount()
        val categoryDistribution = categoryCounts.map { (catId, count) ->
            val cat = categories.find { it.id == catId }
            CategoryStat(
                categoryName = cat?.name ?: "Unknown",
                count = count,
                colorHex = cat?.colorHex,
                percentage = count.toFloat() / total
            )
        }.sortedByDescending { it.count }

        val topRated = ratedItems.sortedByDescending { it.rating }.take(10)
        val recentlyUpdated = items.sortedByDescending { it.lastUpdated }.take(10)

        return StatsUiState(
            totalEntries = total,
            totalCompleted = completed,
            totalWatching = watching,
            totalReading = reading,
            totalDropped = dropped,
            totalPlanToWatch = planToWatch,
            totalFavorites = favorites,
            completionRate = completed.toFloat() / total,
            averageRating = avgRating,
            totalProgressConsumed = totalProgress,
            genreDistribution = genreDistribution,
            categoryDistribution = categoryDistribution,
            topRatedItems = topRated,
            recentlyUpdatedItems = recentlyUpdated
        )
    }
}
