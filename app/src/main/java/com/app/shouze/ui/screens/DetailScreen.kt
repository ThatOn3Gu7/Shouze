package com.app.shouze.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.StarHalf
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.CategoryEntity
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.data.local.Status
import com.app.shouze.ui.components.SafeRemoteImage
import com.app.shouze.ui.components.rememberTooltipPositionProvider
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
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onIncrementRewatch: () -> Unit = {},
    onIncrementProgress: (MediaItemEntity) -> Unit = {},
    onMarkCompleted: (MediaItemEntity) -> Unit = {},
    onWhereToWatch: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var notesExpanded by remember { mutableStateOf(false) }
    val isLiterature = category?.name?.let { name ->
        name.contains("novel", ignoreCase = true) ||
            name.contains("book", ignoreCase = true) ||
            name.contains("manga", ignoreCase = true)
    } ?: false
    val progressUnit = if (isLiterature) "Chapter" else "Episode"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Details", 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    val view = androidx.compose.ui.platform.LocalView.current
                    TooltipBox(
                        tooltip = {
                            PlainTooltip {
                                Text(if (item.isFavorite) "Unfavorite" else "Favorite")
                            }
                        },
                        state = rememberTooltipState(),
                        positionProvider = rememberTooltipPositionProvider(),
                        focusable = false
                    ) {
                        IconButton(onClick = {
                            com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(view)
                            onToggleFavorite()
                        }) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = if (item.isFavorite) "Unfavorite" else "Favorite",
                                tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    TooltipBox(
                        tooltip = {
                            PlainTooltip { Text("Edit") }
                        },
                        state = rememberTooltipState(),
                        positionProvider = rememberTooltipPositionProvider(),
                        focusable = false
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Outlined.Edit, 
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    val viewForDelete = androidx.compose.ui.platform.LocalView.current
                    TooltipBox(
                        tooltip = {
                            PlainTooltip { Text("Delete") }
                        },
                        state = rememberTooltipState(),
                        positionProvider = rememberTooltipPositionProvider(),
                        focusable = false
                    ) {
                        IconButton(onClick = {
                            com.app.shouze.ui.components.HapticsHelper.performDeleteHaptic(viewForDelete)
                            showDeleteConfirm = true
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete, 
                                contentDescription = "Delete", 
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TooltipBox(
                        tooltip = {
                            PlainTooltip { Text("Where to Watch") }
                        },
                        state = rememberTooltipState(),
                        positionProvider = rememberTooltipPositionProvider(),
                        focusable = false
                    ) {
                        IconButton(onClick = onWhereToWatch) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "Where to Watch",
                                tint = Color(0xFF00BCD4)
                            )
                        }
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
        ) {
            CoverBanner(
                coverUri = item.coverImageUri,
                title = item.title,
                categoryName = category?.name ?: "Unknown",
                status = item.status
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                text = category?.name ?: "Unknown",
                                fontWeight = FontWeight.SemiBold
                            ) 
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                text = statusLabel(item.status),
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = statusContainerColor(item.status),
                            labelColor = statusContentColor(item.status)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (item.genres.isNotEmpty()) {
                    DetailInfoCard(title = "Genres") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item.genres.forEach { genre ->
                                AssistChip(
                                    onClick = { },
                                    label = { 
                                        Text(
                                            text = genre,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                DetailInfoCard(title = "Progress") {
                    val progressText = if (item.totalCount > 0) {
                        "${item.currentProgress} / ${item.totalCount}"
                    } else {
                        "${item.currentProgress} / ongoing"
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (item.totalCount > 0) {
                        LinearProgressIndicator(
                            progress = { (item.currentProgress.toFloat() / item.totalCount).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = { onIncrementProgress(item) },
                            enabled = item.totalCount == 0 || item.currentProgress < item.totalCount,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+1 $progressUnit", fontWeight = FontWeight.Bold)
                        }
                        if (item.status != Status.COMPLETED) {
                            Button(
                                onClick = { onMarkCompleted(item) },
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark Complete", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (item.currentVolume != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Current Volume: ${item.currentVolume}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (item.rating > 0.0) {
                    DetailInfoCard(title = "Rating") {
                        LargeRatingBar(rating = item.rating)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (item.rewatchCount > 0 || item.status == Status.COMPLETED) {
                    DetailInfoCard(title = "Rewatches") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.rewatchCount}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = onIncrementRewatch,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("+1 Rewatch", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (item.notes.isNotBlank()) {
                    DetailInfoCard(title = "Notes") {
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (notesExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.notes.length > 120) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = { notesExpanded = !notesExpanded }) {
                                Text(
                                    text = if (notesExpanded) "Show less" else "Read more",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                DetailInfoCard(title = "Info") {
                    InfoRow(label = "Last Updated", value = dateFormat.format(Date(item.lastUpdated)))
                    if (item.startDate != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        InfoRow(label = "Started", value = dateFormat.format(Date(item.startDate)))
                    }
                    if (item.endDate != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        InfoRow(label = "Finished", value = dateFormat.format(Date(item.endDate)))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { 
                Text(
                    text = "Delete item?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = { 
                Text(
                    text = "This will permanently remove '${item.title}' from your library.",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun BannerPlaceholder(failed: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Icon(
            imageVector = if (failed) Icons.Rounded.BrokenImage else Icons.Rounded.Image,
            contentDescription = if (failed) "Image failed to load" else null,
            tint = if (failed) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            },
            modifier = Modifier.align(Alignment.Center).size(80.dp)
        )
    }
}

@Composable
private fun CoverBanner(
    coverUri: String?,
    title: String,
    categoryName: String,
    status: Status,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
    ) {
        if (!coverUri.isNullOrBlank()) {
            SafeRemoteImage(
                url = coverUri,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = { BannerPlaceholder() },
                errorContent = { BannerPlaceholder(failed = true) }
            )
        } else {
            BannerPlaceholder()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                        ),
                        startY = 80f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (coverUri.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$categoryName · ${statusLabel(status)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (coverUri.isNullOrBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)
            )
        }
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
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(14.dp))
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
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
                index < fullStars -> Icons.Rounded.Star
                index == fullStars && hasHalf -> Icons.AutoMirrored.Rounded.StarHalf
                else -> Icons.Rounded.StarBorder
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
            fontWeight = FontWeight.Bold,
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
    Status.PLAN_TO_WATCH -> MaterialTheme.colorScheme.surfaceContainerHighest
    Status.WATCHING, Status.READING -> MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun statusContentColor(status: Status): Color = when (status) {
    Status.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
    Status.DROPPED -> MaterialTheme.colorScheme.onErrorContainer
    Status.PLAN_TO_WATCH -> MaterialTheme.colorScheme.onSurface
    Status.WATCHING, Status.READING -> MaterialTheme.colorScheme.onPrimaryContainer
}

