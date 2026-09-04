package com.app.shouze.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.app.shouze.data.local.MediaStatus

@Composable
fun MediaStatus.containerColor(): Color = when (this) {
    MediaStatus.CURRENT -> MaterialTheme.colorScheme.primaryContainer
    MediaStatus.PLANNING -> MaterialTheme.colorScheme.surfaceContainerHighest
    MediaStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
    MediaStatus.DROPPED -> MaterialTheme.colorScheme.errorContainer
    MediaStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
    MediaStatus.REPEATING -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
fun MediaStatus.contentColor(): Color = when (this) {
    MediaStatus.CURRENT -> MaterialTheme.colorScheme.onPrimaryContainer
    MediaStatus.PLANNING -> MaterialTheme.colorScheme.onSurfaceVariant
    MediaStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
    MediaStatus.DROPPED -> MaterialTheme.colorScheme.onErrorContainer
    MediaStatus.PAUSED -> MaterialTheme.colorScheme.onSecondaryContainer
    MediaStatus.REPEATING -> MaterialTheme.colorScheme.onSecondaryContainer
}

/** Traffic-light color for an AniList 0–100 score. */
fun scoreColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF2E7D32)
    score >= 60 -> Color(0xFFEF6C00)
    score >= 40 -> Color(0xFFC62828)
    else -> Color(0xFF757575)
}
