package com.app.shouze.data

import com.app.shouze.data.local.AppDatabase
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.LibraryDao
import com.app.shouze.data.local.LibraryEntryEntity
import com.app.shouze.data.local.MediaStatus
import com.app.shouze.data.local.MediaType
import com.app.shouze.data.remote.AniListApi
import com.app.shouze.data.remote.AniListTitle
import com.app.shouze.data.remote.FuzzyDate
import com.app.shouze.data.remote.MediaDetail
import com.app.shouze.data.remote.MediaListEntryDto
import com.app.shouze.data.remote.ViewerDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

data class AccountUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val viewer: ViewerDto? = null,
    val error: String? = null
)

/**
 * Single source of truth for the library. Room is a local cache; AniList is the
 * canonical store. Every local mutation is optimistically applied and then
 * pushed to AniList via `SaveMediaListEntry`.
 */
class MediaRepository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val auth: AniListAuth,
    private val api: AniListApi
) {
    private val libraryDao: LibraryDao = db.libraryDao()
    private val categoryDao = db.categoryDao()

    val library: Flow<List<LibraryEntryEntity>> = libraryDao.observeAll()
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAll()
    val settingsFlow: StateFlow<AppSettings> = settings.settings

    private val _account = MutableStateFlow(AccountUiState(isLoggedIn = auth.isLoggedIn))
    val account: StateFlow<AccountUiState> = _account.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /** Sets a transient user-facing message (e.g. sync results). */
    fun notify(message: String) {
        _syncMessage.value = message
    }

    fun entryForMedia(mediaId: Int): Flow<LibraryEntryEntity?> =
        library.map { list -> list.firstOrNull { it.mediaId == mediaId } }

    // --- Account ---

    suspend fun loadAccount() {
        val token = auth.token()
        if (token == null) {
            _account.value = AccountUiState(isLoggedIn = false)
            return
        }
        _account.value = AccountUiState(isLoggedIn = true, isLoading = true)
        api.viewer(token.accessToken).fold(
            onSuccess = { viewer -> _account.value = AccountUiState(isLoggedIn = true, viewer = viewer) },
            onFailure = { e -> _account.value = AccountUiState(isLoggedIn = true, error = friendlyError(e)) }
        )
    }

    suspend fun completeLogin(code: String): Result<ViewerDto> =
        auth.exchangeCode(code).fold(
            onSuccess = { token ->
                api.viewer(token.accessToken).fold(
                    onSuccess = { viewer ->
                        _account.value = AccountUiState(isLoggedIn = true, viewer = viewer)
                        Result.success(viewer)
                    },
                    onFailure = { e ->
                        _account.value = AccountUiState(isLoggedIn = true, error = friendlyError(e))
                        Result.failure(e)
                    }
                )
            },
            onFailure = { e ->
                _account.value = AccountUiState(isLoggedIn = false, error = friendlyError(e))
                Result.failure(e)
            }
        )

    fun logout() {
        auth.clearToken()
        _account.value = AccountUiState(isLoggedIn = false)
    }

    // --- Sync ---

    suspend fun syncLibrary(): Result<Int> {
        val token = auth.token() ?: return Result.failure(IllegalStateException("Not signed in to AniList"))
        val viewerResult = api.viewer(token.accessToken)
        val viewer = viewerResult.getOrElse {
            _account.value = _account.value.copy(error = friendlyError(it))
            return Result.failure(it)
        }
        _account.value = AccountUiState(isLoggedIn = true, viewer = viewer)

        return api.mediaListCollection(viewer.id, token.accessToken).map { remote ->
            val now = System.currentTimeMillis()
            val local = libraryDao.getAllSnapshot()
            val byMediaId = local.filter { it.mediaId != null }.associateBy { it.mediaId }
            val categoryIds = categoryDao.getAllSnapshot().associate { it.name.lowercase() to it.id }

            val upserts = remote.mapNotNull { dto ->
                dto.media?.let { m ->
                    val existing = byMediaId[dto.mediaId]
                    val type = if (m.type == "MANGA") MediaType.MANGA else MediaType.ANIME
                    LibraryEntryEntity(
                        localId = existing?.localId ?: UUID.randomUUID().toString(),
                        anilistListId = dto.id,
                        mediaId = dto.mediaId,
                        title = displayTitle(m.title),
                        type = type,
                        categoryId = existing?.categoryId ?: defaultCategoryId(categoryIds, type),
                        format = m.format,
                        status = parseStatus(dto.status),
                        progress = dto.progress ?: 0,
                        progressVolumes = dto.progressVolumes,
                        totalEpisodes = m.episodes,
                        totalChapters = m.chapters,
                        totalVolumes = m.volumes,
                        score = dto.score?.toInt() ?: 0,
                        repeat = dto.repeat ?: 0,
                        notes = dto.notes.orEmpty(),
                        coverImageUrl = m.coverImage?.extraLarge ?: m.coverImage?.large ?: m.coverImage?.medium,
                        bannerImageUrl = m.bannerImage,
                        genres = m.genres ?: emptyList(),
                        description = m.description,
                        season = m.season,
                        seasonYear = m.seasonYear,
                        averageScore = m.averageScore,
                        meanScore = m.meanScore,
                        popularity = m.popularity,
                        startedAt = dto.startedAt?.toIso(),
                        completedAt = dto.completedAt?.toIso(),
                        isFavorite = existing?.isFavorite ?: false,
                        pendingSync = false,
                        lastSyncedAt = now,
                        lastUpdated = existing?.lastUpdated ?: now
                    )
                }
            }
            libraryDao.upsertAll(upserts)

            // Entries the server no longer knows about (deleted on AniList) are
            // pruned, unless they are local-only or have unsaved local changes.
            val serverIds = remote.map { it.mediaId }.toSet()
            local.filter { it.mediaId != null && !it.pendingSync && it.mediaId !in serverIds }
                .forEach { libraryDao.deleteByLocalId(it.localId) }

            upserts.size
        }
    }

    // --- Mutations (optimistic local write, then push to AniList) ---

    suspend fun setStatus(entry: LibraryEntryEntity, status: MediaStatus) =
        mutate(entry.copy(status = status, pendingSync = true, lastUpdated = System.currentTimeMillis()))

    suspend fun setProgress(entry: LibraryEntryEntity, progress: Int) =
        mutate(entry.copy(progress = progress.coerceAtLeast(0), pendingSync = true, lastUpdated = System.currentTimeMillis()))

    suspend fun setVolumes(entry: LibraryEntryEntity, volumes: Int?) =
        mutate(entry.copy(progressVolumes = volumes, pendingSync = true, lastUpdated = System.currentTimeMillis()))

    suspend fun setScore(entry: LibraryEntryEntity, score: Int) =
        mutate(entry.copy(score = score.coerceIn(0, 100), pendingSync = true, lastUpdated = System.currentTimeMillis()))

    suspend fun setNotes(entry: LibraryEntryEntity, notes: String) =
        mutate(entry.copy(notes = notes, pendingSync = true, lastUpdated = System.currentTimeMillis()))

    suspend fun setRepeat(entry: LibraryEntryEntity, repeat: Int) =
        mutate(entry.copy(repeat = repeat.coerceAtLeast(0), pendingSync = true, lastUpdated = System.currentTimeMillis()))

    suspend fun toggleFavorite(entry: LibraryEntryEntity) {
        // Favourites are a local-only convenience flag.
        libraryDao.upsert(entry.copy(isFavorite = !entry.isFavorite, lastUpdated = System.currentTimeMillis()))
    }

    suspend fun removeEntry(entry: LibraryEntryEntity) {
        libraryDao.deleteByLocalId(entry.localId)
        val listId = entry.anilistListId ?: return
        val token = auth.token() ?: return
        api.deleteMediaListEntry(token.accessToken, listId)
    }

    suspend fun addManualEntry(entry: LibraryEntryEntity) {
        libraryDao.upsert(entry.copy(pendingSync = false, lastUpdated = System.currentTimeMillis()))
    }

    /** Adds an AniList media to the user's list and pushes it immediately. */
    suspend fun addFromDetail(detail: MediaDetail, status: MediaStatus, type: MediaType) {
        val categoryIds = categoryDao.getAllSnapshot().associate { it.name.lowercase() to it.id }
        val entry = LibraryEntryEntity(
            mediaId = detail.id,
            title = displayTitle(detail.title),
            type = type,
            categoryId = defaultCategoryId(categoryIds, type),
            format = detail.format,
            status = status,
            progress = if (status == MediaStatus.COMPLETED) detail.episodes ?: detail.chapters ?: 0 else 0,
            totalEpisodes = detail.episodes,
            totalChapters = detail.chapters,
            totalVolumes = detail.volumes,
            coverImageUrl = detail.coverImage?.extraLarge ?: detail.coverImage?.large ?: detail.coverImage?.medium,
            bannerImageUrl = detail.bannerImage,
            genres = detail.genres ?: emptyList(),
            description = detail.description,
            season = detail.season,
            seasonYear = detail.seasonYear,
            averageScore = detail.averageScore,
            meanScore = detail.meanScore,
            popularity = detail.popularity,
            pendingSync = true,
            lastUpdated = System.currentTimeMillis()
        )
        mutate(entry)
    }

    private suspend fun mutate(entry: LibraryEntryEntity) {
        libraryDao.upsert(entry)
        if (entry.mediaId == null) return
        val token = auth.token() ?: return
        pushToAniList(entry, token.accessToken).fold(
            onSuccess = { dto ->
                libraryDao.upsert(
                    entry.copy(
                        anilistListId = dto.id,
                        pendingSync = false,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )
            },
            onFailure = { e -> _syncMessage.value = friendlyError(e) }
        )
    }

    private suspend fun pushToAniList(entry: LibraryEntryEntity, token: String): Result<MediaListEntryDto> {
        val mediaId = entry.mediaId ?: return Result.failure(IllegalStateException("No AniList media id"))
        return api.saveMediaListEntry(
            token = token,
            mediaId = mediaId,
            status = entry.status.name,
            progress = entry.progress,
            progressVolumes = entry.progressVolumes,
            score = entry.score.toDouble(),
            notes = entry.notes,
            repeat = entry.repeat
        )
    }

    // --- Categories ---

    suspend fun addCategory(name: String, colorHex: String?) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        categoryDao.insert(CategoryEntity(name = trimmed, colorHex = colorHex?.takeIf { it.isNotBlank() }))
    }

    suspend fun deleteCategory(categoryId: String) {
        categoryDao.delete(categoryId)
        libraryDao.clearCategory(categoryId)
    }

    // --- Discovery passthroughs ---

    suspend fun searchMedia(query: String, type: String) = api.searchMedia(query, type)
    suspend fun trending(type: String, sort: String = "POPULARITY_DESC") = api.trending(type, sort)
    suspend fun seasonal(type: String, season: String, seasonYear: Int) = api.seasonal(type, season, seasonYear)
    suspend fun airing() = api.airingSchedules()
    suspend fun mediaById(id: Int) = api.mediaById(id)

    fun currentSeason(): Pair<String, Int> {
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH)
        val year = now.get(Calendar.YEAR)
        val season = when (month) {
            11, 0, 1 -> "WINTER"
            2, 3, 4 -> "SPRING"
            5, 6, 7 -> "SUMMER"
            else -> "FALL"
        }
        val seasonYear = if (month == 11) year + 1 else year
        return season to seasonYear
    }

    private fun defaultCategoryId(categoryIds: Map<String, String>, type: MediaType): String? =
        if (type == MediaType.MANGA) {
            categoryIds["manga"] ?: categoryIds["light novel"] ?: categoryIds["novel"]
        } else {
            categoryIds["anime"] ?: categoryIds["tv series"]
        }

    private fun displayTitle(title: AniListTitle?): String =
        title?.english?.takeIf { it.isNotBlank() }
            ?: title?.romaji?.takeIf { it.isNotBlank() }
            ?: title?.native?.takeIf { it.isNotBlank() }
            ?: "Unknown"

    private fun parseStatus(status: String?): MediaStatus = try {
        MediaStatus.valueOf((status ?: "PLANNING").uppercase())
    } catch (_: Exception) {
        MediaStatus.PLANNING
    }
}

