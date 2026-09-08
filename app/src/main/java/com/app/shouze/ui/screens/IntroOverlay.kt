package com.app.shouze.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.app.shouze.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Raster size for the punched-out mark. Matches the launcher foreground's native art. */
private const val MARK_RASTER_PX = 512

/** Same green as the launcher's adaptive-icon background and the system splash —
 *  the intro is a seamless continuation of the splash, with no visible cut. */
private val IntroBackground = Color(0xFF1B5E20)

// Slow initial swell, then rapid acceleration past the camera.
private val WarpSpeedEasing = CubicBezierEasing(0.7f, 0f, 0.15f, 1f)

/**
 * Cold-start intro that continues the system splash.
 *
 * The mark is *punched out* of an opaque green ground with [BlendMode.DstOut],
 * so what animates is a hole revealing the app below — never a drawn logo.
 * The ground swells gently, then accelerates past the camera and the app is
 * simply there. Runs once per cold start; input is consumed while it plays.
 */
@Composable
fun IntroOverlay(onFinished: () -> Unit = {}) {
    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return

    // Rasterize through the DRAWABLE pipeline, not ImageBitmap.imageResource():
    // that helper only decodes raster assets and throws on VectorDrawables.
    // ContextCompat.getDrawable().toBitmap() handles vectors and PNGs alike.
    val context = LocalContext.current
    val logoBitmap: ImageBitmap = remember {
        val drawable = checkNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher_foreground)) {
            "Intro mark drawable could not be loaded"
        }
        drawable.toBitmap(width = MARK_RASTER_PX, height = MARK_RASTER_PX).asImageBitmap()
    }

    val zoomScale = remember { Animatable(1f) }
    val backgroundAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(80)
        val duration = 650
        coroutineScope {
            launch {
                zoomScale.animateTo(
                    targetValue = 18f,
                    animationSpec = tween(duration, easing = WarpSpeedEasing),
                )
            }
            launch {
                delay(200)
                backgroundAlpha.animateTo(0f, tween(duration - 200))
            }
        }
        dismissed = true
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = backgroundAlpha.value
            }
            .background(IntroBackground)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) { awaitPointerEvent().changes.forEach { it.consume() } }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = zoomScale.value
            val scaledWidth = (logoBitmap.width * scale).toInt()
            val scaledHeight = (logoBitmap.height * scale).toInt()
            val left = (size.width - scaledWidth) / 2f
            val top = (size.height - scaledHeight) / 2f
            drawImage(
                image = logoBitmap,
                dstOffset = IntOffset(left.toInt(), top.toInt()),
                dstSize = IntSize(scaledWidth, scaledHeight),
                blendMode = BlendMode.DstOut
            )
        }
    }
}
