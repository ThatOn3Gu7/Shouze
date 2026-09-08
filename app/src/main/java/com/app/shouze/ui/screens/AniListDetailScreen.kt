package com.app.shouze.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.data.local.Status
import com.app.shouze.data.remote.AniListFuzzyDate
import com.app.shouze.data.remote.AniListMedia
import com.app.shouze.data.remote.AniListMediaTag
import com.app.shouze.data.remote.AniListScoreCount
import com.app.shouze.data.remote.AniListStatusCount
import com.app.shouze.ui.MediaViewModel
import com.app.shouze.ui.components.SafeRemoteImage

/**
 * Full AniList detail experience, modelled on the reference client:
 * rich hero + stats strip, expandable synopsis, then five pill tabs —
 * Information / Staff / Relations / Stats / Threads — each backed by its
 * own lazily-loaded query slice.
 *
 * [media] may arrive from a light search/trending/library payload; [fullMedia]
 * carries the complete detail once fetched. When [trackedItem] is non-null the
 * media is already on the user's list and a floating status button replaces
 * the add bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onOpenRelated: (AniListMedia) -> Unit = {},
    trackedItem: MediaItemEntity? = null,
    staffState: MediaViewModel.DetailStaffState = MediaViewModel.DetailStaffState(),
    relationsState: MediaViewModel.DetailRelationsState = MediaViewModel.DetailRelationsState(),
    statsState: MediaViewModel.DetailStatsState = MediaViewModel.DetailStatsState(),
    socialState: MediaViewModel.DetailSocialState = MediaViewModel.DetailSocialState(),
    onLoadStaff: (Int) -> Unit = {},
    onLoadRelations: (Int) -> Unit = {},
    onLoadStats: (Int) -> Unit = {},
    onLoadSocial: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember(media.id) { mutableIntStateOf(0) }
    var showStatusBottomSheet by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember(media.id) { mutableStateOf(false) }

    val d = fullMedia ?: media
    LaunchedEffect(media.id) { onLoadDetail(media.id) }

    // Lazy-load whichever tab is visible.
    LaunchedEffect(selectedTab, media.id) {
        when (selectedTab) {
            1 -> if (staffState.isEmpty() && !staffState.isLoading) onLoadStaff(media.id)
            2 -> if (relationsState.isEmpty() && !relationsState.isLoading) onLoadRelations(media.id)
            3 -> if (statsState.isEmpty() && !statsState.isLoading) onLoadStats(media.id)
            4 -> if (socialState.isEmpty() && !socialState.isLoading) onLoadSocial(media.id)
        }
    }

    val mainTitle = media.title.english?.ifBlank { null }
        ?: media.title.romaji?.ifBlank { null }
        ?: media.title.native?.ifBlank { null }
        ?: "Unknown Title"
    val subTitle = media.title.romaji?.takeIf { it != mainTitle }
        ?: media.title.native?.takeIf { it != mainTitle }

    val isManga = media.format?.equals("MANGA", ignoreCase = true) == true ||
        media.chapters != null || media.volumes != null
    val defaultAddStatus = if (isManga) Status.READING else Status.PLAN_TO_WATCH

    fun handleStatusPicked(status: Status) {
        onAdd(media, status)
        val label = statusLabel(status)
        Toast.makeText(context, "Saved \"$mainTitle\" as $label", Toast.LENGTH_SHORT).show()
        showStatusBottomSheet = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (trackedItem != null) {
                ExtendedFloatingActionButton(
                    onClick = { showStatusBottomSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = statusLabel(trackedItem.status), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        bottomBar = {
            if (trackedItem == null) {
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
                        Button(
                            onClick = { handleStatusPicked(defaultAddStatus) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = MaterialTheme.shapes.large,
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
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(bottom = padding.calculateBottomPadding())
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ---------------- Hero ----------------
                val coverUrl = d.coverImage?.large ?: d.coverImage?.medium
                val bannerUrl = d.bannerImage ?: coverUrl

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (!bannerUrl.isNullOrBlank()) {
                        SafeRemoteImage(
                            url = bannerUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.80f),
                                        MaterialTheme.colorScheme.background
                                    )
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
                            modifier = Modifier
                                .width(120.dp)
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

                        Column(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Format row: icon + format
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isManga) Icons.Rounded.MenuBook else Icons.Rounded.Tv,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = (d.format ?: if (isManga) "MANGA" else "ANIME").replace("_", " "),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            // Duration row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = d.duration?.let { "$it min" } ?: "Unknown",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Release status row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = readableReleaseStatus(d.status),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Title + alt title
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats strip
                StatsStrip(
                    nextEpisode = d.nextAiringEpisode?.episode,
                    secondsUntil = d.nextAiringEpisode?.timeUntilAiring ?: 0,
                    meanScore = d.averageScore,
                    popularity = d.popularity,
                    favourites = d.favourites
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Synopsis: expand + copy
                val cleaned = parseHtmlDescription(d.description)
                if (cleaned.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .animateContentSize()
                    ) {
                        Text(
                            text = cleaned,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 5,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (cleaned.length > 200) {
                                IconButton(onClick = { isDescriptionExpanded = !isDescriptionExpanded }) {
                                    Icon(
                                        imageVector = if (isDescriptionExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = if (isDescriptionExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = {
                                try {
                                    val cm = context.getSystemService(ClipboardManager::class.java)
                                    cm?.setPrimaryClip(ClipData.newPlainText(mainTitle, cleaned))
                                    Toast.makeText(context, "Synopsis copied", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy synopsis",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Genres
                if (!d.genres.isNullOrEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        d.genres.filter { it.isNotBlank() }.forEach { genre ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(genre, style = MaterialTheme.typography.labelMedium) },
                                shape = CircleShape,
                                colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = null
                            )
                        }
                    }
                }

                if (isLoadingDetail) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ---------------- Tab bar ----------------
                DetailTabBar(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    tabs = listOf(
                        Triple("Information", Icons.Rounded.Info, 0),
                        Triple("Staff", Icons.Rounded.People, 1),
                        Triple("Related", Icons.Rounded.AccountTree, 2),
                        Triple("Stats", Icons.Rounded.BarChart, 3),
                        Triple("Social", Icons.Rounded.Chat, 4)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---------------- Tab content ----------------
                when (selectedTab) {
                    0 -> InformationTab(
                        d = d,
                        context = context,
                        onStatusPicked = ::handleStatusPicked
                    )
                    1 -> StaffTab(
                        state = staffState,
                        onRetry = { onLoadStaff(media.id) }
                    )
                    2 -> RelationsTab(
                        state = relationsState,
                        onOpenRelated = onOpenRelated,
                        onRetry = { onLoadRelations(media.id) }
                    )
                    3 -> StatsTab(
                        state = statsState,
                        onRetry = { onLoadStats(media.id) }
                    )
                    else -> SocialTab(
                        state = socialState,
                        onRetry = { onLoadSocial(media.id) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Top bar overlay
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(24.dp), color = Color.Black.copy(alpha = 0.45f)) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(24.dp), color = Color.Black.copy(alpha = 0.45f)) {
                    Row {
                        IconButton(onClick = {
                            try {
                                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "$mainTitle — ${d.siteUrl ?: ""}"
                                    )
                                }
                                context.startActivity(android.content.Intent.createChooser(send, "Share"))
                            } catch (_: Exception) {
                            }
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showStatusBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStatusBottomSheet = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = if (trackedItem != null) "Update tracking" else "Add to your list",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                Status.entries.forEach { status ->
                    val config = statusUiConfig(status)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { handleStatusPicked(status) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(config.icon, contentDescription = null, tint = config.color)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = config.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (trackedItem?.status == status) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Current status",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Hero stats strip
// ---------------------------------------------------------------------------

@Composable
private fun StatsStrip(
    nextEpisode: Int?,
    secondsUntil: Int,
    meanScore: Int?,
    popularity: Int?,
    favourites: Int?
) {
    if (nextEpisode == null && meanScore == null && popularity == null && favourites == null) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (nextEpisode != null && secondsUntil > 0) {
                StripItem(
                    value = "Ep $nextEpisode in " + formatCountdown(secondsUntil),
                    label = "Airing",
                    modifier = Modifier.weight(1.4f)
                )
                StripDivider()
            }
            meanScore?.let {
                StripItem(value = "$it%", label = "Mean score", modifier = Modifier.weight(1f))
                StripDivider()
            }
            StripItem(
                value = formatCompact(popularity ?: 0),
                label = "Popularity",
                modifier = Modifier.weight(1f)
            )
            StripDivider()
            StripItem(
                value = formatCompact(favourites ?: 0),
                label = "Favorites",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StripItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StripDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
    )
}

// ---------------------------------------------------------------------------
// Tab bar
// ---------------------------------------------------------------------------

@Composable
private fun DetailTabBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    tabs: List<Triple<String, ImageVector, Int>>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (label, icon, index) ->
            val isSelected = selected == index
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.inverseSurface
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (isSelected) MaterialTheme.colorScheme.inverseOnSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Information tab
// ---------------------------------------------------------------------------

@Composable
private fun InformationTab(
    d: AniListMedia,
    context: android.content.Context,
    onStatusPicked: (Status) -> Unit
) {
    var showAllSynonyms by remember { mutableStateOf(false) }
    var showSpoilerTags by remember { mutableStateOf(false) }
    var showAllTags by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DetailSectionTitle("Information")

        InfoRow("Duration", d.duration?.let { "$it min" } ?: "Unknown")
        InfoRow("Start date", formatFuzzyDate(d.startDate) ?: "Unknown")
        InfoRow("End date", formatFuzzyDate(d.endDate) ?: "Unknown")
        InfoRow(
            "Season",
            listOfNotNull(
                d.season?.lowercase()?.replaceFirstChar { it.uppercase() },
                d.seasonYear?.toString()
            ).joinToString(" ").ifBlank { "Unknown" }
        )
        InfoRow(
            "Source",
            d.source?.lowercase()?.split("_")?.joinToString(" ")?.replaceFirstChar { it.uppercase() }
                ?: "Unknown"
        )
        InfoRow("Romaji", d.title.romaji ?: "—")
        InfoRow("English", d.title.english ?: "—")
        InfoRow("Native", d.title.native ?: "—")

        val synonyms = d.synonyms.orEmpty().filter { it.isNotBlank() }
        if (synonyms.isNotEmpty()) {
            val shown = if (showAllSynonyms) synonyms else synonyms.take(3)
            InfoRow("Synonyms", shown.joinToString("\n"))
            if (synonyms.size > 3) {
                ExpandTextButton(
                    expanded = showAllSynonyms,
                    onChange = { showAllSynonyms = it }
                )
            }
        }

        d.trailer?.let { trailer ->
            if (trailer.site.equals("youtube", ignoreCase = true) && !trailer.id.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
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
                        .height(46.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.SmartDisplay, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Watch Trailer")
                }
            }
        }

        // Studios / Producers
        val studioEdges = d.studios?.edges.orEmpty().filter { it.node?.name != null }
        val mainStudios = studioEdges.filter { it.isMainStudio == true }.mapNotNull { it.node?.name }
        val producers = studioEdges.filter { it.isMainStudio != true }.mapNotNull { it.node?.name }
        if (mainStudios.isNotEmpty()) {
            ChipSection("Studios", mainStudios, context)
        }
        if (producers.isNotEmpty()) {
            ChipSection("Producers", producers, context)
        }

        // Tags with rank %, spoiler toggle and expander
        val allTags = d.tags.orEmpty().filter { !it.name.isNullOrBlank() }
        if (allTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailSectionTitle("Tags")
                TextButton(onClick = { showSpoilerTags = !showSpoilerTags }) {
                    Text(if (showSpoilerTags) "Hide Spoilers" else "Show Spoiler")
                }
            }
            val pool = allTags
                .filter { showSpoilerTags || it.isMediaSpoiler != true }
                .sortedByDescending { it.rank ?: 0 }
            val visibleTags = if (showAllTags) pool else pool.take(8)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleTags.forEach { tag -> TagChip(tag) }
            }
            if (pool.size > 8) {
                ExpandTextButton(expanded = showAllTags, onChange = { showAllTags = it })
            }
        }

        // Streaming episode thumbnails
        val episodes = d.streamingEpisodes.orEmpty().filter { !it.thumbnail.isNullOrBlank() }
        if (episodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            DetailSectionTitle("Episodes")
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(episodes.size) { i ->
                    val ep = episodes[i]
                    Column(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable {
                                try {
                                    context.startActivity(
                                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(ep.url))
                                    )
                                } catch (_: Exception) {
                                }
                            }
                    ) {
                        Box {
                            Card(
                                modifier = Modifier.size(width = 180.dp, height = 100.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                SafeRemoteImage(
                                    url = ep.thumbnail,
                                    contentDescription = ep.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(34.dp),
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.55f)
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ep.title ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Streaming sites + external links
        val streaming = d.externalLinks.orEmpty().filter { it.type.equals("STREAMING", true) && it.url.isNotBlank() }
        val info = d.externalLinks.orEmpty().filter { !it.type.equals("STREAMING", true) && it.url.isNotBlank() }
        if (streaming.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LinkChipSection("Streaming sites", streaming, context)
        }
        if (info.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LinkChipSection("External links", info, context)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ExpandTextButton(expanded: Boolean, onChange: (Boolean) -> Unit) {
    TextButton(onClick = { onChange(!expanded) }, contentPadding = PaddingValues(0.dp)) {
        Icon(
            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(if (expanded) "Less" else "More", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TagChip(tag: AniListMediaTag) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tag.rank?.let {
                Text(
                    text = "$it%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = tag.name ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ChipSection(title: String, items: List<String>, context: android.content.Context) {
    Spacer(modifier = Modifier.height(6.dp))
    DetailSectionTitle(title)
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { name ->
            SuggestionChip(
                onClick = {
                    try {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://anilist.co/search/anime?search=" + android.net.Uri.encode(name))
                            )
                        )
                    } catch (_: Exception) {
                    }
                },
                label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                shape = CircleShape
            )
        }
    }
}

@Composable
private fun LinkChipSection(title: String, links: List<com.app.shouze.data.remote.ExternalLink>, context: android.content.Context) {
    DetailSectionTitle(title)
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        links.forEach { link ->
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
                        Text(link.site, style = MaterialTheme.typography.labelMedium)
                        link.language?.let { lang ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lang,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = CircleShape
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Staff tab
// ---------------------------------------------------------------------------

@Composable
private fun StaffTab(
    state: MediaViewModel.DetailStaffState,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.isLoading) {
            TabLoading()
        } else if (state.error != null && state.staff.isEmpty() && state.characters.isEmpty()) {
            TabError(state.error, onRetry)
        } else {
            if (state.staff.isNotEmpty()) {
                DetailSectionTitle("Staff")
                state.staff.forEach { edge ->
                    PersonRow(
                        name = edge.node?.name?.full ?: "",
                        role = edge.role ?: "",
                        imageUrl = edge.node?.image?.large
                    )
                }
            }
            if (state.characters.isNotEmpty()) {
                DetailSectionTitle("Characters")
                state.characters.forEach { edge ->
                    CharacterRow(edge)
                }
            }
            if (state.staff.isEmpty() && state.characters.isEmpty() && state.error == null) {
                Text(
                    "No staff or character data available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PersonRow(name: String, role: String, imageUrl: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SafeRemoteImage(
            url = imageUrl,
            contentDescription = name,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CharacterRow(edge: com.app.shouze.data.remote.AniListCharacterEdge) {
    val va = edge.voiceActors.firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SafeRemoteImage(
            url = edge.node?.image?.large,
            contentDescription = edge.node?.name?.full,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = edge.node?.name?.full ?: "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = edge.role?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (va != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = va.name?.full ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.height(4.dp))
                SafeRemoteImage(
                    url = va.image?.large,
                    contentDescription = va.name?.full,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Relations tab
// ---------------------------------------------------------------------------

@Composable
private fun RelationsTab(
    state: MediaViewModel.DetailRelationsState,
    onOpenRelated: (AniListMedia) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.isLoading) {
            TabLoading()
        } else if (state.error != null && state.related.isEmpty() && state.recommendations.isEmpty()) {
            TabError(state.error, onRetry)
        } else {
            if (state.related.isNotEmpty()) {
                DetailSectionTitle("Related")
                Spacer(modifier = Modifier.height(4.dp))
                MediaCardRow(
                    items = state.related.mapNotNull { edge ->
                        edge.node?.let { MediaCardItemData(it, relationLabel(edge.relationType)) }
                    },
                    onOpen = onOpenRelated
                )
            }
            if (state.recommendations.isNotEmpty()) {
                DetailSectionTitle("Recommendations")
                Spacer(modifier = Modifier.height(4.dp))
                MediaCardRow(
                    items = state.recommendations.mapNotNull { edge ->
                        edge.node?.mediaRecommendation?.let {
                            MediaCardItemData(
                                it,
                                edge.rating?.takeIf { r -> r > 0 }?.let { r -> "$r likes" } ?: "Recommended"
                            )
                        }
                    },
                    onOpen = onOpenRelated
                )
            }
        }
    }
}

private data class MediaCardItemData(val media: AniListMedia, val caption: String)

@Composable
private fun MediaCardRow(items: List<MediaCardItemData>, onOpen: (AniListMedia) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items.size) { i ->
            val entry = items[i]
            Column(
                modifier = Modifier
                    .width(110.dp)
                    .clickable { onOpen(entry.media) }
            ) {
                Card(
                    modifier = Modifier.size(width = 110.dp, height = 150.dp),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    SafeRemoteImage(
                        url = entry.media.coverImage?.large,
                        contentDescription = entry.media.title?.english ?: entry.media.title?.romaji,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.media.title?.english ?: entry.media.title?.romaji ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stats tab
// ---------------------------------------------------------------------------

@Composable
private fun StatsTab(
    state: MediaViewModel.DetailStatsState,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.isLoading) {
            TabLoading()
        } else if (state.error != null && state.rankings.isEmpty() && state.statusDist.isEmpty()) {
            TabError(state.error, onRetry)
        } else {
            if (state.rankings.isNotEmpty()) {
                DetailSectionTitle("Rankings")
                state.rankings.take(6).forEach { ranking ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (ranking.type.equals("RATED", true)) Icons.Rounded.Star else Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = if (ranking.type.equals("RATED", true)) Color(0xFFFFB800) else Color(0xFFE85D75),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = rankingText(ranking.rank, ranking.type, ranking.context, ranking.year, ranking.allTime),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            if (state.statusDist.isNotEmpty()) {
                DetailSectionTitle("Status Distribution")
                val total = state.statusDist.sumOf { it.amount ?: 0 }.coerceAtLeast(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                ) {
                    state.statusDist.forEach { entry ->
                        val frac = (entry.amount ?: 0).toFloat() / total
                        if (frac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(frac)
                                    .fillMaxSize()
                                    .background(statusColor(entry.status))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.statusDist.forEach { entry ->
                        Surface(shape = CircleShape, color = statusColor(entry.status)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatCompact(entry.amount ?: 0),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = entry.status?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Total: ${formatCompact(total)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.scoreDist.isNotEmpty()) {
                DetailSectionTitle("Score Distribution")
                ScoreDistributionChart(state.scoreDist)
            }
        }
    }
}

@Composable
private fun ScoreDistributionChart(dist: List<AniListScoreCount>) {
    val max = dist.maxOfOrNull { it.amount ?: 0 }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        dist.sortedBy { it.score ?: 0 }.forEach { entry ->
            val amount = entry.amount ?: 0
            val frac = amount.toFloat() / max
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formatCompact(amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((14 + 100 * frac).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f + 0.5f * frac))
                )
                Text(
                    text = "${(entry.score ?: 0) / 10}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Social tab
// ---------------------------------------------------------------------------

@Composable
private fun SocialTab(
    state: MediaViewModel.DetailSocialState,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.isLoading) {
            TabLoading()
        } else if (state.error != null && state.threads.isEmpty() && state.reviews.isEmpty()) {
            TabError(state.error, onRetry)
        } else {
            if (state.threads.isNotEmpty()) {
                DetailSectionTitle("Threads")
                state.threads.forEach { thread ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = thread.title ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${thread.replyCount ?: 0}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "${formatCompact(thread.viewCount ?: 0)} views",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    thread.user?.name ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (state.reviews.isNotEmpty()) {
                DetailSectionTitle("Reviews")
                state.reviews.forEach { review ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = review.summary ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = Color(0xFFFFB800)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${((review.score ?: 0) / 10.0).format1()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    review.user?.name ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (state.activities.isNotEmpty()) {
                DetailSectionTitle("Recent activity")
                state.activities.forEach { activity ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = buildString {
                                    append(activity.user?.name ?: "Someone")
                                    activity.progress?.let { append(" · $it") }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            activity.createdAt?.let {
                                Text(
                                    text = relativeTime(it * 1000L),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (state.threads.isEmpty() && state.reviews.isEmpty() && state.activities.isEmpty() && state.error == null) {
                Text(
                    "Nothing here yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Small shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun TabLoading() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun TabError(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

// ---------------------------------------------------------------------------
// Pure helpers
// ---------------------------------------------------------------------------

/** True when neither slice of this tab state has data yet (triggers a load). */
private fun MediaViewModel.DetailStaffState.isEmpty() = staff.isEmpty() && characters.isEmpty() && error == null
private fun MediaViewModel.DetailRelationsState.isEmpty() = related.isEmpty() && recommendations.isEmpty() && error == null
private fun MediaViewModel.DetailStatsState.isEmpty() = rankings.isEmpty() && statusDist.isEmpty() && scoreDist.isEmpty() && error == null
private fun MediaViewModel.DetailSocialState.isEmpty() = threads.isEmpty() && reviews.isEmpty() && activities.isEmpty() && error == null

