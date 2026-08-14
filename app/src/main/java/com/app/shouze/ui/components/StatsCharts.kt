package com.app.shouze.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (caption != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusDonutChart(
    completed: Int,
    watching: Int,
    reading: Int,
    dropped: Int,
    planToWatch: Int,
    modifier: Modifier = Modifier
) {
    val total = (completed + watching + reading + dropped + planToWatch).coerceAtLeast(1)
    val completedAngle = (completed.toFloat() / total) * 360f
    val watchingAngle = (watching.toFloat() / total) * 360f
    val readingAngle = (reading.toFloat() / total) * 360f
    val droppedAngle = (dropped.toFloat() / total) * 360f

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(total) {
        animatedProgress.animateTo(1f, animationSpec = tween(800))
    }

    val strokeWidth = 28.dp

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension - strokeWidth.toPx()
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2,
                (size.height - diameter) / 2
            )

            var startAngle = -90f

            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = startAngle,
                sweepAngle = completedAngle * animatedProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            startAngle += completedAngle

            drawArc(
                color = primaryColor,
                startAngle = startAngle,
                sweepAngle = watchingAngle * animatedProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            startAngle += watchingAngle

            drawArc(
                color = Color(0xFF2196F3),
                startAngle = startAngle,
                sweepAngle = readingAngle * animatedProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            startAngle += readingAngle

            drawArc(
                color = errorColor,
                startAngle = startAngle,
                sweepAngle = droppedAngle * animatedProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            startAngle += droppedAngle

            drawArc(
                color = outlineColor,
                startAngle = startAngle,
                sweepAngle = (360f - startAngle - 90f) * animatedProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$total",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HorizontalBarChart(
    items: List<ChartItem>,
    modifier: Modifier = Modifier
) {
    val maxCount = items.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (item in items) {
            BarChartItem(item = item, maxCount = maxCount)
        }
    }
}

@Composable
private fun BarChartItem(
    item: ChartItem,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(item.count) {
        animatedProgress.animateTo(
            targetValue = item.count.toFloat() / maxCount,
            animationSpec = tween(600)
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.value)
                    .clip(RoundedCornerShape(4.dp))
                    .background(item.color)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${item.count}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
    }
}

@Composable
fun StatusLegend(
    completed: Int,
    watching: Int,
    reading: Int,
    dropped: Int,
    planToWatch: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendItem(color = Color(0xFF4CAF50), label = "Completed", count = completed)
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Watching", count = watching)
        LegendItem(color = Color(0xFF2196F3), label = "Reading", count = reading)
        LegendItem(color = MaterialTheme.colorScheme.error, label = "Dropped", count = dropped)
        LegendItem(color = MaterialTheme.colorScheme.outline, label = "Plan to Watch", count = planToWatch)
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class ChartItem(
    val label: String,
    val count: Int,
    val color: Color
)
