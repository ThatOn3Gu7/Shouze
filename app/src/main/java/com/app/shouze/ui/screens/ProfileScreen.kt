package com.app.shouze.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.ui.components.SafeRemoteImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    username: String,
    profilePictureUri: String?,
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
        contract = ActivityResultContracts.PickVisualMedia(),
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
            TopAppBar(
                title = { Text("Profile") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    if (!profilePictureUri.isNullOrBlank()) {
                        SafeRemoteImage(
                            url = profilePictureUri,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            placeholder = { ProfileInitials(username) },
                            errorContent = { ProfileInitials(username) }
                        )
                    } else {
                        ProfileInitials(username)
                    }
                }
                SmallFloatingActionButton(
                    onClick = { showPictureDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Change picture")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = username.ifBlank { "No username" },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { showUsernameDialog = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit username")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Filled.BarChart,
                        title = "Statistics",
                        subtitle = "View your library insights",
                        onClick = onNavigateToStatistics
                    )
                    HorizontalDivider()
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

    if (showUsernameDialog) {
        var text by remember { mutableStateOf(username) }
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("Edit username") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("Enter a username") }
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
        AlertDialog(
            onDismissRequest = { showPictureDialog = false },
            title = { Text("Change profile picture") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose how to set your profile picture.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPictureDialog = false
                    galleryLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text("From Gallery") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showPictureDialog = false
                        showUrlDialog = true
                    }) { Text("From URL") }
                    if (!profilePictureUri.isNullOrBlank()) {
                        TextButton(onClick = {
                            onProfilePictureChange(null)
                            showPictureDialog = false
                        }) { Text("Remove") }
                    }
                }
            }
        )
    }

    if (showUrlDialog) {
        var url by remember { mutableStateOf(profilePictureUri ?: "") }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Picture from URL") },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    placeholder = { Text("https://...") }
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
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = username.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineLarge,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
