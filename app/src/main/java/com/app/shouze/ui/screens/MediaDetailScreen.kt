package com.app.shouze.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.LibraryEntryEntity
import com.app.shouze.data.local.MediaStatus
import com.app.shouze.data.local.MediaType
import com.app.shouze.data.remote.MediaDetail
import com.app.shouze.ui.DetailUiState
import com.app.shouze.ui.components.PosterCard
import com.app.shouze.ui.components.SafeRemoteImage
import com.app.shouze.ui.components.containerColor
import com.app.shouze.ui.components.contentColor
import com.app.shouze.ui.components.scoreColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    detail: DetailUiState,
    entry: LibraryEntryEntity?,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onAdd: (MediaStatus) -> Unit,
    onSetStatus: (MediaStatus) -> Unit,
    onSetProgress: (Int) -> Unit,
    onSetScore: (Int) -> Unit,
    onSetNotes: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val media = detail.media
    var showStatusSheet by remember { mutableStateOf(false) }
    var showScoreDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    when {
        detail.isLoading && media == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        detail.error != null && media == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Couldn't load details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(detail.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = onBack) { Text("Back") }
                }
            }
        }
        media != null -> {
            val type = if (media.type == "MANGA") MediaType.MANGA else MediaType.ANIME
            val title = media.title?.english?.ifBlank { null }
                ?: media.title?.romaji?.ifBlank { null }
                ?: media.title?.native?.ifBlank { null }
                ?: "Unknown"
            val romaji = media.title?.romaji?.takeIf { it != title }
            val banner = media.bannerImage ?: media.coverImage?.extraLarge ?: media.coverImage?.large
            val poster = media.coverImage?.extraLarge ?: media.coverImage?.large ?: media.coverImage?.medium

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    DetailBottomBar(
                        entry = entry,
                        isLoggedIn = isLoggedIn,
                        onAddClick = { showStatusSheet = true },
                        onStatusClick = { showStatusSheet = true },
                        onIncrement = { onSetProgress((entry?.progress ?: 0) + 1) },
                        onMarkComplete = { onSetStatus(MediaStatus.COMPLETED) },
                        onScoreClick = { showScoreDialog = true },
                        onNotesClick = { showNotesDialog = true },
                        onRemoveClick = { showRemoveDialog = true },
                        onToggleFavorite = onToggleFavorite
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(bottom = padding.calculateBottomPadding()).fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        HeroHeader(title = title, romaji = romaji, media = media, banner = banner, poster = poster)

                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            media.description?.let { raw ->
                                val cleaned = parseHtml(raw)
                                if (cleaned.isNotBlank()) {
                                    SynopsisCard(description = cleaned)
                                }
                            }

                            StatsGrid(media = media, type = type)

                            if (!media.genres.isNullOrEmpty()) {
                                SectionLabel("Genres")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    media.genres.take(10).forEach { genre ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(genre) },
                                            shape = CircleShape
                                        )
                                    }
                                }
                            }

                            media.characters?.edges?.filter { it.node?.name?.full != null }?.let { edges ->
                                if (edges.isNotEmpty()) {
                                    SectionLabel("Characters")
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 20.dp)
                                    ) {
                                        items(edges, key = { it.node?.id ?: 0 }) { edge ->
                                            CharacterCard(
                                                name = edge.node?.name?.full ?: "",
                                                imageUrl = edge.node?.image?.large,
                                                role = edge.role
                                            )
                                        }
                                    }
                                }
                            }

                            media.staff?.edges?.filter { it.node?.name?.full != null }?.let { edges ->
                                if (edges.isNotEmpty()) {
                                    SectionLabel("Staff")
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        edges.forEach { edge ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = edge.node?.name?.full ?: "",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = edge.role ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val relations = media.relations?.edges?.mapNotNull { it.node } ?: emptyList()
                            if (relations.isNotEmpty()) {
                                SectionLabel("Related")
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp)
                                ) {
                                    items(relations, key = { it.id }) { rel ->
                                        PosterCard(
                                            title = rel.title?.english ?: rel.title?.romaji ?: "Unknown",
                                            coverUrl = rel.coverImage?.large ?: rel.coverImage?.medium,
                                            score = rel.averageScore,
                                            subtitle = rel.format?.replace('_', ' '),
                                            onClick = { onOpenMedia(rel.id) },
                                            width = 116.dp
                                        )
                                    }
                                }
                            }

                            val recommendations = media.recommendations?.nodes ?: emptyList()
                            if (recommendations.isNotEmpty()) {
                                SectionLabel("Recommendations")
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp)
                                ) {
                                    items(recommendations, key = { it.id }) { rec ->
                                        PosterCard(
                                            title = rec.title?.english ?: rec.title?.romaji ?: "Unknown",
                                            coverUrl = rec.coverImage?.large ?: rec.coverImage?.medium,
                                            score = rec.averageScore,
                                            subtitle = rec.format?.replace('_', ' '),
                                            onClick = { onOpenMedia(rec.id) },
                                            width = 116.dp
                                        )
                                    }
                                }
                            }

                            val links = buildLinks(media)
                            if (links.isNotEmpty() || media.siteUrl != null) {
                                SectionLabel("Where to watch / read")
                                media.siteUrl?.let { url ->
                                    LinkRow(label = "Official page on AniList", onClick = { onOpenLink(url) })
                                }
                                links.forEach { link ->
                                    LinkRow(label = link.first, onClick = { onOpenLink(link.second) })
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // Floating back button over the hero
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 12.dp, top = 8.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            if (showStatusSheet) {
                StatusSheet(
                    current = entry?.status,
                    type = type,
                    onSelect = { status ->
                        showStatusSheet = false
                        if (entry == null) onAdd(status) else onSetStatus(status)
                    },
                    onDismiss = { showStatusSheet = false }
                )
            }

            if (showScoreDialog && entry != null) {
                ScoreDialog(
                    current = entry.score,
                    onSave = { onSetScore(it); showScoreDialog = false },
                    onDismiss = { showScoreDialog = false }
                )
            }

            if (showNotesDialog && entry != null) {
                NotesDialog(
                    current = entry.notes,
                    onSave = { onSetNotes(it); showNotesDialog = false },
                    onDismiss = { showNotesDialog = false }
                )
            }

            if (showRemoveDialog && entry != null) {
                AlertDialog(
                    onDismissRequest = { showRemoveDialog = false },
                    title = { Text("Remove from list?") },
                    text = { Text("This removes \"${entry.title}\" from your list on AniList and this device.") },
                    confirmButton = {
                        TextButton(onClick = { onRemove(); showRemoveDialog = false }) {
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroHeader(
    title: String,
    romaji: String?,
    media: MediaDetail,
    banner: String?,
    poster: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        if (!banner.isNullOrBlank()) {
            SafeRemoteImage(
                url = banner,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(12.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.2f to Color.Black.copy(alpha = 0.35f),
                        0.6f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.width(124.dp).aspectRatio(2f / 3f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                if (!poster.isNullOrBlank()) {
                    SafeRemoteImage(
                        url = poster,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.displayMedium)
                    }
                }
            }
            Column(
                modifier = Modifier.padding(bottom = 6.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                media.format?.let {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            text = it.replace('_', ' ').uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                val score = media.meanScore ?: media.averageScore
                if (score != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Star, contentDescription = "Score", tint = scoreColor(score), modifier = Modifier.size(18.dp))
                        Text(
                            text = "%.1f / 10".format(score / 10.0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                media.status?.let {
                    Text(
                        text = it.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (media.season != null || media.seasonYear != null) {
                    Text(
                        text = "${media.season?.replaceFirstChar { c -> c.uppercase() } ?: ""} ${media.seasonYear ?: ""}".trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!romaji.isNullOrBlank()) {
            Text(
                text = romaji,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SynopsisCard(description: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis
            )
            if (description.length > 220) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (expanded) "Show less" else "Read more", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(media: MediaDetail, type: MediaType) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCell(
            modifier = Modifier.weight(1f),
            label = if (type == MediaType.MANGA) "Chapters" else "Episodes",
            value = when (type) {
                MediaType.MANGA -> media.chapters?.toString()
                MediaType.ANIME -> media.episodes?.toString()
            } ?: "—"
        )
        StatCell(
            modifier = Modifier.weight(1f),
            label = "Duration",
            value = media.duration?.let { "$it min" } ?: "—"
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCell(
            modifier = Modifier.weight(1f),
            label = "Season",
            value = if (media.seasonYear != null) "${media.season?.replaceFirstChar { c -> c.uppercase() } ?: ""} ${media.seasonYear}".trim() else "—"
        )
        StatCell(
            modifier = Modifier.weight(1f),
            label = "Studio",
            value = media.studios?.nodes?.firstOrNull()?.name ?: "—"
        )
    }
    val source = media.source?.replace('_', ' ').orEmpty()
    val country = media.countryOfOrigin ?: ""
    if (source.isNotBlank() || country.isNotBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCell(
                modifier = Modifier.weight(1f),
                label = "Source",
                value = source.ifBlank { "—" }
            )
            StatCell(
                modifier = Modifier.weight(1f),
                label = "Origin",
                value = country.ifBlank { "—" }
            )
        }
    }
}

@Composable
private fun StatCell(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CharacterCard(name: String, imageUrl: String?, role: String?) {
    Column(modifier = Modifier.width(96.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                SafeRemoteImage(
                    url = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(name.firstOrNull()?.uppercase() ?: "?", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        if (!role.isNullOrBlank()) {
            Text(role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Rounded.OpenInBrowser, contentDescription = "Open", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun buildLinks(media: MediaDetail): List<Pair<String, String>> {
    val links = mutableListOf<Pair<String, String>>()
    media.externalLinks?.forEach { link ->
        val url = link.url ?: return@forEach
        links.add((link.site ?: "Link") to url)
    }
    media.streamingEpisodes?.forEach { ep ->
        val url = ep.url ?: return@forEach
        links.add("${ep.site ?: "Stream"} — ${ep.title ?: "Episode"}" to url)
    }
    return links.distinctBy { it.second }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSheet(
    current: MediaStatus?,
    type: MediaType,
    onSelect: (MediaStatus) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (current == null) "Add to your list" else "Change status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            MediaStatus.entries.forEach { status ->
                Surface(
                    onClick = { onSelect(status) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (current == status) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusLabel(status, type),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (current == status) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreDialog(current: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(current / 10f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Score") },
        text = {
            Column {
                Text(
                    text = "%.1f / 10".format(value),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..10f
                )
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Text("0", style = MaterialTheme.typography.labelSmall)
                    Text("5", style = MaterialTheme.typography.labelSmall)
                    Text("10", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave((value * 10).toInt()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NotesDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notes") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("Personal notes about this title…") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DetailBottomBar(
    entry: LibraryEntryEntity?,
    isLoggedIn: Boolean,
    onAddClick: () -> Unit,
    onStatusClick: () -> Unit,
    onIncrement: () -> Unit,
    onMarkComplete: () -> Unit,
    onScoreClick: () -> Unit,
    onNotesClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        if (entry == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = isLoggedIn
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLoggedIn) "Add to list" else "Sign in to add", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onStatusClick,
                    shape = CircleShape,
                    color = entry.status.containerColor()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = entry.statusLabel(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = entry.status.contentColor()
                        )
                        Icon(Icons.Rounded.UnfoldMore, contentDescription = null, modifier = Modifier.size(16.dp), tint = entry.status.contentColor())
                    }
                }
                FilledTonalIconButton(onClick = onIncrement) {
                    Icon(Icons.Rounded.Add, contentDescription = "+1")
                }
                OutlinedButton(onClick = onMarkComplete) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
                FilledTonalIconButton(onClick = onScoreClick) {
                    Icon(Icons.Rounded.StarBorder, contentDescription = "Score")
                }
                FilledTonalIconButton(onClick = onNotesClick) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Notes")
                }
                FilledTonalIconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (entry.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = onRemoveClick) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun statusLabel(status: MediaStatus, type: MediaType): String = when (status) {
    MediaStatus.CURRENT -> if (type == MediaType.MANGA) "Reading" else "Watching"
    MediaStatus.PLANNING -> if (type == MediaType.MANGA) "Plan to Read" else "Plan to Watch"
    MediaStatus.COMPLETED -> "Completed"
    MediaStatus.DROPPED -> "Dropped"
    MediaStatus.PAUSED -> "Paused"
    MediaStatus.REPEATING -> if (type == MediaType.MANGA) "Rereading" else "Rewatching"
}

private fun parseHtml(html: String): String =
    html
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p>"), "\n\n")
        .replace(Regex("(?i)<p>"), "")
        .replace(Regex("<.*?>"), "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
