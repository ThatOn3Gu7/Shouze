package com.app.shouze.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.shouze.data.SettingsRepository
import com.app.shouze.data.ThemeMode
import com.app.shouze.data.local.*
import com.app.shouze.ui.StatsUiState
import com.app.shouze.ui.GenreStat
import com.app.shouze.ui.CategoryStat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class HomeUiState(
    val allItems: List<MediaItemEntity> = emptyList(),
    val items: List<MediaItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val syncMessage: String? = null,
    val error: String? = null
)

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.mediaDao()
    private val categoryDao = db.categoryDao()
    private val syncController = DataSyncController(db)
    private val settingsRepo = SettingsRepository(application)
    private val json = Json { ignoreUnknownKeys = true }

    val settings = settingsRepo.settings

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val statsUiState: StateFlow<StatsUiState> = combine(
        dao.getAllItems(),
        categoryDao.getAll()
    ) { items, categories ->
        try {
            computeStats(items, categories)
        } catch (e: Exception) {
            Log.e("Shouze", "Failed to compute statistics", e)
            StatsUiState()
        }
    }.catch { e ->
        Log.e("Shouze", "Statistics data flow failed", e)
        emit(StatsUiState())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())


    init {
        viewModelScope.launch {
            try {
                combine(
                    dao.getAllItems(),
                    categoryDao.getAll(),
                    _selectedCategoryId,
                    _searchQuery
                ) { allItems, allCategories, catId, query ->
                    val filtered = filterItems(allItems, catId, query)
                    _uiState.update { current ->
                        current.copy(
                            allItems = allItems,
                            items = filtered,
                            categories = allCategories,
                            selectedCategoryId = catId,
                            searchQuery = query
                        )
                    }
                }.collect()
            } catch (e: Exception) {
                Log.e("Shouze", "Failed to load library data", e)
                _error.value = "Failed to load data: ${e.message}"
                _uiState.update {
                    it.copy(error = "Failed to load data: ${e.message}", isLoading = false)
                }
            }
        }
    }

    fun addOrUpdate(item: MediaItemEntity) {
        viewModelScope.launch {
            dao.insertOrUpdate(item.copy(lastUpdated = System.currentTimeMillis()))
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
        query: String
    ): List<MediaItemEntity> {
        return all
            .filter { categoryId == null || it.categoryId == categoryId }
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
    }

    fun setThemeMode(mode: ThemeMode) = settingsRepo.setThemeMode(mode)
    fun setDynamicColor(enabled: Boolean) = settingsRepo.setDynamicColor(enabled)
    fun setAmoledBlack(enabled: Boolean) = settingsRepo.setAmoledBlack(enabled)
    fun setHasSeenOnboarding(seen: Boolean) = settingsRepo.setHasSeenOnboarding(seen)

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
       
        val ratedItems = items.filter { it.rating > 0.0 }
        val avgRating = if (ratedItems.isNotEmpty()) {
            ratedItems.map { it.rating }.average()
        } else 0.0
       
        val totalProgress = items.sumOf { it.currentProgress }
       
        // Genre distribution
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
       
        // Category distribution
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
       
        // Top rated (at least 5 items with rating > 0, sorted desc)
        val topRated = ratedItems
            .sortedByDescending { it.rating }
            .take(10)
       
        // Recently updated (last 10)
        val recentlyUpdated = items
            .sortedByDescending { it.lastUpdated }
            .take(10)
       
        return StatsUiState(
            totalEntries = total,
            totalCompleted = completed,
            totalWatching = watching,
            totalReading = reading,
            totalDropped = dropped,
            totalPlanToWatch = planToWatch,
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
