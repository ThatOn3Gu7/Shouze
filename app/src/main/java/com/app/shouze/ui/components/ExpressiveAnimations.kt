package com.app.shouze.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import kotlin.math.PI
import kotlin.math.sin

// ─── M3 Expressive Motion Specs ───

/** M3 emphasized decelerate — used for entering elements */
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** M3 emphasized accelerate — used for exiting elements */
val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** M3 standard easing — general purpose */
val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** M3 standard decelerate — incoming */
val StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)

/** M3 standard accelerate — outgoing */
val StandardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)

/** Durations following M3 guidelines (in ms) */
object MotionDurations {
    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 500
    const val EXTRA_LONG = 700
}

// ─── Screen Transition Helpers ───

fun fadeSlideInFromBottom(
    duration: Int = MotionDurations.LONG,
    delay: Int = 0
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(
        animationSpec = tween(duration, delay, EmphasizedDecelerate)
    ) + slideInVertically(
        animationSpec = tween(duration, delay, EmphasizedDecelerate),
        initialOffsetY = { it / 5 }
    ) + scaleIn(
        animationSpec = tween(duration, delay, EmphasizedDecelerate),
        initialScale = 0.96f
    )
}

fun fadeSlideOutToBottom(
    duration: Int = MotionDurations.MEDIUM
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(
        animationSpec = tween(duration, easing = EmphasizedAccelerate)
    ) + slideOutVertically(
        animationSpec = tween(duration, easing = EmphasizedAccelerate),
        targetOffsetY = { it / 5 }
    ) + scaleOut(
        animationSpec = tween(duration, easing = EmphasizedAccelerate),
        targetScale = 0.96f
    )
}

fun fadeSlideInFromRight(
    duration: Int = MotionDurations.MEDIUM,
    delay: Int = 0
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(
        animationSpec = tween(duration, delay, StandardDecelerate)
    ) + slideInHorizontally(
        animationSpec = tween(duration, delay, StandardDecelerate),
        initialOffsetX = { it }
    )
}

fun fadeSlideOutToRight(
    duration: Int = MotionDurations.SHORT
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(
        animationSpec = tween(duration, easing = StandardAccelerate)
    ) + slideOutHorizontally(
        animationSpec = tween(duration, easing = StandardAccelerate),
        targetOffsetX = { it }
    )
}

fun fadeSlideInFromLeft(
    duration: Int = MotionDurations.MEDIUM,
    delay: Int = 0
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(
        animationSpec = tween(duration, delay, StandardDecelerate)
    ) + slideInHorizontally(
        animationSpec = tween(duration, delay, StandardDecelerate),
        initialOffsetX = { -it }
    )
}

fun fadeSlideOutToLeft(
    duration: Int = MotionDurations.SHORT
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(
        animationSpec = tween(duration, easing = StandardAccelerate)
    ) + slideOutHorizontally(
        animationSpec = tween(duration, easing = StandardAccelerate),
        targetOffsetX = { -it }
    )
}

/** M3 fade-through transition for sibling screens */
fun fadeThroughEnter(
    duration: Int = MotionDurations.MEDIUM
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(duration, duration / 2, StandardDecelerate))
}

fun fadeThroughExit(
    duration: Int = MotionDurations.SHORT
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(duration, easing = StandardAccelerate))
}

// ─── List Stagger Animation ───

@Composable
fun <T> List<T>.staggeredItemIndex(
    visibleItems: Set<Int> = this.indices.toSet()
): Map<T, Int> = remember(this, visibleItems) {
    this.withIndex()
        .filter { it.index in visibleItems }
        .associate { it.value to it.index }
}

/**
 * M3 staggered list item animation. Items fade + slide up with a staggered delay.
 * Use inside LazyColumn/LazyRow items.
 */
fun Modifier.staggeredItem(
    index: Int,
    totalItems: Int = 20,
    staggerDelayMs: Int = 40,
    baseDurationMs: Int = MotionDurations.MEDIUM
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "staggeredItem"
        properties["index"] = index
    }
) {
    val delay = (index * staggerDelayMs).coerceAtMost(300)
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(baseDurationMs, delay, EmphasizedDecelerate),
        label = "stagger_alpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(baseDurationMs, delay, EmphasizedDecelerate),
        label = "stagger_offset"
    )
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "stagger_scale"
    )

    graphicsLayer {
        alpha = animatedAlpha
        translationY = animatedOffset * 24f
        scaleX = animatedScale
        scaleY = animatedScale
    }
}

// ─── Press Feedback ───

/**
 * M3 expressive press feedback — gentle scale down with spring physics.
 */
fun Modifier.scalePress(
    minScale: Float = 0.97f,
    enabled: Boolean = true
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scalePress"
        properties["minScale"] = minScale
    }
) {
    if (!enabled) return@composed this

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) minScale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "press_scale"
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    isPressed = event.changes.any { it.pressed }
                }
            }
        }
}

// ─── Shimmer Loading ───

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val shimmerHighlight = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerColor)
            .graphicsLayer {
                alpha = 0.6f + 0.4f * sin(shimmerTranslate * 2f * PI.toFloat())
            }
    )
}

// ─── Page Entrance Wrapper ───

/**
 * Wraps screen content with a subtle fade + slide-up entrance animation.
 * Ideal for settings, about, profile, and other detail screens.
 */
@Composable
fun AnimatedPageEntrance(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(MotionDurations.MEDIUM, easing = EmphasizedDecelerate),
        label = "page_alpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(MotionDurations.MEDIUM, easing = EmphasizedDecelerate),
        label = "page_offset"
    )

    Column(
        modifier = modifier
            .alpha(animatedAlpha)
            .graphicsLayer { translationY = animatedOffset * 16f },
        content = content
    )
}

// ─── Bento Stagger ───

/**
 * Staggered entrance for grid/bento items. Each item gets a progressive delay.
 */
@Composable
fun BentoStaggeredEntrance(
    index: Int,
    content: @Composable () -> Unit
) {
    val delay = (index * 80).coerceAtMost(400)
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(MotionDurations.LONG, delay, EmphasizedDecelerate),
        label = "bento_alpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(MotionDurations.LONG, delay, EmphasizedDecelerate),
        label = "bento_scale"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(MotionDurations.LONG, delay, EmphasizedDecelerate),
        label = "bento_offset"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
                translationY = animatedOffset * 20f
            }
    ) {
        content()
    }
}
