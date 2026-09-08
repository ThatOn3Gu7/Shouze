package com.app.shouze.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.shouze.data.SettingsRepository
import com.app.shouze.data.ThemeMode
import com.app.shouze.data.auth.AniListAuthRepository
import com.app.shouze.data.auth.AniListAuthState
import com.app.shouze.data.auth.ImplicitRedirectParser
import com.app.shouze.data.local.*
import com.app.shouze.data.mapper.AniListMapper
import com.app.shouze.data.remote.AniListApi
import com.app.shouze.data.remote.AniListException
import com.app.shouze.data.remote.AniListMedia
import com.app.shouze.data.sync.AniListLibraryRepository
import com.app.shouze.data.sync.NetworkMonitor
import com.app.shouze.ui.components.CoverImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
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
    val searchType: String = "ANIME",
    val trending: List<AniListMedia> = emptyList(),
    val isTrendingLoading: Boolean = false,
    val trendingError: String? = null,
    val lastQuery: String = "",
    // Pagination + offline hints
    val canLoadMore: Boolean = false,
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val resultsFromCache: Boolean = false,
    val trendingFromCache: Boolean = false
)

data class AiringScheduleUiState(
    val schedules: List<com.app.shouze.data.remote.AiringSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** True when showing the cached schedule because AniList is unreachable. */
    val fromCache: Boolean = false
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
    private val authRepository = AniListAuthRepository(application)
    private val networkMonitor = NetworkMonitor(application)
    private val aniListApi = AniListApi { authRepository.accessToken() }
    private val libraryRepository = AniListLibraryRepository(db, aniListApi, authRepository)
    private val json = Json { ignoreUnknownKeys = true }

    val settings = settingsRepo.settings
    val settingsRepository: SettingsRepository = settingsRepo

    // --- AniList account & sync state ---
    val authState: StateFlow<AniListAuthState> = authRepository.state
    val syncStatus: StateFlow<AniListLibraryRepository.SyncStatus> = libraryRepository.syncStatus
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, networkMonitor.isOnline())

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
        viewModelScope.launch {
            fetchTrendingNow()
        }
        viewModelScope.launch {
            libraryRepository.primeSyncStatus()
        }
        viewModelScope.launch {
            // Auto-refresh: quietly reconcile with AniList on launch if the cached
            // library is stale and the device is online.
            if (authState.value.isSignedIn && isOnline.value) {
                libraryRepository.refreshLibrary(force = false)
            }
        }
        viewModelScope.launch {
            // Flush queued edits whenever connectivity returns.
            isOnline.collect { online ->
                if (online && authState.value.isSignedIn) {
                    val pending = libraryRepository.pendingOpsSnapshot()
                    if (pending > 0) {
                        libraryRepository.flushOutbox()
                        libraryRepository.refreshLibrary(force = true)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // AniList account management
    // ------------------------------------------------------------------

    /**
     * Builds the AniList OAuth authorize URL (Implicit Grant). Returns null with a
     * helpful message when the app hasn't been given a client id yet.
     */
    fun startAniListLogin(): String? {
        if (!authRepository.isTokenConfigured()) {
            authRepository.setLoginError(
                "One-tap sign-in isn't set up in this build — use \"Sign in manually\" below, "
                    + "or add an AniList client id when building (docs/ANILIST_SETUP.md)."
            )
            return null
        }
        authRepository.setLoginError(null)
        return authRepository.startLoginUrl()
    }

    /** Completes login from the shouze://anilist-auth deep link (or manual token). */
    fun handleAniListRedirect(uriString: String) {
        viewModelScope.launch {
            when (val parsed = ImplicitRedirectParser.parse(uriString)) {
                is ImplicitRedirectParser.Result.NotAnAuthRedirect -> Unit
                is ImplicitRedirectParser.Result.Denied -> {
                    val reason = parsed.description ?: parsed.error
                    authRepository.setLoginError("AniList sign-in failed: $reason")
                }
                is ImplicitRedirectParser.Result.Success -> {
                    val token = parsed.parsed.accessToken
                    val viewerResult = aniListApi.getViewer(token)
                    viewerResult.fold(
                        onSuccess = { viewer ->
                            authRepository.applyToken(token, parsed.parsed.expiresInSeconds)
                            authRepository.applyViewer(
                                userId = viewer.id,
                                userName = viewer.name,
                                avatarUrl = viewer.avatar?.large ?: viewer.avatar?.medium,
                                bannerUrl = viewer.bannerImage,
                                profileUrl = viewer.siteUrl,
                                scoreFormat = viewer.mediaListOptions?.scoreFormat
                                    ?: "POINT_100"
                            )
                            libraryRepository.ensureDefaultCategories()
                            showMessage("Signed in to AniList as ${viewer.name}")
                            libraryRepository.refreshLibrary(force = true)
                        },
                        onFailure = { e ->
                            authRepository.setLoginError(friendlyError(e))
                        }
                    )
                }
            }
        }
    }

    /** Authorize URL for the manual paste flow, or null when no client id is configured. */
    val manualLoginUrl: String?
        get() = if (authRepository.isTokenConfigured()) authRepository.startLoginUrl() else null

    /**
     * Manual sign-in fallback: accepts either the full address the browser was
     * redirected to (shouze://anilist-auth#access_token=…) or a bare token.
     */
    fun loginWithManualToken(input: String) {
        val trimmed = input.trim().removeSurrounding("\"")
        if (trimmed.isEmpty()) {
            authRepository.setLoginError("Paste the address you were redirected to, or your AniList token.")
            return
        }
        if (trimmed.contains("access_token=")) {
            handleAniListRedirect(trimmed)
        } else {
            handleAniListRedirect("shouze://anilist-auth#access_token=${Uri.encode(trimmed)}&expires=31536000")
        }
    }

    fun logoutFromAniList() {
        val name = authState.value.session?.userName
        authRepository.logout()
        viewModelScope.launch {
            libraryRepository.onSignedOut()
        }
        showMessage(if (name != null) "Signed out of AniList ($name)" else "Signed out of AniList")
    }

    /** Manual pull + push. Safe to call anytime; no-ops when signed out. */
    fun syncNow() {
        viewModelScope.launch {
            if (!authState.value.isSignedIn) return@launch
            val delivered = libraryRepository.flushOutbox()
            // Capture drain errors before the pull refresh clears them.
            val drainError = libraryRepository.syncStatus.value.lastError
            val result = libraryRepository.refreshLibrary(force = true)
            val status = libraryRepository.syncStatus.value
            when {
                drainError != null -> showMessage(drainError, isError = true)
                status.lastError != null -> showMessage(status.lastError!!, isError = true)
                delivered > 0 -> showMessage("Synced $delivered change(s) to AniList ✓")
                result.isSuccess -> showMessage("Up to date with AniList ✓")
                else -> showMessage("Couldn't reach AniList — try again when you're online.", isError = true)
            }
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

            persist(updated)
        }
    }

    /**
     * Single write entry point: AniList-backed items go through the optimistic
     * sync engine (instant local apply + queued mutation), local items stay in Room.
     */
    private suspend fun persist(item: MediaItemEntity) {
        if (item.isAniListBacked) {
            libraryRepository.applyEntryEdit(item)
            // Fire-and-forget drain; the rate limiter paces it if the user is spamming edits.
            libraryRepository.flushOutbox()
        } else {
            dao.insertOrUpdate(item)
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
            val item = dao.getById(itemId)
            if (item != null && item.isAniListBacked) {
                libraryRepository.applyEntryDelete(item)
                libraryRepository.flushOutbox()
            } else {
                dao.deleteById(itemId)
            }
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

    fun clearHomeFilters() {
        _searchQuery.value = ""
        _selectedCategoryId.value = null
        _selectedTag.value = null
        if (_showFavoritesOnly.value) {
            _showFavoritesOnly.value = false
        }
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

    fun addCategory(name: String, colorHex: String? = null) {
        viewModelScope.launch {
            categoryDao.insert(CategoryEntity(name = name.trim(), colorHex = colorHex))
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
            val selected = _uiState.value.allItems
                .filter { it.id in _uiState.value.selectedIds }
            val local = selected.filter { !it.isAniListBacked }
            val skipped = selected.size - local.size
            local.forEach { dao.deleteById(it.id) }
            showMessage(
                buildString {
                    append("Deleted ${local.size} items")
                    if (skipped > 0) append(" — $skipped AniList item(s) skipped (manage them individually)")
                }
            )
            clearSelection()
        }
    }

    fun bulkUpdateCategory(categoryId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val selected = _uiState.value.allItems
                .filter { it.id in _uiState.value.selectedIds }
            val local = selected.filter { !it.isAniListBacked }
            val skipped = selected.size - local.size
            local.forEach { dao.insertOrUpdate(it.copy(categoryId = categoryId, lastUpdated = now)) }
            val categoryName = _uiState.value.categories.find { it.id == categoryId }?.name ?: "new category"
            showMessage(
                buildString {
                    append("Moved ${local.size} items to $categoryName")
                    if (skipped > 0) append(" — $skipped AniList item(s) skipped")
                }
            )
            clearSelection()
        }
    }

    fun bulkUpdateStatus(status: Status) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val selected = _uiState.value.allItems
                .filter { it.id in _uiState.value.selectedIds }
            val local = selected.filter { !it.isAniListBacked }
            val skipped = selected.size - local.size
            local.forEach { item ->
                var updated = item.copy(status = status, lastUpdated = now)
                if ((status == Status.WATCHING || status == Status.READING) && item.startDate == null) {
                    updated = updated.copy(startDate = now)
                }
                if (status == Status.COMPLETED && item.endDate == null) {
                    updated = updated.copy(endDate = now)
                }
                dao.insertOrUpdate(updated)
            }
            val statusName = status.name.lowercase().split("_").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
            showMessage(
                buildString {
                    append("Marked ${local.size} items as $statusName")
                    if (skipped > 0) append(" — $skipped AniList item(s) skipped")
                }
            )
            clearSelection()
        }
    }

    fun bulkToggleFavorite() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val selected = _uiState.value.allItems
                .filter { it.id in _uiState.value.selectedIds }
            val local = selected.filter { !it.isAniListBacked }
            val skipped = selected.size - local.size
            // Smart toggle: if any selected item is NOT a favorite, favorite them all.
            // Only when every selected item is already a favorite do we unfavorite.
            val addToFavorites = local.any { !it.isFavorite }
            local.forEach { dao.insertOrUpdate(it.copy(isFavorite = addToFavorites, lastUpdated = now)) }
            showMessage(
                buildString {
                    if (addToFavorites) append("Added ${local.size} items to favorites")
                    else append("Removed ${local.size} items from favorites")
                    if (skipped > 0) append(" — $skipped AniList item(s) skipped")
                }
            )
            clearSelection()
        }
    }

    // --- Quick progress actions ---

    private fun isLiterature(item: MediaItemEntity): Boolean {
        val cat = _uiState.value.categories.find { it.id == item.categoryId }
        return cat?.name?.let { name ->
            name.contains("novel", ignoreCase = true) ||
                name.contains("book", ignoreCase = true) ||
                name.contains("manga", ignoreCase = true)
        } ?: false
    }

    fun incrementProgress(itemId: String) {
        viewModelScope.launch {
            val item = _uiState.value.allItems.find { it.id == itemId } ?: return@launch
            val now = System.currentTimeMillis()
            val newProgress = if (item.totalCount > 0) {
                (item.currentProgress + 1).coerceAtMost(item.totalCount)
            } else {
                item.currentProgress + 1
            }
            var updated = item.copy(currentProgress = newProgress, lastUpdated = now)
            if (item.status == Status.PLAN_TO_WATCH || item.status == Status.COMPLETED) {
                updated = updated.copy(
                    status = if (isLiterature(item) || item.mediaType.equals("MANGA", true)) Status.READING else Status.WATCHING,
                    startDate = item.startDate ?: now
                )
            }
            // An AniList entry that reaches its final episode/chapter is completed there too.
            if (item.isAniListBacked && item.totalCount in 1..newProgress) {
                updated = updated.copy(status = Status.COMPLETED, endDate = item.endDate ?: now)
            }
            persist(updated)
        }
    }

    fun markCompleted(itemId: String) {
        viewModelScope.launch {
            val item = _uiState.value.allItems.find { it.id == itemId } ?: return@launch
            val now = System.currentTimeMillis()
            val updated = item.copy(
                currentProgress = item.totalCount,
                status = Status.COMPLETED,
                endDate = item.endDate ?: now,
                lastUpdated = now
            )
            persist(updated)
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
            _searchUiState.update { it.copy(isLoading = true, error = null, lastQuery = query.trim()) }
            val type = _searchUiState.value.searchType
            val result = aniListApi.searchMediaPaged(query, type, 1)
            result.fold(
                onSuccess = { page ->
                    cacheSearchPage(query, type, 1, page)
                    _searchUiState.update {
                        it.copy(
                            results = page.media,
                            isLoading = false,
                            canLoadMore = page.hasNextPage,
                            currentPage = 1,
                            resultsFromCache = false
                        )
                    }
                },
                onFailure = { e ->
                    val cached = readCachedSearchPage(query, type, 1)
                    if (cached != null) {
                        _searchUiState.update {
                            it.copy(
                                results = cached.media,
                                isLoading = false,
                                canLoadMore = false,
                                resultsFromCache = true,
                                error = null
                            )
                        }
                        showMessage("Offline — showing cached results for \"$query\"", isError = false)
                    } else {
                        _searchUiState.update { it.copy(error = friendlyError(e), isLoading = false) }
                    }
                }
            )
        }
    }

    /** Fetches the next page of the current search (rate limit aware, cached per page). */
    fun loadMoreSearchResults() {
        val state = _searchUiState.value
        if (!state.canLoadMore || state.isLoading || state.isLoadingMore || state.lastQuery.isBlank()) return
        viewModelScope.launch {
            _searchUiState.update { it.copy(isLoadingMore = true) }
            val nextPage = state.currentPage + 1
            val result = aniListApi.searchMediaPaged(state.lastQuery, state.searchType, nextPage)
            result.fold(
                onSuccess = { page ->
                    cacheSearchPage(state.lastQuery, state.searchType, nextPage, page)
                    _searchUiState.update {
                        it.copy(
                            results = it.results + page.media,
                            isLoadingMore = false,
                            canLoadMore = page.hasNextPage,
                            currentPage = nextPage,
                            resultsFromCache = false
                        )
                    }
                },
                onFailure = { e ->
                    _searchUiState.update { it.copy(isLoadingMore = false, error = friendlyError(e)) }
                }
            )
        }
    }

    fun setSearchType(type: String) {
        _searchUiState.update { it.copy(searchType = type, results = emptyList(), trending = emptyList()) }
    }

    fun loadTrending() {
        viewModelScope.launch {
            fetchTrendingNow()
        }
    }

    private suspend fun fetchTrendingNow() {
        if (_searchUiState.value.trending.isNotEmpty()) return
        if (_searchUiState.value.isTrendingLoading) return
        _searchUiState.update { it.copy(isTrendingLoading = true, trendingError = null) }
        val type = _searchUiState.value.searchType
        val result = aniListApi.getTrending(type)
        result.fold(
            onSuccess = { media ->
                cacheList(RemoteCacheKeys.TRENDING_PREFIX + type.lowercase(), AniListMedia.serializer(), media)
                _searchUiState.update { it.copy(trending = media, isTrendingLoading = false, trendingFromCache = false) }
                preloadTrendingCovers()
            },
            onFailure = { e ->
                val cached = readCachedList(RemoteCacheKeys.TRENDING_PREFIX + type.lowercase(), AniListMedia.serializer())
                if (cached != null) {
                    _searchUiState.update {
                        it.copy(trending = cached, isTrendingLoading = false, trendingFromCache = true, trendingError = null)
                    }
                } else {
                    _searchUiState.update {
                        it.copy(isTrendingLoading = false, trendingError = friendlyError(e))
                    }
                }
            }
        )
    }

    private suspend fun preloadTrendingCovers() {
        val trending = _searchUiState.value.trending
        if (trending.isEmpty()) return
        CoverImageStore.init(getApplication())
        withContext(Dispatchers.IO) {
            trending.forEach { media ->
                val url = media.coverImage?.medium ?: media.coverImage?.large
                if (!url.isNullOrBlank()) {
                    val bitmap = CoverImageStore.getOrLoad(url) ?: return@forEach
                    CoverImageStore.imageBitmap(url, bitmap)
                }
            }
        }
    }

    fun clearSearchResults() {
        _searchUiState.update { AniListSearchUiState() }
    }

    // --- Search History ---

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
        runCatching {
            historyPrefs.edit().putString(SEARCH_HISTORY_KEY, updated.joinToString("\n")).apply()
        }
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

    // --- Airing Schedule ---

    fun fetchAiringSchedule() {
        viewModelScope.launch {
            _airingScheduleUiState.update { it.copy(isLoading = true, error = null) }
            val result = aniListApi.getAiringSchedule()
            result.fold(
                onSuccess = { schedules ->
                    cacheList(RemoteCacheKeys.AIRING_SCHEDULE, com.app.shouze.data.remote.AiringSchedule.serializer(), schedules)
                    _airingScheduleUiState.update { it.copy(schedules = schedules, isLoading = false, fromCache = false) }
                },
                onFailure = { e ->
                    val cached = readCachedList(RemoteCacheKeys.AIRING_SCHEDULE, com.app.shouze.data.remote.AiringSchedule.serializer())
                    if (cached != null) {
                        _airingScheduleUiState.update {
                            it.copy(schedules = cached, isLoading = false, fromCache = true, error = null)
                        }
                    } else {
                        _airingScheduleUiState.update { it.copy(error = friendlyError(e), isLoading = false) }
                    }
                }
            )
        }
    }

    // --- Discovery cache helpers (offline read-only support) ---

    private suspend fun cacheSearchPage(query: String, type: String, page: Int, data: AniListApi.AniListMediaPage) {
        try {
            libraryRepository.cacheJson(
                RemoteCacheKeys.search(query, type, page),
                json.encodeToString(AniListApi.AniListMediaPage.serializer(), data)
            )
        } catch (_: Exception) {
        }
    }

    private suspend fun readCachedSearchPage(query: String, type: String, page: Int): AniListApi.AniListMediaPage? =
        try {
            libraryRepository.cachedJson(RemoteCacheKeys.search(query, type, page))?.let {
                json.decodeFromString(AniListApi.AniListMediaPage.serializer(), it)
            }
        } catch (_: Exception) {
            null
        }

    private suspend fun <T> cacheList(key: String, serializer: KSerializer<T>, value: List<T>) {
        try {
            libraryRepository.cacheJson(key, json.encodeToString(ListSerializer(serializer), value))
        } catch (_: Exception) {
        }
    }

    private suspend fun <T> readCachedList(key: String, serializer: KSerializer<T>): List<T>? =
        try {
            libraryRepository.cachedJson(key)?.let { json.decodeFromString(ListSerializer(serializer), it) }
        } catch (_: Exception) {
            null
        }

    fun createItemFromAiringSchedule(schedule: com.app.shouze.data.remote.AiringSchedule): MediaItemEntity {
        val title = schedule.media.title.english ?: schedule.media.title.romaji ?: "Unknown"
        val categories = uiState.value.categories
        val categoryId = categories.find { it.name.equals("Anime", ignoreCase = true) }?.id
            ?: categories.find { it.name.equals("TV Series", ignoreCase = true) }?.id
            ?: categories.firstOrNull()?.id ?: ""

        // When signed in, "add" means track it on the user's AniList planning list.
        if (authState.value.isSignedIn) {
            return MediaItemEntity(
                id = AniListMapper.localIdFor(schedule.media.id),
                title = title,
                categoryId = AniListMapper.resolveCategoryId("ANIME", categories),
                status = Status.PLAN_TO_WATCH,
                currentProgress = 0,
                totalCount = 0,
                coverImageUri = schedule.media.coverImage?.large ?: schedule.media.coverImage?.medium,
                source = MediaSource.ANILIST,
                anilistId = schedule.media.id,
                listEntryId = null,
                mediaType = "ANIME"
            )
        }

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

    fun loadStreamingForTitle(title: String, type: String = "ANIME") {
        viewModelScope.launch {
            _streamingUiState.update { it.copy(isLoading = true, error = null, title = title) }
            val searchResult = aniListApi.searchMedia(title, type)
            searchResult.fold(
                onSuccess = { mediaList ->
                    val match = mediaList.firstOrNull { candidate ->
                        val candidateTitle = candidate.title.english ?: candidate.title.romaji ?: ""
                        candidateTitle.equals(title, ignoreCase = true)
                    } ?: mediaList.firstOrNull()
                    if (match != null) {
                        val streamResult = aniListApi.getStreamingEpisodes(match.id)
                        streamResult.fold(
                            onSuccess = { (episodes, links) ->
                                _streamingUiState.update {
                                    it.copy(streamingEpisodes = episodes, externalLinks = links, isLoading = false)
                                }
                            },
                            onFailure = { e ->
                                _streamingUiState.update { it.copy(error = friendlyError(e), isLoading = false) }
                            }
                        )
                    } else {
                        _streamingUiState.update { it.copy(error = "That title wasn't found on AniList.", isLoading = false) }
                    }
                },
                onFailure = { e ->
                    _streamingUiState.update { it.copy(error = friendlyError(e), isLoading = false) }
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

    val selectedAniListMedia = mutableStateOf<AniListMedia?>(null)

    fun selectAniListMedia(media: AniListMedia) {
        selectedAniListMedia.value = media
    }

    fun clearSelectedAniListMedia() {
        selectedAniListMedia.value = null
    }

    fun createItemFromAniList(
        media: AniListMedia,
        defaultStatus: Status = Status.PLAN_TO_WATCH
    ): MediaItemEntity {
        val title = media.title.english ?: media.title.romaji ?: "Unknown"
        val mediaType = media.type ?: AniListMapper.mediaTypeFromFormat(media.format)

        // For anime: use episodes. For manga: use chapters, fall back to volumes.
        val totalCount = when {
            mediaType.equals("MANGA", true) -> media.chapters ?: media.volumes
            else -> media.episodes
        } ?: 0

        val coverImage = media.coverImage?.large ?: media.coverImage?.medium
        val genres = media.genres ?: emptyList()
        val notes = media.description?.let { desc ->
            desc.replace(Regex("<.*?>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        } ?: ""

        val categories = uiState.value.categories

        // Signed in: the entry is AniList-owned (stable id so re-adds update in place).
        if (authState.value.isSignedIn) {
            val existing = _uiState.value.allItems.firstOrNull { it.anilistId == media.id }
            val categoryId = AniListMapper.resolveCategoryId(mediaType, categories)
            return MediaItemEntity(
                id = existing?.id ?: AniListMapper.localIdFor(media.id),
                title = media.title.english ?: media.title.romaji ?: title,
                categoryId = categoryId,
                status = defaultStatus,
                currentProgress = existing?.currentProgress ?: 0,
                totalCount = totalCount,
                rating = existing?.rating ?: 0.0,
                coverImageUri = coverImage ?: existing?.coverImageUri,
                genres = genres,
                notes = notes,
                startDate = existing?.startDate,
                endDate = existing?.endDate,
                source = MediaSource.ANILIST,
                anilistId = media.id,
                listEntryId = existing?.listEntryId,
                mediaType = mediaType
            )
        }

        // Smart category matching using exact names first, then partial
        val categoryId = when (mediaType.uppercase()) {
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
            else -> {
                categories.find { it.name.equals("Anime", ignoreCase = true) }?.id
                    ?: categories.find { it.name.equals("TV Series", ignoreCase = true) }?.id
                    ?: categories.find { it.name.equals("OVA", ignoreCase = true) }?.id
                    ?: categories.find { it.name.equals("Movie", ignoreCase = true) }?.id
                    ?: categories.find { it.name.contains("anime", ignoreCase = true) }?.id
                    ?: categories.find { it.name.contains("tv", ignoreCase = true) }?.id
            }
        } ?: categories.firstOrNull()?.id ?: ""

        return MediaItemEntity(
            title = title,
            categoryId = categoryId,
            status = defaultStatus,
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
    fun setUsername(name: String) = settingsRepo.setUsername(name)
    fun setProfilePicture(uri: String?) = settingsRepo.setProfilePicture(uri)

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
                        val displayName = try {
                            getApplication<Application>().contentResolver
                                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                        } catch (_: Exception) {
                            null
                        }
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
                        showMessage(
                            if (displayName != null) "Backup \"$displayName\" saved successfully ($itemCount items)"
                            else "Backup saved successfully ($itemCount items)"
                        )
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
                        item.id.sanitizeCsv(),
                        item.title.sanitizeCsv(),
                        item.categoryId.sanitizeCsv(),
                        item.status.name.sanitizeCsv(),
                        item.currentProgress,
                        item.totalCount,
                        item.currentVolume ?: "",
                        item.rating,
                        item.coverImageUri?.sanitizeCsv() ?: "",
                        item.genres.joinToString(", ").sanitizeCsv(),
                        item.tags.joinToString(", ").sanitizeCsv(),
                        item.notes.replace("\n", " ").sanitizeCsv(),
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

                val items = withContext(Dispatchers.Default) { parseMalXml(xml) }
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

    private fun friendlyError(e: Throwable?): String {
        if (e is AniListException) {
            return when (e.kind) {
                AniListException.Kind.NETWORK ->
                    "Couldn't reach AniList. Check your internet connection and try again."
                AniListException.Kind.RATE_LIMITED ->
                    e.message?.ifBlank { null } ?: "Too many requests to AniList. Please wait a moment and try again."
                AniListException.Kind.UNAUTHORIZED ->
                    if (authState.value.isSignedIn) "Your AniList session expired — please sign in again."
                    else "AniList denied this request. Please sign in and try again."
                AniListException.Kind.GRAPHQL ->
                    e.message?.ifBlank { null } ?: "AniList couldn't process that request."
                AniListException.Kind.HTTP ->
                    e.message?.ifBlank { null } ?: "AniList is having server trouble right now. Please try again later."
            }
        }
        val msg = e?.message?.lowercase() ?: ""
        return when {
            msg.contains("resolve host") ||
                msg.contains("unknownhost") ||
                msg.contains("unable to resolve") ||
                msg.contains("no address associated with hostname") ->
                "Couldn't reach AniList. Check your internet connection and try again."
            msg.contains("timeout") || msg.contains("timed out") ->
                "The request timed out. Please try again."
            msg.contains("cleartext") || msg.contains("protocol") ->
                "A secure connection to AniList couldn't be made. Please try again."
            msg.contains("temporarily disabled") || msg.contains("stability") ->
                "AniList is temporarily down for maintenance. Please try again later."
            msg.contains("rate limit") || msg.contains("too many requests") ->
                "Too many requests to AniList. Please wait a moment and try again."
            msg.contains("http 5") || msg.contains("server error") ||
                msg.contains("bad gateway") || msg.contains("service unavailable") ->
                "AniList is having server trouble right now. Please try again later."
            msg.contains("http 404") || msg.contains("not found") ->
                "That wasn't found on AniList."
            msg.contains("unauthorized") || msg.contains("forbidden") ->
                "AniList denied this request. Please try again later."
            msg.isNotBlank() ->
                "Something went wrong reaching AniList: ${e?.message}"
            else -> "Something went wrong reaching AniList. Please try again."
        }
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

private const val SEARCH_HISTORY_PREFS = "search_history"
private const val SEARCH_HISTORY_KEY = "history_list"
private const val MAX_SEARCH_HISTORY = 10

private fun String.sanitizeCsv(): String {
    val sanitized = this.replace("\"", "\"\"")
    return if (sanitized.startsWith("=") || sanitized.startsWith("+") || sanitized.startsWith("-") || sanitized.startsWith("@") || sanitized.startsWith("\t") || sanitized.startsWith("\r")) {
        "'$sanitized"
    } else {
        "\"$sanitized\""
    }
}
