package com.app.shouze.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.shouze.ui.StatsUiState
import com.app.shouze.ui.components.SafeRemoteImage
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.app.shouze.R
import com.app.shouze.ui.components.AnimatedPageEntrance

val ProfileHeaderFont = FontFamily(
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    username: String,
    profilePictureUri: String?,
    stats: StatsUiState = StatsUiState(),
    onUsernameChange: (String) -> Unit,
    onProfilePictureChange: (String?) -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showPictureDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                onProfilePictureChange(it.toString())
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
              title = { 
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = ProfileHeaderFont,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        AnimatedPageEntrance(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            
            // Hero Avatar — tap the picture itself to change it
            Surface(
                onClick = { showPictureDialog = true },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(132.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                if (!profilePictureUri.isNullOrBlank()) {
                    if (profilePictureUri.startsWith("emoji:")) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profilePictureUri.removePrefix("emoji:"),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        SafeRemoteImage(
                            url = profilePictureUri,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            placeholder = { ProfileInitials(username) },
                            errorContent = { ProfileInitials(username) }
                        )
                    }
                } else {
                    ProfileInitials(username)
                }
            }

            // Interactive Username Pill
            Surface(
                onClick = { showUsernameDialog = true },
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Transparent,
                modifier = Modifier.clip(MaterialTheme.shapes.extraLarge)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = username.ifBlank {
                            if (stats.totalEntries > 0) "Edit Profile" else "Create Profile"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Edit, 
                        contentDescription = "Edit username",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (stats.totalEntries > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ProfileStatCell(
                                value = stats.totalEntries.toString(),
                                label = "Total",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileStatCell(
                                value = stats.totalWatching.toString(),
                                label = "Watching",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ProfileStatCell(
                                value = stats.totalCompleted.toString(),
                                label = "Completed",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileStatCell(
                                value = "%.1f".format(stats.averageRating),
                                label = "Avg score",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Unified Settings-Style Menu Group
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Filled.BarChart,
                        title = "Statistics",
                        subtitle = "View your library insights",
                        onClick = onNavigateToStatistics
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                    ProfileMenuItem(
                        icon = Icons.Filled.Settings,
                        title = "Settings",
                        subtitle = "Appearance, categories, backup",
                        onClick = onNavigateToSettings
                    )
                }
            }
        }
    }

    // --- Dialogs ---

    if (showUsernameDialog) {
        var text by remember { mutableStateOf(username) }
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null) },
            title = { Text("Edit username") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("Enter a username") },
                    shape = MaterialTheme.shapes.large,
                    trailingIcon = {
                        IconButton(onClick = { text = randomAnimeUsername() }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Generate random username"
                            )
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUsernameChange(text.trim())
                    showUsernameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPictureDialog) {
        var avatarTab by remember { mutableStateOf(0) }
        AlertDialog(
            onDismissRequest = { showPictureDialog = false },
            title = { Text("Change profile picture") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Pick a default avatar or upload your own.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    ScrollableTabRow(
                        selectedTabIndex = avatarTab,
                        containerColor = Color.Transparent,
                        edgePadding = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AVATAR_PRESETS.forEachIndexed { index, (name, _) ->
                            Tab(
                                selected = avatarTab == index,
                                onClick = { avatarTab = index },
                                text = { Text(name, style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AVATAR_PRESETS[avatarTab].second.forEach { emoji ->
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onProfilePictureChange("emoji:$emoji")
                                        showPictureDialog = false
                                    },
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = emoji,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            // Horizontally laid out buttons at the bottom: From URL -> From Gallery -> Remove
            confirmButton = {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = {
                        showPictureDialog = false
                        showUrlDialog = true
                    }) { Text("From URL") }

                    TextButton(onClick = {
                        showPictureDialog = false
                        galleryLauncher.launch(arrayOf("image/*"))
                    }) { Text("From Gallery") }

                    if (!profilePictureUri.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                onProfilePictureChange(null)
                                showPictureDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        )
    }

    if (showUrlDialog) {
        var url by remember { mutableStateOf(profilePictureUri?.takeIf { !it.startsWith("emoji:") } ?: "") }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            icon = { Icon(Icons.Rounded.Link, contentDescription = null) },
            title = { Text("Picture from URL") },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    placeholder = { Text("https://...") },
                    shape = MaterialTheme.shapes.large
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onProfilePictureChange(url.trim().ifBlank { null })
                    showUrlDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileInitials(username: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = username.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ProfileMenuItem(
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
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun ProfileStatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val AVATAR_PRESETS = listOf(
    "Chibi" to listOf("🐱", "🐶", "🐰", "🐼", "🦊", "🐲", "🐥", "🐙"),
    "Shonen" to listOf("⚔️", "🔥", "💥", "⚡", "🥷", "🦸", "🚀", "🌪️"),
    "Minimalist" to listOf("⭐", "🌸", "🌟", "🌙", "💎", "🍃", "☀️", "🌈")
)

private val USERNAME_ADJECTIVES = listOf(
    "Shadow", "Neon", "Crimson", "Silent", "Lunar",
    "Electric", "Hidden", "Mystic", "Rapid", "Frozen"
)

private val USERNAME_NOUNS = listOf(
    "Shinobi", "Otaku", "Samurai", "Ronin", "Kitsune",
    "Titan", "Reaper", "Phantom", "Saiyan", "Wolf"
)

private fun randomAnimeUsername(): String {
    val adj = USERNAME_ADJECTIVES.random()
    val noun = USERNAME_NOUNS.random()
    val num = (10..99).random()
    return "$adj$noun$num"
}
