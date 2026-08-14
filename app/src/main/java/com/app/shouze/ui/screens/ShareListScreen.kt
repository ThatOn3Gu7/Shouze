package com.app.shouze.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
            TopAppBar(
                title = { Text("Share List") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Library Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${items.size} total entries",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${items.count { it.status.name == "COMPLETED" }} completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "My Shouze Library")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share List")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Import Shared List",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Paste a shared Shouze list below to import titles as Plan to Watch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Paste shared list here") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (importText.isNotBlank()) {
                                onImportSharedList(importText)
                                importText = ""
                            }
                        },
                        enabled = importText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Titles")
                    }
                }
            }

            if (shareText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Text(
                        text = shareText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
