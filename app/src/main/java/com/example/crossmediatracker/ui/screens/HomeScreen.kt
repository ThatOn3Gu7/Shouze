package com.example.crossmediatracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.crossmediatracker.data.local.MediaItemEntity
import com.example.crossmediatracker.ui.HomeUiState
import com.example.crossmediatracker.ui.components.MediaCardItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddClick: () -> Unit,
    onItemClick: (MediaItemEntity) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBackup: (Uri) -> Unit,
    onRestore: (Uri) -> Unit,
    onClearMessage: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var pendingRestoreUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let(onBackup) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> pendingRestoreUri = uri }

    fun launchBackup() {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        backupLauncher.launch("mediatracker-backup-$stamp.zip")
    }

    val isError = uiState.error != null
    val message = uiState.error ?: uiState.syncMessage
    val configuration = LocalConfiguration.current
    val isCompactWidth = configuration.screenWidthDp < 600

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Tracker") },
                actions = {
                    IconButton(onClick = ::launchBackup, enabled = !uiState.isLoading) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Backup")
                    }
                    IconButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = "Restore")
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
                text = { Text("Add Media") },
                modifier = if (isCompactWidth) Modifier.fillMaxWidth(0.9f) else Modifier.widthIn(min = 160.dp),
                expanded = true
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            val selectedIndex = uiState.categories.indexOfFirst { it.id == uiState.selectedCategoryId }
                .let { if (it == -1) 0 else it + 1 }

            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = uiState.selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    text = { Text("All") }
                )
                uiState.categories.forEach { category ->
                    Tab(
                        selected = uiState.selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        text = { Text(category.name) }
                    )
                }
                Tab(
                    selected = false,
                    onClick = { showAddCategory = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Add category") },
                    text = { Text("New") }
                )
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                    onAddClick = onAddClick,
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
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    if (pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            icon = { Icon(Icons.Filled.CloudDownload, contentDescription = null) },
            title = { Text("Restore backup?") },
            text = { Text("This will replace all current data with the backup's contents. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri?.let(onRestore)
                    pendingRestoreUri = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddCategory) {
        AddCategoryDialog(
            onDismiss = { showAddCategory = false },
            onConfirm = { name ->
                onAddCategory(name)
                showAddCategory = false
            }
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = isValid) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EmptyState(
    hasSearchOrFilter: Boolean,
    onAddClick: () -> Unit,
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
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add your first media")
            }
        }
    }
}
