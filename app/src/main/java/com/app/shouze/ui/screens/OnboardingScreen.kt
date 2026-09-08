package com.app.shouze.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.shouze.R
import com.app.shouze.data.ThemeMode

/** The same green as the launcher icon background, splash, and intro. */
private val IntroBrandGreen = Color(0xFF1B5E20)

/**
 * First-launch onboarding as three light steps:
 *  1. What Shouze is.
 *  2. Connect AniList (skippable — the app is fully usable signed out).
 *  3. Appearance preference.
 * Every step's action is optional; "Skip" is always one tap away.
 */
@Composable
fun OnboardingScreen(
    isSignedIn: Boolean,
    userName: String?,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onSignIn: () -> Unit,
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val enterAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "enter_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IntroBrandGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp, top = 48.dp)
                .alpha(enterAlpha)
        ) {
            // Top bar: back + progress dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    IconButton(onClick = { step-- }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                StepDots(current = step, count = 3)
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(350)) { it / 3 } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(350)))
                    } else {
                        (slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(tween(350)) { it / 3 } + fadeOut(tween(350)))
                    }
                },
                label = "step_content"
            ) { current ->
                when (current) {
                    0 -> WelcomeStep()
                    1 -> ConnectStep(isSignedIn = isSignedIn, userName = userName, onSignIn = onSignIn)
                    else -> PreferencesStep(themeMode = themeMode, onThemeChange = onThemeChange)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val cta: () -> Unit = when (step) {
                0 -> ({ step = 1 })
                1 -> ({ step = 2 })
                else -> onGetStarted
            }
            val ctaLabel = when (step) {
                0 -> "Continue"
                1 -> if (isSignedIn) "Continue" else "Skip for now"
                else -> "Start tracking"
            }

            Button(
                onClick = cta,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = IntroBrandGreen
                )
            ) {
                Text(ctaLabel, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Skip setup",
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun WelcomeStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(36.dp))
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = "Shouze",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Welcome to Shouze",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Your personal keeper for anime and manga — and it syncs both ways with your AniList account.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConnectStep(isSignedIn: Boolean, userName: String?, onSignIn: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(20.dp))
        FeatureBadge(Icons.Rounded.CloudSync)
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Connect AniList",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        BenefitRow("Your lists, on every device")
        BenefitRow("Progress you tap here shows on AniList instantly")
        BenefitRow("Changes made anywhere sync back automatically")

        Spacer(modifier = Modifier.height(28.dp))

        if (isSignedIn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connected${userName?.let { " as $it" } ?: ""}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            OutlinedButton(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign in with AniList")
            }
        }
    }
}

@Composable
private fun PreferencesStep(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(20.dp))
        FeatureBadge(Icons.Rounded.Sync)
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Make it yours",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Pick a theme — you can change this anytime in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeOption("System", Icons.Rounded.Sync, themeMode == ThemeMode.SYSTEM) { onThemeChange(ThemeMode.SYSTEM) }
            ThemeOption("Light", Icons.Rounded.LightMode, themeMode == ThemeMode.LIGHT) { onThemeChange(ThemeMode.LIGHT) }
            ThemeOption("Dark", Icons.Rounded.DarkMode, themeMode == ThemeMode.DARK) { onThemeChange(ThemeMode.DARK) }
        }
    }
}

@Composable
private fun ThemeOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val borderColor by animateFloatAsState(if (selected) 1f else 0.25f, label = "border")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = if (selected) 0.18f else 0.07f))
            .border(1.5.dp, Color.White.copy(alpha = borderColor), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        Spacer(modifier = Modifier.height(4.dp))
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
private fun FeatureBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun StepDots(current: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 24.dp else 8.dp, label = "dot")
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(alpha = if (i == current) 1f else 0.35f)
                    )
            )
        }
    }
}
