package com.dlab.sirinium.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateTimeUtils {
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
    val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val DAY_NAME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale("ru"))
    val FULL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("ru"))
    val MONTH_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))

    fun todayFormatted(): String {
        return LocalDate.now().format(DATE_FORMATTER)
    }

    fun getWeekMonday(date: LocalDate = LocalDate.now()): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    fun calculateWeekOffset(targetDate: LocalDate, baseDate: LocalDate = LocalDate.now()): Int {
        val baseMonday = getWeekMonday(baseDate)
        val targetMonday = getWeekMonday(targetDate)
        return ChronoUnit.WEEKS.between(baseMonday, targetMonday).toInt()
    }

    fun getWeekDatesForOffset(weekOffset: Int, baseDate: LocalDate = LocalDate.now()): List<String> {
        val monday = getWeekMonday(baseDate).plusWeeks(weekOffset.toLong())
        return (0..6).map { day ->
            monday.plusDays(day.toLong()).format(DATE_FORMATTER)
        }
    }

    fun formatDate(date: LocalDate): String {
        return date.format(DATE_FORMATTER)
    }

    fun formatReadableDate(dateStr: String): String {
        val parsed = parseDate(dateStr) ?: return dateStr
        return parsed.format(FULL_DATE_FORMATTER)
    }

    fun formatShortDate(dateStr: String): String {
        val parsed = parseDate(dateStr) ?: return dateStr
        val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
        return parsed.format(formatter)
    }

    fun formatDateShort(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())
        return date.format(formatter)
    }

    /**
     * Formats a date relative to baseDate:
     * - today: "сегодня"
     * - tomorrow: "завтра"
     * - day after tomorrow: "послезавтра"
     * - other: "d MMMM" (e.g. "12 сентября")
     */
    fun formatRelativeDate(dateStr: String?, baseDate: LocalDate = LocalDate.now()): String {
        val parsed = parseDate(dateStr) ?: return dateStr.orEmpty()
        val daysDiff = ChronoUnit.DAYS.between(baseDate, parsed)
        return when (daysDiff) {
            0L -> "сегодня"
            1L -> "завтра"
            2L -> "послезавтра"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
                parsed.format(formatter)
            }
        }
    }

    fun formatLastUpdated(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "Никогда"
        val instant = java.time.Instant.ofEpochMilli(timestampMillis)
        val ldt = java.time.LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val date = ldt.toLocalDate()
        val timeStr = ldt.format(TIME_FORMATTER)
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(date, today)
        return when (daysDiff) {
            0L -> "сегодня в $timeStr"
            1L -> "вчера в $timeStr"
            else -> "${date.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault()))} в $timeStr"
        }
    }

    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dateStr.trim(), DATE_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    fun parseTime(timeStr: String?): LocalTime? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            LocalTime.parse(timeStr.trim(), TIME_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    fun toLocalDateTime(dateStr: String?, timeStr: String?): LocalDateTime? {
        val date = parseDate(dateStr) ?: return null
        val time = parseTime(timeStr) ?: return null
        return LocalDateTime.of(date, time)
    }

    fun toEpochMillis(dateStr: String?, timeStr: String?, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        val dt = toLocalDateTime(dateStr, timeStr) ?: return null
        return dt.atZone(zoneId).toInstant().toEpochMilli()
    }

    sealed interface LessonTimeStatus {
        data class Upcoming(val minutesUntilStart: Long) : LessonTimeStatus
        data class Ongoing(val minutesRemaining: Long) : LessonTimeStatus
        data object Finished : LessonTimeStatus
        data object FarAway : LessonTimeStatus
    }

    /**
     * Computes TickTick-style relative time status for a lesson.
     */
    fun calculateLessonStatus(
        dateStr: String?,
        startTimeStr: String?,
        endTimeStr: String?,
        now: LocalDateTime = LocalDateTime.now()
    ): LessonTimeStatus {
        val startDt = toLocalDateTime(dateStr, startTimeStr) ?: return LessonTimeStatus.FarAway
        val endDt = toLocalDateTime(dateStr, endTimeStr) ?: startDt.plusMinutes(90)

        return when {
            now.isAfter(endDt) -> LessonTimeStatus.Finished
            now.isBefore(startDt) -> {
                val minsUntil = ChronoUnit.MINUTES.between(now, startDt)
                if (minsUntil <= 180) {
                    LessonTimeStatus.Upcoming(minsUntil)
                } else {
                    LessonTimeStatus.FarAway
                }
            }
            else -> {
                val minsLeft = ChronoUnit.MINUTES.between(now, endDt)
                LessonTimeStatus.Ongoing(minsLeft.coerceAtLeast(1))
            }
        }
    }

    /**
     * Format status into Russian string representation (TickTick countdown pill)
     */
    fun formatStatusText(status: LessonTimeStatus): String? {
        return when (status) {
            is LessonTimeStatus.Upcoming -> {
                when {
                    status.minutesUntilStart <= 0 -> "Начинается прямо сейчас"
                    status.minutesUntilStart < 60 -> "До пары ${status.minutesUntilStart} мин"
                    else -> {
                        val hours = status.minutesUntilStart / 60
                        val mins = status.minutesUntilStart % 60
                        "Через ${hours}ч ${if (mins > 0) "${mins}м" else ""}"
                    }
                }
            }
            is LessonTimeStatus.Ongoing -> {
                "Идёт сейчас (осталось ${status.minutesRemaining} мин)"
            }
            LessonTimeStatus.Finished -> "Завершена"
            LessonTimeStatus.FarAway -> null
        }
    }

    data class StandardPairSlot(
        val pairNumber: Int,
        val startTime: String,
        val endTime: String,
        val startMinutes: Int,
        val endMinutes: Int,
        val timeSlot: String
    )

    val STANDARD_PAIRS: List<StandardPairSlot> = listOf(
        StandardPairSlot(1, "08:45", "10:05", 8 * 60 + 45, 10 * 60 + 5, "08:45 – 10:05"),
        StandardPairSlot(2, "10:20", "11:40", 10 * 60 + 20, 11 * 60 + 40, "10:20 – 11:40"),
        StandardPairSlot(3, "11:55", "13:15", 11 * 60 + 55, 13 * 60 + 15, "11:55 – 13:15"),
        StandardPairSlot(4, "13:30", "14:50", 13 * 60 + 30, 14 * 60 + 50, "13:30 – 14:50"),
        StandardPairSlot(5, "15:05", "16:25", 15 * 60 + 5, 16 * 60 + 25, "15:05 – 16:25"),
        StandardPairSlot(6, "16:40", "18:00", 16 * 60 + 40, 18 * 60 + 0, "16:40 – 18:00"),
        StandardPairSlot(7, "18:15", "19:35", 18 * 60 + 15, 19 * 60 + 35, "18:15 – 19:35"),
        StandardPairSlot(8, "19:50", "21:10", 19 * 60 + 50, 21 * 60 + 10, "19:50 – 21:10")
    )

    val PE_PAIR_1 = StandardPairSlot(1, "09:25", "10:45", 9 * 60 + 25, 10 * 60 + 45, "09:25 – 10:45")

    fun parseTimeToMinutes(timeStr: String?): Int? {
        if (timeStr.isNullOrBlank()) return null
        val clean = timeStr.trim()
        val parts = clean.split(":")
        if (parts.size < 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        return hours * 60 + minutes
    }

    fun isTimeAnomaly(startTime: String?, endTime: String?): Boolean {
        val startMin = parseTimeToMinutes(startTime) ?: return true
        val endMin = parseTimeToMinutes(endTime)
        val validSlots = STANDARD_PAIRS + PE_PAIR_1
        return if (endMin != null) {
            validSlots.none { it.startMinutes == startMin && it.endMinutes == endMin }
        } else {
            validSlots.none { it.startMinutes == startMin }
        }
    }

    fun doesIntervalOverlap(start1: Int, end1: Int, start2: Int, end2: Int): Boolean {
        return start1 < end2 && end1 > start2
    }

    fun doesLessonOverlapInterval(startTime: String?, endTime: String?, slotStartMinutes: Int, slotEndMinutes: Int): Boolean {
        val start = parseTimeToMinutes(startTime) ?: return false
        val end = parseTimeToMinutes(endTime) ?: (start + 80)
        return doesIntervalOverlap(start, end, slotStartMinutes, slotEndMinutes)
    }

    fun doesLessonOverlapSlot(startTime: String?, endTime: String?, slot: StandardPairSlot): Boolean {
        return doesLessonOverlapInterval(startTime, endTime, slot.startMinutes, slot.endMinutes)
    }

    /**
     * Shortens a person's full name (e.g. "Бахтин Александр Геннадьевич" -> "Бахтин А. Г.",
     * "Бахтин Александр" -> "Бахтин А."). If already short or contains multiple people (comma-separated),
     * formats each part properly.
     */
    fun formatShortPerson(fullName: String?): String {
        if (fullName.isNullOrBlank()) return ""
        val trimmed = fullName.trim()
        if (trimmed.contains(",")) {
            return trimmed.split(",").joinToString(", ") { formatShortPerson(it) }
        }
        val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.size >= 3 -> {
                val f = parts[1].firstOrNull()?.uppercaseChar()
                val o = parts[2].firstOrNull()?.uppercaseChar()
                if (f != null && o != null) "${parts[0]} $f. $o."
                else if (f != null) "${parts[0]} $f."
                else parts[0]
            }
            parts.size == 2 -> {
                val f = parts[1].firstOrNull()?.uppercaseChar()
                if (f != null) "${parts[0]} $f." else parts[0]
            }
            else -> trimmed
        }
    }
}
