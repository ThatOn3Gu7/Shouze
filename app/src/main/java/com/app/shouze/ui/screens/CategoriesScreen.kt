package com.app.shouze.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Categories help you organize your library. Use the search and filter chips on the home screen to show one at a time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    categories.forEach { cat ->
                        val color = cat.colorHex?.let { hex ->
                            runCatching {
                                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                            }.getOrNull()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (color != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete ${cat.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (categories.isEmpty()) {
                        Text(
                            text = "No categories yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("New category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val trimmed = newCategoryName.trim()
                                if (trimmed.isNotBlank()) {
                                    onAddCategory(trimmed)
                                    newCategoryName = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add category")
                        }
                    }
                }
            }
        }
    }
}
