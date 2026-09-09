package com.app.shouze.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.local.Status
import com.app.shouze.data.remote.AniListFuzzyDate
import com.app.shouze.data.remote.AniListMedia
import com.app.shouze.ui.components.SafeRemoteImage

/**
 * Helper data class for Status UI configuration
 */
private data class StatusUiConfig(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun getStatusConfig(status: Status): StatusUiConfig {
    return when (status) {
        Status.WATCHING -> StatusUiConfig(
            label = "Watching",
            icon = Icons.Rounded.PlayCircle,
            color = MaterialTheme.colorScheme.primary
        )
        Status.READING -> StatusUiConfig(
            label = "Reading",
            icon = Icons.Rounded.MenuBook,
            color = MaterialTheme.colorScheme.primary
        )
        Status.COMPLETED -> StatusUiConfig(
            label = "Completed",
            icon = Icons.Rounded.CheckCircle,
            color = MaterialTheme.colorScheme.tertiary
        )
        Status.PLAN_TO_WATCH -> StatusUiConfig(
            label = "Plan to Watch",
            icon = Icons.Rounded.Bookmark,
            color = MaterialTheme.colorScheme.secondary
        )
        Status.DROPPED -> StatusUiConfig(
            label = "Dropped",
            icon = Icons.Rounded.Cancel,
            color = MaterialTheme.colorScheme.error
        )
        Status.PAUSED -> StatusUiConfig(
            label = "Paused",
            icon = Icons.Rounded.PauseCircle,
            color = MaterialTheme.colorScheme.secondary
        )
        Status.REPEATING -> StatusUiConfig(
            label = "Rewatching",
            icon = Icons.Rounded.Replay,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AniListDetailScreen(
    media: AniListMedia,
    onBack: () -> Unit,
    onAdd: (AniListMedia, Status) -> Unit,
    modifier: Modifier = Modifier,
    fullMedia: AniListMedia? = null,
    isLoadingDetail: Boolean = false,
    detailError: String? = null,
    onLoadDetail: (Int) -> Unit = {},
    onOpenRelated: (AniListMedia) -> Unit = {}
) {
    val context = LocalContext.current
    var showStatusBottomSheet by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    // Safe title resolution prioritize English -> Romaji -> Native
    val mainTitle = media.title.english?.ifBlank { null }
        ?: media.title.romaji?.ifBlank { null }
        ?: media.title.native?.ifBlank { null }
        ?: "Unknown Title"

    val subTitle = media.title.romaji?.takeIf { it != mainTitle }
        ?: media.title.native?.takeIf { it != mainTitle }

    // Determine media format (Anime vs Manga) using available fields
    val isManga = media.format?.equals("MANGA", ignoreCase = true) == true ||
            media.chapters != null || media.volumes != null

    val defaultAddStatus = if (isManga) Status.READING else Status.PLAN_TO_WATCH

    // Light search data renders the header immediately; the full-detail fetch
    // (dates, staff, studios, relations, watch links…) fills the sections below.
    val d = fullMedia ?: media
    LaunchedEffect(media.id) { onLoadDetail(media.id) }

    fun handleAddWithStatus(status: Status) {
        onAdd(media, status)
        val configLabel = when (status) {
            Status.WATCHING -> "Watching"
            Status.READING -> "Reading"
            Status.COMPLETED -> "Completed"
            Status.PLAN_TO_WATCH -> "Plan to Watch / Read"
            Status.DROPPED -> "Dropped"
            Status.PAUSED -> "Paused"
            Status.REPEATING -> "Rewatching"
        }
        Toast.makeText(context, "Added \"$mainTitle\" to $configLabel", Toast.LENGTH_SHORT).show()
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Primary Add Button
                    Button(
                        onClick = { handleAddWithStatus(defaultAddStatus) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large, // use 'large' instead of 'extraLarge'
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isManga) "Add to Reading" else "Add to Plan to Watch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // More Status Options Button
                    FilledTonalIconButton(
                        onClick = { showStatusBottomSheet = true },
                        modifier = Modifier.size(52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.UnfoldMore,
                            contentDescription = "Choose status options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(bottom = padding.calculateBottomPadding())
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
            val coverUrl = media.coverImage?.large
                ?: media.coverImage?.medium

            // Real banner when the detail fetch has it; cover as fallback
            val bannerUrl = d.bannerImage ?: coverUrl

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                // Blurred Ambient Background Banner
                if (!bannerUrl.isNullOrBlank()) {
                    SafeRemoteImage(
                        url = bannerUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(16.dp)
                    )
                }

                // Dark Gradient Mask
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Floating Hero Poster Card
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .aspectRatio(2f / 3f),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        if (!coverUrl.isNullOrBlank()) {
                            SafeRemoteImage(
                                url = coverUrl,
                                contentDescription = mainTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mainTitle.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Side Summary Info inside Hero
                    Column(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Media Format / Type Chip
                        val formatText = media.format?.replace("_", " ") ?: if (isManga) "MANGA" else "ANIME"
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = formatText.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Media Rating Badge
                        media.averageScore?.let { score ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = "Rating",
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "%.1f / 10".format(score / 10.0),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        // Airing / Release Status
                        media.status?.let { status ->
                            val readableStatus = status.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() }
                            Text(
                                text = readableStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = mainTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (!subTitle.isNullOrBlank()) {
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                val altTitles = d.synonyms.orEmpty().filter { it.isNotBlank() }.take(3)
                if (altTitles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Also known as: " + altTitles.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stat 1: Episodes / Chapters / Volumes (+ per-episode length)
                    val countLabel = when {
                        d.episodes != null -> "${d.episodes} Episodes" + d.duration?.let { " × $it min" }.orEmpty()
                        d.chapters != null -> "${d.chapters} Chapters"
                        d.volumes != null -> "${d.volumes} Volumes"
                        else -> "Ongoing / Unknown"
                    }
                    DetailBentoCard(
                        modifier = Modifier.weight(1f),
                        icon = if (isManga) Icons.Rounded.MenuBook else Icons.Rounded.Tv,
                        label = if (isManga) "Length" else "Episodes",
                        value = countLabel
                    )

                    // Stat 2: Format + season
                    val formatDisplay = buildString {
                        append(d.format?.replace("_", " ") ?: "N/A")
                        if (d.season != null || d.seasonYear != null) {
                            append(" · ")
                            d.season?.let { append(it.lowercase().replaceFirstChar { c -> c.uppercase() } + " ") }
                            d.seasonYear?.let { append("$it") }
                        }
                    }
                    DetailBentoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.CalendarToday,
                        label = "Format",
                        value = formatDisplay
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stat 3: Source material / country
                    val originDisplay = listOfNotNull(
                        d.source?.lowercase()?.split("_")?.joinToString(" ")?.replaceFirstChar { it.uppercase() },
                        d.countryOfOrigin
                    ).joinToString(" · ").ifBlank { "N/A" }
                    DetailBentoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Public,
                        label = "Origin",
                        value = originDisplay
                    )

                    // Stat 4: Aired / published dates
                    val datesDisplay = listOfNotNull(
                        formatFuzzyDate(d.startDate),
                        formatFuzzyDate(d.endDate)
                    ).joinToString(" – ").ifBlank {
                        if (d.status.equals("RELEASING", true) || d.status.equals("NOT_YET_RELEASED", true)) "Ongoing" else "N/A"
                    }
                    DetailBentoCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.CalendarMonth,
                        label = "Aired",
                        value = datesDisplay
                    )
                }
            }

            if (!media.genres.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Genres",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        media.genres.filter { it.isNotBlank() }.forEach { genre ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                shape = CircleShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            media.description?.let { rawDescription ->
                val cleanedDescription = parseHtmlDescription(rawDescription)
                if (cleanedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = "Synopsis",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .animateContentSize()
                            ) {
                                Text(
                                    text = cleanedDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 5,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (cleanedDescription.length > 200) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { isDescriptionExpanded = !isDescriptionExpanded },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = if (isDescriptionExpanded) "Show Less" else "Read More",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (isDescriptionExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- Full-detail sections ----------------

            d.nextAiringEpisode?.let { next ->
                if (next.episode != null && (next.timeUntilAiring ?: 0) > 0) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        DetailSectionTitle("Airing")
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Episode ${next.episode} airs in " + formatCountdown(next.timeUntilAiring ?: 0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            val studios = d.studios?.nodes.orEmpty().mapNotNull { it.name }.filter { it.isNotBlank() }
            if (studios.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    DetailSectionTitle("Studios")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = studios.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val visibleTags = d.tags.orEmpty()
                .filter { it.isMediaSpoiler != true && !it.name.isNullOrBlank() }
                .sortedByDescending { it.rank ?: 0 }
                .take(10)
            if (visibleTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailSectionTitle("Tags")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        visibleTags.forEach { tag ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = tag.name ?: "",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                shape = CircleShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            val staffEdges = d.staff?.edges.orEmpty().filter { it.node != null }
            if (staffEdges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    DetailSectionTitle("Staff")
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(staffEdges.size) { i ->
                            val edge = staffEdges[i]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(92.dp)
                            ) {
                                SafeRemoteImage(
                                    url = edge.node?.image?.large,
                                    contentDescription = edge.node?.name?.full,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = edge.node?.name?.full ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = edge.role ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            d.trailer?.let { trailer ->
                if (trailer.site.equals("youtube", ignoreCase = true) && !trailer.id.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://www.youtube.com/watch?v=" + trailer.id)
                                    )
                                )
                            } catch (_: Exception) {
                                Toast.makeText(context, "No app can open YouTube links", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Rounded.SmartDisplay, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Trailer")
                    }
                }
            }

            val watchLinks = d.externalLinks.orEmpty()
                .filter { !it.url.isNullOrBlank() }
                .sortedByDescending { it.type.equals("STREAMING", ignoreCase = true) }
            if (watchLinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailSectionTitle("Where to watch")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        watchLinks.forEach { link ->
                            SuggestionChip(
                                onClick = {
                                    try {
                                        context.startActivity(
                                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link.url))
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Link,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = link.site,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                shape = CircleShape
                            )
                        }
                    }
                }
            }

            val relatedEdges = d.relations?.edges.orEmpty()
                .filter { it.node != null && it.node?.id != null }
            if (relatedEdges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    DetailSectionTitle("Related")
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(relatedEdges.size) { i ->
                            val edge = relatedEdges[i]
                            val node = edge.node ?: return@items
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { onOpenRelated(node) }
                            ) {
                                Card(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 150.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    SafeRemoteImage(
                                        url = node.coverImage?.large,
                                        contentDescription = node.title?.english ?: node.title?.romaji,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = relationLabel(edge.relationType),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = node.title?.english ?: node.title?.romaji ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = mainTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .padding(end = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showStatusBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStatusBottomSheet = false },
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
                    text = "Select Library Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Status.entries.forEach { status ->
                    val config = getStatusConfig(status)
                    Surface(
                        onClick = {
                            showStatusBottomSheet = false
                            handleAddWithStatus(status)
                        },
                        shape = MaterialTheme.shapes.large,
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = config.color.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = config.icon,
                                        contentDescription = null,
                                        tint = config.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Text(
                                text = config.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBentoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Safely strips HTML tags and replaces entities in API descriptions.
 */
private fun parseHtmlDescription(html: String?): String {
    if (html.isNullOrBlank()) return ""
    return html
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
}

// ---------------------------------------------------------------------------
// Detail formatting helpers
// ---------------------------------------------------------------------------

private val MONTH_ABBREVIATIONS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** AniList fuzzy date -> "Jan 5, 2024" (tolerates missing month/day). */
private fun formatFuzzyDate(date: AniListFuzzyDate?): String? {
    val year = date?.year ?: return null
    val month = date.month
    val day = date.day
    return buildString {
        if (month != null && month in 1..12) {
            append(MONTH_ABBREVIATIONS[month - 1])
            if (day != null) append(" $day")
            append(", ")
        }
        append(year)
    }
}

/** Seconds until broadcast -> "2d 14h", "3h 25m", or "soon". */
private fun formatCountdown(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    val days = total / 86400
    val hours = (total % 86400) / 3600
    val minutes = (total % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "moments"
    }
}

/** PREQUEL -> "Prequel", SOURCE -> "Source", null -> "Related". */
private fun relationLabel(type: String?): String =
    type?.lowercase()
        ?.split("_")
        ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        ?.takeIf { it.isNotBlank() }
        ?: "Related"

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}
