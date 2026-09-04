package com.app.shouze.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.LibraryEntryEntity
import com.app.shouze.data.local.MediaStatus
import com.app.shouze.ui.StatsUiState
import com.app.shouze.ui.components.ChartItem
import com.app.shouze.ui.components.HorizontalBarChart
import com.app.shouze.ui.components.SafeRemoteImage
import com.app.shouze.ui.components.StatCard
import com.app.shouze.ui.components.containerColor
import com.app.shouze.ui.components.scoreColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    stats: StatsUiState,
    onBack: () -> Unit,
    onItemClick: (LibraryEntryEntity) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Your Statistics", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { OverviewGrid(stats = stats) }

            if (stats.totalEntries > 0) {
                item {
                    SectionCard(title = "Status distribution", icon = Icons.Rounded.BarChart) {
                        HorizontalBarChart(items = statusItems(stats))
                    }
                }
            }

            if (stats.genreDistribution.isNotEmpty()) {
                item {
                    SectionCard(title = "Top genres", icon = Icons.Rounded.Category) {
                        HorizontalBarChart(
                            items = stats.genreDistribution.map { g ->
                                ChartItem(g.genre, g.count, MaterialTheme.colorScheme.primary)
                            }
                        )
                    }
                }
            }

            if (stats.formatDistribution.isNotEmpty()) {
                item {
                    SectionCard(title = "By format", icon = Icons.Rounded.CheckCircle) {
                        HorizontalBarChart(
                            items = stats.formatDistribution.map { f ->
                                ChartItem(f.format, f.count, MaterialTheme.colorScheme.tertiary)
                            }
                        )
                    }
                }
            }

            if (stats.topRated.isNotEmpty()) {
                item {
                    SectionCard(
                        title = "Top rated",
                        icon = Icons.Rounded.WorkspacePremium,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(stats.topRated, key = { it.localId }) { entry ->
                                MiniPoster(entry = entry, onClick = { onItemClick(entry) })
                            }
                        }
                    }
                }
            }

            if (stats.recentlyUpdated.isNotEmpty()) {
                item {
                    SectionCard(
                        title = "Recently active",
                        icon = Icons.Rounded.History,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(stats.recentlyUpdated, key = { it.localId }) { entry ->
                                MiniPoster(entry = entry, onClick = { onItemClick(entry) })
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun OverviewGrid(stats: StatsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                value = "${stats.totalEntries}",
                label = "Entries",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = if (stats.meanScore > 0) "%.1f".format(stats.meanScore / 10.0) else "—",
                label = "Mean score",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                value = "${(stats.completionRate * 100).toInt()}%",
                label = "Completed",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${stats.favorites}",
                label = "Favorites",
                modifier = Modifier.weight(1f)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${stats.totalProgress}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Episodes & chapters consumed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

@Composable
private fun statusItems(stats: StatsUiState): List<ChartItem> {
    val entries = listOf(
        MediaStatus.CURRENT to stats.current,
        MediaStatus.PLANNING to stats.planning,
        MediaStatus.COMPLETED to stats.completed,
        MediaStatus.DROPPED to stats.dropped,
        MediaStatus.PAUSED to stats.paused,
        MediaStatus.REPEATING to stats.repeating
    )
    return entries.filter { it.second > 0 }.map { (status, count) ->
        ChartItem(
            label = statusChartLabel(status),
            count = count,
            color = status.containerColor()
        )
    }
}

private fun statusChartLabel(status: MediaStatus): String = when (status) {
    MediaStatus.CURRENT -> "In progress"
    MediaStatus.PLANNING -> "Planned"
    MediaStatus.COMPLETED -> "Completed"
    MediaStatus.DROPPED -> "Dropped"
    MediaStatus.PAUSED -> "Paused"
    MediaStatus.REPEATING -> "Repeating"
}

@Composable
private fun MiniPoster(
    entry: LibraryEntryEntity,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.width(120.dp)) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!entry.coverImageUrl.isNullOrBlank()) {
                    SafeRemoteImage(
                        url = entry.coverImageUrl,
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.title.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (entry.score > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 10.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = scoreColor(entry.score),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "%.1f".format(entry.score / 10.0),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
