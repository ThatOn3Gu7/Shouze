package com.app.shouze.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.shouze.data.remote.ExternalLink
import com.app.shouze.data.remote.StreamingEpisode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingLinksScreen(
    title: String,
    streamingEpisodes: List<StreamingEpisode>,
    externalLinks: List<ExternalLink>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onLoad: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Where to Watch") },
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
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load links", style = MaterialTheme.typography.titleMedium)
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                val allLinks = remember(streamingEpisodes, externalLinks) {
                    val eps = streamingEpisodes.mapNotNull { ep ->
                        if (!ep.url.isNullOrBlank() && !ep.site.isNullOrBlank()) {
                            LinkItem(ep.site, ep.url, "Episode: ${ep.title ?: "N/A"}")
                        } else null
                    }
                    val ext = externalLinks.map { LinkItem(it.site, it.url, null) }
                    eps + ext
                }

                if (allLinks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No streaming links found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(allLinks, key = { it.url }) { link ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    context.startActivity(intent)
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = link.site,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (link.subtitle != null) {
                                            Text(
                                                text = link.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.OpenInBrowser,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LinkItem(
    val site: String,
    val url: String,
    val subtitle: String?
)
