package com.app.shouze.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.app.shouze.data.remote.AniListMedia
import com.app.shouze.ui.AniListSearchUiState
import com.app.shouze.ui.components.SafeRemoteImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: AniListSearchUiState,
    onSearch: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onSelect: (AniListMedia) -> Unit,
    onLoadTrending: () -> Unit = {},
    searchHistory: List<String> = emptyList(),
    onClearSearchHistory: () -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var fieldFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val historyMatches = remember(searchHistory, query) {
        if (query.isBlank()) emptyList()
        else searchHistory.filter { it.contains(query.trim(), ignoreCase = true) }
    }

    var trendingLoadedOnce by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!trendingLoadedOnce) {
            trendingLoadedOnce = true
            onLoadTrending()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Search AniList",
                        fontWeight = FontWeight.Bold
                    ) 
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
        ) {
            // --- Search Bar ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .onFocusChanged { fieldFocused = it.isFocused },
                    placeholder = { Text("Search anime or manga...") },
                    trailingIcon = {
                        IconButton(onClick = {
                            onSearch(query)
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearch(query)
                            focusManager.clearFocus()
                        }
                    )
                )
                SearchHistoryDropdown(
                    visible = fieldFocused && historyMatches.isNotEmpty(),
                    matches = historyMatches,
                    onSelect = { history ->
                        query = history
                        onSearch(history)
                        focusManager.clearFocus()
                    },
                    onClear = onClearSearchHistory
                )
            }

            // --- Tabs ---
            TabRow(
                selectedTabIndex = if (uiState.searchType == "ANIME") 0 else 1,
                containerColor = Color.Transparent,
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                }
            ) {
                Tab(
                    selected = uiState.searchType == "ANIME",
                    onClick = { onTypeChange("ANIME"); onLoadTrending() },
                    text = { Text("Anime", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = uiState.searchType == "MANGA",
                    onClick = { onTypeChange("MANGA"); onLoadTrending() },
                    text = { Text("Manga", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Content States ---
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline, 
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Search Failed", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = uiState.error, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else when {
                uiState.results.isNotEmpty() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(uiState.results, key = { it.id }) { media ->
                            PremiumAniListResultCard(
                                media = media,
                                onClick = { onSelect(media) }
                            )
                        }
                    }
                }
                !uiState.isLoading -> {
                    if (uiState.isTrendingLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.trending.isNotEmpty()) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            item {
                                Text(
                                    text = "Trending Now",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(uiState.trending, key = { it.id }) { media ->
                                PremiumAniListResultCard(
                                    media = media,
                                    onClick = { onSelect(media) }
                                )
                            }
                        }
                    } else if (uiState.trendingError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.trendingError,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "Search for anime or manga\nto add to your library",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumAniListResultCard(
    media: AniListMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elevated, rounded poster image identical to AiringSchedule
            Surface(
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(2f / 3f),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp
            ) {
                val coverUrl = media.coverImage?.medium ?: media.coverImage?.large
                if (coverUrl != null) {
                    SafeRemoteImage(
                        url = coverUrl,
                        contentDescription = media.title.english ?: media.title.romaji,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (media.title.english ?: media.title.romaji ?: "?").first().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = media.title.english ?: media.title.romaji ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (!media.genres.isNullOrEmpty()) {
                    Text(
                        text = media.genres.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val countText = when {
                        media.episodes != null -> "${media.episodes} Ep"
                        media.chapters != null -> "${media.chapters} Ch"
                        else -> ""
                    }
                    if (countText.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    media.averageScore?.let { score ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f".format(score / 10.0),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryDropdown(
    visible: Boolean,
    matches: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(200)
        ) + fadeIn(animationSpec = tween(200)),
        exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 72.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp
        ) {
            Column {
                matches.forEach { history ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(history) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = history,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 44.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClear)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Clear search history",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
