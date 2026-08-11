package com.app.shouze.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.data.local.Status
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    item: MediaItemEntity,
    category: CategoryEntity?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val hue = (item.title.hashCode() and 0x7fffffff) % 360
            val avatarColor = Color(
                android.graphics.Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.52f, 0.88f))
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.title.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text(category?.name ?: "Unknown") })
                AssistChip(
                    onClick = { },
                    label = { Text(statusLabel(item.status)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusContainerColor(item.status),
                        labelColor = statusContentColor(item.status)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (item.genres.isNotEmpty()) {
                DetailInfoCard(title = "Genres") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item.genres.forEach { genre ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(genre) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            DetailInfoCard(title = "Progress") {
                val progressText = if (item.totalCount > 0) {
                    "${item.currentProgress} / ${item.totalCount}"
                } else {
                    "${item.currentProgress} / ongoing"
                }
                Text(text = progressText, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (item.totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { (item.currentProgress.toFloat() / item.totalCount).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (item.currentVolume != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current Volume: ${item.currentVolume}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (item.rating > 0.0) {
                DetailInfoCard(title = "Rating") {
                    LargeRatingBar(rating = item.rating)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            DetailInfoCard(title = "Info") {
                InfoRow(label = "Last Updated", value = dateFormat.format(Date(item.lastUpdated)))
                if (!item.coverImageUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow(label = "Cover URI", value = item.coverImageUri)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete item?") },
            text = { Text("This will permanently remove \"${item.title}\" from your library.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
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
}

@Composable
private fun DetailInfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LargeRatingBar(rating: Double, modifier: Modifier = Modifier) {
    val scale = (rating / 2.0).coerceIn(0.0, 5.0)
    val fullStars = scale.toInt()
    val hasHalf = scale - fullStars >= 0.5
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            val icon: ImageVector = when {
                index < fullStars -> Icons.Filled.Star
                index == fullStars && hasHalf -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "%.1f".format(rating),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun statusLabel(status: Status): String = when (status) {
    Status.COMPLETED -> "Completed"
    Status.DROPPED -> "Dropped"
    Status.PLAN_TO_WATCH -> "Plan to Watch"
    Status.WATCHING -> "Watching"
    Status.READING -> "Reading"
}

@Composable
private fun statusContainerColor(status: Status): Color = when (status) {
    Status.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
    Status.DROPPED -> MaterialTheme.colorScheme.errorContainer
    Status.PLAN_TO_WATCH -> MaterialTheme.colorScheme.surfaceVariant
    Status.WATCHING -> MaterialTheme.colorScheme.primaryContainer
    Status.READING -> MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun statusContentColor(status: Status): Color = when (status) {
    Status.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
    Status.DROPPED -> MaterialTheme.colorScheme.onErrorContainer
    Status.PLAN_TO_WATCH -> MaterialTheme.colorScheme.onSurfaceVariant
    Status.WATCHING -> MaterialTheme.colorScheme.onPrimaryContainer
    Status.READING -> MaterialTheme.colorScheme.onPrimaryContainer
}
