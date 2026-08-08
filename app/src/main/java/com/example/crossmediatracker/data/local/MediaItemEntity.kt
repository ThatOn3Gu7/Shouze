package com.example.crossmediatracker.data.local

import androidx.room.*
import java.util.UUID

/**
 * Types of media the app tracks.
 */
enum class MediaType { TV_SERIES, ANIME, NOVEL }

/**
 * User tracking status for a media item.
 */
enum class Status { WATCHING, READING, COMPLETED, DROPPED, PLAN_TO_WATCH }

/**
 * Room entity representing a single cross‑media item.
 * Primary key is a UUID string for portability during backups.
 */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val mediaType: MediaType,
    val status: Status,
    val currentProgress: Int,          // episodes watched / pages read
    val totalCount: Int,               // total episodes / total pages
    val currentVolume: Int? = null,    // optional volume for literature
    val rating: Double = 0.0,          // 0.0 – 10.0
    val coverImageUri: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)