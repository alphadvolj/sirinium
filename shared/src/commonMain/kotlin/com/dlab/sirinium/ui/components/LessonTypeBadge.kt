package com.dlab.sirinium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.core.model.LessonType

@Composable
fun LessonTypeBadge(
    lessonType: LessonType,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    text: String = lessonType.title
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bgColor = if (isDark) lessonType.darkContainer else lessonType.lightContainer
    val contentColor = if (isDark) lessonType.darkContent else lessonType.lightContent
    val borderColor = if (isDark) lessonType.darkBorder else lessonType.lightBorder

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(
                horizontal = if (isCompact) 6.dp else 8.dp,
                vertical = if (isCompact) 2.dp else 4.dp
            )
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = if (isCompact) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
