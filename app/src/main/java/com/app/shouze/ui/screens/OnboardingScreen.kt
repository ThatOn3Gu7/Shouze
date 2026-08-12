package com.app.shouze.ui.screens

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onNotNow: () -> Unit
) {
    // Use a solid background color – change to your preferred color
    val backgroundColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacer pushes content down a bit
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Welcome to Shouze",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your personal keeper for anime, manga, and everything you watch.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Get Started", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNotNow) {
                Text(
                    "Not Now",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AnimePlaceholderPage(page: Int, modifier: Modifier = Modifier) {
    val gradients = listOf(
        Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))),
        Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2))),
        Brush.verticalGradient(listOf(Color(0xFFB71C1C), Color(0xFFC62828)))
    )

    Box(
        modifier = modifier.background(gradients[page % gradients.size]),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Anime Image ${page + 1}",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun AnimatedWave(modifier: Modifier = Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val path = Path()
        val w = size.width
        val h = size.height
        val amplitude = h * 0.5f  // gentler bumps
        val frequency = (1.5f * 2f * PI.toFloat()) / w  // 1.5 cycles across width

        // Start at top-left
        path.moveTo(0f, 0f)

        // Draw wavy top edge — sine dips down from y=0, never goes above
        for (x in 0..w.toInt()) {
            val y = amplitude * (1f - sin(frequency * x + phase)) * 0.5f
            path.lineTo(x.toFloat(), y)
        }

        // Close down to bottom-right, bottom-left
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()

        drawPath(path, color = color)
    }
}
