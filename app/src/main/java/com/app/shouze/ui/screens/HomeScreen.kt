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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.ui.HomeUiState
import com.app.shouze.ui.components.MediaCardItem

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
    onStatisticsClick: () -> Unit
) {
    val isError = uiState.error != null
    val message = uiState.error ?: uiState.syncMessage

    var selectedItem by remember { mutableStateOf<MediaItemEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && selectedItem != null) {
        val target = selectedItem!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${target.title}\"?") },
            text = { Text("This entry will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteItem(target)
                    selectedItem = null
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            val selection = selectedItem
            if (selection != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            val item = selection
                            selectedItem = null
                            onEditItem(item)
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Edit")
                        }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Delete")
                        }
                        TextButton(onClick = { selectedItem = null }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Shouze") },
                actions = {
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
                            onClick = {
                                if (selectedItem != null) {
                                    selectedItem = if (selectedItem?.id == item.id) null else item
                                } else {
                                    onItemClick(item)
                                }
                            },
                            onLongClick = { selectedItem = item },
                            selected = selectedItem?.id == item.id,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
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
