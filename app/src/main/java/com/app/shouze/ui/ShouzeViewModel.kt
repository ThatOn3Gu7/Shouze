package com.app.shouze.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.shouze.data.AccountUiState
import com.app.shouze.data.AniListAuth
import com.app.shouze.data.AniListCredentials
import com.app.shouze.data.MediaRepository
import com.app.shouze.data.SettingsRepository
import com.app.shouze.data.ThemeMode
import com.app.shouze.data.friendlyError
import com.app.shouze.data.local.AppDatabase
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.LibraryEntryEntity
import com.app.shouze.data.local.MediaStatus
import com.app.shouze.data.local.MediaType
import com.app.shouze.data.remote.AiringSchedule
import com.app.shouze.data.remote.AniListApi
import com.app.shouze.data.remote.MediaDetail
import com.app.shouze.data.remote.MediaSummary
import com.app.shouze.data.remote.ViewerDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibrarySort { LAST_UPDATED, TITLE, SCORE, PROGRESS }

data class LibraryUiState(
    val entries: List<LibraryEntryEntity> = emptyList(),
    val filtered: List<LibraryEntryEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val statusFilter: MediaStatus? = null,
    val typeFilter: MediaType? = null,
    val categoryFilter: String? = null,
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val sort: LibrarySort = LibrarySort.LAST_UPDATED,
    val isSyncing: Boolean = false,
    val message: String? = null
)

