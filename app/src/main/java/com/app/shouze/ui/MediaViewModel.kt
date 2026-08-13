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
    val error: String? = null
)

data class AniListSearchUiState(
    val results: List<AniListMedia> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchType: String = "ANIME"
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
                _filterConfig
            ) { allItems, allCategories, catId, query, filterConfig ->
                val (sort, favOnly) = filterConfig
                val filtered = filterItems(allItems, catId, query, sort, favOnly)
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
                        showFavoritesOnly = favOnly
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

    private fun filterItems(
        all: List<MediaItemEntity>,
        categoryId: String?,
        query: String,
        sort: SortMode,
        favoritesOnly: Boolean
    ): List<MediaItemEntity> {
        val filtered = all
            .filter { categoryId == null || it.categoryId == categoryId }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            .filter { !favoritesOnly || it.isFavorite }

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
        val totalCount = when (_searchUiState.value.searchType) {
            "ANIME" -> media.episodes ?: 0
            else -> media.chapters ?: 0
        }
        val coverImage = media.coverImage?.large ?: media.coverImage?.medium
        val genres = media.genres ?: emptyList()
        val notes = media.description?.let { desc ->
            desc.replace(Regex("<.*?>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        } ?: ""

        val categories = uiState.value.categories
        val categoryId = when (_searchUiState.value.searchType) {
            "ANIME" -> categories.find { it.name.contains("anime", ignoreCase = true) }?.id
            "MANGA" -> categories.find {
                it.name.contains("manga", ignoreCase = true) ||
                it.name.contains("novel", ignoreCase = true) ||
                it.name.contains("book", ignoreCase = true)
            }?.id
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
