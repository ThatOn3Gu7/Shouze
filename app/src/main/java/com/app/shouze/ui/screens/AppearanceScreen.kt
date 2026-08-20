package com.app.shouze.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.shouze.data.AppSettings
import com.app.shouze.data.ThemeMode
import com.app.shouze.ui.components.AnimatedPageEntrance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Appearance", 
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
        AnimatedPageEntrance(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            
            // --- Group 1: App Theme ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "App Theme",
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
                    Column(modifier = Modifier.selectableGroup()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> "System Default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                            val icon = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                            }
                            
                            SettingsRadioItem(
                                icon = icon,
                                title = label,
                                selected = settings.themeMode == mode,
                                onClick = { onThemeModeChange(mode) }
                            )
                            
                            if (index < ThemeMode.entries.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }

            val isCurrentlyDark = settings.themeMode == ThemeMode.DARK ||
                    (settings.themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())

            // --- Group 2: Colors & Display ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Colors & Display",
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
                        SettingsSwitchItem(
                            icon = Icons.Rounded.Palette,
                            title = "Dynamic Colors",
                            subtitle = "Use the accent color from your wallpaper (Android 12+)",
                            checked = settings.useDynamicColor,
                            onCheckedChange = onDynamicColorChange
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        
                        SettingsSwitchItem(
                            icon = Icons.Rounded.Contrast,
                            title = "AMOLED Black",
                            subtitle = if (isCurrentlyDark) {
                                "Use pure black backgrounds for OLED screens"
                            } else {
                                "Only available in dark mode"
                            },
                            checked = settings.amoledBlack && isCurrentlyDark,
                            onCheckedChange = onAmoledBlackChange,
                            enabled = isCurrentlyDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRadioItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
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
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null // Handled by the ListItem's clickable modifier
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
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
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null, // Handled by ListItem's clickable
                enabled = enabled
            )
        },
        modifier = Modifier.clickable(
            enabled = enabled,
            onClick = { onCheckedChange(!checked) }
        ),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}
