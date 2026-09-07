package com.dlab.sirinium.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import com.dlab.sirinium.domain.model.LessonMarkerInfo
import com.dlab.sirinium.core.model.LessonType
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.ui.theme.ExpressiveScheduleTheme
import com.dlab.sirinium.ui.theme.expressiveBounceClick

/**
 * Homework Badge for schedule items
 */
@Composable
fun LessonHomeworkBadge(
    totalTasks: Int,
    completedTasks: Int,
    modifier: Modifier = Modifier
) {
    val isAllDone = totalTasks > 0 && completedTasks == totalTasks
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val bgColor = when {
        isAllDone -> if (isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5)
        else -> if (isDark) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB)
    }
    val contentColor = when {
        isAllDone -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
        else -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    }
    val borderColor = contentColor.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (isAllDone) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Assignment,
            contentDescription = "ДЗ",
            modifier = Modifier.size(11.dp),
            tint = contentColor
        )
        Text(
            text = if (isAllDone) "ДЗ выполнено" else "$completedTasks/$totalTasks ДЗ",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * Notes Badge for schedule items
 */
@Composable
fun LessonNotesBadge(
    notesCount: Int,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bgColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF)
    val contentColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val borderColor = contentColor.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.EditNote,
            contentDescription = "Заметка",
            modifier = Modifier.size(12.dp),
            tint = contentColor
        )
        Text(
            text = if (notesCount > 1) "$notesCount зам." else "Заметка",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * Material 3 Expressive Timeline Item
 * Combines TickTick vertical node timeline with ColorNote tonal card styling.
 */
@Composable
fun ScheduleTimelineItem(
    lesson: Lesson,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onSetAlarmClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    markerInfo: LessonMarkerInfo? = null
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val lessonType = lesson.lessonType

    val typeContainer = if (isDark) lessonType.darkContainer else lessonType.lightContainer
    val typeAccent = if (isDark) lessonType.darkContent else lessonType.lightContent

    val timeStatus = DateTimeUtils.calculateLessonStatus(
        dateStr = lesson.date,
        startTimeStr = lesson.startTime,
        endTimeStr = lesson.endTime
    )

    val isOngoing = timeStatus is DateTimeUtils.LessonTimeStatus.Ongoing
    val isPast = timeStatus is DateTimeUtils.LessonTimeStatus.Finished

    val alpha = if (isPast) 0.65f else 1.0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 1. TickTick Left Spine: Start Time, Node, Connecting Line
        Column(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Start Time
            Text(
                text = lesson.startTime,
                fontSize = 13.sp,
                fontWeight = if (isOngoing) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isOngoing) typeAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Node dot
            Box(
                modifier = Modifier
                    .size(if (isOngoing) 16.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isOngoing -> typeAccent
                            isPast -> MaterialTheme.colorScheme.outlineVariant
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    .border(
                        width = if (isOngoing) 3.dp else 2.dp,
                        color = if (isOngoing) typeAccent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
            )

            // Dynamic Connecting Line
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(
                        when {
                            isOngoing -> typeAccent.copy(alpha = 0.6f)
                            isPast -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    )
            )
            Spacer(modifier = Modifier.height(4.dp))

            // End Time
            Text(
                text = lesson.endTime,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline.copy(alpha = alpha)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. Main Lesson Card (ColorNote & Notion hybrid style)
        Card(
            modifier = Modifier
                .weight(1f)
                .then(cardModifier)
                .clip(RoundedCornerShape(22.dp))
                .expressiveBounceClick(scaleDown = 0.97f, onClick = onClick)
                .border(
                    width = if (isOngoing) 1.5.dp else 1.dp,
                    color = if (isOngoing) typeAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.65f else 0.45f),
                    shape = RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = alpha)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isOngoing) 2.dp else 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top row: Pair number badge + Lesson Type pill + Ongoing live badge + Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Pair number badge or time anomaly mark
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (lesson.isTimeAnomaly) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .padding(horizontal = if (lesson.isTimeAnomaly) 9.dp else 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (lesson.isTimeAnomaly) "!" else "#${lesson.numberPair}",
                                fontSize = if (lesson.isTimeAnomaly) 12.sp else 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (lesson.isTimeAnomaly) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Lesson type pill
                        val chipBg = if (isDark) typeAccent.copy(alpha = 0.16f) else typeContainer
                        val chipBorder = if (isDark) typeAccent.copy(alpha = 0.35f) else lessonType.lightBorder
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = lesson.rawLessonType.ifBlank { lessonType.title },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeAccent
                            )
                        }

                        // If ongoing: show "ИДЁТ" live badge ALWAYS RED
                        if (isOngoing) {
                            val ongoingRed = if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ongoingRed.copy(alpha = 0.14f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ongoingRed)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ИДЁТ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = ongoingRed
                                )
                            }
                        }
                    }

                    // 3 dots indicator (non-functional visual affordance that card can be opened)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Подробнее",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                    }
                }

                // Markers row: Homework and Notes (placed on the next line below lesson type)
                if (markerInfo != null && (markerInfo.hasTasks || markerInfo.hasNotes)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Homework badge
                        if (markerInfo.hasTasks) {
                            LessonHomeworkBadge(
                                totalTasks = markerInfo.totalTasksCount,
                                completedTasks = markerInfo.completedTasksCount
                            )
                        }

                        // Notes badge
                        if (markerInfo.hasNotes) {
                            LessonNotesBadge(
                                notesCount = markerInfo.notesCount
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subject Title
                Text(
                    text = lesson.discipline,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Details Row based on sectionType:
                // Group: classroom + teacher
                // Teacher: classroom + group
                // Classroom: teacher + group
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (lesson.sectionType) {
                        "teacher" -> {
                            if (!lesson.classroom.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = "Аудитория",
                                        modifier = Modifier.size(13.dp),
                                        tint = typeAccent
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = lesson.classroom,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!lesson.group.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.School,
                                        contentDescription = "Группа",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = lesson.group,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        "classroom" -> {
                            if (!lesson.teacher.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = "Преподаватель",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = lesson.teacher,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (!lesson.group.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.School,
                                        contentDescription = "Группа",
                                        modifier = Modifier.size(13.dp),
                                        tint = typeAccent
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = lesson.group,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        else -> { // "group" or fallback
                            if (!lesson.classroom.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = "Аудитория",
                                        modifier = Modifier.size(13.dp),
                                        tint = typeAccent
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = lesson.classroom,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!lesson.teacher.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = "Преподаватель",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = lesson.teacher,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Optional Link quick tap
                if (!lesson.onlineUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lesson.onlineUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ссылка на конференцию",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Light Timeline Item Preview", showBackground = true)
@Composable
private fun ScheduleTimelineItemLightPreview() {
    ExpressiveScheduleTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            ScheduleTimelineItem(
                lesson = Lesson(
                    id = "timeline_1",
                    discipline = "Прикладной искусственный интеллект",
                    teacher = "Смирнов К.А.",
                    classroom = "3.2 Ауд.",
                    startTime = "08:45",
                    endTime = "10:05",
                    date = DateTimeUtils.todayFormatted(),
                    numberPair = 1,
                    lessonType = LessonType.LECTURE,
                    onlineUrl = "https://meet.google.com"
                ),
                isFirst = true,
                isLast = false,
                onClick = {},
                onSetAlarmClick = {}
            )
        }
    }
}

@Preview(name = "Dark Timeline Item Preview", showBackground = true)
@Composable
private fun ScheduleTimelineItemDarkPreview() {
    ExpressiveScheduleTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            ScheduleTimelineItem(
                lesson = Lesson(
                    id = "timeline_2",
                    discipline = "Архитектура распределенных систем",
                    teacher = "Михайлов С.В.",
                    classroom = "1.3К_5",
                    startTime = "10:45",
                    endTime = "12:15",
                    date = DateTimeUtils.todayFormatted(),
                    numberPair = 2,
                    lessonType = LessonType.PRACTICE,
                    onlineUrl = null
                ),
                isFirst = false,
                isLast = true,
                onClick = {},
                onSetAlarmClick = {}
            )
        }
    }
}
