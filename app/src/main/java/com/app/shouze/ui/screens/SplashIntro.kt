package com.app.shouze.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Branded intro shown right after the system splash on a cold start: pulsing
 * rings around the Shouze mark, then the wordmark. Auto-dismisses; a tap
 * skips it. Runs once per process, so it never delays returning to the app.
 */
@Composable
fun SplashIntro(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1500)
        if (visible) onDismiss()
    }

    val infinite = rememberInfiniteTransition(label = "splash")

    // Expanding rings
    val ringScale by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "ring_scale"
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "ring_alpha"
    )
    // Core pulse
    val coreScale by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "core_scale"
    )
    // Wordmark rise
    var titleAlpha by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        delay(350)
        titleAlpha = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF14141B))
            .clickable { visible = false; onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Rings (two, offset in phase)
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(ringScale)
                .alpha(ringAlpha)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(ringScale * 0.8f)
                .alpha(ringAlpha * 0.8f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The Shouze mark: gradient orb with a bold S
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(coreScale)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF43A047), Color(0xFF1B5E20))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Shouze",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .graphicsLayer {
                        translationY = (1f - titleAlpha) * 40f
                    }
            )
            Text(
                text = "Track. Sync. Binge.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.alpha(titleAlpha)
            )
        }
    }
}
