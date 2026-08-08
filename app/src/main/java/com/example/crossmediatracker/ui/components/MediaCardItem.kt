package com.example.crossmediatracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.crossmediatracker.data.local.MediaItemEntity
import com.example.crossmediatracker.data.local.MediaType
import com.example.crossmediatracker.data.local.Status

@Composable
fun MediaCardItem(
    item: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(item = item)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildSubtitle(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.totalCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (item.currentProgress.toFloat() / item.totalCount).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (item.rating > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    RatingBar(rating = item.rating)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            StatusBadge(status = item.status)
        }
    }
}

@Composable
private fun AvatarCircle(item: MediaItemEntity, modifier: Modifier = Modifier) {
    val hue = (item.title.hashCode() and 0x7fffffff) % 360
    val color = Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(hue.toFloat(), 0.52f, 0.88f)
        )
    )
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.title.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
private fun StatusBadge(status: Status, modifier: Modifier = Modifier) {
    val container: Color
    val content: Color
    val label: String
    when (status) {
        Status.COMPLETED -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            content = MaterialTheme.colorScheme.onTertiaryContainer
            label = "Completed"
        }
        Status.DROPPED -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
            label = "Dropped"
        }
        Status.PLAN_TO_WATCH -> {
            container = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
            label = "Plan to Watch"
        }
        Status.WATCHING -> {
            container = MaterialTheme.colorScheme.primaryContainer
            content = MaterialTheme.colorScheme.onPrimaryContainer
            label = "Watching"
        }
        Status.READING -> {
            container = MaterialTheme.colorScheme.primaryContainer
            content = MaterialTheme.colorScheme.onPrimaryContainer
            label = "Reading"
        }
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun RatingBar(rating: Double, modifier: Modifier = Modifier) {
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
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "%.1f".format(rating),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildSubtitle(item: MediaItemEntity): String {
    val typeLabel = when (item.mediaType) {
        MediaType.TV_SERIES -> "TV Series"
        MediaType.ANIME -> "Anime"
        MediaType.NOVEL -> "Novel"
    }
    val progressLabel = if (item.totalCount > 0) {
        "${item.currentProgress}/${item.totalCount}"
    } else {
        "${item.currentProgress}/ongoing"
    }
    val volumeLabel = if (item.currentVolume != null && item.mediaType == MediaType.NOVEL) {
        " · Vol.${item.currentVolume}"
    } else {
        ""
    }
    return "$typeLabel · $progressLabel$volumeLabel"
}