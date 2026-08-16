package com.app.shouze.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.remote.AniListMedia
import com.app.shouze.ui.AniListSearchUiState
import com.app.shouze.ui.components.SafeRemoteImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: AniListSearchUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onSelect: (AniListMedia) -> Unit,
    onLoadTrending: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onLoadTrending()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search AniList") },
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search anime or manga...") },
                trailingIcon = {
                    IconButton(onClick = { onSearch(query) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch(query) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = if (uiState.searchType == "ANIME") 0 else 1) {
                Tab(
                    selected = uiState.searchType == "ANIME",
                    onClick = { onTypeChange("ANIME"); onLoadTrending() },
                    text = { Text("Anime") }
                )
                Tab(
                    selected = uiState.searchType == "MANGA",
                    onClick = { onTypeChange("MANGA"); onLoadTrending() },
                    text = { Text("Manga") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }
            }

            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            when {
                uiState.results.isNotEmpty() -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.results, key = { it.id }) { media ->
                            AniListResultCard(
                                media = media,
                                onClick = { onSelect(media) }
                            )
                        }
                    }
                }
                uiState.error == null && !uiState.isLoading -> {
                    if (uiState.isTrendingLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                        }
                    } else if (uiState.trending.isNotEmpty()) {
                        Text(
                            text = "Trending",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(uiState.trending, key = { it.id }) { media ->
                                AniListResultCard(
                                    media = media,
                                    onClick = { onSelect(media) }
                                )
                            }
                        }
                    } else if (uiState.trendingError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.trendingError!!,
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
                            Text(
                                text = "Search for anime or manga to add to your library",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AniListResultCard(
    media: AniListMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 96.dp)
                    .clip(MaterialTheme.shapes.medium)
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
                        modifier = Modifier.fillMaxSize(),
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title.english ?: media.title.romaji ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
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
                    Spacer(modifier = Modifier.height(4.dp))
                }
                val countText = when {
                    media.episodes != null -> "${media.episodes} episodes"
                    media.chapters != null -> "${media.chapters} chapters"
                    else -> ""
                }
                if (countText.isNotBlank()) {
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                media.averageScore?.let { score ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Score: %.1f".format(score / 10.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
