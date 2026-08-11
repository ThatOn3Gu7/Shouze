package com.app.shouze.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.data.local.Status
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailEditDialog(
    item: MediaItemEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (MediaItemEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember { mutableStateOf(item?.title ?: "") }
    var categoryId by remember {
        mutableStateOf(item?.categoryId ?: categories.firstOrNull()?.id ?: "")
    }
    var status by remember { mutableStateOf(item?.status ?: Status.PLAN_TO_WATCH) }
    var currentProgress by remember { mutableStateOf(item?.currentProgress?.toString() ?: "0") }
    var totalCount by remember { mutableStateOf(item?.totalCount?.toString() ?: "1") }
    var currentVolume by remember { mutableStateOf(item?.currentVolume?.toString() ?: "") }
    var rating by remember { mutableStateOf(item?.rating?.toString() ?: "0.0") }
    var coverImageUri by remember { mutableStateOf(item?.coverImageUri ?: "") }
    var genres by remember { mutableStateOf(item?.genres ?: emptyList()) }
    var newGenre by remember { mutableStateOf("") }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }

    val isTitleValid = title.isNotBlank()
    val progressInt = currentProgress.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val totalInt = totalCount.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val clampedProgress = if (totalInt > 0) progressInt.coerceIn(0, totalInt) else progressInt

    val selectedCategory = categories.find { it.id == categoryId }
    val isLiterature = selectedCategory?.name?.contains("novel", ignoreCase = true) == true
            || selectedCategory?.name?.contains("book", ignoreCase = true) == true
            || selectedCategory?.name?.contains("manga", ignoreCase = true) == true
    val unitLabel = if (isLiterature) "Chapter" else "Episode"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 24.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
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
                    supportingText = if (isTitleValid) null else { { Text("Title is required") } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categories.find { it.id == categoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showCategoryPicker = true }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = status.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showStatusPicker = true }
                    )
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

                if (isLiterature) {
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
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Genres",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (genres.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        genres.forEach { genre ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(genre) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { genres = genres - genre },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove $genre",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newGenre,
                        onValueChange = { newGenre = it },
                        label = { Text("Add genre") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val trimmed = newGenre.trim()
                            if (trimmed.isNotBlank() && !genres.any { it.equals(trimmed, ignoreCase = true) }) {
                                genres = genres + trimmed
                                newGenre = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add genre")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                if (item != null) {
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
                                categoryId = categoryId,
                                status = status,
                                currentProgress = clampedProgress,
                                totalCount = totalInt,
                                currentVolume = currentVolume.toIntOrNull(),
                                rating = rating.toDoubleOrNull()?.coerceIn(0.0, 10.0) ?: 0.0,
                                coverImageUri = coverImageUri.ifBlank { null },
                                genres = genres,
                                lastUpdated = System.currentTimeMillis()
                            )
                            onSave(newItem)
                            onDismiss()
                        },
                        enabled = isTitleValid && categoryId.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Select Category") },
            text = {
                Column {
                    if (categories.isEmpty()) {
                        Text(
                            text = "No categories available. Create one in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categories.forEach { cat ->
                            TextButton(
                                onClick = {
                                    categoryId = cat.id
                                    showCategoryPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(cat.name)
                            }
                        }
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
            title = { Text("Select Status") },
            text = {
                Column {
                    Status.entries.forEach { s ->
                        TextButton(
                            onClick = {
                                status = s
                                showStatusPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(s.name.replace("_", " "))
                        }
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
