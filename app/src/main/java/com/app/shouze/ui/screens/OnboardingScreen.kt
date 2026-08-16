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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onNotNow: () -> Unit
) {
    // State to trigger the staggered entrance animations
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // --- Entrance Animation States ---
    // Logo Animations
    val logoAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    // Title Animations
    val titleAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )
    val titleOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 30.dp,
        animationSpec = tween(800, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "titleOffset"
    )

    // Subtitle Animations
    val subAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "subAlpha"
    )
    val subOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 30.dp,
        animationSpec = tween(800, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "subOffset"
    )

    // Buttons Animations
    val btnAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 450, easing = FastOutSlowInEasing),
        label = "btnAlpha"
    )
    val btnOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 30.dp,
        animationSpec = tween(800, delayMillis = 450, easing = FastOutSlowInEasing),
        label = "btnOffset"
    )

    // --- Background Ambient Animations ---
    val infiniteTransition = rememberInfiniteTransition(label = "bg_gradient")
    val color1 by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary,
        targetValue = MaterialTheme.colorScheme.primaryContainer,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.tertiaryContainer,
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = infiniteRepeatable(tween(5000), RepeatMode.Reverse),
        label = "color2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(color1, color2)))
    ) {
        // Fluid Overlapping Waves in the Background
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(300.dp)
        ) {
            val waveColor = MaterialTheme.colorScheme.onPrimary
            AnimatedWave(
                modifier = Modifier.fillMaxSize(),
                color = waveColor.copy(alpha = 0.1f),
                speed = 4000,
                amplitudeScale = 0.8f
            )
            AnimatedWave(
                modifier = Modifier.fillMaxSize().offset(y = 20.dp),
                color = waveColor.copy(alpha = 0.15f),
                speed = 5000,
                amplitudeScale = 1f,
                direction = -1f // Moves in the opposite direction
            )
            AnimatedWave(
                modifier = Modifier.fillMaxSize().offset(y = 40.dp),
                color = waveColor.copy(alpha = 0.2f),
                speed = 6000,
                amplitudeScale = 1.2f
            )
        }

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp, top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            FloatingLogo(alpha = logoAlpha, scale = logoScale)

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Welcome to Shouze",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                    translationY = titleOffset.toPx()
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your personal keeper for anime, manga, and everything you watch.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = subAlpha
                    translationY = subOffset.toPx()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        alpha = btnAlpha
                        translationY = btnOffset.toPx()
                    },
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Get Started", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onNotNow,
                modifier = Modifier.graphicsLayer {
                    alpha = btnAlpha
                    translationY = btnOffset.toPx()
                }
            ) {
                Text(
                    text = "Not Now",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun FloatingLogo(alpha: Float, scale: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_float"
    )

    // A glassy, floating placeholder orb
    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
                this.translationY = offsetY.dp.toPx()
            }
            .size(120.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
        )
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
        val amplitude = h * 0.15f * amplitudeScale
        val frequency = (1.5f * 2f * PI.toFloat()) / w 

        path.moveTo(0f, amplitude)

        for (x in 0..w.toInt()) {
            val currentPhase = phase * direction
            val y = amplitude * sin(frequency * x + currentPhase) + amplitude
            path.lineTo(x.toFloat(), y)
        }

        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()

        drawPath(path, color = color)
    }
}

// Left intact just in case you need it elsewhere!
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
