package com.app.shouze.ui

import com.app.shouze.data.local.LibraryEntryEntity

data class StatsUiState(
    val totalEntries: Int = 0,
    val current: Int = 0,
    val planning: Int = 0,
    val completed: Int = 0,
    val dropped: Int = 0,
    val paused: Int = 0,
    val repeating: Int = 0,
    val favorites: Int = 0,
    val completionRate: Float = 0f,
    /** Mean score across scored entries, 0–100. */
    val meanScore: Double = 0.0,
    val totalProgress: Int = 0,
    val genreDistribution: List<GenreStat> = emptyList(),
    val formatDistribution: List<FormatStat> = emptyList(),
    val topRated: List<LibraryEntryEntity> = emptyList(),
    val recentlyUpdated: List<LibraryEntryEntity> = emptyList()
)

data class GenreStat(
    val genre: String,
    val count: Int,
    val percentage: Float
)

data class FormatStat(
    val format: String,
    val count: Int,
    val percentage: Float
)
