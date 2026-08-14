package com.app.shouze.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.Status
import com.app.shouze.data.remote.AniListMedia
import com.app.shouze.ui.components.SafeRemoteImage

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AniListDetailScreen(
    media: AniListMedia,
    onBack: () -> Unit,
    onAdd: (AniListMedia, Status) -> Unit
) {
    val context = LocalContext.current
    var showStatusPicker by remember { mutableStateOf(false) }
    val title = media.title.english ?: media.title.romaji ?: "Unknown"

    fun addWith(status: Status) {
        onAdd(media, status)
        val label = statusLabel(status)
        Toast.makeText(context, "Added $title to $label", Toast.LENGTH_SHORT).show()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .combinedClickable(
                        onClick = { addWith(Status.PLAN_TO_WATCH) },
                        onLongClick = { showStatusPicker = true }
                    ),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add to Library  ·  hold to choose status",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val cover = media.coverImage?.large ?: media.coverImage?.medium
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                if (cover != null) {
                    SafeRemoteImage(
                        url = cover,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                                startY = 120f
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val typeLabel = if (media.chapters != null || media.volumes != null) "Manga" else "Anime"
            val countText = when {
                media.episodes != null -> "${media.episodes} episodes"
                media.chapters != null -> "${media.chapters} chapters"
                media.volumes != null -> "${media.volumes} volumes"
                else -> typeLabel
            }
            AssistChip(onClick = {}, label = { Text(countText) })
            media.averageScore?.let { score ->
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Score: %.1f".format(score / 10.0)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!media.genres.isNullOrEmpty()) {
                Text(
                    "Genres",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    media.genres.forEach { genre ->
                        AssistChip(onClick = {}, label = { Text(genre) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            media.description?.let { raw ->
                val clean = raw.replace(Regex("<.*?>"), " ").replace(Regex("\\s+"), " ").trim()
                if (clean.isNotBlank()) {
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = clean,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (clean.length > 120) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "Show less" else "Read more")
                        }
                    }
                }
            }
        }
    }

    if (showStatusPicker) {
        AlertDialog(
            onDismissRequest = { showStatusPicker = false },
            title = { Text("Add with status") },
            text = {
                Column {
                    Status.entries.forEach { s ->
                        TextButton(
                            onClick = {
                                showStatusPicker = false
                                addWith(s)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(statusLabel(s))
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

private fun statusLabel(status: Status): String = when (status) {
    Status.COMPLETED -> "Completed"
    Status.DROPPED -> "Dropped"
    Status.PLAN_TO_WATCH -> "Plan to Watch"
    Status.WATCHING -> "Watching"
    Status.READING -> "Reading"
}
