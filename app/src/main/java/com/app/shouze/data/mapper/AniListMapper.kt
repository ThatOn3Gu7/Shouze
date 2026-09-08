package com.app.shouze.data.mapper

import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.data.local.MediaSource
import com.app.shouze.data.local.SaveEntryPayload
import com.app.shouze.data.local.Status
import com.app.shouze.data.remote.AniListListEntry
import com.app.shouze.data.remote.AniListMedia
import com.app.shouze.data.remote.AniListMediaListCollection
import com.app.shouze.data.remote.FuzzyDate
import java.util.Locale

/**
 * Conversions between AniList list entries and the app's [MediaItemEntity].
 *
 * Mapping rationale:
 *  - AniList has one `CURRENT` status for both anime and manga; the app splits it
 *    into WATCHING/READING by media type.
 *  - `PAUSED` / `REPEATING` map 1:1 to the app statuses added for this integration.
 *  - Scores: the app stores a 0..10 double. AniList stores the raw score in the
 *    user's own [ScoreFormat] scale, so both directions are converted per format.
 *  - Favorites/tags stay local-only (AniList favorites live on Media, not entries).
 */
object AniListMapper {

    // ---------------------- Status ----------------------

    fun statusFromAniList(aniListStatus: String?, mediaType: String?): Status = when (aniListStatus?.uppercase(Locale.US)) {
        "CURRENT" -> if (mediaType.equals("MANGA", true)) Status.READING else Status.WATCHING
        "PLANNING" -> Status.PLAN_TO_WATCH
        "COMPLETED" -> Status.COMPLETED
        "DROPPED" -> Status.DROPPED
        "PAUSED" -> Status.PAUSED
        "REPEATING" -> Status.REPEATING
        else -> Status.PLAN_TO_WATCH
    }

    fun statusToAniList(status: Status): String = when (status) {
        Status.WATCHING, Status.READING -> "CURRENT"
        Status.PLAN_TO_WATCH -> "PLANNING"
        Status.COMPLETED -> "COMPLETED"
        Status.DROPPED -> "DROPPED"
        Status.PAUSED -> "PAUSED"
        Status.REPEATING -> "REPEATING"
    }

    // ---------------------- Scores ----------------------

    /** App rating (0..10) -> AniList score in the user's format. */
    fun ratingToAniListScore(rating: Double, scoreFormat: String): Double = when (scoreFormat.uppercase(Locale.US)) {
        "POINT_100" -> (rating * 10).coerceIn(0.0, 100.0)
        "POINT_5" -> (rating / 2).coerceIn(0.0, 5.0)
        "POINT_3" -> rating.coerceIn(0.0, 3.0)
        // POINT_10 and POINT_10_DECIMAL share the 0..10 scale; AniList rounds POINT_10 itself.
        else -> rating.coerceIn(0.0, 10.0)
    }

    /** AniList score (in the user's format) -> app rating (0..10). */
    fun scoreToAppRating(score: Double, scoreFormat: String): Double = when (scoreFormat.uppercase(Locale.US)) {
        "POINT_100" -> (score / 10).coerceIn(0.0, 10.0)
        "POINT_5" -> (score * 2).coerceIn(0.0, 10.0)
        "POINT_3" -> score.coerceIn(0.0, 3.0)
        else -> score.coerceIn(0.0, 10.0)
    }

    // ---------------------- Entry -> Entity ----------------------

    /** Stable id so re-syncs upsert instead of duplicating. */
    fun localIdFor(mediaId: Int): String = "anilist-$mediaId"

    fun entryToEntity(
        entry: AniListListEntry,
        scoreFormat: String,
        categories: List<CategoryEntity>
    ): MediaItemEntity? {
        val media = entry.media ?: return null
        val mediaType = media.type ?: mediaTypeFromFormat(media.format)
        val title = media.title.display ?: return null
        val cover = media.coverImage?.large ?: media.coverImage?.medium ?: media.coverImage?.extraLarge
        val total = when {
            mediaType.equals("MANGA", true) -> media.chapters ?: media.volumes ?: 0
            else -> media.episodes ?: 0
        }
        return MediaItemEntity(
            id = localIdFor(media.id),
            title = title,
            categoryId = resolveCategoryId(mediaType, categories),
            status = statusFromAniList(entry.status, mediaType),
            currentProgress = entry.progress ?: 0,
            totalCount = total,
            currentVolume = entry.progressVolumes,
            rating = scoreToAppRating(entry.score ?: 0.0, scoreFormat),
            coverImageUri = cover,
            genres = media.genres ?: emptyList(),
            tags = emptyList(),
            lastUpdated = (entry.updatedAt?.toLong() ?: 0L) * 1000L,
            isFavorite = false,
            notes = entry.notes.orEmpty(),
            rewatchCount = entry.repeat ?: 0,
            startDate = FuzzyDate.toEpochMillis(entry.startedAt),
            endDate = FuzzyDate.toEpochMillis(entry.completedAt),
            source = MediaSource.ANILIST,
            anilistId = media.id,
            listEntryId = entry.id,
            mediaType = mediaType,
            pendingSync = false
        )
    }

    /** Flattens a collection into unique entities (custom lists overlap status lists). */
    fun collectionToEntities(
        collection: AniListMediaListCollection,
        scoreFormat: String,
        categories: List<CategoryEntity>
    ): List<MediaItemEntity> {
        val seen = LinkedHashSet<Int>()
        val entities = ArrayList<MediaItemEntity>()
        collection.lists.forEach { group ->
            group.entries.forEach { entry ->
                if (seen.add(entry.mediaId)) {
                    entryToEntity(entry, scoreFormat, categories)?.let(entities::add)
                }
            }
        }
        return entities
    }

    // ---------------------- Entity -> Save payload ----------------------

    fun entityToSavePayload(item: MediaItemEntity, scoreFormat: String): SaveEntryPayload {
        val mediaId = item.anilistId
            ?: throw IllegalArgumentException("Cannot save an AniList entry without anilistId")
        return SaveEntryPayload(
            mediaId = mediaId,
            status = statusToAniList(item.status),
            score = if (item.rating > 0.0) ratingToAniListScore(item.rating, scoreFormat) else null,
            progress = item.currentProgress,
            progressVolumes = item.currentVolume,
            notes = item.notes.ifBlank { null },
            repeat = item.rewatchCount,
            startedAt = item.startDate,
            completedAt = item.endDate
        )
    }

    // ---------------------- Media type helpers ----------------------

    fun mediaTypeFromFormat(format: String?): String =
        if (format.equals("MANGA", true)) "MANGA" else "ANIME"

    fun resolveCategoryId(mediaType: String?, categories: List<CategoryEntity>): String {
        val wanted = if (mediaType.equals("MANGA", true)) "Manga" else "Anime"
        return categories.firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.id
            ?: categories.firstOrNull()?.id
            ?: ""
    }
}
