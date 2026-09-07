package com.dlab.sirinium.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.ui.theme.ExpressiveScheduleTheme

@Composable
fun CountdownBadge(
    status: DateTimeUtils.LessonTimeStatus,
    modifier: Modifier = Modifier
) {
    val text = DateTimeUtils.formatStatusText(status) ?: return
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_badge")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val (badgeBg, badgeContent, dotColor) = when (status) {
        is DateTimeUtils.LessonTimeStatus.Ongoing -> if (isDark) {
            Triple(Color(0xFF450A0A), Color(0xFFF87171), Color(0xFFEF4444))
        } else {
            Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), Color(0xFFEF4444))
        }
        is DateTimeUtils.LessonTimeStatus.Upcoming -> if (isDark) {
            Triple(Color(0xFF064E3B), Color(0xFF34D399), Color(0xFF22C55E))
        } else {
            Triple(Color(0xFFECFDF5), Color(0xFF059669), Color(0xFF10B981))
        }
        DateTimeUtils.LessonTimeStatus.Finished -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.outline
        )
        DateTimeUtils.LessonTimeStatus.FarAway -> return
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(badgeBg)
            .border(1.dp, badgeContent.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status is DateTimeUtils.LessonTimeStatus.Ongoing || status is DateTimeUtils.LessonTimeStatus.Upcoming) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }

        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = badgeContent
        )
    }
}

@Preview(name = "Ongoing Badge Preview", showBackground = true)
@Composable
private fun CountdownBadgeOngoingPreview() {
    ExpressiveScheduleTheme {
        CountdownBadge(status = DateTimeUtils.LessonTimeStatus.Ongoing(minutesRemaining = 35))
    }
}

@Preview(name = "Upcoming Badge Preview", showBackground = true)
@Composable
private fun CountdownBadgeUpcomingPreview() {
    ExpressiveScheduleTheme {
        CountdownBadge(status = DateTimeUtils.LessonTimeStatus.Upcoming(minutesUntilStart = 12))
    }
}
