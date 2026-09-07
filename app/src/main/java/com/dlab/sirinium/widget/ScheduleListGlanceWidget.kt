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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.color.ColorProvider
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
import com.dlab.sirinium.core.model.LessonType
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.data.local.SiriniumDatabase
import com.dlab.sirinium.data.remote.dto.toDomain
import com.dlab.sirinium.domain.model.Lesson
import com.dlab.sirinium.domain.repository.ScheduleRepository
import com.dlab.sirinium.sync.ScheduleSyncWorker
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Material 3 Expressive List Glance Widget
 * Daily schedule widget with color-coded lesson types, separate classroom and teacher lines, and monochrome icons.
 * Uses reactive Flow observation via WidgetStateHelper so widget updates automatically
 * when the database or target/section preferences change — no app restart needed.
 */
class ScheduleListGlanceWidget : GlanceAppWidget(), KoinComponent {

    override val sizeMode: SizeMode = SizeMode.Exact

    private val database: SiriniumDatabase by inject()
    private val scheduleRepository: ScheduleRepository by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stateFlow = WidgetStateHelper.observeListWidgetState(context, database, scheduleRepository)
        val initialData = stateFlow.first()

        provideContent {
            val state by stateFlow.collectAsState(initialData)
            val size = LocalSize.current
            val fontScale = context.resources.configuration.fontScale

            // Continuous font scale compensation:
            val s = (1f / fontScale.coerceIn(1.0f, 1.7f)).coerceIn(0.62f, 1.0f)

            val isCompact = size.width < 280.dp || size.height < 180.dp || fontScale > 1.15f
            val isExtremeScale = fontScale >= 1.3f || size.width < 240.dp
            val rootCorner = if (isCompact) 16.dp else 22.dp
            val rootPadding = if (isExtremeScale) 6.dp else if (isCompact) 8.dp else 12.dp

            GlanceTheme(colors = GlanceThemeHelper.getColorProviders(context)) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(rootCorner)
                        .padding(rootPadding)
                ) {
                    // Header: Target, Date, and Open App Pill
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(bottom = if (isCompact) 3.dp else 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = state.target,
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = ((if (isCompact) 12.5f else 15f) * s).sp
                                )
                            )
                            Text(
                                text = state.formattedDate.replaceFirstChar { it.uppercase() },
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.outline,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = ((if (isCompact) 9f else 10.5f) * s).sp
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        // Expressive "Open App" Pill
                        val openAppText = if (size.width < 260.dp || fontScale > 1.15f) "↗" else if (size.width < 320.dp) "В приложение" else "В приложение ↗"
                        Box(
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.primaryContainer)
                                .cornerRadius(10.dp)
                                .padding(horizontal = if (openAppText == "↗") 7.dp else 10.dp, vertical = 3.dp)
                                .clickable(actionStartActivity<MainActivity>())
                        ) {
                            Text(
                                text = openAppText,
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = ((if (openAppText == "↗") 12f else 10.5f) * s).sp
                                )
                            )
                        }
                    }

                    if (state.lessons.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "✓",
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = ((if (isCompact) 20f else 26f) * s).sp
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = "На сегодня пар нет",
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
                                        fontSize = ((if (isCompact) 8.5f else 10f) * s).sp
                                    )
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = GlanceModifier.fillMaxSize()
                        ) {
                            items(state.lessons) { lesson ->
                                GlanceLessonItemRow(lesson, state.sectionType, context, isCompact, isExtremeScale, s)
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun GlanceLessonItemRow(
        lesson: Lesson,
        sectionType: String,
        context: Context,
        isCompact: Boolean,
        isExtremeScale: Boolean,
        s: Float
    ) {
        val status = DateTimeUtils.calculateLessonStatus(
            lesson.date,
            lesson.startTime,
            lesson.endTime
        )
        val isOngoing = status is DateTimeUtils.LessonTimeStatus.Ongoing

        val cardBg = when {
            isOngoing -> GlanceTheme.colors.primaryContainer
            else -> GlanceTheme.colors.surfaceVariant
        }

        // Auto-scaled time column width
        val timeColumnWidth = if (isExtremeScale) 44.dp else if (isCompact) 48.dp else 52.dp

        Column(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .cornerRadius(if (isExtremeScale) 10.dp else 14.dp)
                    .padding(
                        horizontal = if (isExtremeScale) 6.dp else if (isCompact) 8.dp else 10.dp,
                        vertical = if (isExtremeScale) 4.dp else if (isCompact) 6.dp else 8.dp
                    )
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Time column (never wraps!)
                Column(
                    modifier = GlanceModifier.width(timeColumnWidth),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = lesson.startTime,
                        maxLines = 1,
                        style = TextStyle(
                            color = if (isOngoing) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                            fontWeight = if (isOngoing) FontWeight.Bold else FontWeight.Medium,
                            fontSize = ((if (isCompact) 11.5f else 12.5f) * s).sp
                        )
                    )
                    Text(
                        text = lesson.endTime,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.outline,
                            fontSize = ((if (isCompact) 8.5f else 9.5f) * s).sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(4.dp))

                // 2. Color-coded spine accent line
                Spacer(
                    modifier = GlanceModifier
                        .width(3.dp)
                        .height(if (isCompact) 26.dp else 32.dp)
                        .background(
                            ColorProvider(
                                day = lesson.lessonType.lightContent,
                                night = lesson.lessonType.darkContent
                            )
                        )
                        .cornerRadius(2.dp)
                )

                Spacer(modifier = GlanceModifier.width(5.dp))

                // 3. Lesson Details column
                Column(
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    // Top meta: Pair number, Color-coded Type badge, Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (lesson.isTimeAnomaly) "!" else "#${lesson.numberPair}",
                            maxLines = 1,
                            style = TextStyle(
                                color = if (lesson.isTimeAnomaly) GlanceTheme.colors.error else GlanceTheme.colors.outline,
                                fontWeight = FontWeight.Bold,
                                fontSize = ((if (lesson.isTimeAnomaly) 11f else 10f) * s).sp
                            )
                        )

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        // Color-coded Lesson Type Badge
                        val lessonTypeTitle = if (isCompact) lesson.lessonType.shortTitle else lesson.lessonType.title
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
                                    fontSize = (8.5f * s).sp
                                )
                            )
                        }

                        if (isOngoing) {
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Box(
                                modifier = GlanceModifier
                                    .background(GlanceTheme.colors.primary)
                                    .cornerRadius(5.dp)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "СЕЙЧАС",
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (7.5f * s).sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(1.dp))

                    // Subject title
                    Text(
                        text = lesson.discipline,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = ((if (isCompact) 11.5f else 12.5f) * s).sp
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(1.dp))

                    // Context-aware Location and Person info
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

                    val combinedInfo = listOf(locationText, personOrGroup).filter { it.isNotBlank() }.joinToString(" • ")
                    if (combinedInfo.isNotBlank()) {
                        Text(
                            text = combinedInfo,
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontWeight = FontWeight.Normal,
                                fontSize = ((if (isCompact) 8.5f else 9.5f) * s).sp
                            )
                        )
                    }
                }
            }

            // Visible neutral gap between blocks
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(if (isCompact) 6.dp else 8.dp))
        }
    }
}

class ScheduleListGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleListGlanceWidget()
}
