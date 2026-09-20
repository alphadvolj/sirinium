package com.dlab.sirinium.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.VideoCameraFront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonMarkerInfo
import com.dlab.sirinium.platform.LocalPlatformActions
import com.dlab.sirinium.ui.theme.expressiveBounceClick

/**
 * Material 3 Expressive Current Lesson Hero Card
 * Visual centerpiece for the active ongoing lesson or immediately next upcoming lesson.
 */
@Composable
fun CurrentLessonHeroCard(
    lesson: Lesson,
    timeStatus: DateTimeUtils.LessonTimeStatus,
    onClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onSetAlarmClick: () -> Unit,
    modifier: Modifier = Modifier,
    markerInfo: LessonMarkerInfo? = null
) {
    val platformActions = LocalPlatformActions.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val lessonType = lesson.lessonType

    val isOngoing = timeStatus is DateTimeUtils.LessonTimeStatus.Ongoing
    val isUpcoming = timeStatus is DateTimeUtils.LessonTimeStatus.Upcoming

    val typeAccent = if (isDark) lessonType.darkContent else lessonType.lightContent

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val progress = remember(timeStatus) {
        if (timeStatus is DateTimeUtils.LessonTimeStatus.Ongoing) {
            try {
                val start = DateTimeUtils.timeToMinutes(lesson.startTime)
                val end = DateTimeUtils.timeToMinutes(lesson.endTime)
                val now = DateTimeUtils.currentTimeInMinutes()
                if (end > start) {
                    ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
                } else 0.5f
            } catch (_: Exception) {
                0.5f
            }
        } else 0f
    }

    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceContainer
                 else MaterialTheme.colorScheme.surfaceContainerLowest

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .expressiveBounceClick(scaleDown = 0.98f, onClick = onClick)
            .border(
                width = if (isOngoing) 1.5.dp else 1.dp,
                color = if (isOngoing) typeAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.65f else 0.5f),
                shape = RoundedCornerShape(26.dp)
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column {
                val fontScale = LocalDensity.current.fontScale
                val s = (1f / fontScale.coerceIn(1.0f, 1.7f)).coerceIn(0.70f, 1.0f)
                val isLargeFont = fontScale > 1.15f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge (CircleShape pill)
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isOngoing) typeAccent.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .padding(
                                horizontal = if (isLargeFont) 8.dp else 12.dp,
                                vertical = if (isLargeFont) 4.dp else 6.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isLargeFont) 6.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOngoing) typeAccent.copy(alpha = pulseAlpha)
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                        Spacer(modifier = Modifier.width(if (isLargeFont) 4.dp else 6.dp))
                        Text(
                            text = if (isOngoing) "ИДЕТ СЕЙЧАС" else "СЛЕДУЮЩАЯ ПАРА",
                            fontSize = (11f * s).sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            color = if (isOngoing) typeAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    val countdownText = when (timeStatus) {
                        is DateTimeUtils.LessonTimeStatus.Ongoing -> {
                            if (fontScale >= 1.25f) "${timeStatus.minutesRemaining} мин"
                            else "Осталось ${timeStatus.minutesRemaining} мин"
                        }
                        is DateTimeUtils.LessonTimeStatus.Upcoming -> {
                            if (fontScale >= 1.25f) "Через ${timeStatus.minutesUntilStart} мин"
                            else "Через ${timeStatus.minutesUntilStart} мин"
                        }
                        else -> "${lesson.startTime} – ${lesson.endTime}"
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isOngoing) typeAccent.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .padding(
                                horizontal = if (isLargeFont) 8.dp else 12.dp,
                                vertical = if (isLargeFont) 4.dp else 6.dp
                            )
                    ) {
                        Text(
                            text = countdownText,
                            fontSize = (12f * s).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = if (isOngoing) typeAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lesson Title
                Text(
                    text = lesson.discipline,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) typeAccent.copy(alpha = 0.16f) else lessonType.lightContainer,
                        border = BorderStroke(1.dp, if (isDark) typeAccent.copy(alpha = 0.35f) else lessonType.lightBorder),
                        contentColor = typeAccent
                    ) {
                        Text(
                            text = lesson.rawLessonType.ifBlank { lessonType.title },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (lesson.sectionType == "classroom") {
                        if (lesson.group.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = "Группа",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lesson.group,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        if (lesson.classroom.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = "Аудитория",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lesson.classroom,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (lesson.isTimeAnomaly) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(
                            text = "#${lesson.numberPair} пара",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (lesson.sectionType == "teacher") {
                    if (lesson.group.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = "Группа",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = lesson.group,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    if (lesson.teacher.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Преподаватель",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = lesson.teacher,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (markerInfo != null && (markerInfo.hasTasks || markerInfo.hasNotes)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (markerInfo.hasTasks) {
                            LessonHomeworkBadge(
                                totalTasks = markerInfo.totalTasksCount,
                                completedTasks = markerInfo.completedTasksCount
                            )
                        }
                        if (markerInfo.hasNotes) {
                            LessonNotesBadge(
                                notesCount = markerInfo.notesCount
                            )
                        }
                    }
                }

                if (isOngoing) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lesson.startTime,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "До конца ${(timeStatus as? DateTimeUtils.LessonTimeStatus.Ongoing)?.minutesRemaining ?: 0} мин (${(progress * 100).toInt()}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeAccent
                            )
                            Text(
                                text = lesson.endTime,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = typeAccent,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!lesson.onlineUrl.isNullOrBlank()) {
                        Button(
                            onClick = {
                                platformActions.openUrl(lesson.onlineUrl)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = typeAccent
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideoCameraFront,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Подключиться",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (lesson.sectionType != "classroom") {
                        FilledTonalButton(
                            onClick = onAddNoteClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Заметка",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSetAlarmClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = if (lesson.sectionType == "classroom" && lesson.onlineUrl.isNullOrBlank()) {
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp)
                        } else if (lesson.sectionType == "classroom") {
                            Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                        } else {
                            Modifier.size(42.dp)
                        },
                        contentPadding = if (lesson.sectionType == "classroom") {
                            androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        } else {
                            androidx.compose.foundation.layout.PaddingValues(0.dp)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = "Уведомление о паре",
                            modifier = Modifier.size(18.dp)
                        )
                        if (lesson.sectionType == "classroom") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Напомнить",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
