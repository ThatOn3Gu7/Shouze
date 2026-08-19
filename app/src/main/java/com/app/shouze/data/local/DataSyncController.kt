package com.app.shouze.data.local

import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class MediaItemExport(
    val id: String,
    val title: String,
    val categoryId: String? = null,
    val mediaType: String? = null,
    val status: String,
    val currentProgress: Int,
    val totalCount: Int,
    val currentVolume: Int?,
    val rating: Double,
    val coverImageUri: String?,
    val genres: List<String> = emptyList(),
    val lastUpdated: Long,
    val isFavorite: Boolean = false,
    val notes: String = "",
    val rewatchCount: Int = 0,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class CategoryExport(
    val id: String,
    val name: String,
    val colorHex: String? = null,
    val createdAt: Long
)

@Serializable
data class BackupPayload(
    val version: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0,
    val items: List<MediaItemExport> = emptyList(),
    val categories: List<CategoryExport> = emptyList()
)

class DataSyncController(private val db: AppDatabase) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun exportToJson(): Result<String> {
        return try {
            val snapshot = db.mediaDao().getAllItemsSnapshot()
            val categorySnapshot = db.categoryDao().getAllSnapshot()
            val exports = snapshot.map { it.toExport() }
            val catExports = categorySnapshot.map { it.toExport() }
            val payload = BackupPayload(
                itemCount = exports.size,
                items = exports,
                categories = catExports
            )
            Result.success(json.encodeToString(payload))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromJson(jsonString: String): Result<Int> {
        return try {
            val payload = json.decodeFromString<BackupPayload>(jsonString)
            if (payload.version != 1 && payload.version != 2 && payload.version != 3) {
                return Result.failure(IllegalArgumentException("Unsupported backup version ${payload.version}"))
            }
            if (payload.items.isEmpty()) {
                return Result.failure(IllegalArgumentException("Backup contains no media items"))
            }
            db.withTransaction {
                db.mediaDao().clearAll()
                db.categoryDao().clearAll()

                payload.categories.forEach { export ->
                    db.categoryDao().insertOrUpdate(export.toEntity())
                }
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
    categoryId = categoryId,
    status = status.name,
    currentProgress = currentProgress,
    totalCount = totalCount,
    currentVolume = currentVolume,
    rating = rating,
    coverImageUri = coverImageUri,
    genres = genres,
    tags = tags,
    lastUpdated = lastUpdated,
    isFavorite = isFavorite,
    notes = notes,
    rewatchCount = rewatchCount,
    startDate = startDate,
    endDate = endDate
)

private fun MediaItemExport.toEntity(): MediaItemEntity {
    val statusEnum = try {
        Status.valueOf(status)
    } catch (_: IllegalArgumentException) {
        Status.PLAN_TO_WATCH
    }
    val catId = categoryId ?: mediaType ?: "TV_SERIES"
    return MediaItemEntity(
        id = id,
        title = title,
        categoryId = catId,
        status = statusEnum,
        currentProgress = currentProgress,
        totalCount = totalCount,
        currentVolume = currentVolume,
        rating = rating,
        coverImageUri = coverImageUri,
        genres = genres,
        tags = tags,
        lastUpdated = lastUpdated,
        isFavorite = isFavorite,
        notes = notes,
        rewatchCount = rewatchCount,
        startDate = startDate,
        endDate = endDate
    )
}

private fun CategoryEntity.toExport() = CategoryExport(
    id = id,
    name = name,
    colorHex = colorHex,
    createdAt = createdAt
)

private fun CategoryExport.toEntity() = CategoryEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    createdAt = createdAt
)