private fun FuzzyDate.toIso(): String? {
    val y = year ?: return null
    val m = month?.toString()?.padStart(2, '0') ?: "00"
    val d = day?.toString()?.padStart(2, '0') ?: "00"
    return "$y-$m-$d"
}

fun friendlyError(e: Throwable?): String {
    val msg = e?.message?.lowercase() ?: ""
    return when {
        msg.contains("resolve host") || msg.contains("unknownhost") ||
            msg.contains("unable to resolve") || msg.contains("no address associated with hostname") ->
            "Couldn't reach AniList. Check your internet connection and try again."
        msg.contains("timeout") || msg.contains("timed out") ->
            "The request timed out. Please try again."
        msg.contains("rate limit") || msg.contains("too many requests") ->
            "Too many requests to AniList. Please wait a moment and try again."
        msg.contains("invalid token") || msg.contains("unauthorized") || msg.contains("401") ->
            "Your AniList session expired. Please sign in again."
        msg.contains("http 404") || msg.contains("not found") ->
            "That wasn't found on AniList."
        msg.contains("client id") || msg.contains("client_secret") || msg.contains("credentials") ->
            "AniList API credentials look wrong. Check them in your account settings."
        msg.isNotBlank() -> "Something went wrong reaching AniList: ${e?.message}"
        else -> "Something went wrong reaching AniList. Please try again."
    }
}
