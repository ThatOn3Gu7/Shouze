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
import androidx.compose.ui.graphics.Color
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
    var categoryId by remember(item?.id) {
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

    LaunchedEffect(item?.id, categories.size) {
        if (item == null && categoryId.isBlank() && categories.isNotEmpty()) {
            categoryId = categories.first().id
        }
    }

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
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categories.find { it.id == categoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showCategoryPicker = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = status.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showStatusPicker = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentProgress,
                        onValueChange = { currentProgress = it },
                        label = { Text("Progress") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = totalCount,
                        onValueChange = { totalCount = it },
                        label = { Text("Total") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isLiterature) {
                    OutlinedTextField(
                        value = currentVolume,
                        onValueChange = { currentVolume = it },
                        label = { Text("Current Volume (optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = rating,
                    onValueChange = { rating = it },
                    label = { Text("Rating (0-10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = coverImageUri,
                    onValueChange = { coverImageUri = it },
                    label = { Text("Cover Image URL") },
                    supportingText = { Text("Direct image link only (ends in .jpg, .png, .webp). Webpages won't work.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Genres",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (genres.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newGenre,
                            onValueChange = { newGenre = it },
                            placeholder = { Text("Add a genre...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        IconButton(
                            onClick = {
                                if (newGenre.isNotBlank() && newGenre !in genres) {
                                    genres = genres + newGenre.trim()
                                    newGenre = ""
                                }
                            },
                            enabled = newGenre.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add genre")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val finalItem = MediaItemEntity(
                                id = item?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                categoryId = categoryId,
                                status = status,
                                currentProgress = clampedProgress,
                                totalCount = totalInt,
                                currentVolume = currentVolume.toIntOrNull(),
                                rating = rating.toDoubleOrNull()?.coerceIn(0.0, 10.0) ?: 0.0,
                                coverImageUri = coverImageUri.trim().takeIf { it.isNotBlank() },
                                genres = genres,
                                lastUpdated = System.currentTimeMillis()
                            )
                            onSave(finalItem)
                        },
                        enabled = isTitleValid && categoryId.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }

                if (item != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onDelete(item.id) },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Item")
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
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryPicker = false }) {
                    Text("Cancel")
                }
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
                TextButton(onClick = { showStatusPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
