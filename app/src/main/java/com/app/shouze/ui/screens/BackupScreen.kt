package com.app.shouze.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.app.shouze.ui.components.AnimatedPageEntrance

private enum class SnackbarKind { Success, Error, Info }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onBackup: (Uri) -> Unit,
    onRestore: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit = {},
    onImportMalXml: (Uri) -> Unit = {},
    syncMessage: String? = null,
    error: String? = null,
    onClearMessage: () -> Unit = {}
) {
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMalXmlUri by remember { mutableStateOf<Uri?>(null) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var snackbarKind by remember { mutableStateOf(SnackbarKind.Success) }

    LaunchedEffect(error, syncMessage) {
        when {
            error != null -> {
                snackbarMessage = error
                snackbarKind = SnackbarKind.Error
            }
            syncMessage != null -> {
                snackbarMessage = syncMessage
                snackbarKind = SnackbarKind.Success
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            onBackup(uri)
        } else {
            snackbarMessage = "Backup cancelled"
            snackbarKind = SnackbarKind.Info
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> pendingRestoreUri = uri }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExportCsv) }

    val malXmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> pendingMalXmlUri = uri }

    fun launchBackup() {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        backupLauncher.launch("shouze-backup-$stamp.zip")
    }

    fun launchCsv() {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        csvLauncher.launch("shouze-export-$stamp.csv")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Backup & Restore", 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
        AnimatedPageEntrance(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            
            // --- Group 1: Native Backup & Restore ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Shouze Data",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column {
                        BackupActionItem(
                            icon = Icons.Rounded.CloudUpload,
                            title = "Create Backup",
                            subtitle = "Export your library to a zip file",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = ::launchBackup
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        BackupActionItem(
                            icon = Icons.Rounded.CloudDownload,
                            title = "Restore Backup",
                            subtitle = "Import data from a previous backup",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                restoreLauncher.launch(
                                    arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
                                )
                            }
                        )
                    }
                }
            }

            // --- Group 2: Third-Party Export & Import ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "External Formats",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column {
                        BackupActionItem(
                            icon = Icons.Rounded.Description,
                            title = "Export to CSV",
                            subtitle = "Download your library as a spreadsheet",
                            tint = MaterialTheme.colorScheme.tertiary,
                            onClick = ::launchCsv
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        BackupActionItem(
                            icon = Icons.Rounded.UploadFile,
                            title = "Import MAL XML",
                            subtitle = "Import from MyAnimeList XML export",
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                malXmlLauncher.launch(arrayOf("text/xml", "application/xml"))
                            }
                        )
                    }
                }
            }
        }

            AnimatedVisibility(
                visible = snackbarMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                val isError = snackbarKind == SnackbarKind.Error
                val snackbarColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface
                val snackbarContent = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface
                Snackbar(
                    containerColor = snackbarColor,
                    contentColor = snackbarContent,
                    shape = RoundedCornerShape(16.dp),
                    action = {
                        TextButton(onClick = {
                            snackbarMessage = null
                            onClearMessage()
                        }) {
                            Text("Dismiss", color = snackbarContent, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (snackbarKind) {
                                SnackbarKind.Error -> Icons.Rounded.ErrorOutline
                                SnackbarKind.Success -> Icons.Rounded.CheckCircle
                                SnackbarKind.Info -> Icons.Rounded.Cancel
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(snackbarMessage ?: "", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            icon = { Icon(Icons.Rounded.CloudDownload, contentDescription = null) },
            title = { Text("Restore backup?") },
            text = { Text("This will replace all current data with the backup's contents. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri?.let(onRestore)
                    pendingRestoreUri = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
            }
        )
    }

    if (pendingMalXmlUri != null) {
        AlertDialog(
            onDismissRequest = { pendingMalXmlUri = null },
            icon = { Icon(Icons.Rounded.UploadFile, contentDescription = null) },
            title = { Text("Import MAL XML?") },
            text = { Text("This will add all entries from the MAL export to your library. Existing entries with the same title will not be overwritten.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingMalXmlUri?.let(onImportMalXml)
                    pendingMalXmlUri = null
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { pendingMalXmlUri = null }) { Text("Cancel") }
            }
        )
    }
}

// Perfectly matches the ProfileMenuItem and AboutRow from previous screens
@Composable
private fun BackupActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
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
                tint = tint,
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

