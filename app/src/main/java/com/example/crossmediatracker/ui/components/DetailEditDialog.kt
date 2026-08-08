package com.example.crossmediatracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.crossmediatracker.data.local.MediaItemEntity
import com.example.crossmediatracker.data.local.MediaType
import com.example.crossmediatracker.data.local.Status
import java.util.UUID

/**
 * Material 3 full‑screen dialog for adding/editing an item.
 * Supports all fields, progress increment, and deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailEditDialog(
    item: MediaItemEntity?,   // null = creating a new item
    onDismiss: () -> Unit,
    onSave: (MediaItemEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    // Editable state
    var title by remember { mutableStateOf(item?.title ?: "") }
    var mediaType by remember { mutableStateOf(item?.mediaType ?: MediaType.TV_SERIES) }
    var status by remember { mutableStateOf(item?.status ?: Status.PLAN_TO_WATCH) }
    var currentProgress by remember { mutableStateOf(item?.currentProgress?.toString() ?: "0") }
    var totalCount by remember { mutableStateOf(item?.totalCount?.toString() ?: "1") }
    var currentVolume by remember { mutableStateOf(item?.currentVolume?.toString() ?: "") }
    var rating by remember { mutableStateOf(item?.rating?.toString() ?: "0.0") }
    var coverImageUri by remember { mutableStateOf(item?.coverImageUri ?: "") }

    // Parsed values (0 total = ongoing / unknown length)
    val isTitleValid = title.isNotBlank()
    val progressInt = currentProgress.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val totalInt = totalCount.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val clampedProgress = if (totalInt > 0) progressInt.coerceIn(0, totalInt) else progressInt

    val unitLabel = if (mediaType == MediaType.NOVEL) "Chapter" else "Episode"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (item == null) "Add New Item" else "Edit Item",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    isError = !isTitleValid,
                    supportingText = if (isTitleValid) null else {
                        { Text("Title is required") }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Media type dropdown
                var expandedMedia by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedMedia,
                    onExpandedChange = { expandedMedia = it }
                ) {
                    OutlinedTextField(
                        value = mediaType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Media Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMedia) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expandedMedia, onDismissRequest = { expandedMedia = false }) {
                        MediaType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ")) },
                                onClick = {
                                    mediaType = type
                                    expandedMedia = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Status dropdown
                var expandedStatus by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedStatus,
                    onExpandedChange = { expandedStatus = it }
                ) {
                    OutlinedTextField(
                        value = status.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                        Status.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name.replace("_", " ")) },
                                onClick = {
                                    status = s
                                    expandedStatus = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentProgress,
                        onValueChange = { currentProgress = it },
                        label = { Text("Progress") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalCount,
                        onValueChange = { totalCount = it },
                        label = { Text("Total (0 = ongoing)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (mediaType == MediaType.NOVEL) {
                    OutlinedTextField(
                        value = currentVolume,
                        onValueChange = { currentVolume = it },
                        label = { Text("Current Volume (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = rating,
                    onValueChange = { rating = it },
                    label = { Text("Rating (0-10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = coverImageUri,
                    onValueChange = { coverImageUri = it },
                    label = { Text("Cover Image URI") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                if (item != null) {
                    // Quick progress increment — updates local state so Save persists it once
                    OutlinedButton(
                        onClick = {
                            val next = progressInt + 1
                            currentProgress = next.toString()
                            if (totalInt > 0 && next >= totalInt && status != Status.DROPPED) {
                                status = Status.COMPLETED
                            }
                        },
                        enabled = totalInt == 0 || progressInt < totalInt,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+1 $unitLabel")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (item != null) {
                        TextButton(onClick = {
                            onDelete(item.id)
                            onDismiss()
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newItem = MediaItemEntity(
                                id = item?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                mediaType = mediaType,
                                status = status,
                                currentProgress = clampedProgress,
                                totalCount = totalInt,
                                currentVolume = currentVolume.toIntOrNull(),
                                rating = rating.toDoubleOrNull()?.coerceIn(0.0, 10.0) ?: 0.0,
                                coverImageUri = coverImageUri.ifBlank { null },
                                lastUpdated = System.currentTimeMillis()
                            )
                            onSave(newItem)
                            onDismiss()
                        },
                        enabled = isTitleValid
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}