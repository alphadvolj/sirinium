package com.dlab.sirinium.ui.onboarding.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Animated Material 3 Expressive background with organic floating geometric shapes.
 * Subtle, non-distracting ambient motion inspired by Google Pixel setup wizard.
 */
@Composable
fun ExpressiveBackgroundShapes(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive_bg")

    // Slow organic rotation
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_rot_1"
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_rot_2"
    )

    // Breathing scale
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_scale_1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.08f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_scale_2"
    )

    // Gentle vertical and horizontal drifting
    val driftY1 by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_drift_y1"
    )

    val driftX2 by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_drift_x2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        // Shape 1: Top-Right Large Expressive Arch / Pill
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp + driftY1.dp)
                .size(260.dp, 200.dp)
                .graphicsLayer {
                    rotationZ = rotation1 * 0.4f
                    scaleX = scale1
                    scaleY = scale1
                }
                .clip(RoundedCornerShape(topStart = 100.dp, topEnd = 60.dp, bottomStart = 90.dp, bottomEnd = 120.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        )

        // Shape 2: Center-Left Soft Tertiary Asymmetric Blob
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-70).dp + driftX2.dp, y = 30.dp)
                .size(240.dp, 240.dp)
                .graphicsLayer {
                    rotationZ = rotation2 * 0.3f
                    scaleX = scale2
                    scaleY = scale2
                }
                .clip(RoundedCornerShape(topStart = 70.dp, topEnd = 110.dp, bottomStart = 120.dp, bottomEnd = 60.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f))
        )

        // Shape 3: Bottom-Right Secondary Container Pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 50.dp - driftY1.dp)
                .size(210.dp, 210.dp)
                .graphicsLayer {
                    rotationZ = rotation1 * 0.25f
                    scaleX = scale2
                    scaleY = scale2
                }
                .clip(RoundedCornerShape(topStart = 95.dp, topEnd = 80.dp, bottomStart = 60.dp, bottomEnd = 110.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.30f))
        )

        // Shape 4: Subtle Floating Circular Accent
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 30.dp + driftX2.dp, y = 90.dp + driftY1.dp)
                .size(90.dp)
                .scale(scale1)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        )
    }
}
