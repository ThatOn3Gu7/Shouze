package com.app.shouze.data.local

import androidx.room.*
import java.util.UUID

/**
 * User-facing tracking status.
 *
 * WATCHING/READING distinguish live progress on anime vs. literature locally;
 * on AniList both map to the single `CURRENT` list status (the media type decides
 * the label). PAUSED and REPEATING mirror AniList's `PAUSED` / `REPEATING`.
 */
enum class Status { WATCHING, READING, COMPLETED, DROPPED, PLAN_TO_WATCH, PAUSED, REPEATING }

/** Where a library item comes from. */
enum class MediaSource { LOCAL, ANILIST }

@Entity(tableName = "media_items", indices = [Index("anilistId")])
data class MediaItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val categoryId: String,
    val status: Status,
    val currentProgress: Int,
    val totalCount: Int,
    val currentVolume: Int? = null,
    val rating: Double = 0.0,
    val coverImageUri: String? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val notes: String = "",
    val rewatchCount: Int = 0,
    val startDate: Long? = null,
    val endDate: Long? = null,
    // --- AniList-backed fields (ignored for LOCAL items) ---
    val source: MediaSource = MediaSource.LOCAL,
    /** AniList media id (the show/manga itself). */
    val anilistId: Int? = null,
    /** AniList list entry id (the user's tracking row for that media). */
    val listEntryId: Int? = null,
    /** AniList media type: ANIME or MANGA. */
    val mediaType: String? = null,
    /** True while local edits are queued/flushing to AniList and not yet acknowledged. */
    val pendingSync: Boolean = false
) {
    val isAniListBacked: Boolean get() = source == MediaSource.ANILIST && anilistId != null
}
