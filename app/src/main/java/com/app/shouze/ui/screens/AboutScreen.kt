package com.app.shouze.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import com.app.shouze.data.GITHUB_REPO
import com.app.shouze.data.SettingsRepository
import com.app.shouze.data.UpdateDownloader
import com.app.shouze.data.UpdateFrequency
import com.app.shouze.data.UpdateScheduler
import com.app.shouze.data.currentVersionName
import com.app.shouze.data.fetchLatestRelease
import com.app.shouze.data.isNewerVersion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val APP_VERSION = "6.5.0"
private const val FEEDBACK_EMAIL = "recoveringdotcom@gmail.com"

// Expanded UpdateState to hold release notes and the direct APK URL
private sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    object Error : UpdateState
    data class Available(
        val tag: String, 
        val htmlUrl: String, 
        val releaseNotes: String, 
        val directApkUrl: String?
    ) : UpdateState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    settingsRepository: SettingsRepository
) {
    val context = LocalContext.current
    val settings by settingsRepository.settings.collectAsState()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var showLicenses by remember { mutableStateOf(false) }
    var showAboutDev by remember { mutableStateOf(false) }
    var showUpdateMenu by remember { mutableStateOf(false) } // State for the new Bottom Sheet
    
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var checkButtonLabel by remember { mutableStateOf("Check Now") }
    val scope = rememberCoroutineScope()

    fun checkForUpdates() {
        scope.launch {
            updateState = UpdateState.Checking
            
            // Artificial delay for UX (Labor Illusion) - prevents instant flashing
            delay(1200) 
            
            val latest = fetchLatestRelease(GITHUB_REPO)
            if (latest == null) {
                updateState = UpdateState.Error
                checkButtonLabel = "Retry"
                return@launch
            }
            
            val current = currentVersionName(context)
            if (current == null) {
                updateState = UpdateState.UpToDate
                checkButtonLabel = "Check Again"
                return@launch
            }
            
            updateState = if (isNewerVersion(latest.tag, current)) {
                UpdateState.Available(latest.tag, latest.htmlUrl, latest.body, latest.apkUrl)
            } else {
                checkButtonLabel = "Check Again"
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

            Text(
                text = "Your personal keeper for anime, manga, and everything you watch. Track your progress, organize by categories, and never lose track of what you're watching or reading.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                        title = "Updates & Settings",
                        subtitle = "Check for updates and manage frequency",
                        onClick = { 
                            showUpdateMenu = true 
                            requestNotificationPermission()
                            if (updateState == UpdateState.Idle) checkForUpdates()
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

    // --- New Update Menu Bottom Sheet ---
    if (showUpdateMenu) {
        ModalBottomSheet(
            onDismissRequest = { showUpdateMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            )
        ) {
            UpdateMenuContent(
                state = updateState,
                checkLabel = checkButtonLabel,
                onCheckForUpdates = { checkForUpdates() },
                onDownloadUpdate = { apkUrl ->
                    requestNotificationPermission()
                    if (UpdateDownloader.enqueue(context, apkUrl)) {
                        Toast.makeText(context, "Download started — check your notifications", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Couldn't start the download", Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenBrowser = { url -> openUrl(context, url) },
                frequency = settings.updateFrequency,
                onFrequencyChange = { frequency ->
                    requestNotificationPermission()
                    settingsRepository.setUpdateFrequency(frequency)
                    UpdateScheduler.apply(context, frequency)
                }
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
                    "WorkManager — Apache-2.0",
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
    if (showAboutDev) { AboutDevDialog(onDismiss = { showAboutDev = false }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateMenuContent(
    state: UpdateState,
    checkLabel: String,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
    frequency: UpdateFrequency,
    onFrequencyChange: (UpdateFrequency) -> Unit
) {
    val frequencyLabels = mapOf(
        UpdateFrequency.EVERY_LAUNCH to "Every Launch",
        UpdateFrequency.WEEKLY to "Weekly",
        UpdateFrequency.BI_WEEKLY to "Bi-weekly",
        UpdateFrequency.NEVER to "Never"
    )
    var showFrequencyDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp) // Extra padding for bottom navigation bar
    ) {
        Text(
            text = "App Updates",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Status Card with smooth height expansion
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Crossfade status header content smoothly
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + expandVertically()) togetherWith
                                (fadeOut(animationSpec = tween(200)) + shrinkVertically())
                    },
                    label = "status_transition"
                ) { targetState ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when(targetState) {
                                is UpdateState.Checking -> Icons.Default.Sync
                                is UpdateState.Available -> Icons.Default.NewReleases
                                is UpdateState.UpToDate -> Icons.Default.CheckCircle
                                is UpdateState.Error -> Icons.Default.Error
                                is UpdateState.Idle -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = if (targetState is UpdateState.Error) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (targetState) {
                                is UpdateState.Checking -> "Checking GitHub..."
                                is UpdateState.Available -> "Version ${targetState.tag} is available!"
                                is UpdateState.UpToDate -> "Shouze is up to date."
                                is UpdateState.Error -> "Failed to check for updates."
                                is UpdateState.Idle -> "Ready to check."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (targetState is UpdateState.Error) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Color.Unspecified
                            }
                        )
                    }
                }
                // Animated expand/collapse for release notes section
                AnimatedVisibility(
                    visible = state is UpdateState.Available,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val availableState = state as? UpdateState.Available
                    if (availableState != null) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Release Notes:", style = MaterialTheme.typography.labelLarge)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .padding(top = 8.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = availableState.releaseNotes.ifBlank { "No release notes provided." },
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState()),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onOpenBrowser(availableState.htmlUrl) }) {
                                    Text("View on GitHub")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (availableState.directApkUrl != null) {
                                    Button(onClick = { onDownloadUpdate(availableState.directApkUrl) }) {
                                        Text("Update Now")
                                    }
                                } else {
                                    Button(onClick = { onOpenBrowser(availableState.htmlUrl) }) {
                                        Text("Download Manually")
                                    }
                                }
                            }
                        }
                    }
                }

                // Animated visibility for Check Now / Retry / Check Again button
                AnimatedVisibility(
                    visible = state !is UpdateState.Checking && state !is UpdateState.Available,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCheckForUpdates,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(checkLabel)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        // Preferences Section
        Text(
            text = "Preferences",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // Auto-check frequency dropdown
        ExposedDropdownMenuBox(
            expanded = showFrequencyDropdown,
            onExpandedChange = { showFrequencyDropdown = !showFrequencyDropdown }
        ) {
            OutlinedTextField(
                value = frequencyLabels[frequency] ?: "Weekly",
                onValueChange = {},
                readOnly = true,
                label = { Text("Auto-check frequency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFrequencyDropdown) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = showFrequencyDropdown,
                onDismissRequest = { showFrequencyDropdown = false }
            ) {
                UpdateFrequency.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(frequencyLabels[option] ?: option.name) },
                        onClick = {
                            showFrequencyDropdown = false
                            onFrequencyChange(option)
                        }
                    )
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
                    DevLink("Email", "socialzoneop@gmail.com") {
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

@Composable
private fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(text = subtitle, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
        trailingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
