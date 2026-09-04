package com.app.shouze.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.LibraryEntryEntity
import com.app.shouze.data.local.MediaStatus
import com.app.shouze.data.local.MediaType

/**
 * The offline fallback: adds a local-only entry (never synced to AniList).
 * Titles from AniList should be added from Search / detail instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (LibraryEntryEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MediaType.ANIME) }
    var categoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var status by remember { mutableStateOf(MediaStatus.PLANNING) }
    var progress by remember { mutableStateOf("0") }
    var total by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Add manually",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Local-only entry — it won't sync to AniList.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == MediaType.ANIME,
                            onClick = { type = MediaType.ANIME },
                            label = { Text("Anime") }
                        )
                        FilterChip(
                            selected = type == MediaType.MANGA,
                            onClick = { type = MediaType.MANGA },
                            label = { Text("Manga") }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = categories.find { it.id == categoryId }?.name ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showCategoryPicker = true }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = status.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Status") },
                            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showStatusPicker = true }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = progress,
                            onValueChange = { progress = it },
                            label = { Text(if (type == MediaType.MANGA) "Chapters" else "Episodes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        )
                        OutlinedTextField(
                            value = total,
                            onValueChange = { total = it },
                            label = { Text("Total") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        )
                    }

                    OutlinedTextField(
                        value = score,
                        onValueChange = { score = it },
                        label = { Text("Score (0–10)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        minLines = 2,
                        maxLines = 4
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        TextButton(
                            onClick = {
                                val totalInt = total.toIntOrNull()
                                val entry = LibraryEntryEntity(
                                    mediaId = null,
                                    title = title.trim(),
                                    type = type,
                                    categoryId = categoryId.takeIf { it.isNotBlank() },
                                    status = status,
                                    progress = progress.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                                    totalEpisodes = if (type == MediaType.ANIME) totalInt else null,
                                    totalChapters = if (type == MediaType.MANGA) totalInt else null,
                                    score = (score.toDoubleOrNull()?.coerceIn(0.0, 10.0) ?: 0.0)
                                        .let { (it * 10).toInt() },
                                    notes = notes.trim()
                                )
                                onSave(entry)
                            },
                            enabled = title.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Select category") },
            text = {
                Column {
                    categories.forEach { cat ->
                        TextButton(
                            onClick = {
                                categoryId = cat.id
                                showCategoryPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(cat.name) }
                    }
                    if (categories.isEmpty()) {
                        Text(
                            text = "No categories yet — create one in Settings.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryPicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showStatusPicker) {
        AlertDialog(
            onDismissRequest = { showStatusPicker = false },
            title = { Text("Select status") },
            text = {
                Column {
                    MediaStatus.entries.forEach { s ->
                        TextButton(
                            onClick = {
                                status = s
                                showStatusPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(s.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatusPicker = false }) { Text("Cancel") }
            }
        )
    }
}
