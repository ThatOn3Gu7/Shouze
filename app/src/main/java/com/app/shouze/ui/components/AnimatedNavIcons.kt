package com.app.shouze.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun BottomFillIcon(
    selected: Boolean,
    outlinedIcon: ImageVector,
    filledIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    val fillFraction by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "fill_anim"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Icon(
            imageVector = outlinedIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            imageVector = filledIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.drawWithContent {
                val topOffset = size.height * (1f - fillFraction)
                clipRect(top = topOffset) {
                    this@drawWithContent.drawContent()
                }
            }
        )
    }
}

@Composable
fun SpinningSearchIcon(
    selected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(selected) {
        if (selected) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.graphicsLayer {
            rotationZ = rotation.value
        }
    )
}