private fun Double.format1(): String = String.format("%.1f", this)

private fun formatCompact(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 10_000 -> String.format("%.0fK", n / 1_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}

private fun relativeTime(epochMillis: Long): String =
    android.text.format.DateUtils.getRelativeTimeSpanString(epochMillis).toString()

private fun readableReleaseStatus(status: String?): String =
    status?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""

private fun statusLabel(status: Status): String = when (status) {
    Status.WATCHING -> "Watching"
    Status.READING -> "Reading"
    Status.COMPLETED -> "Completed"
    Status.PLAN_TO_WATCH -> "Plan to Watch / Read"
    Status.DROPPED -> "Dropped"
    Status.PAUSED -> "Paused"
    Status.REPEATING -> "Rewatching"
}

private fun rankingText(rank: Int?, type: String?, context: String?, year: Int?, allTime: Boolean?): String {
    val kind = if (type.equals("RATED", true)) "Highest rated" else "Most popular"
    val scope = when {
        allTime == true || context.equals("ALL_TIME", true) -> "all time"
        year != null -> year.toString()
        else -> context?.lowercase()?.replace('_', ' ') ?: "all time"
    }
    return "#${rank ?: "?"} $kind $scope"
}

private fun statusColor(status: String?): Color = when (status?.uppercase()) {
    "CURRENT" -> Color(0xFF6BBE5A)
    "PLANNING" -> Color(0xFF8D959E)
    "COMPLETED" -> Color(0xFF5C7CD9)
    "DROPPED" -> Color(0xFFE85D75)
    "PAUSED" -> Color(0xFFE8A13C)
    "REPEATING" -> Color(0xFF3FAF9E)
    else -> Color(0xFF8D959E)
}

/** PREQUEL -> "Prequel", SOURCE -> "Source", null -> "Related". */
private fun relationLabel(type: String?): String =
    type?.lowercase()
        ?.split("_")
        ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        ?.takeIf { it.isNotBlank() }
        ?: "Related"

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

/** Strips HTML tags and decodes the entities AniList descriptions use. */
private fun parseHtmlDescription(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return raw
        .replace("<br>", "\n")
        .replace("<br/>", "\n")
        .replace("<br />", "\n")
        .replace("<i>", "").replace("</i>", "")
        .replace("<b>", "").replace("</b>", "")
        .replace("<em>", "").replace("</em>", "")
        .replace("<strong>", "").replace("</strong>", "")
        .replace(Regex("<[^>]*>"), " ")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&#039;", "'")
        .replace("&apos;", "'")
        .replace("&ldquo;", "\u201C")
        .replace("&rdquo;", "\u201D")
        .replace("&mdash;", "\u2014")
        .replace("&hellip;", "\u2026")
        .replace(Regex("\\s+\\n"), "\n")
        .trim()
}

private data class StatusUiConfig(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

private fun statusUiConfig(status: Status): StatusUiConfig = when (status) {
    Status.WATCHING -> StatusUiConfig("Watching", Icons.Rounded.PlayArrow, Color(0xFF43A047))
    Status.READING -> StatusUiConfig("Reading", Icons.Rounded.PlayArrow, Color(0xFF43A047))
    Status.COMPLETED -> StatusUiConfig("Completed", Icons.Rounded.Check, Color(0xFF1E88E5))
    Status.PLAN_TO_WATCH -> StatusUiConfig("Plan to Watch / Read", Icons.Rounded.Schedule, Color(0xFF8E24AA))
    Status.DROPPED -> StatusUiConfig("Dropped", Icons.Rounded.Favorite, Color(0xFFE53935))
    Status.PAUSED -> StatusUiConfig("Paused", Icons.Rounded.Schedule, Color(0xFFFB8C00))
    Status.REPEATING -> StatusUiConfig("Rewatching", Icons.Rounded.PlayArrow, Color(0xFF00897B))
}

