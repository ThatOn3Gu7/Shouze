package com.app.shouze.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.MediaItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareListScreen(
    items: List<MediaItemEntity>,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onImportSharedList: (String) -> Unit
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }

    val shareText = remember(items, categories) {
        buildString {
            appendLine("📚 My Shouze Library")
            appendLine("====================")
            appendLine()

            val grouped = items.groupBy { it.categoryId }
            categories.forEach { cat ->
                val catItems = grouped[cat.id] ?: return@forEach
                if (catItems.isEmpty()) return@forEach
                appendLine("${cat.name} (${catItems.size})")
                appendLine("-".repeat(cat.name.length + 10))
                catItems.forEach { item ->
                    val status = item.status.name.replace("_", " ")
                    val progress = if (item.totalCount > 0) "${item.currentProgress}/${item.totalCount}" else "${item.currentProgress}/?"
                    val rating = if (item.rating > 0) " ★${item.rating}" else ""
                    appendLine("• ${item.title} [$status] $progress$rating")
                }
                appendLine()
            }

            appendLine("Total: ${items.size} items")
            appendLine("Shared via Shouze")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Share List",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Card 1: Library Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    // You can also try surfaceContainer here if surfaceVariant is still too bright
                    containerColor = MaterialTheme.colorScheme.surfaceVariant 
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Library Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${items.size} total entries",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${items.count { it.status.name == "COMPLETED" }} completed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "My Shouze Library")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share, 
                            contentDescription = null, 
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Share List",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Card 2: Import Shared List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Import Shared List",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Paste a shared Shouze list below to import titles as Plan to Watch.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Paste shared list here") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (importText.isNotBlank()) {
                                onImportSharedList(importText)
                                importText = ""
                            }
                        },
                        enabled = importText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = "Import Titles",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Card 3: Preview Box (Only visible if library isn't completely empty)
            if (shareText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        // Slightly darker/different container for contrast against the main cards
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = shareText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Add a little bottom padding for scrolling
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
