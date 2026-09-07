package com.dlab.sirinium.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dlab.sirinium.MainActivity
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.data.local.SiriniumDatabase
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.repository.ScheduleRepository
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Material 3 Expressive Compact / Hero Glance Widget
 * Visual centerpiece widget displaying active/upcoming lesson with color-coded type tags and separate info lines.
 */
class ScheduleGlanceWidget : GlanceAppWidget(), KoinComponent {

    override val sizeMode: SizeMode = SizeMode.Exact

    private val database: SiriniumDatabase by inject()
    private val scheduleRepository: ScheduleRepository by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stateFlow = WidgetStateHelper.observeCompactWidgetState(context, database, scheduleRepository)
        val initialData = stateFlow.first()

        provideContent {
            val state by stateFlow.collectAsState(initialData)
            val size = LocalSize.current
            val fontScale = context.resources.configuration.fontScale
            val isCompact = size.height < 180.dp || size.width < 180.dp || fontScale > 1.15f
            val rootCorner = if (isCompact) 18.dp else 22.dp
            val rootPadding = if (isCompact) 8.dp else 10.dp

            GlanceTheme(colors = GlanceThemeHelper.getColorProviders(context)) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(rootCorner)
                        .padding(rootPadding)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.TopStart
                ) {
                    val lesson = state.nextLesson
                    if (lesson == null) {
                        EmptyWidgetContent(state.target, isCompact, fontScale)
                    } else {
                        ActiveLessonWidgetContent(
                            lesson = lesson,
                            target = state.target,
                            sectionType = state.sectionType,
                            context = context,
                            size = size,
                            fontScale = fontScale
                        )
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun EmptyWidgetContent(target: String, isCompact: Boolean, fontScale: Float) {
        val s = if (fontScale > 1.2f) 0.85f else if (fontScale > 1.05f) 0.92f else 1f
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Monochrome Target badge
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = target,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = ((if (isCompact) 10f else 11f) * s).sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(if (isCompact) 4.dp else 6.dp))

            // Monochrome check icon
            Text(
                text = "✓",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = ((if (isCompact) 20f else 24f) * s).sp
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Все пары завершены",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = ((if (isCompact) 11f else 13f) * s).sp
                )
            )
            Text(
                text = "Расписание свободно",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.outline,
                    fontSize = ((if (isCompact) 9f else 10f) * s).sp
                )
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun ActiveLessonWidgetContent(
        lesson: Lesson,
        target: String,
        sectionType: String,
        context: Context,
        size: androidx.compose.ui.unit.DpSize,
        fontScale: Float
    ) {
        val width = size.width
        val height = size.height
        val densityDpi = context.resources.configuration.densityDpi

        // Continuous scaling formula:
        // Compensates for system font scaling (1.15x -> 0.88, 1.3x -> 0.77, 1.5x -> 0.67)
        // while also considering narrow widget width / high density.
        val s = (1f / fontScale.coerceIn(1.0f, 1.7f)).coerceIn(0.62f, 1.0f)

        val isTightHeight = height < 120.dp || fontScale > 1.35f
        val isTightWidth = width < 220.dp || fontScale > 1.05f
        val isExtremeScale = fontScale >= 1.4f || height < 110.dp || width < 120.dp

        val status = DateTimeUtils.calculateLessonStatus(
            lesson.date,
            lesson.startTime,
            lesson.endTime
        )
        val isOngoing = status is DateTimeUtils.LessonTimeStatus.Ongoing
        val parsedLessonDate = DateTimeUtils.parseDate(lesson.date)
        val today = LocalDate.now()
        val daysUntilLesson = if (parsedLessonDate != null) {
            java.time.temporal.ChronoUnit.DAYS.between(today, parsedLessonDate)
        } else 0L

        val isFutureDay = daysUntilLesson > 0L
        val dayLabel = when (daysUntilLesson) {
            1L -> "ЗАВТРА"
            2L -> "+2 ДНЯ"
            in 3L..365L -> parsedLessonDate?.let { DateTimeUtils.formatDateShort(it) } ?: "+${daysUntilLesson} ДН"
            else -> null
        }

        val rawCountdownText = when {
            isFutureDay -> when (daysUntilLesson) {
                1L -> "Завтра в ${lesson.startTime}"
                2L -> "Послезавтра в ${lesson.startTime}"
                else -> "${DateTimeUtils.formatRelativeDate(lesson.date, today)} в ${lesson.startTime}"
            }
            else -> DateTimeUtils.formatStatusText(status) ?: "В ${lesson.startTime}"
        }
        val cleanCountdown = rawCountdownText.replace("⏱", "").trim()

        // Status Badge text:
        // If today and ongoing -> "СЕЙЧАС"
        // If future day -> dayLabel (e.g. "ЗАВТРА", "+2 ДНЯ", "12.09")
        // Else if tight width -> "СЛЕД."
        // Else -> "СЛЕДУЮЩАЯ"
        val statusBadgeText = when {
            isOngoing -> "СЕЙЧАС"
            isFutureDay && dayLabel != null -> dayLabel
            isTightWidth -> "СЛЕД."
            else -> "СЛЕДУЮЩАЯ"
        }

        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // 1. Top Row: Monochrome Target Chip & Live Status Pill
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Monochrome Target Chip
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(6.dp)
                        .padding(horizontal = if (isExtremeScale) 4.dp else 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = target,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = ((if (isTightWidth) 9.5f else 11f) * s).sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Status Badge
                Box(
                    modifier = GlanceModifier
                        .background(
                            if (isOngoing) GlanceTheme.colors.primary
                            else GlanceTheme.colors.primaryContainer
                        )
                        .cornerRadius(6.dp)
                        .padding(horizontal = if (isExtremeScale) 4.dp else 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusBadgeText,
                        maxLines = 1,
                        style = TextStyle(
                            color = if (isOngoing) GlanceTheme.colors.onPrimary
                            else GlanceTheme.colors.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = ((if (isTightWidth) 8.5f else 10f) * s).sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(if (isTightHeight) 2.dp else 4.dp))

            // 2. Time & Pair & Color-coded Type Tag
            // In tight width or high font scale, format time compactly ("08:45-10:20" vs "08:45–10:20")
            val timeRangeText = if (isTightWidth) "${lesson.startTime}-${lesson.endTime}" else "${lesson.startTime}–${lesson.endTime}"

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeRangeText,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = ((if (isExtremeScale) 10.5f else if (isTightWidth) 11.5f else 13f) * s).sp
                    )
                )
                Spacer(modifier = GlanceModifier.width(3.dp))
                Text(
                    text = if (lesson.isTimeAnomaly) "!" else "#${lesson.numberPair}",
                    maxLines = 1,
                    style = TextStyle(
                        color = if (lesson.isTimeAnomaly) GlanceTheme.colors.error else GlanceTheme.colors.outline,
                        fontWeight = if (lesson.isTimeAnomaly) FontWeight.Bold else FontWeight.Medium,
                        fontSize = ((if (lesson.isTimeAnomaly) 11f else 9.5f) * s).sp
                    )
                )
                Spacer(modifier = GlanceModifier.width(3.dp))

                // Color-coded Lesson Type Badge (always use shortTitle in compact widget to prevent truncation)
                val lessonTypeTitle = lesson.lessonType.shortTitle
                Box(
                    modifier = GlanceModifier
                        .background(
                            GlanceThemeHelper.adaptiveColor(
                                context = context,
                                dayColor = lesson.lessonType.lightContainer,
                                nightColor = lesson.lessonType.darkContainer
                            )
                        )
                        .cornerRadius(5.dp)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = lessonTypeTitle,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceThemeHelper.adaptiveColor(
                                context = context,
                                dayColor = lesson.lessonType.lightContent,
                                nightColor = lesson.lessonType.darkContent
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = ((if (isExtremeScale) 7.5f else 8.5f) * s).sp
                        )
                    )
                }
            }

            // 3. Countdown banner only if vertical space is spacious and font isn't large
            if (!isTightHeight && !isExtremeScale && height >= 160.dp) {
                Spacer(modifier = GlanceModifier.height(3.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(
                            if (isOngoing) GlanceTheme.colors.primaryContainer
                            else GlanceTheme.colors.secondaryContainer
                        )
                        .cornerRadius(6.dp)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "• $cleanCountdown",
                        maxLines = 1,
                        style = TextStyle(
                            color = if (isOngoing) GlanceTheme.colors.onPrimaryContainer
                            else GlanceTheme.colors.onSecondaryContainer,
                            fontWeight = FontWeight.Medium,
                            fontSize = (9.5f * s).sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(if (isTightHeight) 1.dp else 3.dp))

            // 4. Discipline title (allows 2 lines on normal 2x2 widget height)
            Text(
                text = lesson.discipline,
                maxLines = if (isTightHeight || isExtremeScale) 1 else 2,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = ((if (isExtremeScale) 10.5f else if (isTightWidth) 11.5f else 13f) * s).sp
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            // 5. Context-aware Location and Teacher
            val effectiveSection = when {
                sectionType == "teacher" || lesson.sectionType == "teacher" -> "teacher"
                sectionType == "classroom" || lesson.sectionType == "classroom" -> "classroom"
                else -> "group"
            }

            val locationText = if (lesson.classroom.isNotBlank()) "Ауд. ${lesson.classroom}" else ""
            val rawPerson = when (effectiveSection) {
                "teacher" -> lesson.group.ifBlank { lesson.teacher }
                "classroom" -> listOfNotNull(lesson.teacher.takeIf { it.isNotBlank() }, lesson.group.takeIf { it.isNotBlank() }).joinToString(", ")
                else -> lesson.teacher
            }
            val personOrGroup = DateTimeUtils.formatShortPerson(rawPerson)

            if (isTightHeight || isExtremeScale || height < 170.dp) {
                val combinedBottom = listOf(locationText, personOrGroup).filter { it.isNotBlank() }.joinToString(" • ")
                if (combinedBottom.isNotBlank()) {
                    Text(
                        text = combinedBottom,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = ((if (isExtremeScale) 8.5f else 9.5f) * s).sp
                        )
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    if (locationText.isNotBlank()) {
                        Text(
                            text = locationText,
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = (10.5f * s).sp
                            )
                        )
                    }
                    if (personOrGroup.isNotBlank()) {
                        Text(
                            text = personOrGroup,
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.outline,
                                fontWeight = FontWeight.Normal,
                                fontSize = (9.5f * s).sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class ScheduleGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleGlanceWidget()
}
