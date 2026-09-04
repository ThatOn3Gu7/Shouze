package com.app.shouze.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.LibraryEntryEntity
import com.app.shouze.data.local.MediaStatus
import com.app.shouze.data.local.MediaType
import com.app.shouze.ui.LibrarySort
import com.app.shouze.ui.LibraryUiState
import com.app.shouze.ui.components.MediaCardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onStatusFilter: (MediaStatus?) -> Unit,
    onTypeFilter: (MediaType?) -> Unit,
    onCategoryFilter: (String?) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onSort: (LibrarySort) -> Unit,
    onSync: () -> Unit,
    onOpenEntry: (LibraryEntryEntity) -> Unit,
    onToggleFavorite: (LibraryEntryEntity) -> Unit,
    onAddManual: () -> Unit,
    onManageCategories: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddFromSearch: () -> Unit,
    onClearMessage: () -> Unit,
    onClearFilters: () -> Unit
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    var moreMenuOpen by remember { mutableStateOf(false) }
    val hasActiveFilter = uiState.statusFilter != null || uiState.typeFilter != null ||
        uiState.query.isNotBlank() || uiState.favoritesOnly || uiState.categoryFilter != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                actions = {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp).size(22.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onSync) {
                            Icon(Icons.Rounded.Sync, contentDescription = "Sync with AniList", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    IconButton(onClick = onToggleFavorites) {
                        Icon(
                            imageVector = if (uiState.favoritesOnly) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "Favorites only",
                            tint = if (uiState.favoritesOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            LibrarySort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sortLabel(sort), fontWeight = FontWeight.Medium) },
                                    trailingIcon = {
                                        if (uiState.sort == sort) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    onClick = { onSort(sort); sortMenuOpen = false }
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { moreMenuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = moreMenuOpen, onDismissRequest = { moreMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Add manually") },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                onClick = { moreMenuOpen = false; onAddManual() }
                            )
                            DropdownMenuItem(
                                text = { Text("Manage categories") },
                                leadingIcon = { Icon(Icons.Rounded.VideoLibrary, contentDescription = null) },
                                onClick = { moreMenuOpen = false; onManageCategories() }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                onClick = { moreMenuOpen = false; onOpenSettings() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFromSearch,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Search, contentDescription = "Add from AniList")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item(key = "search") {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search your library…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )
                }

                item(key = "status_chips") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.statusFilter == null,
                                onClick = { onStatusFilter(null) },
                                label = { Text("All") }
                            )
                        }
                        items(MediaStatus.entries, key = { it.name }) { status ->
                            FilterChip(
                                selected = uiState.statusFilter == status,
                                onClick = { onStatusFilter(if (uiState.statusFilter == status) null else status) },
                                label = { Text(statusFilterLabel(status)) }
                            )
                        }
                    }
                }

                item(key = "type_chips") {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.typeFilter == null,
                            onClick = { onTypeFilter(null) },
                            label = { Text("Anime + Manga") }
                        )
                        FilterChip(
                            selected = uiState.typeFilter == MediaType.ANIME,
                            onClick = { onTypeFilter(if (uiState.typeFilter == MediaType.ANIME) null else MediaType.ANIME) },
                            label = { Text("Anime") }
                        )
                        FilterChip(
                            selected = uiState.typeFilter == MediaType.MANGA,
                            onClick = { onTypeFilter(if (uiState.typeFilter == MediaType.MANGA) null else MediaType.MANGA) },
                            label = { Text("Manga") }
                        )
                    }
                }

                if (uiState.categories.isNotEmpty()) {
                    item(key = "category_chips") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.categoryFilter == null,
                                    onClick = { onCategoryFilter(null) },
                                    label = { Text("All categories") }
                                )
                            }
                            items(uiState.categories, key = { it.id }) { cat ->
                                FilterChip(
                                    selected = uiState.categoryFilter == cat.id,
                                    onClick = { onCategoryFilter(if (uiState.categoryFilter == cat.id) null else cat.id) },
                                    label = { Text(cat.name) }
                                )
                            }
                        }
                    }
                }

                if (uiState.filtered.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.VideoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (hasActiveFilter) "Nothing matches those filters" else "Your library is empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (hasActiveFilter) "Try clearing a filter or two." else "Sync your AniList or search to add titles.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (hasActiveFilter) {
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(onClick = onClearFilters) { Text("Clear filters") }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.localId }) { entry ->
                        MediaCardItem(
                            entry = entry,
                            onClick = { onOpenEntry(entry) },
                            onToggleFavorite = { onToggleFavorite(entry) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.message != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(16.dp),
                    action = {
                        TextButton(onClick = onClearMessage) { Text("Dismiss", fontWeight = FontWeight.Bold) }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (uiState.message?.startsWith("Couldn") == true || uiState.message?.contains("expired") == true) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.message ?: "", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun statusFilterLabel(status: MediaStatus): String = when (status) {
    MediaStatus.CURRENT -> "Current"
    MediaStatus.PLANNING -> "Planning"
    MediaStatus.COMPLETED -> "Completed"
    MediaStatus.DROPPED -> "Dropped"
    MediaStatus.PAUSED -> "Paused"
    MediaStatus.REPEATING -> "Repeating"
}

private fun sortLabel(sort: LibrarySort): String = when (sort) {
    LibrarySort.LAST_UPDATED -> "Last updated"
    LibrarySort.TITLE -> "Title (A–Z)"
    LibrarySort.SCORE -> "Score"
    LibrarySort.PROGRESS -> "Progress"
}