data class DiscoverUiState(
    val trending: List<MediaSummary> = emptyList(),
    val seasonal: List<MediaSummary> = emptyList(),
    val airing: List<AiringSchedule> = emptyList(),
    val seasonLabel: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SearchUiState(
    val type: String = "ANIME",
    val results: List<MediaSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class DetailUiState(
    val media: MediaDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

private data class FilterConfig(
    val status: MediaStatus?,
    val type: MediaType?,
    val category: String?,
    val query: String,
    val favoritesOnly: Boolean,
    val sort: LibrarySort
)

class ShouzeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val settings = SettingsRepository(application)
    private val auth = AniListAuth(application)
    private val api = AniListApi()
    private val repo = MediaRepository(db, settings, auth, api)

    /** Exposed for the About screen (update-check frequency, etc.). */
    val settingsRepository: SettingsRepository = settings

    val settingsFlow: StateFlow<com.app.shouze.data.AppSettings> = repo.settingsFlow
    val account: StateFlow<AccountUiState> = repo.account
    val syncMessage: StateFlow<String?> = repo.syncMessage
    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _credentials = MutableStateFlow(auth.credentials())
    val credentials: StateFlow<AniListCredentials?> = _credentials.asStateFlow()

    /** Persists the client credentials and returns the AniList authorize URL to open. */
    fun connectAniList(clientId: String, clientSecret: String, redirectUri: String): String? {
        auth.saveCredentials(clientId, clientSecret, redirectUri)
        _credentials.value = auth.credentials()
        return auth.authorizeUrl()
    }

    // --- Library ---

    private val _statusFilter = MutableStateFlow<MediaStatus?>(null)
    private val _typeFilter = MutableStateFlow<MediaType?>(null)
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _query = MutableStateFlow("")
    private val _favoritesOnly = MutableStateFlow(false)
    private val _sort = MutableStateFlow(LibrarySort.LAST_UPDATED)
    private val _isSyncing = MutableStateFlow(false)

    private val _filterConfig = combine(
        combine(_statusFilter, _typeFilter, _query, _favoritesOnly, _sort) { s, t, q, f, so ->
            FilterConfig(s, t, null, q, f, so)
        },
        _categoryFilter
    ) { config, category -> config.copy(category = category) }

    val libraryUiState: StateFlow<LibraryUiState> = combine(
        repo.library, repo.categories, _filterConfig, _isSyncing, repo.syncMessage
    ) { entries, cats, config, isSyncing, message ->
        val filtered = filterEntries(entries, config)
        LibraryUiState(
            entries = entries,
            filtered = filtered,
            categories = cats,
            statusFilter = config.status,
            typeFilter = config.type,
            categoryFilter = config.category,
            query = config.query,
            favoritesOnly = config.favoritesOnly,
            sort = config.sort,
            isSyncing = isSyncing,
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun setStatusFilter(status: MediaStatus?) = _statusFilter.update { status }
    fun setTypeFilter(type: MediaType?) = _typeFilter.update { type }
    fun setCategoryFilter(categoryId: String?) = _categoryFilter.update { categoryId }
    fun setQuery(query: String) = _query.update { query }
    fun toggleFavoritesOnly() = _favoritesOnly.update { !it }
    fun setSort(sort: LibrarySort) = _sort.update { sort }

    fun clearFilters() {
        _statusFilter.value = null
        _typeFilter.value = null
        _categoryFilter.value = null
        _query.value = ""
        _favoritesOnly.value = false
    }

    // --- Discover ---

    private val _discover = MutableStateFlow(DiscoverUiState())
    val discoverUiState: StateFlow<DiscoverUiState> = _discover.asStateFlow()

    fun refreshDiscover() {
        viewModelScope.launch {
            _discover.update { it.copy(isLoading = true, error = null) }
            val (season, year) = repo.currentSeason()
            val label = season.replaceFirstChar { c -> c.uppercase() } + " $year"

            val trendingResult = repo.trending("ANIME", "TRENDING_DESC")
            val seasonalResult = repo.seasonal("ANIME", season, year)
            val airingResult = repo.airing()

            val error = listOf(trendingResult, seasonalResult).firstOrNull { it.isFailure }?.exceptionOrNull()
            _discover.update {
                it.copy(
                    trending = trendingResult.getOrDefault(emptyList()),
                    seasonal = seasonalResult.getOrDefault(emptyList()),
                    airing = airingResult.getOrDefault(emptyList()),
                    seasonLabel = label,
                    isLoading = false,
                    error = error?.let { e -> friendlyError(e) }
                )
            }
        }
    }

    // --- Search ---

    private val _search = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _search.asStateFlow()

    fun setSearchType(type: String) {
        _search.update { it.copy(type = type, results = emptyList(), error = null) }
    }

    fun searchAniList(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _search.update { it.copy(isLoading = true, error = null) }
            repo.searchMedia(q, _search.value.type).fold(
                onSuccess = { results -> _search.update { it.copy(results = results, isLoading = false) } },
                onFailure = { e -> _search.update { it.copy(isLoading = false, error = friendlyError(e)) } }
            )
        }
    }

    // --- Detail ---

    private val _detail = MutableStateFlow(DetailUiState())
    val detailUiState: StateFlow<DetailUiState> = _detail.asStateFlow()

    fun openDetail(mediaId: Int) {
        _detail.value = DetailUiState(isLoading = true)
        viewModelScope.launch {
            repo.mediaById(mediaId).fold(
                onSuccess = { m -> _detail.update { it.copy(media = m, isLoading = false, error = null) } },
                onFailure = { e -> _detail.update { it.copy(isLoading = false, error = friendlyError(e)) } }
            )
        }
    }

    fun clearDetail() {
        _detail.value = DetailUiState()
    }

    fun entryForMedia(mediaId: Int) = repo.entryForMedia(mediaId)

    // --- Account / sync ---

    fun completeLogin(code: String) {
        viewModelScope.launch {
            repo.completeLogin(code).onSuccess { repo.syncLibrary() }
        }
    }

    fun logout() = repo.logout()

    fun refreshAccount() {
        viewModelScope.launch { repo.loadAccount() }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            repo.syncLibrary().fold(
                onSuccess = { repo.notify("Synced $it entries with AniList") },
                onFailure = { e -> repo.notify(friendlyError(e)) }
            )
            _isSyncing.value = false
        }
    }

    fun clearMessage() {
        repo.clearSyncMessage()
    }

    // --- Settings ---

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    fun setDynamicColor(enabled: Boolean) = settings.setDynamicColor(enabled)
    fun setAmoledBlack(enabled: Boolean) = settings.setAmoledBlack(enabled)
    fun setHasSeenOnboarding(seen: Boolean) = settings.setHasSeenOnboarding(seen)

    // --- Categories ---

    fun addCategory(name: String, colorHex: String?) {
        viewModelScope.launch { repo.addCategory(name, colorHex) }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch { repo.deleteCategory(id) }
    }

    // --- Library mutations ---

    fun setEntryStatus(entry: LibraryEntryEntity, status: MediaStatus) {
        viewModelScope.launch { repo.setStatus(entry, status) }
    }

    fun setEntryProgress(entry: LibraryEntryEntity, progress: Int) {
        viewModelScope.launch { repo.setProgress(entry, progress) }
    }

    fun setEntryScore(entry: LibraryEntryEntity, score: Int) {
        viewModelScope.launch { repo.setScore(entry, score) }
    }

    fun setEntryNotes(entry: LibraryEntryEntity, notes: String) {
        viewModelScope.launch { repo.setNotes(entry, notes) }
    }

    fun toggleEntryFavorite(entry: LibraryEntryEntity) {
        viewModelScope.launch { repo.toggleFavorite(entry) }
    }

    fun removeEntry(entry: LibraryEntryEntity) {
        viewModelScope.launch { repo.removeEntry(entry) }
    }

    fun addManualEntry(entry: LibraryEntryEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.addManualEntry(entry)
            onDone()
        }
    }

    fun addFromDetail(media: MediaDetail, status: MediaStatus, type: MediaType, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.addFromDetail(media, status, type)
            onDone()
        }
    }

    // --- Search history ---

    private val historyPrefs = getApplication<Application>()
        .getSharedPreferences(SEARCH_HISTORY_PREFS, Context.MODE_PRIVATE)

    private val _searchHistory = MutableStateFlow(loadSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    fun recordSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val updated = (listOf(q) + _searchHistory.value.filterNot { it.equals(q, ignoreCase = true) })
            .take(MAX_SEARCH_HISTORY)
        _searchHistory.value = updated
        runCatching { historyPrefs.edit().putString(SEARCH_HISTORY_KEY, updated.joinToString("\n")).apply() }
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        runCatching { historyPrefs.edit().remove(SEARCH_HISTORY_KEY).apply() }
    }

    private fun loadSearchHistory(): List<String> =
        historyPrefs.getString(SEARCH_HISTORY_KEY, null)
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.take(MAX_SEARCH_HISTORY)
            ?: emptyList()

    // --- Statistics ---

    val statsUiState: StateFlow<StatsUiState> = repo.library
        .combine(repo.categories) { entries, _ -> computeStats(entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    init {
        viewModelScope.launch { repo.loadAccount() }
        refreshDiscover()
    }

    private fun filterEntries(all: List<LibraryEntryEntity>, config: FilterConfig): List<LibraryEntryEntity> {
        val filtered = all
            .filter { config.status == null || it.status == config.status }
            .filter { config.type == null || it.type == config.type }
            .filter { config.category == null || it.categoryId == config.category }
            .filter { config.query.isBlank() || it.title.contains(config.query, ignoreCase = true) }
            .filter { !config.favoritesOnly || it.isFavorite }
        return when (config.sort) {
            LibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            LibrarySort.SCORE -> filtered.sortedByDescending { it.score }
            LibrarySort.PROGRESS -> filtered.sortedByDescending { entry ->
                val total = entry.totalCount() ?: 0
                if (total > 0) entry.progress.toFloat() / total else 0f
            }
            LibrarySort.LAST_UPDATED -> filtered.sortedByDescending { it.lastUpdated }
        }
    }

    private fun computeStats(entries: List<LibraryEntryEntity>): StatsUiState {
        if (entries.isEmpty()) return StatsUiState()
        val total = entries.size
        val completed = entries.count { it.status == MediaStatus.COMPLETED }
        val current = entries.count { it.status == MediaStatus.CURRENT }
        val planning = entries.count { it.status == MediaStatus.PLANNING }
        val dropped = entries.count { it.status == MediaStatus.DROPPED }
        val paused = entries.count { it.status == MediaStatus.PAUSED }
        val repeating = entries.count { it.status == MediaStatus.REPEATING }
        val favorites = entries.count { it.isFavorite }

        val scored = entries.filter { it.score > 0 }
        val meanScore = if (scored.isNotEmpty()) scored.map { it.score }.average() else 0.0
        val totalProgress = entries.sumOf { it.progress }

        val genreCounts = mutableMapOf<String, Int>()
        entries.forEach { e -> e.genres.forEach { g -> genreCounts[g] = genreCounts.getOrDefault(g, 0) + 1 } }
        val genreDistribution = genreCounts
            .map { (g, c) -> GenreStat(g, c, c.toFloat() / total) }
            .sortedByDescending { it.count }
            .take(8)

        val formatCounts = entries.groupingBy { it.format ?: "Unknown" }.eachCount()
        val formatDistribution = formatCounts
            .map { (f, c) -> FormatStat(f, c, c.toFloat() / total) }
            .sortedByDescending { it.count }
            .take(8)

        return StatsUiState(
            totalEntries = total,
            current = current,
            planning = planning,
            completed = completed,
            dropped = dropped,
            paused = paused,
            repeating = repeating,
            favorites = favorites,
            completionRate = completed.toFloat() / total,
            meanScore = meanScore,
            totalProgress = totalProgress,
            genreDistribution = genreDistribution,
            formatDistribution = formatDistribution,
            topRated = scored.sortedByDescending { it.score }.take(10),
            recentlyUpdated = entries.sortedByDescending { it.lastUpdated }.take(10)
        )
    }
}

private const val SEARCH_HISTORY_PREFS = "search_history"
private const val SEARCH_HISTORY_KEY = "history_list"
private const val MAX_SEARCH_HISTORY = 10
