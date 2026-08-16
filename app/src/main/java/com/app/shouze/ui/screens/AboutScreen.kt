package com.app.shouze.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val APP_VERSION = "3.0.0"
private const val GITHUB_REPO = "ThatOn3Gu7/Shouze"
private const val FEEDBACK_EMAIL = "recoveringdotcom@gmail.com"

private sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    object Error : UpdateState
    data class Available(val tag: String, val url: String) : UpdateState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }
    var showAboutDev by remember { mutableStateOf(false) }
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    val scope = rememberCoroutineScope()

    fun checkForUpdates() {
        scope.launch {
            updateState = UpdateState.Checking
            val latest = withContext(Dispatchers.IO) { fetchLatestRelease(GITHUB_REPO) }
            if (latest == null) {
                updateState = UpdateState.Error
                Toast.makeText(context, "Couldn't reach GitHub", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val (tag, url) = latest
            updateState = if (isNewerVersion(tag, APP_VERSION)) {
                Toast.makeText(context, "Update available: $tag", Toast.LENGTH_LONG).show()
                UpdateState.Available(tag, url)
            } else {
                Toast.makeText(context, "You're on the latest version", Toast.LENGTH_SHORT).show()
                UpdateState.UpToDate
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Refined App icon placeholder
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 8.dp,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "守",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Shouze",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Version $APP_VERSION",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Text pulled out of cards for a cleaner, modern look
            Text(
                text = "Your personal keeper for anime, manga, and everything you watch. Track your progress, organize by categories, and never lose track of what you're watching or reading.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Unified Actions Group (M3 Settings Style)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    AboutRow(
                        icon = Icons.Default.NewReleases,
                        title = "Check for Updates",
                        subtitle = when (val s = updateState) {
                            UpdateState.Checking -> "Checking for updates…"
                            is UpdateState.Available -> "Update available: ${s.tag}"
                            UpdateState.UpToDate -> "You're on the latest version"
                            UpdateState.Error -> "Couldn't check — tap to retry"
                            UpdateState.Idle -> "Tap to check for new versions"
                        },
                        onClick = {
                            when (val s = updateState) {
                                is UpdateState.Available -> openUrl(context, s.url)
                                else -> checkForUpdates()
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                    AboutRow(
                        icon = Icons.Default.Email,
                        title = "Send Feedback",
                        subtitle = "Report issues or share ideas",
                        onClick = {
                            val pkgInfo = runCatching {
                                context.packageManager.getPackageInfo(context.packageName, 0)
                            }.getOrNull()
                            val versionName = pkgInfo?.versionName ?: "?"
                            val versionCode = if (Build.VERSION.SDK_INT >= 28) {
                                pkgInfo?.longVersionCode
                            } else {
                                pkgInfo?.versionCode?.toLong()
                            }
                            
                            val info = buildString {
                                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                                appendLine("Architecture: ${Build.SUPPORTED_ABIS.joinToString()}")
                                appendLine("App: Shouze $versionName (code $versionCode)")
                                appendLine()
                                appendLine("--- Write your feedback after this line ---")
                                appendLine()
                            }
                            
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:") 
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                                putExtra(Intent.EXTRA_SUBJECT, "Shouze Feedback")
                                putExtra(Intent.EXTRA_TEXT, info)
                            }
                            
                            runCatching {
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, "No email app installed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                    AboutRow(
                        icon = Icons.Default.Person,
                        title = "About the Developer",
                        subtitle = "Story, projects & how to connect",
                        onClick = { showAboutDev = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                    AboutRow(
                        icon = Icons.Default.Description,
                        title = "Licenses",
                        subtitle = "Open-source libraries used",
                        onClick = { showLicenses = true }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Made with Compose",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            icon = { Icon(Icons.Default.Description, contentDescription = null) },
            title = { Text("Open-source Licenses") },
            text = {
                val licenses = listOf(
                    "Android & Jetpack Compose — Apache-2.0",
                    "Material 3 — Apache-2.0",
                    "Room (SQLite) — Apache-2.0",
                    "OkHttp — Apache-2.0",
                    "kotlinx.serialization — Apache-2.0",
                    "Coil (cover images) — Apache-2.0",
                    "AniList GraphQL API — data source"
                )
                LazyColumn {
                    items(licenses.size) { index ->
                        Text(
                            text = "• ${licenses[index]}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) { Text("Close") }
            }
        )
    }

    if (showAboutDev) {
        AboutDevDialog(onDismiss = { showAboutDev = false })
    }
}

@Composable
private fun AboutDevDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Person, contentDescription = null) },
        title = { Text("About the Developer") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text("The Story", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Shouze started as a learning project. I built it because I wanted to learn how to code, and a media tracker felt like the perfect first app — simple to start, but with enough real pieces (a database, a UI, and an API) to actually learn from. It grew into the app you're using now: a friendly place to keep track of what you watch and read.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("About Me", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Hi, I'm Sahil R. — also known as ThatOn3Gu7. I'm a developer who likes to learn by building, and I spend a lot of time in the terminal. When I'm not tinkering with Android apps like this one, I'm usually shipping command-line tools or breaking things on purpose to see how they work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("My Projects", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• ProjectR — a modular Bash terminal setup assistant that installs, inspects, and backs up 240+ tools across Linux, macOS, and Termux.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• UtilityKit — a toolbox of 65 standalone Bash utilities (files, network, git, and more) behind one interactive dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Connect", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    DevLink("GitHub", "ThatOn3Gu7") {
                        openUrl(context, "https://github.com/ThatOn3Gu7")
                    }
                    DevLink("Email", "socialzoneop") {
                        openUrl(context, "mailto:socialzoneop@gmail.com")
                    }
                    DevLink("Instagram", "@thaton3gu7") {
                        openUrl(context, "https://instagram.com/thaton3gu7")
                    }
                    DevLink("TikTok", "@thaton3gu7") {
                        openUrl(context, "https://tiktok.com/@thaton3gu7")
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Credits", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Shouze is built with Jetpack Compose and Kotlin, with a local Room database and AniList's public API powering search and metadata. Thanks to the open-source community that makes projects like this possible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DevLink(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(90.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private suspend fun fetchLatestRelease(repo: String): Pair<String, String>? =
    withContext(Dispatchers.IO) {
        try {
            val conn = (URL("https://api.github.com/repos/$repo/releases/latest")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext null
            }
            val text = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val json = JSONObject(text)
            val tag = json.optString("tag_name")
            val html = json.optString("html_url")
            if (tag.isBlank()) null else (tag to html)
        } catch (_: Exception) {
            null
        }
    }

private fun isNewerVersion(latest: String, current: String): Boolean {
    fun parse(v: String): List<Int> =
        v.trim().trimStart('v', 'V').split('.').map {
            it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
        }
    val a = parse(latest)
    val b = parse(current)
    val n = if (a.size >= b.size) a.size else b.size
    for (i in 0 until n) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

// Uses native M3 ListItem for perfect metrics and accessibility
@Composable
private fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            ) 
        },
        supportingContent = { 
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent // Lets the Card's color show through
        )
    )
}
