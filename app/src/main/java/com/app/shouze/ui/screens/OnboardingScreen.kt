package com.app.shouze.ui.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.shouze.R
import com.app.shouze.ui.components.EmphasizedDecelerate
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onNotNow: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // ── Orchestrated Entrance with Spring Physics ──
    val logoAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(700, easing = EmphasizedDecelerate),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "logoScale"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(700, delayMillis = 120, easing = EmphasizedDecelerate),
        label = "titleAlpha"
    )
    val titleOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 40f,
        animationSpec = tween(700, delayMillis = 120, easing = EmphasizedDecelerate),
        label = "titleOffset"
    )

    val subAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(700, delayMillis = 240, easing = EmphasizedDecelerate),
        label = "subAlpha"
    )
    val subOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = tween(700, delayMillis = 240, easing = EmphasizedDecelerate),
        label = "subOffset"
    )

    val btnAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 400, easing = EmphasizedDecelerate),
        label = "btnAlpha"
    )
    val btnOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 24f,
        animationSpec = tween(600, delayMillis = 400, easing = EmphasizedDecelerate),
        label = "btnOffset"
    )

    // ── Ambient Background ──
    val infiniteTransition = rememberInfiniteTransition(label = "bg_gradient")
    val color1 by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary,
        targetValue = MaterialTheme.colorScheme.tertiary,
        animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse),
        label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.secondary,
        targetValue = MaterialTheme.colorScheme.primaryContainer,
        animationSpec = infiniteRepeatable(tween(8000), RepeatMode.Reverse),
        label = "color2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(color1, color2)))
    ) {
        // Animated mesh-like waves
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(320.dp)
        ) {
            val waveColor = MaterialTheme.colorScheme.onPrimary
            AnimatedWave(
                modifier = Modifier.fillMaxSize(),
                color = waveColor.copy(alpha = 0.08f),
                speed = 5000,
                amplitudeScale = 0.9f
            )
            AnimatedWave(
                modifier = Modifier.fillMaxSize().offset(y = 24.dp),
                color = waveColor.copy(alpha = 0.12f),
                speed = 6500,
                amplitudeScale = 1.1f,
                direction = -1f
            )
            AnimatedWave(
                modifier = Modifier.fillMaxSize().offset(y = 48.dp),
                color = waveColor.copy(alpha = 0.18f),
                speed = 8000,
                amplitudeScale = 1.3f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(bottom = 52.dp, top = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Logo with glassmorphism container
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = logoAlpha
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing inner glow
                val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        tween(2000, easing = EaseInOutSine),
                        RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                )
                Text(
                    text = "守",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Welcome to Shouze",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                    translationY = titleOffset
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your personal keeper for anime, manga, and everything you watch. Track progress, organize by categories, and never lose track.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = subAlpha
                    translationY = subOffset
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .graphicsLayer {
                        alpha = btnAlpha
                        translationY = btnOffset
                    },
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text("Get Started", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onNotNow,
                modifier = Modifier.graphicsLayer {
                    alpha = btnAlpha
                    translationY = btnOffset
                }
            ) {
                Text(
                    text = "Not Now",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun AnimatedWave(
    modifier: Modifier = Modifier,
    color: Color,
    speed: Int = 5000,
    amplitudeScale: Float = 1f,
    direction: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(speed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val path = Path()
        val w = size.width
        val h = size.height
        val amplitude = h * 0.12f * amplitudeScale
        val frequency = (2f * PI.toFloat()) / w

        path.moveTo(0f, h * 0.3f)

        for (x in 0..w.toInt()) {
            val y = amplitude * sin(frequency * x + phase * direction) + h * 0.3f
            path.lineTo(x.toFloat(), y)
        }

        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()

        drawPath(path, color = color)
    }
}
