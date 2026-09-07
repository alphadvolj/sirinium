package com.dlab.sirinium.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlab.sirinium.core.model.LessonType
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.model.LessonMarkerInfo
import com.dlab.sirinium.ui.theme.ExpressiveScheduleTheme
import com.dlab.sirinium.ui.theme.expressiveBounceClick
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val lessonType = lesson.lessonType

    val isOngoing = timeStatus is DateTimeUtils.LessonTimeStatus.Ongoing
    val isUpcoming = timeStatus is DateTimeUtils.LessonTimeStatus.Upcoming

    // Color tones from lessonType + M3 Expressive
    val typeAccent = if (isDark) lessonType.darkContent else lessonType.lightContent

    // Pulse animation for ongoing lesson badge
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

    // Calculate progress percentage if ongoing
    val progress = remember(timeStatus) {
        if (timeStatus is DateTimeUtils.LessonTimeStatus.Ongoing) {
            try {
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val start = LocalTime.parse(lesson.startTime, formatter).toSecondOfDay()
                val end = LocalTime.parse(lesson.endTime, formatter).toSecondOfDay()
                val now = LocalTime.now().toSecondOfDay()
                ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
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
                // Top Row: Status badge & Time countdown
                val fontScale = context.resources.configuration.fontScale
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

                    // Countdown badge (prominent time left with fontScale compensation)
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

                // Lesson Title (Large Expressive typography)
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

                // Metadata Chips (Type, Room, Teacher)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lesson Type chip
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

                    // Classroom or Group chip depending on sectionType
                    if (lesson.sectionType == "classroom") {
                        if (!lesson.group.isNullOrBlank()) {
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
                        if (!lesson.classroom.isNullOrBlank()) {
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

                    // Pair number or time anomaly badge
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

                // Teacher or Group info row
                if (lesson.sectionType == "teacher") {
                    if (!lesson.group.isNullOrBlank()) {
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
                    if (!lesson.teacher.isNullOrBlank()) {
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

                // Homework and Notes badges
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

                // Progress Bar for ongoing lesson
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

                // Action Buttons Row (Expressive buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Online Link Button if present
                    if (!lesson.onlineUrl.isNullOrBlank()) {
                        Button(
                            onClick = {
                                try {
                                    val uri = Uri.parse(lesson.onlineUrl)
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
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

                    // Add Note Button (disabled/hidden for classroom)
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

                    // Alarm Button
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

@Preview(name = "Light Ongoing Preview", showBackground = true)
@Composable
private fun CurrentLessonHeroCardLightPreview() {
    ExpressiveScheduleTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            CurrentLessonHeroCard(
                lesson = Lesson(
                    id = "preview_1",
                    discipline = "Инженерия машинного обучения и нейросетей",
                    teacher = "Прохоров А.С.",
                    classroom = "3.2 Ауд.",
                    startTime = "10:00",
                    endTime = "11:30",
                    date = DateTimeUtils.todayFormatted(),
                    numberPair = 2,
                    lessonType = LessonType.LAB,
                    onlineUrl = "https://meet.google.com/abc-defg-hij"
                ),
                timeStatus = DateTimeUtils.LessonTimeStatus.Ongoing(minutesRemaining = 45),
                onClick = {},
                onAddNoteClick = {},
                onSetAlarmClick = {}
            )
        }
    }
}

@Preview(name = "Dark Ongoing Preview", showBackground = true)
@Composable
private fun CurrentLessonHeroCardDarkPreview() {
    ExpressiveScheduleTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            CurrentLessonHeroCard(
                lesson = Lesson(
                    id = "preview_2",
                    discipline = "Алгоритмы и структуры данных на Kotlin",
                    teacher = "Волков Д.В.",
                    classroom = "1.3К_5",
                    startTime = "12:00",
                    endTime = "13:30",
                    date = DateTimeUtils.todayFormatted(),
                    numberPair = 3,
                    lessonType = LessonType.PRACTICE,
                    onlineUrl = null
                ),
                timeStatus = DateTimeUtils.LessonTimeStatus.Upcoming(minutesUntilStart = 18),
                onClick = {},
                onAddNoteClick = {},
                onSetAlarmClick = {}
            )
        }
    }
}
