package com.app.shouze.ui

import com.app.shouze.data.local.MediaItemEntity

data class StatsUiState(
    val totalEntries: Int = 0,
    val totalCompleted: Int = 0,
    val totalWatching: Int = 0,
    val totalDropped: Int = 0,
    val totalPlanToWatch: Int = 0,
    val totalReading: Int = 0,
    val completionRate: Float = 0f,
    val averageRating: Double = 0.0,
    val totalProgressConsumed: Int = 0,
    val genreDistribution: List<GenreStat> = emptyList(),
    val categoryDistribution: List<CategoryStat> = emptyList(),
    val topRatedItems: List<MediaItemEntity> = emptyList(),
    val recentlyUpdatedItems: List<MediaItemEntity> = emptyList()
)

data class GenreStat(
    val genre: String,
    val count: Int,
    val percentage: Float
)

data class CategoryStat(
    val categoryName: String,
    val count: Int,
    val colorHex: String?,
    val percentage: Float
)

