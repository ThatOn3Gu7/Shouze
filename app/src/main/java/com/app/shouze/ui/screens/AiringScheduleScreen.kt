package com.app.shouze.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.shouze.data.remote.AiringSchedule
import com.app.shouze.ui.components.SafeRemoteImage
import com.app.shouze.ui.components.rememberTooltipPositionProvider
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiringScheduleScreen(
    schedules: List<AiringSchedule>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onAddToLibrary: (AiringSchedule) -> Unit
) {
    val grouped = remember(schedules) {
        schedules.groupBy { schedule ->
            val date = Date(schedule.airingAt * 1000)
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.DAY_OF_WEEK)
        }.toSortedMap()
    }

    val todayDayOfWeek = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) }
    val dateLine = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val dayNames = mapOf(
        Calendar.SUNDAY to "Sunday",
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Airing Schedule",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            if (isLoading && schedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null && schedules.isEmpty()) {
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
                            text = "Failed to load schedule", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = error, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRefresh,
                            shape = MaterialTheme.shapes.large
                        ) { 
                            Text("Retry") 
                        }
                    }
                }
            } else if (grouped.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No airing anime found", 
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    grouped.forEach { (day, daySchedules) ->
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = dayNames[day] ?: "Unknown",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (day == todayDayOfWeek) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        items(daySchedules, key = { it.id }) { schedule ->
                            PremiumAiringScheduleCard(
                                schedule = schedule,
                                onAdd = { onAddToLibrary(schedule) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatChipInfo(format: String): Pair<ImageVector, String>? = when (format.uppercase()) {
    "TV" -> Icons.Rounded.Tv to "TV"
    "TV_SHORT" -> Icons.Rounded.Timer to "Short"
    "ONA" -> Icons.Rounded.Public to "ONA"
    "MOVIE" -> Icons.Rounded.Movie to "Movie"
    "OVA", "SPECIAL" -> Icons.Rounded.Star to format.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumAiringScheduleCard(
    schedule: AiringSchedule,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val time = remember(schedule.airingAt) {
        timeFormat.format(Date(schedule.airingAt * 1000))
    }
    val title = schedule.media.title.english ?: schedule.media.title.romaji ?: "Unknown"

    Card(
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
            // Elevated, rounded poster image
            Surface(
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(2f / 3f),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp
            ) {
                if (!schedule.media.coverImage?.large.isNullOrBlank()) {
                    SafeRemoteImage(
                        url = schedule.media.coverImage!!.large!!,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title.firstOrNull()?.uppercase() ?: "?",
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Premium Schedule Pill
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Ep ${schedule.episode} · $time",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    if (!schedule.media.format.isNullOrBlank()) {
                        val formatInfo = formatChipInfo(schedule.media.format)
                        if (formatInfo != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = formatInfo.first,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = formatInfo.second,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            TooltipBox(
                tooltip = {
                    PlainTooltip { Text("Add to library") }
                },
                state = rememberTooltipState(),
                positionProvider = rememberTooltipPositionProvider(),
                focusable = false
            ) {
                FilledTonalIconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add, 
                        contentDescription = "Add to library",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
