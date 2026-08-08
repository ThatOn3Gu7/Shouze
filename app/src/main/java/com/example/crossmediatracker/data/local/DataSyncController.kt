package com.example.crossmediatracker.data.local

import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class MediaItemExport(
    val id: String,
    val title: String,
    val mediaType: String,
    val status: String,
    val currentProgress: Int,
    val totalCount: Int,
    val currentVolume: Int?,
    val rating: Double,
    val coverImageUri: String?,
    val lastUpdated: Long
)

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0,
    val items: List<MediaItemExport> = emptyList()
)

class DataSyncController(private val db: AppDatabase) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * Serializes the whole library to a single JSON string.
     * The payload is wrapped in a [BackupPayload] with metadata so that
     * the contents of a backup can be inspected before restoring.
     */
    suspend fun exportToJson(): Result<String> {
        return try {
            val snapshot = db.mediaDao().getAllItemsSnapshot()
            val exports = snapshot.map { it.toExport() }
            val payload = BackupPayload(
                itemCount = exports.size,
                items = exports
            )
            Result.success(json.encodeToString(payload))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Replaces the entire library with the contents of a backup.
     * Returns the number of items restored, or a failure if the file is corrupt.
     */
    suspend fun importFromJson(jsonString: String): Result<Int> {
        return try {
            val payload = json.decodeFromString<BackupPayload>(jsonString)
            if (payload.version != 1) {
                return Result.failure(IllegalArgumentException("Unsupported backup version ${payload.version}"))
            }
            if (payload.items.isEmpty()) {
                return Result.failure(IllegalArgumentException("Backup contains no media items"))
            }
            // Transaction to ensure data consistency — either fully restored or fully rolled back.
            db.withTransaction {
                db.mediaDao().clearAll()
                payload.items.forEach { export ->
                    db.mediaDao().insertOrUpdate(export.toEntity())
                }
            }
            Result.success(payload.items.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun MediaItemEntity.toExport() = MediaItemExport(
    id = id,
    title = title,
    mediaType = mediaType.name,
    status = status.name,
    currentProgress = currentProgress,
    totalCount = totalCount,
    currentVolume = currentVolume,
    rating = rating,
    coverImageUri = coverImageUri,
    lastUpdated = lastUpdated
)

private fun MediaItemExport.toEntity(): MediaItemEntity {
    val mediaType = try {
        MediaType.valueOf(mediaType)
    } catch (_: IllegalArgumentException) {
        MediaType.TV_SERIES
    }
    val status = try {
        Status.valueOf(status)
    } catch (_: IllegalArgumentException) {
        Status.PLAN_TO_WATCH
    }
    return MediaItemEntity(
        id = id,
        title = title,
        mediaType = mediaType,
        status = status,
        currentProgress = currentProgress,
        totalCount = totalCount,
        currentVolume = currentVolume,
        rating = rating,
        coverImageUri = coverImageUri,
        lastUpdated = lastUpdated
    )
}