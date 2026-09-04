package com.app.shouze.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * AniList's list status vocabulary. This is now the single source of truth for
 * the "status" of every library entry, mirroring AniList exactly so statuses
 * round-trip cleanly during two-way sync.
 */
enum class MediaStatus { CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING }

/** Which side of the AniList catalogue this entry belongs to. */
enum class MediaType { ANIME, MANGA }

/**
 * A single entry in the user's library.
 *
 * When the entry came from AniList, [mediaId] / [anilistListId] are set and the
 * row is a cache of the user's real AniList list. When [mediaId] is null the row
 * is a local-only "manual" entry (the offline fallback).
 */
@Entity(
    tableName = "library_entries",
    indices = [Index(value = ["mediaId"], unique = true)]
)
data class LibraryEntryEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    /** AniList MediaListEntry id — null for local-only entries. */
    val anilistListId: Int? = null,
    /** AniList Media id — null for local-only entries. */
    val mediaId: Int? = null,
    val title: String,
    val type: MediaType = MediaType.ANIME,
    /** Optional local grouping category (kept separate from AniList lists). */
    val categoryId: String? = null,
    /** AniList format: TV, MOVIE, OVA, ONA, SPECIAL, MANGA, NOVEL, ONE_SHOT... */
    val format: String? = null,
    val status: MediaStatus = MediaStatus.PLANNING,
    val progress: Int = 0,
    val progressVolumes: Int? = null,
    val totalEpisodes: Int? = null,
    val totalChapters: Int? = null,
    val totalVolumes: Int? = null,
    /** AniList native 0–100 score. 0 = unscored. */
    val score: Int = 0,
    val repeat: Int = 0,
    val notes: String = "",
    val coverImageUrl: String? = null,
    val bannerImageUrl: String? = null,
    val genres: List<String> = emptyList(),
    val description: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    /** Fuzzy date, "yyyy-MM-dd" with missing parts as "00"/"0000". */
    val startedAt: String? = null,
    val completedAt: String? = null,
    val isFavorite: Boolean = false,
    /** Local edits not yet pushed to AniList. */
    val pendingSync: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /** Human label for the status, adjusted for the media type (anime -> Watching). */
    fun statusLabel(): String = when (status) {
        MediaStatus.CURRENT -> if (type == MediaType.MANGA) "Reading" else "Watching"
        MediaStatus.PLANNING -> if (type == MediaType.MANGA) "Plan to Read" else "Plan to Watch"
        MediaStatus.COMPLETED -> "Completed"
        MediaStatus.DROPPED -> "Dropped"
        MediaStatus.PAUSED -> "Paused"
        MediaStatus.REPEATING -> if (type == MediaType.MANGA) "Rereading" else "Rewatching"
    }

    /** Unit word for progress ("Episode" vs "Chapter"). */
    fun progressUnit(): String = if (type == MediaType.MANGA) "Chapter" else "Episode"

    /** Total length for display, falling back to chapters/volumes as appropriate. */
    fun totalCount(): Int? = if (type == MediaType.MANGA) totalChapters ?: totalVolumes else totalEpisodes
}
