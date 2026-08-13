package com.app.shouze.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.ui.HomeUiState
import com.app.shouze.ui.SortMode
import com.app.shouze.ui.components.MediaCardItem
import com.app.shouze.ui.components.SafeRemoteImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddClick: () -> Unit,
    onItemClick: (MediaItemEntity) -> Unit,
    onEditItem: (MediaItemEntity) -> Unit,
    onDeleteItem: (MediaItemEntity) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearMessage: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onSortModeChange: (SortMode) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onSearchAniListClick: () -> Unit,
    showFavoritesOnly: Boolean = false
) {
    val isError = uiState.error != null
    val message = uiState.error ?: uiState.syncMessage

    var selectedItem by remember { mutableStateOf<MediaItemEntity?>(null) }

    Scaffold(
        bottomBar = {
            val sel = selectedItem
            if (sel != null) {
                BottomAppBar(
                    actions = {
                        IconButton(onClick = { onEditItem(sel); selectedItem = null }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { onToggleFavorite(sel); selectedItem = null }) {
                            Icon(
                                imageVector = if (sel.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = if (sel.isFavorite) "Unfavorite" else "Favorite",
                                tint = if (sel.isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                            )
                        }
                        IconButton(
                            onClick = { onDeleteItem(sel); selectedItem = null }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { selectedItem = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Shouze") },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onToggleFavorites) {
                                Icon(
                                    imageVector = if (showFavoritesOnly) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = if (showFavoritesOnly) "Show all" else "Show favorites",
                                    tint = if (showFavoritesOnly) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Last Updated") },
                                onClick = { onSortModeChange(SortMode.LAST_UPDATED); expanded = false },
                                trailingIcon = {
                                    if (uiState.sortMode == SortMode.LAST_UPDATED) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Title (A-Z)") },
                                onClick = { onSortModeChange(SortMode.TITLE); expanded = false },
                                trailingIcon = {
                                    if (uiState.sortMode == SortMode.TITLE) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rating (High-Low)") },
                                onClick = { onSortModeChange(SortMode.RATING_HIGH); expanded = false },
                                trailingIcon = {
                                    if (uiState.sortMode == SortMode.RATING_HIGH) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Progress (Most Complete)") },
                                onClick = { onSortModeChange(SortMode.PROGRESS); expanded = false },
                                trailingIcon = {
                                    if (uiState.sortMode == SortMode.PROGRESS) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                    IconButton(onClick = onSearchAniListClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search AniList")
                    }
                    IconButton(onClick = onStatisticsClick) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Statistics")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add item") },
                text = { Text("Add Media") }
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (uiState.upNextItems.isNotEmpty()) {
                UpNextSection(
                    items = uiState.upNextItems,
                    categories = uiState.categories,
                    onItemClick = onItemClick
                )
            }

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search your library...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("All") },
                        leadingIcon = if (uiState.selectedCategoryId == null) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
                items(uiState.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = uiState.selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = if (uiState.selectedCategoryId == category.id) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val snackbarColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface
                val snackbarContent = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface
                Snackbar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    containerColor = snackbarColor,
                    contentColor = snackbarContent,
                    action = {
                        TextButton(onClick = onClearMessage) {
                            Text("Dismiss", color = snackbarContent)
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message ?: "")
                    }
                }
            }

            if (uiState.items.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    hasSearchOrFilter = uiState.searchQuery.isNotBlank() || uiState.selectedCategoryId != null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = uiState.items,
                        key = { it.id }
                    ) { item ->
                        val categoryName = uiState.categories.find { it.id == item.categoryId }?.name ?: "Unknown"
                        MediaCardItem(
                            item = item,
                            categoryName = categoryName,
                            onClick = { onItemClick(item) },
                            onLongClick = { selectedItem = item },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpNextSection(
    items: List<MediaItemEntity>,
    categories: List<com.app.shouze.data.local.CategoryEntity>,
    onItemClick: (MediaItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Up Next",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val categoryName = categories.find { it.id == item.categoryId }?.name ?: "Unknown"
                UpNextCard(
                    item = item,
                    categoryName = categoryName,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun UpNextCard(
    item: MediaItemEntity,
    categoryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                if (!item.coverImageUri.isNullOrBlank()) {
                    SafeRemoteImage(
                        url = item.coverImageUri,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.title.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$categoryName · ${item.currentProgress}/${if (item.totalCount > 0) item.totalCount else "?"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    hasSearchOrFilter: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MovieFilter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasSearchOrFilter) "No results found" else "Your library is empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (hasSearchOrFilter) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try changing your search or filter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the '+' button to add your first entry",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
