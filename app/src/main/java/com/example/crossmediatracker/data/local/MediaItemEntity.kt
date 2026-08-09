package com.example.crossmediatracker.data.local

import androidx.room.*
import java.util.UUID

enum class Status { WATCHING, READING, COMPLETED, DROPPED, PLAN_TO_WATCH }

@Entity(tableName = "media_items")
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
    val lastUpdated: Long = System.currentTimeMillis()
)
