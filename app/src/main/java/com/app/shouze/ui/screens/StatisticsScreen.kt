package com.app.shouze.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.ui.StatsUiState
import com.app.shouze.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    stats: StatsUiState,
    onBack: () -> Unit,
    onItemClick: (MediaItemEntity) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OverviewSection(stats = stats)
            }

            item {
                StatsSectionCard(title = "Status Distribution") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusDonutChart(
                            completed = stats.totalCompleted,
                            watching = stats.totalWatching,
                            reading = stats.totalReading,
                            dropped = stats.totalDropped,
                            planToWatch = stats.totalPlanToWatch,
                            modifier = Modifier.size(160.dp)
                        )
                        StatusLegend(
                            completed = stats.totalCompleted,
                            watching = stats.totalWatching,
                            reading = stats.totalReading,
                            dropped = stats.totalDropped,
                            planToWatch = stats.totalPlanToWatch,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            if (stats.genreDistribution.isNotEmpty()) {
                item {
                    StatsSectionCard(title = "Top Genres") {
                        val genreItems = stats.genreDistribution.map { genreStat ->
                            ChartItem(
                                label = genreStat.genre,
                                count = genreStat.count,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalBarChart(items = genreItems)
                    }
                }
            }

            if (stats.categoryDistribution.isNotEmpty()) {
                item {
                    StatsSectionCard(title = "By Category") {
                        val categoryItems = stats.categoryDistribution.map { catStat ->
                            val parsedColor = catStat.colorHex?.let { hex ->
                                runCatching {
                                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                                }.getOrNull()
                            }
                            val color = parsedColor ?: MaterialTheme.colorScheme.secondary
                            ChartItem(
                                label = catStat.categoryName,
                                count = catStat.count,
                                color = color
                            )
                        }
                        HorizontalBarChart(items = categoryItems)
                    }
                }
            }

            if (stats.topRatedItems.isNotEmpty()) {
                item {
                    StatsSectionCard(title = "Top Rated") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(stats.topRatedItems) { item ->
                                MiniMediaCard(
                                    item = item,
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }

            if (stats.recentlyUpdatedItems.isNotEmpty()) {
                item {
                    StatsSectionCard(title = "Recently Active") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(stats.recentlyUpdatedItems) { item ->
                                MiniMediaCard(
                                    item = item,
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewSection(stats: StatsUiState) {
    Column {
        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = "${stats.totalEntries}",
                label = "Entries",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "%.1f".format(stats.averageRating),
                label = "Avg Rating",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = "${(stats.completionRate * 100).toInt()}%",
                label = "Completed",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${stats.totalProgressConsumed}",
                label = "Consumed",
                caption = "Episodes & chapters",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = "${stats.totalFavorites}",
                label = "Favorites",
                modifier = Modifier.weight(1f)
            )
            // Empty spacer to keep the row balanced (2 columns)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun MiniMediaCard(
    item: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(item.rating),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.currentProgress}/${if (item.totalCount > 0) item.totalCount else "?"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
