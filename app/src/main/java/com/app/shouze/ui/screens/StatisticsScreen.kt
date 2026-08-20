package com.app.shouze.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.ui.StatsUiState
import com.app.shouze.ui.components.*
import com.app.shouze.ui.components.BentoStaggeredEntrance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    stats: StatsUiState,
    onBack: () -> Unit,
    onItemClick: (MediaItemEntity) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        text = "Your Statistics",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // 1. The Bento Box Overview
            item {
                BentoOverviewGrid(stats = stats)
            }

            // 2. Status Distribution (Donut Chart)
            item {
                PremiumSection(
                    title = "Status Distribution",
                    icon = Icons.Rounded.PieChart
                ) {
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
                            modifier = Modifier.size(150.dp)
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

            // 3. Genres (Bar Chart)
            if (stats.genreDistribution.isNotEmpty()) {
                item {
                    PremiumSection(
                        title = "Top Genres",
                        icon = Icons.Rounded.BarChart
                    ) {
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

            // 4. Categories (Bar Chart)
            if (stats.categoryDistribution.isNotEmpty()) {
                item {
                    PremiumSection(
                        title = "By Category",
                        icon = Icons.Rounded.Category
                    ) {
                        val categoryItems = stats.categoryDistribution.map { catStat ->
                            val parsedColor = catStat.colorHex?.let { hex ->
                                runCatching {
                                    Color(android.graphics.Color.parseColor(hex))
                                }.getOrNull()
                            }
                            val color = parsedColor ?: MaterialTheme.colorScheme.tertiary
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

            // 5. Top Rated Items (LazyRow)
            if (stats.topRatedItems.isNotEmpty()) {
                item {
                    PremiumSection(
                        title = "Top Rated",
                        icon = Icons.Rounded.WorkspacePremium,
                        contentPadding = PaddingValues(horizontal = 0.dp) // Edge-to-edge scroll
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(stats.topRatedItems) { item ->
                                PremiumMiniMediaCard(
                                    item = item,
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Recently Active (LazyRow)
            if (stats.recentlyUpdatedItems.isNotEmpty()) {
                item {
                    PremiumSection(
                        title = "Recently Active",
                        icon = Icons.Rounded.History,
                        contentPadding = PaddingValues(horizontal = 0.dp) // Edge-to-edge scroll
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(stats.recentlyUpdatedItems) { item ->
                                PremiumMiniMediaCard(
                                    item = item,
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp)) // Bottom padding
            }
        }
    }
}

/**
 * A beautiful, staggered "Bento Box" style grid for the top-level stats.
 * Looks much more premium than a stack of identical uniform cards.
 */
@Composable
private fun BentoOverviewGrid(stats: StatsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Large Entries Box + Stacked Smaller Boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Big Hero Stat (Entries)
            BentoStaggeredEntrance(index = 0) {
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Rounded.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column {
                            Text(
                                text = "${stats.totalEntries}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total Entries",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Right side stacked stats
            Column(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avg Rating
                BentoStaggeredEntrance(index = 1) {
                    BentoCard(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "%.1f".format(stats.averageRating),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Avg Rating",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Completion Rate
                BentoStaggeredEntrance(index = 2) {
                    BentoCard(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${(stats.completionRate * 100).toInt()}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Row 2: Bottom Wide & Square Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Favorites (Square)
            BentoStaggeredEntrance(index = 3) {
                BentoCard(
                    modifier = Modifier
                        .weight(0.4f)
                        .aspectRatio(1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stats.totalFavorites}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Total Consumed (Wide)
            BentoStaggeredEntrance(index = 4) {
                BentoCard(
                    modifier = Modifier
                        .weight(0.6f)
                        .aspectRatio(1.5f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(28.dp))
                        Column {
                            Text(
                                text = "${stats.totalProgressConsumed}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Episodes & Chapters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A sleek section wrapper with an icon and proper M3 typography spacing.
 */
@Composable
private fun PremiumSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

/**
 * Base card for the Bento Grid. Enforces heavy rounded corners and proper padding.
 */
@Composable
private fun BentoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp), // Extra rounded for the premium bento look
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * Overhauled MiniMediaCard.
 * Mimics a high-end poster ratio with an integrated gradient and floating rating pill.
 */
@Composable
private fun PremiumMiniMediaCard(
    item: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(130.dp)
            .aspectRatio(2f / 3f) // Standard poster aspect ratio
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Background visual (Imagine an AsyncImage here in the future!)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            // Top-right Rating Pill
            if (item.rating > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "%.1f".format(item.rating),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Text Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Progress indicator bar (Visual representation instead of just numbers)
                val progressFrac = if (item.totalCount > 0) {
                    (item.currentProgress.toFloat() / item.totalCount.toFloat()).coerceIn(0f, 1f)
                } else 0f
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { progressFrac },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.currentProgress}/${if (item.totalCount > 0) item.totalCount else "?"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
