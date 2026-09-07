package com.dlab.sirinium.ui.schedule

import com.dlab.sirinium.core.model.LessonType
import com.dlab.sirinium.core.util.DateTimeUtils
import com.dlab.sirinium.domain.model.Lesson
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class NextLessonInfo(
    val title: String,
    val lesson: Lesson
)

object NextLessonHelper {

    /**
     * Determines whether two lessons share the same instructional type.
     */
    fun isSameType(l1: Lesson, l2: Lesson): Boolean {
        if (l1.lessonType == l2.lessonType && l1.lessonType != LessonType.OTHER) return true
        val r1 = l1.rawLessonType.trim()
        val r2 = l2.rawLessonType.trim()
        if (r1.isNotBlank() && r2.isNotBlank() && r1.equals(r2, ignoreCase = true)) return true
        return l1.lessonType == l2.lessonType
    }

    /**
     * Generates a Russian title for next lesson of specific type (e.g. "Следующая лекция по предмету").
     */
    fun getNextTypeTitle(lesson: Lesson): String {
        return when (lesson.lessonType) {
            LessonType.LECTURE -> "Следующая лекция по предмету"
            LessonType.PRACTICE -> "Следующая практика по предмету"
            LessonType.SEMINAR -> "Следующий семинар по предмету"
            LessonType.LAB -> "Следующая лаб. работа по предмету"
            LessonType.CONSULTATION -> "Следующая консультация по предмету"
            LessonType.EXAM -> "Следующий экзамен по предмету"
            LessonType.CONTROL_WORK -> "Следующая контрольная работа по предмету"
            LessonType.EXTRA_EVENT -> "Следующее мероприятие по предмету"
            LessonType.CREDIT -> "Следующий зачет по предмету"
            LessonType.CREDIT_DIFF -> "Следующий дифф. зачет по предмету"
            else -> {
                val raw = lesson.rawLessonType.lowercase().trim()
                when {
                    raw.contains("лекци") -> "Следующая лекция по предмету"
                    raw.contains("семинар") -> "Следующий семинар по предмету"
                    raw.contains("практик") -> "Следующая практика по предмету"
                    raw.contains("лаборатор") -> "Следующая лаб. работа по предмету"
                    lesson.rawLessonType.isNotBlank() -> "Следующая ${lesson.rawLessonType.lowercase()} по предмету"
                    else -> "Следующая пара того же типа"
                }
            }
        }
    }

    /**
     * Searches lessons up to 3 weeks ahead for the same discipline and group.
     * Rule:
     * - If next found lesson has the same type as current lesson -> show only 1 item: "Следующая пара по предмету".
     * - If next found lesson has a different type -> show 2 items:
     *   1) "Следующая пара по предмету" (points to next upcoming lesson)
     *   2) "Следующая [тип] по предмету" (points to next upcoming lesson with same type as current lesson, if found).
     */
    fun findNextLessonsForDiscipline(
        currentLesson: Lesson,
        allLessons: List<Lesson>
    ): List<NextLessonInfo> {
        val currentDate = DateTimeUtils.parseDate(currentLesson.date) ?: return emptyList()
        val currentTime = DateTimeUtils.parseTime(currentLesson.startTime) ?: return emptyList()
        val currentDiscipline = currentLesson.discipline.trim()
        val currentGroup = currentLesson.group.trim()

        val candidates = allLessons.filter { candidate ->
            if (candidate.id == currentLesson.id) return@filter false

            // Match discipline (case-insensitive)
            if (!candidate.discipline.trim().equals(currentDiscipline, ignoreCase = true)) {
                return@filter false
            }

            // Match group if specified on both
            if (currentGroup.isNotBlank() && candidate.group.isNotBlank()) {
                if (!candidate.group.trim().equals(currentGroup, ignoreCase = true)) {
                    return@filter false
                }
            }

            val candDate = DateTimeUtils.parseDate(candidate.date) ?: return@filter false
            val candTime = DateTimeUtils.parseTime(candidate.startTime) ?: return@filter false

            // Must strictly take place after current lesson
            val isFuture = if (candDate.isEqual(currentDate)) {
                candTime.isAfter(currentTime)
            } else {
                candDate.isAfter(currentDate)
            }
            if (!isFuture) return@filter false

            // Up to 3 weeks (21 days) ahead
            val daysDiff = ChronoUnit.DAYS.between(currentDate, candDate)
            daysDiff in 0..21
        }

        // Sort chronologically
        val sortedCandidates = candidates.sortedWith(
            compareBy<Lesson> { DateTimeUtils.parseDate(it.date) }
                .thenBy { DateTimeUtils.parseTime(it.startTime) }
        )

        val nextAny = sortedCandidates.firstOrNull() ?: return emptyList()

        val sameType = isSameType(nextAny, currentLesson)

        if (sameType) {
            // Case 1: Types match -> show only 1 button
            return listOf(
                NextLessonInfo(
                    title = "Следующая пара по предмету",
                    lesson = nextAny
                )
            )
        } else {
            // Case 2: Types differ -> show up to 2 buttons
            val result = mutableListOf<NextLessonInfo>()
            result.add(
                NextLessonInfo(
                    title = "Следующая пара по предмету",
                    lesson = nextAny
                )
            )

            val nextSameType = sortedCandidates.firstOrNull { isSameType(it, currentLesson) }
            if (nextSameType != null) {
                result.add(
                    NextLessonInfo(
                        title = getNextTypeTitle(currentLesson),
                        lesson = nextSameType
                    )
                )
            }

            return result
        }
    }

    /**
     * User-friendly Russian date formatting for next lesson card badge.
     */
    fun formatLessonDateFriendly(dateStr: String): String {
        val date = DateTimeUtils.parseDate(dateStr) ?: return dateStr
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(today, date)

        val dayName = when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "Пн"
            java.time.DayOfWeek.TUESDAY -> "Вт"
            java.time.DayOfWeek.WEDNESDAY -> "Ср"
            java.time.DayOfWeek.THURSDAY -> "Чт"
            java.time.DayOfWeek.FRIDAY -> "Пт"
            java.time.DayOfWeek.SATURDAY -> "Сб"
            java.time.DayOfWeek.SUNDAY -> "Вс"
            else -> ""
        }

        val monthName = when (date.monthValue) {
            1 -> "янв"
            2 -> "фев"
            3 -> "мар"
            4 -> "апр"
            5 -> "мая"
            6 -> "июн"
            7 -> "июл"
            8 -> "авг"
            9 -> "сен"
            10 -> "окт"
            11 -> "ноя"
            12 -> "дек"
            else -> ""
        }

        return when (daysBetween) {
            0L -> "Сегодня, ${date.dayOfMonth} $monthName ($dayName)"
            1L -> "Завтра, ${date.dayOfMonth} $monthName ($dayName)"
            2L -> "Послезавтра, ${date.dayOfMonth} $monthName ($dayName)"
            else -> "${date.dayOfMonth} $monthName ($dayName)"
        }
    }
}
