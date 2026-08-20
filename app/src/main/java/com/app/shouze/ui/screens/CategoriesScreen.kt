package com.app.shouze.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.ui.components.rememberTooltipPositionProvider
import com.app.shouze.ui.components.AnimatedPageEntrance

private val CATEGORY_COLOR_PALETTE = listOf(
    "#E57373", // Red
    "#F06292", // Pink
    "#BA68C8", // Purple
    "#9575CD", // Deep Purple
    "#7986CB", // Indigo
    "#64B5F6", // Blue
    "#4FC3F7", // Light Blue
    "#4DD0E1", // Cyan
    "#4DB6AC", // Teal
    "#81C784", // Green
    "#AED581", // Light Green
    "#FFB74D", // Orange
    "#FF8A65"  // Deep Orange
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAddCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(CATEGORY_COLOR_PALETTE.first()) }
    var pendingDeleteCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Categories", 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        AnimatedPageEntrance(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Categories help you group and organize your library. Filter by categories anytime from your main library screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Create Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Category name") },
                            placeholder = { Text("e.g. Favorite Mecha, Cozy Slice of Life") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (newCategoryName.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val trimmed = newCategoryName.trim()
                                            if (trimmed.isNotBlank()) {
                                                onAddCategory(trimmed, selectedColorHex)
                                                newCategoryName = ""
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Add category",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Accent Color",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(CATEGORY_COLOR_PALETTE) { hex ->
                                val color = parseHexColor(hex)
                                val isSelected = hex == selectedColorHex
                                
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorHex = hex }
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 3.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val trimmed = newCategoryName.trim()
                                if (trimmed.isNotBlank()) {
                                    onAddCategory(trimmed, selectedColorHex)
                                    newCategoryName = ""
                                }
                            },
                            enabled = newCategoryName.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Category", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Your Categories (${categories.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                if (categories.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No categories created yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add custom categories above to group your entries.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column {
                            categories.forEachIndexed { index, cat ->
                                val color = cat.colorHex?.let { parseHexColor(it) } 
                                    ?: MaterialTheme.colorScheme.primary

                                CategoryRowItem(
                                    category = cat,
                                    badgeColor = color,
                                    onDeleteRequested = { pendingDeleteCategory = cat }
                                )

                                if (index < categories.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteCategory?.let { categoryToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete category?") },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${categoryToDelete.name}\"? Items assigned to this category will become uncategorized."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(categoryToDelete.id)
                        pendingDeleteCategory = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)  // <-- Added this annotation to fix experimental API warnings
@Composable
private fun CategoryRowItem(
    category: CategoryEntity,
    badgeColor: Color,
    onDeleteRequested: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(2.dp, badgeColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                }
            }
        },
        trailingContent = {
            TooltipBox(
                tooltip = { PlainTooltip { Text("Delete category") } },
                state = rememberTooltipState(),
                positionProvider = rememberTooltipPositionProvider(),
                focusable = false
            ) {
                IconButton(onClick = onDeleteRequested) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete ${category.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

private fun parseHexColor(hex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrElse {
        Color(0xFF808080)
    }
}
